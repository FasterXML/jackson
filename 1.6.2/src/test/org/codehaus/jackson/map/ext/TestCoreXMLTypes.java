package org.codehaus.jackson.map.ext;

import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ext.CoreXMLDeserializers;

/**
 * Core XML types (javax.xml) are considered "external" (or more precisely "optional")
 * since some Java(-like) platforms do not include them: specifically, Google AppEngine
 * and Android seem to skimp on their inclusion. As such, they are dynamically loaded
 * only as needed, and need bit special handling.
 * 
 * @author tatu
 */
public class TestCoreXMLTypes
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Serializer tests
    /**********************************************************
     */

    public void testQNameSer() throws Exception
    {
        QName qn = new QName("http://abc", "tag", "prefix");
        assertEquals(quote(qn.toString()), serializeAsString(qn));
    }

    public void testDurationSer() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        // arbitrary value
        Duration dur = dtf.newDurationDayTime(false, 15, 19, 58, 1);
        assertEquals(quote(dur.toString()), serializeAsString(dur));
    }

    public void testXMLGregorianCalendarSerAndDeser() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar cal = dtf.newXMLGregorianCalendar
            (1974, 10, 10, 18, 15, 17, 123, 0);
        /* Due to [JACKSON-308], 1.6 will use configurable Date serialization;
         * and it defaults to using timestamp. So let's try couple of combinations.
         */
        ObjectMapper mapper = new ObjectMapper();
        long timestamp = cal.toGregorianCalendar().getTimeInMillis();
        String numStr = String.valueOf(timestamp);
        assertEquals(numStr, mapper.writeValueAsString(cal));

        // [JACKSON-403] Needs to come back ok as well:
        XMLGregorianCalendar calOut = mapper.readValue(numStr, XMLGregorianCalendar.class);
        assertNotNull(calOut);
        assertEquals(timestamp, calOut.toGregorianCalendar().getTimeInMillis());
        
        // and then textual variant
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        // this is ALMOST same as default for XMLGregorianCalendar... just need to unify Z/+0000
        String exp = cal.toXMLFormat();
        String act = mapper.writeValueAsString(cal);
        act = act.substring(1, act.length() - 1); // remove quotes
        exp = removeZ(exp);
        act = removeZ(act);
        assertEquals(exp, act);
    }

    private String removeZ(String dateStr) {
        if (dateStr.endsWith("Z")) {
            return dateStr.substring(0, dateStr.length()-1);
        }
        if (dateStr.endsWith("+0000")) {
            return dateStr.substring(0, dateStr.length()-5);
        }
        return dateStr;
    }
    
    /*
    /**********************************************************
    /* Deserializer tests
    /**********************************************************
     */
    
    // First things first: must be able to load the deserializers...
    public void testDeserializerLoading()
    {
        new CoreXMLDeserializers.DurationDeserializer();
        new CoreXMLDeserializers.GregorianCalendarDeserializer();
        new CoreXMLDeserializers.QNameDeserializer();
    }

    public void testQNameDeser() throws Exception
    {
        QName qn = new QName("http://abc", "tag", "prefix");
        String qstr = qn.toString();
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("Should deserialize to equal QName (exp serialization: '"+qstr+"')",
                     qn, mapper.readValue(quote(qstr), QName.class));
    }

    public void testCalendarDeser() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar cal = dtf.newXMLGregorianCalendar
            (1974, 10, 10, 18, 15, 17, 123, 0);
        String exp = cal.toXMLFormat();
        assertEquals("Should deserialize to equal XMLGregorianCalendar ('"+exp+"')", cal,
                new ObjectMapper().readValue(quote(exp), XMLGregorianCalendar.class));
    }

    public void testDurationDeser() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        // arbitrary value, like... say, 27d5h15m59s
        Duration dur = dtf.newDurationDayTime(true, 27, 5, 15, 59);
        String exp = dur.toString();
        assertEquals("Should deserialize to equal Duration ('"+exp+"')", dur,
                new ObjectMapper().readValue(quote(exp), Duration.class));
    }
}
