package cn.buding.common.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;

import android.util.Log;

public class FileUtil {
	private static final String TAG = "FileUtil";
	/**
	 * \d \w \. \\ \/ _
	 */
	private static final Pattern FileNamePattern = Pattern
			.compile("[^\\d\\w\\.\\\\\\/_]+");

	/**
	 * get the file name of the url. the invalid char in url are all replaced by
	 * '_'
	 */
	protected static String getNameFromUrl(String url) {
		if (url == null)
			return "";
		String res = url;
		try {
			URL u = new URL(url);
			// remove protocol.(http://)
			res = u.getHost() + u.getFile();
		} catch (MalformedURLException e) {
		}

		Matcher m = FileNamePattern.matcher(res);
		res = m.replaceAll("_");
		return res;
	}

	/** get the file path of the url */
	protected static String getPathFromUrl(File folder, String url) {
		if (folder == null)
			return null;
		return folder.getAbsolutePath() + "/" + getNameFromUrl(url);
	}

	public static File getExistFile(File folder, String url) {
		if (url == null || url.length() == 0 || folder == null)
			return null;
		String pathName = getPathFromUrl(folder, url);
		File file = new File(pathName);
		if (file.exists())
			return file;
		return null;
	}

	public static File writeFile(File folder, String url, InputStream is) {
		if (folder == null)
			return null;
		String path = getPathFromUrl(folder, url);
		File file = new File(path);
		writeFile(file, is);
		return file;
	}

	public static boolean writeFile(File file, InputStream is) {
		return writeFile(file, is, false);
	}

	private static Object FolderMKObject = new Object();

	public static boolean writeFile(File file, InputStream is, boolean append) {
		if (file == null)
			return false;
		File parent = file.getParentFile();
		synchronized (FolderMKObject) {
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
		}
		byte[] buffer = new byte[1024];
		try {
			OutputStream writer = new FileOutputStream(file, append);
			int len;
			while ((len = is.read(buffer)) > 0)
				writer.write(buffer, 0, len);
			writer.flush();
			writer.close();
			Log.d(TAG,
					"Write to file: " + file.getName() + ", size:"
							+ file.length() / 1024 + "kb");
			return true;
		} catch (IOException e) {
			Log.e(TAG, "Error in writing file: " + file.getName(), e);
			return false;
		}
	}

	public static boolean writeFile(File file, String s) {
		return writeFile(file, s, false);
	}

	public static boolean writeFile(File file, String s, boolean append) {
		if (s == null)
			return false;
		InputStream stream = new ByteArrayInputStream(s.getBytes());
		return writeFile(file, stream, append);
	}

	/**
	 * @param url
	 *            The url of the remote file.
	 * @param is
	 *            The input stream of the remote file;
	 * @param maxFlowRate
	 *            Max rate of flow.
	 */
	public static File writeFile(File folder, String url, InputStream is,
			int maxFlowRate) {
		if (folder == null)
			return null;
		String path = getPathFromUrl(folder, url);
		File file = new File(path);
		byte[] buffer = new byte[1024];
		try {
			OutputStream writer = new FileOutputStream(file);
			int len;
			long cur = SystemClock.uptimeMillis();
			int sleep;
			long diff;
			while ((len = is.read(buffer)) > 0) {
				writer.write(buffer, 0, len);
				diff = SystemClock.uptimeMillis() - cur;
				sleep = (int) (1024f / maxFlowRate - diff);
				if (sleep < 0)
					sleep = 0;
				try {
					if (sleep > 0)
						Thread.sleep(sleep);
				} catch (InterruptedException e) {
				}
				cur = SystemClock.uptimeMillis();
			}
			writer.flush();
			writer.close();
			Log.d(TAG,
					"Write to file: " + file.getName() + ", size:"
							+ file.length() / 1024 + "kb");
		} catch (IOException e) {
			Log.e(TAG, "Error in writing file: " + url);
		}
		return file;
	}

	public static void appendFileByName(File folder, String fileName, String s) {
		if (folder == null)
			return;
		File file = new File(folder, fileName);
		writeFile(file, s, true);
	}

	public static File writeFileByName(File folder, String name, String s) {
		if (folder == null)
			return null;
		File file = new File(folder, name);
		writeFile(file, s);
		return file;
	}

	public static File writeFileByName(File folder, String name, InputStream is) {
		if (folder == null)
			return null;
		File file = new File(folder, name);
		writeFile(file, is);
		return file;
	}

	/** delete specified file by url. */
	public static void deleteFile(File folder, String url) {
		if (folder == null)
			return;
		File file = getExistFile(folder, url);
		if (file != null) {
			file.delete();
		}
	}

	private static final int BUFFE_LEN = 8 * 1024;

	public static String readFileContent(InputStream in) {
		byte[] res = readFileBytes(in);
		if (res == null)
			return null;
		try {
			return new String(res, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static String readFileContent(File file) {
		try {
			if (file == null)
				return null;
			return readFileContent(new FileInputStream(file));
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	public static byte[] readFileBytes(InputStream in) {
		byte[] buffer = new byte[BUFFE_LEN];
		int len = 0;
		java.io.ByteArrayOutputStream output = new ByteArrayOutputStream(
				BUFFE_LEN);
		try {
			while ((len = in.read(buffer, 0, BUFFE_LEN)) > 0) {
				output.write(buffer, 0, len);
			}
			return output.toByteArray();
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

	public static byte[] readFileBytes(File file) {
		try {
			return readFileBytes(new FileInputStream(file));
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	public static void addNoMedia(File folder) {
		if (folder == null)
			return;
		File nomedia = new File(folder, "/.nomedia");
		try {
			if (!nomedia.exists())
				nomedia.createNewFile();
		} catch (IOException e) {
		}
	}

	public static File getFolder(Context context, String folderName,
			boolean permitInRom) {
		File file = null;
		if (hasSDCard()) {
			file = new File(Environment.getExternalStorageDirectory(),
					folderName);
		} else {
			try {
				if (permitInRom) {
					folderName = folderName.replaceAll("/", "_");
					file = context.getDir(folderName, Context.MODE_PRIVATE);
				}
			} catch (Exception e) {
			}
		}
		try {
			if (file != null)
				file.mkdirs();
		} catch (Exception e) {
		}
		return file;
	}

	public static File getFile(Context context, String folderName,
			String fileName) {
		File folder = getFolder(context, folderName, false);
		if (folder == null)
			return null;
		return new File(folder, fileName);
	}

	public static boolean hasSDCard() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED))
			return true;
		else
			return false;
	}

	public static void copyFile(File srcFile, File desFile) {
		if (srcFile == null || !srcFile.exists())
			return;
		char[] buffer = new char[4096];
		FileReader reader = null;
		FileWriter writer = null;
		try {
			reader = new FileReader(srcFile);
			writer = new FileWriter(desFile);
			int len = 0;
			while ((len = reader.read(buffer, 0, 4096)) > 0) {
				writer.write(buffer, 0, len);
			}
			writer.flush();
		} catch (Exception e) {

		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (Exception e) {
			}
			try {
				if (reader != null)
					writer.close();
			} catch (Exception e) {
			}
		}

	}

	public static List<File> getAllBufferedFiles(final String[] accepts,
			File folder, List<File> res) {
		if (res == null || folder == null)
			return res;
		if (folder.isDirectory()) {
			File[] files = folder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					if (accepts == null)
						return true;
					for (String c : accepts) {
						if (pathname.getName().endsWith(c))
							return true;
					}
					return false;
				}
			});
			for (File f : files) {
				if (f.isFile()) {
					res.add(f);
				} else if (f.isDirectory()) {
					getAllBufferedFiles(accepts, f, res);
				}
			}
		}
		return res;
	}
}
