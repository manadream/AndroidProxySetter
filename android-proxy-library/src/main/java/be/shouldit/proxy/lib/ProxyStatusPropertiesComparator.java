package be.shouldit.proxy.lib;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Comparator;

import be.shouldit.proxy.lib.enums.ProxyStatusProperties;

public class ProxyStatusPropertiesComparator implements Comparator<ProxyStatusProperties>, Parcelable
{
    public int compare(ProxyStatusProperties o1, ProxyStatusProperties o2)
    {
    	int result = o1.getPriority().compareTo(o2.getPriority());
    	return result;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {}

    public ProxyStatusPropertiesComparator() {}

    private ProxyStatusPropertiesComparator(Parcel in) {}

    public static final Creator<ProxyStatusPropertiesComparator> CREATOR = new Creator<ProxyStatusPropertiesComparator>()
    {
        public ProxyStatusPropertiesComparator createFromParcel(Parcel source) {return new ProxyStatusPropertiesComparator(source);}

        public ProxyStatusPropertiesComparator[] newArray(int size) {return new ProxyStatusPropertiesComparator[size];}
    };
}
