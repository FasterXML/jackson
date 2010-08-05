package org.codehaus.jackson.io;

import java.util.concurrent.atomic.AtomicReference;

/**
 * String token that can lazily serialize String contained and then reuse that
 * serialization later on. This is similar to JDBC prepared statements, for example,
 * in that instances should only be created when they are used more than use;
 * prime candidates are various serializers.
 *<p>
 * Class is final for performance reasons and since this is not designed to
 * be extensible or customizable (customizations would occur in calling code)
 *
 * @since 1.6
 */
public class SerializedString
{
    private final String _value;

    private final AtomicReference<byte[]> _quotedUTF8Ref = new AtomicReference<byte[]>();

    private final AtomicReference<byte[]> _unquotedUTF8Ref = new AtomicReference<byte[]>();

    /**
     * To save bit of memory, let's just use volatile for quoted characters; both because
     * this is unlikely to be used as often, and because use cases are less of high
     * performance ones to begin with.
     */
    private volatile char[] _quotedChars;

    public SerializedString(String v) { _value = v; }

    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    public final String getValue() { return _value; }
    
    public char[] asQuotedChars()
    {
        char[] result = _quotedChars;
        if (result == null) {
            // !!! TBI
            _quotedChars = result;
        }
        return null;
    }

    /**
     * Accessor for accessing value that has been quoted using JSON
     * quoting rules, and encoded using UTF-8 encoding.
     */
    public byte[] asUnquotedUTF8()
    {
        byte[] result = _unquotedUTF8Ref.get();
        if (result == null) {
            // !!! TBI
            _unquotedUTF8Ref.set(result);
        }
        return null;
    }

    /**
     * Accessor for accessing value as is (without JSON quoting)
     * encoded using UTF-8 encoding.
     */
    public byte[] asQuotedUTF8()
    {
        byte[] result = _quotedUTF8Ref.get();
        if (result == null) {
            // !!! TBI
            _quotedUTF8Ref.set(result);
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

}
