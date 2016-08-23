/*
 * Copyright (c) 2016 John Paul Krause.
 * ProxyChangeAsync.java is part of AndroidProxySetter.
 *
 * AndroidProxySetter is free software: you can redistribute it and/or modify
 * iit under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndroidProxySetter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndroidProxySetter.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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

	private MainActivity activity;
	private ProxyChangeExecutor executor;

	private static final String TAG = "ProxySetterApp";

	public ProxyChangeAsync(MainActivity activity) {
		this.activity = activity;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// init executor and register it to receive wifi state change broadcasts
		executor = new ProxyChangeExecutor(activity, this);
		activity.getApplicationContext().registerReceiver(executor, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		onProgressUpdate("Executing proxy change request...");
	}

	@Override
	protected Void doInBackground(Object... params) {
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
		activity.showPopup(msg);
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		activity.finish();
	}
}