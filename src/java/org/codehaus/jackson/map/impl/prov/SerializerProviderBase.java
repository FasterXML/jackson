package org.codehaus.jackson.map.impl.prov;

import java.util.*;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Abstract base class that defines lowest-level common feature of most
 * implementations of {@JsonSerializerProvider}: that of locally
 * storing references to serializers once they have been succesfully
 * constructed.
 */
public abstract class SerializerProviderBase
    extends JsonSerializerProvider
{
    public abstract <T> JsonSerializer<T> findSerializer(Class<T> type);
}
