package cn.buding.common.widget;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.StateSet;
import android.widget.ImageView;

public class ClickableImageView extends ImageView {
	private static final String TAG = "ClickableImageView";

	public ClickableImageView(Context context) {
		this(context, null);
	}

	public ClickableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDuplicateParentStateEnabled(true);
	}

	@TargetApi(4)
	@Override
	public void setImageBitmap(Bitmap bm) {
		if (bm == null) {
			setImageDrawable(null);
			return;
		}
		BitmapDrawable drawableNormal = new BitmapDrawable(getContext()
				.getResources(), bm);
		AlphaStateListDrawable d = new AlphaStateListDrawable();
		d.addState(PRESSED_ENABLED_STATE_SET, drawableNormal, 150);
		d.addState(EMPTY_STATE_SET, drawableNormal, 255);
		setImageDrawable(d);
	}

	/**
	 * StateListDrawable could only have one alpha. but we want to have
	 * different for different state.
	 */
	@TargetApi(5)
	static class AlphaStateListDrawable extends StateListDrawable {
		private List<Pair<int[], Integer>> mAlphas;

		public AlphaStateListDrawable() {
			super();
			mAlphas = new ArrayList<Pair<int[], Integer>>();
		}

		public void addState(int[] stateSet, Drawable drawable, int alpha) {
			super.addState(stateSet, drawable);
			mAlphas.add(new Pair<int[], Integer>(stateSet, alpha));
		}

		@Override
		protected boolean onStateChange(int[] stateSet) {
			boolean res = super.onStateChange(stateSet);
			Drawable d = getCurrent();
			if (d == null)
				return res;
			d.setAlpha(255);
			for (Pair<int[], Integer> e : mAlphas) {
				if (StateSet.stateSetMatches(e.first, stateSet)) {
					d.setAlpha(e.second);
					d.invalidateSelf();
					break;
				}
			}
			return res;
		}
	}
}