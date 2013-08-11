package cn.buding.common.location.google;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.json.JSONObject;

import android.util.Log;
import cn.buding.common.location.Location;
import cn.buding.common.location.WifiInfo;
import cn.buding.common.location.google.WifiTelEntity.TelInfo;
import cn.buding.common.net.BaseHttpsManager;

/**
 * use wifi and tel info to request location.
 */
public class WifiTelToLoc {
	private static final String TAG = "WifiTelToLoc";
	private int cell_id;
	private int lac_id;
	private String mnc;
	private String mcc;
	private List<TelInfo> mTelInfos;
	private List<WifiInfo> mWifis;

	public WifiTelToLoc(String mcc, String mnc, int aCellID, int aLAC,
			List<TelInfo> telInfos, List<WifiInfo> wifis) {
		this.cell_id = aCellID;
		this.lac_id = aLAC;
		this.mnc = mnc;
		this.mcc = mcc;
		mTelInfos = telInfos;
		mWifis = wifis;
	}

	public Location locate() throws IOException {
		String response = "";
		try {
			String baseURL = "http://www.google.com/loc/json";
			HttpEntity entity = new WifiTelEntity(mcc, mnc, cell_id, lac_id,
					mTelInfos, mWifis);
			response = BaseHttpsManager.executePostRequest(baseURL, entity);
			if(response == null)
				return null;
			JSONObject jo = new JSONObject(response);
			if (jo.has("location")) {
				JSONObject jofile = jo.getJSONObject("location");
				double latitude = jofile.getDouble("latitude");
				double longitude = jofile.getDouble("longitude");
				double accuracy = jofile.getDouble("accuracy");
				Location loc = new Location(longitude, latitude);
				loc.setAccuracy((float) accuracy);
				return loc;
			}
			return null;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			Log.e(TAG, response, e);
			return null;
		}
	}

	public static final String MCC_CHINA = "460";

	public static void main(String[] args) {
		List<TelInfo> list = new ArrayList<TelInfo>();
		List<WifiInfo> wList = new ArrayList<WifiInfo>();
		// wList.add(new WifiInfo("00:3a:98:ee:f6:c1", -53));
		WifiTelToLoc cto = new WifiTelToLoc("460", "00", 25395, 4569, list,
				wList);
		// cto.locate();
	}
}
