package org.codehaus.jackson.map.jsontype;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.module.SimpleModule;

public class TestSubtypes extends org.codehaus.jackson.map.BaseMapTest
{
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    static abstract class SuperType {
    }

    @JsonTypeName("TypeB")
    static class SubB extends SuperType {
        public int b = 1;
    }

    static class SubC extends SuperType {
        public int c = 2;
    }

    static class SubD extends SuperType {
        public int d;
    }

    // "Empty" bean, to test [JACKSON-366]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    static abstract class BaseBean { }
    
    static class EmptyBean extends BaseBean { }

    static class EmptyNonFinal { }

    // Verify combinations with [JACKSON-510]

    static class PropertyBean
    {
        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
        public SuperType value;
        
        public PropertyBean() { this(null); }
        public PropertyBean(SuperType v) { value = v; }
    }

    // And then [JACKSON-614]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY,
            property="#type",
            defaultImpl=DefaultImpl.class)
    static abstract class SuperTypeWithDefault { }

    static class DefaultImpl extends SuperTypeWithDefault {
        public int a;
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="#type")
    static abstract class SuperTypeWithoutDefault { }

    static class DefaultImpl505 extends SuperTypeWithoutDefault {
        public int a;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // JACKSON-510
    public void testPropertyWithSubtypes() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // must register subtypes
        mapper.registerSubtypes(SubB.class, SubC.class, SubD.class);
        String json = mapper.writeValueAsString(new PropertyBean(new SubC()));
        PropertyBean result = mapper.readValue(json, PropertyBean.class);
        assertSame(SubC.class, result.value.getClass());
    }

    public void testSerialization() throws Exception
    {
        // serialization can detect type name ok without anything extra:
        SubB bean = new SubB();
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("{\"@type\":\"TypeB\",\"b\":1}", mapper.writeValueAsString(bean));

        // but we can override type name here too
        mapper = new ObjectMapper();
        mapper.registerSubtypes(new NamedType(SubB.class, "typeB"));
        assertEquals("{\"@type\":\"typeB\",\"b\":1}", mapper.writeValueAsString(bean));

        // and default name ought to be simple class name; with context
        assertEquals("{\"@type\":\"TestSubtypes$SubD\",\"d\":0}", mapper.writeValueAsString(new SubD()));  
    }

    public void testDeserializationNonNamed() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(SubC.class);

        // default name should be unqualified class name
        SuperType bean = mapper.readValue("{\"@type\":\"TestSubtypes$SubC\", \"c\":1}", SuperType.class);
        assertSame(SubC.class, bean.getClass());
        assertEquals(1, ((SubC) bean).c);
    }

    public void testDeserializatioNamed() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(SubB.class);
        mapper.registerSubtypes(new NamedType(SubD.class, "TypeD"));

        SuperType bean = mapper.readValue("{\"@type\":\"TypeB\", \"b\":13}", SuperType.class);
        assertSame(SubB.class, bean.getClass());
        assertEquals(13, ((SubB) bean).b);

        // but we can also explicitly register name too
        bean = mapper.readValue("{\"@type\":\"TypeD\", \"d\":-4}", SuperType.class);
        assertSame(SubD.class, bean.getClass());
        assertEquals(-4, ((SubD) bean).d);
    }

    // Trying to reproduce [JACKSON-366]
    public void testEmptyBean() throws Exception
    {
        // First, with annotations
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, true);
        String json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"@type\":\"TestSubtypes$EmptyBean\"}", json);

        mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"@type\":\"TestSubtypes$EmptyBean\"}", json);

        // and then with defaults
        mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        json = mapper.writeValueAsString(new EmptyNonFinal());
        assertEquals("[\"org.codehaus.jackson.map.jsontype.TestSubtypes$EmptyNonFinal\",{}]", json);
    }

    public void testDefaultImpl() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // first, test with no type information
        SuperTypeWithDefault bean = mapper.readValue("{\"a\":13}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(13, ((DefaultImpl) bean).a);

        // and then with unmapped info
        bean = mapper.readValue("{\"a\":14,\"#type\":\"foobar\"}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(14, ((DefaultImpl) bean).a);

        bean = mapper.readValue("{\"#type\":\"foobar\",\"a\":15}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(15, ((DefaultImpl) bean).a);

        bean = mapper.readValue("{\"#type\":\"foobar\"}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(0, ((DefaultImpl) bean).a);
    }

    // [JACKSON-505]: ok to also default to mapping there might be for base type
    public void testDefaultImplViaModule() throws Exception
    {
        final String JSON = "{\"a\":123}";
        
        // first: without registration etc, epic fail:
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue(JSON, SuperTypeWithoutDefault.class);
            fail("Expected an exception");
        } catch (JsonMappingException e) {
            verifyException(e, "missing property");
        }

        // but then succeed when we register default impl
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addAbstractTypeMapping(SuperTypeWithoutDefault.class, DefaultImpl505.class);
        mapper.registerModule(module);
        SuperTypeWithoutDefault bean = mapper.readValue(JSON, SuperTypeWithoutDefault.class);
        assertNotNull(bean);
        assertEquals(DefaultImpl505.class, bean.getClass());
        assertEquals(123, ((DefaultImpl505) bean).a);

        bean = mapper.readValue("{\"#type\":\"foobar\"}", SuperTypeWithoutDefault.class);
        assertEquals(DefaultImpl505.class, bean.getClass());
        assertEquals(0, ((DefaultImpl505) bean).a);
    
    }
}

