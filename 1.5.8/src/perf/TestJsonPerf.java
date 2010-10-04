import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.util.BufferRecycler;

// json.org's reference implementation
import org.json.*;
// StringTree implementation
import org.stringtree.json.JSONReader;
// Jsontool implementation
import com.sdicons.json.parser.JSONParser;
// Noggit:
//import org.apache.noggit.JSONParser;

public final class TestJsonPerf
{
    private final int REPS;

    private final static int TEST_PER_GC = 15;

    final JsonFactory mJsonFactory;

    final byte[] mData;

    protected int mBatchSize;

    public TestJsonPerf(File f)
        throws Exception
    {
        mJsonFactory = new JsonFactory();
        mData = readData(f);

        // Let's try to guestimate suitable size... to get to 50 megs parsed
        REPS = (int) ((double) (50 * 1000 * 1000) / (double) mData.length);

        System.out.println("Read "+mData.length+" bytes from '"+f+"'; will do "+REPS+" reps");
        System.out.println();
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            // Use 9 to test all...
            int round = (i++ % 5);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Jackson, stream/byte";
                sum += testJacksonStream(REPS, true);
                break;
            case 1:
                msg = "Jackson, stream/char";
                sum += testJacksonStream(REPS, false);
                break;
            case 2:
                msg = "Noggit";
                sum += testNoggit(REPS);
                break;

            case 3:
                msg = "Jackson, Java types";
                sum += testJacksonJavaTypes(REPS);
                break;

            case 4:
                msg = "Jackson, JSON types";
                sum += testJacksonJsonTypes(REPS);
                break;
            case 5:
                msg = "Json.org";
                sum += testJsonOrg(REPS);
                break;
            case 6:
                msg = "Json-simple";
                sum += testJsonSimple(REPS);
                break;
            case 7:
                msg = "JSONTools (berlios.de)";
                sum += testJsonTools(REPS);
                break;
            case 8:
                msg = "StringTree";
                sum += testStringTree(REPS);
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


            if ((i % TEST_PER_GC) == 0) {
                System.out.println("[GC]");
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
                System.gc();
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            }
        }
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

    protected int testJsonOrg(int reps)
        throws Exception
    {
        Object ob = null;
        // Json.org's code only accepts Strings:
        String input = new String(mData, "UTF-8");
        for (int i = 0; i < reps; ++i) {
            JSONTokener tok = new JSONTokener(input);
            ob = tok.nextValue();
        }
        return ob.hashCode();
    }

    protected int testJsonTools(int reps)
        throws Exception
    {
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            // Json-tools accepts streams, yay!
            JSONParser jp = new JSONParser(new ByteArrayInputStream(mData), "byte stream");
            /* Hmmmh. Will we get just one object for the whole thing?
             * Or a stream? Seems like just one
             */
            //while ((ob = jp.nextValue()) != null) { ; }
            ob = jp.nextValue();
        }
        return ob.hashCode();
    }

    protected int testStringTree(int reps)
        throws Exception
    {
        Object ob = null;
        String input = new String(mData, "UTF-8");
        for (int i = 0; i < reps; ++i) {
            // StringTree impl only accepts Strings:
            ob = new JSONReader().read(input);
        }
        return ob.hashCode();
    }

    protected int testJsonSimple(int reps)
        throws Exception
    {
        // Json.org's code only accepts Strings:
        String input = new String(mData, "UTF-8");
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            ob = org.json.simple.JSONValue.parse(input);
        }
        return ob.hashCode();
    }

    protected int testNoggit(int reps)
        throws Exception
    {
        ByteArrayInputStream bin = new ByteArrayInputStream(mData);

        char[] cbuf = new char[mData.length];

        IOContext ctxt = new IOContext(new BufferRecycler(), this, false);
        int sum = 0;

        for (int i = 0; i < reps; ++i) {
            /* This may be unfair advantage (allocating buffer of exact
             * size)? But let's do that for now
             */
            //char[] cbuf = new char[mData.length];
            //InputStreamReader r = new InputStreamReader(bin, "UTF-8");
            byte[] bbuf = ctxt.allocReadIOBuffer();
            /* 13-Jan-2009, tatu: Note: Noggit doesn't use our turbo-charged
             *   UTF8 codec by default. But let's make it as fast as we
             *   possibly can...
             */
            UTF8Reader r = new UTF8Reader(ctxt, bin, bbuf, 0, 0);

            bin.reset();
            org.apache.noggit.JSONParser jp = new org.apache.noggit.JSONParser(r, cbuf);
            int type;
            while ((type = jp.nextEvent()) != org.apache.noggit.JSONParser.EOF) {
                if (type == org.apache.noggit.JSONParser.STRING) {
                    sum += jp.getString().length();
                }
            }
        }
        return sum;
    }

    protected int testJacksonStream(int reps, boolean fast)
        throws Exception
    {
        int sum = 0;
        for (int i = 0; i < reps; ++i) {
            // note: fast is not used any more
            JsonParser jp;

            if (fast) {
                jp = mJsonFactory.createJsonParser(mData, 0, mData.length);
            } else {
                jp = mJsonFactory.createJsonParser(new ByteArrayInputStream(mData));
            }
            JsonToken t;
            while ((t = jp.nextToken()) != null) {
                // Field names are always constructed
                if (t == JsonToken.VALUE_STRING
                    //|| t == JsonToken.FIELD_NAME
                    ) {
                    sum += jp.getText().length();
                }
            }
            jp.close();
        }
        return sum;
    }

    protected int testJacksonJavaTypes(int reps)
        throws Exception
    {
        Object ob = null;
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < reps; ++i) {
            JsonParser jp = mJsonFactory.createJsonParser(new ByteArrayInputStream(mData));
            // This is "untyped"... Maps, Lists etc
            ob = mapper.readValue(jp, Object.class);
            jp.close();
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    @SuppressWarnings("deprecation")
    protected int testJacksonJsonTypes(int reps)
        throws Exception
    {
        Object ob = null;
        TreeMapper mapper = new TreeMapper();
        for (int i = 0; i < reps; ++i) {
            JsonParser jp = mJsonFactory.createJsonParser(new ByteArrayInputStream(mData));
            ob = mapper.readTree(jp);
            jp.close();
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... <file>");
            System.exit(1);
        }
        new TestJsonPerf(new File(args[0])).test();
    }
}

