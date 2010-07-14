package org.codehaus.jackson.map.m10r;

import java.util.*;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

public class TestGenericTypes
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes, enums
    /**********************************************************
     */

    public interface ListBean {
        public List<LeafBean> getLeaves();
    }

    public class LeafBean {
        public String value;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * Test simple leaf-level bean with 2 implied _beanProperties
     */
    public void testSimpleInteface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().setAbstractTypeResolver(new AbstractTypeMaterializer());
        ListBean bean = mapper.readValue("{\"leaves\":[{\"value\":\"foo\"}] }", ListBean.class);
        assertNotNull(bean);
        List<LeafBean> leaves = bean.getLeaves();
        assertNotNull(leaves);
        assertEquals(1, leaves.size());
        assertSame(LeafBean.class, leaves.get(0).getClass());
        assertEquals("foo", leaves.get(0).value);
    }
    
}
