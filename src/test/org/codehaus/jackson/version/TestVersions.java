package org.codehaus.jackson.version;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.*;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Tests to verify [JACKSON-278]
 * 
 * @since 1.6
 */
public class TestVersions extends main.BaseTest
{
    /* NOTE: named this way so as NOT to run from Eclipse with normal settings.
     */
    public void allVersions()
    {
        coreVersions();
        mapperVersions();
    }

    private void coreVersions()
    {
        assertVersion(new JsonFactory().version(), 1, 6);
        assertVersion(new ReaderBasedParser(null, 0, null, null, null).version(), 1, 6);
        assertVersion(new JsonFactory().version(), 1, 6);
    }

    private void mapperVersions()
    {
        assertVersion(new ObjectMapper().version(), 1, 6);
    }
    
    private void assertVersion(Version v, int major, int minor)
    {
        assertFalse("Should find version information", v.isUknownVersion());
        assertEquals(major, v.getMajorVersion());
        assertEquals(minor, v.getMinorVersion());
//        assertEquals(0, v.getPatchLevel());
    }
}
