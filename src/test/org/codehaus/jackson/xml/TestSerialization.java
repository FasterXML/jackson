package org.codehaus.jackson.xml;

//import javax.xml.bind.annotation.XmlAttribute;

//import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import org.codehaus.jackson.xml.annotate.JacksonXmlProperty;

public class TestSerialization extends BaseXmlTest
{
    static class StringBean
    {
        public String text = "foobar";
    }

    static class StringBean2
    {
        public String text = "foobar";
    }

    static class AttributeBean
    {
        @JacksonXmlProperty(isAttribute=true, localName="attr")
        public String text = "something";
    }

    static class AttrAndElem
    {
        public String elem = "whatever";
        
        @JacksonXmlProperty(isAttribute=true, localName="id")
        public int attr = 42;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    /**
     * Unit test to verify that root name is properly set
     */
    public void testRootName() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new StringBean());
        
        /* Hmmh. Looks like JDK Stax adds bogus ns declaration. As such,
         * let's just check that name starts ok...
         */
        if (xml.indexOf("<StringBean") != 0) {
            fail("Expected root name of 'StringBean'; but XML document is ["+xml+"]");
        }
    }
    
    public void testSimpleAttribute() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new AttributeBean());
        xml = removeSjsxpNamespace(xml);
        assertEquals("<AttributeBean attr=\"something\"></AttributeBean>", xml);
    }

    public void testSimpleAttrAndElem() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new AttrAndElem());
        xml = removeSjsxpNamespace(xml);
        assertEquals("<AttrAndElem id=\"42\"><elem>whatever</elem></AttrAndElem>", xml);
    }
}
