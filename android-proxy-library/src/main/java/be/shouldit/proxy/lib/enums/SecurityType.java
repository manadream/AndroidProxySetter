package be.shouldit.proxy.lib.enums;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * These values are matched in string arrays -- changes must be kept in sync
 */
public enum SecurityType implements Parcelable
{
    SECURITY_NONE(0),
    SECURITY_WEP(1),
    SECURITY_PSK(2),
    SECURITY_EAP(3);

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(value);
    }

    private final Integer value;

    public static final Parcelable.Creator<SecurityType> CREATOR = new Parcelable.Creator<SecurityType>()
    {

        public SecurityType createFromParcel(Parcel in)
        {
            return SecurityType.values()[in.readInt()];
        }

        public SecurityType[] newArray(int size)
        {
            return new SecurityType[size];
        }

    };

    SecurityType(Parcel in)
    {
        this.value = in.readInt();
    }

    SecurityType(int index)
    {
        this.value = index;
    }

    public Integer getValue()
    {
        return value;
    }
}

