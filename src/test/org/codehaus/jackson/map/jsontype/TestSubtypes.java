package org.codehaus.jackson.map.jsontype;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.ObjectMapper;

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
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

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
   }

    public void testDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(SubB.class);
        mapper.registerSubtypes(SubC.class);
        mapper.registerSubtypes(new NamedType(SubD.class, "TypeD"));

        SuperType bean = mapper.readValue("{\"@type\":\"TypeB\", \"b\":13}", SuperType.class);
        assertSame(SubB.class, bean.getClass());
        assertEquals(13, ((SubB) bean).b);

        // default name should be unqualified class name
        
        bean = mapper.readValue("{\"@type\":\"SubC\", \"c\":1}", SuperType.class);
        assertSame(SubC.class, bean.getClass());
        assertEquals(1, ((SubC) bean).c);

        // but we can also explicitly register name too
        bean = mapper.readValue("{\"@type\":\"TypeD\", \"d\":-4}", SuperType.class);
        assertSame(SubD.class, bean.getClass());
        assertEquals(-4, ((SubD) bean).d);
    }
}
