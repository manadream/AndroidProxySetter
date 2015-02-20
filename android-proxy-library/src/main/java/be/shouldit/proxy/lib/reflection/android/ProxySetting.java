package be.shouldit.proxy.lib.reflection.android;

public enum ProxySetting {
    /* No proxy is to be used. Any existing proxy settings
     * should be cleared. */
    NONE,
    /* Use statically configured proxy. Configuration can be accessed
     * with linkProperties */
    STATIC,
    /* no proxy details are assigned, this is used to indicate
     * that any existing proxy settings should be retained */
    UNASSIGNED
}
