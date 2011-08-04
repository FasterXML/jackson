package org.codehaus.jackson.jaxb;

import java.util.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Unit tests to verify handling of @XmlElementWrapper annotation.
 * 
 * @since 1.7
 */
public class TestElementWrapper extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    // Beans for [JACKSON-436]
    static class Person {
        @XmlElementWrapper
        @XmlElement(type=Phone.class)
        public Collection<IPhone> phones;
    }

    interface IPhone {
        public String getNumber();
    }

    static class Phone implements IPhone
    {
        private String number;

        public Phone() { }
        
        public Phone(String number) { this.number = number; }
        @Override
        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // [JACKSON-436]
    public void testWrapperWithCollection() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        Collection<IPhone> phones = new HashSet<IPhone>();
        phones.add(new Phone("555-6666"));
        Person p = new Person();
        p.phones = phones;

        String json = mapper.writeValueAsString(p);
//        System.out.println("JSON == "+json);

        Person result = mapper.readValue(json, Person.class);
        assertNotNull(result.phones);
        assertEquals(1, result.phones.size());
        assertEquals("555-6666", result.phones.iterator().next().getNumber());
    }
}
