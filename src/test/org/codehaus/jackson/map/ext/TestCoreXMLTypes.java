package org.codehaus.jackson.map.ext;

import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ext.CoreXMLDeserializers;

public class TestCoreXMLTypes
    extends BaseMapTest
{
    // First things first: must be able to load the deserializers...
    public void testLoading()
    {
        new CoreXMLDeserializers.DurationDeserializer();
        new CoreXMLDeserializers.GregorianCalendarDeserializer();
        new CoreXMLDeserializers.QNameDeserializer();
    }

    public void testQName() throws Exception
    {
        QName qn = new QName("http://abc", "tag", "prefix");
        String qstr = qn.toString();
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("Should deserialize to equal QName (exp serialization: '"+qstr+"')",
                     qn, readAndMapFromString(mapper, qstr, QName.class));
    }

    public void testCalendar() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar cal = dtf.newXMLGregorianCalendar
            (1974, 10, 10, 18, 15, 17, 123, 0);
        String exp = cal.toXMLFormat();
        assertEquals("Should deserializer to equal XMLGregorianCalendar ('"+exp+"')", cal, readAndMapFromString(new ObjectMapper(), exp, XMLGregorianCalendar.class));
    }

    public void testDuration() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        // arbitrary value, like... say, 27d5h15m59s
        Duration dur = dtf.newDurationDayTime(true, 27, 5, 15, 59);
        String exp = dur.toString();
        assertEquals("Should deserializer to equal XMLGregorianCalendar ('"+exp+"')", dur, readAndMapFromString(new ObjectMapper(), exp, Duration.class));
    }
}
