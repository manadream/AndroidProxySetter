/*
 * Copyright (c) 2016 John Paul Krause.
 * ProxyChangeExecutor.java is part of AndroidProxySetter.
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.APLNetworkId;
import be.shouldit.proxy.lib.WiFiApConfig;
import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Class that executes the proxy change and listens to wifi state changes
 */
public class ProxyChangeExecutor extends BroadcastReceiver {

	// ***************************************************
	// CONSTANTS
	// ***************************************************

	private static final String TAG = "ProxySetterApp";

	// ***************************************************
	// ATTRIBUTES
	// ***************************************************

	private volatile boolean wifiConnected = false;
	private Activity activity;
	private ProxyChangeAsync proxyChangeAsync;

	// ***************************************************
	// METHODS
	// ***************************************************

	public ProxyChangeExecutor(Activity activity, ProxyChangeAsync proxyChangeAsync) {
		this.activity = activity;
		this.proxyChangeAsync = proxyChangeAsync;
	}

	/**
	 * Receiver for wifi connectivity state changes
	 */
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

	/**
	 * Executes the change with the given intent
	 *
	 * @param intent Intent that contains change details
	 */
	public void executeChange(Intent intent) {
		Log.v(TAG, "Executing Change Request");
		debugIntent(intent, TAG);
		setUpAPL();
		ProxyChangeParams proxyChangeParams = new ProxyChangeParams(intent);
		// find the network id to make proxy change on
		proxyChangeParams.setNetworkId(getAPLNetworkId(proxyChangeParams));
		if (proxyChangeParams.getNetworkId() != null) {
			proxyChangeParams.prepareAndSetWiFiApConfig(APL.getWiFiAPConfiguration(APL.getConfiguredNetwork(proxyChangeParams.getNetworkId())));
			executeProxyChange(proxyChangeParams);
		} else {
			Log.e(TAG, "Error getting network ID. Given Network may not exist. Aborting.");
			showGeneralError(4);
		}

	}

	private void setUpAPL() {
		APL.setup(activity.getApplicationContext());
	}

	private APLNetworkId getAPLNetworkId(ProxyChangeParams params) {
		APLNetworkId networkId = findNetworkId(params);
		if (networkId == null && params.getSsid() != null) {
			if (params.isResetWifi()) {
				// if nothing was picked up and an ssid was given, reset the wifi to try again
				clearAndReconnectWifi(params);
				networkId = findNetworkId(params);
			} else {
				// The device may not be connected to the wifi network, try connecting then find again
				try {
					connectToWifiNetwork(params);
					networkId = findNetworkId(params);
				} catch (Exception e) {
					proxyChangeAsync.onProgressUpdate("Unable to connect to ssid: " + params.getSsid());
					Log.e(TAG, "Error connecting to ssid " + params.getSsid(), e);
				}
			}
		}
		return networkId;
	}

	private APLNetworkId findNetworkId(ProxyChangeParams params) {
		boolean isSecured = params.getKey() != null;
		Map<APLNetworkId, WifiConfiguration> networks = APL.getConfiguredNetworks();
		Log.d(TAG, networks.toString());

		if (params.getSsid() == null) {
			proxyChangeAsync.onProgressUpdate("No SSID specified - trying to find connected.");
			for (Map.Entry<APLNetworkId, WifiConfiguration> entry : networks.entrySet()) {
					if (entry.getValue().status == WifiConfiguration.Status.CURRENT) {	                
						proxyChangeAsync.onProgressUpdate(String.format("No SSID specified, but found %s as the connected network - settings proxy on this!",
							entry.getKey().SSID));
					return entry.getKey();
				}
			}
		}
 
		for (APLNetworkId aplNetworkId : networks.keySet()) {
			if ((aplNetworkId.SSID.equals(params.getSsid()) || params.getSsid() == null)
					&& ((isSecured && !aplNetworkId.Security.equals(SecurityType.SECURITY_NONE))
					|| (aplNetworkId.Security.equals(SecurityType.SECURITY_NONE) && !isSecured))) {
				return aplNetworkId;
			} else {
				Log.d(TAG, String.format("Network ID %s with security %s does not match requirements ssid = %s and isSecured = %b",
						aplNetworkId.SSID, aplNetworkId.Security.toString(), params.getSsid(), isSecured));
			}
		}
		return null;
	}

	private void executeProxyChange(ProxyChangeParams params) {
		if (!setProxy(params)) {
			if (params.isResetWifi()) {
				proxyChangeAsync.onProgressUpdate("Error: proxy not set. Trying to reset wifi and set again.");
				clearAndReconnectWifi(params);
				if (!setProxy(params)) {
					Log.e(TAG, "Error setting proxy. See logs for details");
					showGeneralError(1);
				}
			} else {
				Log.e(TAG, "Error setting wifi and resetWifi is false. See logs for details");
				showGeneralError(2);
			}
		}
	}

	private boolean setProxy(ProxyChangeParams params) {

		if (params.getWiFiApConfig() != null) {
			try {
				APL.writeWifiAPConfig(params.getWiFiApConfig(), 10, 10000);
			} catch (Exception e) {
				if (!params.isClearProxy()) {
					proxyChangeAsync.onProgressUpdate("APL Error: proxy not set");
					Log.e(TAG, "APL Error", e);
					return false;
				}
			}
		}

		// Get the current config settings to see if proxy was changed
		WiFiApConfig newConfig = APL.getWiFiAPConfiguration(APL.getConfiguredNetwork(params.getNetworkId()));
		if (newConfig != null && newConfig.getProxySetting().equals(params.getProxySetting())) {
			if (params.getProxySetting().equals(ProxySetting.NONE)) {
				proxyChangeAsync.onProgressUpdate("Proxy cleared");
				return true;
			} else if (newConfig.getProxyHost().equals(params.getHost())
					&& newConfig.getProxyPort() == params.getPort()
					&& (newConfig.getProxyExclusionList().isEmpty()
					|| newConfig.getProxyExclusionList().equals(params.getBypass()))) {

				proxyChangeAsync.onProgressUpdate("Proxy on " + newConfig.getSSID()
						+ " with security " + newConfig.getSecurityType().name()
						+ " set to " + params.getHost() + ":" + params.getPort()
						+ " bypass: " + params.getBypass());
				try {
					proxyChangeAsync.onProgressUpdate("Checking wifi connectivity...");
					waitForWifiConnectivity();
					proxyChangeAsync.onProgressUpdate("Wifi connected and proxy set!");
				} catch (Exception e) {
					proxyChangeAsync.onProgressUpdate("Warning: Wifi is not connected. Check that the " +
							"correct SSID and key combination were given.");
					Log.e(TAG, "", e);
					clearWifiConfigs();
				}
			} else {
				Log.e(TAG, "Proxy not cleared or does not match proxy settings: " + newConfig);
				return false;
			}
		} else {
			Log.e(TAG, "Config " + newConfig + " doesn't match proxy setting " + params.getProxySetting());
			return false;
		}
		return true;
	}

	/**
	 * Clears the wifi configs and connects to the given network
	 */
	private void clearAndReconnectWifi(ProxyChangeParams params) {
		try {
			clearWifiConfigs();
			connectToWifiNetwork(params);
		} catch (Exception e) {
			Log.e(TAG, "Error resetting wifi", e);
		}
	}

	private void clearWifiConfigs() {
		WifiManager wifiManager = APL.getWifiManager();
		try {
			// remove the existing configurations to ensure that the newly inserted one is the only one
			for (WifiConfiguration wifiConfiguration : wifiManager.getConfiguredNetworks()) {
				wifiManager.removeNetwork(wifiConfiguration.networkId);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error clearing wifi configs", e);
		}
		try {
			APL.enableWifi();
		} catch (Exception e) {
			Log.e(TAG, "Error enabling wifi", e);
		}
		wifiManager.saveConfiguration();
	}

	private void connectToWifiNetwork(ProxyChangeParams params) throws Exception {
		WifiManager wifiManager = APL.getWifiManager();
		// create new config with given ssid and key and connect to it
		WifiConfiguration wifiConfiguration = new WifiConfiguration();
		wifiConfiguration.SSID = "\"" + params.getSsid() + "\"";
		if (params.getKey() != null && params.getKey().length() >= 8) {
			wifiConfiguration.preSharedKey = "\"" + params.getKey() + "\"";
		} else {
			BitSet bitSet = new BitSet();
			bitSet.set(WifiConfiguration.KeyMgmt.NONE);
			wifiConfiguration.allowedKeyManagement = bitSet;
		}
		int netId = wifiManager.addNetwork(wifiConfiguration);
		if (netId < 0) {
			netId = wifiManager.updateNetwork(wifiConfiguration);
			if (netId < 0) {
				proxyChangeAsync.onProgressUpdate("Having trouble resetting wifi, hard resetting...");
				APL.disableWifi();
				APL.enableWifi();
				try {
					waitForWifiConnectivity();
				} catch (Exception e) {
					Log.e(TAG, "Timeout when trying to hard reset wifi", e);
				}
				netId = wifiManager.addNetwork(wifiConfiguration);
				if (netId < 0) {
					throw new RuntimeException("Unable to add or update network configuration for " + params.getSsid());
				}
			}
		}

		wifiManager.saveConfiguration();
		wifiManager.disconnect();
		wifiManager.enableNetwork(netId, true);
		wifiManager.reconnect();
		try {
			waitForWifiConnectivity();
		} catch (Exception e) {
			clearWifiConfigs();
			throw e;
		}
	}


	private void showGeneralError(int code) {
		String errorMessage = "Error: proxy not set. Try clearing the proxy setting manually first. Error Code: " + code;
		proxyChangeAsync.onProgressUpdate(errorMessage);
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

	private void debugIntent(Intent intent, String tag) {
		Log.v(tag, "*Begin Intent Details*");
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
		Log.v(tag, "*End Intent Details*");
	}

}