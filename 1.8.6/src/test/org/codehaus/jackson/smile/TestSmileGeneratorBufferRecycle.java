package org.codehaus.jackson.smile;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;

/* Test based on kimchy's issue (see https://gist.github.com/853232);
 * exhibits an issue with buffer recycling.
 * 
 * @since 1.8
 */
public class TestSmileGeneratorBufferRecycle extends SmileTestBase
{
    public void testMaps() throws Exception
    {
        SmileFactory factory = new SmileFactory();

        Map<?,?> props1 = buildMap("", 65);
        Map<?,?> props2 = buildMap("", 1);

        writeMapAndParse(factory, props1);
        writeMapAndParse(factory, props2);
        writeMapAndParse(factory, props1);
        writeMapAndParse(factory, props2);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private static void writeMapAndParse(SmileFactory factory, Map<?,?> map) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // generate
        JsonGenerator generator = factory.createJsonGenerator(os);
        writeMap(generator, map);
        generator.close();

        // parse
        JsonParser parser = factory.createJsonParser(os.toByteArray());
        while (parser.nextToken() != null) {

        }
    }

    private static Map<?,?> buildMap(String prefix, int size) {
        HashMap<String,String> props = new HashMap<String, String>();
        for (int it = 0; it < size; it++) {
            String key = prefix + "prop_" + it;
            props.put(key, "a");
        }
        return props;
    }


    // A sample utility to write a map

    public static void writeMap(JsonGenerator gen, Map<?,?> map) throws IOException {
        gen.writeStartObject();

        for (Map.Entry<?,?> entry : map.entrySet()) {
            gen.writeFieldName((String) entry.getKey());
            Object value = entry.getValue();
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toString());
            }
        }

        gen.writeEndObject();
    }
}
