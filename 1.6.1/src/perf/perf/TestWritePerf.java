package perf;
import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.SerializedString;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;

public class TestWritePerf
{
    private final int REPS = 250000;

    private final static String NAME1 = "name";
    private final static String NAME2 = "value";
    private final static String NAME3 = "longerName";
    private final static String NAME4 = "something";
    private final static String NAME5 = "x";
    private final static String NAME6 = "width";

    private final static SerializedString TOKEN1 = new SerializedString(NAME1);
    private final static SerializedString TOKEN2 = new SerializedString(NAME2);
    private final static SerializedString TOKEN3 = new SerializedString(NAME3);
    private final static SerializedString TOKEN4 = new SerializedString(NAME4);
    private final static SerializedString TOKEN5 = new SerializedString(NAME5);
    private final static SerializedString TOKEN6 = new SerializedString(NAME6);
    
    final JsonFactory _jsonFactory;
    
    final SmileFactory _smileFactory;
    
    private TestWritePerf() throws Exception
    {
        _jsonFactory = new JsonFactory();
        _smileFactory = new SmileFactory();
        // whether to use back-refs for field names has measurable impact on ser/deser (but different direction):
        _smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, false);
    }

    public void test() throws Exception
    {
        int round = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            testCopy(REPS, round);
            ++round;
        }
    }

    private long testCopy(int reps, int round)
        throws IOException
    {
        int mode = (round % 1);
        if (mode == 0) System.out.println();

        final JsonFactory factory;
        final boolean useSymbols;
        boolean useBytes = true;
        String desc;
        final DummyOutputStream out = new DummyOutputStream();
        final DummyWriter outw = new DummyWriter();

        switch (mode) {
        case 0:
            factory = _jsonFactory;
            desc = "JsonGenerator";
            useSymbols = true;
            break;
        case 1:
            factory = _jsonFactory;
            desc = "JsonGenerator";
            useSymbols = false;
            break;
        case 2:
            factory = _smileFactory;
            desc = "SmileGenerator";
            useSymbols = true;
            break;
        case 3:
            factory = _smileFactory;
            desc = "SmileGenerator";
            useSymbols = false;
            break;
        case 4:
            factory = _jsonFactory;
            desc = "JsonGenerator/Writer";
            useSymbols = true;
            useBytes = false;
            break;
        case 5:
            factory = _jsonFactory;
            desc = "JsonGenerator/Writer";
            useSymbols = false;
            useBytes = false;
            break;
        default:
            throw new Error();
        }
        final long start = System.currentTimeMillis();
        JsonGenerator jg = null;
        if (useSymbols) {
            while (--reps >= 0) {
                jg = useBytes ? factory.createJsonGenerator(out, JsonEncoding.UTF8)
                            : factory.createJsonGenerator(outw);
                writeUsingTokens(jg);
                jg.close();
            }
        } else {
            while (--reps >= 0) {
                jg = useBytes ? factory.createJsonGenerator(out, JsonEncoding.UTF8)
                        : factory.createJsonGenerator(outw);
                writeUsingNames(jg);
                jg.close();
            }
        }
        jg.close();

        long time = System.currentTimeMillis() - start;
        if (useSymbols) {
            desc += "/TOKEN";
        } else {
            desc += "/name";
        }
        int count = useBytes ? out.getCount() : outw.getCount();
        System.out.println("Took "+time+" msecs (round "+round+") for: "+desc +" / "+count);
        return time;
    }

    private final void writeUsingNames(JsonGenerator jgen)
        throws IOException
    {
        jgen.writeStartObject();
        jgen.writeFieldName(NAME1);
        jgen.writeNumber(10);
        jgen.writeFieldName(NAME2);
        jgen.writeBoolean(true);
        jgen.writeFieldName(NAME3);
        jgen.writeNull();
        jgen.writeFieldName(NAME4);
        jgen.writeBoolean(false);
        jgen.writeFieldName(NAME5);
        jgen.writeNumber(3);
        jgen.writeFieldName(NAME6);
        jgen.writeNull();
        jgen.writeEndObject();
    }

    private final void writeUsingTokens(JsonGenerator jgen) throws IOException
    {
        jgen.writeStartObject();
        jgen.writeFieldName(TOKEN1);
        jgen.writeNumber(10);
        jgen.writeFieldName(TOKEN2);
        jgen.writeBoolean(true);
        jgen.writeFieldName(TOKEN3);
        jgen.writeNull();
        jgen.writeFieldName(TOKEN4);
        jgen.writeBoolean(false);
        jgen.writeFieldName(TOKEN5);
        jgen.writeNumber(3);
        jgen.writeFieldName(TOKEN6);
        jgen.writeNull();
        jgen.writeEndObject();
    }
    
    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        new TestWritePerf().test();
    }
    
    private final static class DummyOutputStream
        extends OutputStream
    {
        public int count = 0;
        
        @Override public void write(byte[] b) { count += b.length; }
        @Override public void write(byte[] b, int offset, int len) { count += len; }
        
        @Override
        public void write(int b) throws IOException {
            ++count;
        }

        public int getCount() { return count; }
    }

    private final static class DummyWriter
        extends Writer
    {
        public int count = 0;

        @Override
        public void close() throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            count += len;
        }

        public int getCount() { return count; }
    }
}
