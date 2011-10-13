package org.codehaus.jackson.map;

import java.util.*;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
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
    
    static class CStyleStrategy extends PropertyNamingStrategy
    {
        @Override
        public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName)
        {
            return convert(defaultName);
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName)
        {
            return convert(defaultName);
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName)
        {
            return convert(defaultName);
        }

        private String convert(String input)
        {
            // easy: replace capital letters with underscore, lower-cases equivalent
            StringBuilder result = new StringBuilder();
            for (int i = 0, len = input.length(); i < len; ++i) {
                char c = input.charAt(i);
                if (Character.isUpperCase(c)) {
                    result.append('_');
                    c = Character.toLowerCase(c);
                }
                result.append(c);
            }
            return result.toString();
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

    @JsonPropertyOrder({"first_name", "last_name"})
    static class PersonBean {
        public String firstName;
        public String lastName;
        public int age;

        public PersonBean() { this(null, null, 0); }
        public PersonBean(String f, String l, int a)
        {
            firstName = f;
            lastName = l;
            age = a;
        }
    }

    static class Value {
        public int intValue;
        
        public Value() { this(0); }
        public Value(int v) { intValue = v; }
    }

    static class SetterlessWithValue
    {
        protected ArrayList<Value> values = new ArrayList<Value>();

        public List<Value> getValueList() { return values; }

        public SetterlessWithValue add(int v) {
            values.add(new Value(v));
            return this;
        }
    }

    // [JACKSON-687]
    static class LcStrategy extends PropertyNamingStrategy.PropertyNamingStrategyBase
    {
        @Override
        public String translate(String propertyName) {
            return propertyName.toLowerCase();
        }
    }
    
    static class RenamedCollectionBean
    {
        @JsonDeserialize
        private List<String> THEvalues = Collections.emptyList();
        
        // intentionally odd name, to be renamed by naming strategy
        public List<String> getTheVALUEs() { return THEvalues; }
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

    public void testCStyleNaming() throws Exception
    {
        // First serialize
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new CStyleStrategy());
        String json = mapper.writeValueAsString(new PersonBean("Joe", "Sixpack", 42));
        assertEquals("{\"first_name\":\"Joe\",\"last_name\":\"Sixpack\",\"age\":42}", json);
        
        // then deserialize
        PersonBean result = mapper.readValue(json, PersonBean.class);
        assertEquals("Joe", result.firstName);
        assertEquals("Sixpack", result.lastName);
        assertEquals(42, result.age);
    }

    public void testWithGetterAsSetter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new CStyleStrategy());
        SetterlessWithValue input = new SetterlessWithValue().add(3);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"value_list\":[{\"int_value\":3}]}", json);

        SetterlessWithValue result = mapper.readValue(json, SetterlessWithValue.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.size());
        assertEquals(3, result.values.get(0).intValue);
    }

    // For [JACKSON-687]
    public void testJson() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new LcStrategy());
//        mapper.disable(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS);
        RenamedCollectionBean foo = mapper.readValue("{\"thevalues\":[\"a\"]}", RenamedCollectionBean.class);
        assertNotNull(foo.getTheVALUEs());
        assertEquals(1, foo.getTheVALUEs().size());
        assertEquals("a", foo.getTheVALUEs().get(0));
    }
}
