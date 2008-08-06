import java.io.*;

import org.codehaus.jackson.*;

public final class TestReadPerf
{
    private final static int REPS = 2500;

    private final static int TEST_PER_GC = 5;

    final JsonFactory mJsonFactory;

    final byte[] mData;

    private TestReadPerf(File f)
        throws Exception
    {
        mJsonFactory = new JsonFactory();
        mData = readData(f);
    }

    public void test()
        throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(2000);
        System.out.println("Output length: "+bos.size());
        System.out.println();

        int counter = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            long curr = System.currentTimeMillis();
            int result = testRead(REPS);
            curr = System.currentTimeMillis() - curr;
            System.out.println("Took "+curr+" msecs ("
                               +(result & 0xFF)+").");
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
        JsonParser jp = null;
        while (--reps >= 0) {
            jp = mJsonFactory.createJsonParser(new ByteArrayInputStream(mData));
            JsonToken t;

            while ((t = jp.nextToken()) != null) {
                if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                    jp.getDoubleValue();
                }
            }
            jp.close();
        }
        return jp.hashCode();
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
