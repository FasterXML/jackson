package perf;
import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Micro benchmark for testing whether specialized String serialization
 * functionality is faster than generic one
 */
public final class TestStringSerPerf
{
    private final int REPS;
    private final ObjectMapper _mapper;

    final JsonNode _tree;

    final int _byteSize;

    public TestStringSerPerf(File f)
        throws Exception
    {
        _mapper = new ObjectMapper();
        int length = (int) f.length();
        _tree = _mapper.readValue(f, JsonNode.class);
        // Let's try to guestimate suitable size... ~2 megs
        REPS = 5 + ((2 * 1000 * 1000) / length);

        // Also: figure out byte size
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        _mapper.writeValue(bos, _tree);
        _byteSize = bos.size();

        System.out.println("DEBUG: byte size: "+_byteSize+"; "+REPS+" reps");
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 3);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Byte[]";
                sum += testToBytes();
                break;
            case 1:
                msg = "StringWriter";
                sum += testUsingStringWriter();
                break;
            case 2:
                msg = "String, specialized";
                sum += testUsingSpecial();
                break;

            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    protected int testToBytes() throws Exception
    {
        int total = 0;
        for (int i = 0; i < REPS; ++i) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(_byteSize);
            _mapper.writeValue(out, _tree);
            total += out.size();
        }
        return total;
    }

    protected int testUsingStringWriter() throws Exception
    {
        int total = 0;
        for (int i = 0; i < REPS; ++i) {
            StringWriter sw = new StringWriter(16);
            _mapper.writeValue(sw, _tree);
            total += sw.toString().length();
        }
        return total;
    }

    protected int testUsingSpecial() throws Exception
    {
        int total = 0;
        for (int i = 0; i < REPS; ++i) {
            String str = _mapper.writeValueAsString(_tree);
            total += str.length();
        }
        return total;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... <file>");
            System.exit(1);
        }
        new TestStringSerPerf(new File(args[0])).test();
    }
}
