package cn.buding.common.util;

public final class Base64 {

	public static byte[] encodeBase64(byte[] data) {
		return org.apache.commons.codec.binary.Base64.encodeBase64(data);
	}

	public static byte[] encodeBase64(String data) {
		return encodeBase64(data.getBytes());
	}

	public static String encodeBase64String(byte[] data) {
		return new String(encodeBase64(data));
	}

	public static String encodeBase64String(String data) {
		return new String(encodeBase64(data.getBytes()));
	}

	public static byte[] decodeBase64(byte[] data) {
		return org.apache.commons.codec.binary.Base64.decodeBase64(data);
	}

	public static byte[] decodeBase64(String data) {
		return decodeBase64(data.getBytes());
	}

	public static String decodeBase64String(byte[] data) {
		return new String(decodeBase64(data));
	}

	public static String decodeBase64String(String data) {
		return new String(decodeBase64(data.getBytes()));
	}
}
