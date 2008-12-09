package org.codehaus.jackson.map.impl.prov;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerFactory;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Default implementation of
 */
public abstract class StdSerializerProvider
    extends SerializerProviderBase
{
    /**
     * Serializer factory 
     */
    //final JsonSerializerFactory _primarySerializerFactory;

    public StdSerializerProvider()
    {
        this(null);
    }

    public StdSerializerProvider(JsonSerializerFactory customSerializerFactory)
    {

        //this(customSerializeFactory, defaultSerializerFactory);
    }

    @Override
    public abstract <T> JsonSerializer<T> findValueSerializer(Class<T> type);

    @Override
    public abstract <T> JsonSerializer<T> findNonNullKeySerializer(Class<T> type);

    @Override
    public abstract JsonSerializer<?> getNullKeySerializer();

    @Override
    public abstract JsonSerializer<?> getNullValueSerializer();
}

