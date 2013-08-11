package cn.buding.common.activity;

import android.annotation.TargetApi;
import android.app.LocalActivityManager;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

/**
 * base class for TabHost.
 */
@TargetApi(4)
public abstract class BaseTabHost extends TabActivity {
	/** the start tab index */
	public static final String EXTRA_INDEX = "extra_index";
	protected TabHost mTabHost;
	protected TabWidget mTabWidget;
	protected int mTabCount;
	/** the text indicator of each tab */
	protected CharSequence[] mTabIndicator;
	/** the icon indicator of each tab */
	protected int[] mTabIcons;
	/** the content Intent of each tab */
	protected Intent[] mTabIntents;
	protected LocalActivityManager mLocalActivityManager;

	protected int mDefaultIndex = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(getLayout());
		initElements();
		initTabParams();
		initTabView();
		int index = getIntent().getIntExtra(EXTRA_INDEX, mDefaultIndex);
		mTabHost.setCurrentTab(index);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		int index = getIntent().getIntExtra(EXTRA_INDEX, mDefaultIndex);
		mTabHost.setCurrentTab(index);
	}

	protected int getLayout() {
		return cn.buding.common.R.layout.base_tab_host;
	}

	protected void initElements() {
		mTabHost = this.getTabHost();
		mTabWidget = mTabHost.getTabWidget();
		mLocalActivityManager = this.getLocalActivityManager();
	}

	@TargetApi(4)
	protected void addEmptyTabView(String tag, Intent intent) {
		View v = getTabIndicator(0);
		TabSpec tab = mTabHost.newTabSpec(tag).setIndicator(v)
				.setContent(intent);
		mTabHost.addTab(tab);
		LayoutParams param = v.getLayoutParams();
		param.width = param.height = 0;
	}

	/**
	 * init {@link #mTabCount}, {@link #mTabIndicator}, {@link #mTabIcons},
	 * {@link #mTabIntents}
	 */
	protected abstract void initTabParams();

	/** init each tab view. */
	protected void initTabView() {
		for (int i = 0; i < mTabCount; i++) {
			Intent intent = mTabIntents[i];
			View v = getTabIndicator(i);
			String tag = mTabIndicator[i].toString();
			TabSpec tab = mTabHost.newTabSpec(tag).setIndicator(v)
					.setContent(intent);
			mTabHost.addTab(tab);
			onTabAdded(i, v);
			final int index = i;
			if (mOnTabIndicatorClickListener != null)
				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mOnTabIndicatorClickListener != null)
							mOnTabIndicatorClickListener.onTabWidgetClick(
									mTabIndicator[index].toString(), index);
					}
				});
		}
	}

	/** get the ith tab indicator. */
	private View getTabIndicator(int i) {
		View v = getTabIndicatorView(i);
		TextView title = (TextView) v.findViewById(android.R.id.title);
		ImageView icon = (ImageView) v.findViewById(android.R.id.icon);
		title.setText(mTabIndicator[i]);
		if (icon != null && mTabIcons != null) {
			icon.setImageResource(mTabIcons[i]);
		}
		int tabBkgRes = getTabIndicatorBackgroundRes(i);
		if (tabBkgRes != 0) {
			v.setBackgroundResource(tabBkgRes);
		}
		return v;
	}

	protected void onTabAdded(int index, View v) {
	}

	/**
	 * the tab indicator view. must have a TextView named android.R.id.title, a
	 * ImageView(optional) named android.R.id.icon
	 */
	protected abstract View getTabIndicatorView(int i);

	/** return the background resource of tab indicator. */
	protected int getTabIndicatorBackgroundRes(int i) {
		return 0;
	}

	/**
	 * will be invoked when a tabIndicator is clicked. this is different with
	 * {@link TabHost#setOnTabChangedListener(iw.avatar.widget.TabHost.OnTabChangeListener)}
	 */
	private OnTabIndicatorClickListener mOnTabIndicatorClickListener;

	protected void setOnTabIndicatorClickListener(OnTabIndicatorClickListener l) {
		mOnTabIndicatorClickListener = l;
	}

	/** custom tab indicator click listener. */
	protected interface OnTabIndicatorClickListener {
		public void onTabWidgetClick(String tag, int index);
	}
}
