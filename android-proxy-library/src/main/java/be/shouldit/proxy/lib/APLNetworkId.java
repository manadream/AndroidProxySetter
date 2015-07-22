package be.shouldit.proxy.lib;

import android.os.Parcel;
import android.os.Parcelable;

import be.shouldit.proxy.lib.enums.SecurityType;

/**
 * Created by Marco on 08/06/13.
 */
public class APLNetworkId implements Parcelable
{
    public String SSID;
    public SecurityType Security;

    private APLNetworkId(Parcel in)
    {
        this.SSID = in.readString();
        int tmpSecurity = in.readInt();
        this.Security = tmpSecurity == -1 ? null : SecurityType.values()[tmpSecurity];
    }

    public static final Creator<APLNetworkId> CREATOR = new Creator<APLNetworkId>()
    {
        public APLNetworkId createFromParcel(Parcel source) {return new APLNetworkId(source);}

        public APLNetworkId[] newArray(int size) {return new APLNetworkId[size];}
    };

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

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.SSID);
        dest.writeInt(this.Security == null ? -1 : this.Security.ordinal());
    }
}
