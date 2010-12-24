package org.codehaus.jackson.xml;

public abstract class XmlTestBase
    extends org.codehaus.jackson.map.BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    protected static class StringBean
    {
        public String text;

        public StringBean() { this("foobar"); }
        public StringBean(String s) { text = s; }
    }

    protected XmlTestBase() {
        super();
    }
    
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
