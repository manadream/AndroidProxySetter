package be.shouldit.proxy.lib;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.io.Serializable;

import be.shouldit.proxy.lib.enums.PskType;
import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.utils.ProxyUtils;

public class AccessPoint implements Comparable<AccessPoint>, Serializable
{
	static final String TAG = AccessPoint.class.getSimpleName();

	public static final int[] STATE_SECURED = { R.attr.state_encrypted };
	public static final int[] STATE_NONE = {};

	public String ssid;
	public String bssid;
	public SecurityType security;
	public int networkId;
	public PskType pskType = PskType.UNKNOWN;
	public transient WifiConfiguration wifiConfig;
	private int mRssi;

	public AccessPoint(WifiConfiguration config)
	{
		loadConfig(config);
	}

	private void loadConfig(WifiConfiguration config)
	{
		ssid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
		bssid = config.BSSID;
		security = ProxyUtils.getSecurity(config);
		networkId = config.networkId;
		mRssi = Integer.MAX_VALUE;
//        mInfo = info;
		wifiConfig = config;
	}

	@Override
	public int compareTo(AccessPoint ap)
	{
		if (!(ap instanceof AccessPoint))
		{
			return 1;
		}
		
		AccessPoint other = (AccessPoint) ap;

//		// Active one goes first: Only check different SSID
//	    if (ssid.compareTo(other.ssid) != 0 && mInfo != other.mInfo)
//		{
//			return (mInfo != null) ? -1 : 1;
//		}

		// Reachable one goes before unreachable one.
		if ((mRssi ^ other.mRssi) < 0)
		{
			return (mRssi != Integer.MAX_VALUE) ? -1 : 1;
		}

		// Configured one goes before unconfigured one.
		if ((networkId ^ other.networkId) < 0)
		{
			return (networkId != -1) ? -1 : 1;
		}

		// Sort by signal strength.
//		int difference = WifiManager.compareSignalLevel(other.mRssi, mRssi);
//		if (difference != 0)
//		{
//			return difference;
//		}
		// Sort by ssid.
		return ssid.compareToIgnoreCase(other.ssid);
	}

    public String toShortString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("SSID: %s, RSSI: %d, LEVEL: %d, NETID: %d",ssid, mRssi, getLevel(), networkId));

        return sb.toString();
    }

    public void clearScanStatus()
    {
        mRssi = Integer.MAX_VALUE;
        pskType = PskType.UNKNOWN;
    }

	public boolean update(ScanResult result)
	{
		if (ssid.equals(result.SSID) && security == ProxyUtils.getSecurity(result))
		{
			if (WifiManager.compareSignalLevel(result.level, mRssi) > 0)
			{
				mRssi = result.level;
			}
			// This flag only comes from scans, is not easily saved in config
			if (security == SecurityType.SECURITY_PSK)
			{
				pskType = ProxyUtils.getPskType(result);
			}
			return true;
		}
		return false;
	}

	public int getLevel()
	{
		if (mRssi == Integer.MAX_VALUE)
		{
			return -1;
		}
		return WifiManager.calculateSignalLevel(mRssi, 4);
	}

//	WifiInfo getInfo()
//	{
//		return mInfo;
//	}

	static String removeDoubleQuotes(String string)
	{
		int length = string.length();
		if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"'))
		{
			return string.substring(1, length - 1);
		}
		return string;
	}

	static String convertToQuotedString(String string)
	{
		return "\"" + string + "\"";
	}
}
