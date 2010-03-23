package org.codehaus.jackson.map.deser;

import java.text.DateFormat;
import java.text.ParseException;
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

    public StdDeserializationContext(DeserializationConfig config, JsonParser jp)
    {
    	super(config);
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
        try {
            return getDateFormat().parse(dateStr);
        } catch (ParseException pex) {
            throw new IllegalArgumentException(pex.getMessage());
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
    public JsonMappingException unknownFieldException(Object instanceOrClass, String fieldName)
    {
        String clsName = determineClassName(instanceOrClass);
        return JsonMappingException.from(_parser, "Unrecognized field \""+fieldName+"\" (Class "+clsName+"), not marked as ignorable");
    }

    protected String determineClassName(Object instance)
    {
        if (instance == null) {
            return "unknown";
        }
        Class<?> cls = (instance instanceof Class) ?
            (Class<?>) instance : instance.getClass();
        return cls.getName();
    }

    /*
    ///////////////////////////////////////////////////
    // Overridable internal methods
    ///////////////////////////////////////////////////
     */

    protected DateFormat getDateFormat()
    {
        if (_dateFormat == null) {
            // must create a clone since Formats are not thread-safe:
            _dateFormat = (DateFormat)_config.getDateFormat().clone();
        }
        return _dateFormat;
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
