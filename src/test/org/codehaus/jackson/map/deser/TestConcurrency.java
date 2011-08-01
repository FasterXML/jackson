package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.*;

/**
 * Testing for [JACKSON-237] (NPE due to race condition)
 * 
 * @since 1.5
 */
public class TestConcurrency extends BaseMapTest
{
    /*
    /**********************************************
    /* Helper beans
    /**********************************************
     */

    @JsonDeserialize(using=BeanDeserializer.class)
    static class Bean
    {
        public int value = 42;
    }

    /*
    /**********************************************
    /* Helper classes
    /**********************************************
     */
    
    /**
     * Dummy deserializer used for verifying that partially handled (i.e. not yet
     * resolved) deserializers are not allowed to be used.
     */
    static class BeanDeserializer
        extends JsonDeserializer<Bean>
        implements ResolvableDeserializer
    {
        protected volatile boolean resolved = false;
        
        @Override
        public Bean deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException
        {
            if (!resolved) {
                throw new IOException("Deserializer not yet completely resolved");
            }
            Bean b = new Bean();
            b.value = 13;
            return b;
        }

        @Override
        public void resolve(DeserializationConfig config, DeserializerProvider provider)
            throws JsonMappingException
        {
            try {
                Thread.sleep(100L);
            } catch (Exception e) { }
            resolved = true;
        }
    }

    /*
    /**********************************************
    /* Unit tests
    /**********************************************
     */

    public void testDeserializerResolution() throws Exception
    {
        /* Let's repeat couple of times, just to be sure; thread timing is not
         * exact science; plus caching plays a role too
         */
        final String JSON = "{\"value\":42}";
        
        for (int i = 0; i < 5; ++i) {
            final ObjectMapper mapper = new ObjectMapper();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        /*Bean b =*/ mapper.readValue(JSON, Bean.class);
                    } catch (Exception e) { }
                }
            };
            Thread t = new Thread(r);
            t.start();
            // then let it proceed
            Thread.sleep(10L);
            // and try the same...
            Bean b = mapper.readValue(JSON, Bean.class);
            // note: funny deserializer, mangles data.. :)
            assertEquals(13, b.value);
            t.join();
        }   
    }
}
