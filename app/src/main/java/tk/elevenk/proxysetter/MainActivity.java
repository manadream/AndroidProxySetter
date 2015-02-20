package tk.elevenk.proxysetter;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.util.Log;

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
        int port;
        try {
            port = Integer.parseInt(intent.getStringExtra(PORT));
        } catch (Exception e){
            port = 8080;
        }
        boolean clearProxy;
        try{
            clearProxy = Boolean.parseBoolean(intent.getStringExtra(CLEAR));
        } catch (Exception e) {
            clearProxy = false;
        }
        Map<APLNetworkId, WifiConfiguration> networks = APL.getConfiguredNetworks();
        APLNetworkId networkId = null;
        for (APLNetworkId aplNetworkId : networks.keySet()) {
            if (aplNetworkId.SSID.equals(ssid)) {
                networkId = aplNetworkId;
            }
        }
        if (networkId != null) {
            WiFiApConfig wiFiApConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
            if(clearProxy)
                wiFiApConfig.setProxySetting(ProxySetting.NONE);
            else if(host != null){
                wiFiApConfig.setProxySetting(ProxySetting.STATIC);
                wiFiApConfig.setProxyHost(host);
                wiFiApConfig.setProxyPort(port);
                wiFiApConfig.setProxyExclusionString(bypass);
            }
            try {
                APL.writeWifiAPConfig(wiFiApConfig);
            } catch (Exception e) {
                if(!clearProxy)
                    Log.e(TAG, "Unable to set proxy", e);
            }
        } else {
            Log.e(TAG, "Invalid SSID: " + ssid);
        }
        finish();
    }


}
