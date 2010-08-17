package org.codehaus.jackson.jaxb;

import java.util.*;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * @since 1.6
 */
public class TestXmlAnyElementWithElementRef
    extends BaseJaxbTest
{
    static class Bean {
        @XmlAnyElement(lax=true)
        @XmlElementRefs({
            @XmlElementRef(name="a", type=Name.class),
            @XmlElementRef(name="b", type=Count.class)
        })
        public List<Object> others;

        public Bean() { }
        public Bean(Object ob) {
            others = new ArrayList<Object>();
            others.add(ob);
        }
    }

    static class Name {
        public String name;
    }

    static class Count {
        public int count;
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [JACKSON-254]: verify that things do work
    public void testXmlAnyElementWithElementRef() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();

        Count count = new Count();
        count.count = 8;
        Bean value = new Bean(count);

        // typed handling should be triggered by annotation on property, so
        String json = mapper.writeValueAsString(value);
        assertEquals("{\"others\":[{\"b\":{\"count\":8}}]}", json);

        Bean result = mapper.readValue(json, Bean.class);
        assertNotNull(result);
        assertEquals(1, result.others.size());
        Object resultOb = result.others.get(0);
        assertSame(Count.class, resultOb.getClass());
        assertEquals(8, ((Count) resultOb).count);
    }

}
