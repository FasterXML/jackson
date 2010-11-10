package org.codehaus.jackson.xml;

import java.io.IOException;

import javax.xml.stream.*;

public class StaxUtil
{
    /**
     * Adapter method used when only IOExceptions are declared to be thrown, but
     * a {@link XMLStreamException} was caught.
     *<p>
     * Note: dummy type variable is used for convenience, to allow caller to claim
     * that this method returns result of any necessary type.
     */
    public static <T> T throwXmlAsIOException(XMLStreamException e) throws IOException
    {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof Error) throw (Error) t;
        if (t instanceof RuntimeException) throw (RuntimeException) t;
        throw new IOException(t);
    }
}
