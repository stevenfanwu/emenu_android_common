package cn.buding.common.util;

import android.content.Context;

public class GlobalProperties {
	private static String ROOT_PATH = null;

	public static String getRootPath(Context context) {
		if (ROOT_PATH != null)
			return ROOT_PATH;
		else
			return "." + context.getPackageName();
	}

	public static void setRootPath(String rootPath) {
		ROOT_PATH = rootPath;
	}

	public static String getFolderPath(Context context, String folder) {
		return getRootPath(context) + "/" + folder;
	}
}
