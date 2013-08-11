package cn.buding.common.asynctask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

public class ProgessTask<A, B, C> extends TaskWraper<A, B, C> {

	private ProgressDialog mProgressDialog;
	private String mMessage;
	private String mTitle;
	private boolean mShowProgressDialog = false;
	private Context mContext;

	public ProgessTask(Context context, TaskWraper<A, B, C> wraper) {
		super(wraper);
		mContext = context;
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
				createLoadingDialog();
			if (!mProgressDialog.isShowing())
				mProgressDialog.show();
		} catch (Exception e) {
		}
	}

	private void createLoadingDialog() {
		// use top parent context to create the dialog. since the dialog should
		// not appear if the activity is in a
		// TabHost.
		mProgressDialog = new ProgressDialog(getTopParent((Activity) mContext));
		mProgressDialog.setTitle(mTitle);
		mProgressDialog.setMessage(mMessage);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {

			}
		});
	}

	private Context getTopParent(Activity context) {
		Activity parent = context.getParent();
		while (parent != null) {
			context = parent;
			parent = context.getParent();
		}
		return context;
	}

	public void setLoadingTitle(String title) {
		mTitle = title;
		if (mProgressDialog != null)
			mProgressDialog.setTitle(mTitle);
	}

	public void setLoadingMessage(String message) {
		mMessage = message;
		if (mProgressDialog != null)
			mProgressDialog.setMessage(message);
	}

	@Override
	public void onPreExecute() {
		super.onPreExecute();
		if (mShowProgressDialog) {
			showDialog();
		}
	}

	protected void onPostExecute(C result) {
		super.onPostExecute(result);
		dismissDialog();
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		dismissDialog();
	}

}
