package org.codehaus.jackson.map.ser;
import java.io.*;

import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestCoreXMLTypes
    extends BaseMapTest
{
    public void testQName() throws Exception
    {
        QName qn = new QName("http://abc", "tag", "prefix");
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(quote(qn.toString()), serializeAsString(qn));
    }

    public void testCalendar() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar cal = dtf.newXMLGregorianCalendar
            (1974, 10, 10, 18, 15, 17, 123, 0);
        assertEquals(quote(cal.toXMLFormat()), serializeAsString(cal));
    }

    public void testDuration() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        // arbitrary value
        Duration dur = dtf.newDurationDayTime(false, 15, 19, 58, 1);
        assertEquals(quote(dur.toString()), serializeAsString(dur));
    }
}
