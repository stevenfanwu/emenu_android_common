package cn.buding.common.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.util.Log;
import cn.buding.common.file.LoadResThread.OnResLoadedListener;
import cn.buding.common.util.BitmapUtils;
import cn.buding.common.util.BitmapUtils.CompressedBitmap;
import cn.buding.common.util.DisplayUtils;
import cn.buding.common.util.GlobalProperties;
import cn.buding.common.util.PackageUtils;

/**
 * images native buffer.
 * 
 * @author renfei
 * 
 */
public class ImageBuffer extends MemBuffer<Bitmap> {

	private static final String TAG = "ImageBuffer";
	private final static int MAX_BUF_SIZE_DEFAULT = 8 * 1024 * 1024;
	private final static int MAX_BUF_COUNT_DEFAULT = 1024;
	private final static int MAX_MEM_SIZE_DEFAULT = (int) (4 * 1024 * 1024);
	private final static int MAX_MEM_COUNT_DEFAULT = 48;
	private boolean mCouldRead = true;
	private Bitmap mEmptyBitmap;
	private int mMaxScale = 2048 * 2048;

	private static ImageBuffer mInstance;

	private static final String DEFAULT_FOLDER = "imgbuffer";

	public static void init(Context context, int maxBufferSize) {
		init(context, GlobalProperties.getFolderPath(context, DEFAULT_FOLDER),
				maxBufferSize);
	}

	public static void init(Context context, String folderPath,
			int maxBufferSize) {
		init(context, FileUtil.getFolder(context, folderPath, true),
				maxBufferSize);
	}

	public static void init(Context context, String folderPath) {
		init(context, folderPath, MAX_BUF_SIZE_DEFAULT);
	}

	public static void init(Context context, File folder) {
		init(context, folder, MAX_BUF_SIZE_DEFAULT);
	}

	public static void init(Context context, File folder, int maxBufferSize) {
		mInstance = new ImageBuffer(context, folder, maxBufferSize);
	}

	private static int getMaxMemSize(Context context) {
		int memClass = (int) (PackageUtils.getMemoryClass(context) * 1024 * 1024 / 4.0f);
		int screenScale = DisplayUtils.getScreenWidth(context)
				* DisplayUtils.getScreenHeight(context) * 4 * 4;
		int maxSize = Math.min(memClass, screenScale);
		if (maxSize < MAX_MEM_SIZE_DEFAULT)
			maxSize = MAX_MEM_SIZE_DEFAULT;
		return maxSize;
	}

	public static ImageBuffer getInstance() {
		if (mInstance == null)
			throw new RuntimeException("must call ImageBuffer.init() first");
		return mInstance;
	}

	private ImageBuffer(Context context, File folder, int maxBufSize) {
		super(context, folder, maxBufSize, MAX_BUF_COUNT_DEFAULT);
		setMaxMemCount(MAX_MEM_COUNT_DEFAULT);
		setMaxMemSize(getMaxMemSize(context));
		setUrlItemFactory(new UrlBitmapFactory());
	}

	private ImageBuffer(Context context, File folder) {
		this(context, folder, MAX_BUF_SIZE_DEFAULT);
	}

	public void setCouldRead(boolean flag) {
		mCouldRead = flag;
	}

	public void setEmptyBitmap(Bitmap emptyBitmap) {
		mEmptyBitmap = emptyBitmap;
	}

	public void setMaxScale(int maxScale) {
		mMaxScale = maxScale;
	}

	public Bitmap readImg(String url, BitmapParams params) {
		if (!mCouldRead)
			return mEmptyBitmap;
		return super.readItem(url, params);
	}

	public Bitmap readImg(String url) {
		return readImg(url, null);
	}

	public void reorderImg(String url, ItemParams params) {
		readFromMem(url, params);
	}

	@Override
	protected Bitmap createItem(String url, File file, ItemParams param) {
		if (file == null)
			return null;
		try {
			BitmapParams bParam = null;
			if (param != null && param instanceof BitmapParams) {
				bParam = (BitmapParams) param;
			}
			int maxScale = mMaxScale;
			if (bParam != null && bParam.maxScale > 0
					&& bParam.maxScale < mMaxScale) {
				maxScale = bParam.maxScale;
			}
			BitmapFactory.Options opts = null;
			if (bParam != null)
				opts = bParam.opts;
			CompressedBitmap cBitmap = BitmapUtils.getAppropriateBitmap(
					mContext, file, maxScale, opts);
			if (cBitmap == null)
				return null;
//			Log.i(TAG, "createItem, " + url + cBitmap.getBitmap().getWidth()
//					+ " * " + cBitmap.getBitmap().getHeight() + ", "
//					+ cBitmap.getBitmap().getConfig().toString());
			Bitmap bitmap = cBitmap.getBitmap();
			if (bParam != null && bParam.filters != null) {
				for (BitmapFilter filter : bParam.filters) {
					Bitmap oldBitmap = bitmap;
					bitmap = filter.onFilter(bitmap);
					oldBitmap.recycle();
				}
			}
			return bitmap;
		} catch (Exception e) {
			Log.e(TAG, "Error in reading " + file.getName(), e);
		}
		return null;
	}

	public static class BitmapParams extends ItemParams {
		public int maxScale;
		public List<BitmapFilter> filters;
		public BitmapFactory.Options opts;

		public BitmapParams() {
			maxScale = 0;
		}

		public void addFilter(BitmapFilter f) {
			if (filters == null)
				filters = new ArrayList<BitmapFilter>();
			filters.add(f);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof BitmapParams) {
				BitmapParams bp = (BitmapParams) o;
				boolean scaleEqual = maxScale == bp.maxScale;
				boolean filtersEqual = filters != null && bp.filters != null
						&& filters.equals(bp.filters) || filters == null
						&& bp.filters == null;
				return scaleEqual && filtersEqual;
			}
			return super.equals(o);
		}

	}

	public interface BitmapFilter {
		public Bitmap onFilter(Bitmap bitmap);
	}

	@Override
	protected void onItemRemoved(
			cn.buding.common.file.MemBuffer.UrlItem<Bitmap> u) {
		super.onItemRemoved(u);
		// recycle the bitmap manually.
		Bitmap b = u.getItemValue();
		if (b != null)
			b.recycle();
	}

	public void readBitmapSync(String url, OnResLoadedListener listener) {
		if (!mCouldRead) {
			listener.onResLoaded(url, new File(""));
			return;
		}
		LoadResThread thread = new LoadResThread(mContext, url, this, listener);
		thread.run();
	}

	/**
	 * read img async, try to download it if the img doesn't exist in local.
	 */
	public LoadResThread readBitmapAsync(String url,
			OnResLoadedListener listener) {
		if (!mCouldRead) {
			listener.onResLoaded(url, new File(""));
			return null;
		}
		return ThreadPool.execute(mContext, url, this, listener);
	}

	@Override
	public synchronized UrlBitmap readFromMem(String url) {
		return (UrlBitmap) super.readFromMem(url);
	}

	@Override
	public synchronized UrlBitmap readFromMem(String url,
			cn.buding.common.file.MemBuffer.ItemParams params) {
		return (UrlBitmap) super.readFromMem(url, params);
	}

	public class UrlBitmapFactory extends UrlItemFactory<Bitmap> {
		@Override
		public cn.buding.common.file.MemBuffer.UrlItem<Bitmap> createUrlItem(
				String url, File file) {
			return new UrlBitmap(url, file);
		}
	}

	/**
	 * wrap a Bitmap and the web url of the bitmap.
	 */
	public static class UrlBitmap extends UrlItem<Bitmap> {
		private int mBitmapSize = 0;

		public UrlBitmap(String url, File file) {
			super(url, file);
		}

		@Override
		public BitmapParams getItemParams() {
			return (BitmapParams) super.getItemParams();
		}

		public Bitmap getImg() {
			return getItemValue();
		}

		@Override
		public Bitmap getItemValue() {
			Bitmap bt = super.getItemValue();
			// XXX why it could be recycled.
			if (bt == null || bt.isRecycled())
				return null;
			return bt;
		}

		@Override
		protected void setItem(Bitmap t) {
			super.setItem(t);
			mBitmapSize = getImgSize();
		}

		public void setUrl(String url) {
			this.mUrl = url;
		}

		/**
		 * @return estimated img size in memory.
		 */
		public int getImgSize() {
			Bitmap b = getImg();
			if (b != null) {
				Bitmap.Config config = b.getConfig();
				int scale = 4;
				if (config != null)
					switch (config) {
					case RGB_565:
					case ARGB_4444:
						scale = 2;
						break;
					case ARGB_8888:
						scale = 4;
						break;
					}
				return b.getWidth() * b.getHeight() * scale;
			}
			return 0;
		}

		@Override
		public long getItemSize() {
			return mBitmapSize;
		}
	}

}
