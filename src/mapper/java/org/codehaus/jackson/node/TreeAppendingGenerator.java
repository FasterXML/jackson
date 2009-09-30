package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;

public class TreeAppendingGenerator extends JsonGenerator
{
    /*
     *********************************************
     * Configuration
     *********************************************
     */

    protected ObjectCodec _objectCodec;

    /*
     *********************************************
     * State
     *********************************************
     */

    /*
     *********************************************
     * Life-cycle
     *********************************************
     */

    public TreeAppendingGenerator(ContainerNode n) { this(n, null); }

    public TreeAppendingGenerator(ContainerNode n, ObjectCodec codec) {
        // !!! TBI
    }
    
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void copyCurrentEvent(JsonParser jp) throws IOException,
            JsonProcessingException {
        // TODO Auto-generated method stub

    }

    @Override
    public void copyCurrentStructure(JsonParser jp) throws IOException,
            JsonProcessingException {
        // TODO Auto-generated method stub

    }

    @Override
    public JsonGenerator disable(Feature f) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonGenerator enable(Feature f) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void flush() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public ObjectCodec getCodec() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonStreamContext getOutputContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isClosed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isEnabled(Feature f) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JsonGenerator setCodec(ObjectCodec oc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonGenerator useDefaultPrettyPrinter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset,
            int len) throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeBoolean(boolean state) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeEndArray() throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeEndObject() throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeFieldName(String name) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeNumber(int v) throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeNumber(long v) throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeNumber(BigInteger v) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeNumber(double d) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeNumber(float f) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,
            JsonGenerationException, UnsupportedOperationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeObject(Object pojo) throws IOException,
            JsonProcessingException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeRaw(String text) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeRawValue(String text) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeRawValue(String text, int offset, int len)
            throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeRawValue(char[] text, int offset, int len)
            throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeStartArray() throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeStartObject() throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeString(String text) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeString(char[] text, int offset, int len)
            throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeTree(JsonNode rootNode) throws IOException,
            JsonProcessingException {
        // TODO Auto-generated method stub

    }

}
