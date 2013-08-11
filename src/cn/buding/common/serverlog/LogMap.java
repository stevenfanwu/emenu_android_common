package cn.buding.common.serverlog;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.view.View;

public abstract class LogMap {
	private Map<String, String> mPageMap;
	private Map<String, Map<Integer, String>> mViewMap;
	// The tag id for log id. This tag is used when View.id cannot be used for
	// logging, for example, RadioButtons in
	// RadioGroup.
	public static final int LOG_ID = 0x33333333;
	// The tag id for log data.
	public static final int LOG_DATA = 0x33333334;

	protected LogMap() {
		mPageMap = new HashMap<String, String>();
		mViewMap = new HashMap<String, Map<Integer, String>>();
		initPageMap();
		initViewMap();
	}

	/**
	 * @example putPage(AboutActivity.class, "Page.About");
	 */
	protected abstract void initPageMap();

	protected void putPage(Class<?> page, String tag) {
		mPageMap.put(page.getName(), tag);
	}

	/**
	 * @example putView(ShopList.class, R.id.bt_title_left, "Btn.More");
	 */
	protected abstract void initViewMap();

	protected void putView(Class<?> page, int viewId, String tag) {
		Map<Integer, String> map = mViewMap.get(page.getName());
		if (map == null) {
			map = new HashMap<Integer, String>();
			mViewMap.put(page.getName(), map);
		}
		map.put(viewId, tag);
	}

	protected String getLogName(Class<?> klass) {
		return mPageMap.get(klass.getName());
	}

	public String getPageLogName(String klass) {
		return mPageMap.get(klass);
	}

	protected String getLogName(View view) {
		Map<Integer, String> map = mViewMap.get(view.getContext().getClass().getName());
		String res = null;
		if (map != null) {
			res = map.get(view.getId());
			// if the id of view cannot be used in logging, for instance,
			// RadioButtons in RadioGroup, we add a new tag
			// name LOG_ID to get the logname.
			if (res == null) {
				Integer logId = (Integer) view.getTag(LOG_ID);
				res = map.get(logId);
			}
		}
		return res;
	}

	public Object getLogData(View view) {
		Object o = view.getTag(LOG_DATA);
		Object data;
		if (o instanceof LogAble) {
			data = ((LogAble) o).getLogData();
		} else {
			data = o;
		}
		return data;
	}
}
