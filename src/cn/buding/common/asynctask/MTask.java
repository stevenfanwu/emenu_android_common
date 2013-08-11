package cn.buding.common.asynctask;

import android.content.Context;

public class MTask extends TaskWraper<Void, Object, Object> {
	protected Context mContext;

	public MTask(Context context) {
		this(context, null);
	}

	public MTask(Context context, MTask task) {
		super(task);
		mContext = context;
	}
}
