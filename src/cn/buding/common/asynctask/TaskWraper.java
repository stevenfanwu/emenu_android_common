package cn.buding.common.asynctask;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;

@TargetApi(3)
public abstract class TaskWraper<A, B, C> {
	private ConnectorTask mConnector;
	private TaskWraper<A, B, C> mTask;

	public TaskWraper(TaskWraper<A, B, C> task) {
		mConnector = new ConnectorTask();
		mTask = task;
	}

	public Status getStatus() {
		return mConnector.getStatus();
	}

	public boolean isCanceled() {
		return mConnector.isCancelled();
	}

	public final boolean cancel(boolean mayInterruptIfRunning) {
		return mConnector.cancel(mayInterruptIfRunning);
	}

	public final C get() throws InterruptedException, ExecutionException {
		return mConnector.get();
	}

	public final C get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return mConnector.get(timeout, unit);
	}

	public final AsyncTask<A, B, C> execute(A... params) {
		return mConnector.execute(params);
	}

	public void publishProgress(B... values) {
		mConnector.publishProgressM(values);
	}

	protected C doInBackground(A... params) {
		if (mTask != null)
			return mTask.doInBackground(params);
		return null;
	}

	protected void onPreExecute() {
		if (mTask != null)
			mTask.onPreExecute();
	}

	protected void onPostExecute(C result) {
		if (mTask != null)
			mTask.onPostExecute(result);
	}

	protected void onProgressUpdate(B... values) {
		if (mTask != null)
			mTask.onProgressUpdate(values);
	}

	protected void onCancelled() {
		if (mTask != null)
			mTask.onCancelled();
	}

	private class ConnectorTask extends AsyncTask<A, B, C> {

		protected C doInBackground(A... params) {
			return TaskWraper.this.doInBackground(params);
		};

		@Override
		protected void onCancelled() {
			TaskWraper.this.onCancelled();
		}

		protected void onPostExecute(C result) {
			TaskWraper.this.onPostExecute(result);
		};

		@Override
		protected void onPreExecute() {
			TaskWraper.this.onPreExecute();
		}

		protected void onProgressUpdate(B... values) {
			TaskWraper.this.onProgressUpdate(values);
		};

		public void publishProgressM(B... values) {
			super.publishProgress(values);
		}
	}
}
