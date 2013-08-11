package cn.buding.common.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.LayoutParams;

public class Utils {
	public static boolean notNullEmpty(String s) {
		return s != null && s.length() > 0;
	}

	public static boolean equalOrNull(String a, String b) {
		if (a != null)
			return a.equals(b);
		else
			return b == null;
	}

	/** set the height of listview to its full height. it could be used when a listview must be shown in a scrollview. */
	public static void setListViewHeightBasedOnChildren(ListView list) {
		ListAdapter adapter = list.getAdapter();
		if (adapter == null)
			return;

		int totalHeight = 0;
		for (int i = 0, len = adapter.getCount(); i < len; i++) {
			View item = adapter.getView(i, null, list);
			ListView.LayoutParams p =
					(ListView.LayoutParams) item.getLayoutParams();
			if (p == null) {
				p =
						new ListView.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT,
								ViewGroup.LayoutParams.WRAP_CONTENT, 0);
				item.setLayoutParams(p);
			}
			item.measure(0, 0);
			totalHeight += item.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = list.getLayoutParams();
		int dividerHeight = list.getDividerHeight() * adapter.getCount();
		params.height =
				totalHeight + dividerHeight + list.getPaddingBottom()
						+ list.getPaddingTop();
		list.setLayoutParams(params);
	}

	/**
	 * if text is notNullAndEmpty, set text to textview, or hide textview
	 */
	public static boolean setValidTextOrHide(TextView tv, String text) {
		return setValidTextOrHide(tv, "", text);
	}

	/**
	 * @param prefix the prefix to add before text
	 */
	public static boolean setValidTextOrHide(TextView tv, String prefix,
			String text) {
		if (tv == null)
			return false;
		boolean valid = notNullEmpty(text);
		if (valid) {
			tv.setText(prefix + text);
			tv.setVisibility(View.VISIBLE);
		} else
			tv.setVisibility(View.GONE);
		return valid;
	}

	/**
	 * set the text to textview. if text is not vaild, then hide the textview and all relatedview
	 */
	public static boolean setValidTextOrHide(TextView tv, String text,
			View... relatedViews) {
		int visibility = 0;
		if (setValidTextOrHide(tv, text)) {
			visibility = View.VISIBLE;
		} else
			visibility = View.GONE;
		for (View v : relatedViews) {
			if (v != null)
				v.setVisibility(visibility);
		}
		return false;
	}
}
