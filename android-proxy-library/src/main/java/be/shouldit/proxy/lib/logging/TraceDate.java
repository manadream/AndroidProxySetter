package be.shouldit.proxy.lib.logging;

import java.util.Date;

/**
 * Created by mpagliar on 27/08/2014.
 */
public class TraceDate
{
    public Date getStartTime()
    {
        return startTime;
    }

    public Date getLastTime()
    {
        return lastTime;
    }

    private Date startTime;
    private Date lastTime;

    public TraceDate()
    {
        startTime = new Date();
        lastTime = new Date();
    }

    public void updateLast(Date now)
    {
        lastTime = now;
    }
}
