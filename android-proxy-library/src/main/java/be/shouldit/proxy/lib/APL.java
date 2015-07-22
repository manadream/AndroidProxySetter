package be.shouldit.proxy.lib;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.shouldit.proxy.lib.constants.APLIntents;
import be.shouldit.proxy.lib.enums.SaveStatus;
import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.logging.TraceUtils;
import be.shouldit.proxy.lib.reflection.ReflectionUtils;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;
import be.shouldit.proxy.lib.utils.ProxyUtils;
import be.shouldit.proxy.lib.utils.SaveResult;
import timber.log.Timber;

/**
 * Main class that contains utilities for getting the proxy configuration of the
 * current or the all configured networks
 */
public class APL
{
    public static final String TAG = APL.class.getSimpleName();

    private static ConnectivityManager mConnManager;
    private static WifiManager mWifiManager;
    private static Context gContext;
    private static boolean sSetupCalled;
    private static int deviceVersion;
    private static TraceUtils traceUtils;

    private static String webIsReachableUrl = "http://www.telize.com/ip";

    public static String getWebIsReachableUrl()
    {
        return webIsReachableUrl;
    }

    public static void setWebIsReachableUrl(String webIsReachableUrl)
    {
        APL.webIsReachableUrl = webIsReachableUrl;
    }

    public static TraceUtils getTraceUtils()
    {
        return traceUtils;
    }

    public static boolean setup(Context context)
    {
        gContext = context;
        deviceVersion = Build.VERSION.SDK_INT;

        // Make sure this is only called once.
        if (sSetupCalled)
        {
            return false;
        }

        sSetupCalled = true;

        traceUtils = new TraceUtils();

        Timber.d("APL setup executed");

        return sSetupCalled;
    }

    public static Context getContext()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        return gContext;
    }

    public static int getDeviceVersion()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        return deviceVersion;
    }

    public static WifiManager getWifiManager()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

//        if (mWifiManager == null)
//        {
        // Always get updated WifiManager
        mWifiManager = (WifiManager) gContext.getSystemService(Context.WIFI_SERVICE);
//        }

        return mWifiManager;
    }

    public static void enableWifi() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        WifiManager wm = getWifiManager();
        wm.setWifiEnabled(true);
    }

    public static void disableWifi() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        WifiManager wm = getWifiManager();
        wm.setWifiEnabled(false);
    }

    public static ConnectivityManager getConnectivityManager()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        if (mConnManager == null)
        {
            mConnManager = (ConnectivityManager) gContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        return mConnManager;
    }

    /**
     * Main entry point to access the proxy settings
     */
    public static Proxy getCurrentProxyConfiguration(URI uri) throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        Proxy proxyConfig;

        if (deviceVersion >= 12) // Honeycomb 3.1
        {
            proxyConfig = getProxySelectorConfiguration(uri);
        }
        else
        {
            proxyConfig = getGlobalProxy();
        }

        /**
         * Set direct connection if no proxyConfig received
         * */
        if (proxyConfig == null)
        {
            proxyConfig = Proxy.NO_PROXY;
        }

        /**
         * Add connection details
         * */
//        ConnectivityManager connManager = (ConnectivityManager) gContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetInfo = connManager.getActiveNetworkInfo();
////		proxyConfig.currentNetworkInfo = activeNetInfo;
//
//        if (activeNetInfo != null)
//        {
//            switch (activeNetInfo.getType())
//            {
//                case ConnectivityManager.TYPE_WIFI:
//                    WifiManager wifiManager = (WifiManager) gContext.getSystemService(Context.WIFI_SERVICE);
//                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//                    List<WifiConfiguration> wifiConfigurations = wifiManager.getConfiguredNetworks();
//                    for (WifiConfiguration wc : wifiConfigurations)
//                    {
//                        if (wc.networkId == wifiInfo.getNetworkId())
//                        {
//                            proxyConfig.ap = new AccessPoint(wc);
//                            break;
//                        }
//                    }
//                    break;
//                case ConnectivityManager.TYPE_MOBILE:
//                    break;
//                default:
//                    throw new UnsupportedOperationException("Not yet implemented support for" + activeNetInfo.getTypeName() + " network type");
//            }
//        }

        return proxyConfig;
    }

    /**
     * For API >= 12: Returns the current proxy configuration based on the URI,
     * this implementation is a wrapper of the Android's ProxySelector class.
     * Just add some other details that can be useful to the developer.
     */
    public static Proxy getProxySelectorConfiguration(URI uri) throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        ProxySelector defaultProxySelector = ProxySelector.getDefault();
        Proxy proxy = null;

        List<Proxy> proxyList = defaultProxySelector.select(uri);
        if (proxyList.size() > 0)
        {
            proxy = proxyList.get(0);
            Timber.d("Current Proxy Configuration: %s", proxy.toString());
        }
        else
            throw new Exception("Not found valid proxy configuration!");

//        ConnectivityManager connManager = (ConnectivityManager) gContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//
//        WiFiApConfig proxyConfig = null;
//        if (proxy != Proxy.NO_PROXY)
//        {
//            proxyConfig = new WiFiApConfig(ProxySetting.STATIC, null, null, null, null);
//        }
//        else
//        {
//            InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
//            proxyConfig = new WiFiApConfig(ProxySetting.NONE, proxyAddress.getHostName(), proxyAddress.getPort(), null, null);
//        }

        return proxy;
    }

    /**
     * Return the current proxy configuration for HTTP protocol
     */
    public static Proxy getCurrentHttpProxyConfiguration() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        URI uri = new URI("http", "wwww.google.it", null, null);
        return getCurrentProxyConfiguration(uri);
    }

    /**
     * Return the current proxy configuration for HTTPS protocol
     */
    public static Proxy getCurrentHttpsProxyConfiguration() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        URI uri = new URI("https", "wwww.google.it", null, null);
        return getCurrentProxyConfiguration(uri);
    }

    /**
     * Return the current proxy configuration for FTP protocol
     */
    public static Proxy getCurrentFtpProxyConfiguration() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        URI uri = new URI("ftp", "google.it", null, null);
        return getCurrentProxyConfiguration(uri);
    }

    /**
     * For API < 12: Get global proxy configuration.
     */
    @Deprecated
    public static Proxy getGlobalProxy()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        Proxy proxyConfig = null;

        ContentResolver contentResolver = gContext.getContentResolver();
        String proxyString = Settings.Secure.getString(contentResolver, Settings.Secure.HTTP_PROXY);

        if (!TextUtils.isEmpty(proxyString) && proxyString.contains(":"))
        {
            String[] proxyParts = proxyString.split(":");
            if (proxyParts.length == 2)
            {
                String proxyAddress = proxyParts[0];
                try
                {
                    int proxyPort = Integer.parseInt(proxyParts[1]);
                    proxyConfig = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort));
                }
                catch (NumberFormatException e)
                {
                    Timber.e(e, "Port is not a number: " + proxyParts[1]);
                }
            }
        }

        return proxyConfig;
    }

    /**
     * Get proxy configuration for Wi-Fi access point. Valid for API >= 12
     */
    @TargetApi(12)
    public static WiFiApConfig getWiFiAPConfiguration(WifiConfiguration wifiConf)
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        WiFiApConfig wiFiApConfig = null;

        try
        {
            APL.getTraceUtils().startTrace(TAG, "getWiFiAPConfiguration", Log.DEBUG);

            Object proxySetting = ReflectionUtils.getProxySetting(wifiConf);

            if (proxySetting != null)
            {
                int ordinal = ((Enum) proxySetting).ordinal();

                if (ordinal == ProxySetting.NONE.ordinal() || ordinal == ProxySetting.UNASSIGNED.ordinal())
                {
                    wiFiApConfig = new WiFiApConfig(wifiConf, ProxySetting.NONE, null, null, "", Uri.EMPTY);
                }
                else if (ordinal == ProxySetting.STATIC.ordinal())
                {
                    Object mHttpProxy = ReflectionUtils.getHttpProxy(wifiConf);

                    if (mHttpProxy != null)
                    {
                        Field mHostField = ReflectionUtils.getField(mHttpProxy.getClass().getDeclaredFields(), "mHost");
                        mHostField.setAccessible(true);
                        String mHost = (String) mHostField.get(mHttpProxy);

                        Field mPortField = ReflectionUtils.getField(mHttpProxy.getClass().getDeclaredFields(), "mPort");
                        mPortField.setAccessible(true);
                        Integer mPort = (Integer) mPortField.get(mHttpProxy);

                        Field mExclusionListField = ReflectionUtils.getField(mHttpProxy.getClass().getDeclaredFields(), "mExclusionList");
                        mExclusionListField.setAccessible(true);
                        String mExclusionList = (String) mExclusionListField.get(mHttpProxy);

                        Timber.d("Read HTTP proxy configuration: '%s:%d' (el: '%s')",mHost,mPort,mExclusionList);

                        wiFiApConfig = new WiFiApConfig(wifiConf, ProxySetting.STATIC, mHost, mPort, mExclusionList, Uri.EMPTY);
                    }
                }
                else if (ordinal == ProxySetting.PAC.ordinal())
                {
                    Object mHttpProxy = ReflectionUtils.getHttpProxy(wifiConf);

                    if (mHttpProxy != null)
                    {
                        Field mPacFileUrlField = ReflectionUtils.getField(mHttpProxy.getClass().getDeclaredFields(), "mPacFileUrl");
                        mPacFileUrlField.setAccessible(true);
                        Uri mPacFileUrl = (Uri) mPacFileUrlField.get(mHttpProxy);

                        Timber.d("Read PAC proxy configuration: '%s'", mPacFileUrl.toString());

                        wiFiApConfig = new WiFiApConfig(wifiConf, ProxySetting.PAC, null, null, null, mPacFileUrl);
                    }
                }
                else
                {
		            Timber.e(new InvalidParameterException(),"Not valid ProxySetting value: " + ordinal);
                }
            }

            if (wiFiApConfig == null)
            {
                Timber.e("Cannot find proxySettings object");
                wiFiApConfig = new WiFiApConfig(wifiConf, ProxySetting.NONE, null, null, "", Uri.EMPTY);
            }

            APL.getTraceUtils().stopTrace(TAG, "getWiFiAPConfiguration", String.format("Got configuration for %s", wiFiApConfig.getAPLNetworkId().toString()), Log.DEBUG);
        }
        catch (Exception e)
        {
            Timber.e(e, "Problem getting WiFiApConfig from WifiConfiguration");
        }

        return wiFiApConfig;
    }

    public static Map<APLNetworkId,WifiConfiguration> getConfiguredNetworks()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        Map<APLNetworkId,WifiConfiguration> networksMap = new HashMap<APLNetworkId, WifiConfiguration>();

        APL.getTraceUtils().startTrace(TAG,"getConfiguredNetworks", Log.DEBUG);
        List<WifiConfiguration> configuredNetworks = getWifiManager().getConfiguredNetworks();
//        APL.getTraceUtils().partialTrace(TAG,"getConfiguredNetworks", "Got configured networks from WifiManager", Log.DEBUG);

        if (configuredNetworks != null)
        {
            Timber.d("Found %d configured Wi-Fi networks", configuredNetworks.size());
            for (WifiConfiguration wifiConf : configuredNetworks)
            {
                APLNetworkId networkId = new APLNetworkId(ProxyUtils.cleanUpSSID(wifiConf.SSID), ProxyUtils.getSecurity(wifiConf));
                networksMap.put(networkId, wifiConf);
//                APL.getTraceUtils().partialTrace(TAG,"getConfiguredNetworks",String.format("Added %s to configured networks map", networkId.toString()),Log.DEBUG);
            }
        }
        else
        {
            Timber.d("NULL configured Wi-Fi networks");
        }

        APL.getTraceUtils().stopTrace(TAG, "getConfiguredNetworks", String.format("Built configured newtworks map (#%d)",networksMap.size()), Log.DEBUG);

        return networksMap;
    }

    public static WifiConfiguration getConfiguredNetwork(int androidNetworkId)
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        WifiConfiguration result=null;

        Map<APLNetworkId,WifiConfiguration> networksMap = getConfiguredNetworks();
        for(WifiConfiguration configuration: networksMap.values())
        {
            if (configuration.networkId == androidNetworkId)
            {
                result = configuration;
            }
        }

        return result;
    }

    public static WifiConfiguration getConfiguredNetwork(APLNetworkId networkId)
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        WifiConfiguration result=null;

        Map<APLNetworkId,WifiConfiguration> networksMap = getConfiguredNetworks();
        if (networksMap.containsKey(networkId))
        {
            result = networksMap.get(networkId);
        }

        return result;
    }

    @TargetApi(12)
    public static Map<APLNetworkId,WiFiApConfig> getWifiAPConfigurations()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        Map<APLNetworkId,WiFiApConfig> WiFiAPConfigs = new HashMap<APLNetworkId, WiFiApConfig>();

        APL.getTraceUtils().startTrace(TAG,"getWifiAPConfigurations", Log.DEBUG);
        Map<APLNetworkId,WifiConfiguration> configuredNetworks = getConfiguredNetworks();
        APL.getTraceUtils().partialTrace(TAG, "getWifiAPConfigurations", "Got configured networks", Log.DEBUG);

        if (configuredNetworks != null)
        {
            for (WifiConfiguration wifiConf : configuredNetworks.values())
            {
                WiFiApConfig conf = getWiFiAPConfiguration(wifiConf);
                WiFiAPConfigs.put(conf.getAPLNetworkId(), conf);
            }
        }

        APL.getTraceUtils().stopTrace(TAG, "getWifiAPConfigurations", "Got WiFiApConfig for configured networks", Log.DEBUG);

        return WiFiAPConfigs;
    }


    static final String WRITE_WIFI_KEY = "saveWifiConfiguration";

    /**
     * Get proxy configuration for Wi-Fi access point. Valid for API >= 12
     */
    @TargetApi(12)
    public static SaveResult writeWifiAPConfig(WiFiApConfig confToSave, int maxAttempt, int timeout) throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        APL.getTraceUtils().startTrace(TAG, WRITE_WIFI_KEY, Log.INFO, true);

        SaveResult result = new SaveResult();
        result.status = SaveStatus.FAILED;
        result.attempts = 0;
        result.elapsedTime = 0L;

        boolean succesfullySaved = false;
        int attempt = 1;
        int sleep = 100;
        int slept = 0;

        Timber.i("Writing WiFiAPConfig to device: %s", confToSave.toShortString());

        if (confToSave.getSecurityType() == SecurityType.SECURITY_EAP)
        {
            Exception e = new Exception("writeConfiguration does not support Wi-Fi security 802.1x");
            throw e;
        }

        WifiManager wifiManager = (WifiManager) APL.getContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled())
        {
            Exception e = new Exception("Wi-Fi should be enabled in order to save a Wi-Fi configuration");
            throw e;
        }

        List<WifiConfiguration> configuredNetworks = null;
        while ((attempt <= maxAttempt) && (slept < timeout))
        {
            configuredNetworks = wifiManager.getConfiguredNetworks();
            if (configuredNetworks != null)
            {
                break;
            }

            attempt++;

            try
            {
                Thread.sleep(sleep);
                slept += sleep;
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        if (configuredNetworks == null)
            throw new Exception(String.format("Got a NULL result from WifiManager getConfiguredNetworks method after %d attempts", attempt));

        if (configuredNetworks.size() == 0)
            throw new Exception("Cannot find any configured network to edit into the device");

        WifiConfiguration selectedConfiguration = null;
        for (WifiConfiguration conf : configuredNetworks)
        {
            if (conf.networkId == confToSave.getNetworkId())
            {
                selectedConfiguration = conf;
                break;
            }
        }

        if (selectedConfiguration != null)
        {
            WifiConfiguration newConf = ReflectionUtils.setProxyFieldsOnWifiConfiguration(confToSave, selectedConfiguration);
            APL.getTraceUtils().partialTrace(TAG, WRITE_WIFI_KEY, "Set proxy fields on WifiConfiguration", Log.INFO);

            ReflectionUtils.saveWifiConfiguration(wifiManager, newConf);
            APL.getTraceUtils().partialTrace(TAG, WRITE_WIFI_KEY, "Save configuration to device", Log.INFO);

            /***************************************************************************************
             * TODO: improve method adding callback in order to return the result of the operation
             */
            while ((attempt <= maxAttempt) && (slept < timeout))
            {
                try
                {
                    Thread.sleep(sleep);
                    slept += sleep;
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                WifiConfiguration savedWifiConfig = APL.getConfiguredNetwork(newConf.networkId);
                WiFiApConfig savedConf = APL.getWiFiAPConfiguration(savedWifiConfig);
                succesfullySaved = confToSave.isSameConfiguration(savedConf);

                if (succesfullySaved)
                {
                    confToSave.updateProxyConfiguration(savedConf);
                    break;
                }
                else
                {
                    attempt++;
                    Timber.w("Problem saving configuration to device after #%d attempt. Try again ...", attempt);
                    ReflectionUtils.saveWifiConfiguration(wifiManager, newConf);
                }
            }

            result.attempts = attempt;
            result.elapsedTime = APL.getTraceUtils().totalElapsedTime(WRITE_WIFI_KEY);
            result.status = succesfullySaved? SaveStatus.SAVED : SaveStatus.FAILED;

            if (!succesfullySaved)
            {
                throw new Exception(String.format("Cannot save proxy configuration after %s attempt", attempt));
            }
            /**************************************************************************************/

            APL.getTraceUtils().stopTrace(TAG, WRITE_WIFI_KEY, Log.INFO);
            confToSave.getStatus().clear();

            Timber.d("Succesfully updated configuration %s, after %d attempt", confToSave.toShortString(), attempt);

            Timber.i("Sending broadcast intent: " + APLIntents.APL_UPDATED_PROXY_CONFIGURATION);
            Intent intent = new Intent(APLIntents.APL_UPDATED_PROXY_CONFIGURATION);
            APL.getContext().sendBroadcast(intent);
        }
        else
        {
            throw new Exception("Cannot find selected configuration among configured networks during writing to the device: " + confToSave.toShortString());
        }

        return result;
    }

    /**
     * Force a crash into APL in order to test CrashReporting for library
     * */
    public static void crash()
    {
        Map<String,String> map = new HashMap<String, String>();

        Timber.v("Test bug reporting log 0");
        Timber.i("Test bug reporting log 1");
        Timber.w("Test bug reporting log 2");
        Timber.d("Test bug reporting log 3");
        Timber.e(new Exception(),"EXCEPTION ONLY FOR TEST");

        // Force a CRASH
        throw new RuntimeException("APL forced to crash!");
    }
}
