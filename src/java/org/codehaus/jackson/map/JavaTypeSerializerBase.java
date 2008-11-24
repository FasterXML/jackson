package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;

/**
 * An empty base implementation of the {@link JavaTypeSerializer} interface. All methods
 * do nothing and return <code>false</code>. Concrete implementation can choose which
 * methods to override.
 *
 * @author Stanislaw Osinski
 */
public class JavaTypeSerializerBase<T> implements JavaTypeSerializer<T>
{
    public boolean writeAny(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen,
        T value) throws IOException, JsonParseException
    {
        return false;
    }
}
