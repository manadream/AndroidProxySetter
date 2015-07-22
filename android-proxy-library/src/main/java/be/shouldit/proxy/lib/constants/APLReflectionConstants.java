package be.shouldit.proxy.lib.constants;

/**
 * Created by mpagliar on 12/08/2014.
 */
public class APLReflectionConstants
{
    /**
     * Broadcast intent action indicating that the configured networks changed.
     * This can be as a result of adding/updating/deleting a network. If
     * {@link #EXTRA_MULTIPLE_NETWORKS_CHANGED} is set to true the new configuration
     * can be retreived with the {@link #EXTRA_WIFI_CONFIGURATION} extra. If multiple
     * Wi-Fi configurations changed, {@link #EXTRA_WIFI_CONFIGURATION} will not be present.
     */
    public static final String CONFIGURED_NETWORKS_CHANGED_ACTION = "android.net.wifi.CONFIGURED_NETWORKS_CHANGE";

    /**
     * The lookup key for a (@link android.net.wifi.WifiConfiguration} object representing
     * the changed Wi-Fi configuration when the {@link #CONFIGURED_NETWORKS_CHANGED_ACTION}
     * broadcast is sent.
     */
    public static final String EXTRA_WIFI_CONFIGURATION = "wifiConfiguration";

    /**
     * Multiple network configurations have changed.
     * @see #CONFIGURED_NETWORKS_CHANGED_ACTION
     *
     */
    public static final String EXTRA_MULTIPLE_NETWORKS_CHANGED = "multipleChanges";

    /**
     * The lookup key for an integer indicating the reason a Wi-Fi network configuration
     * has changed. Only present if {@link #EXTRA_MULTIPLE_NETWORKS_CHANGED} is {@code false}
     * @see #CONFIGURED_NETWORKS_CHANGED_ACTION
     */
    public static final String EXTRA_CHANGE_REASON = "changeReason";

    /**
     * The configuration is new and was added.
     */
    public static final int CHANGE_REASON_ADDED = 0;

    /**
     * The configuration was removed and is no longer present in the system's list of
     * configured networks.
     */
    public static final int CHANGE_REASON_REMOVED = 1;

    /**
     * The configuration has changed as a result of explicit action or because the system
     * took an automated action such as disabling a malfunctioning configuration.
     */
    public static final int CHANGE_REASON_CONFIG_CHANGE = 2;


    /* These values come from "wifi_eap_method" resource array */
    public static final int WIFI_EAP_METHOD_PEAP = 0;
    public static final int WIFI_EAP_METHOD_TLS  = 1;
    public static final int WIFI_EAP_METHOD_TTLS = 2;
    public static final int WIFI_EAP_METHOD_PWD  = 3;

    /* These values come from "wifi_peap_phase2_entries" resource array */
    public static final int WIFI_PEAP_PHASE2_NONE 	    = 0;
    public static final int WIFI_PEAP_PHASE2_MSCHAPV2 	= 1;
    public static final int WIFI_PEAP_PHASE2_GTC        = 2;
}
