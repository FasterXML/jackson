package org.codehaus.jackson.xml;

//import javax.xml.bind.annotation.XmlAttribute;

//import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.*;
import java.util.*;

//import javax.xml.bind.JAXB;

import org.codehaus.jackson.xml.annotate.JacksonXmlProperty;

public class TestSerialization extends XmlTestBase
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

    static class ListBean
    {
        public final List<Integer> values = new ArrayList<Integer>();

        public ListBean() { }
        public ListBean(int... ints) {
            for (int i : ints) {
                values.add(Integer.valueOf(i));
            }
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    /**
     * Unit test to verify that root name is properly set
     */
    public void testRootName() throws IOException
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
    
    public void testSimpleAttribute() throws IOException
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new AttributeBean());
        xml = removeSjsxpNamespace(xml);
        assertEquals("<AttributeBean attr=\"something\"/>", xml);
    }

    public void testSimpleAttrAndElem() throws IOException
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new AttrAndElem());
        xml = removeSjsxpNamespace(xml);
        assertEquals("<AttrAndElem id=\"42\"><elem>whatever</elem></AttrAndElem>", xml);
    }

    public void testSimpleList() throws IOException
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new ListBean(1, 2, 3));
        xml = removeSjsxpNamespace(xml);
        // 06-Dec-2010, tatu: Not completely ok; should default to not using wrapper...
        assertEquals("<ListBean><values><values>1</values><values>2</values><values>3</values></values></ListBean>", xml);
        
    }

    /*
      // manual 'test':
    public void testJAXB() throws Exception
    {
        StringWriter sw = new StringWriter();
        JAXB.marshal(new ListBean(1, 2, 3), sw);
        System.out.println("JAXB -> "+sw);
    }
    */
}
