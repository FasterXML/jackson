package org.codehaus.jackson.map;

import java.io.IOException;

/**
 * This class provides backwards compatibility to reduce impact
 * of fixing [JACKSON-30].
 *
 * @deprecated Use {@link TreeMapper} directly
 */
public class JsonTypeMapper extends TreeMapper
{
    public JsonTypeMapper() { super(); }
}
