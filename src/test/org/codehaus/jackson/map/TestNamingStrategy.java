package org.codehaus.jackson.map;

import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;

/**
 * Unit tests to verify functioning of {@link PropertyNamingStrategy} which
 * was added in Jackson 1.8, as per [JACKSON-178].
 * 
 * @since 1.8
 */
public class TestNamingStrategy extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    static class PrefixStrategy extends PropertyNamingStrategy
    {
        @Override
        public String nameForField(MapperConfig<?> config,
                AnnotatedField field, String defaultName)
        {
            return "Field-"+defaultName;
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config,
                AnnotatedMethod method, String defaultName)
        {
            return "Get-"+defaultName;
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config,
                AnnotatedMethod method, String defaultName)
        {
            return "Set-"+defaultName;
        }
    }

    static class GetterBean {
        public int getKey() { return 123; }
    }

    static class SetterBean {
        protected int value;
        
        public void setKey(int v) {
            value = v;
        }
    }

    static class FieldBean {
        public int key;

        public FieldBean() { this(0); }
        public FieldBean(int v) { key = v; }
    }
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */
    
    public void testSimpleGetters() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new PrefixStrategy());
        assertEquals("{\"Get-key\":123}", mapper.writeValueAsString(new GetterBean()));
    }

    public void testSimpleSetters() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new PrefixStrategy());
        SetterBean bean = mapper.readValue("{\"Set-key\":13}", SetterBean.class);
        assertEquals(13, bean.value);
    }

    public void testSimpleFields() throws Exception
    {
        // First serialize
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new PrefixStrategy());
        String json = mapper.writeValueAsString(new FieldBean(999));
        assertEquals("{\"Field-key\":999}", json);

        // then deserialize
        FieldBean result = mapper.readValue(json, FieldBean.class);
        assertEquals(999, result.key);
    }
}
