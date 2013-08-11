package cn.buding.common.location;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import cn.buding.common.location.IAddressFactory.OnAddressGetListener;
import cn.buding.common.util.ParcelHelper;

public class LocationHolder {
	private static final String TAG = "LocationHolder";
	private static LocationHolder mInstance;
	public static final String PRE_KEY_LAST_LOCATION = "pre_key_last_location";

	public static synchronized LocationHolder getSingleton(Context context) {
		if (mInstance == null)
			mInstance = new LocationHolder(context);
		return mInstance;
	}

	private LocationService mService;
	private Context mContext;
	private Handler mHandler;
	private Location mLocation = null;
	private List<OnLocationChangedListener> mOnLocationChangedListeners;
	private List<OnAddressChangedListener> mOnAddressChangedListeners;

	private IAddressFactory mAddressFactory;

	private boolean mStopServiceOnLocated = true;

	private static final long STOP_SERVICE_DELAY_TIME = 5 * 1000;

	private static final long RESTART_SERVICE_INTERVAL = 4 * 60 * 1000;

	private static final long LOCATION_VALID_TIME = 20 * 60 * 1000;

	private LocationHolder(Context context) {
		mContext = context.getApplicationContext();
		mHandler = new Handler(mContext.getMainLooper());
		mService = LocationService.getSingleInstance(context);
		mAddressFactory = CachedAddressFactory.getInstance(context);
		mOnLocationChangedListeners = new ArrayList<OnLocationChangedListener>();
		mOnAddressChangedListeners = new ArrayList<OnAddressChangedListener>();
		initLocation();
	}

	public LocationService getService() {
		return mService;
	}

	public void setStopServiceOnLocated(boolean b) {
		mStopServiceOnLocated = b;
	}

	private void checkStopService() {
		if (mStopServiceOnLocated) {
			mHandler.removeCallbacks(mStopServiceRunnable);
			mHandler.postDelayed(mStopServiceRunnable, STOP_SERVICE_DELAY_TIME);
		}
	}

	private Runnable mStopServiceRunnable = new Runnable() {
		public void run() {
			mService.stopService();
		};
	};

	public void init() {
		checkLocationService();
	}

	public void onDestroy() {
		mService.stopService();
	}

	private void initLocation() {
		Location loc = getLatestLocation();
		if (loc == null)
			loc = mService.getLastLocatedLocation();
		if (isLocValid(loc)) {
			setmLocation(loc);
		}
		checkLocationService();
	}

	public synchronized void setmLocation(final Location newLocation) {
		if (newLocation == null || !newLocation.isValid())
			return;
		// new location could replace the old location only if it is more
		// accurate, newer .
		if (Location.isBetterLocation(newLocation, mLocation)) {
			final Location oldLocation = mLocation;
			mAddressFactory.getAddress(newLocation, new OnAddressGetListener() {
				@Override
				public void onAddressGet(IAddress address) {
					onAddressChanged(oldLocation, newLocation);
				}
			});
			mLocation = newLocation;
			saveLatestLocation(newLocation);
			for (OnLocationChangedListener l : mOnLocationChangedListeners)
				l.onLocationChanged(newLocation);
			checkStopService();
		}

		Log.i(TAG, "New loc: " + newLocation.toString()
				+ ". Location changed: " + (mLocation == newLocation));
	}

	private boolean isLocValid(Location loc) {
		return loc != null && loc.isValid()
				&& loc.isTimeWithin(LOCATION_VALID_TIME);
	}

	public Location getmLocation() {
		checkLocationService();
		if (isLocValid(mLocation))
			return mLocation;
		return null;
	}

	private void checkLocationService() {
		if (!isLocValid(mLocation)
				|| !mLocation.isTimeWithin(RESTART_SERVICE_INTERVAL)) {
			// XXX: forse to locate.
			mService.startService(true);
		}
	}

	public String getCityName() {
		Location loc = getmLocation();
		if (loc == null)
			return null;
		return loc.getCityName();
	}

	private void saveLatestLocation(Location location) {
		ParcelHelper.getHelper(mContext).writeObject(PRE_KEY_LAST_LOCATION,
				location);
		if (location.getAddress() != null) {
			ICity city = CityHolder.getInstance(mContext).getCityFactory()
					.getCity(location.getCityName());
			if (city != null)
				CityHolder.getInstance(mContext).saveLastLocatedCity(city);
		}
	}

	public synchronized Location getLatestLocation() {
		Location loc = ParcelHelper.getHelper(mContext).readObject(
				Location.class, PRE_KEY_LAST_LOCATION);
		if (loc != null && loc.isValid()) {
			Log.i(TAG, "Get last location: " + loc.toString());
			return loc;
		}
		return null;
	}

	public void addOnLocationChangedListener(OnLocationChangedListener l) {
		checkLocationService();
		if (!mOnLocationChangedListeners.contains(l))
			mOnLocationChangedListeners.add(l);
	}

	public void removeOnLocationChangedListener(OnLocationChangedListener l) {
		mOnLocationChangedListeners.remove(l);
	}

	/**
	 * use the newLoc and oldLoc to determine whether the city and the address
	 * is changed.
	 */
	private void onAddressChanged(Location oldLoc, Location newLoc) {
		saveLatestLocation(newLoc);
		String oldCity = oldLoc != null ? oldLoc.getCityName() : null;
		String newCity = newLoc.getCityName();
		boolean cityChanged = oldCity == null || !oldCity.equals(newCity);
		IAddress oldAddress = oldLoc != null ? oldLoc.getAddress() : null;
		IAddress newAddress = newLoc.getAddress();
		boolean addressChanged = oldAddress == null
				|| !oldAddress.equals(newAddress);
		for (OnAddressChangedListener l : mOnAddressChangedListeners)
			l.onAddressChanged(addressChanged, cityChanged);

	}

	public void addOnAddressChangedListener(OnAddressChangedListener l) {
		if (!mOnAddressChangedListeners.contains(l))
			mOnAddressChangedListeners.add(l);
	}

	public void removeOnAddressChangedListener(OnAddressChangedListener l) {
		mOnAddressChangedListeners.remove(l);
	}

	public interface OnAddressChangedListener {
		public void onAddressChanged(boolean addressChanged, boolean cityChanged);
	}

	public interface OnLocationChangedListener {
		public void onLocationChanged(Location newLoc);
	}
}
