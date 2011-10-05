package perf;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.util.BufferRecycler;

// json.org's reference implementation
import org.json.*;

// jackson-4-org.json
import com.fasterxml.jackson.module.jsonorg.JsonOrgModule;

// json-smart
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

@SuppressWarnings("unused")
public final class TestJsonPerf
{
    private final int REPS;

    private final static int TEST_PER_GC = 15;

    final JsonFactory _jsonFactory;
    
    final ObjectMapper _mapper;

    final ObjectMapper _smileMapper;
    
    final SmileFactory _smileFactory;
    
    final byte[] _jsonData;

    final String _jsonString;
    
    final byte[] _smileData;
    
    protected int _batchSize;

    public TestJsonPerf(File f) throws IOException
    {
        _jsonFactory = new JsonFactory();
        _mapper = new ObjectMapper(_jsonFactory);
        _mapper.registerModule(new JsonOrgModule());
        
        _smileFactory = new SmileFactory();
        _smileMapper = new ObjectMapper(_smileFactory);
        _jsonData = readData(f);
        _jsonString = new String(_jsonData, "UTF-8");
        _smileData = convertToSmile(_jsonData);

        // Let's try to guestimate suitable size... to get to 25 megs parsed
        REPS = (int) ((double) (25 * 1000 * 1000) / (double) _jsonData.length);

        System.out.println("Read "+_jsonData.length+" bytes (smile: "+_smileData.length+") from '"+f+"'; will do "+REPS+" reps");
        System.out.println();
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;
        int round = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            ++i;
            
//            round = i % 7;
//            int round = 2 + (i % 2);
            round = 6;

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Jackson, stream/byte[]"; // byte
                sum += testJacksonStream(REPS, _jsonFactory, _jsonData);
                break;

            case 1:
                msg = "Jackson, stream/String"; // String
                sum += testJacksonStream(REPS, _jsonFactory, _jsonString);
                break;
                
            case 2:
                msg = "Jackson, tree/byte[]";
                sum += testJacksonTree(REPS, _mapper, _jsonData);
                break;

            case 3:
                msg = "Jackson, tree/String";
                sum += testJacksonTree(REPS, _mapper, _jsonString);
                break;

            case 4:
                msg = "Jackson, Object/byte[]";
                sum += testJacksonObject(REPS, _mapper, _jsonData);
                break;

            case 5:
                msg = "Jackson, Object/String";
                sum += testJacksonObject(REPS, _mapper, _jsonString);
                break;
                
            case 6:
                msg = "Json-smart/String";
                sum += testJsonSmart(REPS, _jsonData, _jsonString);
                break;
                
                /*
            case 3:
                msg = "Jackson + module-org-json";
                sum += testJsonOrgViaJackson(REPS);
                break;

            case 4:
                msg = "Json.org";
                sum += testJsonOrg(REPS);
                break;

            case 0:
                msg = "Jackson/smile, stream";
                sum += testJacksonStream(REPS, _smileFactory, _smileData, true);
                break;
            case 0:
                msg = "Smile/data-bind";
                sum += testJacksonDatabind(_smileMapper, _smileData, REPS);
                break;

            case 1:
                msg = "Jackson/data-bind";
                sum += testJacksonDatabind(_mapper, _jsonData, REPS);
                break;
            case 1:
                msg = "Jackson, stream/byte";
                sum += testJacksonStream(REPS, _jsonFactory, _jsonData, true);
                break;
            case 2:
                msg = "Jackson, stream/char";
                sum += testJacksonStream(REPS, _jsonFactory, _jsonData, false);
                break;
                */
            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs"
//                    +"("+(sum & 0xFF)+")"
                    		);


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

    private byte[] convertToSmile(byte[] json) throws IOException
    {
    	JsonParser jp = _jsonFactory.createJsonParser(json);
    	ByteArrayOutputStream out = new ByteArrayOutputStream(200);
        System.out.println("Converting and verifying Smile data...");
    	JsonGenerator jg = _smileFactory.createJsonGenerator(out);
    	while (jp.nextToken() != null) {
    	    jg.copyCurrentEvent(jp);
    	}
    	jp.close();
    	jg.close();
    	byte[] smileBytes = out.toByteArray();
    	System.out.println("Written as "+smileBytes.length+" Smile bytes from "+json.length+" JSON bytes; will verify correctness");

    	// One more thing: let's actually verify correctness!
    	JsonParser sp = _smileFactory.createJsonParser(new ByteArrayInputStream(smileBytes));
        jp = _jsonFactory.createJsonParser(json);
        while (true) {
            JsonToken t1 = jp.nextToken();
            JsonToken t2;
            try {
                t2 = sp.nextToken();
            } catch (IOException ioe) {
                System.err.println("WARN: problem for token matching input token "+t1+" at "+jp.getCurrentLocation());
                throw ioe;
            }
            if (t1 != t2) {
                throw new IllegalArgumentException("Error: tokens differ (json: "+t1+", smile "+t2+") at "+jp.getCurrentLocation());
            }
            if (t1 == null) break;
            if (t1.isScalarValue() || t1 == JsonToken.FIELD_NAME) {
                String str1 = jp.getText();
                String str2 = jp.getText();
                if (str1 == null) {
                    throw new IllegalArgumentException("Error: token texts differ (json: null, smile '"+str2+"') at "+jp.getCurrentLocation());                    
                } else if (!str1.equals(str2)) {
                    throw new IllegalArgumentException("Error: token texts differ (json: '"+str1+"', smile '"+str2+"') at "+jp.getCurrentLocation());                    
                }
            }
        }
        System.out.println("Verified Smile data ("+smileBytes.length+"): same as JSON ("+json.length+")");
    	return smileBytes;
    }
    
    protected int testJsonOrg(int reps)
        throws Exception
    {
        Object ob = null;
        // Json.org's code only accepts Strings:
        String input = new String(_jsonData, "UTF-8");
        for (int i = 0; i < reps; ++i) {
            JSONTokener tok = new JSONTokener(input);
            ob = tok.nextValue();
        }
        return ob.hashCode();
    }

    protected int testJsonOrgViaJackson(int reps) throws Exception
    {
        JSONObject ob = null;
        for (int i = 0; i < reps; ++i) {
            ob = _mapper.readValue(_jsonData, JSONObject.class);
        }
        return ob.hashCode();
    }

    private int testJacksonStream(int reps, JsonFactory factory, byte[] data) throws Exception
    {
        int sum = 0;
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            JsonParser jp = factory.createJsonParser(data, 0, data.length);
            ob = mapStreamToObject(jp, jp.nextToken());
            jp.close();
        }
        return ob.hashCode();
    }

    private int testJacksonStream(int reps, JsonFactory factory, String data) throws Exception
    {
        int sum = 0;
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            JsonParser jp = factory.createJsonParser(data);
            ob = mapStreamToObject(jp, jp.nextToken());
            jp.close();
        }
        return ob.hashCode();
    }

    private Object mapStreamToObject(JsonParser jp, JsonToken t) throws IOException
    {
        switch (t) {
        case VALUE_FALSE:
            return Boolean.FALSE;
        case VALUE_TRUE:
            return Boolean.TRUE;
        case VALUE_STRING:
            return jp.getText();
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
            return jp.getNumberValue();
        case VALUE_EMBEDDED_OBJECT:
            return jp.getEmbeddedObject();
        case START_ARRAY:
            {
                ArrayList<Object> kids = new ArrayList<Object>(4);
                while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
                    kids.add(mapStreamToObject(jp, t));
                }
                return kids;
            }
        case START_OBJECT:
            {
//                HashMap<String, Object> kids = new LinkedHashMap<String, Object>();
                HashMap<String, Object> kids = new HashMap<String, Object>(8);
                while ((t = jp.nextToken()) != JsonToken.END_OBJECT) {
                    kids.put(jp.getCurrentName(), mapStreamToObject(jp, jp.nextToken()));
                }
                return kids;
            }
        default:
            throw new RuntimeException("Unexpected token: "+t);
        }
    }
    
    private int testJacksonDatabind(int reps, ObjectMapper mapper, byte[] data)
        throws Exception
    {
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            // This is "untyped"... Maps, Lists etc
            ob = mapper.readValue(data, Object.class);
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    private int testJacksonTree(int reps, ObjectMapper mapper, byte[] data)
        throws Exception
    {
      Object ob = null;
      for (int i = 0; i < reps; ++i) {
        ob = mapper.readValue(data, JsonNode.class);
          /*
          ByteArrayInputStream in = new ByteArrayInputStream(data);
          ob = mapper.readValue(in, JsonNode.class);
          */
      }
      return ob.hashCode(); // just to get some non-optimizable number
    }

    private int testJacksonTree(int reps, ObjectMapper mapper, String data)
        throws Exception
    {
        JsonNode tree = null;
//        Object ob = null;
        for (int i = 0; i < reps; ++i) {
//            ob = mapper.readValue(data, JsonNode.class);
            tree = mapper.readTree(data);
        }
        return tree.hashCode(); // just to get some non-optimizable number
    }

    private int testJacksonObject(int reps, ObjectMapper mapper, byte[] data)
        throws Exception
    {
      Object ob = null;
      for (int i = 0; i < reps; ++i) {
        ob = mapper.readValue(data, Object.class);
      }
      return ob.hashCode(); // just to get some non-optimizable number
    }
    
    private int testJacksonObject(int reps, ObjectMapper mapper, String data)
        throws Exception
    {
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            ob = mapper.readValue(data, Object.class);
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }
    
    private int testJsonSmart(int reps, byte[] dataBytes, String dataString)
        throws Exception
    {
        JSONObject root = null;
        final Charset utf8 = Charset.forName("UTF-8");
        for (int i = 0; i < reps; ++i) {
    //        ob = mapper.readValue(data, JsonNode.class);
            String str = new String(dataBytes, utf8);
            root = (JSONObject) JSONValue.parse(str);
        }
        return root.hashCode();
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

