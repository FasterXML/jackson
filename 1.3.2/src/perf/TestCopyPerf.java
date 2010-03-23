import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;

public final class TestCopyPerf
{
    private final static int REPS = 2500;

    final JsonFactory _jsonFactory;

    final JsonNode _tree;

    private TestCopyPerf(File f)
        throws Exception
    {
        _jsonFactory = new JsonFactory();
        FileInputStream fis = new FileInputStream(f);
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = _jsonFactory.createJsonParser(fis);
        _tree = mapper.readTree(jp);
        jp.close();
    }

    public void test()
        throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(2000);
        testCopy(1, bos);
        System.out.println("Output length: "+bos.size());
        System.out.println();

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            long curr = System.currentTimeMillis();
            int result = testCopy(REPS, bos);
            curr = System.currentTimeMillis() - curr;
            System.out.println("Took "+curr+" msecs ("
                               +(result & 0xFF)+").");
        }
    }

    private int testCopy(int reps, ByteArrayOutputStream bos)
        throws IOException
    {
        JsonGenerator jg = null;
        while (--reps >= 0) {
            bos.reset();
            jg = _jsonFactory.createJsonGenerator(bos, JsonEncoding.UTF8);
            _tree.writeTo(jg);
            jg.close();
        }
        return jg.hashCode();
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... <file>");
            System.exit(1);
        }
        new TestCopyPerf(new File(args[0])).test();
    }
}
