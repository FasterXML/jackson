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

    public void testNonNumericQuoting()
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default, quoting should be enabled
        _testNonNumericQuoting(jf, true);
        // can disable it
        jf.disableGeneratorFeature(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS);
        _testNonNumericQuoting(jf, false);
        // and (re)enable:
        jf.enableGeneratorFeature(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS);
        _testNonNumericQuoting(jf, true);
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
    private void _testNonNumericQuoting(JsonFactory jf, boolean quoted)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator jg = jf.createJsonGenerator(sw);
        jg.writeStartObject();
        jg.writeFieldName("double");
        jg.writeNumber(Double.NaN);
        jg.writeEndObject();
        jg.writeStartObject();
        jg.writeFieldName("float");
        jg.writeNumber(Float.NaN);
        jg.writeEndObject();
        jg.close();
	
        String result = sw.toString();
        if (quoted) {
            assertEquals("{\"double\":\"NaN\"} {\"float\":\"NaN\"}", result);
        } else {
            assertEquals("{\"double\":NaN} {\"float\":NaN}", result);
        }
    }
}
