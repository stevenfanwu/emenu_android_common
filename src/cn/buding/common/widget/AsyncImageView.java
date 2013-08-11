package cn.buding.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import cn.buding.common.R;
import cn.buding.common.R.attr;

/**
 * custom image view that could show a remote image.
 * 
 * @see attr#scaleType
 * @see attr#loadingBackground
 * @see attr#imageUrl
 */
public class AsyncImageView extends cn.buding.common.widget.BaseAsyncImageView {
	private static final String TAG = "AsyncImageView";
	private static final ScaleType[] sScaleTypeArray = { ScaleType.MATRIX,
			ScaleType.FIT_XY, ScaleType.FIT_START, ScaleType.FIT_CENTER,
			ScaleType.FIT_END, ScaleType.CENTER, ScaleType.CENTER_CROP,
			ScaleType.CENTER_INSIDE };

	public AsyncImageView(Context context) {
		this(context, null);
	}

	public AsyncImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs,
				R.styleable.AsyncImageView);

		int imageLayout = ta
				.getResourceId(R.styleable.AsyncImageView_imageLayout,
						getDefaultImageLayout());
		if (imageLayout != 0) {
			setImageLayout(imageLayout);
		}

		Drawable loadingDrawable = ta
				.getDrawable(R.styleable.AsyncImageView_loadingBackground);
		if (loadingDrawable != null)
			setLoadingBackground(loadingDrawable);
		Drawable loadFailedDrawable = ta
				.getDrawable(R.styleable.AsyncImageView_loadFailedDrawable);
		if (loadFailedDrawable != null)
			setLoadFailedDrawable(loadFailedDrawable);

		String imgUrl = ta.getString(R.styleable.AsyncImageView_imageUrl);
		if (imgUrl != null)
			setImageUrlAndLoad(imgUrl);

		int scaleType = ta.getInt(R.styleable.AsyncImageView_scaleType, -1);
		if (scaleType >= 0 && scaleType < sScaleTypeArray.length)
			setScaleType(sScaleTypeArray[scaleType]);

		ta.recycle();
	}

	@Override
	protected int getLayout() {
		return R.layout.widget_async_image_view;
	}

	protected int getDefaultImageLayout() {
		return R.layout.simple_image_view;
	}

}
