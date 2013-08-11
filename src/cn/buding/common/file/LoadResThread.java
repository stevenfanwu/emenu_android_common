package cn.buding.common.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.Thread.State;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.webkit.URLUtil;
import cn.buding.common.net.NetUtil;

/**
 * load image thread.
 */
public class LoadResThread implements Runnable {
	private static final String TAG = "LoadResThread";
	/** the url of the image to load */
	private String mUrl;
	private Context mContext;
	/** the handler list to process when the image is loaded. */
	private List<OnResLoadedListener> mListeners;
	private FileBuffer mFileBuffer;
	private State mState = State.NEW;
	private boolean mForseLoad = false;

	public LoadResThread(Context context, String url, FileBuffer buffer) {
		this(context, url, buffer, null);
	}

	public LoadResThread(Context context, String url, FileBuffer buffer,
			OnResLoadedListener postHandler) {
		this.mUrl = url;
		mFileBuffer = buffer;
		mContext = context;
		mListeners = new ArrayList<OnResLoadedListener>();
		registerListener(postHandler);
	}

	public void setForseLoad(boolean b) {
		mForseLoad = b;
	}

	public void run() {
		if (mUrl == null || mUrl.length() == 0)
			return;
		mState = State.RUNNABLE;
		File file = mFileBuffer.getFileByUrl(mUrl);
		if (file != null && !mForseLoad) {
			executeListeners(file);
			return;
		}
		try {
			Log.d(TAG, "start loading " + mUrl);
			URL url = new URL(mUrl);
			HttpURLConnection conn = NetUtil
					.getHttpUrlConnection(mContext, url);
			conn.connect();
			int contentLength = conn.getHeaderFieldInt("Content-Length", -1);
			InputStream is = conn.getInputStream();
			// Here I restore the remote data in the local outputstream.
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int len;
			while ((len = is.read(buffer)) > 0) {
				bo.write(buffer, 0, len);
				synchronized (this) {
					for (OnResLoadedListener mh : mListeners) {
						mh.onProgressUpdate(bo.size(), contentLength);
					}
				}
			}
			bo.flush();
			is.close();
			conn.disconnect();

			ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
			file = mFileBuffer.writeFile(mUrl, bi);

			// Log.d(TAG, mUrl + " loaded.");
		} catch (Exception e) {
			Log.e(TAG, "Loading res failed: " + mUrl, e);
		}
		executeListeners(file);
	}

	private synchronized void executeListeners(File file) {
		for (OnResLoadedListener mh : mListeners)
			mh.onResLoaded(mUrl, file);
		mState = State.TERMINATED;
	}

	public State getState() {
		return mState;
	}

	public List<OnResLoadedListener> getOnResLoadedListeners() {
		return mListeners;
	}

	public String getUrl() {
		return mUrl;
	}

	public synchronized void registerListener(OnResLoadedListener mh) {
		if (!mListeners.contains(mh) && mh != null)
			mListeners.add(mh);
	}

	public synchronized void registerListener(List<OnResLoadedListener> mh) {
		for (OnResLoadedListener m : mh)
			registerListener(m);
	}

	public synchronized void unregisterListener(OnResLoadedListener mh) {
		mListeners.remove(mh);
	}

	public boolean hasSameUrl(Object o) {
		if (o == null || o.getClass() != this.getClass())
			return false;
		LoadResThread t = (LoadResThread) o;
		String u = t.getUrl();
		if (u == null || u.length() == 0)
			return false;
		return u.equals(mUrl);
	}

	@Override
	public boolean equals(Object o) {
		return hasSameUrl(o);
	}

	public static interface OnResLoadedListener {
		public void onResLoaded(String url, File file);

		public void onProgressUpdate(int currentSize, int totalSize);
	}
}
