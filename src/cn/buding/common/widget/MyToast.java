package cn.buding.common.widget;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * custom toast util.
 */
public class MyToast extends Toast {
	private static MyToast mLastToast;
	private static int TOAST_BACKGROUND = android.R.drawable.toast_frame;

	public MyToast(Context context) {
		super(context);
	}

	public static void setBackgroundRes(int res) {
		TOAST_BACKGROUND = res;
	}

	public static MyToast makeText(Context context, String text, int duration) {
		MyToast toast = new MyToast(context);
		TextView tv = new TextView(context);
		tv.setBackgroundResource(TOAST_BACKGROUND);
		tv.setTextAppearance(context,
				android.R.style.TextAppearance_Small_Inverse);
		tv.setTextColor(Color.WHITE);
		tv.setGravity(Gravity.CENTER);
		tv.setShadowLayer(2.75f, 0.1f, 0.1f, 0xBB000000);
		tv.setText(text);

		toast.setView(tv);
		toast.setDuration(duration);
		return toast;
	}

	public static MyToast makeText(Context context, String text) {
		if (mLastToast == null
				|| mLastToast.getView().getWindowVisibility() != View.VISIBLE) {
			mLastToast = makeText(context, text, Toast.LENGTH_SHORT);
		} else {
			((TextView) (mLastToast.getView())).setText(text);
		}
		return mLastToast;
	}

	public static MyToast makeText(Context context, int resId) {
		return makeText(context, context.getResources().getString(resId));
	}

}
