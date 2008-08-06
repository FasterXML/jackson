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

    /**
     * Defines whether (escaped) linefeeds are included when serializing
     * binary data into base64 values or not.
     *<p>
     * Default setting is <b>false</b> mostly because linefeeds can not
     * be included natively anyway, and instead encoded/escaped entries
     * have to be used. Additionally it is unlikely that recipient would
     * not be able to decode data (since it needs to be json aware and
     * do fair bit of handling before being able to access data).
     * Nonetheless, for maximum interoperability it may be desireable
     * to enable this setting.
     */
    protected boolean mCfgBase64LFs = false;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (construction, configuration)
    ////////////////////////////////////////////////////
     */

    public BaseMapper() { }

    public void setDupFieldHandling(DupFields mode) { mCfgDupFields = mode; }
    public DupFields getDupFieldHandling() { return mCfgDupFields; }

    public void setAddLinefeedsToBase64(boolean state) { mCfgBase64LFs = state; }

    public boolean getAddLinefeedsToBase64() { return mCfgBase64LFs; }

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
