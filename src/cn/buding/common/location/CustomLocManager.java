package cn.buding.common.location;

import java.io.IOException;
import java.lang.Thread.State;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationListener;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import cn.buding.common.location.google.WifiTelEntity.TelInfo;
import cn.buding.common.location.google.WifiTelToLoc;
import cn.buding.common.net.BaseHttpsManager;

/**
 * custom location service. listen to the wifi/tele change and try to located by
 * tele/wifi info.
 */
@TargetApi(3)
public class CustomLocManager {
	private static final String TAG = "CustomLocManager";
	public static final String CUSTOM_GOOGLE_PROVIDER = "custom_google_provider";

	/** we set cdma type here since android 1.5 sdk do not contain this value. */
	public static int PHONE_TYPE_CDMA = 2;
	private List<WifiInfo> mWifiInfos = new ArrayList<WifiInfo>();

	private Context mContext;
	private Map<LocationListener, String> mListeners = new HashMap<LocationListener, String>();
	/** the min time duration between two request. */
	private long mMinTimeInterval = 2 * 60 * 1000;
	/** the min distance to update locate, doesn't used yet */
	private float mMinDistance = 0;

	/** listen to the wifi state changed */
	private BroadcastReceiver mWifiStateReceiver;
	/** listen to the cell info changed */
	private PhoneStateListener mCellListener;
	/** listen to wifi scan result */
	private BroadcastReceiver mWifiResultReceiver;

	protected CustomLocManager(Context context) {
		mContext = context.getApplicationContext();
	}

	public void setMinTimeInterval(long time) {
		mMinTimeInterval = time;
	}

	public void requestLocationUpdates(String provider, long minTime,
			float minDistance, LocationListener listener) {
		mMinTimeInterval = minTime;
		mMinDistance = minDistance;
		mLastExecuteTime = 0;
		if (mListeners.size() == 0)
			registerReceivers();
		mListeners.put(listener, provider);
	}

	public void removeUpdates(LocationListener listener) {
		mListeners.remove(listener);
		if (mListeners.size() == 0)
			unregisterReceivers();
	}

	private void registerReceivers() {
		try {
			registerTelInfo();
			registerWifiInfo();
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
	}

	private void unregisterReceivers() {
		try {
			if (mCellListener != null)
				getTeleManager().listen(mCellListener,
						PhoneStateListener.LISTEN_NONE);
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		try {
			if (mWifiResultReceiver != null)
				mContext.unregisterReceiver(mWifiResultReceiver);
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		try {
			if (mWifiStateReceiver != null)
				mContext.unregisterReceiver(mWifiStateReceiver);
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
	}

	/**
	 * register cellListener for cell location change
	 */
	private void registerTelInfo() {
		if (mCellListener == null)
			mCellListener = new PhoneStateListener() {
				public void onCellLocationChanged(CellLocation location) {
					if (onTelInfoChanged())
						onWifiTelStateChanged();
				}
			};
		getTeleManager().listen(mCellListener,
				PhoneStateListener.LISTEN_CELL_LOCATION);
	}

	private TelephonyManager getTeleManager() {
		return (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
	}

	/**
	 * register a listener for wifi scan result, and restore wifiinfo in memory.
	 * register a listener for wifi state change, and check if the connected
	 * wifi is changed.
	 */
	private void registerWifiInfo() {
		WifiManager wifimanager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo connWifiInfo = new WifiInfo(wifimanager.getConnectionInfo());
		if (connWifiInfo.isValid() && !mWifiInfos.contains(connWifiInfo))
			mWifiInfos.add(connWifiInfo);
		if (mWifiResultReceiver == null)
			mWifiResultReceiver = new BroadcastReceiver() {
				/** max wifi count restored in mWifiinfos */
				private final int MAX_WIFI_COUNT = 5;

				public void onReceive(Context c, Intent intent) {
					WifiManager wifimanager = (WifiManager) c
							.getSystemService(Context.WIFI_SERVICE);
					List<ScanResult> wifis = wifimanager.getScanResults();
					if (null == wifis)
						return;
					// order by level desc
					Collections.sort(wifis, new Comparator<ScanResult>() {
						@Override
						public int compare(ScanResult object1,
								ScanResult object2) {
							return object2.level - object1.level;
						}
					});
					mWifiInfos.clear();
					int len = Math.min(wifis.size(), MAX_WIFI_COUNT);
					for (int i = 0; i < len; i++) {
						WifiInfo info = new WifiInfo(wifis.get(i));
						if (info.isValid())
							mWifiInfos.add(info);
					}
				}
			};
		mContext.registerReceiver(mWifiResultReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		if (mWifiStateReceiver == null)
			mWifiStateReceiver = new BroadcastReceiver() {
				private String mConnectedBSSID;

				@Override
				public void onReceive(Context context, Intent intent) {
					// if connected wifi is changed.
					String bssid = intent
							.getStringExtra(WifiManager.EXTRA_BSSID);
					if (bssid != null && !bssid.equals(mConnectedBSSID)) {
						mConnectedBSSID = bssid;
						onWifiTelStateChanged();
					}
				}
			};
		mContext.registerReceiver(mWifiStateReceiver, new IntentFilter(
				WifiManager.NETWORK_STATE_CHANGED_ACTION));
	}

	private long mLastExecuteTime;

	private GetGoogleLocationThread mGetGoogleLocationThread;

	/**
	 * use wifi and tele info to invoke {@link GetGoogleLocationThread}. time
	 * duration between two request can not within {@link #mMinTimeInterval}
	 */
	private void onWifiTelStateChanged() {
		long timeDiffer = SystemClock.elapsedRealtime() - mLastExecuteTime;
		if (timeDiffer < mMinTimeInterval)
			return;
		mLastExecuteTime = SystemClock.elapsedRealtime();
		if (mListeners.values().contains(CUSTOM_GOOGLE_PROVIDER)) {
			loadFromGoogle();
		}
	}

	private void loadFromGoogle() {
		if (mGetGoogleLocationThread == null
				|| mGetGoogleLocationThread.getState() == State.TERMINATED) {
			mGetGoogleLocationThread = new GetGoogleLocationThread();
			mGetGoogleLocationThread.setOnLocatedListener(mOnLocatedListener);
			mGetGoogleLocationThread.start();
		}
	}

	private OnLocatedListener mOnLocatedListener = new OnLocatedListener() {
		public void onLocated(String provider, Location loc) {
			if (provider == null)
				return;
			for (Entry<LocationListener, String> e : mListeners.entrySet()) {
				if (provider.equals(e.getValue())) {
					e.getKey().onLocationChanged(loc);
				}
			}
		};
	};

	/** connected tele info in gsm */
	private GsmInfo gsmInfo;
	/** connected tele info in cdma */
	private CDMAInfo cdmaInfo;

	/**
	 * collected tele info on tele changed.
	 * 
	 * @return whether the current tele location is different.
	 */
	private boolean onTelInfoChanged() {
		boolean telChanged = false;
		TelephonyManager telManager = getTeleManager();
		CellLocation location = telManager.getCellLocation();
		if (location == null)
			return false;
		try {
			if (telManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
				GsmCellLocation gsmL = (GsmCellLocation) location;
				telChanged = gsmInfo == null || gsmInfo.cid != gsmL.getCid();
				gsmInfo = new GsmInfo();
				gsmInfo.cid = gsmL.getCid();
				gsmInfo.lac = gsmL.getLac();
			} else if (telManager.getPhoneType() == PHONE_TYPE_CDMA) {
				Class<?> cdma = Class
						.forName("android.telephony.cdma.CdmaCellLocation");
				int bid = getMethodResult(cdma, "getBaseStationId", location);
				telChanged = cdmaInfo == null || cdmaInfo.bid != bid;
				cdmaInfo = new CDMAInfo();
				cdmaInfo.bid = bid;
				cdmaInfo.bid = getMethodResult(cdma, "getNetworkId", location);
				cdmaInfo.sid = getMethodResult(cdma, "getSystemId", location);
			}
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return false;
		}
		return telChanged;
	}

	private int getMethodResult(Class<?> cla, String methodName,
			CellLocation location) throws Exception {
		Method method = cla
				.getMethod(methodName, new Class<?>[] { Void.class });
		return Integer.valueOf(method.invoke(location, new Object[] {})
				.toString());
	}

	public class GsmInfo {
		public int lac;
		public int cid;
	}

	public class CDMAInfo {
		public int bid;
		public int nid;
		public int sid;
	}

	public class GetGoogleLocationThread extends Thread {
		private String mccmnc;
		private int phoneType;
		private List<NeighboringCellInfo> neighbors;
		private OnLocatedListener mListener;

		public GetGoogleLocationThread() {
			TelephonyManager telManager = getTeleManager();
			mccmnc = telManager.getNetworkOperator();
			phoneType = telManager.getPhoneType();
			neighbors = telManager.getNeighboringCellInfo();
		}

		public GetGoogleLocationThread setOnLocatedListener(OnLocatedListener l) {
			mListener = l;
			return this;
		}

		@Override
		public void run() {
			Location loc = locateFromGoogle();
			if (loc != null && mListener != null) {
				mListener.onLocated(CUSTOM_GOOGLE_PROVIDER, loc);
			}
		}

		public static final String MCC_CHINA = "460";
		public static final String MNC_MOBILE = "00";

		private static final int MAX_RETRY_TIME = 3;
		private static final long START_SLEEP_TIME = 2000;

		private Location locateFromGoogle() {

			long sleepTime = START_SLEEP_TIME;
			for (int time = 0; time < MAX_RETRY_TIME; time++) {
				try {
					String mcc = MCC_CHINA;
					String mnc = MNC_MOBILE;
					int cid = 0;
					int lac = 0;
					if (mccmnc != null && mccmnc.length() == 5) {
						mcc = mccmnc.substring(0, 3);
						mnc = mccmnc.substring(3);
					}
					if (phoneType == TelephonyManager.PHONE_TYPE_GSM
							&& gsmInfo != null) {
						cid = gsmInfo.cid;
						lac = gsmInfo.lac;
					} else if (phoneType == CustomLocManager.PHONE_TYPE_CDMA
							&& cdmaInfo != null) {
						mnc = "" + cdmaInfo.sid;
						cid = cdmaInfo.bid;
						lac = cdmaInfo.nid;
					}
					List<TelInfo> telInfos = new ArrayList<TelInfo>();
					for (NeighboringCellInfo info : neighbors) {
						int i = info.getCid();
						int l = getLac(info);
						TelInfo t = new TelInfo(i, l);
						telInfos.add(t);
					}
					// WifiTetToLoc need http request.
					BaseHttpsManager.init(mContext);
					WifiTelToLoc loc = new WifiTelToLoc(mcc, mnc, cid, lac,
							telInfos, mWifiInfos);
					Location res = loc.locate();
					if (res != null)
						res.setProvider(CUSTOM_GOOGLE_PROVIDER);
					return res;
				} catch (IOException e) {
					Log.e(TAG, "", e);
					try {
						sleep(sleepTime);
					} catch (InterruptedException e1) {
					}
					sleepTime *= 2;
				} catch (Exception e) {
					Log.e(TAG, "", e);
					break;
				}
			}
			return null;
		}

		private int getLac(NeighboringCellInfo nei) {
			if (Integer.valueOf(android.os.Build.VERSION.SDK) > 3) {
				try {
					Method m = NeighboringCellInfo.class.getMethod("getLac",
							new Class[] {});
					m.invoke(nei);
				} catch (Exception e) {
				}
			}
			return 0;
		}
	}

	interface OnLocatedListener {
		public void onLocated(String provider, Location loc);
	}

}
