package org.codehaus.jackson.map.m10r;

import static org.junit.Assert.assertArrayEquals;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

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

    // then invalid one; conflicting setter/getter types
    public interface InvalidBean {
        public int getX();
        public void setX(String value);
    }

    public interface ArrayBean {
        public int[] getValues();
        public String[] getWords();
        public void setWords(String[] words);
    }
    
    /*
    /**********************************************************
    /* Unit tests, low level
    /**********************************************************
     */

    /**
     * First test verifies that bean builder works as expected
     */
    public void testLowLevelMaterializer() throws Exception
    {
        AbstractTypeMaterializer mat = new AbstractTypeMaterializer();
        Class<?> impl = mat.materializeClass(Bean.class);
        assertNotNull(impl);
        assertTrue(Bean.class.isAssignableFrom(impl));
        // also, let's instantiate to make sure:
        Object ob = impl.newInstance();
        // and just for good measure do actual cast
        Bean bean = (Bean) ob;
        // call something to ensure generation worked...
        assertNull(bean.getA());
    }

    public void testLowLevelMaterializerFail() throws Exception
    {
        AbstractTypeMaterializer mat = new AbstractTypeMaterializer();
        try {
            mat.materializeClass(InvalidBean.class);
            fail("Expected exception for incompatible property types");
        } catch (IllegalArgumentException e) {
            verifyException(e, "incompatible types");
        }
    }

    /*
    /**********************************************************
    /* Unit tests, higher level
    /**********************************************************
     */

    /**
     * And the a test to verify it via registration
     */
    public void testSimpleInteface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().setAbstractTypeResolver(new AbstractTypeMaterializer());
        Bean bean = mapper.readValue("{\"a\":\"value\",\"x\":123 }", Bean.class);
        assertNotNull(bean);
        assertEquals("value", bean.getA());
        assertEquals(123, bean.getX());
    }

    public void testArrayInteface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().setAbstractTypeResolver(new AbstractTypeMaterializer());
        ArrayBean bean = mapper.readValue("{\"values\":[1,2,3], \"words\": [ \"cool\", \"beans\" ] }",
                ArrayBean.class);
        assertNotNull(bean);
        assertArrayEquals(new int[] { 1, 2, 3} , bean.getValues());
        assertArrayEquals(new String[] { "cool", "beans" } , bean.getWords());
    }
}
