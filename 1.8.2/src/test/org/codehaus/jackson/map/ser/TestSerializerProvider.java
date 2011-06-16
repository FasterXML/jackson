package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.*;

public class TestSerializerProvider
    extends org.codehaus.jackson.map.BaseMapTest
{
    static class MyBean {
        public int getX() { return 3; }
    }

    public void testFindExplicit() throws JsonMappingException
    {
        ObjectMapper mapper = new ObjectMapper();
        SerializationConfig config = mapper.getSerializationConfig();
        SerializerFactory f = new BeanSerializerFactory(null);
        StdSerializerProvider prov = new StdSerializerProvider().createInstance(config, f);

        // Should have working default key and null key serializers
        assertNotNull(prov.findKeySerializer(null, null));
        assertNotNull(prov.getNullKeySerializer());
        // as well as 'unknown type' one (throws exception)
        assertNotNull(prov.getUnknownTypeSerializer(getClass()));
        
        assertTrue(prov.hasSerializerFor(config, String.class, f));
        // call twice to verify it'll be cached (second code path)
        assertTrue(prov.hasSerializerFor(config, String.class, f));

        assertTrue(prov.hasSerializerFor(config, MyBean.class, f));
        assertTrue(prov.hasSerializerFor(config, MyBean.class, f));
    }
}
