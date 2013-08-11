package cn.buding.common.file;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.util.Log;
import cn.buding.common.util.NTPTime;

/**
 * a buffer class for file IO. handle the lifecyle of each file in one folder.
 */
public class FileBuffer {
	private static final String TAG = "FileBuffer";
	protected File mFolder;
	protected static final int DEFAULT_MAX_BUFFER_SIZE = (int) (3 * 1024 * 1024);
	protected static final int DEFAULT_MAX_FILE_COUNT = 1024;
	private int mMaxBufferSize;
	private int mMaxFileCount;
	private long mMaxFileAvailableTime = 365 * 24 * 3600 * 1000;
	protected Context mContext;
	private Thread mInitThread;

	/**
	 * @param bufferFolder
	 *            The folder of the file buffer
	 * @param maxBufferSize
	 *            The max file size in the folder
	 * @param maxFileCount
	 *            The max file count in this folder.
	 */
	protected FileBuffer(Context context, File bufferFolder, int maxBufferSize,
			int maxFileCount) {
		mContext = context.getApplicationContext();
		mFolder = bufferFolder;
		mMaxBufferSize = maxBufferSize;
		mMaxFileCount = maxFileCount;
		init();
	}

	protected FileBuffer(Context context, File bufferFolder, int maxBufferSize) {
		this(context, bufferFolder, maxBufferSize, DEFAULT_MAX_FILE_COUNT);
	}

	protected FileBuffer(Context context, File bufferFolder) {
		this(context, bufferFolder, DEFAULT_MAX_BUFFER_SIZE);
	}

	protected void setMaxFileAvailableTime(long availeTime) {
		if (availeTime <= 0)
			return;
		mMaxFileAvailableTime = availeTime;
	}

	/** init buffer, delete half files if exceed MaxBufferSize */
	protected void init() {
		if (mInitThread != null)
			return;
		mInitThread = new Thread() {
			public void run() {
				List<File> bufferFiles = getAllBufferedFiles();
				int curSize = 0;
				for (File f : bufferFiles) {
					curSize += f.length();
				}
				if (curSize > mMaxBufferSize
						|| bufferFiles.size() > mMaxFileCount) {
					Log.d(TAG, "Delete half files.");
					Collections.sort(bufferFiles, new FileTimeComparator());
					int halfCount = bufferFiles.size() / 2;
					for (int i = 0; i < halfCount; i++) {
						bufferFiles.get(i).delete();
						try {
							Thread.sleep(100);
						} catch (Exception e) {
							Log.e(TAG, "fuck:", e);
							break;
						}
					}
				}
				mInitThread = null;
			};
		};
		mInitThread.start();
	}

	public File getFileByUrl(String url) {
		File file = FileUtil.getExistFile(mFolder, url);
		if (file != null) {
			if (NTPTime.currentTimeMillis() - file.lastModified() > mMaxFileAvailableTime) {
				file.delete();
				return null;
			}
		}
		return file;
	}

	public File getFileByName(String fileName) {
		if (mFolder == null)
			return null;
		return new File(mFolder, fileName);
	}

	public File writeFile(String url, InputStream is) {
		return FileUtil.writeFile(mFolder, url, is);
	}

	public File writeFile(String url, InputStream is, int maxFlowRate) {
		return FileUtil.writeFile(mFolder, url, is, maxFlowRate);
	}

	public File writeFileByName(String fileName, InputStream in) {
		return FileUtil.writeFileByName(mFolder, fileName, in);
	}

	public File writeFileByName(String fileName, String content) {
		return FileUtil.writeFileByName(mFolder, fileName, content);
	}

	public void deleteAllFile() {
		List<File> files = getAllBufferedFiles();
		for (File f : files)
			f.delete();
	}

	public List<File> getAllBufferedFiles() {
		List<File> files = new ArrayList<File>();
		return FileUtil.getAllBufferedFiles(null, mFolder, files);
	}

	public long getAllBufferFileSize() {
		List<File> files = new ArrayList<File>();
		FileUtil.getAllBufferedFiles(null, mFolder, files);
		long res = 0;
		for (File f : files) {
			res += f.length();
		}
		return res;
	}


	/**
	 * order by file lastModefied time desc
	 */
	public class FileTimeComparator implements Comparator<File> {

		public int compare(File object1, File object2) {
			long i1 = object1.lastModified();
			long i2 = object2.lastModified();
			if (i1 > i2)
				return -1;
			else if (i1 < i2)
				return 1;
			return 0;
		}
	}

}
