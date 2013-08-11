package cn.buding.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;

public class ParcelHelper {
	private static final String TAG = "ParcelHelper";
	private static final String DEFAULT_PREFERENCE_NAME = "parcel_preference";
	private static Map<String, ParcelHelper> mHelpers =
			new HashMap<String, ParcelHelper>();

	public static ParcelHelper getHelper(Context context) {
		return getHelper(context, DEFAULT_PREFERENCE_NAME);
	}

	public static ParcelHelper getHelper(Context context, String name) {
		if (!mHelpers.containsKey(name)) {
			mHelpers.put(name, new ParcelHelper(context, name));
		}
		return mHelpers.get(name);
	}

	private Context mContext;
	private String mPreName;

	private ParcelHelper(Context context, String preName) {
		mContext = context.getApplicationContext();
		mPreName = preName;
	}

	public void writeObject(String preKey, Parcelable object) {
		if (object == null)
			return;
		Parcel parcel = Parcel.obtain();
		object.writeToParcel(parcel, 0);
		byte[] bytes = parcel.marshall();
		String byteStr = new String(Base64.encodeBase64(bytes));
		writePreference(preKey, byteStr);
	}

	@SuppressWarnings("unchecked")
	public <T extends Parcelable> T readObject(Class<T> klass, String preKey) {
		try {
			Parcelable.Creator<T> creator =
					(Creator<T>) klass.getField("CREATOR").get(null);
			String str = readPreference(preKey);
			if (str == null)
				return null;
			byte[] data = Base64.decodeBase64(str.getBytes());
			Parcel p = Parcel.obtain();
			p.unmarshall(data, 0, data.length);
			p.setDataPosition(0);
			T t = creator.createFromParcel(p);
			return t;
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return null;
		}
	}

	public void writeArray(String preKey, Collection<? extends Parcelable> object) {
		if (object == null)
			return;
		Parcel parcel = Parcel.obtain();
		for (Parcelable p : object) {
			p.writeToParcel(parcel, 0);
		}
		byte[] bytes = parcel.marshall();
		String byteStr = new String(Base64.encodeBase64(bytes));
		writePreference(preKey, byteStr);
	}

	private static final int DEFAULT_MAX_ARRAY_SIZE = 10;

	public <T extends Parcelable> List<T> readArray(Class<T> klass,
			String preKey) {
		return readArray(klass, preKey, DEFAULT_MAX_ARRAY_SIZE);
	}

	@SuppressWarnings("unchecked")
	public <T extends Parcelable> List<T> readArray(Class<T> klass,
			String preKey, int maxArraySize) {
		try {
			Parcelable.Creator<T> creator =
					(Creator<T>) klass.getField("CREATOR").get(null);
			String str = readPreference(preKey);
			if (str == null)
				return null;
			byte[] data = Base64.decodeBase64(str.getBytes());
			Parcel p = Parcel.obtain();
			p.unmarshall(data, 0, data.length);
			p.setDataPosition(0);
			List<T> list = new ArrayList<T>();
			int arraySize = 0;
			while (p.dataAvail() > 0) {
				try {
					if (arraySize >= maxArraySize)
						break;
					arraySize++;
					T t = creator.createFromParcel(p);
					list.add(t);
				} catch (Throwable e) {
					Log.e(TAG, "", e);
					break;
				}
			}
			return list;
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return null;
		}
	}

	private void writePreference(String key, String value) {
		PreferenceHelper.getHelper(mContext, mPreName).writePreference(key,
				value);
	}

	private String readPreference(String key) {
		return PreferenceHelper.getHelper(mContext, mPreName).readPreference(
				key);
	}
}
