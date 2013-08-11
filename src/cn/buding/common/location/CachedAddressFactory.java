package cn.buding.common.location;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import cn.buding.common.location.google.GoogleAddressFactory;
import cn.buding.common.util.ParcelHelper;

/**
 * a cache that hold the the address of your recent location.
 */
public class CachedAddressFactory implements IAddressFactory {
	private static final String TAG = "AddressHolder";
	private static CachedAddressFactory instance;
	public static final String PRE_KEY_ADDRESS_HOLDER =
			"pre_key_address_holder";
	private static final int MAX_LOC_COUNT = 15;
	private static final int MIN_DISTANCE_BETWEEN_LOCATION = 100;
	private List<Location> mCachedLocs;
	private Context mContext;
	private IAddressFactory mFactory;

	public synchronized static CachedAddressFactory getInstance(Context context) {
		if (instance == null)
			instance = new CachedAddressFactory(context);
		return instance;
	}

	private CachedAddressFactory(Context context) {
		this(context, GoogleAddressFactory.getSingleton(context));
	}

	private CachedAddressFactory(Context context, IAddressFactory factory) {
		mContext = context;
		mFactory = factory;
		init();
	}

	private void init() {
		mCachedLocs = new ArrayList<Location>();
		restoreCachedLocs();
		Log.i(TAG, "Address init, location in store: " + mCachedLocs.size());
	}

	private void restoreCachedLocs() {
		List<Location> storedLocs =
				ParcelHelper.getHelper(mContext).readArray(Location.class,
						PRE_KEY_ADDRESS_HOLDER);
		if (storedLocs != null) {
			for (Location l : storedLocs)
				if (l.isValid()) {
					mCachedLocs.add(l);
				}
		}
	}

	private void saveCachedLocs() {
		int end = Math.min(mCachedLocs.size(), MAX_LOC_COUNT);
		List<Location> locs = mCachedLocs.subList(0, end);
		ParcelHelper.getHelper(mContext).writeArray(PRE_KEY_ADDRESS_HOLDER,
				locs);
	}

	public void getAddress(final Location loc,
			final OnAddressGetListener listener) {
		if (loc == null)
			return;
		// find the nearest location in cache with the new location.
		double minD = Double.MAX_VALUE;
		Location minL = null;
		for (Location l : mCachedLocs) {
			double dis = l.distanceTo(loc);
			if (dis < minD) {
				minD = dis;
				minL = l;
			}
		}
		// if the nearest loc in cache is in 100m, we just set the cached address to the new location. or we will keep
		// the new loc in cache and try to reverse gecode
		if (minL != null && minD < MIN_DISTANCE_BETWEEN_LOCATION
				&& minL.getAddress() != null) {
			loc.setAddress(minL.getAddress());
			if (listener != null)
				listener.onAddressGet(minL.getAddress());
		} else {
			mCachedLocs.add(0, loc);
			mFactory.getAddress(loc, new OnAddressGetListener() {
				@Override
				public void onAddressGet(IAddress address) {
					saveCachedLocs();
					if (listener != null)
						listener.onAddressGet(address);
				}
			});
		}
	}

	@Override
	public void getAddress(Location loc) {
		getAddress(loc, null);
	}

}
