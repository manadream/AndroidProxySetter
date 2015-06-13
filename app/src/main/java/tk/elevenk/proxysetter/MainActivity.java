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

	private static final String HOST = "host", PORT = "port", SSID = "ssid", TAG = "ProxySetterApp",
			CLEAR = "clear", BYPASS = "bypass", RESET_WIFI = "reset-wifi", KEY = "key";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (!intent.hasExtra(HOST) && !intent.hasExtra(CLEAR)) {
			showPopup("Error: No HOST given, stopping");
			finish();
		}
		if (!intent.hasExtra(SSID)) {
			showPopup("Error: No SSID given, setting on the fist one");
		}
		new ProxyChangeAsync().execute(this, intent);
	}

	/**
	 * Shows a toast and logs to logcat
	 *
	 * @param msg Message to show/log
	 */
	public void showPopup(String msg) {
		Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
		Log.e(TAG, msg);
	}

	/**
	 * Async task that handles executing the proxy change request
	 */
	public class ProxyChangeAsync extends AsyncTask<Object, String, Void> {

		private Activity activity;
		private ProxyChangeExecutor executor;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// init executor and register it to receive wifi state change broadcasts
			executor = new ProxyChangeExecutor();
			getApplicationContext().registerReceiver(executor, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}

		@Override
		protected Void doInBackground(Object... params) {
			activity = (Activity) params[0];
			onProgressUpdate("Executing proxy change request...");

			// Looper is needed to handle broadcast messages
			try {
				Looper.prepare();
			} catch (Exception e) {
				Log.e(TAG, "Error starting looper on thread", e);
			}

			executor.executeChange((Intent) params[1]);
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			final String msg = values[0];
			activity.runOnUiThread(new Runnable() {
				public void run() {
					showPopup(msg);
				}
			});

		}

		@Override
		protected void onPostExecute(Void aVoid) {
			activity.finish();
		}

		/**
		 * Class that executes the proxy change and listens to wifi state changes
		 */
		public class ProxyChangeExecutor extends BroadcastReceiver {

			private volatile boolean wifiConnected = false;

			@Override
			public void onReceive(Context context, Intent intent) {
				final ConnectivityManager connMgr = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				final NetworkInfo wifi = connMgr
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				wifiConnected = wifi.isAvailable() && wifi.isConnected();
				Log.d(TAG, "Received broadcast about wifi. Connected = " + wifiConnected);
				debugIntent(intent, TAG);
			}

			private void debugIntent(Intent intent, String tag) {
				Log.v(tag, "action: " + intent.getAction());
				Log.v(tag, "component: " + intent.getComponent());
				Bundle extras = intent.getExtras();
				if (extras != null) {
					for (String key : extras.keySet()) {
						Log.v(tag, "key [" + key + "]: " +
								extras.get(key));
					}
				} else {
					Log.v(tag, "no extras");
				}
			}

			private void executeChange(Intent intent) {
				debugIntent(intent, TAG);
				String ssid = intent.getStringExtra(SSID);
				String key = intent.getStringExtra(KEY);
				// Resetting wifi will fix problems with a new wifi network being created by Genymotion
				// that has an identical SSID or a null SSID
				boolean resetWifi;
				try {
					resetWifi = Boolean.parseBoolean(intent.getStringExtra(RESET_WIFI));
				} catch (Exception e) {
					resetWifi = false;
				}

				APL.setup(getApplicationContext());

				// find the network id to make proxy change on
				APLNetworkId networkId = findNetworkId(ssid, key != null);
				if (networkId == null && resetWifi && ssid != null) {
					restartWifi(ssid, key);
					networkId = findNetworkId(ssid, key != null);
				} else if (ssid != null) {
					try {
						connectToWifiNetwork(ssid, key);
						networkId = findNetworkId(ssid, key != null);
					} catch (Exception e) {
						onProgressUpdate("Unable to connect to ssid: " + ssid);
						Log.e(TAG, "Error connecting to ssid " + ssid, e);
					}
				}
				if (networkId != null) {
					ssid = networkId.SSID;
					// get the remaining extras from the intent
					String host = intent.getStringExtra(HOST);
					String bypass = intent.getStringExtra(BYPASS);
					boolean clearProxy;
					try {
						clearProxy = Boolean.parseBoolean(intent.getStringExtra(CLEAR));
					} catch (Exception e) {
						clearProxy = false;
					}

					WiFiApConfig wiFiApConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
					if (wiFiApConfig != null) {
						boolean proceed = true;
						ProxySetting proxySetting = null;
						int port = 8080;
						if (clearProxy) {
							proxySetting = ProxySetting.NONE;
						} else if (host != null) {
							try {
								port = Integer.parseInt(intent.getStringExtra(PORT));
							} catch (Exception e) {
								onProgressUpdate("Invalid port or none given, defaulting to 8080.");
							}
							proxySetting = ProxySetting.STATIC;
							wiFiApConfig.setProxyHost(host);
							wiFiApConfig.setProxyPort(port);
							wiFiApConfig.setProxyExclusionString(bypass);
						} else {
							onProgressUpdate("Error: proxy not set. No host given or clear flag not set.");
							proceed = false;
						}

						if (proceed) {
							wiFiApConfig.setProxySetting(proxySetting);
							if (!setProxy(wiFiApConfig, networkId, proxySetting, host, port, bypass, clearProxy)) {
								if (resetWifi) {
									onProgressUpdate("Error: proxy not set. Trying to reset wifi and set again.");
									restartWifi(ssid, key);
									if (!setProxy(wiFiApConfig, networkId, proxySetting, host, port, bypass, clearProxy)) {
										showGeneralError(1);
									}
								} else {
									showGeneralError(2);
								}
							}
						}
					} else {
						showGeneralError(3);
					}
				} else {
					showGeneralError(4);
				}

			}

			private boolean setProxy(WiFiApConfig wiFiApConfig, APLNetworkId networkId,
									 ProxySetting proxySetting, String host, int port, String bypass,
									 boolean clearProxy) {

				if (wiFiApConfig != null) {
					try {
						APL.writeWifiAPConfig(wiFiApConfig);
					} catch (Exception e) {
						if (!clearProxy) {
							onProgressUpdate("APL Error: proxy not set");
							Log.e(TAG, "APL Error", e);
							return false;
						}
					}
				}

				// Get the current config settings to see if proxy was changed
				WiFiApConfig newConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
				if (newConfig != null && newConfig.getProxySetting().equals(proxySetting)) {
					if (proxySetting.equals(ProxySetting.NONE)) {
						onProgressUpdate("Proxy cleared");
						return true;
					} else if (newConfig.getProxyHost().equals(host)
							&& newConfig.getProxyPort() == port
							&& (newConfig.getProxyExclusionList().isEmpty()
							|| newConfig.getProxyExclusionList().equals(bypass))) {

						onProgressUpdate("Proxy on " + newConfig.getSSID()
								+ " with security " + newConfig.getSecurityType().name()
								+ " set to " + host + ":" + port
								+ " bypass: " + bypass);
						try {
							onProgressUpdate("Checking wifi connectivity...");
							waitForWifiConnectivity();
							onProgressUpdate("Wifi connected and proxy set!");
						} catch (Exception e) {
							onProgressUpdate("Warning: Wifi is not connected. Check that the " +
									"correct SSID and key combination were given.");
							Log.e(TAG, "", e);
						}
						return true;
					} else {
						return false;
					}
				} else {
					return false;
				}

			}

			private void restartWifi(String ssid, String key) {
				try {
					WifiManager wifiManager = APL.getWifiManager();
					try {
						// remove the existing configurations to ensure that the newly inserted one is the only one
						for (WifiConfiguration wifiConfiguration : wifiManager.getConfiguredNetworks()) {
							wifiManager.removeNetwork(wifiConfiguration.networkId);
						}
					} catch (Exception e) {
						Log.e(TAG, "Error clearing wifi configs", e);
					}
					APL.enableWifi();
					wifiManager.saveConfiguration();

					connectToWifiNetwork(ssid, key);

				} catch (Exception e) {
					Log.e(TAG, "Error resetting wifi", e);
				}
			}

			private void connectToWifiNetwork(String ssid, String key) throws Exception {
				WifiManager wifiManager = APL.getWifiManager();
				// create new config with given ssid and key and connect to it
				WifiConfiguration wifiConfiguration = new WifiConfiguration();
				wifiConfiguration.SSID = "\"" + ssid + "\"";
				if (key != null && key.length() >= 8) {
					wifiConfiguration.preSharedKey = "\"" + key + "\"";
				} else {
					BitSet bitSet = new BitSet();
					bitSet.set(WifiConfiguration.KeyMgmt.NONE);
					wifiConfiguration.allowedKeyManagement = bitSet;
				}
				int netId = wifiManager.addNetwork(wifiConfiguration);
				if (netId < 0) {
					netId = wifiManager.updateNetwork(wifiConfiguration);
					if (netId < 0) {
						onProgressUpdate("Having trouble resetting wifi, hard resetting...");
						APL.disableWifi();
						APL.enableWifi();
						try {
							waitForWifiConnectivity();
						} catch (Exception e) {
							Log.e(TAG, "Timeout when trying to hard reset wifi", e);
						}
						netId = wifiManager.addNetwork(wifiConfiguration);
						if (netId < 0) {
							throw new RuntimeException("Unable to add or update network configuration for " + ssid);
						}
					}
				}

				wifiManager.saveConfiguration();
				wifiManager.disconnect();
				wifiManager.enableNetwork(netId, true);
				wifiManager.reconnect();
				waitForWifiConnectivity();
			}

			private APLNetworkId findNetworkId(String ssid, boolean isSecured) {
				Map<APLNetworkId, WifiConfiguration> networks = APL.getConfiguredNetworks();
				Log.d(TAG, networks.toString());
				for (APLNetworkId aplNetworkId : networks.keySet()) {
					if ((aplNetworkId.SSID.equals(ssid) || ssid == null)
							&& ((isSecured && !aplNetworkId.Security.equals(SecurityType.SECURITY_NONE))
							|| (aplNetworkId.Security.equals(SecurityType.SECURITY_NONE) && !isSecured))) {
						return aplNetworkId;
					} else {
						Log.d(TAG, String.format("NetowrkID %s with security %s does not match requirements ssid = %s and isSecured = %b",
								aplNetworkId.SSID, aplNetworkId.Security.toString(), ssid, isSecured));
					}
				}
				return null;
			}

			private void showGeneralError(int code) {
				String errorMessage = "Error: proxy not set. Try clearing the proxy setting manually first. Error Code: " + code;
				onProgressUpdate(errorMessage);

			}

			private void waitForWifiConnectivity() throws TimeoutException {
				long timeout = 10000;
				long sleepTime = 2000;
				do {
					try {
						Thread.sleep(sleepTime);
					} catch (Exception e) {
						// no-op
					}
					timeout -= sleepTime;
				} while (timeout > 0 && !wifiConnected);
				if (!wifiConnected) {
					throw new TimeoutException("Timeout while waiting for wifi to connect");
				}
			}

		}
	}
}
