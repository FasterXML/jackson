package perf;
import java.io.*;

import org.codehaus.jackson.*;

public final class TestReadPerf
{
    private final int REPS;

//    private final static int TEST_PER_GC = 5;
    private final static int TEST_PER_GC = 100;

    final JsonFactory mJsonFactory;

    final byte[] mData;

    final String mDataStr; // if testing reader-backed parser
    
    private TestReadPerf(File f)
        throws Exception
    {
        mJsonFactory = new JsonFactory();
        mData = readData(f);
        mDataStr = new String(mData, "UTF-8");
        // Estimate about 10 megs worth of data...
        REPS = 1 + (int) (10000000L / mData.length);
        System.out.println("Input size: "+mData.length+" bytes ("+mDataStr.length()+" chars); use "+REPS+" reps");
    }

    public void test()
        throws Exception
    {
        int counter = 0;
        
        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            long curr = System.currentTimeMillis();
            int result = testRead(REPS);
            curr = System.currentTimeMillis() - curr;
            System.out.println("Took "+curr+" msecs ("+(result & 0xFF)+").");
            if (++counter >= TEST_PER_GC) {
                counter = 0;
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
                System.out.println("[GC]");
                System.gc();
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            }
        }
    }

    private int testRead(int reps)
        throws IOException
    {
        int count = 0;
        JsonParser jp = null;
        while (--reps >= 0) {
            // Bytes or chars?
            jp = mJsonFactory.createJsonParser(mData, 0, mData.length);
            //jp = mJsonFactory.createJsonParser(new StringReader(mDataStr));
            JsonToken t;

            while ((t = jp.nextToken()) != null) {
                if (t == JsonToken.VALUE_NUMBER_INT) {
                    count += jp.getIntValue();
                }
                if (t == JsonToken.VALUE_STRING) {
                    count += jp.getText().length();
                }
            }
            jp.close();
        }
        return count;
    }

    private final byte[] readData(File f)
        throws IOException
    {
        int len = (int) f.length();
        byte[] data = new byte[len];
        int offset = 0;
        FileInputStream fis = new FileInputStream(f);
        
        while (len > 0) {
            int count = fis.read(data, offset, len-offset);
            offset += count;
            len -= count;
        }

        return data;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... <file>");
            System.exit(1);
        }
        new TestReadPerf(new File(args[0])).test();
    }
}
