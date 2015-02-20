package be.shouldit.proxy.lib;

import java.io.Serializable;
import java.util.Comparator;

import be.shouldit.proxy.lib.enums.ProxyStatusProperties;

public class ProxyStatusPropertiesComparator implements Comparator<ProxyStatusProperties>, Serializable
{
    public int compare(ProxyStatusProperties o1, ProxyStatusProperties o2)
    {
    	int result = o1.getPriority().compareTo(o2.getPriority());
    	return result;
    }
}
