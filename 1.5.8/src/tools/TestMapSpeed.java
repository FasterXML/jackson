import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public final class TestMapSpeed
{
    /**
     * Number of repetitions to run per test. Dynamically variable,
     * based on observed runtime, to try to keep it high enough.
     */
    private int REPS;

    /**
     * Let's keep per-run times above 50 milliseconds
     */
    //final static int MIN_RUN_TIME = 50;
    final static int MIN_RUN_TIME = 5;

    /**
     * Let's keep per-run times below 300 milliseconds
     */
    //final static int MAX_RUN_TIME = 300;
    final static int MAX_RUN_TIME = 1000;

    private final static int TEST_PER_GC = 17;

    final JsonFactory _factory;
    final ObjectMapper _mapper;

    final Object _objectToMap;

    private TestMapSpeed(byte[] data)
        throws Exception
    {
        _factory = new JsonFactory();
        _mapper = new ObjectMapper();
        JsonParser jp = _factory.createJsonParser(data, 0, data.length);
        _objectToMap = _mapper.readValue(jp, Object.class);
        jp.close();

        // Ok how should we guestimate speed... perhaps from data size?
        REPS = 100 + ((8 * 1000 * 1000) / data.length);
        System.out.println("Based on size, will use "+REPS+" repetitions");
    }

    protected int test()
        throws Exception
    {
        int i = 0;
        int total = 0;

        final int TEST_CASES = 2;
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);

        while (true) {
            try {  Thread.sleep(150L); } catch (InterruptedException ie) { }
            int round = (i++ % TEST_CASES);

            long now = System.currentTimeMillis();
            String msg;
            int sum = 0;
//round = 1; // testing just old or new?

            switch (round) {
            case 0:
                msg = "Map using OLD";
                sum = testOld(REPS, bos);
                break;
            case 1:
                msg = "Map using NEW";
                sum = testNew(REPS, bos);
                break;

            default:
                throw new Error("Internal error");
            }

            now = System.currentTimeMillis() - now;
            if (round == 0) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+now+" msecs ("
                               +sum+" -> "+(total & 0xFF)+").");

            total += sum;

            if ((i % TEST_PER_GC) == 0) {
                System.out.println("[GC]");
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
                System.gc();
                try {  Thread.sleep(200L); } catch (InterruptedException ie) { }

                /* One more tweak: let's add load if things start
                 * running too fast or slow, to try to get sweet range
                 * of 50 to 250 millisseconds
                 */
                if (now < MIN_RUN_TIME) {
                    REPS += (REPS / 5); // 20% up
                    System.out.println("[NOTE: increasing reps, now: "+REPS+"]");
                    try {  Thread.sleep(200L); } catch (InterruptedException ie) { }
                } else if (now > MAX_RUN_TIME && i > 20) {
                    /* Let's reduce load slower than increase; also,
                     * due to initial warmup, let's not adjust until
                     * we've gone through a few cycles
                     */
                    REPS -= (REPS / 10); // 10% down
                    System.out.println("[NOTE: decreasing reps, now: "+REPS+"]");
                    try {  Thread.sleep(200L); } catch (InterruptedException ie) { }
                }
            }
        }
    }

    /*
    /////////////////////////////////////////////////////////
    // Actual value type access, ones via Stax 1.0
    /////////////////////////////////////////////////////////
     */

    protected int testOld(int reps, ByteArrayOutputStream out)
        throws Exception
    {
        int total = 0;
        for (int i = 0; i < reps; ++i) {
            out.reset();
            JsonGenerator jg = _factory.createJsonGenerator(out, JsonEncoding.UTF8);
            _mapper.writeValue(jg, _objectToMap);
            jg.close();
            total = out.size();
        }
        return total;
    }

    protected int testNew(int reps, ByteArrayOutputStream out)
        throws Exception
    {
        int total = 0;
        for (int i = 0; i < reps; ++i) {
            out.reset();
            JsonGenerator jg = _factory.createJsonGenerator(out, JsonEncoding.UTF8);
            _mapper.writeValue(jg, _objectToMap);
            jg.close();
            total = out.size();
        }
        return total;
    }

    /*
    /////////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////////
     */

    static byte[] readData(File file) throws IOException
    {
        InputStream fin = new FileInputStream(file);
        byte[] buf = new byte[4000];
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4000);
        int count;
        
        while ((count = fin.read(buf)) > 0) {
            bos.write(buf, 0, count);
        }
        fin.close();
        return bos.toByteArray();
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... <file>");
            System.exit(1);
        }
        byte[] data = readData(new File(args[0]));
        System.out.println(" -> "+data.length+" bytes read.");
        new TestMapSpeed(data).test();
    }
}
