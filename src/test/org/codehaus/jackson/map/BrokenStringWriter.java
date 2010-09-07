package org.codehaus.jackson.map;

import java.io.*;

public class BrokenStringWriter
    extends FilterWriter
{
    final String _message;

    public BrokenStringWriter(String msg) {
        super(new StringWriter());
        _message = msg;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
        throw new IOException(_message);
    }
    
    @Override
    public void write(int c) throws IOException
    {
        throw new IOException(_message);
    }
    
    @Override
    public void write(String str, int off, int len)  throws IOException
    {
        throw new IOException(_message);
    }
}
