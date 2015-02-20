package be.shouldit.proxy.lib;

import be.shouldit.proxy.lib.enums.SecurityType;

import java.io.Serializable;

/**
 * Created by Marco on 08/06/13.
 */
public class APLNetworkId implements Serializable
{
    public String SSID;
    public SecurityType Security;

    public APLNetworkId(String ssid, SecurityType sec)
    {
        SSID = ssid;
        Security = sec;
    }

    @Override
    public boolean equals(Object another)
    {
        Boolean result = false;

        if ((another instanceof APLNetworkId))
        {
            APLNetworkId anotherWifi = (APLNetworkId) another;

            if (SSID.equals(anotherWifi.SSID))
            {
                if (Security != null && anotherWifi.Security != null && Security.equals(anotherWifi.Security))
                    result = true;
                else
                    result = false;
            }
        }

        return result;
    }

    @Override
    public int hashCode()
    {
        int ssidHash = SSID.hashCode();
        int secHash = Security.hashCode();

        return ssidHash + secHash;
    }

    @Override
    public String toString()
    {
        return String.format("'%s' - '%s'", SSID, Security);
    }
}
