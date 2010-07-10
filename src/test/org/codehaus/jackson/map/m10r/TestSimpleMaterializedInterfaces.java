package org.codehaus.jackson.map.m10r;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;

public class TestSimpleMaterializedInterfaces
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes, enums
    /**********************************************************
     */

    public interface Bean {
        public int getX();
        public String getA();
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * First test verifies that bean builder works as expected
     */
    public void testBeanBuilder() throws Exception
    {
        Class<?> impl = new BeanBuilder().implement(Bean.class).load("test.BeanImpl");
        assertNotNull(impl);
        assertTrue(Bean.class.isAssignableFrom(impl));
        // also, let's instantiate to make sure:
        Object ob = impl.newInstance();
        // and just for good measure do actual cast
        Bean bean = (Bean) ob;
        // call something to ensure generation worked...
        assertNull(bean.getA());
    }

    /**
     * And the a test to verify it via registration
     */
    public void testSimpleInteface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().setAbstractTypeResolver(new AbstractTypeMaterializer());
        Bean bean = mapper.readValue("{\"a\":\"value\",\"b\":123", Bean.class);
        assertNotNull(bean);
        assertEquals("value", bean.getA());
        assertEquals(123, bean.getX());
    }
}
