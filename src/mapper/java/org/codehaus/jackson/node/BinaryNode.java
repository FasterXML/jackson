package org.codehaus.jackson.node;

import java.io.IOException;
import java.util.Arrays;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Value node that contains Base64 encoded binary value, which will be
 * output and stored as Json String value.
 */
public final class BinaryNode
    extends ValueNode
{
    final static BinaryNode EMPTY_BINARY_NODE = new BinaryNode(new byte[0]);

    final byte[] _data;

    public BinaryNode(byte[] data)
    {
        _data = data;
    }

    public BinaryNode(byte[] data, int offset, int length)
    {
        if (offset == 0 && length == data.length) {
            _data = data;
        } else {
            _data = new byte[length];
            System.arraycopy(data, offset, _data, 0, length);
        }
    }

    public static BinaryNode valueOf(byte[] data)
    {
        if (data == null) {
            return null;
        }
        if (data.length == 0) {
            return EMPTY_BINARY_NODE;
        }
        return new BinaryNode(data);
    }

    public static BinaryNode valueOf(byte[] data, int offset, int length)
    {
        if (data == null) {
            return null;
        }
        if (length == 0) {
            return EMPTY_BINARY_NODE;
        }
        return new BinaryNode(data, offset, length);
    }

    @Override
    public JsonToken asToken() {
        /* No distinct type; could use one for textual values,
         * but given that it's not in text form at this point,
         * embedded-object is closest
         */
        return JsonToken.VALUE_EMBEDDED_OBJECT;
    }

    @Override
    public boolean isBinary() { return true; }

    /**
     *<p>
     * Note: caller is not to modify returned array in any way, since
     * it is not a copy but reference to the underlying byte array.
     */
    @Override
    public byte[] getBinaryValue() { return _data; }

    /**
     * Hmmh. This is not quite as efficient as using {@link #serialize},
     * but will work correctly.
     */
    public String getValueAsText() {
        return _asBase64(false, _data);
    }

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeBinary(_data);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        return Arrays.equals(((BinaryNode) o)._data, _data);
    }

    @Override
    public int hashCode() {
        return (_data == null) ? -1 : _data.length;
    }

    /**
     * Different from other values, since contents need to be surrounded
     * by (double) quotes.
     */
    @Override
    public String toString()
    {
        return _asBase64(true, _data);
    }

    /*
    /////////////////////////////////////////////////////////////////
    // Internal methods
    /////////////////////////////////////////////////////////////////
     */

    protected static String _asBase64(boolean addQuotes, byte[] input)
    {
        int inputEnd = input.length;
        StringBuilder sb = new StringBuilder(_outputLength(inputEnd));
        if (addQuotes) {
            sb.append('"');
        }
        // should there be a way to customize this?
        Base64Variant b64variant = Base64Variants.getDefaultVariant();

        int chunksBeforeLF = b64variant.getMaxLineLength() >> 2;

        // Ok, first we loop through all full triplets of data:
        int inputPtr = 0;
        int safeInputEnd = inputEnd-3; // to get only full triplets

        while (inputPtr <= safeInputEnd) {
            // First, mash 3 bytes into lsb of 32-bit int
            int b24 = ((int) input[inputPtr++]) << 8;
            b24 |= ((int) input[inputPtr++]) & 0xFF;
            b24 = (b24 << 8) | (((int) input[inputPtr++]) & 0xFF);
            b64variant.encodeBase64Chunk(sb, b24);
            if (--chunksBeforeLF <= 0) {
                // note: must quote in JSON value, so not really useful...
                sb.append('\\');
                sb.append('n');
                chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
            }
        }

        // And then we may have 1 or 2 leftover bytes to encode
        int inputLeft = inputEnd - inputPtr; // 0, 1 or 2
        if (inputLeft > 0) { // yes, but do we have room for output?
            int b24 = ((int) input[inputPtr++]) << 16;
            if (inputLeft == 2) {
                b24 |= (((int) input[inputPtr++]) & 0xFF) << 8;
            }
            b64variant.encodeBase64Partial(sb, b24, inputLeft);
        }

        if (addQuotes) {
            sb.append('"');
        }
        return sb.toString();
    }

    private static int _outputLength(int inputLen)
    {
        // let's approximate... 33% overhead, ~= 3/8 (0.375)
        return inputLen + (inputLen >> 2) + (inputLen >> 3);
    }

}
