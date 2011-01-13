package org.codehaus.jackson.smile;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.smile.SmileFactory;

public class TestSmileFeatures
    extends SmileTestBase
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    static class Bean {
        public int value;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    // Let's ensure indentation doesn't break anything (should be NOP)
    public void testIndent() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new SmileFactory());
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        Bean bean = new Bean();
        bean.value = 42;
        
        byte[] smile = mapper.writeValueAsBytes(bean);
        Bean result = mapper.readValue(smile, 0, smile.length, Bean.class);
        assertEquals(42, result.value);
    }
}
