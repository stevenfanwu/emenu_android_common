package cn.buding.common.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;
/** utils for screen display */
public class DisplayUtils {
	public static int getPixel(Context context, float dp) {
		float density = getDensity(context);
		return (int) (dp * density);
	}

	public static float getDensity(Context context) {
		DisplayMetrics metrics = getMetric(context);
		if (metrics != null)
			return metrics.density;
		else
			return 1;
	}

	public static int getScreenHeight(Context context) {
		DisplayMetrics metrics = getMetric(context);
		if (metrics != null)
			return metrics.heightPixels;
		else
			return 801;
	}

	public static int getScreenWidth(Context context) {
		DisplayMetrics metrics = getMetric(context);
		if (metrics != null)
			return metrics.widthPixels;
		else
			return 481;
	}

	public static DisplayMetrics getMetric(Context context) {
		try {
			DisplayMetrics metrics = new DisplayMetrics();
			WindowManager winMgr =
					(WindowManager) context
							.getSystemService(Context.WINDOW_SERVICE);
			winMgr.getDefaultDisplay().getMetrics(metrics);
			return metrics;
		} catch (Exception e) {
		}
		return null;
	}
}
