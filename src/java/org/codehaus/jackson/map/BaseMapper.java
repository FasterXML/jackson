package org.codehaus.jackson.map;

import org.codehaus.jackson.*;

/**
 * This base class defines API aspects that are shared
 * between different concrete mapper types.
 */
public abstract class BaseMapper
{
    /*
    ////////////////////////////////////////////////////
    // Shared public enums for configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Enumeration that defines strategies available for dealing with
     * duplicate field names (when mapping JSON to Java types).
     */
    public enum DupFields {
        ERROR /* default */
            , USE_FIRST
            , USE_LAST
            ;
    }

    /*
    ////////////////////////////////////////////////////
    // Common config settings
    ////////////////////////////////////////////////////
     */

    /**
     * This option defines how duplicate field names (from JSON input)
     * are to be handled. Default is to throw a {@link JsonParseException}.
     */
    protected DupFields mCfgDupFields = DupFields.ERROR;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (construction, configuration)
    ////////////////////////////////////////////////////
     */

    public BaseMapper() { }

    public void setDupFieldHandling(DupFields mode) { mCfgDupFields = mode; }
    public DupFields getDupFieldHandling() { return mCfgDupFields; }

    /*
    ////////////////////////////////////////////////////
    // Methods for sub-classes
    ////////////////////////////////////////////////////
     */

    protected void throwInternal(String msg)
    {
        throw new RuntimeException("Internal error: "+msg);
    }

    protected void reportProblem(JsonParser jp, String msg)
        throws JsonParseException
    {
        throw new JsonParseException(msg, jp.getTokenLocation());
    }
}
