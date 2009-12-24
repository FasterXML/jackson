package org.codehaus.jackson.util;

import org.codehaus.jackson.*;

/**
 * Utility class used for efficient storage of {@link JsonToken}
 * sequences, needed for temporary buffering.
 * Space efficient for different sequence lengths (especially so for smaller
 * ones; but not significantly less efficient for larger), highly efficient
 * for linear iteration and appending. Implemented as segmented/chunked
 * linked list of tokens; only modifications are via appends.
 * 
 * @since 1.5
 */
public class TokenBuffer
{
    protected Segment _first, _last;
    
    public TokenBuffer()
    {
        // at first we have just one segment
        _first = _last = new Segment();
    }

    /**
     * Method used to create a {@link JsonParser} that can read contents
     * stored in this buffer.
     *<p>
     * Note: instances are not synchronized, that is, they are not thread-safe
     * if there are concurrent appends to the underlying buffer.
     * 
     * @return Parser that can be used for reading contents stored in this buffer
     */
    public JsonParser asParser() {
        // !!! TBI
        return null;
    }

    /*
    *****************************************************************
    * Supporting classes
    *****************************************************************
     */
    
    /**
     * Individual segment of TokenBuffer that can store up to 10 tokens.
     * This variant is immutable
     */
    protected static class Segment 
    {
        // // // First, type marker constants

        /**
         * Marker that denotes "empty slot"; used 
         */
        private final static int TYPE_NONE = 0;

        /**
         * Marker for start and end markers of JSON arrays and objects.
         */
        private final static int TYPE_STRUCTURE_MARKER = 0;

        /**
         * Marker for field names
         */
        private final static int TYPE_FIELD_NAME = 1;

        /**
         * JSON integral numeric value
         */
        private final static int TYPE_VALUE_NUMBER_INT = 2;

        /**
         * JSON floating point numeric value
         */
        private final static int TYPE_VALUE_NUMBER_FLOAT = 3;

        /**
         * JSON String value
         */
        private final static int TYPE_VALUE_STRING = 4;

        /**
         * JSON literal: "null", "true" or "false"
         */
        private final static int TYPE_VALUE_LITERAL = 5;

        /**
         * Embedded object or JSON "null" literal
         */
        private final static int TYPE_OBJECT = 6;

        // ... which means we have marker for one more thing if we want

        // // // State

        /**
         * Number of tokens this segment contains
         */
        protected int _tokenCount;

        /**
         * Bit field used to store 3-bit markers for each token
         */
        protected int _typeMarkers;
        
        // Actual tokens
        
        protected Object _token0;
        protected Object _token1;
        protected Object _token2;
        protected Object _token3;
        protected Object _token4;
        protected Object _token5;
        protected Object _token6;
    }
}
