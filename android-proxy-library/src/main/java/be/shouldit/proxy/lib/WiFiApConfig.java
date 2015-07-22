package be.shouldit.proxy.lib;

import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.UUID;

import be.shouldit.proxy.lib.enums.CheckStatusValues;
import be.shouldit.proxy.lib.enums.PskType;
import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;
import be.shouldit.proxy.lib.utils.ProxyUtils;
import timber.log.Timber;

public class WiFiApConfig implements Comparable<WiFiApConfig>, Parcelable
{
    private static final String TAG = WiFiApConfig.class.getSimpleName();

    public static final int INVALID_NETWORK_ID = -1;
    public static final String LOCAL_EXCL_LIST = "";
    public static final int LOCAL_PORT = -1;
    public static final String LOCAL_HOST = "localhost";

    public static final int[] STATE_SECURED = {R.attr.state_encrypted};
    public static final int[] STATE_NONE = {};

    private final UUID id;
    private final APLNetworkId internalWifiNetworkId;
    private ProxyStatus status;
    private ProxySetting proxySetting;
    private String proxyHost;
    private Integer proxyPort;
    private String stringProxyExclusionList;

    private String pacFileUri;
    private String[] parsedProxyExclusionList;

    /* AccessPoint class fields */
//    public AccessPoint ap;
    private String ssid;
    private String bssid;
    private SecurityType securityType;
    private int networkId;
    private PskType pskType = PskType.UNKNOWN;
    private transient WifiConfiguration wifiConfig;
    private WifiInfo mInfo;
    private int mRssi;
    private NetworkInfo.DetailedState mState;

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeSerializable(this.id);
        dest.writeParcelable(this.internalWifiNetworkId, 0);
        dest.writeParcelable(this.status, flags);
        dest.writeInt(this.proxySetting == null ? -1 : this.proxySetting.ordinal());
        dest.writeString(this.proxyHost);
        dest.writeValue(this.proxyPort);
        dest.writeString(this.stringProxyExclusionList);
        dest.writeString(this.pacFileUri);
        dest.writeStringArray(this.parsedProxyExclusionList);
        dest.writeString(this.ssid);
        dest.writeString(this.bssid);
        dest.writeInt(this.securityType == null ? -1 : this.securityType.ordinal());
        dest.writeInt(this.networkId);
        dest.writeInt(this.pskType == null ? -1 : this.pskType.ordinal());
        dest.writeParcelable(this.wifiConfig, 0);
        dest.writeParcelable(this.mInfo, 0);
        dest.writeInt(this.mRssi);
        dest.writeInt(this.mState == null ? -1 : this.mState.ordinal());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof WiFiApConfig)) return false;

        WiFiApConfig that = (WiFiApConfig) o;

        if (mRssi != that.mRssi) return false;
        if (networkId != that.networkId) return false;
        if (bssid != null ? !bssid.equals(that.bssid) : that.bssid != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (internalWifiNetworkId != null ? !internalWifiNetworkId.equals(that.internalWifiNetworkId) : that.internalWifiNetworkId != null)
            return false;
        if (mInfo != null ? !mInfo.equals(that.mInfo) : that.mInfo != null) return false;
        if (mState != that.mState) return false;
        if (pacFileUri != null ? !pacFileUri.equals(that.pacFileUri) : that.pacFileUri != null)
            return false;
        if (!Arrays.equals(parsedProxyExclusionList, that.parsedProxyExclusionList))
            return false;
        if (proxyHost != null ? !proxyHost.equals(that.proxyHost) : that.proxyHost != null)
            return false;
        if (proxyPort != null ? !proxyPort.equals(that.proxyPort) : that.proxyPort != null)
            return false;
        if (proxySetting != that.proxySetting) return false;
        if (pskType != that.pskType) return false;
        if (securityType != that.securityType) return false;
        if (ssid != null ? !ssid.equals(that.ssid) : that.ssid != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (stringProxyExclusionList != null ? !stringProxyExclusionList.equals(that.stringProxyExclusionList) : that.stringProxyExclusionList != null)
            return false;

        // Cannot call equals on WifiConfiguration class, since it doesn't override the base method
//        if (wifiConfig != null ? !wifiConfig.equals(that.wifiConfig) : that.wifiConfig != null)
//            return false;

        return true;
    }

    private WiFiApConfig(Parcel in)
    {
        this.id = (UUID) in.readSerializable();
        this.internalWifiNetworkId = in.readParcelable(APLNetworkId.class.getClassLoader());
        this.status = in.readParcelable(ProxyStatus.class.getClassLoader());

        int tmpProxySetting = in.readInt();
        this.proxySetting = tmpProxySetting < 0 || tmpProxySetting >= ProxySetting.values().length ? null : ProxySetting.values()[tmpProxySetting];

        this.proxyHost = in.readString();
        this.proxyPort = (Integer) in.readValue(Integer.class.getClassLoader());
        this.stringProxyExclusionList = in.readString();
        this.pacFileUri = in.readString();
        this.parsedProxyExclusionList = in.createStringArray();
        this.ssid = in.readString();
        this.bssid = in.readString();

        int tmpSecurityType = in.readInt();
        this.securityType = tmpSecurityType < 0 || tmpSecurityType >= SecurityType.values().length ? null : SecurityType.values()[tmpSecurityType];

        this.networkId = in.readInt();

        int tmpPskType = in.readInt();
        this.pskType = tmpPskType < 0 || tmpPskType >= PskType.values().length ? null : PskType.values()[tmpPskType];

        this.wifiConfig = in.readParcelable(WifiConfiguration.class.getClassLoader());
        this.mInfo = in.readParcelable(WifiInfo.class.getClassLoader());
        this.mRssi = in.readInt();

        int tmpMState = in.readInt();
        this.mState = tmpMState < 0 || tmpMState >= NetworkInfo.DetailedState.values().length ? null : NetworkInfo.DetailedState.values()[tmpMState];
    }

    public static final Creator<WiFiApConfig> CREATOR = new Creator<WiFiApConfig>()
    {
        public WiFiApConfig createFromParcel(Parcel source) {return new WiFiApConfig(source);}

        public WiFiApConfig[] newArray(int size) {return new WiFiApConfig[size];}
    };

    public WiFiApConfig(WifiConfiguration wifiConf, ProxySetting setting, String host, Integer port, String exclusionList, Uri pacFile)
    {
        if (wifiConf == null)
            throw new IllegalArgumentException("WifiConfiguration parameter cannot be null");

        id = UUID.randomUUID();

        setProxySetting(setting);

        proxyHost = host;
        proxyPort = port;
        setProxyExclusionString(exclusionList);
        pacFileUri = pacFile.toString();

        ssid = (wifiConf.SSID == null ? "" : removeDoubleQuotes(wifiConf.SSID));
        bssid = wifiConf.BSSID;
        securityType = ProxyUtils.getSecurity(wifiConf);
        networkId = wifiConf.networkId;
        mRssi = Integer.MAX_VALUE;
        wifiConfig = wifiConf;

        internalWifiNetworkId = new APLNetworkId(getSSID(), getSecurityType());

        status = new ProxyStatus();
    }

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
            pacFileUri = updated.pacFileUri;

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
    public int compareTo(WiFiApConfig wiFiApConfig) {

        if (!(wiFiApConfig instanceof WiFiApConfig))
        {
            return 1;
        }

        return WifiApConfigComparator.compareWifiAp(this, (WiFiApConfig) wiFiApConfig);
    }

    public boolean isActive()
    {
        return mInfo != null;
    }

    public boolean isReachable()
    {
        return mRssi != Integer.MAX_VALUE;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SSID: %s\n", getSSID()));
        sb.append(String.format("Internal ID: %s\n", getId().toString()));
        sb.append(String.format("Proxy setting: %s\n", getProxySetting().toString()));
        sb.append(String.format("Proxy: %s\n", getProxyStatusString()));
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
            jsonObject.put("proxy_status", getProxyStatusString());
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

        sb.append(" - " + getProxyStatusString());

        if (getStatus() != null)
            sb.append(" - " + getStatus().toShortString());

        return sb.toString();
    }

    public String getProxyStatusString()
    {
        ProxySetting setting = getProxySetting();
        String result = "";

        try
        {
//            APL.getTraceUtils().startTrace(TAG,"getProxyStatusString", Log.DEBUG);

            if (setting == null)
            {
                result = APL.getContext().getString(R.string.not_available);
            }
            else if (setting == ProxySetting.NONE || setting == ProxySetting.UNASSIGNED)
            {
                result = APL.getContext().getString(R.string.direct_connection);
            }
            else if (setting == ProxySetting.STATIC)
            {
                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(proxyHost) && proxyPort != null && proxyPort > 0)
                    sb.append(String.format("%s:%d", proxyHost, proxyPort));
                else
                {
                    sb.append(APL.getContext().getString(R.string.not_set));
                }

                result = sb.toString();
            }
            else if (setting == ProxySetting.PAC)
            {
                StringBuilder sb = new StringBuilder();

                if (!TextUtils.isEmpty(pacFileUri.toString()))
                    sb.append(String.format("%s", pacFileUri));
                else
                {
                    sb.append(APL.getContext().getString(R.string.not_set));
                }

                result = sb.toString();
            }
            else
            {
                result = APL.getContext().getString(R.string.not_valid_proxy_setting);
            }

//            APL.getTraceUtils().stopTrace(TAG, "getProxyStatusString", result, Log.DEBUG);
        }
        catch (Exception e)
        {
            Timber.e(e,"Exception building proxy status string");
        }

        return result;
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

    public Uri getPacFileUri()
    {
        return Uri.parse(pacFileUri);
    }

    public void setPacUriFile(Uri pacUriFile)
    {
        this.pacFileUri = pacUriFile.toString();
    }

    public CheckStatusValues getCheckingStatus()
    {
        return getStatus().getCheckingStatus();
    }

    public String getBSSID()
    {
        return bssid;
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

    public int getRssi()
    {
        return mRssi;
    }
}
