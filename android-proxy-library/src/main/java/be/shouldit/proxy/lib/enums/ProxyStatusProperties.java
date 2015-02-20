package be.shouldit.proxy.lib.enums;

/**
 * Created by Marco on 15/09/13.
 */
public enum ProxyStatusProperties
{
    WIFI_ENABLED(0),
    WIFI_SELECTED(1),
    PROXY_ENABLED(2),
    WEB_REACHABLE(3),
    PROXY_VALID_HOSTNAME(4),
    PROXY_VALID_PORT(5),
    PROXY_REACHABLE(6),
    PROXY_VALID_EXCLUSION_LIST(7),
    PROXY_VALID_EXCLUSION_ITEM(8);

    private final Integer priority;

    ProxyStatusProperties(int index)
    {
        this.priority = index;
    }

    public Integer getPriority()
    {
        return priority;
    }
}
