package be.shouldit.proxy.lib.enums;

/**
 * These values are matched in string arrays -- changes must be kept in sync
 */
public enum SecurityType
{
    SECURITY_NONE(0),
    SECURITY_WEP(1),
    SECURITY_PSK(2),
    SECURITY_EAP(3);

    private final Integer value;

    SecurityType(int index)
    {
        this.value = index;
    }

    public Integer getValue()
    {
        return value;
    }
}

