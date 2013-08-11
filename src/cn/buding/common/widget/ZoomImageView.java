/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.buding.common.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

@TargetApi(8)
public class ZoomImageView extends ImageView {
	private static final int INVALID_POINTER_ID = -1;

	private RectF mInitRect;
	private float mPosX;
	private float mPosY;

	private float mLastTouchX;
	private float mLastTouchY;
	private int mActivePointerId = INVALID_POINTER_ID;

	private ScaleGestureDetector mScaleDetector;
	private float mSumScaleFactor = 1.f;
	private float mLastSumScaleFactor = 1.f;
	private float mFocusX;
	private float mFocusY;
	private float mScaleFactor;

	private RestoreRunnable mRestoreRunnable;

	public ZoomImageView(Context context) {
		this(context, null, 0);
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		setScaleType(ScaleType.FIT_START);
		mRestoreRunnable = new RestoreRunnable();
	}

	/**
	 * Only FIT_START permitted.
	 */
	@Override
	public void setScaleType(ScaleType scaleType) {
		super.setScaleType(ScaleType.FIT_START);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		mInitRect = getInitRect();
		RectF viewRect =
				new RectF(0, 0, getWidth() - getPaddingLeft()
						- getPaddingRight(), getHeight() - getPaddingTop()
						- getPaddingBottom());
		mPosX = viewRect.centerX() - mInitRect.centerX();
		mPosY = viewRect.centerY() - mInitRect.centerY();
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
	public boolean onTouchEvent(MotionEvent ev) {
//		Log.i("Tag", "Image " + getAction(ev.getAction()));
		// Let the ScaleGestureDetector inspect all events.
		mScaleDetector.onTouchEvent(ev);

		final int action = ev.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			mRestoreRunnable.clearRunnable();
			final float x = ev.getX();
			final float y = ev.getY();
			mLastTouchX = x;
			mLastTouchY = y;
			mActivePointerId = ev.getPointerId(0);
			break;
		}
		
		case MotionEvent.ACTION_POINTER_DOWN:
			mRestoreRunnable.clearRunnable();
			break;

		case MotionEvent.ACTION_MOVE:
			mRestoreRunnable.clearRunnable();
			int pointerIndex = ev.findPointerIndex(mActivePointerId);
			final float x = ev.getX(pointerIndex);
			final float y = ev.getY(pointerIndex);

			// Only move if the ScaleGestureDetector isn't processing a gesture.
			if (!mScaleDetector.isInProgress()) {
				final float dx = x - mLastTouchX;
				final float dy = y - mLastTouchY;

				RectF bound = getCurrentImgBound(mPosX, mPosY);
				if (bound.left < getLeft() && dx > 0)
					mPosX += dx;
				if (bound.right > getRight() && dx < 0)
					mPosX += dx;
				if (bound.left >= getLeft() && bound.right <= getRight())
					mPosX += dx;
				if (bound.top < getTop() && dy > 0)
					mPosY += dy;
				if (bound.bottom > getBottom() && dy < 0)
					mPosY += dy;
				if (bound.top >= getTop() && bound.bottom <= getBottom())
					mPosY += dy;

				invalidate();

			}
			mLastTouchX = x;
			mLastTouchY = y;
			break;

		case MotionEvent.ACTION_UP:
			mActivePointerId = INVALID_POINTER_ID;
			restoreToCenter();
			break;

		case MotionEvent.ACTION_CANCEL:
			mActivePointerId = INVALID_POINTER_ID;
			restoreToCenter();
			break;

		case MotionEvent.ACTION_POINTER_UP:
			mRestoreRunnable.clearRunnable();
			pointerIndex =
					(ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			final int pointerId = ev.getPointerId(pointerIndex);
			if (pointerId == mActivePointerId) {
				// This was our active pointer going up. Choose a new
				// active pointer and adjust accordingly.
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mLastTouchX = ev.getX(newPointerIndex);
				mLastTouchY = ev.getY(newPointerIndex);
				mActivePointerId = ev.getPointerId(newPointerIndex);
			}
			break;

		}

		return true;
	}

	private void restoreToCenter() {
		RectF imgBound = getCurrentImgBound();
		// restore img to center only when the img is smaller than screen
		if (imgBound.width() < getRight() - getLeft()) {
			float dx = (getRight() - getLeft()) / 2f - imgBound.centerX();
			float dy = (getBottom() - getTop()) / 2f - imgBound.centerY();
			mRestoreRunnable.startByDistance(dx, dy, 300);
		}
	}

	public boolean isScaling() {
		return mScaleDetector.isInProgress();
	}

	@Override
	public void onDraw(Canvas canvas) {
		canvas.save();
		/**
		 * first scale sumScaleFactor and then translate to mPos
		 */
		Matrix matrix = canvas.getMatrix();
		matrix.preTranslate(mPosX, mPosY);
		matrix.preScale(mSumScaleFactor, mSumScaleFactor);
		canvas.setMatrix(matrix);
		// if (mImg != null)
		// mImg.draw(canvas);
		super.onDraw(canvas);
		canvas.restore();
	}

	public RectF getCurrentImgBound() {
		return getCurrentImgBound(mPosX, mPosY);
	}

	public float getDx(MotionEvent ev) {
		return ev.getX() - mLastTouchX;
	}

	private RectF getCurrentImgBound(float posX, float posY) {
		RectF rect = new RectF(mInitRect);
		scaleRectf(rect, mSumScaleFactor);
		rect.offset((int) posX, (int) posY);
		return rect;
	}

	private RectF getInitRect() {
		RectF src = new RectF();
		RectF dst = new RectF();
		float dw =
				getDrawable() != null ? getDrawable().getIntrinsicWidth() : 0;
		float dh =
				getDrawable() != null ? getDrawable().getIntrinsicHeight() : 0;
		src.set(0, 0, dw, dh);
		dst.set(0, 0, getWidth() - getPaddingLeft() - getPaddingRight(),
				getHeight() - getPaddingTop() - getPaddingBottom());

		float srcRatio = src.width() / src.height();
		float dstRatio = dst.width() / dst.height();
		if (srcRatio < dstRatio) {
			scaleRectf(src, dst.height() / src.height());
		} else {
			scaleRectf(src, dst.width() / src.width());
		}

		// src.offset(dst.centerX() - src.centerX(), dst.centerY() - src.centerY());
		return src;
	}

	private void scaleRectf(RectF rect, float scale) {
		rect.left *= scale;
		rect.top *= scale;
		rect.right *= scale;
		rect.bottom *= scale;
	}

	private static final float MIN_SCALE_FACTOR = 0.8f;
	private static final float MAX_SCALE_FACTOR = 2.0f;

	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor = (float) Math.sqrt(detector.getScaleFactor());
			mSumScaleFactor *= mScaleFactor;
			// Don't let the object get too small or too large.
			mSumScaleFactor =
					Math.max(MIN_SCALE_FACTOR,
							Math.min(mSumScaleFactor, MAX_SCALE_FACTOR));
			mScaleFactor = mSumScaleFactor / mLastSumScaleFactor;
			mLastSumScaleFactor = mSumScaleFactor;

			mFocusX = detector.getFocusX();
			mFocusY = detector.getFocusY();

			mPosX = (mPosX - mFocusX) * mScaleFactor + mFocusX;
			mPosY = (mPosY - mFocusY) * mScaleFactor + mFocusY;

			invalidate();
			return true;
		}
	}

	class RestoreRunnable implements Runnable {
		private float mDx;
		private float mDy;
		private float mDuration;

		public RestoreRunnable() {
		}

		public void clearRunnable() {
			removeCallbacks(this);
		}

		public void startByDistance(float dx, float dy, long duration) {
			mDuration = duration;
			mDx = dx;
			mDy = dy;
			mLastRunTime = SystemClock.uptimeMillis();
			clearRunnable();
			post(this);
		}

		private long mLastRunTime;

		@Override
		public void run() {
			long durationStep = SystemClock.uptimeMillis() - mLastRunTime;
			mLastRunTime = SystemClock.uptimeMillis();
			int count = (int) (mDuration / durationStep);
			float dx = mDx / count;
			float dy = mDy / count;
			mPosX += dx;
			mPosY += dy;
			mDx -= dx;
			mDy -= dy;
			mDuration -= durationStep;
			invalidate();
			if (mDuration > durationStep)
				post(this);
		}
	}

}
