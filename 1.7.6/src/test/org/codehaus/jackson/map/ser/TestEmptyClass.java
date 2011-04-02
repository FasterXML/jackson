package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.*;

public class TestEmptyClass
    extends BaseMapTest
{
    static class Empty { }

    @JsonSerialize
    static class EmptyWithAnno { }

    /**
     * Test to check that [JACKSON-201] works if there is a recognized
     * annotation (which indicates type is serializable)
     */
    public void testEmptyWithAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // First: without annotations, should complain
        try {
            serializeAsString(mapper, new Empty());
        } catch (JsonMappingException e) {
            verifyException(e, "No serializer found for class");
        }

        // But not if there is a recognized annotation
        assertEquals("{}", serializeAsString(mapper, new EmptyWithAnno()));

        // Including class annotation through mix-ins
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(Empty.class, EmptyWithAnno.class);
        assertEquals("{}", serializeAsString(mapper, new Empty()));
    }

    /**
     * Alternative it is possible to use a feature to allow
     * serializing empty classes, too
     */
    public void testEmptyWithFeature() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // should be enabled by default
        assertTrue(mapper.getSerializationConfig().isEnabled(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS));
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        assertEquals("{}", serializeAsString(mapper, new Empty()));
    }
}
