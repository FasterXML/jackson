package org.codehaus.jackson.map;

import java.io.*;

public class BrokenStringReader
    extends FilterReader
{
    final String _message;

    public BrokenStringReader(String content, String msg)
    {
        super(new StringReader(content));
        _message = msg;
    }

    @Override
    public int read(char[] cbuf, int off, int len)
        throws IOException
    {
        int i = super.read(cbuf, off, len);
        if (i < 0) {
            throw new IOException(_message);
        }
        return i;
    }
}
