package cn.buding.common.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.buding.common.exception.JSONParseException;
import android.util.Log;

/**
 * utils for JSONParser.
 */
public class JSONUtils {
	private static final String TAG = "JSONUtils";

	public static <T extends JSONBean> T parseJSONToObject(Class<T> tlass,
			String json) throws JSONParseException {
		try {
			if (isChild(tlass, JSONArrayBean.class)) {
				Class<? extends JSONArrayBean<? extends JSONBean>> arrayClass =
						(Class<? extends JSONArrayBean<? extends JSONBean>>) tlass;
				return (T) parseJSONToObject(arrayClass, new JSONArray(json));
			} else
				return parseJSONToObject(tlass, new JSONObject(json));
		} catch (Exception e) {
			throw new JSONParseException("input json format error", e);
		}
	}

	/** child extend or implements parent */
	private static boolean isChild(Class<?> child, Class<?> parent) {
		boolean isChild = false;
		Class<?> c = child;
		while (c != null) {
			if (c == parent || contain(c.getInterfaces(), parent))
				return true;
			c = c.getSuperclass();
		}
		return isChild;
	}

	private static boolean contain(Object[] list, Object obj) {
		for (Object o : list)
			if (o == obj)
				return true;
		return false;
	}

	public static <T extends JSONArrayBean> T parseJSONToObject(Class<T> tlass,
			JSONArray jsArray) throws JSONParseException {
		try {
			T list = tlass.newInstance();
			for (int i = 0; i < jsArray.length(); i++) {
				JSONBean item =
						parseJSONToObject(list.getGenericClass(),
								jsArray.getJSONObject(i));
				list.add(item);
			}
			return list;
		} catch (Exception e) {
			throw new JSONParseException("Class: " + tlass.getName()
					+ "can not be instantiate.", e);
		}
	}

	public static <T extends JSONBean> T parseJSONToObject(Class<T> klass,
			JSONObject jsObj) throws JSONParseException {
		if (jsObj == null)
			return null;
		T instance = null;
		try {
			instance = klass.newInstance();
		} catch (Exception e) {
			throw new JSONParseException(
					"can not create instance for " + klass, e);
		}
		Field[] fields = klass.getFields();
		Method[] methods = klass.getMethods();
		Member[] members = new Member[fields.length + methods.length];
		for (int i = 0; i < fields.length; i++)
			members[i] = fields[i];
		for (int i = 0; i < methods.length; i++)
			members[i + fields.length] = methods[i];

		boolean optional = false;
		String key = null;
		Object value = null;
		Class<?> fType = null;

		for (Member member : members) {
			if (!Modifier.isPublic(member.getModifiers())
					|| Modifier.isStatic(member.getModifiers()))
				continue;
			optional = containAnnotation(member, Optional.class);
			if (member instanceof Method) {
				Method method = (Method) member;
				String mName = method.getName();
				if (!mName.startsWith("set") || key.length() == 0
						|| Character.isLowerCase(key.charAt(0))
						|| method.getParameterTypes().length != 1)
					continue;
				fType = method.getParameterTypes()[0];
				key = mName.substring(3);
				if (key.length() == 1)
					key = key.toLowerCase();
				else if (!Character.isUpperCase(key.charAt(1)))
					key = key.substring(0, 1).toLowerCase() + key.substring(1);
			} else if (member instanceof Field) {
				Field field = (Field) member;
				fType = field.getType();
				key = field.getName();
			} else
				continue;
			if (jsObj.has(key)) {
				try {
					value = jsObj.get(key);
				} catch (JSONException e) {
					Log.e(TAG, "", e);
				}
				try {
					Object newValue = wrapToObject(fType, value);
					if (member instanceof Field) {
						Field field = (Field) member;
						field.set(instance, newValue);
					} else if (member instanceof Method) {
						Method method = (Method) member;
						method.invoke(instance, new Object[] { newValue });
					}
				} catch (Exception e) {
					Log.e(TAG, "Error in setting Value: " + value
							+ ", Method: " + member.getName(), e);
				}
			} else {
				if (!optional) {
					Log.e(TAG, member.getDeclaringClass().getName() + "." + key
							+ " doesn't exist in json object");
				}
			}
		}
		return instance;
	}

	private static Object wrapToObject(Class<?> fType, Object value)
			throws JSONParseException {
		try {
			String typeName = fType.getSimpleName();
			if (typeName.equals("int") || typeName.equals("Integer")) {
				return Integer.valueOf(value.toString());
			} else if (typeName.equals("boolean") || typeName.equals("Boolean")) {
				return Boolean.valueOf(value.toString());
			} else if (typeName.equals("double") || typeName.equals("Double")) {
				return Double.valueOf(value.toString());
			} else if (typeName.equals("String")) {
				return value.toString();
			} else if (typeName.equals("Date")) {
				Timestamp t = Timestamp.valueOf(value.toString());
				return new Date(t.getTime());
			} else if (typeName.equals("String[]")) {
				String[] strs = null;
				JSONArray ja = new JSONArray(value.toString());
				int count = ja.length();
				strs = new String[count];
				for (int i = 0; i < count; i++)
					strs[i] = ja.getString(i);
				return strs;
			} else if (typeName.equals("int[]")) {
				int[] ints = null;
				JSONArray ja = new JSONArray(value.toString());
				int count = ja.length();
				ints = new int[count];
				for (int i = 0; i < count; i++)
					ints[i] = ja.getInt(i);
				return ints;
			} else if (fType.isArray()) {
				String gName = fType.toString();
				int s = gName.indexOf("[L") + 2;
				int e = gName.indexOf(";");
				gName = gName.substring(s, e);
				Class<? extends JSONBean> subC =
						(Class<? extends JSONBean>) Class.forName(gName);
				JSONArray ja = new JSONArray(value.toString());
				int count = ja.length();
				Object objs = Array.newInstance(subC, count);
				for (int i = 0; i < count; i++) {
					JSONObject jsob = ja.getJSONObject(i);
					JSONBean subInstance = parseJSONToObject(subC, jsob);
					Array.set(objs, i, subInstance);
				}
				return objs;
			} else {
				Class<? extends JSONBean> cla =
						(Class<? extends JSONBean>) fType;
				JSONObject jsob = new JSONObject(value.toString());
				JSONBean obj = parseJSONToObject(cla, jsob);
				return obj;
			}
		} catch (Exception e) {
			throw new JSONParseException(e);
		}
	}

	public static JSONObject parseObjectToJSON(Object instance) {
		Field[] fields = instance.getClass().getFields();
		Method[] methods = instance.getClass().getMethods();
		String key = null;
		Object value = null;
		JSONObject res = new JSONObject();
		Member[] members = new Member[fields.length + methods.length];
		for (int i = 0; i < fields.length; i++)
			members[i] = fields[i];
		for (int i = 0; i < methods.length; i++)
			members[i + fields.length] = methods[i];
		boolean discard;
		for (Member member : members) {
			try {
				if (!Modifier.isPublic(member.getModifiers())
						|| Modifier.isStatic(member.getModifiers()))
					continue;
				discard = containAnnotation(member, Discard.class);
				if (discard)
					continue;
				key = member.getName();
				if (member instanceof Field) {
					Field field = (Field) member;
					value = field.get(instance);
				} else if (member instanceof Method) {
					Method method = (Method) member;
					if (key.startsWith("get")) {
						if (key.equals("getClass")
								|| key.equals("getDeclaringClass")) {
							key = "";
						} else {
							key = key.substring(3);
						}
					} else if (key.startsWith("is")) {
						key = key.substring(2);
					} else
						continue;
					if (key.length() == 0
							|| Character.isLowerCase(key.charAt(0))
							|| method.getParameterTypes().length != 0)
						continue;
					if (key.length() == 1) {
						key = key.toLowerCase();
					} else if (!Character.isUpperCase(key.charAt(1))) {
						key =
								key.substring(0, 1).toLowerCase()
										+ key.substring(1);
					}
					value = method.invoke(instance, new Object[] {});
				}
				value = wrapToJSON(value);
				res.putOpt(key, value);
			} catch (Exception e) {
				Log.e(TAG,
						"fail in parsing field/value : " + key + "/" + value, e);
			}
		}
		return res;
	}

	private static Object wrapToJSON(Object object) {
		try {
			if (object == null) {
				return null;
			}
			if (object instanceof JSONObject || object instanceof JSONArray
					|| object instanceof Byte || object instanceof Character
					|| object instanceof Short || object instanceof Integer
					|| object instanceof Long || object instanceof Boolean
					|| object instanceof Float || object instanceof Double
					|| object instanceof String) {
				return object;
			}

			if (object instanceof Date) {
				Date d = (Date) object;
				Timestamp t = new Timestamp(d.getTime());
				return t.toString();
			}

			if (object.getClass().isArray()) {
				JSONArray array = new JSONArray();
				int length = Array.getLength(object);
				for (int i = 0; i < length; i += 1) {
					array.put(wrapToJSON(Array.get(object, i)));
				}
				return array;
			}

			Package objectPackage = object.getClass().getPackage();
			String objectPackageName =
					objectPackage != null ? objectPackage.getName() : "";
			if (objectPackageName.startsWith("java.")
					|| objectPackageName.startsWith("javax.")
					|| object.getClass().getClassLoader() == null) {
				return object.toString();
			}
			return parseObjectToJSON(object);
		} catch (Exception e) {
			Log.e(TAG, "error in wrapingToJSON", e);
			return null;
		}
	}

	private static boolean containAnnotation(Member member,
			Class<? extends Annotation> anno) {
		if (member instanceof Method) {
			Method method = (Method) member;
			return method.getAnnotation(anno) != null;
		} else if (member instanceof Field) {
			Field field = (Field) member;
			return field.getAnnotation(anno) != null;
		}
		return false;
	}

}
