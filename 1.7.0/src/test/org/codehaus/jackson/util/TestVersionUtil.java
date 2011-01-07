package org.codehaus.jackson.util;

import org.codehaus.jackson.Version;

public class TestVersionUtil extends main.BaseTest
{
    public void testVersionPartParsing()
    {
        assertEquals(13, VersionUtil.parseVersionPart("13"));
        assertEquals(27, VersionUtil.parseVersionPart("27.8"));
        assertEquals(0, VersionUtil.parseVersionPart("-3"));
    }

    public void testVersionParsing()
    {
        assertEquals(new Version(1, 2, 15, "foo"), VersionUtil.parseVersion("1.2.15-foo"));
    }
}
