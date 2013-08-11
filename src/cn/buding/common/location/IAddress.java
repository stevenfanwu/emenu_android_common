package cn.buding.common.location;

import java.io.Serializable;

import android.os.Parcelable;

/**
 * address interface.
 */
public interface IAddress extends Parcelable, Serializable {
	public String getCityName();

	/** @return the detail address. contain city area district and street */
	public String getDetailAddress();
}
