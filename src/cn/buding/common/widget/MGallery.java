package cn.buding.common.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Gallery;

public class MGallery extends Gallery {
	private int mSpacing;
	private boolean mFlingEnable = false;

	public MGallery(Context context) {
		this(context, null);
	}

	public MGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFadingEdgeLength(0);
		mSpacing = 20;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (mFlingEnable)
			return super.onFling(e1, e2, velocityX, velocityY);
		if (velocityX > 0)
			onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
		else
			onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
		return true;
	}

	@Override
	public void setSpacing(int spacing) {
		mSpacing = spacing;
		super.setSpacing(spacing);
	}

	public void moveNext() {
		onScroll(null, null, mSpacing, 0);
		onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
	}

	public void movePrevious() {
		onScroll(null, null, -mSpacing, 0);
		onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
	}

	public void setFlingEnable(boolean b) {
		mFlingEnable = b;
	}

}
