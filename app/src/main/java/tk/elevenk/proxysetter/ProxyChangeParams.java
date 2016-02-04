/*
 * Copyright (c) 2016 John Paul Krause.
 * ProxyChangeParams.java is part of AndroidProxySetter.
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

import android.content.Intent;
import android.util.Log;
import be.shouldit.proxy.lib.APLNetworkId;
import be.shouldit.proxy.lib.WiFiApConfig;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;

/**
 * @author John Krause
 */
public class ProxyChangeParams {

	// ***************************************************
	// CONSTANTS
	// ***************************************************

	public static final String HOST = "host", PORT = "port", SSID = "ssid",
			CLEAR = "clear", BYPASS = "bypass", RESET_WIFI = "reset-wifi", KEY = "key";
	private static final String TAG = "ProxySetterApp";

	// ***************************************************
	// ATTRIBUTES
	// ***************************************************

	private String ssid, key, host, bypass;
	private int port;
	private boolean resetWifi, clearProxy;
	private ProxySetting proxySetting;
	private APLNetworkId networkId;
	private WiFiApConfig wiFiApConfig;

	// ***************************************************
	// METHODS
	// ***************************************************

	public ProxyChangeParams(Intent intent) {
		processIntent(intent);
		processAPLSpecificProperties();
	}

	private void processIntent(Intent intent) {
		ssid = intent.getStringExtra(SSID);
		key = intent.getStringExtra(KEY);
		// Resetting wifi will fix problems with a new wifi network being created by Genymotion
		// that has an identical SSID or a null SSID
		try {
			resetWifi = Boolean.parseBoolean(intent.getStringExtra(RESET_WIFI));
		} catch (Exception e) {
			Log.e(TAG, "Problem getting resetWifi flag", e);
			resetWifi = false;
		}
		host = intent.getStringExtra(HOST);
		bypass = intent.getStringExtra(BYPASS);
		try {
			clearProxy = Boolean.parseBoolean(intent.getStringExtra(CLEAR));
		} catch (Exception e) {
			clearProxy = false;
		}
		try {
			port = Integer.parseInt(intent.getStringExtra(PORT));
		} catch (Exception e) {
			Log.d(TAG, "Invalid port or none given, defaulting to 8080.", e);
			port = 8080;
		}
	}

	private void processAPLSpecificProperties(){
		if (clearProxy) {
			proxySetting = ProxySetting.NONE;
		} else if (host != null) {
			proxySetting = ProxySetting.STATIC;
		} else {
			proxySetting = ProxySetting.UNASSIGNED;
		}
		networkId = null;
		wiFiApConfig = null;
	}

	// ***************************************************
	// ACCESSORS/MODIFIERS
	// ***************************************************


	public String getSsid() {
		return ssid;
	}

	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getBypass() {
		return bypass;
	}

	public void setBypass(String bypass) {
		this.bypass = bypass;
	}

	public boolean isResetWifi() {
		return resetWifi;
	}

	public void setResetWifi(boolean resetWifi) {
		this.resetWifi = resetWifi;
	}

	public boolean isClearProxy() {
		return clearProxy;
	}

	public void setClearProxy(boolean clearProxy) {
		this.clearProxy = clearProxy;
	}

	public ProxySetting getProxySetting() {
		return proxySetting;
	}

	public void setProxySetting(ProxySetting proxySetting) {
		this.proxySetting = proxySetting;
	}

	public APLNetworkId getNetworkId() {
		return networkId;
	}

	public void setNetworkId(APLNetworkId networkId) {
		this.networkId = networkId;
		if(networkId != null && networkId.SSID != null && !networkId.SSID.isEmpty()) {
			this.ssid = networkId.SSID;
		}
	}

	public WiFiApConfig getWiFiApConfig() {
		return wiFiApConfig;
	}

	public void prepareAndSetWiFiApConfig(WiFiApConfig wiFiApConfig) {
		this.wiFiApConfig = wiFiApConfig;
		if(this.wiFiApConfig != null) {
			if (!clearProxy && host != null) {
				this.wiFiApConfig.setProxyHost(host);
				this.wiFiApConfig.setProxyPort(port);
				this.wiFiApConfig.setProxyExclusionString(bypass);
			}
			this.wiFiApConfig.setProxySetting(proxySetting);
		}
	}
}
