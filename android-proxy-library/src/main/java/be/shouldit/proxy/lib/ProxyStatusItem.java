package be.shouldit.proxy.lib;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

import be.shouldit.proxy.lib.enums.CheckStatusValues;
import be.shouldit.proxy.lib.enums.ProxyStatusProperties;
import timber.log.Timber;

public class ProxyStatusItem implements Parcelable
{
	public ProxyStatusProperties statusCode;
	public CheckStatusValues status;
	public Boolean result;
	public Boolean effective;
	public String message;
	public Date checkedDate;
	
	public ProxyStatusItem(ProxyStatusProperties code, CheckStatusValues st, Boolean val, Boolean eff, String msg, Date date)
	{
		statusCode = code;
		status = st;
		result = val;
		effective = eff;
		message = msg;
		checkedDate = date;
	}
	
	public ProxyStatusItem(ProxyStatusProperties code, CheckStatusValues st, Boolean val, Boolean eff, String msg)
	{
		this(code,st,val,eff,msg,null);
	}
	
	public ProxyStatusItem(ProxyStatusProperties code, CheckStatusValues st, Boolean eff, Boolean val)
	{
		this(code,st,val,eff, "",null);
	}
	
	public ProxyStatusItem(ProxyStatusProperties code)
	{
		this(code,CheckStatusValues.NOT_CHECKED, false, true, "",null);
	}
	
	public ProxyStatusItem(ProxyStatusProperties code, CheckStatusValues checked, boolean res, String msg)
	{
		this(code,checked, res, true, msg,null);
	}

	public ProxyStatusItem(ProxyStatusProperties code, CheckStatusValues checked, boolean res)
	{
		this(code,checked, res, true, "",null);
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("%s (Effective: %s, Status: %s, Result: %s", statusCode, effective, status, result));

        if (checkedDate != null)
        {
            DateFormat df = DateFormat.getDateTimeInstance();
            sb.append(", Checked at: " + df.format(checkedDate));
        }

		if (message != null && message.length() > 0)
			sb.append(", Message: " + message);
		
		sb.append(")");
		
		return sb.toString();
	}

    public String toShortString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%s %s/%s", statusCode, status, result));

        return sb.toString();
    }

    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();

        try
        {
            jsonObject.put("status", statusCode);
            jsonObject.put("effective", effective);
            jsonObject.put("status",status);
            jsonObject.put("result",result);

            if (checkedDate != null)
            {
                DateFormat df = DateFormat.getDateTimeInstance();
                jsonObject.put("checked_date",df.format(checkedDate));
            }
            else
            {
                jsonObject.put("checked_date",null);
            }

            jsonObject.put("message",message);
        }
        catch (JSONException e)
        {
            Timber.e(e, "Exception converting to JSON object ProxyStatusItem");
        }
        return jsonObject;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(this.statusCode == null ? -1 : this.statusCode.ordinal());
        dest.writeInt(this.status == null ? -1 : this.status.ordinal());
        dest.writeValue(this.result);
        dest.writeValue(this.effective);
        dest.writeString(this.message);
        dest.writeLong(checkedDate != null ? checkedDate.getTime() : -1);
    }

    private ProxyStatusItem(Parcel in)
    {
        int tmpStatusCode = in.readInt();
        this.statusCode = tmpStatusCode == -1 ? null : ProxyStatusProperties.values()[tmpStatusCode];
        int tmpStatus = in.readInt();
        this.status = tmpStatus == -1 ? null : CheckStatusValues.values()[tmpStatus];
        this.result = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.effective = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.message = in.readString();
        long tmpCheckedDate = in.readLong();
        this.checkedDate = tmpCheckedDate == -1 ? null : new Date(tmpCheckedDate);
    }

    public static final Creator<ProxyStatusItem> CREATOR = new Creator<ProxyStatusItem>()
    {
        public ProxyStatusItem createFromParcel(Parcel source) {return new ProxyStatusItem(source);}

        public ProxyStatusItem[] newArray(int size) {return new ProxyStatusItem[size];}
    };

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ProxyStatusItem)) return false;

        ProxyStatusItem that = (ProxyStatusItem) o;

        if (checkedDate != null ? !checkedDate.equals(that.checkedDate) : that.checkedDate != null)
            return false;
        if (effective != null ? !effective.equals(that.effective) : that.effective != null)
            return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (result != null ? !result.equals(that.result) : that.result != null) return false;
        if (status != that.status) return false;
        if (statusCode != that.statusCode) return false;

        return true;
    }
}
