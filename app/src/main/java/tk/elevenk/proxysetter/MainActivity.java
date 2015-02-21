package tk.elevenk.proxysetter;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Map;

import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.APLNetworkId;
import be.shouldit.proxy.lib.WiFiApConfig;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;


public class MainActivity extends Activity {

    private static final String HOST = "host", PORT = "port", SSID = "ssid", TAG = "ProxySetterApp",
            CLEAR = "clear", BYPASS = "bypass";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        // must have ssid to continue
        if (intent.hasExtra(SSID)) {
            String ssid = intent.getStringExtra(SSID);

            // setup android proxy library and get list of network id's
            APL.setup(getApplicationContext());
            Map<APLNetworkId, WifiConfiguration> networks = APL.getConfiguredNetworks();

            // find the network id associated with the given SSID
            APLNetworkId networkId = null;
            for (APLNetworkId aplNetworkId : networks.keySet()) {
                if (aplNetworkId.SSID.equals(ssid)) {
                    networkId = aplNetworkId;
                }
            }

            if (networkId != null) {
                boolean proceed = true;
                // get the remaining extras from the intent
                String host = intent.getStringExtra(HOST);
                String bypass = intent.getStringExtra(BYPASS);
                boolean clearProxy;
                try {
                    clearProxy = Boolean.parseBoolean(intent.getStringExtra(CLEAR));
                } catch (Exception e) {
                    clearProxy = false;
                }
                int port = 8080;

                WiFiApConfig wiFiApConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
                ProxySetting proxySetting = null;
                if (clearProxy) {
                    proxySetting = ProxySetting.NONE;
                } else if (host != null) {
                    try {
                        port = Integer.parseInt(intent.getStringExtra(PORT));
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(),
                                "Invalid port or none given, defaulting to 8080.",
                                Toast.LENGTH_SHORT).show();
                    }
                    proxySetting = ProxySetting.STATIC;
                    wiFiApConfig.setProxyHost(host);
                    wiFiApConfig.setProxyPort(port);
                    wiFiApConfig.setProxyExclusionString(bypass);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error: proxy not set. No host given or clear flag not set.",
                            Toast.LENGTH_LONG).show();
                    proceed = false;
                }

                if (proceed) {
                    wiFiApConfig.setProxySetting(proxySetting);

                    // Make change using APL
                    try {
                        APL.writeWifiAPConfig(wiFiApConfig);
                    } catch (Exception e) {
                        if (!clearProxy) {
                            Toast.makeText(getApplicationContext(),
                                    "APL Error: proxy not set", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "APL Error", e);
                        }
                    }

                    // Get the current config settings to see if proxy was changed
                    WiFiApConfig newConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
                    if(newConfig.getProxySetting().equals(proxySetting)) {
                        if (proxySetting.equals(ProxySetting.NONE)) {
                            Toast.makeText(getApplicationContext(), "Proxy cleared",
                                    Toast.LENGTH_LONG).show();
                        }
                        else if (newConfig.getProxyHost().equals(host)
                                && newConfig.getProxyPort() == port
                                && (newConfig.getProxyExclusionList().isEmpty()
                                || newConfig.getProxyExclusionList().equals(bypass))) {
                            Toast.makeText(getApplicationContext(), "Proxy set to " + host + ":" + port
                                    + " bypass: " + bypass, Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(getApplicationContext(),
                                    "Error: proxy not set. Try clearing the proxy setting manually first.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Error: proxy not set. Try clearing the proxy setting manually first.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(),
                        "Error: proxy not set. Invalid SSID: " + ssid,
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    "Error: No SSID given",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }


}
