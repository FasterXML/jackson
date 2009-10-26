package org.codehaus.jackson.main;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;

/**
 * Set of basic unit tests for verifying that the basic generator
 * functionality works as expected.
 */
public class TestJsonGeneratorFeatures
    extends main.BaseTest
{
    public void testConfigDefaults() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        JsonGenerator jg = jf.createJsonGenerator(new StringWriter());
        assertFalse(jg.isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));
    }

    public void testFieldNameQuoting() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default, quoting should be enabled
        _testFieldNameQuoting(jf, true);
        // can disable it
        jf.disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        _testFieldNameQuoting(jf, false);
        // and (re)enable:
        jf.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        _testFieldNameQuoting(jf, true);
    }

    public void testNonNumericQuoting()
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default, quoting should be enabled
        _testNonNumericQuoting(jf, true);
        // can disable it
        jf.disable(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS);
        _testNonNumericQuoting(jf, false);
        // and (re)enable:
        jf.enable(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS);
        _testNonNumericQuoting(jf, true);
    }

    /**
     * Testing for [JACKSON-176], ability to force serializing numbers
     * as JSON Strings.
     */
    public void testNumbersAsJSONStrings() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default should output numbers as-is:
        assertEquals("[1,2,1.25,2.25,3001,0.5,-1]", _writeNumbers(jf));        

        // but if overridden, quotes as Strings
        jf.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        assertEquals("[\"1\",\"2\",\"1.25\",\"2.25\",\"3001\",\"0.5\",\"-1\"]",
                     _writeNumbers(jf));
    }

    private String _writeNumbers(JsonFactory jf) throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator jg = jf.createJsonGenerator(sw);
    
        jg.writeStartArray();
        jg.writeNumber(1);
        jg.writeNumber(2L);
        jg.writeNumber(1.25);
        jg.writeNumber(2.25f);
        jg.writeNumber(BigInteger.valueOf(3001));
        jg.writeNumber(BigDecimal.valueOf(0.5));
        jg.writeNumber("-1");
        jg.writeEndArray();
        jg.close();

        return sw.toString();
    }

    
    @SuppressWarnings("deprecation")
    public void testDeprecated() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        JsonGenerator jg = jf.createJsonGenerator(new StringWriter());

        jg.enableFeature(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        assertTrue(jg.isFeatureEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        jg.disableFeature(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        assertFalse(jg.isFeatureEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        jg.setFeature(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        assertTrue(jg.isFeatureEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
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
