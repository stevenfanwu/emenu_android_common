package cn.buding.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

import android.util.Log;

public class CodecUtils {
	private static final String TAG = "CodecUtils";
	private static final String UTF_8 = "UTF-8";

	public static String md5Hex(String input) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			byte[] md5 = messageDigest.digest(input.getBytes(UTF_8));
			return new String(Hex.encodeHex(md5));
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

	public static String getFileMD5String(File file) {
		try {
			FileInputStream in = new FileInputStream(file);
			FileChannel ch = in.getChannel();
			MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY,
					0, file.length());
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(byteBuffer);
			return new String(Hex.encodeHex(messageDigest.digest()));
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

}
