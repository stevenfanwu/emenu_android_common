package cn.buding.common.location;

import java.util.HashMap;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import cn.buding.common.util.NTPTime;

/**
 * Location service try to update location from 3 service.<br/>
 * 1 GPS, {@link LocationManager#GPS_PROVIDER}.<br/>
 * 2 Google Service in Android, {@link LocationManager#NETWORK_PROVIDER}<br/>
 * 3 3rd part location service {@link Location#PROVIDER_SERVER} 4 custom
 * location provider. {@link Location#PROVIDER_GOOGLE}
 */
public class LocationService {
	private static final String TAG = "LocationService";
	private Context mContext;
	private CustomLocManager mCustomLocManager;
	private boolean mServiceStarted = false;
	private static final float MIN_UPDATE_DISTANCE_NORMAL = 100;
	private static final long MIN_UPDATE_DURATION_NORMAL = 2 * 60 * 1000;
	private static final float MIN_UPDATE_DISTANCE_INSTANT = 5;
	private static final long MIN_UPDATE_DURATION_INSTANT = 1 * 1000;

	private Handler mHandler;
	private HashMap<String, LocProvider> mProviders = new HashMap<String, LocProvider>();
	private boolean mLocateInstantly = false;

	private static final String NETWORK_PROVIDER = LocationManager.NETWORK_PROVIDER;
	private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
	private static final String CUSTOM_GOOGLE_PROVIDER = CustomLocManager.CUSTOM_GOOGLE_PROVIDER;

	private HashMap<String, LocationListener> mListenerMap = new HashMap<String, LocationListener>();

	private static LocationService mInstance;

	public static LocationService getSingleInstance(Context context) {
		if (mInstance == null) {
			mInstance = new LocationService(context);
		}
		return mInstance;
	}

	private LocationService(Context context) {
		mContext = context;
		mHandler = new Handler(context.getMainLooper());
		mCustomLocManager = new CustomLocManager(context);
	}

	/**
	 * init the 3 location service.
	 */
	public synchronized void startService(boolean locateInstantly) {
		if (mLocateInstantly != locateInstantly) {
			mLocateInstantly = locateInstantly;
			// restart service.
			if (mServiceStarted)
				stopService();
		}
		if (mServiceStarted)
			return;
		Log.i(TAG, "StartService");
		mServiceStarted = true;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					startGoogleLocService();
				} catch (Exception e) {
					Log.e(TAG, "", e);
				}
				try {
					startGpsService();
				} catch (Exception e) {
					Log.e(TAG, "", e);
				}
				try {
					startCustomLocService();
				} catch (Exception e) {
					Log.e(TAG, "", e);
				}
				try {
					startCustomProviders();
				} catch (Exception e) {
					Log.e(TAG, "", e);
				}
			}
		});
	}

	private long getMinDura() {
		return mLocateInstantly ? MIN_UPDATE_DURATION_INSTANT
				: MIN_UPDATE_DURATION_NORMAL;
	}

	private float getMinDis() {
		return mLocateInstantly ? MIN_UPDATE_DISTANCE_INSTANT
				: MIN_UPDATE_DISTANCE_NORMAL;
	}

	public synchronized void stopService() {
		if (!mServiceStarted)
			return;
		mServiceStarted = false;
		Log.i(TAG, "stopService");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				getLocationManager().removeUpdates(
						getLocListener(NETWORK_PROVIDER));
				getLocationManager()
						.removeUpdates(getLocListener(GPS_PROVIDER));
				mCustomLocManager
						.removeUpdates(getLocListener(CUSTOM_GOOGLE_PROVIDER));
				for (LocProvider provider : mProviders.values()) {
					String providerName = provider.getProviderName();
					if (providerName != null) {
						provider.removeUpdates(getLocListener(providerName));
					}
				}
			}
		});

	}

	public boolean isServiceStarted() {
		return mServiceStarted;
	}

	private LocationManager getLocationManager() {
		return (LocationManager) mContext
				.getSystemService(Context.LOCATION_SERVICE);
	}

	/**
	 * get last located location of best loc service.
	 */
	public Location getLastLocatedLocation() {
		try {
			LocationManager manager = getLocationManager();
			String provider = manager.getBestProvider(new Criteria(), true);
			android.location.Location loc = manager
					.getLastKnownLocation(provider);
			if (loc == null)
				return null;
			Location nloc = new Location(loc);
			transformLoc(nloc);
			return nloc;
		} catch (Exception e) {
			return null;
		}
	}

	private void onLocationUpdate(android.location.Location loc) {
		onLocationUpdate(new Location(loc));
	}

	/**
	 * try to converse location and set to memory.
	 */
	private void onLocationUpdate(Location loc) {
		Location oldLoc = new Location(loc);
		LocProvider provider = mProviders.get(loc.getProvider());
		if (provider == null || provider.needTransform()) {
			transformLoc(loc);
			Log.i(TAG, oldLoc.getLocStr() + " convert to " + loc.getLocStr());
		}
		LocationHolder.getSingleton(mContext).setmLocation(loc);
	}

	private void transformLoc(Location loc) {
		Transformer transformer = new Transformer();
		transformer.transform(loc);
	}

	private void startCustomLocService() {
		mCustomLocManager.requestLocationUpdates(CUSTOM_GOOGLE_PROVIDER,
				getMinDura(), getMinDis(),
				getLocListener(CUSTOM_GOOGLE_PROVIDER));
	}

	private void startCustomProviders() {
		for (LocProvider provider : mProviders.values()) {
			String providerName = provider.getProviderName();
			if (providerName != null) {
				provider.requestLocationUpdates(getMinDura(), getMinDis(),
						getLocListener(providerName));
			}
		}
	}

	private boolean startGoogleLocService() {
		LocationManager manager = getLocationManager();
		if (!manager.isProviderEnabled(NETWORK_PROVIDER))
			return false;
		manager.requestLocationUpdates(NETWORK_PROVIDER, getMinDura(),
				getMinDis(), getLocListener(NETWORK_PROVIDER));
		return true;
	}

	private boolean startGpsService() {
		LocationManager manager = getLocationManager();
		if (!manager.isProviderEnabled(GPS_PROVIDER))
			return false;
		manager.requestLocationUpdates(GPS_PROVIDER, getMinDura(), getMinDis(),
				getLocListener(GPS_PROVIDER));
		return true;
	}

	private class LocProviderListener implements LocationListener {
		private String mProvider;

		public LocProviderListener(String provider) {
			mProvider = provider;
		}

		@Override
		public void onLocationChanged(android.location.Location location) {
			location.setProvider(mProvider);
			if (location.getTime() == 0)
				location.setTime(NTPTime.currentTimeMillis());
			onLocationUpdate(location);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onProviderEnabled(String provider) {
			if (mServiceStarted) {
				if (NETWORK_PROVIDER.equals(provider)) {
					startGoogleLocService();
				} else if (GPS_PROVIDER.equals(provider)) {
					startGpsService();
				}
			}
		}

		@Override
		public void onProviderDisabled(String provider) {

		}

	}

	private LocationListener getLocListener(String provider) {
		if (provider == null)
			return null;
		LocationListener res = mListenerMap.get(provider);
		if (res == null) {
			res = new LocProviderListener(provider);
			mListenerMap.put(provider, res);
		}
		return res;
	}

	public void addProvider(LocProvider loc) {
		if (loc != null)
			mProviders.put(loc.getProviderName(), loc);
	}

	public static interface LocProvider {
		public String getProviderName();

		public void requestLocationUpdates(long minDura, float minDistance,
				LocationListener listener);

		public void removeUpdates(LocationListener listener);

		public boolean needTransform();
	}

}
