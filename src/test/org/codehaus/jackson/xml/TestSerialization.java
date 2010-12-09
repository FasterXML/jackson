package org.codehaus.jackson.xml;

import java.io.*;
import java.util.*;

//import javax.xml.bind.JAXB;

import org.codehaus.jackson.xml.annotate.JacksonXmlProperty;

public class TestSerialization extends XmlTestBase
{
    static class StringBean
    {
        public String text;

        public StringBean() { this("foobar"); }
        public StringBean(String s) { text = s; }
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

    static class StringListBean
    {
        public List<StringBean> strings;
        
        public StringListBean(String... texts)
        {
            strings = new ArrayList<StringBean>();
            for (String text : texts) {
                strings.add(new StringBean(text));
            }
        }
    }
    
    static class NsElemBean
    {
        @JacksonXmlProperty(namespace="http://foo")
        public String text = "blah";
    }

    static class NsAttrBean
    {
        @JacksonXmlProperty(namespace="http://foo", isAttribute=true)
        public String attr = "3";
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

    public void testSimpleNsElem() throws IOException
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new NsElemBean());
        xml = removeSjsxpNamespace(xml);
        // here we assume woodstox automatic prefixes, not very robust but:
        assertEquals("<NsElemBean><wstxns1:text xmlns:wstxns1=\"http://foo\">blah</wstxns1:text></NsElemBean>", xml);
    }

    public void testSimpleNsAttr() throws IOException
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new NsAttrBean());
        xml = removeSjsxpNamespace(xml);
        // here we assume woodstox automatic prefixes, not very robust but:
        assertEquals("<NsAttrBean xmlns:wstxns1=\"http://foo\" wstxns1:attr=\"3\"/>", xml);
    }
    
    public void testSimpleList() throws IOException
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new ListBean(1, 2, 3));
        xml = removeSjsxpNamespace(xml);
        // 06-Dec-2010, tatu: Not completely ok; should default to not using wrapper...
        assertEquals("<ListBean><values><values>1</values><values>2</values><values>3</values></values></ListBean>", xml);
    }

    public void testStringList() throws IOException
    {
        XmlMapper mapper = new XmlMapper();
        StringListBean list = new StringListBean("a", "b", "c");
        String xml = mapper.writeValueAsString(list);
        xml = removeSjsxpNamespace(xml);
        // 06-Dec-2010, tatu: Not completely ok; should default to not using wrapper...
        System.out.println("xml == "+xml);
        assertEquals("<StringListBean><string>"
                +"<string><text>a</text></string>"
                +"<string><text>b</text></string>"
                +"<string><text>c</text></string>"
                +"</string></StringListBean>", xml);
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
