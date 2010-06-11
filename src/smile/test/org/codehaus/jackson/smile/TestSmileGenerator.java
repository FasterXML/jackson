package org.codehaus.jackson.smile;

import java.io.*;

import org.junit.Assert;

import static org.codehaus.jackson.smile.SmileConstants.*;

public class TestSmileGenerator
    extends BaseSmileTest
{
    /**
     * Test for verifying handling of 'true', 'false' and 'null' literals
     */
    public void testSimpleLiterals() throws Exception
    {
        // true, no header (or frame marker)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = _generator(out, false);
        gen.writeBoolean(true);
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_TRUE);

        // false, no header or frame marker
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeBoolean(false);
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_FALSE);

        // null, no header or frame marker
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNull();
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_NULL);

        // And then with some other combinations:
        // true, but with header
        out = new ByteArrayOutputStream();
        gen = _generator(out, true);
        gen.writeBoolean(true);
        gen.close();
        _verifyBytes(out.toByteArray(),
                HEADER_BYTE_1, HEADER_BYTE_2, HEADER_BYTE_3, HEADER_BYTE_4,
                SmileConstants.TOKEN_LITERAL_TRUE);

        // null, with header and end marker
        out = new ByteArrayOutputStream();
        gen = _generator(out, true);
        gen.enable(SmileGenerator.Feature.WRITE_END_MARKER);
        gen.writeNull();
        gen.close();
        _verifyBytes(out.toByteArray(),
                HEADER_BYTE_1, HEADER_BYTE_2, HEADER_BYTE_3, HEADER_BYTE_4,
                TOKEN_LITERAL_NULL, BYTE_MARKER_END_OF_CONTENT);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

}
