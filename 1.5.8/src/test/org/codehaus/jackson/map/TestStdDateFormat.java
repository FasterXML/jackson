package org.codehaus.jackson.map;

import java.util.*;

import org.codehaus.jackson.map.util.StdDateFormat;

public class TestStdDateFormat
    extends BaseMapTest
{
    public void testFactories() {
        assertNotNull(StdDateFormat.getBlueprintISO8601Format());
        assertNotNull(StdDateFormat.getBlueprintRFC1123Format());
        TimeZone tz = TimeZone.getTimeZone("GMT");
        assertNotNull(StdDateFormat.getISO8601Format(tz));
        assertNotNull(StdDateFormat.getRFC1123Format(tz));
    }

    public void testInvalid() {
        StdDateFormat std = new StdDateFormat();
        try {
            std.parse("foobar");
        } catch (java.text.ParseException e) {
            verifyException(e, "Can not parse");
        }
    }
}
