package be.shouldit.proxy.lib.utils;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import org.apache.http.HttpHost;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.regex.Matcher;

import be.shouldit.proxy.lib.WiFiApConfig;
import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.constants.APLConstants;
import be.shouldit.proxy.lib.constants.APLIntents;
import be.shouldit.proxy.lib.ProxyStatus;
import be.shouldit.proxy.lib.ProxyStatusItem;
import be.shouldit.proxy.lib.R;
import be.shouldit.proxy.lib.enums.CheckStatusValues;
import be.shouldit.proxy.lib.enums.ProxyCheckOptions;
import be.shouldit.proxy.lib.enums.ProxyStatusProperties;
import be.shouldit.proxy.lib.enums.PskType;
import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.reflection.ReflectionUtils;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;
import timber.log.Timber;

public class ProxyUtils
{
    public static void startWifiScan()
    {
        if (APL.getWifiManager() != null && APL.getWifiManager().isWifiEnabled())
        {
            APL.getWifiManager().startScan();
        }
    }

    public static void connectToAP(WiFiApConfig conf) throws Exception
    {
        if (APL.getWifiManager() != null && APL.getWifiManager().isWifiEnabled())
        {
            if (conf != null && conf.getLevel() > -1)
            {
                // Connect to AP only if it's available
                ReflectionUtils.connectToWifi(APL.getWifiManager(), conf.getNetworkId());

                APL.getWifiManager().enableNetwork(conf.getNetworkId(), true);
            }
        }
    }

    public static NetworkInfo getCurrentNetworkInfo()
    {
        NetworkInfo ni = APL.getConnectivityManager().getActiveNetworkInfo();
        return ni;
    }

    public static NetworkInfo getCurrentWiFiInfo()
    {
        NetworkInfo ni = APL.getConnectivityManager().getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return ni;
    }

    public static Boolean isConnectedToWiFi()
    {
        NetworkInfo ni = ProxyUtils.getCurrentWiFiInfo();
        if (ni != null && ni.isAvailable() && ni.isConnected())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static Boolean isConnected()
    {
        NetworkInfo ni = ProxyUtils.getCurrentNetworkInfo();
        if (ni != null && ni.isAvailable() && ni.isConnected())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static String cleanUpSSID(String SSID)
    {
        if (SSID != null)
        {
            if (SSID.startsWith("\""))
                return removeDoubleQuotes(SSID);    // Remove double quotes from SSID
            else
                return SSID;
        }
        else
            return "";  // For safety return always and empty string
    }

    public static String removeDoubleQuotes(String string)
    {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"'))
        {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public static String convertToQuotedString(String string)
    {
        return "\"" + string + "\"";
    }

    public static Intent getProxyIntent()
    {
        if (Build.VERSION.SDK_INT >= 12) // Honeycomb 3.1
        {
            return getAPProxyIntent();
        }
        else
        {
            return getGlobalProxyIntent();
        }
    }

    /**
     * For API < 12
     */
    private static Intent getGlobalProxyIntent()
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.ProxySelector"));

        return intent;
    }

    /**
     * For API >= 12
     */
    private static Intent getAPProxyIntent()
    {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);

        return intent;
    }

    // public static Intent getWebViewWithProxy(Context context, URI uri)
    // {
    // Intent intent = new Intent(context, );
    // intent.putExtra("URI", uri);
    //
    // return intent;
    // }

    public static boolean isHostReachable(Proxy proxy, int timeout)
    {
        Boolean standardResult = standardAPIPingHost(proxy, timeout);

        if (standardResult)
        {
            return true;
        }
        else
        {
            Boolean lowResult = lowLevelPingHost(proxy, timeout);
            return lowResult;
        }
    }

    public static boolean standardAPIPingHost(Proxy proxy, int timeout)
    {
        Boolean result = false;

        try
        {
            InetSocketAddress proxySocketAddress = (InetSocketAddress) proxy.address();
            String socketAddressString = proxySocketAddress.toString();
            if (!TextUtils.isEmpty(socketAddressString) && socketAddressString.contains(":"))
            {
                String host = socketAddressString.split(":")[0];

                if (!TextUtils.isEmpty(host))
                {
                    InetAddress address = InetAddress.getByName(host);
                    if (address != null)
                    {
                        result = address.isReachable(timeout);
                    }
                }
            }
        }
        catch (UnknownHostException e)
        {
            Timber.e(e.toString());
        }
        catch (Exception e)
        {
            Timber.e(e,"Exception during standardAPIPingHost");
        }

        return result;
    }

//	public static void pingMe()
//	{
//
//		try
//		{
//			ByteBuffer sendEvent = ByteBuffer.wrap("Hello".getBytes());
//			ByteBuffer receive = ByteBuffer.allocate("Hello".getBytes().length);
//			//use echo port 7
//			InetSocketAddress socketAddress = new InetSocketAddress("192.168.1.2", 7);
//			DatagramChannel dgChannel = DatagramChannel.open();
//			//we have the channel non-blocking.
//			dgChannel.configureBlocking(false);
//			dgChannel.connect(socketAddress);
//			dgChannel.sendEvent(sendEvent, socketAddress);
//			/*
//			 * it's non-blocking so we need some amount of delay to get the
//			 * response
//			 */
//			Thread.sleep(10000);
//			dgChannel.receive(receive);
//			String response = new String(receive.array());
//			if (response.isSameConfiguration("Hello"))
//			{
//				System.out.println("Ping is alive");
//			}
//			else
//			{
//				System.out.println("No response");
//			}
//
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//		catch (InterruptedException e)
//		{
//			e.printStackTrace();
//		}
//
//	}

    public static boolean lowLevelPingHost(Proxy proxy, int timeout)
    {
        int exitValue;
        Runtime runtime = Runtime.getRuntime();
        Process proc;

        String cmdline = null;
        String proxyAddress = null;

        if (proxy == null)
            return false;

        try
        {
            InetSocketAddress proxySocketAddress = (InetSocketAddress) proxy.address();
            if (proxySocketAddress != null)
            {
                InetAddress inetAddress = proxySocketAddress.getAddress();
                if (inetAddress != null)
                {
                    proxyAddress = inetAddress.getHostAddress();
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "LowLevelPingHost - Exception getting proxyAddress string from InetAddress");
        }

        if (proxyAddress == null)
        {
            try
            {
                InetSocketAddress proxySocketAddress = (InetSocketAddress) proxy.address();
                proxyAddress = proxySocketAddress.toString();
            }
            catch (Exception e)
            {
                Timber.e(e, "LowLevelPingHost - Exception getting proxyAddress string from InetSocketAddress");
            }
        }

        if (proxyAddress != null)
        {
            cmdline = "ping -c 1 -w " + timeout/1000 + " " + proxyAddress;

            try
            {
                proc = runtime.exec(cmdline);
                proc.waitFor();
                exitValue = proc.exitValue();

                Timber.d("Ping exit value: " + exitValue);

                if (exitValue == 0)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
            catch (Exception e)
            {
                Timber.e(e, "LowLevelPingHost - Exception executing PING");
            }
        }
        else
        {
            Timber.w("Cannot find available address to ping the proxy host");
        }

        return false;
    }

    @SuppressLint("NewApi")
    public static String getProxyHost(Proxy proxy)
    {
        SocketAddress sa = proxy.address();
        InetSocketAddress isa = (InetSocketAddress) sa;

        if (Build.VERSION.SDK_INT >= 19)
        {
            return isa.getHostString();
        }
        else
        {
            return isa.getHostName();
        }
    }

    public static int testHTTPConnection(URI uri, Proxy proxy, int timeout)
    {
        int step = 0;
        while (step < 5)
        {
            try
            {
                URL url = uri.toURL();

                if (proxy.type() == Type.HTTP)
                {
                    SocketAddress sa = proxy.address();
                    InetSocketAddress isa = (InetSocketAddress) sa;

                    String proxyHost = getProxyHost(proxy);
                    System.setProperty("http.proxyHost", proxyHost);
                    System.setProperty("http.proxyPort", String.valueOf(isa.getPort()));
                }
                else
                {
                    System.setProperty("http.proxyHost", "");
                    System.setProperty("http.proxyPort", "");
                }

                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(timeout);
                httpURLConnection.setConnectTimeout(timeout);

                int result = httpURLConnection.getResponseCode();
                return result;
            }
            catch (Exception e)
            {
                Timber.w(e.toString());
            }

            step++;

            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                Timber.e(e, "InterruptedException during thread sleep on testHTTPConnection");
                return -1;
            }
        }

        return -1;
    }

    public static HttpAnswer getHttpAnswerURI(URI uri, Proxy proxy, int timeout) throws IOException
    {
        URL url = uri.toURL();
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(proxy);

        httpURLConnection.setReadTimeout(timeout);
        httpURLConnection.setConnectTimeout(timeout);

        HttpAnswer answer = new HttpAnswer(httpURLConnection);
        return answer;
    }

    public static String getURI(URI uri, Proxy proxy, int timeout) throws IOException
    {
        HttpAnswer answer = getHttpAnswerURI(uri, proxy, timeout);

        if (answer.getStatus() == HttpURLConnection.HTTP_OK)
        {
            return answer.getBody();
        }
        else
        {
            throw new IOException("INCORRECT RETURN CODE: " + answer.getStatus());
        }
    }

    public static boolean canGetWebResources(Proxy proxy, int timeout)
    {
        try
        {
            // TODO: add better method to check web resources
            int result = testHTTPConnection(new URI("http://www.un.org/"), proxy, timeout);
//            int rawresult = testHTTPConnection(new URI("http://157.150.34.32"), WiFiApConfig, timeout);

            switch (result)
            {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_CREATED:
                case HttpURLConnection.HTTP_NO_CONTENT:
                case HttpURLConnection.HTTP_NOT_AUTHORITATIVE:
                case HttpURLConnection.HTTP_ACCEPTED:
                case HttpURLConnection.HTTP_PARTIAL:
                case HttpURLConnection.HTTP_RESET:
                    return true;

                default:
                    return false;
            }
        }
        catch (URISyntaxException e)
        {
            Timber.w(e.toString());
//            APL.getEventsReporter().sendEvent(e);
        }

        return false;
    }

    /**
     * Try to set the Proxy Settings for active WebViews. This works only for devices with API version < 12.
     */
    public static void setWebViewProxy(Context context, WiFiApConfig wiFiAPConfig)
    {
        if (Build.VERSION.SDK_INT >= 12)
        {
            // On newer devices do nothing!
        }
        else
        {
            // On older devices try to set or clear the proxy settings, depending on the argument configuration
            try
            {
                if (wiFiAPConfig != null && wiFiAPConfig.getProxyType() == Type.HTTP && APL.getDeviceVersion() < 12)
                {
                    setProxy(context, wiFiAPConfig.getProxyIPHost(), wiFiAPConfig.getProxyPort());
                }
                else
                {
                    resetProxy(context);
                }
            }
            catch (Exception e)
            {
                Timber.e(e,"Exception setting proxy for WebView");
            }
        }
    }

    public static void resetProxy(Context ctx) throws Exception
    {
        Object requestQueueObject = getRequestQueue(ctx);
        if (requestQueueObject != null)
        {
            setDeclaredField(requestQueueObject, "mProxyHost", null);
        }
    }

    private static boolean setProxy(Context ctx, String host, int port)
    {
        boolean ret = false;
        try
        {
            Object requestQueueObject = getRequestQueue(ctx);
            if (requestQueueObject != null)
            {
                // Create Proxy config object and set it into request Q
                HttpHost httpHost = new HttpHost(host, port, "http");
                setDeclaredField(requestQueueObject, "mProxyHost", httpHost);
                // LogWrapper.d("Webkit Setted Proxy to: " + host + ":" + port);
                ret = true;
            }
        }
        catch (Exception e)
        {
           Timber.e(e,"Exception setting WebKit proxy settings");
        }
        return ret;
    }

    @SuppressWarnings("rawtypes")
    private static Object GetNetworkInstance(Context ctx) throws ClassNotFoundException
    {
        Class networkClass = Class.forName("android.webkit.Network");
        return networkClass;
    }

    private static Object getRequestQueue(Context ctx) throws Exception
    {
        Object ret = null;
        Object networkClass = GetNetworkInstance(ctx);
        if (networkClass != null)
        {
            Object networkObj = invokeMethod(networkClass, "getInstance", new Object[]{ctx}, Context.class);
            if (networkObj != null)
            {
                ret = getDeclaredField(networkObj, "mRequestQueue");
            }
        }
        return ret;
    }

    private static Object getDeclaredField(Object obj, String name) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        return out;
    }

    private static void setDeclaredField(Object obj, String name, Object value) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    @SuppressWarnings("rawtypes")
    private static Object invokeMethod(Object object, String methodName, Object[] params, Class... types) throws Exception
    {
        Object out = null;
        Class c = object instanceof Class ? (Class) object : object.getClass();

        if (types != null)
        {
            Method method = c.getMethod(methodName, types);
            out = method.invoke(object, params);
        }
        else
        {
            Method method = c.getMethod(methodName);
            out = method.invoke(object);
        }
        return out;
    }

    public static SecurityType getSecurity(WifiConfiguration config)
    {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK))
        {
            return SecurityType.SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X))
        {
            return SecurityType.SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SecurityType.SECURITY_WEP : SecurityType.SECURITY_NONE;
    }

    public static SecurityType getSecurity(ScanResult result)
    {
        SecurityType security = SecurityType.SECURITY_NONE;

        if (result != null && result.capabilities != null)
        {
            if (result.capabilities.contains("WEP"))
            {
                security = SecurityType.SECURITY_WEP;
            }
            else if (result.capabilities.contains("PSK"))
            {
                security = SecurityType.SECURITY_PSK;
            }
            else if (result.capabilities.contains("EAP"))
            {
                security = SecurityType.SECURITY_EAP;
            }
        }

        return security;
    }

    public static String getSecurityString(WiFiApConfig conf, Context ctx, boolean concise)
    {
        if (conf != null)
        {
            return getSecurityString(conf.getSecurityType(), conf.getPskType(), ctx, true);
        }
        else
            return "";
    }

    public static String getSecurityString(SecurityType security, PskType pskType, Context context, boolean concise)
    {
        switch (security)
        {
            case SECURITY_EAP:
                return concise ? context.getString(R.string.wifi_security_short_eap) : context.getString(R.string.wifi_security_eap);
            case SECURITY_PSK:
                switch (pskType)
                {
                    case WPA:
                        return concise ? context.getString(R.string.wifi_security_short_wpa) : context.getString(R.string.wifi_security_wpa);
                    case WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa2) : context.getString(R.string.wifi_security_wpa2);
                    case WPA_WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2) : context.getString(R.string.wifi_security_wpa_wpa2);
                    case UNKNOWN:
                    default:
                        return concise ? context.getString(R.string.wifi_security_short_psk_generic) : context.getString(R.string.wifi_security_psk_generic);
                }
            case SECURITY_WEP:
                return concise ? context.getString(R.string.wifi_security_short_wep) : context.getString(R.string.wifi_security_wep);
            case SECURITY_NONE:
            default:
                return concise ? context.getString(R.string.wifi_security_none) : context.getString(R.string.wifi_security_none);
        }
    }

    public static PskType getPskType(ScanResult result)
    {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa)
        {
            return PskType.WPA_WPA2;
        }
        else if (wpa2)
        {
            return PskType.WPA2;
        }
        else if (wpa)
        {
            return PskType.WPA;
        }
        else
        {
            Timber.w("Received abnormal flag string: " + result.capabilities);
            return PskType.UNKNOWN;
        }
    }

    public static void acquireProxyStatus(WiFiApConfig conf, ProxyStatus status)
    {
        acquireProxyStatus(conf, status, ProxyCheckOptions.ALL, APLConstants.DEFAULT_TIMEOUT);
    }

    /**
     * Can take a long time to execute this task. - Check if the proxy is
     * enabled - Check if the proxy address is valid - Check if the proxy is
     * reachable (using a PING) - Check if is possible to retrieve an URI
     * resource using the proxy
     */
    public static void acquireProxyStatus(WiFiApConfig conf, ProxyStatus status, EnumSet<ProxyCheckOptions> checkOptions, int timeout)
    {
        status.clear();
        status.startchecking();
        broadCastUpdatedStatus();

        if (Build.VERSION.SDK_INT >= 12)
        {
            acquireProxyStatusSDK12(conf, status, checkOptions);
        }
        else
        {
            acquireProxyStatusSDK1_11(conf, status, checkOptions);
        }

        if (checkOptions.contains(ProxyCheckOptions.ONLINE_CHECK))
        {
            // Always check if WEB is reachable
            Timber.d("Checking if web is reachable ...");
            status.set(isWebReachable(conf, timeout));
            broadCastUpdatedStatus();
        }
        else
        {
            status.set(ProxyStatusProperties.WEB_REACHABLE, CheckStatusValues.NOT_CHECKED, false, false);
        }
    }

    private static void acquireProxyStatusSDK1_11(WiFiApConfig conf, ProxyStatus status, EnumSet<ProxyCheckOptions> checkOptions)
    {
        // API version <= 11 (Older devices)
        status.set(ProxyStatusProperties.WIFI_ENABLED, CheckStatusValues.NOT_CHECKED, false, false);
        status.set(ProxyStatusProperties.WIFI_SELECTED, CheckStatusValues.NOT_CHECKED, false, false);

        Timber.d("Checking if proxy is enabled ...");
        status.set(isProxyEnabled(conf));
        broadCastUpdatedStatus();

        if (status.getProperty(ProxyStatusProperties.PROXY_ENABLED).result)
        {
            Timber.d("Checking if proxy is valid hostname ...");
            status.set(isProxyValidHostname(conf));
            broadCastUpdatedStatus();

            Timber.d("Checking if proxy is valid port ...");
            status.set(isProxyValidPort(conf));
            broadCastUpdatedStatus();

            if (checkOptions.contains(ProxyCheckOptions.ONLINE_CHECK)
                    && status.getProperty(ProxyStatusProperties.PROXY_VALID_HOSTNAME).result
                    && status.getProperty(ProxyStatusProperties.PROXY_VALID_PORT).result)
            {
                Timber.d("Checking if proxy is reachable ...");
                status.set(isProxyReachable(conf, APLConstants.DEFAULT_TIMEOUT));
                broadCastUpdatedStatus();
            }
            else
            {
                status.set(ProxyStatusProperties.PROXY_REACHABLE, CheckStatusValues.NOT_CHECKED, false, false);
            }
        }
        else
        {
            wifiNotEnabled_DisableChecking(status);
        }
    }

    private static void acquireProxyStatusSDK12(WiFiApConfig conf, ProxyStatus status, EnumSet<ProxyCheckOptions> checkOptions)
    {
        Timber.d("Checking if Wi-Fi is enabled ...");
        status.set(isWifiEnabled(conf));
        broadCastUpdatedStatus();

        if (status.getProperty(ProxyStatusProperties.WIFI_ENABLED).result)
        {
            Timber.d("Checking if Wi-Fi is selected ...");
            status.set(isWifiSelected(conf));
            broadCastUpdatedStatus();

            if (status.getProperty(ProxyStatusProperties.WIFI_SELECTED).result)
            {
                // Wi-Fi enabled & selected
                Timber.d("Checking if proxy is enabled ...");
                status.set(isProxyEnabled(conf));
                broadCastUpdatedStatus();

                if (status.getProperty(ProxyStatusProperties.PROXY_ENABLED).result)
                {
                    Timber.d("Checking if proxy is valid hostname ...");
                    status.set(isProxyValidHostname(conf));
                    broadCastUpdatedStatus();

                    Timber.d("Checking if proxy is valid port ...");
                    status.set(isProxyValidPort(conf));
                    broadCastUpdatedStatus();

                    if (checkOptions.contains(ProxyCheckOptions.ONLINE_CHECK)
                            && status.getProperty(ProxyStatusProperties.PROXY_VALID_HOSTNAME).result
                            && status.getProperty(ProxyStatusProperties.PROXY_VALID_PORT).result)
                    {
                        Timber.d("Checking if proxy is reachable ...");
                        status.set(isProxyReachable(conf, APLConstants.DEFAULT_TIMEOUT));
                        broadCastUpdatedStatus();
                    }
                    else
                    {
                        status.set(ProxyStatusProperties.PROXY_REACHABLE, CheckStatusValues.NOT_CHECKED, false, false);
                    }
                }
                else
                {
                    wifiNotEnabled_DisableChecking(status);
                }
            }
            else
            {
                wifiNotEnabled_DisableChecking(status);
            }
        }
        else
        {
            status.set(ProxyStatusProperties.WIFI_SELECTED, CheckStatusValues.NOT_CHECKED, false, false);
            wifiNotEnabled_DisableChecking(status);
        }
    }

    private static void wifiNotEnabled_DisableChecking(ProxyStatus status)
    {
        status.set(ProxyStatusProperties.PROXY_ENABLED, CheckStatusValues.NOT_CHECKED, false, false);
        status.set(ProxyStatusProperties.PROXY_VALID_HOSTNAME, CheckStatusValues.NOT_CHECKED, false, false);
        status.set(ProxyStatusProperties.PROXY_VALID_PORT, CheckStatusValues.NOT_CHECKED, false, false);
        status.set(ProxyStatusProperties.PROXY_REACHABLE, CheckStatusValues.NOT_CHECKED, false, false);
    }


    private static void broadCastUpdatedStatus()
    {
//        LogWrapper.d(TAG, "Sending broadcast intent: " + APLConstants.APL_UPDATED_PROXY_STATUS_CHECK);
        Intent intent = new Intent(APLIntents.APL_UPDATED_PROXY_STATUS_CHECK);
        // intent.putExtra(APLConstants.ProxyStatus, status);
        APL.getContext().sendBroadcast(intent);
    }

    protected static ProxyStatusItem isWifiEnabled(WiFiApConfig conf)
    {
        ProxyStatusItem result = null;

        if (APL.getWifiManager().isWifiEnabled())
        {
            NetworkInfo ni = APL.getConnectivityManager().getActiveNetworkInfo();
            if (ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI)
            {
                String status = APL.getContext().getString(R.string.status_wifi_enabled);
                result = new ProxyStatusItem(ProxyStatusProperties.WIFI_ENABLED, CheckStatusValues.CHECKED, true, true, status);
            }
            else
            {
                result = new ProxyStatusItem(ProxyStatusProperties.WIFI_ENABLED, CheckStatusValues.CHECKED, false, true, APL.getContext().getString(R.string.status_wifi_enabled_disconnected));
            }
        }
        else
        {
            result = new ProxyStatusItem(ProxyStatusProperties.WIFI_ENABLED, CheckStatusValues.CHECKED, false, true, APL.getContext().getString(R.string.status_wifi_not_enabled));
        }

        return result;
    }

    protected static ProxyStatusItem isWifiSelected(WiFiApConfig conf)
    {
        ProxyStatusItem result = null;

        if (conf.isActive())
        {
            result = new ProxyStatusItem(ProxyStatusProperties.WIFI_SELECTED, CheckStatusValues.CHECKED, true, true, APL.getContext().getString(R.string.status_wifi_selected, conf.getSSID()));
        }
        else
        {
            result = new ProxyStatusItem(ProxyStatusProperties.WIFI_SELECTED, CheckStatusValues.CHECKED, false, true, APL.getContext().getString(R.string.status_wifi_not_selected));
        }

        return result;
    }

    protected static ProxyStatusItem isProxyEnabled(WiFiApConfig conf)
    {
        ProxyStatusItem result;

        if (Build.VERSION.SDK_INT >= 12)
        {
            // On API version > Honeycomb 3.1 (HONEYCOMB_MR1)
            // Proxy is disabled by default on Mobile connection
            ConnectivityManager cm = APL.getConnectivityManager();
            if (cm != null)
            {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE)
                {
                    result = new ProxyStatusItem(ProxyStatusProperties.PROXY_ENABLED, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_proxy_mobile_disabled));
                    return result;
                }
            }
        }

        if (conf.getProxySetting() == ProxySetting.UNASSIGNED || conf.getProxySetting() == ProxySetting.NONE)
        {
            result = new ProxyStatusItem(ProxyStatusProperties.PROXY_ENABLED, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_proxy_disabled));
        }
        else
        {
            // if (proxyHost != null && proxyPort != null)
            // {
            // HTTP or SOCKS proxy
            result = new ProxyStatusItem(ProxyStatusProperties.PROXY_ENABLED, CheckStatusValues.CHECKED, true, APL.getContext().getString(R.string.status_proxy_enabled));
            // }
            // else
            // {
            // result = new ProxyStatusItem(ProxyStatusProperties.PROXY_ENABLED,
            // CheckStatusValues.CHECKED, false);
            // }
        }

        return result;
    }

    public static ProxyStatusItem isProxyValidHostname(WiFiApConfig conf)
    {
        String proxyHost = conf.getProxyHostString();
        return isProxyValidHostname(proxyHost);
    }

    public static ProxyStatusItem isProxyValidHostname(String proxyHost)
    {
        try
        {
            if (TextUtils.isEmpty(proxyHost))
            {
                return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_HOSTNAME, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_hostname_empty));
            }
            else
            {
                Matcher match = APLConstants.HOSTNAME_PATTERN.matcher(proxyHost);
                if (match.matches())
                {
                    String hostnameValidMsg = APL.getContext().getString(R.string.status_hostname_valid);
                    String msg = String.format("%s %s", hostnameValidMsg, proxyHost);
                    return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_HOSTNAME, CheckStatusValues.CHECKED, true, msg);
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception parsing hostname");
        }

        return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_HOSTNAME, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_hostname_notvalid));
    }

    public static ProxyStatusItem isProxyValidExclusionList(WiFiApConfig conf)
    {
        String proxyExclusionList = conf.getProxyExclusionList();
        return isProxyValidExclusionList(proxyExclusionList);
    }

    public static ProxyStatusItem isProxyValidExclusionList(String proxyExclusionList)
    {
        return isProxyValidExclusionList(proxyExclusionList.toLowerCase().split(","));
    }

    public static ProxyStatusItem isProxyValidExclusionList(String[] proxyExclusionList)
    {
        try
        {
            for (int i = 0; i < proxyExclusionList.length; i++)
            {
                String s = proxyExclusionList[i].trim();
                ProxyStatusItem status = isProxyValidExclusionAddress(s);

                if (!status.result)
                {
                    return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_EXCLUSION_LIST, CheckStatusValues.CHECKED, true, APL.getContext().getString(R.string.status_exclusion_list_notvalid));
                }
            }

            String exclusionItemValid = APL.getContext().getString(R.string.status_exclusion_item_valid);
            String msg = String.format("%s %s", exclusionItemValid, TextUtils.join(",", proxyExclusionList));
            return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_EXCLUSION_ITEM, CheckStatusValues.CHECKED, true, msg);
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception parsing exclusion list");
        }

        return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_EXCLUSION_ITEM, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_exclusion_item_notvalid));
    }

    public static ProxyStatusItem isProxyValidExclusionAddress(String proxyExclusionAddress)
    {
        try
        {
//            if (TextUtils.isEmpty(proxyExclusionAddress))
//            {
//                return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_EXCLUSION_ITEM, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_exclusion_item_empty));
//            }
//            else
//            {
            Matcher match = APLConstants.EXCLUSION_PATTERN.matcher(proxyExclusionAddress);
            if (match.matches())
            {
                String msg = String.format("%s %s", APL.getContext().getString(R.string.status_exclusion_item_valid), proxyExclusionAddress);
                return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_EXCLUSION_ITEM, CheckStatusValues.CHECKED, true, msg);
            }
//            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception parsing exclusion address");
        }

        return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_EXCLUSION_ITEM, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_exclusion_item_notvalid));
    }

    public static ProxyStatusItem isProxyValidPort(WiFiApConfig conf)
    {
        Integer proxyPort = conf.getProxyPort();
        return isProxyValidPort(proxyPort);
    }

    public static ProxyStatusItem isProxyValidPort(Integer proxyPort)
    {
        if ((proxyPort == null))
        {
            return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_PORT, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_port_empty));
        }

        if ((proxyPort < 1) || (proxyPort > 65535))
        {
            return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_PORT, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_port_notvalid));
        }

        String msg = String.format("%s %d", APL.getContext().getString(R.string.status_port_valid), proxyPort);
        return new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_PORT, CheckStatusValues.CHECKED, true, msg);
    }

    /**
     * Try to PING the HOST specified in the current proxy configuration
     */
    protected static ProxyStatusItem isProxyReachable(WiFiApConfig conf, int timeout)
    {
        if (conf.getProxy() != null && conf.getProxyType() != Proxy.Type.DIRECT)
        {
            Boolean result = ProxyUtils.isHostReachable(conf.getProxy(), timeout);
            if (result)
            {
                return new ProxyStatusItem(ProxyStatusProperties.PROXY_REACHABLE, CheckStatusValues.CHECKED, true, APL.getContext().getString(R.string.status_proxy_reachable));
            }
            else
            {
                return new ProxyStatusItem(ProxyStatusProperties.PROXY_REACHABLE, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_proxy_not_reachable));
            }
        }
        else
        {
            return new ProxyStatusItem(ProxyStatusProperties.PROXY_REACHABLE, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_proxy_not_valid_informations));
        }
    }

    public static String[] parseExclusionList(String exclusionList)
    {
        String[] exList = null;

        if (TextUtils.isEmpty(exclusionList))
        {
            exList = new String[0];
        }
        else
        {
            String splitExclusionList[] = exclusionList.toLowerCase().split(",");
//            exList = new String[splitExclusionList.length * 2];
            exList = new String[splitExclusionList.length];
            for (int i = 0; i < splitExclusionList.length; i++)
            {
                String s = splitExclusionList[i].trim();
                if (s.startsWith("."))
                    s = s.substring(1);
                exList[i] = s;
//                exList[i * 2] = s;
//                exList[(i * 2) + 1] = "." + s;
            }
        }

        return exList;
    }

    public static void startAndroidWifiSettings(Context ctx)
    {
        Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
        ctx.startActivity(intent);
    }

    public static boolean isValidProxyConfiguration(WiFiApConfig config)
    {
        boolean result = false;
        ProxyStatusItem hostStatus = isProxyValidHostname(config);
        ProxyStatusItem portStatus = isProxyValidPort(config);
        ProxyStatusItem exclStatus = isProxyValidExclusionList(config);

        if (hostStatus.effective && hostStatus.status == CheckStatusValues.CHECKED && hostStatus.result
                && portStatus.effective && portStatus.status == CheckStatusValues.CHECKED && portStatus.result
                && exclStatus.effective && exclStatus.status == CheckStatusValues.CHECKED && exclStatus.result)
        {
            result = true;
        }

        return result;
    }

    protected ProxyStatusItem isWebReachable(WiFiApConfig conf)
    {
        return isWebReachable(conf, APLConstants.DEFAULT_TIMEOUT);
    }

    protected static ProxyStatusItem isWebReachable(WiFiApConfig conf, int timeout)
    {
        Boolean result = ProxyUtils.canGetWebResources(conf.getProxy(), timeout);

        if (result)
        {
            return new ProxyStatusItem(ProxyStatusProperties.WEB_REACHABLE, CheckStatusValues.CHECKED, true, APL.getContext().getString(R.string.status_web_reachable));
        }
        else
        {
            return new ProxyStatusItem(ProxyStatusProperties.WEB_REACHABLE, CheckStatusValues.CHECKED, false, APL.getContext().getString(R.string.status_web_not_reachable));
        }
    }

}
