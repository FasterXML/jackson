package org.codehaus.jackson.xml.util;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.PrettyPrinter;
import org.codehaus.jackson.xml.ToXmlGenerator;

/**
 * Indentation to use with XML is different from JSON, because JSON
 * requires use of separator characters and XML just basic whitespace.
 * 
 * @since 1.7
 */
public class DefaultXmlPrettyPrinter
    implements PrettyPrinter
{
    /*
    /**********************************************************
    /* Root-level values
    /**********************************************************
     */

    @Override
    public void writeRootValueSeparator(JsonGenerator jgen) throws IOException, JsonGenerationException {
        // Not sure if this should ever be applicable; but if multiple roots were allowed, we'd use linefeed
        jgen.writeRaw('\n');
    }
    
    /*
    /**********************************************************
    /* Array values
    /**********************************************************
     */
    
    @Override
    public void beforeArrayValues(JsonGenerator jgen) throws IOException, JsonGenerationException {
        // anything to do here?
    }

    @Override
    public void writeStartArray(JsonGenerator jgen) throws IOException, JsonGenerationException {
        ((ToXmlGenerator) jgen)._writeStartArray();
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator jgen)  throws IOException, JsonGenerationException {
        // all markup by elements, no separators; nothing to do here
    }
    
    @Override
    public void writeEndArray(JsonGenerator jgen, int nrOfValues) throws IOException, JsonGenerationException {
        ((ToXmlGenerator) jgen)._writeEndArray();
    }
    
    /*
    /**********************************************************
    /* Object values
    /**********************************************************
     */
    
    @Override
    public void beforeObjectEntries(JsonGenerator jg)  throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeStartObject(JsonGenerator jgen) throws IOException, JsonGenerationException {
        ((ToXmlGenerator) jgen)._writeStartObject();
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
        // all markup by elements, no separators; nothing to do here
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
        // all markup by elements, no separators; nothing to do here
    }
    
    @Override
    public void writeEndObject(JsonGenerator jgen, int nrOfEntries) throws IOException, JsonGenerationException {
        ((ToXmlGenerator) jgen)._writeEndObject();
    }

}
