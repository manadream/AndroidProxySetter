package be.shouldit.proxy.lib.reflection;

import android.annotation.TargetApi;
import android.net.ProxyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.WiFiApConfig;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;
import be.shouldit.proxy.lib.reflection.android.WifiServiceHandler;
import timber.log.Timber;

public class ReflectionUtils
{
    public static final String TAG = ReflectionUtils.class.getSimpleName();

    /* Events from WifiService */
    /** @hide */
    public static final int CMD_WPS_COMPLETED               = 11;

    public static final int BASE_SYSTEM_ASYNC_CHANNEL = 0x00011000;
    private static final int BASE = BASE_SYSTEM_ASYNC_CHANNEL;

    /**
     * Command sent when the channel is half connected. Half connected
     * means that the channel can be used to sendEvent commends to the destination
     * but the destination is unaware that the channel exists. The first
     * command sent to the destination is typically CMD_CHANNEL_FULL_CONNECTION if
     * it is desired to establish a long term connection, but any command maybe
     * sent.
     *
     * msg.arg1 == 0 : STATUS_SUCCESSFUL
     *             1 : STATUS_BINDING_UNSUCCESSFUL
     * msg.obj  == the AsyncChannel
     * msg.replyTo == dstMessenger if successful
     */
    public static final int CMD_CHANNEL_HALF_CONNECTED = BASE + 0;

    /** Successful status always 0, !0 is an unsuccessful status */
    public static final int STATUS_SUCCESSFUL = 0;
    /** Error attempting to bind on a connect */
    public static final int STATUS_BINDING_UNSUCCESSFUL = 1;
    /** Error attempting to sendEvent a message */
    public static final int STATUS_SEND_UNSUCCESSFUL = 2;

    public static void connectToWifi(WifiManager wifiManager, Integer networkId) throws Exception
    {
        boolean internalConnectDone = false;

        try
        {
            Class[] knownParam = new Class[1];
            knownParam[0] = int.class;
            Method internalConnect = getMethod(WifiManager.class.getMethods(), "connect", knownParam);
            if (internalConnect != null)
            {
                Class<?>[] paramsTypes = internalConnect.getParameterTypes();
                if (paramsTypes.length == 2)
                    internalConnect.invoke(wifiManager, networkId, null);
                else if (paramsTypes.length == 1)
                    internalConnect.invoke(wifiManager, networkId);

                internalConnectDone = true;
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception trying to connetToWifi");
        }

        if (!internalConnectDone)
        {
            // Use the STANDARD API as a fallback solution
            wifiManager.enableNetwork(networkId, true);
        }
    }

    public static void saveWifiConfiguration(WifiManager wifiManager, WifiConfiguration configuration) throws Exception
    {
        boolean internalSaveDone = false;

        /**
         *   4.4.2_r1
         *   4.4.1_r1
         *   4.4_r1
         *   4.3.1_r1
         *   4.3_r1
         *   4.2.2_r1
         *   4.2.1_r1.2
         *   4.2_r1 public void More ...save(WifiConfiguration config, ActionListener listener)
         *
         *   4.1.2_r1
         *   4.1.1_r1 public void More ...save(Channel c, WifiConfiguration config, ActionListener listener)
         *
         *   4.0.4_r2.1
         *   4.0.4_r1.2
         *   4.0.3_r1
         *   4.0.2_r1
         *   4.0.1_r1 public void More ...saveNetwork(WifiConfiguration config)
         *
         * */

        try
        {
            switch (Build.VERSION.SDK_INT)
            {
                case 14:
                case 15:
                    internalSaveDone = save_4_0(wifiManager, configuration);
                    break;

                case 16:
                    internalSaveDone = save_4_1(wifiManager, configuration);
                    break;

                case 17:
                case 18:
                case 19:
                case 20:
                default:
                    internalSaveDone = save_4_2(wifiManager, configuration);
                    break;
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception on saveWifiConfiguration");
        }

        if (!internalSaveDone)
        {
            // Use the STANDARD API as a fallback solution
            wifiManager.updateNetwork(configuration);
        }
    }

    private static boolean save_4_0(WifiManager wifiManager, WifiConfiguration configuration) throws Exception
    {
        boolean internalSaveDone = false;

        Method internalAsyncConnect = getMethod(WifiManager.class.getMethods(), "asyncConnect");
        Method internalSaveNetwork = getMethod(WifiManager.class.getMethods(), "saveNetwork");

        if (internalAsyncConnect != null && internalSaveNetwork != null)
        {
            try
            {
                Looper looper = Looper.myLooper();
                if (looper == null)
                    Looper.prepare();   // Needed to invoke the asyncConnect method

                WifiServiceHandler wifiServiceHandler = new WifiServiceHandler();
                internalAsyncConnect.invoke(wifiManager, APL.getContext(), wifiServiceHandler);

                if (internalSaveNetwork != null)
                {
                    internalSaveNetwork.invoke(wifiManager, configuration);
                    internalSaveDone = true;
                }
            }
            catch (Exception e)
            {
                throw new Exception("Exception during call of WifiManager.saveNetwork method (4.0) : " + e, e);
            }
        }

        return internalSaveDone;
    }

    private static boolean save_4_1(WifiManager wifiManager, WifiConfiguration configuration) throws Exception
    {
        boolean internalSaveDone = false;

        Method internalSave = getMethod(WifiManager.class.getMethods(), "save");
        Method internalInitialize = getMethod(WifiManager.class.getMethods(), "initialize");

        if (internalInitialize != null && internalSave != null)
        {
            try
            {
                Looper looper = Looper.myLooper();
                int attempt = 0;

                while (attempt < 5 && looper == null)
                {
                    Looper.prepare();   // Needed to invoke the asyncConnect method
                    looper = Looper.myLooper();
                    attempt++;
                }

                Object channel = internalInitialize.invoke(wifiManager, APL.getContext(), looper, null);
                if (channel != null)
                {
                    internalSave.invoke(wifiManager, channel, configuration, null);
                    internalSaveDone = true;
                }
            }
            catch (Exception e)
            {
                throw new Exception("Exception during call of WifiManager.save method (4.1) : " + e, e);
            }
        }

        return internalSaveDone;
    }

    private static boolean save_4_2(WifiManager wifiManager, WifiConfiguration configuration) throws Exception
    {
        boolean internalSaveDone = false;

            Method internalSave = getMethod(WifiManager.class.getMethods(), "save");
        if (internalSave != null)
        {
            try
            {
                /**
                 * TODO: needs pass an instance of the interface WifiManager.ActionListener, in order to receive the status of the call
                 */
                internalSave.invoke(wifiManager, configuration, null);
                internalSaveDone = true;
            }
            catch (Exception e)
            {
                throw new Exception("Exception during call of WifiManager.save method (4.4) : " + e, e);
            }
        }

        return internalSaveDone;
    }


    private static boolean saveNoVersion(WifiManager wifiManager, WifiConfiguration configuration) throws Exception
    {
        boolean internalSaveDone = false;
        Method internalSaveNetwork = null;
        Method internalSave = null;

        try
        {
            internalSaveNetwork = getMethod(WifiManager.class.getMethods(), "saveNetwork");
        }
        catch (Exception e)
        {

        }

        try
        {
            internalSave = getMethod(WifiManager.class.getMethods(), "save");
        }
        catch (Exception e)
        {

        }

        if (internalSave != null)
        {
            Class<?>[] paramsTypes = internalSave.getParameterTypes();
            if (paramsTypes.length == 2)
            {
                internalSave.invoke(wifiManager, configuration, null);
                internalSaveDone = true;
            }
            else if (paramsTypes.length == 1)
            {
                internalSave.invoke(wifiManager, configuration);
                internalSaveDone = true;
            }
            else
            {
                Timber.e("Not handled WifiManager.save method. Found params: " + paramsTypes.length);
            }
        }

        return internalSaveDone;
    }

    public static Constructor getConstructor(Constructor[] constructors, Class[] knownParameters) throws Exception
    {
        Constructor c = null;

        for (Constructor lc : constructors)
        {
            Boolean found = false;

            for (Class knowParam : knownParameters)
            {
                found = false;
                for (Class param : lc.getParameterTypes())
                {
                    if (param.getName().equals(knowParam.getName()))
                    {
                        found = true;
                        break;
                    }
                }

                if (found == false)
                    break;
            }

            if (found)
            {
                c = lc;
                break;
            }
        }

        if (c == null)
            throw new Exception("Constructor not found!");

        return c;
    }

    public static Method getMethod(Method[] methods, String methodName) throws Exception
    {
        Method m = null;

        for (Method lm : methods)
        {
            String currentMethodName = lm.getName();
            if (currentMethodName.equals(methodName))
            {
                m = lm;
                break;
            }
        }

        if (m == null)
            throw new Exception(methodName + " method not found!");

        return m;
    }

    public static Method getMethod(Method[] methods, String methodName, Class[] knownParameters) throws Exception
    {
        Method m = null;

        for (Method lm : methods)
        {
            String currentMethodName = lm.getName();
            if (currentMethodName.equals(methodName))
            {
                Boolean found = false;

                for (Class knowParam : knownParameters)
                {
                    found = false;
                    for (Class param : lm.getParameterTypes())
                    {
                        if (param.getName().equals(knowParam.getName()))
                        {
                            found = true;
                            break;
                        }
                    }

                    if (found == false)
                        break;
                }

                if (found)
                {
                    m = lm;
                    break;
                }
            }
        }

        if (m == null)
            throw new Exception(methodName + " method not found!");

        return m;
    }

    public static List<Method> getMethods(Method[] methods, String methodName) throws Exception
    {
        ArrayList<Method> ml = new ArrayList<Method>();

        for (Method lm : methods)
        {
            String currentMethodName = lm.getName();
            if (currentMethodName.equals(methodName))
            {
                ml.add(lm);
            }
        }

        if (ml.size() == 0)
            throw new Exception(methodName + " method not found!");

        return ml;
    }

    public static Field getField(Field[] fields, String fieldName) throws Exception
    {
        Field f = null;

        for (Field lf : fields)
        {
            String currentFieldName = lf.getName();
            if (currentFieldName.equals(fieldName))
            {
                f = lf;
                break;
            }
        }

        if (f == null)
            throw new Exception(fieldName + " field not found!");

        return f;
    }

    public static Class getDeclaredClass(Class[] classes, String className) throws Exception
    {
        Class c = null;

        for (Class lc : classes)
        {
            String currentFieldName = lc.getName();
            if (currentFieldName.equals(className))
            {
                c = lc;
                break;
            }
        }

        if (c == null)
            throw new Exception(className + " class not found!");

        return c;
    }

    static void describeClassOrInterface(Class className, String name)
    {
        displayModifiers(className.getModifiers());
        displayFields(className.getDeclaredFields());
        displayMethods(className.getDeclaredMethods());

        if (className.isInterface())
        {
            Timber.d("Interface: " + name);
        }
        else
        {
            Timber.d("Class: " + name);
            displayInterfaces(className.getInterfaces());
            displayConstructors(className.getDeclaredConstructors());
        }
    }

    static void displayModifiers(int m)
    {
        Timber.d("Modifiers: " + Modifier.toString(m));
    }

    static void displayInterfaces(Class[] interfaces)
    {
        if (interfaces.length > 0)
        {
            Timber.d("Interfaces: ");
            for (int i = 0; i < interfaces.length; ++i)
                Timber.d(interfaces[i].getName());
        }
    }

    static void displayFields(Field[] fields)
    {
        if (fields.length > 0)
        {
            Timber.d("Fields: ");
            for (int i = 0; i < fields.length; ++i)
                Timber.d(fields[i].toString());
        }
    }

    static void displayConstructors(Constructor[] constructors)
    {
        if (constructors.length > 0)
        {
            Timber.d("Constructors: ");
            for (int i = 0; i < constructors.length; ++i)
                Timber.d(constructors[i].toString());
        }
    }

    static void displayMethods(Method[] methods)
    {
        if (methods.length > 0)
        {
            Timber.d("Methods: ");
            for (int i = 0; i < methods.length; ++i)
                Timber.d(methods[i].toString());
        }
    }

    public static Field[] getAllFields(Class klass)
    {
        List<Field> fields = new ArrayList<Field>();
        fields.addAll(Arrays.asList(klass.getDeclaredFields()));

        if (klass.getSuperclass() != null)
        {
            fields.addAll(Arrays.asList(getAllFields(klass.getSuperclass())));
        }

        return new Field[]{};
    }

    public static WifiConfiguration setProxyFieldsOnWifiConfiguration(WiFiApConfig wiFiApConfig, WifiConfiguration selectedConfiguration) throws Exception
    {
        if (Build.VERSION.SDK_INT >= 20)
        {
            return setFieldsOnWifiConfigurationSDK21(wiFiApConfig, selectedConfiguration);
        }
        else
        {
            return setFieldsOnWifiConfiguration(wiFiApConfig, selectedConfiguration);
        }
    }

    @TargetApi(21)
    private static WifiConfiguration setFieldsOnWifiConfigurationSDK21(WiFiApConfig wiFiApConfig, WifiConfiguration selectedConfiguration) throws Exception
    {
        Constructor wfconfconstr = WifiConfiguration.class.getConstructors()[1];
        WifiConfiguration newConf = (WifiConfiguration) wfconfconstr.newInstance(selectedConfiguration);

        Field mIpConfigurationField = getField(newConf.getClass().getDeclaredFields(), "mIpConfiguration");
        if (mIpConfigurationField != null)
        {
            mIpConfigurationField.setAccessible(true);
            Object mIpConfiguration = mIpConfigurationField.get(newConf);
            if (mIpConfiguration != null)
            {
                Field proxySettingsField = getField(mIpConfiguration.getClass().getFields(), "proxySettings");
                proxySettingsField.set(mIpConfiguration, (Object) proxySettingsField.getType().getEnumConstants()[wiFiApConfig.getProxySetting().ordinal()]);
            }
        }

        Object proxySettings = ReflectionUtils.getProxySetting(newConf);
        int ordinal = ((Enum) proxySettings).ordinal();
        if (ordinal != wiFiApConfig.getProxySetting().ordinal())
            throw new Exception("Cannot set proxySettings variable");

        Object mIpConfiguration = null;
        Field mHttpProxyField = null;

        if (mIpConfigurationField != null)
        {
            mIpConfigurationField.setAccessible(true);
            mIpConfiguration = mIpConfigurationField.get(newConf);
            if (mIpConfiguration != null)
            {
                mHttpProxyField = getField(mIpConfiguration.getClass().getFields(), "httpProxy");
            }
        }

        Constructor constr;
        Object proxyInfo = null;

        if (wiFiApConfig.getProxySetting() == ProxySetting.NONE ||
            wiFiApConfig.getProxySetting() == ProxySetting.UNASSIGNED ||
            !wiFiApConfig.isValidProxyConfiguration())
        {
            constr = ProxyInfo.class.getConstructors()[4];
            proxyInfo = constr.newInstance(null, 0, null);
        }
        else if (wiFiApConfig.getProxySetting() == ProxySetting.STATIC)
        {
            constr = ProxyInfo.class.getConstructors()[4];
            Integer port = wiFiApConfig.getProxyPort();

            proxyInfo = constr.newInstance(wiFiApConfig.getProxyHostString(), port, wiFiApConfig.getProxyExclusionList());
        }
        else if (wiFiApConfig.getProxySetting() == ProxySetting.PAC)
        {
            constr = ProxyInfo.class.getConstructors()[1];
            proxyInfo = constr.newInstance(wiFiApConfig.getPacFileUri());
        }

        mHttpProxyField.set(mIpConfiguration, proxyInfo);

        return newConf;
    }

    private static WifiConfiguration setFieldsOnWifiConfiguration(WiFiApConfig wiFiApConfig, WifiConfiguration selectedConfiguration) throws Exception
    {
        Constructor wfconfconstr = WifiConfiguration.class.getConstructors()[1];
        WifiConfiguration newConf = (WifiConfiguration) wfconfconstr.newInstance(selectedConfiguration);

        Field proxySettingsField = newConf.getClass().getField("proxySettings");
        proxySettingsField.set(newConf, (Object) proxySettingsField.getType().getEnumConstants()[wiFiApConfig.getProxySetting().ordinal()]);

        Object proxySettings = ReflectionUtils.getProxySetting(newConf);
        int ordinal = ((Enum) proxySettings).ordinal();
        if (ordinal != wiFiApConfig.getProxySetting().ordinal())
            throw new Exception("Cannot set proxySettings variable");

        Object linkProperties = null;
        Field mHttpProxyField = null;

        Field linkPropertiesField = newConf.getClass().getField("linkProperties");
        linkProperties = linkPropertiesField.get(newConf);

        mHttpProxyField = getField(linkProperties.getClass().getDeclaredFields(), "mHttpProxy");
        mHttpProxyField.setAccessible(true);

        if (wiFiApConfig.getProxySetting() == ProxySetting.NONE || wiFiApConfig.getProxySetting() == ProxySetting.UNASSIGNED)
        {
            mHttpProxyField.set(linkProperties, null);
        }
        else if (wiFiApConfig.getProxySetting() == ProxySetting.STATIC)
        {
            Class proxyPropertiesClass = mHttpProxyField.getType();
            Integer port = wiFiApConfig.getProxyPort();

            if (port == null)
            {
                Constructor constr = proxyPropertiesClass.getConstructors()[0];
                Object ProxyProperties = constr.newInstance((Object) null);
                mHttpProxyField.set(linkProperties, ProxyProperties);
            }
            else
            {
                Constructor constr;
                Object proxyProperties;

                // NOTE: Hardcoded sdk version number.
                // Instead of comparing against Build.VERSION_CODES.KITKAT, we directly compare against the version
                // number to allow devs to compile with an older version of the sdk.
                if (Build.VERSION.SDK_INT < 19)
                {
                    constr = proxyPropertiesClass.getConstructors()[1];
                }
                else
                {
                    // SDK 19, 20 = KITKAT, KITKAT_WATCH
                    constr = proxyPropertiesClass.getConstructors()[3];
                }

                proxyProperties = constr.newInstance(wiFiApConfig.getProxyHostString(), port, wiFiApConfig.getProxyExclusionList());
                mHttpProxyField.set(linkProperties, proxyProperties);
            }
        }

        return newConf;
    }

    public static Object getHttpProxy(WifiConfiguration wifiConf) throws Exception
    {
        Field mHttpProxyField = null;
        Object httpProxy = null;

        if (Build.VERSION.SDK_INT >= 20)
        {
            httpProxy = ReflectionUtils.getProxyInfo(wifiConf);
        }
        else
        {
            Object linkProperties = ReflectionUtils.getProxyInfo(wifiConf);

            mHttpProxyField = getField(linkProperties.getClass().getDeclaredFields(), "mHttpProxy");
            mHttpProxyField.setAccessible(true);
            httpProxy = mHttpProxyField.get(linkProperties);
        }

        return httpProxy;
    }

    public static Object getProxySetting(WifiConfiguration wifiConf) throws Exception
    {
        Field proxySettingsField = null;
        Object proxySettings = null;

        if (Build.VERSION.SDK_INT >= 20)
        {
            Field mIpConfigurationField = getField(wifiConf.getClass().getDeclaredFields(), "mIpConfiguration");
            if (mIpConfigurationField != null)
            {
                mIpConfigurationField.setAccessible(true);
                Object mIpConfiguration = mIpConfigurationField.get(wifiConf);
                if (mIpConfiguration != null)
                {
                    proxySettingsField = getField(mIpConfiguration.getClass().getFields(), "proxySettings");
                    proxySettings = proxySettingsField.get(mIpConfiguration);
                }
            }
        }
        else
        {
            proxySettingsField = getField(wifiConf.getClass().getFields(), "proxySettings");
            proxySettings = proxySettingsField.get(wifiConf);
        }

        return proxySettings;
    }

    private static Object getProxyInfo(WifiConfiguration wifiConf) throws Exception
    {
        Field proxySettingsField = null;
        Object proxySettings = null;

        if (Build.VERSION.SDK_INT >= 20)
        {
            Field mIpConfigurationField = getField(wifiConf.getClass().getDeclaredFields(), "mIpConfiguration");
            if (mIpConfigurationField != null)
            {
                mIpConfigurationField.setAccessible(true);
                Object mIpConfiguration = mIpConfigurationField.get(wifiConf);
                if (mIpConfiguration != null)
                {
                    proxySettingsField = getField(mIpConfiguration.getClass().getFields(), "httpProxy");
                    proxySettings = proxySettingsField.get(mIpConfiguration);
                }
            }
        }
        else
        {
            proxySettingsField = getField(wifiConf.getClass().getFields(), "linkProperties");
            proxySettings = proxySettingsField.get(wifiConf);
        }

        return proxySettings;
    }
}
