package org.codehaus.jackson.version;

import org.codehaus.jackson.*;

/**
 * Tests to verify [JACKSON-278]
 * 
 * @since 1.6
 */
public class TestVersions extends main.BaseTest
{
    public void testCoreVersions()
    {
        Version v = new JsonFactory().version();
        assertEquals(1, v.getMajorVersion());
        assertEquals(6, v.getMinorVersion());
        assertEquals(0, v.getPatchLevel());
    }
}
