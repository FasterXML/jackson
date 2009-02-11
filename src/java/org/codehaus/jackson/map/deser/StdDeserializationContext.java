package org.codehaus.jackson.map.deser;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.util.ArrayBuilders;
import org.codehaus.jackson.map.util.ObjectBuffer;

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

    protected ArrayBuilders _arrayBuilders;

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

    @Override
	public JsonParser getParser() { return _parser; }

    /*
    ///////////////////////////////////////////////////
    // Public API, helper object recycling
    ///////////////////////////////////////////////////
     */

    @Override
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

    @Override
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

    @Override
	public final ArrayBuilders getArrayBuilders()
    {
        if (_arrayBuilders == null) {
            _arrayBuilders = new ArrayBuilders();
        }
        return _arrayBuilders;
    }

    /*
    //////////////////////////////////////////////////////////////
    // Parsing methods that may use reusable/-cyclable objects
    //////////////////////////////////////////////////////////////
    */

    @Override
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

    @Override
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

    @Override
	public JsonMappingException mappingException(Class<?> targetClass)
    {
        String clsName = _calcName(targetClass);
        return JsonMappingException.from(_parser, "Can not deserialize instance of "+clsName+" out of "+_parser.getCurrentToken()+" token");
    }

    protected String _calcName(Class<?> cls)
    {
        if (cls.isArray()) {
            return _calcName(cls.getComponentType())+"[]";
        }
        return cls.getName();
    }

    @Override
	public JsonMappingException instantiationException(Class<?> instClass, Exception e)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+", problem: "+e.getMessage());
    }

    /**
     * Method that will construct an exception suitable for throwing when
     * some String values are acceptable, but the one encountered is not
     */
    @Override
	public JsonMappingException weirdStringException(Class<?> instClass, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+" from String value '"+_valueDesc()+"': "+msg);
    }

    @Override
	public JsonMappingException weirdNumberException(Class<?> instClass, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+" from number value ("+_valueDesc()+"): "+msg);
    }

    @Override
	public JsonMappingException weirdKeyException(Class<?> keyClass, String keyValue, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct Map key of type "+keyClass.getName()+" from String \""+_desc(keyValue)+"\": "+msg);
    }

    @Override
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
