package cn.buding.common.location;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * a data model for city. including the city name, city areas and which kinds of data are available in this city.
 */
public class City implements ICity {
	private static final long serialVersionUID = 1L;
	private int id;
	private String city;
	private String province;
	private double longitude;
	private double latitude;

	public City(int id, String city, String province) {
		this.id = id;
		this.city = city;
		this.province = province;
	}

	protected City(int id, String city, String province, double lati,
			double longi) {
		this.id = id;
		this.city = city;
		this.province = province;
		this.latitude = lati;
		this.longitude = longi;
	}

	protected City(City c) {
		this(c.id, c.city, c.province, c.latitude, c.longitude);
	}

	protected City(int id, String city) {
		this(id, city, null);
	}

	protected City(int id) {
		this(id, null);
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public String getCity() {
		return city;
	}

	public int getId() {
		return id;
	}

	public String getProvince() {
		return province;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	@Override
	public String toString() {
		return city;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof City)
			return ((City) o).getId() == id;
		return false;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(city);
		dest.writeString(province);
		dest.writeDouble(longitude);
		dest.writeDouble(latitude);
	}

	public static final Parcelable.Creator<City> CREATOR =
			new Parcelable.Creator<City>() {

				@Override
				public City createFromParcel(Parcel src) {
					int id = src.readInt();
					City city = new City(id);
					city.city = src.readString();
					city.province = src.readString();
					city.longitude = src.readDouble();
					city.latitude = src.readDouble();
					return city;
				}

				@Override
				public City[] newArray(int size) {
					return new City[size];
				}

			};

}