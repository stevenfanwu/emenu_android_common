package cn.buding.common.location;

import java.io.Serializable;

import android.os.Parcelable;

public interface ICity extends Parcelable{
	/** return city name */
	public String getCity();

	public int getId();
}
