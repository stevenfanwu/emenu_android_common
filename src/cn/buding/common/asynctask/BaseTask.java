package cn.buding.common.asynctask;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.AsyncTask;

/**
 * a base class for all task.
 */
public abstract class BaseTask<A, B, C> extends AsyncTask<A, B, C> {
	protected Context mContext;
	protected Resources mRes;
	/** whether to finish current activity when the ProgressDialog is canceled. */
	private boolean mFinishActivityOnCancel = false;
	/** whether show the progress dialog when task is running in background */
	private boolean mShowProgressDialog = false;
	/** whether to cancel this task when this Dialog is canceled */
	private boolean mFinshTaskOnCancel = false;

	private String mMessage;
	private String mTitle;
	private Dialog mProgressDialog;

	public BaseTask(Context context) {
		this(context, false);
	}

	public BaseTask(Context context, boolean finishActivityOnCancel) {
		this.mFinishActivityOnCancel = finishActivityOnCancel;
		this.mContext = context;
		mRes = context.getResources();
		mTitle = "消息处理";
		mMessage = "连接服务器";
	}

	public void setShowProgessDialog(boolean f) {
		mShowProgressDialog = f;
	}

	protected void setFinishActivityOnCancel(boolean f) {
		mFinishActivityOnCancel = f;
	}

	protected void setFinshTaskOnCancel(boolean f) {
		mFinshTaskOnCancel = f;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (mShowProgressDialog) {
			showDialog();
		}
	}

	@Override
	protected void onCancelled() {
		dismissDialog();
	}

	protected void onPostExecute(C result) {
		dismissDialog();
	}

	public void dismissDialog() {
		try {
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
		} catch (Exception e) {
		}
	}

	private void showDialog() {
		try {
			if (mProgressDialog == null)
				mProgressDialog = createLoadingDialog();
			if (!mProgressDialog.isShowing())
				mProgressDialog.show();
		} catch (Exception e) {
		}
	}

	protected Dialog createLoadingDialog() {
		// use top parent context to create the dialog. since the dialog should
		// not appear if the activity is in a
		// TabHost.
		ProgressDialog progressDialog = new ProgressDialog(
				getTopParent((Activity) mContext));
		if (mTitle != null)
			progressDialog.setTitle(mTitle);
		if (mMessage != null)
			progressDialog.setMessage(mMessage);
		progressDialog.setCancelable(true);
		progressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (mFinishActivityOnCancel) {
					finishContext();
				}
				if (mFinshTaskOnCancel) {
					cancel(true);
				}
			}
		});
		return progressDialog;
	}

	private Context getTopParent(Activity context) {
		Activity parent = context.getParent();
		while (parent != null) {
			context = parent;
			parent = context.getParent();
		}
		return context;
	}

	public void finishContext() {
		if (mContext instanceof Activity) {
			((Activity) mContext).finish();
		}
	}

	public void setLoadingTitle(String title) {
		mTitle = title;
		if (mProgressDialog instanceof ProgressDialog) {
			((ProgressDialog)mProgressDialog).setTitle(mTitle);
		}
	}

	public void setLoadingMessage(String message) {
		mMessage = message;
		if (mProgressDialog instanceof ProgressDialog) {
			((ProgressDialog)mProgressDialog).setMessage(message);
		}
	}

}
