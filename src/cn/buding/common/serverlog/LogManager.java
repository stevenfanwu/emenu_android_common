package cn.buding.common.serverlog;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.view.View;
import cn.buding.common.location.CityHolder;
import cn.buding.common.location.ICity;
import cn.buding.common.location.Location;
import cn.buding.common.location.LocationHolder;
import cn.buding.common.net.NetUtil;
import cn.buding.common.util.PackageUtils;
import cn.buding.common.util.Utils;

/**
 * log user generated data. send to server as param2
 */
public class LogManager extends BaseLogManager<JSONObject> {
	private static final String TAG = "LogManager";

	private LogMap mLogMap;

	private LogSender mSender;

	protected LogManager(Context context, LogMap logmap) {
		this(context, logmap, false);
	}

	protected LogManager(Context context, LogMap logmap, boolean logException) {
		super(context);
		mLogMap = logmap;
		mSender = new LogSender(this);

		if (logException)
			detectException();
	}

	public LogMap getLogMap() {
		return mLogMap;
	}

	@Override
	protected String getLogCacheName() {
		return "pre_key_log_mamger_cache";
	}

	private void detectException() {
		Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				String trace = Log.getStackTraceString(ex);
				log("Exception", trace);
			}
		};
		Thread.setDefaultUncaughtExceptionHandler(handler);
	}

	/**
	 * add to the header before upload log to server.
	 */
	protected JSONObject getHeaderInfo() {
		try {
			JSONObject job = new JSONObject();
			job.put("logon_time", dateFormat.format(new Date()));
			job.put("platform", "android");
			job.put("imei", PackageUtils.getCustomIMEI(mContext));
			job.put("imsi", PackageUtils.getIMSI(mContext));
			String mac = NetUtil.getWifiMacAddress(mContext);
			if (Utils.notNullEmpty(mac))
				job.put("mac_address", mac);
			job.put("version_code", PackageUtils.getVersionCode(mContext));
			job.put("version_name", PackageUtils.getVersionName(mContext));
			job.put("channel", PackageUtils.getUmengChannel(mContext));
			job.put("app_name", mContext.getPackageName());
			job.put("is_wifi", NetUtil.isNetworkWifi(mContext) ? 1 : 0);

			ICity city = CityHolder.getInstance(mContext).getMSelectedCity();
			if (city != null)
				job.put("city_id", city.getId());
			Location loc = LocationHolder.getSingleton(mContext).getmLocation();
			if (loc != null) {
				job.put("latitude", loc.getLatitude());
				job.put("longitude", loc.getLongitude());
			}

			return job;
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return null;
		}
	}

	@Override
	public synchronized List<JSONObject> getUploadLogArray() {
		List<JSONObject> res = super.getUploadLogArray();
		JSONObject header = getHeaderInfo();
		if (header != null && res != null) {
			res.add(0, header);
		}
		return res;
	}

	/**
	 * For Activity/Fragment
	 * 
	 * @param c
	 *            the class of activity
	 */
	public void log(Class<?> c, Object data) {
		String name = mLogMap.getLogName(c);
		if (Utils.notNullEmpty(name))
			log(name, data);
	}

	/**
	 * for any clickable view
	 */
	public void log(View view) {
		Object data = mLogMap.getLogData(view);
		log(view, data);
	}

	/**
	 * for any clickable view with data.
	 */
	public void log(View view, Object data) {
		try {
			String name = mLogMap.getLogName(view);
			if (name != null) {
				log(name, data);
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Base log method
	 * 
	 * @param type
	 *            log type, a int value.
	 * @param name
	 *            log name.
	 * @param data
	 *            log data. is a jsonobject
	 */
	public void log(String name, Object data) {
		try {
			JSONObject object = new JSONObject();
			object.put("name", name);
			if (data != null) {
				if (data instanceof JSONObject) {
					JSONObject jsObjData = (JSONObject) data;
					Iterator<?> it = jsObjData.keys();
					while (it.hasNext()) {
						String key = (String) it.next();
						object.put(key, jsObjData.get(key));
					}
				} else
					object.put("data", data);
			}
			object.put("time", dateFormat.format(new Date()));
			addLog(object);
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
	}

	public void uploadFinish(boolean isSuccess) {
		if (isSuccess)
			logSuccess();
		else
			logFail();
	}

	@Override
	public String parseToString(JSONObject t) {
		if (t == null)
			return null;
		return t.toString();
	}

	@Override
	public JSONObject parseToObject(String s) {
		if (s == null)
			return null;
		try {
			return new JSONObject(s);
		} catch (JSONException e) {
			return null;
		}
	}
}
