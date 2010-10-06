package org.codehaus.jackson.map.ext;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import org.w3c.dom.*;

import org.codehaus.jackson.map.ObjectMapper;

public class TestDOM extends org.codehaus.jackson.map.BaseMapTest
{
    final static String SIMPLE_XML =
        "<root attr='3'><leaf>Rock &amp; Roll!</leaf><?proc instr?></root>";
    final static String SIMPLE_XML_NS =
        "<root ns:attr='abc' xmlns:ns='http://foo' />";
    
    public void testSerializeSimpleNonNS() throws Exception
    {
        // Let's just parse first, easiest
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse
            (new InputSource(new StringReader(SIMPLE_XML)));
        assertNotNull(doc);
        ObjectMapper mapper = new ObjectMapper();
        // need to strip xml declaration, if any
        String outputRaw = mapper.writeValueAsString(doc);
        // And re-parse as String, since JSON has quotes...
        String output = mapper.readValue(outputRaw, String.class);
        /* ... and finally, normalize to (close to) canonical XML
         * output (single vs double quotes, xml declaration etc)
         */
        assertEquals(SIMPLE_XML, normalizeOutput(output));
    }

    public void testDeserializeNonNS() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < 2; ++i) {
            Document doc;

            if (i == 0) {
                // First, as Document:
                doc = mapper.readValue(quote(SIMPLE_XML), Document.class);
            } else {
                // and then as plain Node (no difference)
                Node node = mapper.readValue(quote(SIMPLE_XML), Node.class);
                doc = (Document) node;
            }
            Element root = doc.getDocumentElement();
            assertNotNull(root);
            // non-ns, simple...
            assertEquals("root", root.getTagName());
            assertEquals("3", root.getAttribute("attr"));
            assertEquals(1, root.getAttributes().getLength());
            NodeList nodes = root.getChildNodes();
            assertEquals(2, nodes.getLength());
            Element leaf = (Element) nodes.item(0);
            assertEquals("leaf", leaf.getTagName());
            assertEquals(0, leaf.getAttributes().getLength());
            //"<root attr='3'><leaf>Rock &amp; Roll!</leaf><?proc instr?></root>";
            ProcessingInstruction pi = (ProcessingInstruction) nodes.item(1);
            assertEquals("proc", pi.getTarget());
            assertEquals("instr", pi.getData());
        }
    }
    
    public void testDeserializeNS() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Document doc = mapper.readValue(quote(SIMPLE_XML_NS), Document.class);
        Element root = doc.getDocumentElement();
        assertNotNull(root);
        assertEquals("root", root.getTagName());
        // Not sure if it ought to be "" or null...
        String uri = root.getNamespaceURI();
        assertTrue((uri == null) || "".equals(uri));
        // no child nodes:
        assertEquals(0, root.getChildNodes().getLength());
        // DOM is weird, includes ns decls as attributes...
        assertEquals(2, root.getAttributes().getLength());
        assertEquals("abc", root.getAttributeNS("http://foo", "attr"));
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
