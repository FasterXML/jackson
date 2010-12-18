package org.codehaus.jackson.xml;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.xml.annotate.JacksonXmlProperty;

public class TestDeserialization extends main.BaseTest
{
    static class AttributeBean
    {
        @JacksonXmlProperty(isAttribute=true, localName="attr")
        public String text = "?";
    }

    static class ListBean
    {
        public final List<Integer> values = new ArrayList<Integer>();
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * Unit test to ensure that we can succesfully also roundtrip
     * example Bean used in Jackson tutorial
     */
    public void testRoundTripWithJacksonExample() throws Exception
    {
        FiveMinuteUser user = new FiveMinuteUser("Joe", "Sixpack",
                true, FiveMinuteUser.Gender.MALE, new byte[] { 1, 2, 3 , 4, 5 });
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(user);
System.out.println("DEBUG: xml == "+xml);        
        FiveMinuteUser result = mapper.readValue(xml, FiveMinuteUser.class);
        assertEquals(user, result);
    }

    public void testFromAttribute() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        AttributeBean bean = mapper.readValue("<AttributeBean attr=\"abc\"></AttributeBean>", AttributeBean.class);
        assertNotNull(bean);
        assertEquals("abc", bean.text);
    }

    public void testListBean() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        ListBean bean = mapper.readValue(
                "<ListBean><values><values>1</values><values>2</values><values>3</values></values></ListBean>",
                ListBean.class);
        assertNotNull(bean);
        assertNotNull(bean.values);
        assertEquals(3, bean.values.size());
        assertEquals(Integer.valueOf(1), bean.values.get(0));
        assertEquals(Integer.valueOf(2), bean.values.get(1));
        assertEquals(Integer.valueOf(3), bean.values.get(2));
    }
}
