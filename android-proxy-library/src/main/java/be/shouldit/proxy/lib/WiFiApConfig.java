package be.shouldit.proxy.lib;

import android.annotation.TargetApi;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.UUID;

import be.shouldit.proxy.lib.enums.CheckStatusValues;
import be.shouldit.proxy.lib.enums.PskType;
import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;
import be.shouldit.proxy.lib.utils.ProxyUtils;
import timber.log.Timber;

public class WiFiApConfig implements Comparable<WiFiApConfig>, Serializable
{
    private static final String TAG = WiFiApConfig.class.getSimpleName();

    private final UUID id;
    private final APLNetworkId internalWifiNetworkId;
    private ProxyStatus status;
    private ProxySetting proxySetting;
    private String proxyHost;
    private Integer proxyPort;
    private String stringProxyExclusionList;
    private String[] parsedProxyExclusionList;

    /* AccessPoint class fields */
//    public AccessPoint ap;
    private static final int[] STATE_SECURED = {R.attr.state_encrypted};
    private static final int[] STATE_NONE = {};

    public static final int INVALID_NETWORK_ID = -1;

    private String ssid;
    private String bssid;
    private SecurityType securityType;
    private int networkId;
    private PskType pskType = PskType.UNKNOWN;
    private transient WifiConfiguration wifiConfig;
    private WifiInfo mInfo;

    public int getRssi()
    {
        return mRssi;
    }

    private int mRssi;
    private NetworkInfo.DetailedState mState;


    public WiFiApConfig(WifiConfiguration wifiConf, ProxySetting setting, String host, Integer port, String exclusionList)
    {
        if (wifiConf == null)
            throw new IllegalArgumentException("WifiConfiguration parameter cannot be null");

        id = UUID.randomUUID();

        setProxySetting(setting);
        proxyHost = host;
        proxyPort = port;
        setProxyExclusionString(exclusionList);

        ssid = (wifiConf.SSID == null ? "" : removeDoubleQuotes(wifiConf.SSID));
        bssid = wifiConf.BSSID;
        securityType = ProxyUtils.getSecurity(wifiConf);
        networkId = wifiConf.networkId;
        mRssi = Integer.MAX_VALUE;
        wifiConfig = wifiConf;

        internalWifiNetworkId = new APLNetworkId(getSSID(), getSecurityType());

        status = new ProxyStatus();
    }

//    public WiFiApConfig(WiFiApConfig wiFiAPConfig)
//    {
//        id = wiFiAPConfig.id;
//        internalWifiNetworkId = wiFiAPConfig.internalWifiNetworkId;
//        status = wiFiAPConfig.status;
//
//        proxySetting = wiFiAPConfig.proxySetting;
//        proxyHost = wiFiAPConfig.proxyHost;
//        proxyPort = wiFiAPConfig.proxyPort;
//        stringProxyExclusionList = wiFiAPConfig.stringProxyExclusionList;
//        parsedProxyExclusionList = wiFiAPConfig.parsedProxyExclusionList;
//
//        ssid = wiFiAPConfig.ssid;
//        bssid = wiFiAPConfig.bssid;
//        securityType = wiFiAPConfig.securityType;
//        networkId = wiFiAPConfig.networkId;
//        pskType = wiFiAPConfig.pskType;
//        wifiConfig = wiFiAPConfig.wifiConfig;
//        mInfo = wiFiAPConfig.mInfo;
//        mRssi = wiFiAPConfig.mRssi;
//        mState = wiFiAPConfig.mState;
//    }

    public boolean updateScanResults(ScanResult result)
    {
        if (getSSID().equals(result.SSID) && getSecurityType() == ProxyUtils.getSecurity(result))
        {
            if (WifiManager.compareSignalLevel(result.level, mRssi) > 0)
            {
                int oldLevel = getLevel();
                mRssi = result.level;
            }
            // This flag only comes from scans, is not easily saved in config
            if (getSecurityType() == SecurityType.SECURITY_PSK)
            {
                pskType = ProxyUtils.getPskType(result);
            }

            return true;
        }
        return false;
    }

    public void updateWifiInfo(WifiInfo info, NetworkInfo.DetailedState state)
    {
        if (info != null
            && getNetworkId() != INVALID_NETWORK_ID
            && getNetworkId() == info.getNetworkId())
        {
            mRssi = info.getRssi();
            mInfo = info;
            mState = state;
        }
        else if (mInfo != null)
        {
            mInfo = null;
            mState = null;
        }
    }

    public boolean updateProxyConfiguration(WiFiApConfig updated)
    {
        //TODO: Add all required fields for updating an old configuration with an updated version
        if (!this.isSameConfiguration(updated))
        {
            Timber.d("Updating proxy configuration: \n%s\n%s",this.toShortString(), updated.toShortString());

            setProxySetting(updated.getProxySetting());
            proxyHost = updated.proxyHost;
            proxyPort = updated.proxyPort;
            stringProxyExclusionList = updated.getStringProxyExclusionList();
            parsedProxyExclusionList = ProxyUtils.parseExclusionList(getStringProxyExclusionList());

            getStatus().clear();

            Timber.d("Updated proxy configuration: \n%s\n%s", this.toShortString(), updated.toShortString());

            return true;
        }
        else
        {
//            LogWrapper.d(TAG,"No need to update proxy configuration: " + this.toShortString());
            return false;
        }
    }

    public Proxy getProxy()
    {
        if (getProxySetting() == ProxySetting.STATIC && proxyHost != null && proxyPort != null)
        {
            SocketAddress sa = null;

            if (isValidProxyConfiguration())
            {
                try
                {
                    sa = InetSocketAddress.createUnresolved(proxyHost, proxyPort);
                }
                catch (Exception e)
                {
                    Timber.e(e, "Failed creating unresolved", e);
                }
            }

            if (sa != null)
                return new Proxy(Proxy.Type.HTTP, sa);
            else
                return Proxy.NO_PROXY;
        }
        else
        {
            return Proxy.NO_PROXY;
        }
    }

    public boolean isValidProxyConfiguration()
    {
        return ProxyUtils.isValidProxyConfiguration(this);
    }

    public void setProxySetting(ProxySetting setting)
    {
        synchronized (getId())
        {
            proxySetting = setting;
        }
    }

    public ProxySetting getProxySetting()
    {
        synchronized (getId())
        {
            return proxySetting;
        }
    }

    public void setProxyHost(String host)
    {
        proxyHost = host;
    }

    public void setProxyPort(Integer port)
    {
        proxyPort = port;
    }

    public void setProxyExclusionString(String exList)
    {
        stringProxyExclusionList = exList;
        parsedProxyExclusionList = ProxyUtils.parseExclusionList(exList);
    }

    public boolean isSameConfiguration(Object another)
    {
        if (!(another instanceof WiFiApConfig))
        {
            Timber.e("Not a WiFiApConfig object");
            return false;
        }

        WiFiApConfig anotherConf = (WiFiApConfig) another;

        if (!this.proxySetting.equals(anotherConf.proxySetting))
        {
            Timber.d("Different proxy settings toggle status: '%s' - '%s'", this.proxySetting, anotherConf.proxySetting);
            return false;
        }

        if (this.proxyHost != null && anotherConf.proxyHost != null)
        {
            if (!this.proxyHost.equalsIgnoreCase(anotherConf.proxyHost))
            {
                Timber.d("Different proxy host value: '%s' - '%s'", this.proxyHost, anotherConf.proxyHost);
                return false;
            }
        }
        else if (this.proxyHost != anotherConf.proxyHost)
        {
            if ((this.proxyHost == null || this.proxyHost.equals("") && (anotherConf.proxyHost == null || anotherConf.proxyHost.equals(""))))
            {
                /** Can happen when a partial configuration is written on the device:
                 *  - ProxySettings enabled but no proxyHost and proxyPort are filled
                 */
            }
            else
            {
                Timber.d("Different proxy host set");
                Timber.d(TextUtils.isEmpty(this.proxyHost) ? "" : this.proxyHost);
                Timber.d(TextUtils.isEmpty(anotherConf.proxyHost) ? "" : anotherConf.proxyHost);
                return false;
            }
        }

        if (this.proxyPort != null && anotherConf.proxyPort != null)
        {
            if (!this.proxyPort.equals(anotherConf.proxyPort))
            {
                Timber.d("Different proxy port value: '%d' - '%d'", this.proxyPort, anotherConf.proxyPort);
                return false;
            }
        }
        else if (this.proxyPort != anotherConf.proxyPort)
        {
            if ((this.proxyPort == null || this.proxyPort == 0) && (anotherConf.proxyPort == null || anotherConf.proxyPort == 0))
            {
                /** Can happen when a partial configuration is written on the device:
                 *  - ProxySettings enabled but no proxyHost and proxyPort are filled
                 */
            }
            else
            {
                Timber.d("Different proxy port set");
                return false;
            }
        }

        if (this.getStringProxyExclusionList() != null && anotherConf.getStringProxyExclusionList() != null)
        {
            if (!this.getStringProxyExclusionList().equalsIgnoreCase(anotherConf.getStringProxyExclusionList()))
            {
                Timber.d("Different proxy exclusion list value: '%s' - '%s'", this.getStringProxyExclusionList(), anotherConf.getStringProxyExclusionList());
                return false;
            }
        }
        else if (this.getStringProxyExclusionList() != anotherConf.getStringProxyExclusionList())
        {
            if (TextUtils.isEmpty(this.getStringProxyExclusionList()) && TextUtils.isEmpty(anotherConf.getStringProxyExclusionList()))
            {
                /** Can happen when a partial configuration is written on the device:
                 *  - ProxySettings enabled but no proxyHost and proxyPort are filled
                 */
            }
            else
            {
                Timber.d("Different proxy exclusion list set");
                return false;
            }
        }

//        LogWrapper.d(TAG,"Same proxy configuration: \n" +  this.toShortString() + "\n" +  anotherConf.toShortString());
        return true;
    }

    @Override
    public int compareTo(WiFiApConfig wiFiAPConfig) {

        if (!(wiFiAPConfig instanceof WiFiApConfig))
        {
            return 1;
        }

        return WifiApConfigComparator.compareWifiAp(this, (WiFiApConfig) wiFiAPConfig);
    }

    public boolean isActive()
    {
        return mInfo != null;
    }

    public boolean isReachable()
    {
        return mRssi != Integer.MAX_VALUE;
    }

//    @Override
//    public int compareTo(WiFiApConfig another)
//    {
//        int result = 0;
//
//        if (this.isCurrentNetwork())
//        {
//            if (another.isCurrentNetwork())
//            {
//                result = 0;
//            }
//            else
//            {
//                result = -1;
//            }
//        }
//        else
//        {
//            if (another.isCurrentNetwork())
//            {
//                result = +1;
//            }
//            else
//            {
//                result = 0;
//            }
//        }
//
//        if (result == 0)
//        {
//            if (ap != null)
//            {
//                if (another.ap != null)
//                {
//                    result = ap.compareTo(another.ap);
//                }
//                else
//                {
//                    result = -1;
//                }
//            }
//            else
//            {
//                if (another.ap != null)
//                {
//                    result = +1;
//                }
//                else
//                {
//                    result = 0;
//                }
//            }
//        }
//
//        return result;
//    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ID: %s\n", getId().toString()));
        sb.append(String.format("Wi-Fi Configuration Info: %s\n", getSSID()));
        sb.append(String.format("Proxy setting: %s\n", getProxySetting().toString()));
        sb.append(String.format("Proxy: %s\n", toStatusString()));
        sb.append(String.format("Is current network: %B\n", isActive()));
        sb.append(String.format("Proxy status checker results: %s\n", getStatus().toString()));

        if (APL.getConnectivityManager().getActiveNetworkInfo() != null)
        {
            sb.append(String.format("Network Info: %s\n", APL.getConnectivityManager().getActiveNetworkInfo()));
        }

        return sb.toString();
    }

    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();

        try
        {
            jsonObject.put("ID", getId().toString());
            jsonObject.put("SSID", getSSID());

            jsonObject.put("proxy_setting", getProxySetting().toString());
            jsonObject.put("proxy_status", toStatusString());
            jsonObject.put("is_current", isActive());
            jsonObject.put("status", getStatus().toJSON());

            if (APL.getConnectivityManager().getActiveNetworkInfo() != null)
            {
                jsonObject.put("network_info", APL.getConnectivityManager().getActiveNetworkInfo());
            }
        }
        catch (JSONException e)
        {
            Timber.e(e, "Exception converting to JSON object WiFiApConfig");
        }

        return jsonObject;
    }

    public String toShortString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("INTERNAL Id: %s, SSID: %s, RSSI: %d, LEVEL: %d, NETID: %d", getId().toString(), getSSID(), mRssi, getLevel(), getNetworkId()));

        sb.append(" - " + toStatusString());
        sb.append(" " + getProxyExclusionList());

        if (getStatus() != null)
            sb.append(" - " + getStatus().toShortString());

        return sb.toString();
    }

    public String toStatusString()
    {
        ProxySetting setting = getProxySetting();

        if (setting == null)
        {
            return APL.getContext().getResources().getString(R.string.not_available);
        }

        if (setting == ProxySetting.NONE || setting == ProxySetting.UNASSIGNED)
        {
            return APL.getContext().getResources().getString(R.string.direct_connection);
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(proxyHost) && proxyPort != null && proxyPort > 0)
                sb.append(String.format("%s:%s", proxyHost, proxyPort));
            else
            {
                sb.append(APL.getContext().getResources().getString(R.string.not_set));
            }

            return sb.toString();
        }
    }

    public Proxy.Type getProxyType()
    {
        return getProxy().type();
    }

    public String getProxyHostString()
    {
        return proxyHost;
    }

    public String getProxyIPHost()
    {
        return proxyHost;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public Integer getProxyPort()
    {
        return proxyPort;
    }

    public String getProxyExclusionList()
    {
        if (getStringProxyExclusionList() == null)
            return "";
        else
            return getStringProxyExclusionList();
    }

    public CheckStatusValues getCheckingStatus()
    {
        return getStatus().getCheckingStatus();
    }

    public String getBSSID()
    {
        return bssid;
    }

    @Deprecated
    @TargetApi(12)
    public void writeConfigurationToDevice() throws Exception
    {
        APL.writeWifiAPConfig(this);
    }

    public String getAPConnectionStatus()
    {
        if (isActive())
        {
            return APL.getContext().getString(R.string.connected);
        }
        else if (getLevel() > 0)
        {
            return APL.getContext().getString(R.string.available);
        }
        else
        {
            return APL.getContext().getString(R.string.not_available);
        }
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

    public int getLevel()
    {
        if (mRssi == Integer.MAX_VALUE)
        {
            return -1;
        }
        return WifiManager.calculateSignalLevel(mRssi, 4);
    }

    public void clearScanStatus()
    {
        mRssi = Integer.MAX_VALUE;
        pskType = PskType.UNKNOWN;
    }

    public UUID getId()
    {
        return id;
    }

    public APLNetworkId getAPLNetworkId()
    {
        return internalWifiNetworkId;
    }

    public ProxyStatus getStatus()
    {
        return status;
    }

    public String getStringProxyExclusionList()
    {
        return stringProxyExclusionList;
    }

    public String getSSID()
    {
        return ssid;
    }

    public SecurityType getSecurityType()
    {
        return securityType;
    }

    public int getNetworkId()
    {
        return networkId;
    }

    public PskType getPskType()
    {
        return pskType;
    }

    public WifiConfiguration getWifiConfig()
    {
        return wifiConfig;
    }
}
