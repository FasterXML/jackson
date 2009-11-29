package org.codehaus.jackson.map.interop;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.codehaus.jackson.map.ObjectMapper;

public class TestDOM extends org.codehaus.jackson.map.BaseMapTest
{
    final static String SIMPLE_XML = "<root attr='3'><leaf>Rock &amp; Roll!</leaf><?proc instr?></root>";
    
    public void testSerializeSimpleNonNS() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // Let's just parse first, easiest
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse
            (new InputSource(new StringReader(SIMPLE_XML)));
        // need to strip xml declaration, if any
        String outputRaw = mapper.writeValueAsString(doc);
        // And re-parse as String, since JSON has quotes...
        String output = mapper.readValue(outputRaw, String.class);
        /* ... and finally, normalize to (close to) canonical XML
         * output (single vs double quotes, xml declaration etc)
         */
        assertEquals(SIMPLE_XML, normalizeOutput(output));
    }

    public void testDeserialize() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Node node = mapper.readValue(SIMPLE_XML, Node.class);
        assertNotNull(node);
        // !!! TBI
    }

    /*
     **********************************************************
     * Helper methods
     **********************************************************
     */

    protected static String normalizeOutput(String output)
    {
        // XML declaration to get rid of?
        output = output.trim();
        if (output.startsWith("<?xml")) {
            // can find closing '>' of xml decl...
            output = output.substring(output.indexOf('>')+1).trim();
        }
        // And replace double quotes with single-quotes...
        return output.replace('"', '\'');
    }
}
