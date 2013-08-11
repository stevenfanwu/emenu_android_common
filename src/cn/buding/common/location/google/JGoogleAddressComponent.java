package cn.buding.common.location.google;

import cn.buding.common.json.JSONBean;

public class JGoogleAddressComponent implements JSONBean {
	private static final long serialVersionUID = 1L;
	public String long_name;
	public String[] types;

	public String getFirstType() {
		if (types != null && types.length > 0)
			return types[0];
		return null;
	}
}
