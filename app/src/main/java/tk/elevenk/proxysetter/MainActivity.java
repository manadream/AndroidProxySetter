package tk.elevenk.proxysetter;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.BitSet;
import java.util.Map;

import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.APLNetworkId;
import be.shouldit.proxy.lib.WiFiApConfig;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;


public class MainActivity extends Activity {

    private static final String HOST = "host", PORT = "port", SSID = "ssid", TAG = "ProxySetterApp",
            CLEAR = "clear", BYPASS = "bypass", RESET_WIFI = "reset-wifi", KEY = "key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        // must have ssid to continue
        if (intent.hasExtra(SSID)) {
            String ssid = intent.getStringExtra(SSID);
            String key = intent.getStringExtra(KEY);
            // Resetting wifi will fix problems with a new wifi network being created by Genymotion
            // that has an identical SSID or a null SSID
            boolean resetWifi;
            try{
                resetWifi = Boolean.parseBoolean(intent.getStringExtra(RESET_WIFI));
            } catch (Exception e) {
                resetWifi = false;
            }

            APL.setup(getApplicationContext());

            APLNetworkId networkId = findNetowrkId(ssid);
            if(networkId == null && resetWifi){
                restartWifi(ssid, key);
                networkId = findNetowrkId(ssid);
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

                WiFiApConfig wiFiApConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
                ProxySetting proxySetting = null;
                int port = 8080;
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
                    if(!setProxy(wiFiApConfig, networkId, proxySetting, host, port, bypass, clearProxy)){
                        if(resetWifi) {
                            Toast.makeText(getApplicationContext(),
                                    "Error: proxy not set. Trying to reset wifi and set again.",
                                    Toast.LENGTH_LONG).show();
                            restartWifi(ssid, key);
                            if (!setProxy(wiFiApConfig, networkId, proxySetting, host, port, bypass, clearProxy)) {
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

                    
                    
                }
            } else {
                Toast.makeText(getApplicationContext(),
                        "Error: proxy not set. Check connection to: " + ssid,
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    "Error: No SSID given",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }

    private boolean setProxy(WiFiApConfig wiFiApConfig, APLNetworkId networkId, 
                             ProxySetting proxySetting, String host, int port, String bypass, 
                             boolean clearProxy){
        
        if(wiFiApConfig != null) {
            try {
                APL.writeWifiAPConfig(wiFiApConfig);
            } catch (Exception e) {
                if (!clearProxy) {
                    Toast.makeText(getApplicationContext(),
                            "APL Error: proxy not set", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "APL Error", e);
                    return false;
                }
            }
        }

        // Get the current config settings to see if proxy was changed
        WiFiApConfig newConfig = APL.getWiFiApConfiguration(APL.getConfiguredNetwork(networkId));
        if(newConfig.getProxySetting().equals(proxySetting)) {
            if (proxySetting.equals(ProxySetting.NONE)) {
                Toast.makeText(getApplicationContext(), "Proxy cleared",
                        Toast.LENGTH_LONG).show();
                return true;
            }
            else if (newConfig.getProxyHost().equals(host)
                    && newConfig.getProxyPort() == port
                    && (newConfig.getProxyExclusionList().isEmpty()
                    || newConfig.getProxyExclusionList().equals(bypass))) {
                Toast.makeText(getApplicationContext(), "Proxy set to " + host + ":" + port
                        + " bypass: " + bypass, Toast.LENGTH_LONG).show();
                return true;
            }
            else {
                return false;
            }
        } else {
            return false;
        }
        
    }
    
    private void restartWifi(String ssid, String key){
        try {
            //TODO: implement broadcast receiver to be notified when the network connection has been made
            APL.enableWifi();
            WifiManager wifiManager = APL.getWifiManager();
            if(APL.getConfiguredNetworks().size() > 0) {
                wifiManager.removeNetwork(wifiManager.getConfiguredNetworks().get(0).networkId);
            }
            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.SSID = "\"" + ssid + "\"";
            if(key != null && key.length() >= 8) {
                wifiConfiguration.preSharedKey = "\"" + key + "\"";
            } else {
                BitSet bitSet = new BitSet();
                bitSet.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfiguration.allowedKeyManagement = bitSet;
            }
            int netId = wifiManager.addNetwork(wifiConfiguration);
            wifiManager.saveConfiguration();
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();

            long timeout = 0;
            while(APL.getConfiguredNetworks() == null 
                    || (APL.getConfiguredNetworks() != null 
                        && APL.getConfiguredNetworks().size() == 0)){
                
                Thread.sleep(100);
                timeout += 100;
                if(timeout >= 5000){
                    break;
                }
            }
        } catch (Exception e){
            Log.e(TAG, "Error resetting wifi", e);
        }
    }
    
    private APLNetworkId findNetowrkId(String _ssid){
        Map<APLNetworkId, WifiConfiguration> networks = APL.getConfiguredNetworks();
        for (APLNetworkId aplNetworkId : networks.keySet()) {
            if (aplNetworkId.SSID.equals(_ssid)) {
                return aplNetworkId;
            }
        }
        return null;
    }

}
