package cn.buding.common.location.google;

import android.content.Context;
import android.util.Log;
import cn.buding.common.location.IAddress;
import cn.buding.common.location.IAddressFactory;
import cn.buding.common.location.Location;
import cn.buding.common.net.BaseHttpsManager;

public class GoogleAddressFactory implements IAddressFactory {
	private static final String TAG = "GoogleAddressFactory";
	private static GoogleAddressFactory mInstance;

	public static GoogleAddressFactory getSingleton(Context context) {
		if (mInstance == null)
			mInstance = new GoogleAddressFactory(context);
		return mInstance;
	}

	private Context mContext;

	public GoogleAddressFactory(Context context) {
		mContext = context;
	}

	@Override
	public void getAddress(Location loc) {
		getAddress(loc, null);
	}

	@Override
	public void getAddress(Location loc, OnAddressGetListener callback) {
		new ReverseGecodeThread(mContext, loc, callback).start();
	}

	public static class ReverseGecodeThread extends Thread {
		public static final String GOOGLE_REVERSE_GECODE_API =
				"http://maps.googleapis.com/maps/api/geocode/json?sensor=true&region=cn&language=zh-CN&latlng=";
		private Context mContext;
		private Location mLoc;
		private OnAddressGetListener mListener;

		public ReverseGecodeThread(Context context, Location loc,
				OnAddressGetListener listener) {
			this.mContext = context;
			this.mLoc = loc;
			mListener = listener;
		}

		@Override
		public void run() {
			IAddress mAddress = null;
			try {
				String api =
						GOOGLE_REVERSE_GECODE_API + mLoc.getLatitude() + ","
								+ mLoc.getLongitude();
				BaseHttpsManager.init(mContext);
				String result = BaseHttpsManager.executeGetRequest(api);
				JGoogleAddress jAddress = JGoogleAddress.parse(result);
				if (jAddress != null) {
					mAddress = new GoogleAddress(jAddress);
				}
				if (mAddress != null) {
					Log.i(TAG, "Address Geted: " + mAddress.toString() + " "
							+ mLoc.toString());
					mLoc.setAddress(mAddress);
				}
			} catch (Exception e) {
				Log.e(TAG, "", e);
			}
			if (mListener != null)
				mListener.onAddressGet(mAddress);
		}
	}

}
