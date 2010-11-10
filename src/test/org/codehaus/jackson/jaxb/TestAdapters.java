package org.codehaus.jackson.jaxb;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
}
