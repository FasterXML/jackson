package org.codehaus.jackson.map;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Iterator exposed by {@link ObjectMapper} when binding sequence of
 * objects. Extension is done to allow more convenient exposing of
 * {@link IOException} (which basic {@link Iterator} does not expose)
 * 
 * @since 1.8
 */
public class MappingIterator<T> implements Iterator<T>
{
    protected final static MappingIterator<?> EMPTY_ITERATOR = new MappingIterator<Object>(null, null, null, null);
    
    protected final JavaType _type;

    protected final DeserializationContext _context;
    
    protected final JsonDeserializer<T> _deserializer;

    protected final JsonParser _parser;
    
    @SuppressWarnings("unchecked")
    protected MappingIterator(JavaType type, JsonParser jp, DeserializationContext ctxt,
            JsonDeserializer<?> deser)
    {
        _type = type;
        _parser = jp;
        _context = ctxt;
        _deserializer = (JsonDeserializer<T>) deser;

        /* One more thing: if we are at START_ARRAY (but NOT root-level
         * one!), advance to next token (to allow matching END_ARRAY)
         */
        if (jp != null && jp.getCurrentToken() == JsonToken.START_ARRAY) {
            JsonStreamContext sc = jp.getParsingContext();
            // safest way to skip current token is to clear it (so we'll advance soon)
            if (!sc.inRoot()) {
                jp.clearCurrentToken();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> MappingIterator<T> emptyIterator() {
        return (MappingIterator<T>) EMPTY_ITERATOR;
    }
    
    /*
    /**********************************************************
    /* Basic iterator impl
    /**********************************************************
     */

    @Override
    public boolean hasNext()
    {
        try {
            return hasNextValue();
        } catch (JsonMappingException e) {
            throw new RuntimeJsonMappingException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public T next()
    {
        try {
            return nextValue();
        } catch (JsonMappingException e) {
            throw new RuntimeJsonMappingException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override public void remove() {
        throw new UnsupportedOperationException();
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Equivalent of {@link #next} but one that may throw checked
     * exceptions from Jackson due to invalid input.
     */
    public boolean hasNextValue() throws IOException
    {
        if (_parser == null) {
            return false;
        }
        JsonToken t = _parser.getCurrentToken();
        if (t == null) { // un-initialized or cleared; find next
            t = _parser.nextToken();
            // If EOF, no more
            if (t == null) {
                _parser.close();
                return false;
            }
            // And similarly if we hit END_ARRAY; except that we won't close parser
            if (t == JsonToken.END_ARRAY) {
                return false;
            }
        }
        return true;
    }
    
    public T nextValue() throws IOException
    {
        T result = _deserializer.deserialize(_parser, _context);
        // Need to consume the token too
        _parser.clearCurrentToken();
        return result;
    }
}
