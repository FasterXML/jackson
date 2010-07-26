package org.codehaus.jackson.schema;

import java.util.*;

import org.codehaus.jackson.map.*;

/**
 * Trivial test to ensure {@link JsonSchema} can be also deserialized
 */
public class TestReadJsonSchema
    extends org.codehaus.jackson.map.BaseMapTest
{
    enum SchemaEnum { YES, NO; }

    static class Schemable {
        public String name;
        public char[] nameBuffer;

        // We'll include tons of stuff, just to force generation of schema
        public boolean[] states;
        public byte[] binaryData;
        public short[] shorts;
        public int[] ints;
        public long[] longs;

        public float[] floats;
        public double[] doubles;

        public Object[] objects;
        public JsonSerializableWithType someSerializable;

        public Iterable<Object> iterableOhYeahBaby;

        public List<String> extra;
        public ArrayList<String> extra2;
        public Iterator<String[]> extra3;

        public Map<String,Double> sizes;
        public EnumMap<SchemaEnum,List<String>> whatever;

        SchemaEnum testEnum;
        public EnumSet<SchemaEnum> testEnums;
    }

    /**
     * Verifies that a simple schema that is serialized can be
     * deserialized back to equal schema instance
     */
    public void testDeserializeSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema schema = mapper.generateJsonSchema(Schemable.class);
        assertNotNull(schema);

        String schemaStr = mapper.writeValueAsString(schema);
        assertNotNull(schemaStr);
        JsonSchema result = mapper.readValue(schemaStr, JsonSchema.class);
        assertEquals("Trying to read from '"+schemaStr+"'", schema, result);
    }
}
