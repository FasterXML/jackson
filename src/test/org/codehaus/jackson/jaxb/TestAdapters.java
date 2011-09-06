package org.codehaus.jackson.jaxb;

import java.util.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Unit tests for checking that JAXB type adapters work (to some
 * degree, anyway).
 * Related to issues [JACKSON-288], [JACKSON-411]
 * 
 * @author tsaloranta
 *
 * @since 1.6.3
 */
public class TestAdapters extends BaseJaxbTest
{
    public static class SillyAdapter extends XmlAdapter<String, Date>
    {
        public SillyAdapter() { }

        @Override
        public Date unmarshal(String date) throws Exception {
            return new Date(29L);
        }

        @Override
        public String marshal(Date date) throws Exception {
            return "XXX";
        }
    }

    static class Bean
    {
        @XmlJavaTypeAdapter(SillyAdapter.class)
        public Date value;

        public Bean() { }
        public Bean(long l) { value = new Date(l); }
    }

    // For [JACKSON-288]
    
    static class Bean288 {
        public List<Person> persons;

        public Bean288() { }
        public Bean288(String str) {
            persons = new ArrayList<Person>();
            persons.add(new Person(str));
        }
    }

    static class Person
    {
        public String name;
        
        @XmlElement(required = true, type = String.class)
        @XmlJavaTypeAdapter(DateAdapter.class)
        protected Calendar date;

        public Person() { }
        public Person(String n) {
            name = n;
            date = Calendar.getInstance();
            date.setTime(new Date(0L));
        }
    }

    public static class DateAdapter
        extends XmlAdapter<String, Calendar>
    {
        public DateAdapter() { }
        
        @Override
        public Calendar unmarshal(String value) {
            return (javax.xml.bind.DatatypeConverter.parseDateTime(value));
        }
    
        @Override
        public String marshal(Calendar value) {
            if (value == null) {
                return null;
            }
            return (javax.xml.bind.DatatypeConverter.printDateTime(value));
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimpleAdapterSerialization() throws Exception
    {
        Bean bean = new Bean(123L);
        assertEquals("{\"value\":\"XXX\"}", getJaxbMapper().writeValueAsString(bean));
    }

    public void testSimpleAdapterDeserialization() throws Exception
    {
        Bean bean = getJaxbMapper().readValue("{\"value\":\"abc\"}", Bean.class);
        assertNotNull(bean.value);
        assertEquals(29L, bean.value.getTime());
    }

    // [JACKSON-288]
    public void testDateAdapter() throws Exception
    {
        Bean288 input = new Bean288("test");
        ObjectMapper mapper = getJaxbMapper();
        String json = mapper.writeValueAsString(input);
        Bean288 output = mapper.readValue(json, Bean288.class);
        assertNotNull(output);
    }
}
