package cn.buding.common.asynctask;

import java.util.Date;

import cn.buding.common.exception.ECode;
import cn.buding.common.util.NTPTime;
import cn.buding.common.util.PreferenceHelper;
import cn.buding.common.util.Utils;
import android.content.Context;

public class PeriodTask extends MTask {
	protected static final long PRROID_NONE = Long.MIN_VALUE;
	protected static final long PEROID_DAYILY = PreferenceHelper.MAX_DATA_AVAILABLE_TIME_DAILY;
	protected static final long PEROID_WEEKLY = PreferenceHelper.MAX_DATA_AVAILABLE_TIME_WEEKLY;
	protected static final long PEROID_MONTYLY = PreferenceHelper.MAX_DATA_AVAILABLE_TIME_MONTYLY;
	protected static final long PEROID_FOREVER = Long.MAX_VALUE;
	protected static final long NOTICE_UPDATE_PERIOD_A_TIME = 3 * 3600 * 1000;
	private long mPeriod;
	/** if this is true, the task will be execute discard the time period */
	private boolean mForseExecute;
	private String mPreKey;

	public PeriodTask(Context context, MTask task) {
		super(context, task);
		mPeriod = PRROID_NONE;
		mForseExecute = false;
		mPreKey = "PERIOD_TASK_" + getClass().getName();
	}

	public void setForseExecute(boolean forse) {
		mForseExecute = forse;
	}

	@Override
	protected final Object doInBackground(Void... params) {
		Date outDate = new Date();
		String cache = PreferenceHelper.getHelper(mContext)
				.readPreferenceAndDate(getPreKeyFlag(), outDate);
		if (!mForseExecute && Utils.notNullEmpty(cache)
				&& NTPTime.currentTimeMillis() - outDate.getTime() < mPeriod)
			return ECode.SUCCESS;

		Object result = super.doInBackground(params);
		if (result != null && result.equals(ECode.SUCCESS)) {
			PreferenceHelper.getHelper(mContext).writePreferenceWithDate(
					getPreKeyFlag());
		}
		return result;
	}

	public void clearPreKey() {
		PreferenceHelper.getHelper(mContext).removePreferenceWithDate(
				getPreKeyFlag());
	}

	public void setPreKeyFlag(String key) {
		mPreKey = key;
	}

	public String getPreKeyFlag() {
		return mPreKey;
	}

	protected void setPeriod(long period) {
		mPeriod = period;
	}

}
