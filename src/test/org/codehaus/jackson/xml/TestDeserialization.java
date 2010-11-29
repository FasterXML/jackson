package org.codehaus.jackson.xml;

import org.codehaus.jackson.map.ObjectMapper;

public class TestDeserialization extends main.BaseTest
{
    static class StringBean
    {
        public String text = "foobar";
    }

    static class StringBean2
    {
        public String text = "foobar";
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
        //System.out.println("XML == "+xml);
        if (xml.indexOf("<StringBean") != 0) {
            fail("Expected root name of 'StringBean'; but XML document is ["+xml+"]");
        }
    }
    
    /**
     * Unit test to ensure that we can succesfully also roundtrip
     * example Bean used in Jackson tutorial
     */
    public void testRoundTripWithJacksonExample() throws Exception
    {
        FiveMinuteUser user = new FiveMinuteUser("Joe", "Sixpack",
                true, FiveMinuteUser.Gender.MALE, new byte[] { 1, 2, 3 , 4, 5 });
        XmlFactory xf = new XmlFactory();
        ObjectMapper mapper = new ObjectMapper(xf);
        String xml = mapper.writeValueAsString(user);
        FiveMinuteUser result = mapper.readValue(xml, FiveMinuteUser.class);
        assertEquals(user, result);
    }
}
