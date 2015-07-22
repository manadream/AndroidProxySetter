package be.shouldit.proxy.lib.enums;

/**
 * Created by Marco on 15/09/13.
 */
public enum CheckStatusValues
{
    NOT_CHECKED,
    CHECKING,
    CHECKED;

    @Override
    public String toString()
    {
        switch (this)
        {
            case NOT_CHECKED:
                return "N";
            case CHECKING:
                return "?";
            case CHECKED:
                return "C";
        }

        return "?";
    }
}