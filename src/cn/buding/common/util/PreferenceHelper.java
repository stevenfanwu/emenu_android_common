package cn.buding.common.util;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import cn.buding.common.file.FileUtil;
import cn.buding.common.net.NetUtil;

public class PreferenceHelper {
	private static final String TAG = "PreferenceHelper";
	public static String DEFAULT_PREFERENCE_NAME = "preference_name_propertyarray";
	private final static String SUFFIX_DATE = "_date";
	public static final long NOTICE_UPDATE_TIME_SPAN = 3 * 3600 * 1000;
	public final static long MAX_DATA_AVAILABLE_TIME_DAILY = 24 * 3600 * 1000;
	public final static long MAX_DATA_AVAILABLE_TIME_WEEKLY = 7 * 24 * 3600 * 1000;
	public final static long MAX_DATA_AVAILABLE_TIME_MONTYLY = 30 * 24 * 3600 * 1000l;
	public final static long MAX_VALID_TIME = MAX_DATA_AVAILABLE_TIME_MONTYLY;

	private static final String PREFIX_COMPRESS = "@compress_prefix@";
	private static final int MIN_COMPRESS_THRESHOLD = 50;

	protected static Map<String, PreferenceHelper> mHelpers = new HashMap<String, PreferenceHelper>();

	public static PreferenceHelper getHelper(Context context) {
		return getHelper(context, DEFAULT_PREFERENCE_NAME);
	}

	public static PreferenceHelper getHelper(Context context, String name) {
		if (!mHelpers.containsKey(name)) {
			mHelpers.put(name, new PreferenceHelper(context, name));
		}
		return mHelpers.get(name);
	}

	private static final String DEFAULT_FOLDER = "preference";
	public static final int FLAG_COMPRESS_MODE = 0x1;
	public static final int FLAG_ONLY_FILE_RESTORE_MODE = 0x2;
	public static final int FLAG_FILE_AND_XML_RESTORE_MODE = 0x4;

	protected Context mContext;
	protected String mName;
	private SharedPreferences preference;
	private int mFlag = 0;

	private File mFolder;

	protected PreferenceHelper(Context context, String name) {
		mContext = context.getApplicationContext();
		mName = name;
		mFlag = FLAG_COMPRESS_MODE;
		String folder = GlobalProperties.getFolderPath(context, DEFAULT_FOLDER
				+ "/" + name);
		mFolder = FileUtil.getFolder(context, folder, false);
		initPreference();
	}

	private void initPreference() {
		preference = mContext.getSharedPreferences(mName, Context.MODE_PRIVATE);
		// clear invalid cache.
		long totalSize = 0;
		Map<String, ?> map = preference.getAll();
		for (String key : map.keySet()) {
			String dKey = getDateKey(key);
			if (preference.contains(dKey)) {
				readPreferenceWithDate(key, MAX_VALID_TIME);
			}
			totalSize += key.length();
			totalSize += map.get(key).toString().length();
		}
		Log.i(TAG, "preference " + mName + " size: " + totalSize / 1024 + "kb");
	}

	public void setFlag(int flag) {
		mFlag |= flag;
	}

	public void removeFlag(int flag) {
		mFlag &= ~flag;
	}

	public boolean isCompressMode() {
		return (mFlag & FLAG_COMPRESS_MODE) != 0;
	}

	public boolean isOnlyFileRestoreMode() {
		return (mFlag & FLAG_ONLY_FILE_RESTORE_MODE) != 0;
	}

	public boolean isFileXmlRestoreMode() {
		return (mFlag & FLAG_FILE_AND_XML_RESTORE_MODE) != 0;
	}

	private boolean shouldSaveInFile() {
		return mFolder != null
				&& (isOnlyFileRestoreMode() || isFileXmlRestoreMode());
	}

	private boolean shouldSaveInXml() {
		return mFolder == null || !isOnlyFileRestoreMode();
	}

	public String readPreferenceWeekly(String name) {
		return readPreferenceWithDate(name, MAX_DATA_AVAILABLE_TIME_WEEKLY);
	}

	public String readPreferenceMonthly(String name) {
		return readPreferenceWithDate(name, MAX_DATA_AVAILABLE_TIME_MONTYLY);
	}

	public String readPreferenceDaily(String name) {
		return readPreferenceWithDate(name, MAX_DATA_AVAILABLE_TIME_DAILY);
	}

	public String readPreferenceAndDate(String name, Date outDate) {
		String res = null;
		try {
			res = readPreference(name);
			if (res == null || res.length() == 0)
				return null;
			String date = readPreference(getDateKey(name));
			if (date == null)
				return null;
			long dateLong = Long.valueOf(date);
			outDate.setTime(dateLong);
		} catch (Exception e) {
			return null;
		}
		return res;
	}

	public String readPreferenceWithDate(String name, long period) {
		Date inDate = new Date();
		String res = readPreferenceAndDate(name, inDate);
		if (res != null
				&& System.currentTimeMillis() - inDate.getTime() > period) {
			removePreferenceWithDate(name);
			return null;
		}
		return res;
	}

	public void writePreferenceWithDate(String name, String value) {
		writePreference(name, value);
		writePreference(getDateKey(name), "" + System.currentTimeMillis());
	}

	public void writePreferenceWithDate(String name) {
		writePreferenceWithDate(name, "{}");
	}

	public void writePreference(String name) {
		writePreference(name, "{}");
	}

	public void removePreferenceWithDate(String name) {
		removePreference(name);
		removePreference(getDateKey(name));
	}

	public void writePreference(String name, String value, boolean append) {
		if (name == null || value == null)
			return;
		Editor editor = preference.edit();
		writePreference(editor, name, value, append);
		editor.commit();
	}

	public void writePreference(Map<String, String> data, boolean append) {
		if (data == null)
			return;
		Editor editor = preference.edit();
		for (String name : data.keySet()) {
			String value = data.get(name);
			writePreference(editor, name, value, append);
		}
		editor.commit();
	}

	private void writePreference(Editor editor, String name, String value,
			boolean append) {
		if (name == null || value == null)
			return;
		if (append) {
			String text = readPreference(name);
			value = text + value;
		}
		if (isCompressMode() && value.length() > MIN_COMPRESS_THRESHOLD) {
			// compress the value if it is in compress mode.
			value = compress(value);
		}
		if (shouldSaveInXml()) {
			editor.putString(name, value);
		}
		if (shouldSaveInFile()) {
			// write file to sdcard
			FileUtil.writeFile(getKeyFile(name), value);
		}
	}

	public void writePreference(String name, String value) {
		writePreference(name, value, false);
	}

	public void removePreference(String name) {
		Editor editor = preference.edit();
		removePreference(editor, name);
		editor.commit();
	}

	public void removePreference(List<String> names) {
		if (names == null)
			return;
		Editor editor = preference.edit();
		for (String name : names) {
			removePreference(editor, name);
		}
		editor.commit();
	}

	private void removePreference(Editor editor, String name) {
		Log.i(TAG, "remove preference " + name);
		if (shouldSaveInXml()) {
			editor.remove(name);
		}
		if (shouldSaveInFile()) {
			// remove file.
			File file = getKeyFile(name);
			if (file.exists())
				file.delete();
		}
	}

	public boolean containPreference(String name) {
		boolean inPre = false;
		if (shouldSaveInXml()) {
			inPre |= preference.contains(name);
		}
		if (!inPre && shouldSaveInFile()) {
			inPre |= getKeyFile(name).exists();
		}
		return inPre;
	}

	public String readPreference(String name) {
		if (name == null)
			return null;

		String value = null;
		if (shouldSaveInXml()) {
			value = preference.getString(name, null);
		}
		if (value == null && shouldSaveInFile()) {
			value = FileUtil.readFileContent(getKeyFile(name));
		}
		return decompress(value);

	}

	public void clearPreference() {
		preference.edit().clear().commit();
		// TODO clear file restore, not support now.
	}

	private File getKeyFile(String keyName) {
		if (mFolder == null)
			return null;
		String fileName = CodecUtils.md5Hex(keyName);
		return new File(mFolder, fileName);
	}

	private static String getDateKey(String name) {
		return name + SUFFIX_DATE;
	}

	public SharedPreferences getPreference() {
		return preference;
	}

	private static String compress(String s) {
		try {
			byte[] compressedData = NetUtil.gzipCompress(s.getBytes());
			byte[] base64Data = Base64.encodeBase64(compressedData);
			String data = new String(base64Data);
			return PREFIX_COMPRESS + data;
		} catch (IOException e) {
			Log.e(TAG, "", e);
			return null;
		}
	}

	private static String decompress(String s) {
		if (s == null)
			return null;
		if (!s.startsWith(PREFIX_COMPRESS)) {
			return s;
		}
		try {
			s = s.substring(PREFIX_COMPRESS.length());
			byte[] data = Base64.decodeBase64(s.getBytes());
			return new String(NetUtil.gzipDecompress(data));
		} catch (IOException e) {
			Log.e(TAG, "", e);
			return null;
		}
	}
}
