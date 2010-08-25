package org.codehaus.jackson.jaxrs;

import java.util.*;

import javax.ws.rs.core.StreamingOutput;

/**
 * @since 1.5
 */
public class TestUntouchables
    extends main.BaseTest
{
    public void testDefaultUntouchables() throws Exception
    {
        JacksonJsonProvider prov = new JacksonJsonProvider();
        // By default, no reason to exclude, say, this test class...
        assertTrue(prov.isReadable(getClass(), getClass(), null, null));
        assertTrue(prov.isWriteable(getClass(), getClass(), null, null));

        // but some types should be ignored (set of ignorable may change over time tho!)
        assertFalse(prov.isReadable(String.class, getClass(), null, null));
        assertFalse(prov.isWriteable(StreamingOutput.class, StreamingOutput.class, null, null));
    }

    public void testCustomUntouchables() throws Exception
    {
        JacksonJsonProvider prov = new JacksonJsonProvider();        
        // can mark this as ignorable...
        prov.addUntouchable(getClass());
        // and then it shouldn't be processable
        assertFalse(prov.isReadable(getClass(), getClass(), null, null));
        assertFalse(prov.isWriteable(getClass(), getClass(), null, null));

        // Same for interfaces, like:
        prov.addUntouchable(Collection.class);
        assertFalse(prov.isReadable(ArrayList.class, ArrayList.class, null, null));
        assertFalse(prov.isWriteable(HashSet.class, HashSet.class, null, null));
    }
}
    