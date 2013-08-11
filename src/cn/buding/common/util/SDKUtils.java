package cn.buding.common.util;

import java.lang.reflect.Method;

public class SDKUtils {
	private static final Object[] EMPTY_PARAM = new Object[] {};

	public static <T> T getMethodBySdk(Object obj, String method,
			T defaultValue, int startSdk) {
		return getMethodBySdk(obj, method, defaultValue, startSdk, EMPTY_PARAM);
	}

	public static <T> T getMethodBySdk(Object obj, String method,
			T defaultValue, int startSdk, Object... params) {
		T res = defaultValue;
		if (android.os.Build.VERSION.SDK_INT >= startSdk) {
			try {
				Class<?>[] cs = new Class<?>[params.length];
				for (int i = 0; i < params.length; i++) {
					cs[i] = params[i].getClass();
					cs[i] = covertClass(cs[i]);
				}

				Method m = obj.getClass().getMethod(method, cs);
				res = (T) m.invoke(obj, params);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	private static Class<?> covertClass(Class<?> c) {
		if (c == null)
			return null;
		if (c == Integer.class)
			return int.class;
		if (c == Float.class)
			return float.class;
		if (c == Double.class)
			return double.class;
		if (c == Long.class)
			return long.class;
		return c;
	}
}
