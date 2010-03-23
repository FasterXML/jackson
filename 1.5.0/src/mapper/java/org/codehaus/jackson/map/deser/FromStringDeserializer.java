package org.codehaus.jackson.map.deser;

import java.io.IOException;
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

    public final T deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
            String text = jp.getText();
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
        throw ctxt.mappingException(_valueClass);
    }
        
    protected abstract T _deserialize(String value, DeserializationContext ctxt)
        throws IOException, JsonProcessingException;

    /*
    /////////////////////////////////////////////////////////////
    // Then concrete implementations
    /////////////////////////////////////////////////////////////
    */

    public static class UUIDDeserializer
        extends FromStringDeserializer<UUID>
    {
        public UUIDDeserializer() { super(UUID.class); }
        
        protected UUID _deserialize(String value, DeserializationContext ctxt)
        {
            return UUID.fromString(value);
        }
    }

    public static class URLDeserializer
        extends FromStringDeserializer<URL>
    {
        public URLDeserializer() { super(URL.class); }
        
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
        
        protected Pattern _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            // will throw IAE (or its subclass) if malformed
            return Pattern.compile(value);
        }
    }
}
