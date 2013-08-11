package cn.buding.common.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

/**
 * utils for intent operation.
 */
public class IntentUtil {
	private static final String TAG = "IntentUtil";

	private static void startActivity(Context context, Intent intent) {
		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "", e);
		}
	}

	/**
	 * try to view a location in google map.
	 */
	public static void startGoogleMap(Context context, Location loc) {
		if (loc == null)
			return;
		Intent intent =
				new Intent(Intent.ACTION_VIEW,
						Uri.parse("http://ditu.google.cn/maps?hl=zh&mrt=loc&q="
								+ loc.getLatitude() + "," + loc.getLongitude()));
		startActivity(context, intent);
	}

	/**
	 * try to start a phone call.if the tels contain multi-tel, we will show a dialog and let user to choose.
	 * 
	 * @param tels the input tels.
	 */
	public static void startPhoneCall(final Context context, String tels) {
		if (tels == null || tels.length() == 0)
			return;
		List<String> telList = parseTels(tels);
		final String[] telStrs = telList.toArray(new String[] {});
		if (telStrs.length == 0)
			return;
		else if (telStrs.length == 1) {
			startTel(context, telStrs[0]);
		} else {
			new AlertDialog.Builder(context)
					.setTitle("请选择电话号码")
					.setSingleChoiceItems(telStrs, -1,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									startTel(context, telStrs[which]);
								}
							}).show();
		}
	}

	private static void startTel(Context context, String tel) {
		Intent intent =
				new Intent("android.intent.action.DIAL",
						Uri.parse("tel:" + tel));
		startActivity(context, intent);
	}

	private static List<String> telPrefixs = Arrays.asList(new String[] {
			"400", "800", "86" });

	private static List<String> parseTels(String tels) {
		char[] chars = tels.toCharArray();
		for (int i = 0, len = chars.length; i < len; i++) {
			if (!Character.isDigit(chars[i]) && chars[i] != '-') {
				chars[i] = ' ';
			}
		}
		tels = new String(chars);
		String[] result = tels.split(" +");
		List<String> res = new ArrayList<String>();
		int length = result.length;
		for (int i = 0; i < length; i++) {
			String s = result[i];
			if (s == null || s.length() == 0)
				continue;
			s = s.trim();
			if (telPrefixs.contains(s) && i < length - 1) {
				s += result[++i];
			}
			if (!res.contains(s))
				res.add(s);
		}
		return res;
	}

	/**
	 * start a browser to view a web site.
	 */
	public static void startWeb(Context context, String url) {
		if (url == null)
			return;
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(context, intent);
	}

	/**
	 * show a image.
	 */
	public static void startImg(Context context, File file) {
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file), "image/*");
		startActivity(context, intent);
	}
	
	/**
	 * 
	 * show video
	 * @param context
	 * @param url
	 */
	public static void startVideo(Context context, String url) {
		if (url == null) {
			return;
		}
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(url), "video/mp4");
		startActivity(context, intent);
	}

	/**
	 * go to send message page.
	 */
	public static void sendMessage(Context context, String address,
			String content) {
		Uri uri = Uri.parse("smsto:" + address);
		Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
		intent.putExtra("sms_body", content);
		startActivity(context, intent);
	}

	/**
	 * send multi-media message.
	 * 
	 * @param context
	 * @param address the address of receiver
	 * @param content the content of message
	 * @param file the content of the attached file.
	 */
	public static void sendMessage(Context context, String address,
			String content, File file) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra("address", address);
		intent.putExtra("sms_body", content);
		Uri uri = Uri.fromFile(file);
		intent.putExtra(Intent.EXTRA_STREAM, uri);
		String suffix = MimeTypeMap.getFileExtensionFromUrl(file.getName());
		String mime = null;
		if (!"".equals(suffix))
			mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
		if (mime == null)
			mime = "image/jpeg";
		intent.setType(mime);

		startActivity(context, intent);
	}

}
