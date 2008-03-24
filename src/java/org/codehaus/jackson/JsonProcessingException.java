package org.codehaus.jackson;

/**
 * Intermediate base class for all problems when processing
 * JSON input or output that are not pure I/O exceptions
 */
public class JsonProcessingException
    extends java.io.IOException
{
	final static long serialVersionUID = 123; // Stupid eclipse...
	
    protected JsonLocation mLocation;

    protected JsonProcessingException(String msg, JsonLocation loc, Throwable rootCause)
    {
        super(msg);
        if (rootCause != null) {
            initCause(rootCause);
        }
        mLocation = loc;
    }

    protected JsonProcessingException(String msg, JsonLocation loc)
    {
        this(msg, loc, null);
    }

    protected JsonProcessingException(String msg, Throwable rootCause)
    {
        this(msg, null, rootCause);
    }

    protected JsonProcessingException(Throwable rootCause)
    {
        this(null, null, rootCause);
    }

    public JsonLocation getLocation()
    {
        return mLocation;
    }

    /**
     * Default method overridden so that we can add location information
     */
    @Override
    public String getMessage()
    {
        String msg = super.getMessage();
        JsonLocation loc = getLocation();
        if (loc != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(msg);
            sb.append('\n');
            sb.append(" at ");
            sb.append(loc.toString());
            return sb.toString();
        }
        return msg;
    }

    @Override
    public String toString() {
        return getClass().getName()+": "+getMessage();
    }
}
