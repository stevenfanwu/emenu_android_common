package cn.buding.common.serverlog;

import java.lang.Thread.State;
import java.util.List;

import android.util.Log;
import cn.buding.common.serverlog.BaseLogManager.OnLogStatusChangedListener;

public abstract class AbsLogSender<T> implements OnLogStatusChangedListener<T> {
	private static final String TAG = "AbsLogSender";
	public static final int MODE_SEND_ON_INIT = 0x1;
	public static final int MODE_SEND_ON_LOG_COUNT_ENOUGH = 0x2;
	public static final int MODE_SEND_ON_LOG_ADDED = 0x4;

	protected BaseLogManager<T> mLogManager;
	private int mSendMode;
	private boolean mSendedOnInit = false;

	public AbsLogSender(BaseLogManager<T> logManager) {
		mLogManager = logManager;
		mSendMode = MODE_SEND_ON_INIT | MODE_SEND_ON_LOG_COUNT_ENOUGH;
		mLogManager.setOnLogStatusListener(this);
	}

	public void setSendMode(int mode) {
		mSendMode = mode;
	}

	private boolean isSendOnInit() {
		return (mSendMode & MODE_SEND_ON_INIT) != 0;
	}

	private boolean isSendOnLogEnough() {
		return (mSendMode & MODE_SEND_ON_LOG_COUNT_ENOUGH) != 0;
	}

	private boolean isSendOnLogAdded() {
		return (mSendMode & MODE_SEND_ON_LOG_ADDED) != 0;
	}

	@Override
	public void onLogAdd(T log) {
//		Log.i(TAG, "onLogAdd, " + mSendMode);
		if (isSendOnInit() && !mSendedOnInit) {
			mSendedOnInit = true;
			invokeSender();
			return;
		}

		int size = mLogManager.getLogCount();
		if (isSendOnLogEnough()
				&& size >= mLogManager.getMaxCacheLogCount() / 2) {
			invokeSender();
			return;
		}

		if (isSendOnLogAdded()) {
			invokeSender();
			return;
		}
	}

	private SendLogThread mSendThread;

	public void invokeSender() {
		Log.i(TAG, "invokeSender");
		if (mSendThread == null || mSendThread.getState() == State.TERMINATED) {
			mSendThread = new SendLogThread();
			mSendThread.start();
		} else {
			synchronized (mSendThread) {
				mSendThread.notify();
			}
		}
	}

	private class SendLogThread extends Thread {
		private int mSleepTime = 5;
		private long mStartSleepTime = 2000;

		public void run() {
			long sleepTime = mStartSleepTime;
			for (int i = 0; i < mSleepTime; i++) {
				Log.i(TAG, "SendLogThread run");
				List<T> logs = mLogManager.getUploadLogArray();
				if (logs != null) {
					boolean res = false;
					try {
						res = sendLogToServer(logs);
					} catch (Exception e) {
						Log.e(TAG, "", e);
					}
					if (res) {
						mLogManager.logSuccess();
						break;
					} else {
						mLogManager.logFail();
					}
				}

				if (i < mSleepTime - 1) {
					synchronized (this) {
						try {
							wait(sleepTime);
						} catch (InterruptedException e) {
						}
					}
					mStartSleepTime *= 2;
				}
			}

		}
	}

	protected abstract boolean sendLogToServer(List<T> logs);
}
