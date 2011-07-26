package org.codehaus.jackson.jaxb;

import java.util.*;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Tests for handling of type-related JAXB annotations 
 */
public class TestJaxbTypes
    extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
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
        protected String b;

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

        public ListBean() { }
        public ListBean(AbstractBean ... beans) {
            this.beans = Arrays.asList(beans);
        }
        public ListBean(List<AbstractBean> beans) {
            this.beans = beans;
        }

        public List<AbstractBean> getBeans() { return beans; }
        public void setBeans(List<AbstractBean> b) { beans = b; }

        public int size() { return beans.size(); }
        public BeanImpl get(int index) { return (BeanImpl) beans.get(index); }
    }

    /* And then mix'n match, to try end-to-end
     */
    static class ComboBean
    {
        private AbstractBean bean;

        public ListBean beans;

        public ComboBean() { }
        public ComboBean(AbstractBean bean, ListBean beans)
        {
            this.bean = bean;
            this.beans = beans;
        }

        @XmlElement(type=BeanImpl.class)
        public AbstractBean getBean() { return bean; }
        public void setBean(AbstractBean bean) { this.bean = bean; }
    }

    /**
     * Unit test for [JACKSON-250]
     */
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY)
    @JsonTypeName("Name")
    @XmlType    
    static class P2
    {
    	String id;
    	public P2(String id) { this.id = id; }
    	public P2() { }

    	@XmlID
    	@XmlAttribute(name="id")
    	public String getId() { return id; }

    	public void setId(String id) { this.id = id; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
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

    public void testXmlElementListArrayDeser() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        ListBean[] listBeans = mapper.readValue
            ("[{\"beans\": [{\"a\":1,\"b\":\"a\"}, {\"a\":7,\"b\":\"b\" }]}]",
             ListBean[].class);
        assertNotNull(listBeans);
        assertEquals(1, listBeans.length);
        List<AbstractBean> beans = listBeans[0].beans;
        assertNotNull(beans);
        assertEquals(2, beans.size());
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

    public void testRoundTrip() throws Exception
    {
        ComboBean input = new ComboBean(new BeanImpl(3, "abc"),
                                        new ListBean(new BeanImpl(1, "a"),
                                                     new BeanImpl(2, "b"),
                                                     new BeanImpl(3, "c")));
        ObjectMapper mapper = getJaxbMapper();
        String str = mapper.writeValueAsString(input);

        ComboBean result = mapper.readValue(str, ComboBean.class);

        assertEquals(3, ((BeanImpl)result.bean).a);
        assertEquals("abc", ((BeanImpl)result.bean).b);

        assertEquals(3, result.beans.size());
        assertEquals(1, (result.beans.get(0)).a);
        assertEquals("a", (result.beans.get(0)).b);
        assertEquals(2, (result.beans.get(1)).a);
        assertEquals("b", (result.beans.get(1)).b);
        assertEquals(3, (result.beans.get(2)).a);
        assertEquals("c", (result.beans.get(2)).b);
    }

    public void testListWithDefaultTyping() throws Exception
    {
        Object input = new ListBean(new BeanImpl(1, "a"));
        ObjectMapper mapper = getJaxbMapper();
        mapper.enableDefaultTyping();
        String str = mapper.writeValueAsString(input);

        ListBean listBean = mapper.readValue(str, ListBean.class);
        assertNotNull(listBean);
        List<AbstractBean> beans = listBean.beans;
        assertNotNull(beans);
        assertEquals(1, beans.size());
        assertNotNull(beans.get(0));
        BeanImpl bean = (BeanImpl) beans.get(0);
        assertEquals(1, bean.a);
        assertEquals("a", bean.b);
    }

    public void testIssue250() throws Exception
    {
        ObjectMapper mapper = getJaxbAndJacksonMapper();
        P2 bean = new P2("myId");
        String str = mapper.writeValueAsString(bean);
        assertEquals("{\"@type\":\"Name\",\"id\":\"myId\"}", str);
    }
}
