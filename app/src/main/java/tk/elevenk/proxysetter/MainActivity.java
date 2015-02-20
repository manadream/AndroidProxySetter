package tk.elevenk.proxysetter;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
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
        APL.setup(getApplicationContext());
        String host = intent.getStringExtra(HOST);
        String ssid = intent.getStringExtra(SSID);
        String bypass = intent.getStringExtra(BYPASS);
        boolean clearProxy;
        int port = 8080;
        Map<APLNetworkId, WifiConfiguration> networks = APL.getConfiguredNetworks();
        APLNetworkId networkId = null;
        for (APLNetworkId aplNetworkId : networks.keySet()) {
            if (aplNetworkId.SSID.equals(ssid)) {
                networkId = aplNetworkId;
            }
        }
        if (networkId != null) {
            try {
                clearProxy = Boolean.parseBoolean(intent.getStringExtra(CLEAR));
            } catch (Exception e) {
                clearProxy = false;
            }
            WiFiApConfig wiFiApConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
            ProxySetting proxySetting = null;
            if (clearProxy) {
                proxySetting = ProxySetting.NONE;
                wiFiApConfig.setProxySetting(proxySetting);
            } else if (host != null) {
                try {
                    port = Integer.parseInt(intent.getStringExtra(PORT));
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Invalid port or none given, defaulting to 8080.",
                            Toast.LENGTH_SHORT).show();
                }
                proxySetting = ProxySetting.STATIC;
                wiFiApConfig.setProxySetting(proxySetting);
                wiFiApConfig.setProxyHost(host);
                wiFiApConfig.setProxyPort(port);
                wiFiApConfig.setProxyExclusionString(bypass);
            } else {
                Toast.makeText(getApplicationContext(),
                        "Error: proxy not set. No host given or clear flag not set.",
                        Toast.LENGTH_LONG).show();
            }
            try {
                APL.writeWifiAPConfig(wiFiApConfig);
                WiFiApConfig newConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
                if (newConfig.getProxySetting().equals(proxySetting)) {
                    if (host != null && newConfig.getProxyHost().equals(host)
                            && newConfig.getProxyPort() == port
                            && (newConfig.getProxyExclusionList().isEmpty()
                            || newConfig.getProxyExclusionList().equals(bypass))) {
                        Toast.makeText(getApplicationContext(), "Proxy set to " + host + ":" + port
                                + " bypass: " + bypass, Toast.LENGTH_LONG).show();
                    } else if (proxySetting.equals(ProxySetting.NONE)) {
                        Toast.makeText(getApplicationContext(), "Proxy cleared",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Error: proxy not set. Try clearing the proxy setting manually first.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                if (!clearProxy) {
                    Toast.makeText(getApplicationContext(),
                            "Error: proxy not set", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(getApplicationContext(),
                            "Proxy cleared", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    "Error: proxy not set. Invalid SSID: " + ssid,
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }


}
