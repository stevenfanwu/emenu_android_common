package cn.buding.common.location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import cn.buding.common.location.IAddressFactory.OnAddressGetListener;
import cn.buding.common.location.google.GoogleAddress;
import cn.buding.common.location.google.GoogleAddressFactory.ReverseGecodeThread;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

@TargetApi(4)
public class NativeAddressFactory implements IAddressFactory {
	private static final String TAG = "NativeAddressFactory";
	private static NativeAddressFactory mInstance;

	public static NativeAddressFactory getSingleton(Context context) {
		if (mInstance == null)
			mInstance = new NativeAddressFactory(context);
		return mInstance;
	}

	private Context mContext;

	public NativeAddressFactory(Context context) {
		mContext = context;
	}

	@Override
	public void getAddress(Location loc, OnAddressGetListener callback) {
		new ReverseGecodeThread(mContext, loc, callback).start();
	}

	@Override
	public void getAddress(Location loc) {
		getAddress(loc, null);
	}

	private class ReverseGeocodingThread extends Thread {
		private Location mLoc;
		private OnAddressGetListener mListener;

		public ReverseGeocodingThread(Location loc,
				OnAddressGetListener listener) {
			mLoc = loc;
			mListener = listener;
		}

		@Override
		public void run() {
			Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
			List<Address> addresses = null;
			try {
				// Call the synchronous getFromLocation() method by passing in
				// the lat/long values.
				addresses = geocoder.getFromLocation(mLoc.getLatitude(),
						mLoc.getLongitude(), 1);
			} catch (IOException e) {
				Log.e(TAG, "", e);
			}
			AddressWraper addr = null;
			if (addresses != null && addresses.size() > 0) {
				Address address = addresses.get(0);
				addr = new AddressWraper(address);
			}

			if (mListener != null)
				mListener.onAddressGet(addr);
		}
	}

	public static class AddressWraper extends BaseAddress {
		private Address mAddress;

		public AddressWraper(Address add) {
			mAddress = add;
		}

		@Override
		public String getCityName() {
			return mAddress.getLocality();
		}

		@Override
		public String getDetailAddress() {
			return mAddress.getLocality() + mAddress.getSubLocality()
					+ mAddress.getThoroughfare();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			mAddress.writeToParcel(dest, flags);
		}

		public static final Parcelable.Creator<AddressWraper> CREATOR = new Parcelable.Creator<AddressWraper>() {
			public AddressWraper createFromParcel(Parcel in) {
				Address add = Address.CREATOR.createFromParcel(in);
				return new AddressWraper(add);
			}

			public AddressWraper[] newArray(int size) {
				return new AddressWraper[size];
			}
		};

	}
}
