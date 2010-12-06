package org.codehaus.jackson.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

public class TestSerialization extends main.BaseTest
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
        @XmlAttribute(name="attr")
        public String text = "something";
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
    
    public void testAttributeVsElem() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        String xml = mapper.writeValueAsString(new AttributeBean());
        assertEquals("<AttributeBean attr=\"something\">", xml);
    }
}
