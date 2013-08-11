package cn.buding.common.location;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import cn.buding.common.util.ParcelHelper;
import cn.buding.common.util.PreferenceHelper;

public class CityHolder {
	private static CityHolder mInstance;

	public static CityHolder getInstance(Context context) {
		if (mInstance == null)
			mInstance = new CityHolder(context, new CityFactory(context));
		return mInstance;
	}

	private static final String PRE_KEY_LAST_SELECT_CITY = "pre_key_last_select_city";
	private ICity mSelectedCity;
	private ICityFactory mCityFactory;
	private Context mContext;
	public static final String ACTION_SELECT_CITY_CHANGED = "action.buding.selected_city_changed";
	public static final String EXTRA_LAST_SELECTED_CITY = "extra_last_selected_city";
	public static final String EXTRA_CUR_SELECTED_CITY = "extra_cur_selected_city";

	public CityHolder(Context context, ICityFactory cityFactory) {
		mCityFactory = cityFactory;
		mContext = context.getApplicationContext();
	}

	public ICity getMSelectedCity() {
		if (mSelectedCity == null) {
			String city = PreferenceHelper.getHelper(mContext).readPreference(
					PRE_KEY_LAST_SELECT_CITY);
			mSelectedCity = mCityFactory.getCity(city);
			if (mSelectedCity == null)
				mSelectedCity = getMLocatedCity();
		}
		return mSelectedCity;
	}

	public void setMSelectedCity(ICity city) {
		if (city == null)
			return;
		PreferenceHelper.getHelper(mContext).writePreference(
				PRE_KEY_LAST_SELECT_CITY, city.getCity());
		ICity lastCity = mSelectedCity;
		mSelectedCity = city;
		if (mSelectedCity != null && !mSelectedCity.equals(lastCity)) {
			Intent intent = new Intent(ACTION_SELECT_CITY_CHANGED);
			if (lastCity != null)
				intent.putExtra(EXTRA_LAST_SELECTED_CITY, lastCity);
			intent.putExtra(EXTRA_CUR_SELECTED_CITY, mSelectedCity);
			mContext.sendBroadcast(intent);
		}
	}

	public ICityFactory getCityFactory() {
		return mCityFactory;
	}

	public ICity getMLocatedCity() {
		String name = LocationHolder.getSingleton(mContext).getCityName();
		return mCityFactory.getCity(name);
	}

	private static final String PRE_KEY_LAST_LOCAETED_CITY = "pre_key_last_located_city";

	protected void saveLastLocatedCity(ICity city) {
		ParcelHelper.getHelper(mContext).writeObject(
				PRE_KEY_LAST_LOCAETED_CITY, city);
	}

	public ICity getLastLocatedCity() {
		return ParcelHelper.getHelper(mContext).readObject(City.class,
				PRE_KEY_LAST_LOCAETED_CITY);
	}

	private static final String DEFAULT_CITY = "北京";

	/**
	 * getMLocatedCity() -> getMSelectedCity() -> get a default city
	 * 
	 * @return
	 */
	public ICity getCurrentDefaultCity() {
		ICity city = getMLocatedCity();
		if (city == null)
			city = getMSelectedCity();
		if (city == null)
			getCityFactory().getCity(DEFAULT_CITY);
		return city;
	}

	/**
	 * @return Whether located city is equals to selected city.
	 */
	public boolean isLocatedEqualSelected() {
		ICity locatedCity = getMLocatedCity();
		ICity selectedCity = getMSelectedCity();
		return locatedCity != null && locatedCity.equals(selectedCity);
	}

	/**
	 * @return located city exists but not equal to selected city.
	 */
	public boolean isExistLocatedNESelected() {
		ICity locatedCity = getMLocatedCity();
		ICity selectedCity = getMSelectedCity();
		return locatedCity != null && !locatedCity.equals(selectedCity);
	}

}
