/*
 * Copyright (c) 2016 John Paul Krause.
 * MainActivity.java is part of AndroidProxySetter.
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
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


public class MainActivity extends Activity {

	private static final String TAG = "ProxySetterApp";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if(!validateIntent(intent)) {
			finish();
		} else {
			new ProxyChangeAsync(this).execute(intent);
		}
	}

	private boolean validateIntent(Intent intent) {
		if (!intent.hasExtra(ProxyChangeParams.HOST) && !intent.hasExtra(ProxyChangeParams.CLEAR)) {
			showPopup("Error: No HOST given or not clearing proxy");
			return false;
		}
		if (!intent.hasExtra(ProxyChangeParams.SSID)) {
			showPopup("Warning: No SSID given, setting on the fist one available");
		}
		return true;
	}

	/**
	 * Shows a toast and logs to logcat
	 *
	 * @param msg Message to show/log
	 */
	public void showPopup(final String msg) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
				Log.d(TAG, msg);
			}
		});
	}
}
