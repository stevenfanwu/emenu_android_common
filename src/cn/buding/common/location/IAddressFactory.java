package cn.buding.common.location;

/** address factory to get address for input locaton */
public interface IAddressFactory {
	public void getAddress(Location loc, OnAddressGetListener callback);

	public void getAddress(Location loc);

	public static interface OnAddressGetListener {
		public void onAddressGet(IAddress address);
	}
}
