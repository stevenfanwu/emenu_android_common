package cn.buding.common.widget;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import cn.buding.common.R;
import cn.buding.common.file.ImageBuffer;
import cn.buding.common.file.ImageBuffer.BitmapFilter;
import cn.buding.common.file.LoadResThread;
import cn.buding.common.file.ThreadPool;
import cn.buding.common.file.ImageBuffer.BitmapParams;
import cn.buding.common.file.ImageBuffer.UrlBitmap;
import cn.buding.common.file.LoadResThread.OnResLoadedListener;

/**
 * custom image view that could show a remote image.
 */
public abstract class BaseAsyncImageView extends RelativeLayout {
	private static final String TAG = "BaseAsyncImageView";
	/** loading image */
	public static final int STATE_LOADING = 1;
	/** load image success */
	public static final int STATE_LOAD_SUCCESS = 2;
	/** load image filed */
	public static final int STATE_LOAD_FAIL = 3;

	protected ProgressBar mProgressBar;
	private ImageView mLoadingBackground;
	private ImageView mImageView;
	private ViewGroup mFrame;

	// this thread is attached to mImageView
	private LoadResThread mLoadImgThread;

	protected String mImgUrl;
	private BitmapParams mParams;
	private Drawable mLoadingDrawable;
	private Drawable mLoadFailedDrawable;
	private int mLoadingState;
	private OnImageLoadedListener mOnImageLoadedListener;

	private Handler mHandler;

	private boolean mAddToFrame = false;

	private int mImageLayout;

	private static class SingleExecutor {
		private static SingleExecutor instance;

		public static SingleExecutor singleton() {
			if (instance == null) {
				instance = new SingleExecutor();
			}
			return instance;
		}

		private int mQueueSize = 15;
		private BlockingQueue<Runnable> mQueue = new ArrayBlockingQueue<Runnable>(
				mQueueSize);
		private Executor mExecutor = new ThreadPoolExecutor(1, 1, 0L,
				TimeUnit.MILLISECONDS, mQueue,
				new ThreadPoolExecutor.DiscardOldestPolicy());

		public void execute(Runnable r) {
			if (mQueue.contains(r)) {
				mQueue.remove(r);
			}
			mExecutor.execute(r);
		}
	}

	public BaseAsyncImageView(Context context) {
		this(context, null);
	}

	public BaseAsyncImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mAddToFrame = false;
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(getLayout(), this);
		mAddToFrame = true;
		mHandler = new Handler(context.getMainLooper());
		initElements();
	}

	protected void setImageLayout(int imageLayout) {
		mImageLayout = imageLayout;
		initImageView();
	}

	protected abstract int getLayout();

	@Override
	public void addView(View child, int index,
			android.view.ViewGroup.LayoutParams params) {
		if (!mAddToFrame) {
			super.addView(child, index, params);
		} else {
			// after construction, views will be added to frame
			setFrameView(child);
		}
	}

	protected void initElements() {
		mProgressBar = (ProgressBar) findViewById(android.R.id.progress);
		mLoadingBackground = (ImageView) findViewById(android.R.id.background);
		mFrame = (ViewGroup) findViewById(android.R.id.widget_frame);
		initImageView();
	}

	private void initImageView() {
		mAddToFrame = false;
		View image = findViewById(android.R.id.content);
		if (image instanceof ViewStub) {
			if (mImageLayout != 0) {
				ViewStub stub = (ViewStub) image;
				stub.setInflatedId(image.getId());
				stub.setLayoutResource(mImageLayout);
				mImageView = (ImageView) stub.inflate();
			}
		} else {
			mImageView = (ImageView) findViewById(android.R.id.content);
		}
		mAddToFrame = true;
	}

	public void setFrameView(View v) {
		if (mFrame != null) {
			if (mFrame.getChildCount() == 1)
				throw new RuntimeException("frame can only has one child");
			mFrame.addView(v);
		}
	}

	public View getFrameView() {
		if (mFrame == null) {
			return null;
		}
		if (mFrame.getChildCount() == 0)
			return null;
		return mFrame.getChildAt(0);
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		try {
			return super.drawChild(canvas, child, drawingTime);
		} catch (Exception e) {
			refreshImage();
		}
		return true;
	}

	public void setLoadingBackground(int resId) {
		Drawable backgroundDrawable = null;
		try {
			backgroundDrawable = getContext().getResources().getDrawable(resId);
			setLoadingBackground(backgroundDrawable);
		} catch (NotFoundException e) {
		}
	}

	public void setLoadingBackground(Drawable drawable) {
		mLoadingDrawable = drawable;
	}

	public void setLoadFailedDrawable(Drawable drawable) {
		mLoadFailedDrawable = drawable;
	}

	public void setScaleType(ScaleType st) {
		mImageView.setScaleType(st);
	}

	public void setImageUrl(String url) {
		mImgUrl = url;
	}

	public int getLoadingState() {
		return mLoadingState;
	}

	public void setImageUrlAndLoad(String url) {
		setImageUrlAndLoad(url, null);
	}

	private void setLoadingState(int state) {
		mLoadingState = state;
		switch (state) {
		case STATE_LOAD_FAIL:
			mImageView.setImageBitmap(null);
			setBackgroundVisiability(View.VISIBLE);
			mLoadingBackground.setBackgroundDrawable(mLoadFailedDrawable);
			if (mOnImageLoadedListener != null)
				mOnImageLoadedListener.onImageLoaded(mImgUrl, getBitmap());
			break;
		case STATE_LOAD_SUCCESS:
			setBackgroundVisiability(View.INVISIBLE);
			if (mOnImageLoadedListener != null)
				mOnImageLoadedListener.onImageLoaded(mImgUrl, getBitmap());
			break;
		case STATE_LOADING:
			mImageView.setImageBitmap(null);
			setBackgroundVisiability(View.VISIBLE);
			mLoadingBackground.setBackgroundDrawable(mLoadingDrawable);
			break;
		}
	}

	private void postSetLoadingState(final int state) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				setLoadingState(state);
			}
		});
	}

	public void postLoading(String url) {
		postLoading(url, 1);
	}

	public void postLoading(String url, float scaleFactor) {
		postLoading(url, scaleFactor, null);
	}

	public void postLoading(String url, float scaleFactor, BitmapParams params) {
		if (isImageNullOrRecycled())
			setLoadingState(STATE_LOADING);
		mPostRunnable.mUrl = url;
		mPostRunnable.setScaleFactor(scaleFactor);
		mPostRunnable.setBitmapParams(params);
		mHandler.removeCallbacks(mPostRunnable);
		mHandler.post(mPostRunnable);
	}

	private PostRunnable mPostRunnable = new PostRunnable();

	private class PostRunnable implements Runnable {
		private float mScaleFactor = 0;
		private BitmapParams mParams;
		private String mUrl;

		public void setBitmapParams(BitmapParams param) {
			mParams = param;
		}

		public void setScaleFactor(float b) {
			mScaleFactor = b;
		}

		@Override
		public void run() {
			if (getWidth() * getHeight() == 0) {
				mHandler.postDelayed(this, 50);
			} else {
				if (mScaleFactor > 1e-6) {
					if (mParams == null)
						mParams = new BitmapParams();
					if (mParams.maxScale == 0)
						mParams.maxScale = (int) (getWidth() * getHeight() * mScaleFactor);
				}
				setImageUrlAndLoad(mUrl, mParams);
			}

		}
	}

	/**
	 * set the url of image, and load the image to sdcard.
	 */
	public void setImageUrlAndLoad(String url, BitmapParams params) {
		if (equalOrNull(url, mImgUrl) && equalOrNull(params, mParams)
				&& !isImageNullOrRecycled()) {
			// img loaded already. do not need to reload.
			return;
		}
		mImgUrl = url;
		mParams = params;
		if (url == null) {
			setLoadingState(STATE_LOAD_FAIL);
			return;
		}
		setLoadingState(STATE_LOADING);
		ImageBuffer imgBuffer = getImageBuffer();
		UrlBitmap bit = imgBuffer.readFromMem(url, params);
		if (bit != null) {
			setImageBitmap(bit.getImg());
		} else {
			mSetImageRunnable.params = params;
			mSetImageRunnable.url = url;
			SingleExecutor.singleton().execute(mSetImageRunnable);
			// mSetImageRunnable.run();
		}
	}

	private static boolean equalOrNull(Object o1, Object o2) {
		return o1 != null && o1.equals(o2) || o1 == null && o2 == null;
	}

	private SetImageRunnable mSetImageRunnable = new SetImageRunnable();

	private class SetImageRunnable implements Runnable {
		public String url;
		public BitmapParams params;

		public void run() {
			try {
				if (url == null)
					return;
				ImageBuffer imgBuffer = getImageBuffer();
				Bitmap bt = imgBuffer.readImg(url, params);
				if (bt != null) {
					// setImageBitmap(bt);
					mRefreshThread.mBitmap = bt;
					mHandler.post(mRefreshThread);
				} else {
					if (mLoadImgThread == null
							|| !ThreadPool.isRunningOrWaiting(mImgUrl)
							|| !url.equals(mLoadImgThread.getUrl())) {
						// thread is finished or url is not match, we have to
						// reload the img.
						if (mLoadImgThread != null
								&& !url.equals(mLoadImgThread.getUrl())) {
							mLoadImgThread
									.unregisterListener(onImgLoadListener);
						}
						mLoadImgThread = ThreadPool.execute(getContext(),
								mImgUrl, imgBuffer, onImgLoadListener);
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "", e);
			}
		}
	};

	protected cn.buding.common.file.ImageBuffer getImageBuffer() {
		return cn.buding.common.file.ImageBuffer.getInstance();
	}

	private OnResLoadedListener onImgLoadListener = new OnResLoadedListener() {

		@Override
		public void onProgressUpdate(int currentSize, int totalSize) {
		}

		@Override
		public void onResLoaded(String url, File file) {
			if (url != null && url.equals(mImgUrl)) {
				if (file != null) {
					mRefreshThread.mBitmap = getImageBuffer().readImg(mImgUrl,
							mParams);
					mHandler.post(mRefreshThread);
				} else {
					postSetLoadingState(STATE_LOAD_FAIL);
				}
			}
		}
	};

	private PostSetImageBitmapRunnable mRefreshThread = new PostSetImageBitmapRunnable();

	private class PostSetImageBitmapRunnable implements Runnable {
		public Bitmap mBitmap;

		@Override
		public void run() {
			setImageBitmap(mBitmap);
			mBitmap = null;
		}
	}

	private void setBackgroundVisiability(int v) {
		if (mLoadingDrawable == null) {
			mLoadingBackground.setVisibility(View.INVISIBLE);
			mProgressBar.setVisibility(v);
		} else {
			mLoadingBackground.setVisibility(v);
			mProgressBar.setVisibility(View.INVISIBLE);
		}
		if (mFrame != null)
			mFrame.setVisibility(v);

		if (v == View.VISIBLE) {
			mImageView.setVisibility(View.INVISIBLE);
		} else {
			mImageView.setVisibility(View.VISIBLE);
		}
	}

	public void setImageBitmap(Bitmap bt) {
		mImageView.setImageBitmap(bt);
		if (bt != null)
			setLoadingState(STATE_LOAD_SUCCESS);
		else
			setLoadingState(STATE_LOAD_FAIL);
	}

	public void setImageResource(int resId) {
		mImageView.setImageResource(resId);
		setLoadingState(STATE_LOAD_SUCCESS);
	}

	public ImageView getImageView() {
		return mImageView;
	}

	public Bitmap getBitmap() {
		Drawable d = mImageView.getDrawable();
		if (d instanceof StateListDrawable) {
			StateListDrawable sd = (StateListDrawable) d;
			d = sd.getCurrent();
		}
		if (d instanceof BitmapDrawable) {
			return ((BitmapDrawable) d).getBitmap();
		}
		return null;
	}

	public String getImgUrl() {
		return mImgUrl;
	}

	public void refreshImage() {
		getImageBuffer().reorderImg(mImgUrl, mParams);
		if (isImageNullOrRecycled()) {
			if (mLoadingState != STATE_LOADING)
				setImageUrlAndLoad(mImgUrl, mParams);
		}
	}

	private boolean isImageNullOrRecycled() {
		Drawable d = mImageView.getDrawable();
		if (d == null)
			return true;
		if (d instanceof StateListDrawable) {
			StateListDrawable sd = (StateListDrawable) d;
			return isBitmapDrawableNullOrRecycled(sd.getCurrent());
		}
		return isBitmapDrawableNullOrRecycled(d);
	}

	private boolean isBitmapDrawableNullOrRecycled(Drawable d) {
		if (d == null)
			return true;
		else if (d instanceof BitmapDrawable) {
			Bitmap bt = ((BitmapDrawable) d).getBitmap();
			if (bt == null || bt.isRecycled())
				return true;
			return false;
		}
		return true;
	}

	public void setGrayScale() {
		setOnImageLoadedListener(new OnImageLoadedListener() {
			@Override
			public void onImageLoaded(String url, Bitmap bt) {
				addGrayScaleFilter();
			}
		});
	}

	public void addGrayScaleFilter() {
		setSaurationFilter(0);
	}

	public void removeGrayScaleFilter() {
		setSaurationFilter(1);
	}

	private void setSaurationFilter(float value) {
		ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(value);
		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
		Drawable drawable = getImageView().getDrawable();
		if (drawable != null)
			drawable.setColorFilter(filter);
	}

	public void setOnImageLoadedListener(OnImageLoadedListener l) {
		mOnImageLoadedListener = l;
	}

	public static interface OnImageLoadedListener {
		public void onImageLoaded(String url, Bitmap bt);
	}

}
