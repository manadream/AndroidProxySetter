package tk.elevenk.proxysetter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.APLNetworkId;
import be.shouldit.proxy.lib.WiFiApConfig;
import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;


public class MainActivity extends Activity {

	private static final String TAG = "ProxySetterApp";
	private static Activity thisActivity;

	public MainActivity(){
		thisActivity = this;
	}

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
	public static void showPopup(final String msg) {
		thisActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(thisActivity.getBaseContext(), msg, Toast.LENGTH_SHORT).show();
				Log.d(TAG, msg);

			}
		});
	}
}
