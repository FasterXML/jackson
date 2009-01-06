package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.*;

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
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+" from String value '"+valueDesc()+"': "+msg);
    }

    public JsonMappingException weirdNumberException(Class<?> instClass, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+" from number value ("+valueDesc()+"): "+msg);
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

    protected String valueDesc()
    {
        String text;
        try {
            text = _parser.getText();
        } catch (Exception e) {
            text = "[N/A]";
        }

        // !!! should we quote it?
        if (text.length() > MAX_ERROR_STR_LEN) {
            text = text.substring(0, MAX_ERROR_STR_LEN) + "]...[" + text.substring(text.length() - MAX_ERROR_STR_LEN);
        }
        return text;
    }
}
