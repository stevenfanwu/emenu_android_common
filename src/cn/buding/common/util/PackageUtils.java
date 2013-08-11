package cn.buding.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * provide utils for package information operation..
 */
public class PackageUtils {
	private static final String TAG = "PackageUtils";
	private static String packageName;
	private static String versionName;
	private static int versionCode = -1;

	public static int getMemoryClass(Context context) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		return am.getMemoryClass();
	}

	/** whether the pkg is installed */
	public static boolean isPackageInstalled(final Context context,
			final String pkgName) {
		PackageManager pm = context.getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(pkgName, 0);
			return pi != null;
		} catch (Exception e) {
			return false;
		}
	}

	/** whether the app is installed in rom */
	public static boolean isInstalledOnROM(final Context context, String pkgName) {
		ApplicationInfo appInfo;
		try {
			appInfo = context.getPackageManager()
					.getApplicationInfo(pkgName, 0);
			if (appInfo != null && appInfo.sourceDir != null
					&& appInfo.sourceDir.startsWith("/system/app")) {
				return true;
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static String getVersionName(Context ctx) {
		try {
			if (versionName == null) {
				ComponentName comp = new ComponentName(ctx, "");
				PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(
						comp.getPackageName(), 0);
				versionName = pinfo.versionName;
			}
		} catch (NameNotFoundException e) {
		}
		return versionName;
	}

	public static int getVersionCode(Context ctx) {
		try {
			if (versionCode < 0) {
				ComponentName comp = new ComponentName(ctx, "");
				PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(
						comp.getPackageName(), 0);
				versionCode = pinfo.versionCode;
			}
		} catch (NameNotFoundException e) {
		}
		return versionCode;
	}

	/** get meta data in manifest.xml */
	public static String getMetaData(Context ctx, String name) {
		String data = null;
		try {
			ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(
					ctx.getPackageName(), PackageManager.GET_META_DATA);
			data = ai.metaData.getString(name);
		} catch (Exception e) {
		}
		return data;

	}

	public static String getUmengChannel(Context ctx) {
		return getMetaData(ctx, "UMENG_CHANNEL");
	}

	public static String getIMEI(Context context) {
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}

	private static final String PRE_KEY_FAKE_IMEI = "pre_key_buding_log_imei";

	/**
	 * return the real IMEI if it exists, or return a custom string.
	 */
	public static String getCustomIMEI(Context context) {
		String imei = getIMEI(context);
		if (imei != null && imei.length() > 0) {
			PreferenceHelper.getHelper(context).writePreference(
					PRE_KEY_FAKE_IMEI, imei);
			return imei;
		} else {
			String imeiFake = PreferenceHelper.getHelper(context)
					.readPreference(PRE_KEY_FAKE_IMEI);
			if (imeiFake == null || imeiFake.length() == 0) {
				String time = new SimpleDateFormat("yyyyMMddHHmmss")
						.format(new Date());
				int r1 = (int) (Math.random() * 99999);
				int r2 = (int) (Math.random() * 99999);
				imeiFake = String.format("~%s%05d%05d", time, r1, r2);
				PreferenceHelper.getHelper(context).writePreference(
						PRE_KEY_FAKE_IMEI, imeiFake);
			}
			return imeiFake;
		}
	}

	public static String getIMSI(Context context) {
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getSubscriberId();
	}

	public static String getPhoneNo(Context context) {
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getLine1Number();
	}

	public static String getPackageName(Context context) {
		try {
			if (packageName == null) {
				ComponentName comp = new ComponentName(context, "");
				PackageInfo pinfo = context.getPackageManager().getPackageInfo(
						comp.getPackageName(), 0);
				packageName = pinfo.packageName;
			}
		} catch (NameNotFoundException e) {
		}
		return packageName;
	}

	public static String getMacAddress(Context context) {
		String macAddress = "";
		WifiManager wifimanager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiinfo = wifimanager.getConnectionInfo();
		if (wifiinfo != null && wifiinfo.getMacAddress() != null
				&& wifiinfo.getMacAddress().length() > 0) {
			macAddress = wifiinfo.getMacAddress();
		}
		return macAddress;
	}

	public static final String SYS_PROP_MOD_VERSION = "ro.modversion";

	public static String getModVersion() {
		String modVer = getSystemProperty(SYS_PROP_MOD_VERSION);

		return (modVer == null || modVer.length() == 0 ? "Unknown" : modVer);
	}

	public static String getSystemProperty(String propName) {
		String line;
		BufferedReader input = null;
		try {
			Process p = Runtime.getRuntime().exec("getprop " + propName);
			input = new BufferedReader(
					new InputStreamReader(p.getInputStream()), 1024);
			line = input.readLine();
			input.close();
		} catch (IOException ex) {
			Log.e(TAG, "Unable to read sysprop " + propName, ex);
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					Log.e(TAG, "Exception while closing InputStream", e);
				}
			}
		}
		return line;
	}
}
