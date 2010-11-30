package org.codehaus.jackson.xml;

public class TestDeserialization extends main.BaseTest
{
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
        FiveMinuteUser result = mapper.readValue(xml, FiveMinuteUser.class);
        assertEquals(user, result);
    }
}
