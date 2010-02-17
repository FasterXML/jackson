package org.codehaus.jackson.jaxb;

import java.util.*;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.*;

/**
 * Tests for handling of type-related JAXB annotations 
 */
public class TestJaxbTypes
    extends BaseJaxbTest
{
    /*
    **************************************************************
    * Helper beans
    **************************************************************
    */

    static class AbstractWrapper {
        @XmlElement(type=BeanImpl.class)
        public AbstractBean wrapped;

        public AbstractWrapper() { this(null); }
        public AbstractWrapper(AbstractBean bean) { wrapped = bean; }
    }

    interface AbstractBean { }

    static class BeanImpl
        implements AbstractBean
    {
        public int a;
        private String b;

        public BeanImpl() { this(0, null); }
        public BeanImpl(int a, String b) {
            this.a = a;
            this.b = b;
        }

        public String getB() { return b; }
        public void setB(String b) { this.b = b; }
    }

    static class ListBean {
        /* Note: here we rely on implicit linking between the field
         * and accessors. 
         */
        @XmlElement(type=BeanImpl.class)
        private List<AbstractBean> beans;

        public List<AbstractBean> getBeans() { return beans; }
        public void setBeans(List<AbstractBean> b) { beans = b; }
    }
    
    /*
    **************************************************************
    * Unit tests
    **************************************************************
    */

    public void testXmlElementTypeDeser() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        AbstractWrapper wrapper = mapper.readValue("{\"wrapped\":{\"a\":13,\"b\":\"...\"}}", AbstractWrapper.class);
        assertNotNull(wrapper);
        BeanImpl bean = (BeanImpl) wrapper.wrapped;
        assertEquals(13, bean.a);
        assertEquals("...", bean.b);
    }

    public void testXmlElementTypeSer() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        AbstractWrapper wrapper = new AbstractWrapper(new BeanImpl(-3, "c"));
        assertEquals("{\"wrapped\":{\"a\":-3,\"b\":\"c\"}}",
                     mapper.writeValueAsString(wrapper));
    }

    public void testXmlElementListTypeDeser() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        ListBean listBean = mapper.readValue
            ("{\"beans\": [{\"a\":1,\"b\":\"a\"}, {\"a\":7,\"b\":\"b\" }]}",
             ListBean.class);
        assertNotNull(listBean);
        List<AbstractBean> beans = listBean.beans;
        assertNotNull(beans);
        assertEquals(2, beans.size());
        assertNotNull(beans.get(0));
        assertNotNull(beans.get(1));

        BeanImpl bean = (BeanImpl) beans.get(0);
        assertEquals(1, bean.a);
        assertEquals("a", bean.b);

        bean = (BeanImpl) beans.get(1);
        assertEquals(7, bean.a);
        assertEquals("b", bean.b);
    }

    public void testXmlElementListTypeSer() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        ListBean bean = new ListBean();
        List<AbstractBean> beans = new ArrayList<AbstractBean>();
        beans.add(new BeanImpl(1, "a"));
        beans.add(new BeanImpl(2, "b"));
        bean.beans = beans;
        
        assertEquals("{\"beans\":[{\"a\":1,\"b\":\"a\"},{\"a\":2,\"b\":\"b\"}]}",
                     mapper.writeValueAsString(bean));
    }
}
