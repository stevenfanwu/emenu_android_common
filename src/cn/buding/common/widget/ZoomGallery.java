package cn.buding.common.widget;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ZoomGallery extends MGallery {
	private MotionEvent mInterceptEvent;
	private MotionEvent mDisInterceptEvent;
	private boolean mLastOutWidth;
	private boolean mOutWidth;
	private int mZoomImageViewResId;
	private RectF mImgBound;
	

	public ZoomGallery(Context context) {
		this(context, null);
	}

	public ZoomGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFadingEdgeLength(0);
	}

	public void setZoomViewId(int id) {
		mZoomImageViewResId = id;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		ImageView selectedImgView = getSelectedZoomView();
		if (selectedImgView instanceof ZoomImageView) {
			ZoomImageView selectedZoomView = (ZoomImageView) selectedImgView;
			mImgBound = selectedZoomView.getCurrentImgBound();
			float dx = selectedZoomView.getDx(ev);
			mOutWidth =
					(mImgBound.left + dx - getLeft())
							* (mImgBound.right + dx - getRight()) > 0;
			switch (ev.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_POINTER_DOWN:
				// if second pointer down, the gallery is disallowed to intercept the following event. and send a
				// ACTION_DOWN to find the target child in ViewGroup.dispatchTouchEvent()
				requestDisallowInterceptTouchEvent(true);
				MotionEvent event = MotionEvent.obtain(ev);
				event.setAction(MotionEvent.ACTION_DOWN);
				dispatchTouchEvent(event);
				break;
			case MotionEvent.ACTION_MOVE:
				// let child intercept the event if img back into bound.
				if (mLastOutWidth ^ mOutWidth) {
//					Log.i("Tag", "Gallery change action " + mLastOutWidth
//							+ "->" + mOutWidth);
					ev.setAction(MotionEvent.ACTION_DOWN);
				}
				break;
			}
			boolean res = super.dispatchTouchEvent(ev);
			mLastOutWidth = mOutWidth;
			return res;
		} else {
			return super.dispatchTouchEvent(ev);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (ev == mInterceptEvent) {
			mInterceptEvent = null;
			return true;
		}
		if (ev == mDisInterceptEvent) {
			mDisInterceptEvent = null;
			return false;
		}
		ImageView view = getSelectedZoomView();
		if (!(view instanceof ZoomImageView)) {
			return false;
		}
		ZoomImageView zView = (ZoomImageView) view;
		// if the view is scaling, do not intercept
		if (zView.isScaling())
			return false;
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE:
			// let gallery intercept an new ACTION_DOWN event to avoid gestureDectector calling onScroll with wrong
			// distance.
			// FIXME: this part of code should only be called when viewgroup begins to intercept event.
			if (mOutWidth) {
				mInterceptEvent = MotionEvent.obtain(ev);
				mInterceptEvent.setAction(MotionEvent.ACTION_DOWN);
				dispatchTouchEvent(mInterceptEvent);
				return true;
			}
			break;
		}
		return false;
	}

	private String getAction(int action) {
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_CANCEL:
			return "cancel";
		case MotionEvent.ACTION_DOWN:
			return "down";
		case MotionEvent.ACTION_MOVE:
			return "move";
		case MotionEvent.ACTION_UP:
			return "up";
		case MotionEvent.ACTION_POINTER_UP:
			return "pointer_up";
		case MotionEvent.ACTION_POINTER_DOWN:
			return "pointer_down";
		}
		return "";
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
//		Log.i("Tag", "Gallery " + getAction(event.getAction()) + " "
//				+ mOutWidth);
		return super.onTouchEvent(event);
	}

	private ImageView getSelectedZoomView() {
		View view = getSelectedView();
		if (mZoomImageViewResId == 0)
			return (ImageView) view;
		else
			return (ImageView) view.findViewById(mZoomImageViewResId);
	}
}
