package perf;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;
import org.codehaus.jackson.type.JavaType;

/**
 * Micro-benchmark for comparing performance of bean deserialization
 */
public final class TestDeserPerf
{
    /*
    /**********************************************************
    /* Actual test
    /**********************************************************
     */

    private final int REPS;

    private TestDeserPerf() {
        // Let's try to guestimate suitable size
        REPS = 25000;
    }

    private MediaItem buildItem()
    {
        MediaItem.Content content = new MediaItem.Content();
        content.setPlayer(MediaItem.Player.JAVA);
        content.setUri("http://javaone.com/keynote.mpg");
        content.setTitle("Javaone Keynote");
        content.setWidth(640);
        content.setHeight(480);
        content.setFormat("video/mpeg4");
        content.setDuration(18000000L);
        content.setSize(58982400L);
        content.setBitrate(262144);
        content.setCopyright("None");
        content.addPerson("Bill Gates");
        content.addPerson("Steve Jobs");

        MediaItem item = new MediaItem(content);

        item.addPhoto(new MediaItem.Photo("http://javaone.com/keynote_large.jpg", "Javaone Keynote", 1024, 768, MediaItem.Size.LARGE));
        item.addPhoto(new MediaItem.Photo("http://javaone.com/keynote_small.jpg", "Javaone Keynote", 320, 240, MediaItem.Size.SMALL));

        return item;
    }
    
    public void test()
        throws Exception
    {
        int sum = 0;

        final MediaItem item = buildItem();
        JsonFactory jsonF =
//            new org.codehaus.jackson.smile.SmileFactory();
            new JsonFactory()
        ;
        
        final ObjectMapper jsonMapper = new ObjectMapper(jsonF);
//        jsonMapper.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        final SmileFactory smileFactory = new SmileFactory();
        final ObjectMapper smileMapper = new ObjectMapper(smileFactory);
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
//        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, false);

        // Use Jackson?
        byte[] json = jsonMapper.writeValueAsBytes(item);
        // or another lib?
//        byte[] json = com.alibaba.fastjson.JSON.toJSONString(item, com.alibaba.fastjson.serializer.SerializerFeature.WriteEnumUsingToString).getBytes("UTF-8");
        
        System.out.println("Warmed up: data size is "+json.length+" bytes; "+REPS+" reps -> "
                +((REPS * json.length) >> 10)+" kB per iteration");
        System.out.println();
        byte[] smile = smileMapper.writeValueAsBytes(item);
        System.out.println(" smile size: "+smile.length+" bytes");

        /*
        byte[] bson;
        final ObjectMapper bsonMapper = new ObjectMapper(new de.undercouch.bson4jackson.BsonFactory());
        {
            bson = bsonMapper.writeValueAsBytes(item);
            System.out.println(" BSON size: "+bson.length+" bytes");
        }
        */
        
        { // verify equality
            System.out.println("Will verify state of Smile...");
            MediaItem result = smileMapper.readValue(smile, 0, smile.length, MediaItem.class);
            String jsonFromSmile = jsonMapper.writeValueAsString(result);
            String jsonFromItem = jsonMapper.writeValueAsString(item);
            if (!jsonFromSmile.equals(jsonFromItem)) {
                int ix = 0;

                for (int max = Math.min(jsonFromSmile.length(), jsonFromItem.length()); ix < max; ++ix) {
                    if (jsonFromSmile.charAt(ix) != jsonFromItem.charAt(ix)) {
                        break;
                    }
                }
                
                System.err.println("Source JSON: ");
                System.err.println(jsonFromItem);
                System.err.println("------------");
                System.err.println("Smile  JSON: ");
                System.err.println(jsonFromSmile);
                System.err.println("------------");                
                for (int i = 0; i < ix; ++i) {
                    System.err.print('=');
                }
                System.err.println("^");
                System.err.println("------------");                
                throw new Error("No smile today -- data corruption!");
            }
            System.out.println("Verification successful: Smile ok!");
        }

// for debugging:
 System.err.println("JSON = "+jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(item));
        
        int round = 0;
        while (true) {
//            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
//            int round = 2;

            long curr = System.currentTimeMillis();
            String msg;
            round = (++round % 4);

//if (true) round = 3; 
if (round < 2) round += 2;
            
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Deserialize, manual, JSON";
                sum += testDeser(jsonMapper.getJsonFactory(), json, REPS);
                break;

            case 1:
                msg = "Deserialize, manual/FAST, JSON";
                sum += testDeserFaster(jsonMapper.getJsonFactory(), json, REPS);
                break;
                
                /*
            case 2:
                msg = "Deserialize, bind, JSON";
                sum += testDeser(jsonMapper, json, REPS);
                break;

            case 2:
                msg = "Deserialize, smile";
                sum += testDeser(smileMapper, smile, REPS * 2);
                break;
                */

            case 2:
                msg = "Deserialize, manual, Smile";
                sum += testDeser(smileMapper.getJsonFactory(), smile, REPS);
                break;

            case 3:
                msg = "Deserialize, manual/FAST, Smile";
                sum += testDeserFaster(smileMapper.getJsonFactory(), smile, REPS);
                break;
                
                /*
            case 2:
                msg = "Deserialize, fast-json";
                sum += testFastJson(json, REPS);
                break;
                */
                
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

    protected int testDeser(ObjectMapper mapper, byte[] input, int reps)
        throws Exception
    {
        JavaType type = TypeFactory.defaultInstance().constructType(MediaItem.class);
        MediaItem item = null;
        for (int i = 0; i < reps; ++i) {
            item = mapper.readValue(input, 0, input.length, type);
        }
        return item.hashCode(); // just to get some non-optimizable number
    }

    protected int testDeser(JsonFactory jf, byte[] input, int reps)
        throws Exception
    {
        MediaItem item = null;
        for (int i = 0; i < reps; ++i) {
            JsonParser jp = jf.createJsonParser(input);
            item = MediaItem.deserialize(jp);
            jp.close();
        }
        return item.hashCode(); // just to get some non-optimizable number
    }

    protected int testDeserFaster(JsonFactory jf, byte[] input, int reps)
            throws Exception
        {
            MediaItem item = null;
            for (int i = 0; i < reps; ++i) {
                JsonParser jp = jf.createJsonParser(input);
                item = MediaItem.deserializeFaster(jp);
                jp.close();
            }
            return item.hashCode(); // just to get some non-optimizable number
        }
    
    /*
    protected int testFastJson(byte[] input, int reps)
        throws Exception
    {
        MediaItem item = null;
        for (int i = 0; i < reps; ++i) {
            item = com.alibaba.fastjson.JSON.parseObject(input, MediaItem.class);
        }
        return item.hashCode(); // just to get some non-optimizable number
    }
    */
    
    public static void main(String[] args) throws Exception
    {
        new TestDeserPerf().test();
    }
}
