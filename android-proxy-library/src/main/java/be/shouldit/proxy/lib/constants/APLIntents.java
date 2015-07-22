package be.shouldit.proxy.lib.constants;

/**
 * Created by mpagliar on 24/02/14.
 */
public class APLIntents
{
    /**
     * Prefix for all intents created
     */
    public static final String INTENT_PREFIX = "io.should.proxy.lib.";

    /**
     * Broadcasted intent when updates on the proxy status are available
     */
    public static final String APL_UPDATED_PROXY_STATUS_CHECK = INTENT_PREFIX + "PROXY_CHECK_STATUS_UPDATE";

    /**
     * Broadcasted intent when a proxy configuration is written on the device
     */
    public static final String APL_UPDATED_PROXY_CONFIGURATION = INTENT_PREFIX + "PROXY_CONFIGURATION_UPDATED";
}
