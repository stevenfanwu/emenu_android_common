package cn.buding.common.asynctask;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import cn.buding.common.exception.CustomException;
import cn.buding.common.exception.ECode;
import cn.buding.common.widget.MyToast;

public abstract class MessageTask extends MTask {
	private static final String TAG = "MessageTask";
	private boolean mShowCodeMsg = true;
	private Map<Integer, Object> mCodeMsgs = new HashMap<Integer, Object>();
	private static Map<Integer, Object> mDefaultCodeMsgs;

	protected Callback mCallback;

	static {
		mDefaultCodeMsgs = new HashMap<Integer, Object>();
		mDefaultCodeMsgs.put(ECode.SUCCESS, "");
		mDefaultCodeMsgs.put(ECode.SUCCESS_LAST_TIME, "");
		mDefaultCodeMsgs.put(ECode.SERVER_RETURN_EMPTY_SET, "到头了");
		mDefaultCodeMsgs
				.put(ECode.SERVER_RETURN_EMPTY_SET_FIRST_TIME, "暂时没有数据");
		mDefaultCodeMsgs.put(ECode.FAIL, "网络连接失败，请稍候重试");
		mDefaultCodeMsgs.put(ECode.CANNOT_LOCATE, "暂时无法定位");
	}

	public MessageTask(Context context) {
		this(context, null);
	}

	public MessageTask(Context context, MTask task) {
		super(context, task);
	}

	public static void setDefaultCodeMsg(int code, Object value) {
		mDefaultCodeMsgs.put(code, value);
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

		String codeMsg = getCodeMsg(code);
		if (codeMsg == null)
			codeMsg = getCodeMsg(ECode.FAIL);
		if (mShowCodeMsg && codeMsg != null && codeMsg.length() > 0)
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
		Object o = mDefaultCodeMsgs.get(code);
		return convertCodeMsg(o);
	}

	private String getCodeMsg(int code) {
		Object o = mCodeMsgs.get(code);
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
		mShowCodeMsg = b;
	}

	public void setCodeMsg(Integer code, String msg) {
		mCodeMsgs.put(code, msg);
	}

	public void setCodeMsg(Integer code, int res) {
		mCodeMsgs.put(code, res);
	}

	public MessageTask setCallback(Callback callback) {
		mCallback = callback;
		return this;
	}

	public Callback getCallback() {
		return mCallback;
	}

	public static interface Callback {
		public void onSuccess(MessageTask task, Object t);

		public void onFail(MessageTask task, Object t);
	}

}
