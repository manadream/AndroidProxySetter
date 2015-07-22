package be.shouldit.proxy.lib.enums;

import java.util.EnumSet;

/**
 * Created by Marco on 15/09/13.
 */
public enum ProxyCheckOptions
{
    OFFLINE_CHECK,
    ONLINE_CHECK;

    public static final EnumSet<ProxyCheckOptions> OFFLINE = EnumSet.of(OFFLINE_CHECK);
    public static final EnumSet<ProxyCheckOptions> ONLINE = EnumSet.of(ONLINE_CHECK);
    public static final EnumSet<ProxyCheckOptions> ALL = EnumSet.allOf(ProxyCheckOptions.class);
}
