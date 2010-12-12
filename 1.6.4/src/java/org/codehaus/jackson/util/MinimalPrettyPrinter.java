package org.codehaus.jackson.util;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.PrettyPrinter;

/**
 * {@link PrettyPrinter} implementation that adds no indentation,
 * just implements everything necessary for value output to work
 * as expected, and provide simpler extension points to allow
 * for creating simple custom implementations that add specific
 * decoration or overrides. Since behavior then is very similar
 * to using no pretty printer at all, usually sub-classes are used.
 *<p>
 * Beyond purely minimal implementation, there is limited amount of
 * configurability which may be useful for actual use: for example,
 * it is possible to redefine separator used between root-level
 * values (default is single space; can be changed to line-feed).
 * 
 * @since 1.6
 */
public class MinimalPrettyPrinter
    implements PrettyPrinter
{
    /**
     * Default String used for separating root values is single space.
     */
    public final static String DEFAULT_ROOT_VALUE_SEPARATOR = " ";
    
    protected String _rootValueSeparator = DEFAULT_ROOT_VALUE_SEPARATOR;

    /*
    /**********************************************************
    /* Life-cycle, construction, configuration
    /**********************************************************
     */
    
    public MinimalPrettyPrinter() { }

    public void setRootValueSeparator(String sep) {
        _rootValueSeparator = sep;
    }
    
    /*
    /**********************************************************
    /* PrettyPrinter impl
    /**********************************************************
     */

    @Override
    public void writeRootValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException
    {
        if (_rootValueSeparator != null) {
            jg.writeRaw(_rootValueSeparator);    
        }
    }
    
    public void writeStartObject(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw('{');
    }
    
    public void beforeObjectEntries(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        // nothing special, since no indentation is added
    }

    /**
     * Method called after an object field has been output, but
     * before the value is output.
     *<p>
     * Default handling will just output a single
     * colon to separate the two, without additional spaces.
     */
    public void writeObjectFieldValueSeparator(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(':');
    }
    
    /**
     * Method called after an object entry (field:value) has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two.
     */
    public void writeObjectEntrySeparator(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(',');
    }

    public void writeEndObject(JsonGenerator jg, int nrOfEntries)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw('}');
    }
    
    public void writeStartArray(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw('[');
    }
    
    public void beforeArrayValues(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        // nothing special, since no indentation is added
    }

    /**
     * Method called after an array value has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate values.
     */
    public void writeArrayValueSeparator(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(',');
    }
    
    public void writeEndArray(JsonGenerator jg, int nrOfValues)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(']');
    }
}
