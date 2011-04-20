package org.codehaus.jackson.io;

import java.io.*;

/**
 * Handler class that can be used to decorate input sources.
 * Typical use is to use a filter abstraction (filtered stream,
 * reader) around original input source, and apply additional
 * processing during read operations.
 * 
 * @since 1.8
 */
public abstract class InputDecorator
{
    /**
     * Method called by {@link org.codehaus.jackson.JsonFactory} instance when
     * creating parser given an {@link InputStream}, when this decorator
     * has been registered.
     * 
     * @param ctxt IO context in use (provides access to declared encoding)
     * @param in Original input source
     * 
     * @return InputStream to use; either passed in argument, or something that
     *   calls it
     */
    public abstract InputStream decorate(IOContext ctxt, InputStream in)
        throws IOException;

    /**
     * Method called by {@link org.codehaus.jackson.JsonFactory} instance when
     * creating parser on given "raw" byte source.
     * Method can either construct a {@link InputStream} for reading; or return
     * null to indicate that no wrapping should occur.
     * 
     * @param ctxt IO context in use (provides access to declared encoding)
     * @param src Input buffer that contains contents to parse
     * 
     * @return Either {@link InputStream} to use as input source; or null to indicate
     *   that contents are to be processed as is by caller
     */
    public abstract InputStream decorate(IOContext ctxt, byte[] src, int offset, int length)
        throws IOException;
    
    /**
     * Method called by {@link org.codehaus.jackson.JsonFactory} instance when
     * creating parser given an {@link Reader}, when this decorator
     * has been registered.
     * 
     * @param ctxt IO context in use (provides access to declared encoding)
     * @param src Original input source
     * 
     * @return Reader to use; either passed in argument, or something that
     *   calls it (for example, a {@link FilterReader}
     */
    public abstract Reader decorate(IOContext ctxt, Reader src) throws IOException;
}
