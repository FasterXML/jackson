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

    public void write(char[] cbuf, int off, int len) throws IOException
    {
        throw new IOException(_message);
    }
    
    public void write(int c) throws IOException
    {
        throw new IOException(_message);
    }
    
    public void write(String str, int off, int len)  throws IOException
    {
        throw new IOException(_message);
    }
}
