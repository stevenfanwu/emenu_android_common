package cn.buding.common.location;

import android.content.Context;

public abstract class BaseAddress implements IAddress {
	public ICity getCity(Context context, String name) {
		return CityHolder.getInstance(context).getCityFactory().getCity(name);
	}
}
