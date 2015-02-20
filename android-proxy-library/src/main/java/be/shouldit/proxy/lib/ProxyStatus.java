package be.shouldit.proxy.lib;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import be.shouldit.proxy.lib.enums.CheckStatusValues;
import be.shouldit.proxy.lib.enums.ProxyStatusProperties;
import timber.log.Timber;

public class ProxyStatus implements Serializable
{
	public static final String TAG = ProxyStatus.class.getSimpleName();
	private static final long serialVersionUID = -2657093750716229587L;

	SortedMap<ProxyStatusProperties, ProxyStatusItem> properties;
	public Date checkedDate;

	public String getCheckedDateString()
	{
		if (checkedDate != null)
		{
			DateFormat df = DateFormat.getDateTimeInstance();
			return df.format(checkedDate);
		}
		else
			return null;
	}

	public CheckStatusValues getCheckingStatus()
	{
		synchronized (this)
		{
			for (ProxyStatusItem prop : properties.values())
			{
				if (prop.effective)
				{
					if (prop.status == CheckStatusValues.NOT_CHECKED)
						return CheckStatusValues.NOT_CHECKED;
				}
			}

			for (ProxyStatusItem prop : properties.values())
			{
				if (prop.effective)
				{
					if (prop.status == CheckStatusValues.CHECKING)
						return CheckStatusValues.CHECKING;
				}
			}

			return CheckStatusValues.CHECKED;
		}
	}

	public ProxyStatusItem getProperty(ProxyStatusProperties property)
	{
		synchronized (this)
		{
			return properties.get(property);
		}
	}

	public ProxyStatus()
	{
		clear();
	}

	public void clear()
	{
		synchronized (this)
		{
            checkedDate = null;
			properties = Collections.synchronizedSortedMap(new TreeMap<ProxyStatusProperties, ProxyStatusItem>(new ProxyStatusPropertiesComparator()));

			properties.put(ProxyStatusProperties.WIFI_ENABLED, new ProxyStatusItem(ProxyStatusProperties.WIFI_ENABLED));
			properties.put(ProxyStatusProperties.WIFI_SELECTED, new ProxyStatusItem(ProxyStatusProperties.WIFI_SELECTED));
			properties.put(ProxyStatusProperties.WEB_REACHABLE, new ProxyStatusItem(ProxyStatusProperties.WEB_REACHABLE));
			properties.put(ProxyStatusProperties.PROXY_ENABLED, new ProxyStatusItem(ProxyStatusProperties.PROXY_ENABLED));
			properties.put(ProxyStatusProperties.PROXY_VALID_HOSTNAME, new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_HOSTNAME));
			properties.put(ProxyStatusProperties.PROXY_VALID_PORT, new ProxyStatusItem(ProxyStatusProperties.PROXY_VALID_PORT));
			properties.put(ProxyStatusProperties.PROXY_REACHABLE, new ProxyStatusItem(ProxyStatusProperties.PROXY_REACHABLE));
		}
	}

	public void startchecking()
	{
		synchronized (this)
		{
			checkedDate = new Date();

			for (ProxyStatusItem prop : properties.values())
			{
				prop.status = CheckStatusValues.CHECKING;
			}
		}
	}

	public void set(ProxyStatusItem item)
	{
		set(item.statusCode, item.status, item.result, item.effective, item.message, new Date());
	}

	public void set(ProxyStatusProperties psp, CheckStatusValues stat, Boolean res, String msg, Date checkDate)
	{
		set(psp, stat, res, true, msg, checkDate);
	}
	
	public void set(ProxyStatusProperties psp, CheckStatusValues stat, boolean res, boolean effect)
	{
		set(psp, stat, res, effect, "", new Date());
	}

	public void set(ProxyStatusProperties psp, CheckStatusValues stat, Boolean res, Boolean effect, String msg, Date checkDate)
	{
		synchronized (this)
		{
			if (properties.containsKey(psp))
			{
				properties.get(psp).status = stat;
				properties.get(psp).result = res;
				properties.get(psp).effective = effect;
				properties.get(psp).message = msg;
				properties.get(psp).checkedDate = checkDate;
			}
			else
			{
                Timber.e("Cannot find status code: " + psp);
			}
		}
	}

	public ProxyStatusItem getMostRelevantErrorProxyStatusItem()
	{
		synchronized (this)
		{
			for (ProxyStatusItem prop : properties.values())
			{
				if (prop.effective)
				{
					if (prop.result == false)
					{
						return prop;
					}
				}
			}

			return null;
		}
	}

	public Integer getErrorCount()
	{
		synchronized (this)
		{
			int count = 0;
			for (ProxyStatusItem prop : properties.values())
			{
				if (prop.effective)
				{
					if (prop.result == false)
					{
						count++;
					}
				}
			}

			return count;
		}
	}

	@Override
	public String toString()
	{
		synchronized (this)
		{
			StringBuilder sb = new StringBuilder();

            if (checkedDate != null)
            {
                DateFormat df = DateFormat.getDateTimeInstance();
			    sb.append("Start checking at: " + df.format(checkedDate) + "\n");
            }
            else
            {
                sb.append("Not checked");
            }

			for (ProxyStatusItem prop : properties.values())
			{
				sb.append(prop.toString() + "\n");
			}

			return sb.toString();
		}
	}

    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();

        try
        {
            if (checkedDate != null)
            {
                DateFormat df = DateFormat.getDateTimeInstance();
                jsonObject.put("checked_date",df.format(checkedDate));
            }
            else
            {
                jsonObject.put("checked_date", null);
            }

            JSONArray propertiesArray = new JSONArray();
            for (ProxyStatusItem prop : properties.values())
            {
                propertiesArray.put(prop.toJSON());
            }

            jsonObject.put("check_properties", propertiesArray);
        }
        catch (JSONException e)
        {
            Timber.e(e, "Exception converting to JSON object ProxyStatus");
        }

        return jsonObject;
    }

    public String toShortString()
    {
        synchronized (this)
        {
            StringBuilder sb = new StringBuilder();

            for (ProxyStatusItem prop : properties.values())
            {
                if (prop.effective) // Print only effective status items
                {
                    sb.append(prop.toShortString() + " - ");
                }
            }

            return sb.toString();
        }
    }


}
