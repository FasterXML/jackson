import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.map.JavaTypeMapper;
import org.codehaus.jackson.map.JsonTypeMapper;
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
    private final static int REPS = 2500;

    private final static int TEST_PER_GC = 5;

    final JsonFactory mJsonFactory;

    final byte[] mData;

    protected int mBatchSize;

    public TestJsonPerf(File f)
        throws Exception
    {
        mJsonFactory = new JsonFactory();
        mData = readData(f);

        System.out.println("Read "+mData.length+" bytes from '"+f+"'");
        System.out.println();
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            // Use 7 to test all...
            int round = (i++ % 3);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = false;

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
                lf = true;
                msg = "Noggit";
                sum += testNoggit(REPS);
                break;
                /*
            case 2:
                msg = "Jackson, Java types";
                sum += testJacksonJavaTypes(REPS);
                break;
                */
            case 3:
                msg = "Jackson, JSON types";
                sum += testJacksonJavaTypes(REPS);
                break;
            case 4:
                msg = "Json.org";
                sum += testJsonOrg(REPS);
                break;
            case 5:
                msg = "JSONTools (berlios.de)";
                sum += testJsonTools(REPS);
                break;
            case 6:
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
        for (int i = 0; i < reps; ++i) {
            // Json.org's code only accepts Strings:
            String input = new String(mData, "UTF-8");
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
        for (int i = 0; i < reps; ++i) {
            // StringTree impl only accepts Strings:
            String input = new String(mData, "UTF-8");
            ob = new JSONReader().read(input);
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
            JsonParser jp = mJsonFactory.createJsonParser(new ByteArrayInputStream(mData));
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
        JavaTypeMapper mapper = new JavaTypeMapper();
        for (int i = 0; i < reps; ++i) {
            JsonParser jp = mJsonFactory.createJsonParser(new ByteArrayInputStream(mData));
            ob = mapper.read(jp);
            jp.close();
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    protected int testJacksonJsonTypes(int reps)
        throws Exception
    {
        Object ob = null;
        JsonTypeMapper mapper = new JsonTypeMapper();
        for (int i = 0; i < reps; ++i) {
            JsonParser jp = mJsonFactory.createJsonParser(new ByteArrayInputStream(mData));
            ob = mapper.read(jp);
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

