package cn.buding.common;

import java.io.File;

import cn.buding.common.file.ImageBuffer;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import cn.buding.common.net.BaseHttpsManager;

public class Application extends android.app.Application {
	private static final String TAG = "Application";

	@Override
	public void onCreate() {
		super.onCreate();
		onAppStart();
	}

	protected void onAppStart() {
		BaseHttpsManager.init(this);
		Log.i(TAG, "Application onAppStart");
	}

}
