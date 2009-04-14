package org.codehaus.jackson.main;

import org.codehaus.jackson.*;

import java.io.*;

/**
 * Set of basic unit tests for verifying that the basic generator
 * functionality works as expected.
 */
public class TestJsonGeneratorFeatures
    extends main.BaseTest
{
    public void testFieldNameQuoting()
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default, quoting should be enabled
        _testFieldNameQuoting(jf, true);
        // can disable it
        jf.disableGeneratorFeature(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        _testFieldNameQuoting(jf, false);
        // and (re)enable:
        jf.enableGeneratorFeature(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        _testFieldNameQuoting(jf, true);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////
     */

    private void _testFieldNameQuoting(JsonFactory jf, boolean quoted)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator jg = jf.createJsonGenerator(sw);
        jg.writeStartObject();
        jg.writeFieldName("foo");
        jg.writeNumber(1);
        jg.writeEndObject();
        jg.close();

        String result = sw.toString();
        if (quoted) {
            assertEquals("{\"foo\":1}", result);
        } else {
            assertEquals("{foo:1}", result);
        }
    }
}
