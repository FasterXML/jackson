package org.codehaus.jackson.map.deser.std;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;

public class AtomicBooleanDeserializer
    extends StdScalarDeserializer<AtomicBoolean>
{
    public AtomicBooleanDeserializer() { super(AtomicBoolean.class); }
    
    @Override
    public AtomicBoolean deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // 16-Dec-2010, tatu: Should we actually convert null to null AtomicBoolean?
        return new AtomicBoolean(_parseBooleanPrimitive(jp, ctxt));
    }
}
