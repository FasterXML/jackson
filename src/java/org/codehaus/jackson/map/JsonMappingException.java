package org.codehaus.jackson.map;

import org.codehaus.jackson.*;

/**
 * Checked exception used to signal fatal problems with mapping of
 * content.
 */
@SuppressWarnings("serial")
public class JsonMappingException
    extends JsonProcessingException
{
    public JsonMappingException(String msg)
    {
        super(msg);
    }

    public JsonMappingException(String msg, JsonLocation loc)
    {
        super(msg, loc);
    }

    public JsonMappingException(String msg, JsonLocation loc, Throwable rootCause)
    {
        super(msg, loc, rootCause);
    }

    public static JsonMappingException from(JsonParser jp, String msg)
    {
        return new JsonMappingException(msg, jp.getTokenLocation());
    }

    public static JsonMappingException from(JsonParser jp, String msg,
                                            Throwable problem)
    {
        return new JsonMappingException(msg, jp.getTokenLocation(), problem);
    }
}
