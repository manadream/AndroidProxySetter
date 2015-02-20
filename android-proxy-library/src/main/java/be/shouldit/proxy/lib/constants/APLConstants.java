package be.shouldit.proxy.lib.constants;

import java.util.regex.Pattern;

public class APLConstants
{
    public static final String ProxyStatus = "ProxyStatus";

    /**
     * Try to download a webpage using the current proxy configuration
     */
    public static final Integer DEFAULT_TIMEOUT = 10000; // 10 seconds


    /**
     * The following logic is taken from Android's ProxySelector.java class
     * @see <a href="https://github.com/android/platform_packages_apps_settings/blob/b568548747c9b137d7da05fcdbb84f157273b3db/src/com/android/settings/wifi/WifiConfigController.java#L440">ProxySelector.java class source</a>
     */
    // Allows underscore char to supports proxies that do not follow the spec
    private static final String HC = "a-zA-Z0-9\\_";

    // Matches blank input, ips, and domain names
    private static final String HOSTNAME_REGEXP = "^$|^[" + HC + "]+(\\-[" + HC + "]+)*(\\.[" + HC + "]+(\\-[" + HC + "]+)*)*$";
    public static final Pattern HOSTNAME_PATTERN;
    private static final String EXCLUSION_REGEXP = "$|^(\\*)?\\.?[" + HC + "]+(\\-[" + HC + "]+)*(\\.[" + HC + "]+(\\-[" + HC + "]+)*)*$";
    public static final Pattern EXCLUSION_PATTERN;

    static
    {
        HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEXP);
        EXCLUSION_PATTERN = Pattern.compile(EXCLUSION_REGEXP);
    }
}
