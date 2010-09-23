package org.codehaus.jackson.map.ser;

import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import org.codehaus.jackson.map.*;

public class TestCoreXMLTypes
    extends BaseMapTest
{
    public void testQName() throws Exception
    {
        QName qn = new QName("http://abc", "tag", "prefix");
        assertEquals(quote(qn.toString()), serializeAsString(qn));
    }

    public void testXMLGregorianCalendar() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar cal = dtf.newXMLGregorianCalendar
            (1974, 10, 10, 18, 15, 17, 123, 0);
        /* Due to [JACKSON-308], 1.6 will use configurable Date serialization;
         * and it defaults to using timestamp. So let's try couple of combinations.
         */
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(String.valueOf(cal.toGregorianCalendar().getTimeInMillis()),
                mapper.writeValueAsString(cal));
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
    
    public void testDuration() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        // arbitrary value
        Duration dur = dtf.newDurationDayTime(false, 15, 19, 58, 1);
        assertEquals(quote(dur.toString()), serializeAsString(dur));
    }
}
