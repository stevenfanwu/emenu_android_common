package cn.buding.common.location;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import cn.buding.common.R;
import cn.buding.common.file.FileUtil;

public class CityFactory implements ICityFactory {
	private List<City> mAllCities;
	private Context mContext;

	public CityFactory(Context context) {
		mContext = context;
	}

	private void initAllCities() {
		InputStream inStream =
				mContext.getResources().openRawResource(R.raw.citylist);
		String data = FileUtil.readFileContent(inStream);
		String[] lines = data.split("\r\n");
		if (mAllCities == null)
			mAllCities = new ArrayList<City>();
		for (String line : lines) {
			String[] items = line.split(",");
			int id = Integer.valueOf(items[0]);
			double longitude = Double.valueOf(items[3]);
			double latitude = Double.valueOf(items[4]);
			City c = buildCity(id, items[1]);
			c.setProvince(items[2]);
			c.setLongitude(longitude);
			c.setLatitude(latitude);
			// if the program exit , then allCityAreas is null. if the function is still running, we just simple return;
			if (mAllCities == null)
				return;
			mAllCities.add(c);
		}
	}

	@Override
	public synchronized List<City> getAllCities() {
		if (mAllCities == null)
			initAllCities();
		return mAllCities;
	}

	@Override
	public City getCity(String name) {
		if (name == null)
			return null;
		for (City c : getAllCities())
			if (c.getCity().equals(name))
				return c;
		return null;
	}

	@Override
	public City buildCity(int id, String name) {
		return new City(id, name);
	}

}
