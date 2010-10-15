package org.codehaus.jackson.map.deser;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Currency;
import java.util.UUID;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;

/**
 * Base class for simple deserializer which only accept Json String
 * values as the source.
 */
public abstract class FromStringDeserializer<T>
    extends StdScalarDeserializer<T>
{
    protected FromStringDeserializer(Class<?> vc) {
        super(vc);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
            String text = jp.getText().trim();
            // 15-Oct-2010, tatu: Empty String usually means null, so
            if (text.length() == 0) {
                return null;
            }
            try {
                T result = _deserialize(text, ctxt);
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException iae) {
                // nothing to do here, yet? We'll fail anyway
            }
            throw ctxt.weirdStringException(_valueClass, "not a valid textual representation");
        }
        if (jp.getCurrentToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
            // Trivial cases; null to null, instance of type itself returned as is
            Object ob = jp.getEmbeddedObject();
            if (ob == null) {
                return null;
            }
            if (_valueClass.isAssignableFrom(ob.getClass())) {
                return (T) ob;
            }
            return _deserializeEmbedded(ob, ctxt);
        }
        throw ctxt.mappingException(_valueClass);
    }
        
    protected abstract T _deserialize(String value, DeserializationContext ctxt)
        throws IOException, JsonProcessingException;

    protected T _deserializeEmbedded(Object ob, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // default impl: error out
        throw ctxt.mappingException("Don't know how to convert embedded Object of type "
                +ob.getClass().getName()+" into "+_valueClass.getName());
    }
    
    /*
    /**********************************************************
    /* Then concrete implementations
    /**********************************************************
     */

    public static class UUIDDeserializer
        extends FromStringDeserializer<UUID>
    {
        public UUIDDeserializer() { super(UUID.class); }
        
        protected UUID _deserialize(String value, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return UUID.fromString(value);
        }

        protected UUID _deserializeEmbedded(Object ob, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (ob instanceof byte[]) {
                byte[] bytes = (byte[]) ob;
                if (bytes.length != 16) {
                    ctxt.mappingException("Can only construct UUIDs from 16 byte arrays; got "+bytes.length+" bytes");
                }
                // clumsy, but should work for now...
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
                long l1 = in.readLong();
                long l2 = in.readLong();
                return new UUID(l1, l2);
            }
            super._deserializeEmbedded(ob, ctxt);
            return null; // never gets here
        }
    }

    public static class URLDeserializer
        extends FromStringDeserializer<URL>
    {
        public URLDeserializer() { super(URL.class); }
        
        @Override
        protected URL _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            return new URL(value);
        }
    }

    public static class URIDeserializer
        extends FromStringDeserializer<URI>
    {
        public URIDeserializer() { super(URI.class); }

        @Override
        protected URI _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            return URI.create(value);
        }
    }

    public static class CurrencyDeserializer
        extends FromStringDeserializer<Currency>
    {
        public CurrencyDeserializer() { super(Currency.class); }
        
        @Override
        protected Currency _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            // will throw IAE if unknown:
            return Currency.getInstance(value);
        }
    }

    public static class PatternDeserializer
        extends FromStringDeserializer<Pattern>
    {
        public PatternDeserializer() { super(Pattern.class); }
        
        @Override
        protected Pattern _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            // will throw IAE (or its subclass) if malformed
            return Pattern.compile(value);
        }
    }
}
