package cn.buding.common.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;
import cn.buding.common.file.LoadResThread.OnResLoadedListener;

/**
 * thread pool for {@link LoadResThread}
 */
public class ThreadPool {
	public final static int CorePoolSize = 6;
	public final static int MaxiMumPoolSize = CorePoolSize * 2;
	public final static int KeepAliveTime = 500;
	public final static int BlockQueueSize = 20;
	private final static ArrayBlockingQueue<Runnable> mQueue = new ArrayBlockingQueue<Runnable>(
			BlockQueueSize);

	private static RejectedExecutionHandler mRejectHandler = new RejectedExecutionHandler() {

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
			if (!e.isShutdown()) {
				Runnable polled = e.getQueue().poll();
				e.execute(r);
				if (polled instanceof LoadResThread) {
					mThreadMap.remove(((LoadResThread) polled).getUrl());
				}
			}

		}

	};

	private static ThreadPoolExecutor poolExector = new ThreadPoolExecutor(
			CorePoolSize, MaxiMumPoolSize, KeepAliveTime,
			TimeUnit.MILLISECONDS, mQueue, mRejectHandler);

	private static Map<String, LoadResThread> mThreadMap = new HashMap<String, LoadResThread>();

	private static List<String> mTempList = new ArrayList<String>();

	public static synchronized LoadResThread execute(Context context,
			String url, FileBuffer buffer, OnResLoadedListener listener) {
		return execute(context, url, buffer, listener, false);
	}

	public static synchronized LoadResThread execute(Context context,
			String url, FileBuffer buffer, OnResLoadedListener listener,
			boolean forseLoad) {
		mTempList.clear();
		mTempList.addAll(mThreadMap.keySet());
		// clear unreusable thread.
		for (String s : mTempList) {
			LoadResThread t = mThreadMap.get(s);
			if (t == null || t.getState() == Thread.State.TERMINATED) {
				mThreadMap.remove(s);
			}
		}
		LoadResThread thread = mThreadMap.get(url);
		if (thread == null) {
			thread = new LoadResThread(context, url, buffer, listener);
			mThreadMap.put(url, thread);
			poolExector.execute(thread);
		} else {
			thread.registerListener(listener);
		}
		thread.setForseLoad(forseLoad);
		return thread;

	}

	public static boolean isRunningOrWaiting(String url) {
		LoadResThread t = mThreadMap.get(url);
		if (t == null)
			return false;
		switch (t.getState()) {
		case NEW:
			return mQueue.contains(t);
		case RUNNABLE:
			return true;
		case TERMINATED:
			return false;
		}
		return false;
	}

}
