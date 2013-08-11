package cn.buding.common.asynctask;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import cn.buding.common.exception.CustomException;
import cn.buding.common.exception.ECode;

import android.util.Log;
import cn.buding.common.widget.MyToast;

/**
 * handle error and success messages. the {@link #doInBackground(Void...)} will
 * return a value in {@link ECode}, we will show correspond error messages for
 * the returned error code.
 */
public abstract class HandlerMessageTask extends BaseTask<Void, Object, Object> {
	private static final String TAG = "HandlerMessageTask";
	private boolean showCodeMsg = true;
	protected String codeMsg = null;
	private Map<Integer, Object> codeMsgs = new HashMap<Integer, Object>();
	private static Map<Integer, Object> defaultCodeMsgs;

	protected Callback mCallback;
	private boolean interrupted = false;

	static {
		defaultCodeMsgs = new HashMap<Integer, Object>();
		defaultCodeMsgs.put(ECode.SUCCESS, "");
		defaultCodeMsgs.put(ECode.SUCCESS_LAST_TIME, "");
		defaultCodeMsgs.put(ECode.SERVER_RETURN_EMPTY_SET, "到头了");
		defaultCodeMsgs.put(ECode.SERVER_RETURN_EMPTY_SET_FIRST_TIME, "暂时没有数据");
		defaultCodeMsgs.put(ECode.FAIL, "网络连接失败，请稍候重试");
		defaultCodeMsgs.put(ECode.CANNOT_LOCATE, "暂时无法定位");
	}

	public HandlerMessageTask(Context context) {
		super(context);
	}

	public static void setDefaultCodeMsg(int code, Object value) {
		defaultCodeMsgs.put(code, value);
	}

	public Object runBackground() {
		return doInBackground();
	}

	public void postExecute(Object result) {
		onPostExecute(result);
	}

	@Override
	protected void onPostExecute(Object result) {
		processResult(result);
		super.onPostExecute(result);
	}

	protected void processResult(Object result) {
		int code = 0;
		if (result == null)
			result = ECode.FAIL;
		if (result.equals(ECode.CANCELED)) {
			return;
		} else if (result instanceof CustomException) {
			CustomException e = (CustomException) result;
			code = e.getCode();
		} else if (result instanceof Integer) {
			code = (Integer) result;
		} else {
			code = ECode.FAIL;
		}

		codeMsg = getCodeMsg(code);
		if (codeMsg == null)
			codeMsg = getCodeMsg(ECode.FAIL);
		if (showCodeMsg && codeMsg != null && codeMsg.length() > 0)
			showResultMessage(codeMsg);
		if (code == ECode.SUCCESS || code == ECode.SUCCESS_LAST_TIME) {
			if (mCallback != null)
				mCallback.onSuccess(this, code);
		} else {
			if (codeMsg == null)
				Log.e(TAG, "Error code message should not be null." + code
						+ " " + this.getClass().getSimpleName());
			if (mCallback != null)
				mCallback.onFail(this, code);
		}
	}

	protected void showResultMessage(String codeMsg) {
		MyToast.makeText(mContext, codeMsg).show();
	}

	private String getDefaultCodeMsg(int code) {
		Object o = defaultCodeMsgs.get(code);
		return convertCodeMsg(o);
	}

	public String getCodeMsg(int code) {
		Object o = codeMsgs.get(code);
		if (o != null)
			return convertCodeMsg(o);
		else
			return getDefaultCodeMsg(code);
	}

	private String convertCodeMsg(Object o) {
		if (o == null)
			return null;
		if (o instanceof String)
			return (String) o;
		if (o instanceof Integer)
			return mContext.getResources().getString((Integer) o);

		return null;
	}

	public void setShowCodeMsg(boolean b) {
		showCodeMsg = b;
	}

	public void setCodeMsg(Integer code, String msg) {
		codeMsgs.put(code, msg);
	}

	public void setCodeMsg(Integer code, int res) {
		codeMsgs.put(code, res);
	}

	public void disableCodeMsg(Integer code) {
		codeMsgs.put(code, "");
	}

	public HandlerMessageTask setCallback(Callback callback) {
		mCallback = callback;
		return this;
	}

	public HandlerMessageTask wrapCallback(final Callback newCall) {
		final Callback oldCall = mCallback;
		mCallback = new Callback() {
			@Override
			public void onSuccess(HandlerMessageTask task, Object t) {
				if (oldCall != null)
					oldCall.onSuccess(task, t);
				if (newCall != null)
					newCall.onSuccess(task, t);

			}

			@Override
			public void onFail(HandlerMessageTask task, Object t) {
				if (oldCall != null)
					oldCall.onFail(task, t);
				if (newCall != null)
					newCall.onFail(task, t);

			}
		};
		return this;
	}

	public Callback getCallback() {
		return mCallback;
	}

	public void interrupt() {
		interrupted = true;
		this.cancel(true);
	}

	public boolean isInterrupt() {
		return interrupted;
	}

	public static interface Callback {
		public void onSuccess(HandlerMessageTask task, Object t);

		public void onFail(HandlerMessageTask task, Object t);
	}
}
