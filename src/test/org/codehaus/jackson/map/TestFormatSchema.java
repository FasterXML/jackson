package org.codehaus.jackson.map;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.*;
import org.codehaus.jackson.io.IOContext;

/**
 * Basic tests to ensure that {@link FormatSchema} instances are properly
 * passed to {@link JsonGenerator} and {@link JsonParser} instances if
 * mapper, reader or writer is configured with one.
 * 
 * @since 1.8
 */
public class TestFormatSchema  extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    static class MySchema implements FormatSchema {
        @Override
        public String getSchemaType() { return "test"; }
    }
    
    static class FactoryWithSchema extends JsonFactory
    {
        @Override
        protected JsonParser _createJsonParser(Reader r, IOContext ctxt)
            throws IOException, JsonParseException
        {
            return new ParserWithSchema(ctxt, _parserFeatures, r);
        }

        @Override
        protected JsonGenerator _createJsonGenerator(Writer out, IOContext ctxt) throws IOException
        {
            return new GeneratorWithSchema(_generatorFeatures, _objectCodec);
        }
    }

    // Ugly, but easiest way to get schema back is to throw exception...
    @SuppressWarnings("serial")
    static class SchemaException extends RuntimeException
    {
        public final FormatSchema _schema;
        
        public SchemaException(FormatSchema s) {
            _schema = s;
        }
    }
    
    static class ParserWithSchema extends ReaderBasedParserBase
    {
        public ParserWithSchema(IOContext ioCtxt, int features, Reader r)
        {
            super(ioCtxt, features, r);
        }

        @Override
        public void setSchema(FormatSchema schema) {
            throw new SchemaException(schema);
        }
        
        @Override
        protected byte[] _decodeBase64(Base64Variant b64variant) {
            return null;
        }

        @Override
        protected void _finishString() throws IOException, JsonParseException { }

        @Override
        public byte[] getBinaryValue(Base64Variant b64variant) {
            return null;
        }

        @Override
        public String getText() throws IOException, JsonParseException {
            return null;
        }

        @Override
        public char[] getTextCharacters() throws IOException {
            return null;
        }

        @Override
        public int getTextLength() throws IOException, JsonParseException {
            return 0;
        }

        @Override
        public int getTextOffset() throws IOException, JsonParseException {
            return 0;
        }

        @Override
        public JsonToken nextToken() throws IOException, JsonParseException {
            return null;
        }

        @Override
        public ObjectCodec getCodec() {
            return null;
        }

        @Override
        public void setCodec(ObjectCodec c) { }
    }

    static class GeneratorWithSchema extends JsonGeneratorBase
    {
        public GeneratorWithSchema(int features, ObjectCodec codec)
        {
            super(features, codec);
        }

        @Override
        public void setSchema(FormatSchema schema) {
            throw new SchemaException(schema);
        }

        @Override
        protected void _releaseBuffers() { }

        @Override
        protected void _verifyValueWrite(String typeMsg) throws IOException { }

        @Override
        public void flush() throws IOException { }

        @Override
        public void writeBinary(Base64Variant b64variant, byte[] data,
                int offset, int len) throws IOException { }

        @Override
        public void writeBoolean(boolean state) throws IOException { }

        @Override
        public void writeFieldName(String name) throws IOException { }

        @Override
        public void writeNull() throws IOException, JsonGenerationException { }

        @Override
        public void writeNumber(int v) throws IOException { }

        @Override
        public void writeNumber(long v) throws IOException { }

        @Override
        public void writeNumber(BigInteger v) throws IOException { }

        @Override
        public void writeNumber(double d) throws IOException { }

        @Override
        public void writeNumber(float f) throws IOException { }

        @Override
        public void writeNumber(BigDecimal dec) throws IOException { }

        @Override
        public void writeNumber(String encodedValue) throws IOException { }

        @Override
        public void writeRaw(String text) throws IOException { }

        @Override
        public void writeRaw(String text, int offset, int len) { }

        @Override
        public void writeRaw(char[] text, int offset, int len) { }

        @Override
        public void writeRaw(char c) throws IOException { }

        @Override
        public void writeRawUTF8String(byte[] text, int offset, int length) { }

        @Override
        public void writeString(String text) throws IOException { }

        @Override
        public void writeString(char[] text, int offset, int len) { }

        @Override
        public void writeUTF8String(byte[] text, int offset, int length) { }
    }
    
    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */
    
    public void testFormatForParsers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new FactoryWithSchema());
        MySchema s = new MySchema();
        StringReader r = new StringReader("{}");
        //  bit ugly, but can't think of cleaner simple way to check this...
        try {
            mapper.schemaBasedReader(s).withType(Object.class).readValue(r);
            fail("Excpected exception");
        } catch (SchemaException e) {
            assertSame(s, e._schema);
        }
    }

    public void testFormatForGenerators() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new FactoryWithSchema());
        MySchema s = new MySchema();
        StringWriter sw = new StringWriter();
        //  bit ugly, but can't think of cleaner simple way to check this...
        try {
            mapper.schemaBasedWriter(s).writeValue(sw, "Foobar");
            fail("Excpected exception");
        } catch (SchemaException e) {
            assertSame(s, e._schema);
        }
    }

}
