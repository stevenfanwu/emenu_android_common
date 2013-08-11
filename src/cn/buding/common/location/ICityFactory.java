package cn.buding.common.location;

import java.util.List;

public interface ICityFactory{
	public ICity getCity(String name);
	
	public ICity buildCity(int id, String name);
	
	public List<? extends ICity> getAllCities();
}
