package org.codehaus.jackson;

/**
 * Class for parsing exceptions, to indicate non-well-formed document
 * content
 */
public class JsonParseException
    extends JsonProcessingException
{
	final static long serialVersionUID = 123; // Stupid eclipse...

	public JsonParseException(String msg, JsonLocation loc)
    {
        super(msg, loc);
    }

    public JsonParseException(String msg, JsonLocation loc, Throwable root)
    {
        super(msg, loc, root);
    }
}
