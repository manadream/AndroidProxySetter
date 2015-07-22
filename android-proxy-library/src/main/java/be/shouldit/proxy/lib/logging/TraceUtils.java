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
     *
     * VERBOSE	Constant Value: 2 (0x00000002)
     *  Use this when you want to go absolutely nuts with your logging.
     *  If for some reason you've decided to log every little thing in a particular
     *  part of your app, use the Log.v tag.
     *
     * DEBUG	Constant Value: 3 (0x00000003)
     *  Use this for debugging purposes.
     *  If you want to print out a bunch of messages so you can log the exact flow
     *  of your program, use this. If you want to keep a log of variable values, use this.
     *
     * INFO	    Constant Value: 4 (0x00000004)
     *  Use this to post useful information to the log.
     *  For example: that you have successfully connected to a server.
     *  Basically use it to report successes.
     *
     * WARN	    Constant Value: 5 (0x00000005)
     *  Use this when you suspect something shady is going on.
     *  You may not be completely in full on error mode, but maybe you recovered
     *  from some unexpected behavior. Basically, use this to log stuff you didn't expect
     *  to happen but isn't necessarily an error.
     *
     * ERROR	Constant Value: 6 (0x00000006)
     *  This is for when bad stuff happens.
     *  Use this tag in places like inside a catch statment.
     *  You know that an error has occurred and therefore you're logging an error.
     *
     * ASSERT	Constant Value: 7 (0x00000007)
     *
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
                log(tag, "FINISH " + key + " " + (msg == null? "NULL": msg) + " %%%%%%%%%%%%% " + diffFromLast + " ms (Tot: " + diffFromStart  + " ms) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%", logLevel);

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
        String logString = null;

        try
        {

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

            logString = sb.toString();

            if (intent.getAction() != null)
            {
                sb.append(intent.getAction());
                sb.append(" ");
            }

            logString = sb.toString();

            if (intent.getDataString() != null)
            {
                sb.append(intent.getDataString());
                sb.append(" ");
            }

            logString = sb.toString();

            if (logExtras)
            {
                Bundle extras = intent.getExtras();
                if (extras != null)
                {
                    for (String key : extras.keySet())
                    {
                        String extra = String.valueOf(extras.get(key));
                        sb.append(String.format("EXTRA [\"%s\"]: %s ", key, extra));
                    }
                }
            }

            logString = sb.toString();
        }
        catch (OutOfMemoryError e)
        {
            Timber.e(e,"OutOfMemoryError preparing intent log");
        }

        log(tag, logString, logLevel);
    }

    public long totalElapsedTime(String key)
    {
        Long diffFromStart = null;

        synchronized (startTraces)
        {
            if (startTraces != null && startTraces.containsKey(key))
            {
                TraceDate start = startTraces.get(key);
                Date now = new Date();
                diffFromStart = now.getTime() - start.getStartTime().getTime();

            }
        }

        return diffFromStart;
    }
}
