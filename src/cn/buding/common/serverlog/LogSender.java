package cn.buding.common.serverlog;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;
import cn.buding.common.exception.CustomException;
import cn.buding.common.json.JSONBean;
import cn.buding.common.net.BaseHttpsManager;
import cn.buding.common.net.NetUtil;
import cn.buding.common.net.BaseHttpsManager.ApiRequestParam;
import cn.buding.common.net.BaseHttpsManager.RequestParam;
import cn.buding.common.net.ServerApi;
import cn.buding.common.util.Base64;
import cn.buding.common.util.CodecUtils;

public class LogSender extends AbsLogSender<JSONObject> {
	private static final String TAG = "LogSender";
	public static final ServerApi SEND_SERVER_LOG = new ServerApi(
			"http://log.2000tuan.com", "/app_log/set_user_daily_log.php",
			JSendLogResponse.class);

	protected LogSender(LogManager logManager) {
		super(logManager);
		setSendMode(MODE_SEND_ON_INIT | MODE_SEND_ON_LOG_COUNT_ENOUGH);
	}

	@Override
	protected boolean sendLogToServer(List<JSONObject> logs) {
		if (logs == null || logs.size() == 0)
			return false;
		JSONArray array = new JSONArray(logs);
		String log = array.toString();
		if (log == null || log.length() == 0)
			return false;
		try {
			JSendLogResponse res = sendServerLog(log);
			if (res != null)
				return true;
		} catch (CustomException e) {
			Log.e(TAG, "", e);
		}
		return false;
	}

	private JSendLogResponse sendServerLog(String log) throws CustomException {
		ApiRequestParam param = new ApiRequestParam(SEND_SERVER_LOG);
		param.setHttpMethod(RequestParam.METHOD_POST);
		param.addNameValuePair("user_log", compress(log));
		param.addNameValuePair("iscompress", "1");
		return BaseHttpsManager.processApi(param);
	}

	private String compress(String log) {
		if (log == null)
			return null;
		byte[] compressed = NetUtil.deflateCompress(log.getBytes());
		return Base64.encodeBase64String(compressed);
	}

	public static class JSendLogResponse implements JSONBean {
		public int flag;
		public String message;
	}

}
