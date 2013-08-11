package cn.buding.common.util;

import android.content.Context;

public class CacheHelpler extends PreferenceHelper {
	public static CacheHelpler getHelper(Context context) {
		return getHelper(context, DEFAULT_PREFERENCE_NAME);
	}

	public static CacheHelpler getHelper(Context context, String name) {
		PreferenceHelper helper = mHelpers.get(name);
		if (helper != null && !(helper instanceof CacheHelpler)) {
			mHelpers.remove(name);
		}
		if (!mHelpers.containsKey(name)) {
			mHelpers.put(name, new CacheHelpler(context, name));
		}
		return (CacheHelpler) mHelpers.get(name);
	}

	private CacheHelpler(Context context, String name) {
		super(context, name);
	}
	
	
}
