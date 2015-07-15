package tk.elevenk.proxysetter;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

/**
 * Async task that handles executing the proxy change request
 */
public class ProxyChangeAsync extends AsyncTask<Object, String, Void> {

	private Activity activity;
	private ProxyChangeExecutor executor;

	private static final String TAG = "ProxySetterApp";

	public ProxyChangeAsync(Activity activity) {
		this.activity = activity;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// init executor and register it to receive wifi state change broadcasts
		executor = new ProxyChangeExecutor(activity, this);
		activity.getApplicationContext().registerReceiver(executor, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	protected Void doInBackground(Object... params) {
		onProgressUpdate("Executing proxy change request...");

		// Looper is needed to handle broadcast messages
		try {
			Looper.prepare();
		} catch (Exception e) {
			Log.e(TAG, "Error starting looper on thread", e);
		}

		executor.executeChange((Intent) params[0]);
		return null;
	}

	@Override
	public void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		final String msg = values[0];
		MainActivity.showPopup(msg);
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		activity.finish();
	}
}