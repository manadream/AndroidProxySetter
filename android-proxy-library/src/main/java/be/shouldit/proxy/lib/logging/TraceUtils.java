package be.shouldit.proxy.lib.logging;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

public class TraceUtils
{
    /**
     * ANDROID LOG LEVELS
     * <p/>
     * VERBOSE	Constant Value: 2 (0x00000002)
     * DEBUG	Constant Value: 3 (0x00000003)
     * INFO	Constant Value: 4 (0x00000004)
     * WARN	Constant Value: 5 (0x00000005)
     * ERROR	Constant Value: 6 (0x00000006)
     * ASSERT	Constant Value: 7 (0x00000007)
     */

    private Map<String, TraceDate> startTraces;

    private static void log(String tag, String msg, int logLevel)
    {
        Timber.tag(tag);

        switch (logLevel)
        {
            case Log.DEBUG:
                Timber.d(msg);
                break;
            case Log.ERROR:
                Timber.e(msg);
                break;
            case Log.INFO:
                Timber.i(msg);
                break;
            case Log.WARN:
                Timber.w(msg);
                break;
            default:
                Timber.v(msg);
                break;
        }
    }

    public void startTrace(String tag, String msg, int logLevel)
    {
        startTrace(tag, msg, logLevel, false);
    }

    public void startTrace(String tag, String key, int logLevel, boolean showStart)
    {
        startTrace(tag,key,"",logLevel,showStart);
    }

    public void startTrace(String tag, String key, String message, int logLevel, boolean showStart)
    {
        if (startTraces == null)
        {
            startTraces = new ConcurrentHashMap<String, TraceDate>();
        }

        TraceDate traceDate = new TraceDate();
        DateFormat df = DateFormat.getDateTimeInstance();
        if (showStart)
        {
            log(tag, "START " + key + " " + message + " ################## " + df.format(traceDate.getStartTime()) + " #####################################################################", logLevel);
        }

        synchronized (startTraces)
        {
            startTraces.put(key, traceDate);
        }
    }

    public void partialTrace(String tag, String key, int logLevel)
    {
        partialTrace(tag, key, "", logLevel);
    }

    public void partialTrace(String tag, String key, String partialMsg, int logLevel)
    {
        synchronized (startTraces)
        {
            if (startTraces != null && startTraces.containsKey(key))
            {
                TraceDate start = startTraces.get(key);
                Date now = new Date();
                long diffFromLast = now.getTime() - start.getLastTime().getTime();
                long diffFromStart = now.getTime() - start.getStartTime().getTime();
                start.updateLast(now);
                log(tag, "PARTIAL " + key + " " + partialMsg + " %%%%%%%%%%%%% " + diffFromLast + " ms (Tot: " + diffFromStart  + " ms) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%", logLevel);
            }
        }
    }

    public void stopTrace(String tag, String key, int logLevel)
    {
        stopTrace(tag, key, "", logLevel);
    }

    public void stopTrace(String tag, String key, String msg, int logLevel)
    {
        synchronized (startTraces)
        {
            if (startTraces != null && startTraces.containsKey(key))
            {
                TraceDate start = startTraces.get(key);
                Date now = new Date();
                long diffFromLast = now.getTime() - start.getLastTime().getTime();
                long diffFromStart = now.getTime() - start.getStartTime().getTime();
                start.updateLast(now);
                log(tag, "FINISH " + key + " " + msg + " %%%%%%%%%%%%% " + diffFromLast + " ms (Tot: " + diffFromStart  + " ms) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%", logLevel);

                startTraces.remove(key);
            }
        }
    }

    public static void logIntent(String tag, String msg, Intent intent, int logLevel)
    {
        logIntent(tag, msg, intent, logLevel, false);
    }

    public static void logIntent(String tag, Intent intent, int logLevel)
    {
        logIntent(tag, null, intent, logLevel, false);
    }

    public static void logIntent(String tag, Intent intent, int logLevel, boolean logExtras)
    {
        logIntent(tag, null, intent, logLevel, logExtras);
    }

    public static void logIntent(String tag, String msg, Intent intent, int logLevel, boolean logExtras)
    {
        StringBuilder sb = new StringBuilder();

        if (msg != null)
        {
            sb.append(msg);
            sb.append(intent.toString());
        }
        else
        {
            sb.append("LOG Intent: ");
            sb.append(intent.toString());
        }

        if (intent.getAction() != null)
        {
            sb.append(intent.getAction());
            sb.append(" ");
        }

        if (intent.getDataString() != null)
        {
            sb.append(intent.getDataString());
            sb.append(" ");
        }

        if (logExtras)
        {
            Bundle extras = intent.getExtras();
            if (extras != null)
            {
                for (String key : extras.keySet())
                {
                    String extra = String.valueOf(extras.get(key));
                    sb.append(String.format("EXTRA [\"%s\"]: %s ",key,extra));
                }
            }
        }

        log(tag, sb.toString(), logLevel);
    }
}
