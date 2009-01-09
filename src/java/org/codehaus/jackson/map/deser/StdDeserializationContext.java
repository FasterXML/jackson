package org.codehaus.jackson.map.deser;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.util.ObjectBuffer;

/**
 * Default implementation of {@link DeserializationContext}.
 */
public class StdDeserializationContext
    extends DeserializationContext
{
    /**
     * Let's limit length of error messages, for cases where underlying data
     * may be very large -- no point in spamming logs with megs of meaningless
     * data.
     */
    final static int MAX_ERROR_STR_LEN = 500;

    // // // Configuration

    protected final JsonParser _parser;

    // // // Helper object recycling

    protected ObjectBuffer _objectBuffer;

    protected DateFormat _dateFormat;

    // // // Construction

    public StdDeserializationContext(JsonParser jp)
    {
        _parser = jp;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, accessors
    ///////////////////////////////////////////////////
     */

    public JsonParser getParser() { return _parser; }

    /*
    ///////////////////////////////////////////////////
    // Public API, helper object recycling
    ///////////////////////////////////////////////////
     */

    public final ObjectBuffer leaseObjectBuffer()
    {
        ObjectBuffer buf = _objectBuffer;
        if (buf == null) {
            buf = new ObjectBuffer();
        } else {
            _objectBuffer = null;
        }
        return buf;
    }

    public final void returnObjectBuffer(ObjectBuffer buf)
    {
        /* Already have a reusable buffer? Let's retain bigger one
         * (or if equal, favor newer one, shorter life-cycle)
         */
        if (_objectBuffer == null
            || buf.initialCapacity() >= _objectBuffer.initialCapacity()) {
            _objectBuffer = buf;
        }
    }

    /*
    //////////////////////////////////////////////////////////////
    // Parsing methods that may use reusable/-cyclable objects
    //////////////////////////////////////////////////////////////
    */

    public Date parseDate(String dateStr)
        throws IllegalArgumentException
    {
        dateStr = dateStr.trim();
        /* Although DateFormat is not thread-safe, serialization contexts
         * are never shared, so we are safe to use it without locking
         */
        if (_dateFormat == null) {
            _dateFormat = _constructDateFormat();
        }
        try {
            return _dateFormat.parse(dateStr);
        } catch (ParseException pex) {
            throw new IllegalArgumentException(pex.getMessage(), pex);
        }
    }

    public Calendar constructCalendar(Date d)
    {
        /* 08-Jan-2008, tatu: not optimal, but should work for the
         *   most part; let's revise as needed.
         */
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, problem handling
    ///////////////////////////////////////////////////
     */

    public JsonMappingException mappingException(Class<?> targetClass)
    {
        return JsonMappingException.from(_parser, "Can not deserialize "+targetClass.getName()+" out of "+_parser.getCurrentToken()+" token");
    }

    public JsonMappingException instantiationException(Class<?> instClass, Exception e)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+", problem: "+e.getMessage());
    }

    /**
     * Method that will construct an exception suitable for throwing when
     * some String values are acceptable, but the one encountered is not
     */
    public JsonMappingException weirdStringException(Class<?> instClass, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+" from String value '"+_valueDesc()+"': "+msg);
    }

    public JsonMappingException weirdNumberException(Class<?> instClass, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+" from number value ("+_valueDesc()+"): "+msg);
    }

    public JsonMappingException weirdKeyException(Class<?> keyClass, String keyValue, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct Map key of type "+keyClass.getName()+" from String \""+_desc(keyValue)+"\": "+msg);
    }

    public JsonMappingException unknownFieldException(Object instance, String fieldName)
    {
        return JsonMappingException.from(_parser, "Unrecognized field \""+fieldName+"\" (Class "+instance.getClass().getName()+"), not marked as ignorable");
    }

    /*
    ///////////////////////////////////////////////////
    // Overridable internal methods
    ///////////////////////////////////////////////////
     */

    /* 07-Jan-2009, tatu: Let's first try parsing using what seems like
     *   a good standard timestamp format. Chances are we need to improve
     *   on this... but have to start somewhere.
     */
    final static DateFormat _stdDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    //final static DateFormat _stdDateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);

    /**
     * Method that can be overridden to provide specific DateFormat
     * when one is needed for parsing (during deserialization)
     */
    protected DateFormat _constructDateFormat()
    {
        /* Should be bit more efficient to actually clone pre-parsed
         * static instance...
         */
        return (DateFormat) _stdDateFormat.clone();
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

    protected String _valueDesc()
    {
        try {
            return _desc(_parser.getText());
        } catch (Exception e) {
            return "[N/A]";
        }
    }
    protected String _desc(String desc)
    {
        // !!! should we quote it? (in case there are control chars, linefeeds)
        if (desc.length() > MAX_ERROR_STR_LEN) {
            desc = desc.substring(0, MAX_ERROR_STR_LEN) + "]...[" + desc.substring(desc.length() - MAX_ERROR_STR_LEN);
        }
        return desc;
    }
}
