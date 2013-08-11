package cn.buding.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;

/**
 * utils for bitmap.
 */
public class BitmapUtils {
	private static final String TAG = "BitmapUtils";
	public static final int compressRatio = 60;

	/**
	 * return a compressed Bitmap of uri.
	 * 
	 * @param uri
	 *            the image uri.
	 * @param maxScale
	 *            maxScale of returned img
	 */
	public static CompressedBitmap getAppropriateBitmap(Context context,
			Uri uri, int maxScale, BitmapFactory.Options opts) {
		try {
			return getAppropriateBitmap(context, context.getContentResolver()
					.openInputStream(uri), maxScale, opts);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "", e);
			return null;
		}
	}

	public static CompressedBitmap getAppropriateBitmap(Context context,
			File file, int maxScale, BitmapFactory.Options opts) {
		try {
			return getAppropriateBitmap(context, new FileInputStream(file),
					maxScale, opts);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "", e);
			return null;
		}
	}

	public static CompressedBitmap getAppropriateBitmap(Context context,
			InputStream in, int maxScale, BitmapFactory.Options opts) {
		if (opts == null)
			opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		byte[] inBytes = null;
		opts.inSampleSize = 1;
		
		try {
			inBytes = getBytes(in);
			BitmapFactory.decodeStream(new ByteArrayInputStream(inBytes), null,
					opts);
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return null;
		}
		int realWidth = opts.outWidth;
		int realHeight = opts.outHeight;
		int realSize = realWidth * realHeight;

		int stepSize = maxScale;
		int sampleSize = 1;
		while (realSize > stepSize) {
			sampleSize <<= 1;
			stepSize <<= 2;
		}

		opts.inJustDecodeBounds = false;
		opts.inSampleSize = sampleSize;

		Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(
				inBytes), null, opts);
		return new CompressedBitmap(bitmap, sampleSize);
	}

	private static byte[] getBytes(InputStream in) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[512];
		int len;
		while ((len = in.read(buffer)) > 0) {
			bos.write(buffer, 0, len);
		}
		return bos.toByteArray();
	}

	/**
	 * save a bitmap to path.
	 */
	public static void saveBitmap(Bitmap bm, String path) {
		File img = new File(path);
		try {
			FileOutputStream fOut = null;
			fOut = new FileOutputStream(img);
			bm.compress(Bitmap.CompressFormat.JPEG, compressRatio, fOut);
			fOut.flush();
			fOut.close();
			Log.d(TAG, "Save Bitmap, Size:" + img.length() / 1024 + ", Path:"
					+ path);
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
	}

	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = pixels;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	/**
	 * a wrapper class for Bitmap. contain the sample radio of this bitmap.
	 * thisImg.width * sample = originalImg.width
	 */
	public static class CompressedBitmap {
		private Bitmap compressedBitmap;
		private int sampleSize;

		public CompressedBitmap(Bitmap bt, int sample) {
			this.compressedBitmap = bt;
			this.sampleSize = sample;
		}

		public Bitmap getBitmap() {
			return compressedBitmap;
		}

		public int getSampleSize() {
			return sampleSize;
		}

	}
}
