package org.codehaus.jackson.xml;

import junit.framework.TestCase;

public abstract class BaseXmlTest
    extends TestCase
{
    /**
     * Helper method that tries to remove unnecessary namespace
     * declaration that default JDK XML parser (SJSXP) seems fit
     * to add.
     */
    protected static String removeSjsxpNamespace(String xml)
    {
        final String match = " xmlns=\"\"";
        int ix = xml.indexOf(match);
        if (ix > 0) {
            xml = xml.substring(0, ix) + xml.substring(ix+match.length());
        }
        return xml;
    }
}
