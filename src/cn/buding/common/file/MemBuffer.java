package cn.buding.common.file;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

public abstract class MemBuffer<T> extends FileBuffer {
	private static final String TAG = "MemBuffer";
	protected static final int DEFAULT_MAX_MEM_SIZE = 4 * 1024 * 1024;
	protected static final int DEFAULT_MAX_MEM_COUNT = 64;
	private int mMaxMemSize;
	private int mMaxMemCount;
	protected int mMaxSingleFileSize = 400 * 1024;

	private List<UrlItem<T>> mMemoryItems;
	private int mCurMemorySize = 0;

	private UrlItemFactory<T> mUrlItemFactory;

	private Object mDeleteListLock = new Object();

	protected MemBuffer(Context context, File bufferFolder) {
		this(context, bufferFolder, DEFAULT_MAX_BUFFER_SIZE,
				DEFAULT_MAX_FILE_COUNT);
	}

	protected MemBuffer(Context context, File bufferFolder, int maxBufferSize,
			int maxFileCount) {
		this(context, bufferFolder, maxBufferSize, maxFileCount,
				DEFAULT_MAX_MEM_SIZE, DEFAULT_MAX_MEM_COUNT);
	}

	protected MemBuffer(Context context, File bufferFolder, int maxBufferSize,
			int maxFileCount, int maxMemSize, int maxMemCount) {
		super(context, bufferFolder, maxBufferSize, maxFileCount);
		mMemoryItems = new LinkedList<UrlItem<T>>();
		mMaxMemSize = maxMemSize;
		mMaxMemCount = maxMemCount;
		mUrlItemFactory = new UrlItemFactory<T>();
	}

	public void setUrlItemFactory(UrlItemFactory<T> factory) {
		mUrlItemFactory = factory;
	}

	public void setMaxMemSize(int size) {
		mMaxMemSize = size;
	}

	public void setMaxMemCount(int count) {
		mMaxMemCount = count;
	}

	public UrlItem<T> readUrlItem(String url, ItemParams param) {
		readItem(url, param);
		return readFromMem(url, param);
	}

	public T readItem(String url) {
		return readItem(url, null);
	}

	public T readItem(String url, ItemParams param) {
		if (url == null || url.length() == 0)
			return null;
		UrlItem<T> urlItem = readFromMem(url, param);
		if (urlItem != null) {
			T res = urlItem.getItemValue();
			if (res != null)
				return res;
		}
		File file = getFileByUrl(url);
		if (file == null)
			return null;
		if (file.length() > mMaxSingleFileSize) {
			Log.w(TAG, "File is too large to load, file size: " + file.length()
					+ ", file url:" + url);
			return null;
		}
		UrlItem<T> item = makeUrlItem(url, file, param);
		if (item != null) {
			addToMem(item);
			checkCurMemItems();
			return item.getItemValue();
		}
		return null;
	}

	public synchronized UrlItem<T> readFromMem(String url) {
		return readFromMem(url, null);
	}

	public synchronized UrlItem<T> readFromMem(String url, ItemParams params) {
		if (url == null)
			return null;
		UrlItem<T> res = null;
		for (UrlItem<T> ui : mMemoryItems) {
			if (url.equals(ui.mUrl)) {
				if (ui.paramsEqual(params)) {
					// the item in memory must have the same ItemParams with
					// this request.
					res = ui;
				}
				break;
			}
		}
		reorderItem(res);
		if (res != null && res.isAvailable())
			return res;
		return null;
	}

	private void reorderItem(UrlItem<T> res) {
		if (res != null) {
			if (mMemoryItems.remove(res))
				mMemoryItems.add(res);
		}
	}

	protected synchronized void addToMem(UrlItem<T> urlItem) {
		removeFromMem(urlItem.getUrl());
		mMemoryItems.add(urlItem);
		mCurMemorySize += urlItem.getItemSize();
		logMemSize("addToMem");
	}

	private void logMemSize(String name) {
		String log = String.format("%s: memBuffer size:%d/%d, allocated:%d",
				name, mCurMemorySize / 1000, mMaxMemSize / 1000,
				Debug.getNativeHeapAllocatedSize());
		// Log.d(TAG, log);
	}

	private void checkCurMemItems() {
		synchronized (mDeleteListLock) {
			if (mCurMemorySize > mMaxMemSize
					|| mMemoryItems.size() > mMaxMemCount) {
				deleteHalfMemItems();
			}
		}
	}

	protected void deleteHalfMemItems() {
		int halfCount = mMemoryItems.size() / 2;
		for (int i = 0; i < halfCount; i++) {
			removeFromMem(0);
		}
		logMemSize("deleteHalfMemItems");
	}

	public UrlItem<T> removeFromMem(String url) {
		if (url == null)
			return null;
		UrlItem<T> item = null;
		for (UrlItem<T> u : mMemoryItems) {
			if (url.equals(u.mUrl)) {
				item = u;
				break;
			}
		}
		removeFromMem(item);
		return item;
	}

	protected UrlItem<T> removeFromMem(int index) {
		if (index < 0 || index >= mMemoryItems.size())
			return null;
		UrlItem<T> res = mMemoryItems.get(index);
		removeFromMem(res);
		return res;
	}

	protected synchronized boolean removeFromMem(UrlItem<T> item) {
		if (item != null && mMemoryItems.remove(item)) {
			mCurMemorySize -= item.getItemSize();
			onItemRemoved(item);
			return true;
		}
		return false;
	}

	public void deleteFromMemAndBuf(String url) {
		removeFromMem(url);
		FileUtil.deleteFile(mFolder, url);
	}

	protected void onItemRemoved(UrlItem<T> u) {
	}

	protected abstract T createItem(String url, File file, ItemParams params);

	protected static class ItemParams {
	}

	protected UrlItem<T> makeUrlItem(String url, File file, ItemParams params) {
		T item = createItem(url, file, params);
		if (item == null)
			return null;
		UrlItem<T> res = mUrlItemFactory.createUrlItem(url, file);
		res.setItem(item);
		res.setItemParams(params);
		return res;
	}

	public class UrlItemFactory<T> {
		public UrlItem<T> createUrlItem(String url, File file) {
			return new UrlItem<T>(url, file);
		}
	}

	public static class UrlItem<T> {
		protected String mUrl;
		protected File mFile;
		protected SoftReference<T> mItem;
		protected ItemParams mParams;

		public UrlItem(String url, File file) {
			this.mUrl = url;
			this.mFile = file;
		}

		public void setItemParams(ItemParams p) {
			mParams = p;
		}

		public ItemParams getItemParams() {
			return mParams;
		}

		public String getUrl() {
			return mUrl;
		}

		public File getFile() {
			return mFile;
		}

		public SoftReference<T> getItem() {
			return mItem;
		}

		public T getItemValue() {
			if (mItem == null)
				return null;
			return mItem.get();
		}

		protected void setItem(T t) {
			mItem = new SoftReference<T>(t);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof UrlItem) {
				return mUrl != null && mUrl.equals(((UrlItem) o).mUrl);
			}
			return false;
		}

		public boolean paramsEqual(ItemParams params) {
			if (mParams == null && params == null)
				return true;
			else if (mParams != null) {
				return mParams.equals(params);
			} else {
				return false;
			}
		}

		public boolean isAvailable() {
			return mItem != null && mItem.get() != null;
		}

		public long getItemSize() {
			if (mFile == null)
				return 0;
			return mFile.length();
		}
	}
}
