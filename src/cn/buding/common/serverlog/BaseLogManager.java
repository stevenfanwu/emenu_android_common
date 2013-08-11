package cn.buding.common.serverlog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import cn.buding.common.util.DateUtil;
import cn.buding.common.util.PreferenceHelper;

/**
 * base class for log manager
 */
public abstract class BaseLogManager<T> {
	private static final String TAG = "BaseLogManager";
	protected static SimpleDateFormat dateFormat = DateUtil.yyyyMMddHHmmss;

	private int mMaxCacheCount = 100;
	/**
	 * restore all log in memory.
	 */
	protected List<T> logList = new ArrayList<T>();;
	/**
	 * a log buffer, contain all logs that are ready to send to server.
	 */
	protected List<T> logBuffer = new ArrayList<T>();;
	/**
	 * whether the log manager is uploading. when the logs are sending to
	 * server, readable is false.
	 */
	protected boolean isUploading = false;
	protected Context mContext;

	private static final String PRE_NAME = "log_preference";

	private boolean mSaveRealTime = false;

	public BaseLogManager(Context context) {
		mContext = context.getApplicationContext();
		init();
	}

	public Context getContext() {
		return mContext;
	}

	private void init() {
		isUploading = false;
		retainCacheLog();
	}

	public void destory() {
		saveCacheLog();
		isUploading = false;
	}

	public void setSaveRealTime(boolean b) {
		mSaveRealTime = b;
	}

	public abstract String parseToString(T t);

	public abstract T parseToObject(String s);

	private static final String LIST_BREAKER = "\n";

	public String parseToString(List<T> list) {
		if (list == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (T t : list) {
			String s = parseToString(t);
			if (s != null)
				sb.append(s + LIST_BREAKER);
		}
		return sb.toString();
	}

	public List<T> parseToList(String s) {
		List<T> res = new ArrayList<T>();
		if (s == null)
			return res;
		String[] strs = s.split(LIST_BREAKER);
		for (String str : strs) {
			T t = parseToObject(str);
			if (t != null)
				res.add(t);
		}
		return res;
	}

	/**
	 * get file name to save logs which are not sent to server.
	 */
	protected String getLogCacheName() {
		return getClass().getName();
	}

	private PreferenceHelper getPreHelper() {
		PreferenceHelper res = PreferenceHelper.getHelper(mContext, PRE_NAME);
		res.setFlag(PreferenceHelper.FLAG_ONLY_FILE_RESTORE_MODE);
		return res;
	}

	private static final String SUFFIX_LIST = "_list";
	private static final String SUFFIX_BUF = "_buf";

	/**
	 * read log file to memory.
	 */
	private void retainCacheLog() {
		String strList = getPreHelper().readPreference(
				getLogCacheName() + SUFFIX_LIST);
		String strBuf = getPreHelper().readPreference(
				getLogCacheName() + SUFFIX_BUF);
		logList.clear();
		logBuffer.clear();
		logList.addAll(parseToList(strList));
		logBuffer.addAll(parseToList(strBuf));
	}

	/**
	 * write the all logs in memory to preferences
	 */
	private void saveCacheLog() {
		saveCacheList();
		saveCacheBuf();
	}

	private void saveCacheList() {
		String listStr = parseToString(logList);
		getPreHelper()
				.writePreference(getLogCacheName() + SUFFIX_LIST, listStr);
	}

	private void saveCacheBuf() {
		String listBuf = parseToString(logBuffer);
		getPreHelper().writePreference(getLogCacheName() + SUFFIX_BUF, listBuf);
	}

	/**
	 * This method is called when sending logs to server
	 */
	public synchronized List<T> getUploadLogArray() {
		return getLogArray();
	}

	private synchronized List<T> getLogArray() {
		Log.i(TAG, "getLogArray, isUploading:" + isUploading + ", logList.size:" + logList.size() + ", logBuffer.size"
				+ logBuffer.size());
		if (isUploading || isEmpty())
			return null;
		isUploading = true;
		// logList -> logBuffer
		logBuffer.addAll(logList);
		logList.clear();
		if (mSaveRealTime)
			saveCacheLog();
		List<T> list = new ArrayList<T>();
		list.addAll(logBuffer);
		return list;
	}

	public boolean isEmpty() {
		return logList.size() + logBuffer.size() == 0;
	}

	/**
	 * If send log successfully, clear logBuffer
	 */
	public synchronized void logSuccess() {
		Log.i(TAG,
				"Succeed in sending log, sended count is " + logBuffer.size());
		logBuffer.clear();
		if (mSaveRealTime)
			saveCacheBuf();
		isUploading = false;
	}

	/**
	 * log send to server failed.
	 */
	public synchronized void logFail() {
		Log.i(TAG, "Failed in sending log");
		isUploading = false;
	}

	public void addLog(T log) {
		addLog(log, logList.size());
	}

	/**
	 * add json log to log list.
	 */
	public synchronized void addLog(T log, int index) {
		if (index < 0 || index > logList.size())
			return;
		Log.d(TAG, "addLog, " + log.toString());
		while (!canLog() && logList.size() > 0) {
			logList.remove(0);
			index--;
		}
		logList.add(index, log);
		if (mSaveRealTime)
			saveCacheList();
		if (mOnLogStatusChangedListener != null)
			mOnLogStatusChangedListener.onLogAdd(log);
	}

	public int getLogCount() {
		return logList.size() + logBuffer.size();
	}

	public void setMaxCacheLogCount(int count) {
		mMaxCacheCount = count;
	}

	public int getMaxCacheLogCount() {
		return mMaxCacheCount;
	}

	/**
	 * @return whether the num of log in memory is smaller than max count.
	 */
	private boolean canLog() {
		return (logList.size() + logBuffer.size() < mMaxCacheCount);
	}

	private OnLogStatusChangedListener<T> mOnLogStatusChangedListener;

	public void setOnLogStatusListener(OnLogStatusChangedListener<T> l) {
		mOnLogStatusChangedListener = l;
	}

	public static interface OnLogStatusChangedListener<T> {
		public void onLogAdd(T log);
	}

}
