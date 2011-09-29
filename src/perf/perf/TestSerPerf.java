package perf;

import java.io.*;

//import de.undercouch.bson4jackson.BsonFactory;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.smile.*;

public final class TestSerPerf
{
    /*
    /**********************************************************
    /* Actual test
    /**********************************************************
     */

    private final int REPS;

    private TestSerPerf() throws Exception
    {
        // Let's try to guestimate suitable size...
        REPS = 20000;
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
        int i = 0;
        int sum = 0;

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        final MediaItem item = buildItem();
        final JsonFactory jsonF =
            new JsonFactory()
//            new org.codehaus.jackson.smile.SmileFactory();
        ;
//        ((SmileFactory) jsonF).configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, false);
            
        final ObjectMapper jsonMapper = new ObjectMapper(jsonF);

//      jsonMapper.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);

        final SmileFactory smileFactory = new SmileFactory();
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, false);
        final ObjectMapper smileMapper = new ObjectMapper(smileFactory);

        // Verify that we can round trip
        {
            byte[] stuff = jsonMapper.writeValueAsBytes(item);
            @SuppressWarnings("unused")
            MediaItem back = jsonMapper.readValue(stuff, 0, stuff.length, MediaItem.class);
            System.out.println("Warmed up: data size is "+stuff.length+" bytes; "+REPS+" reps -> "
                    +((REPS * stuff.length) >> 10)+" kB per iteration");
            System.out.println();
            stuff = smileMapper.writeValueAsBytes(item);
            System.out.println(" Smile size: "+stuff.length+" bytes");
        }

        JsonNode root = jsonMapper.valueToTree(item);
        
        while (true) {
//            Thread.sleep(150L);
            ++i;
            int round = (i % 3);

            // override?
//            round = 4;

            long curr = System.currentTimeMillis();
            String msg;

            switch (round) {

            case 0:
                msg = "Serialize, JSON";
                sum += testObjectSer(jsonMapper, item, REPS+REPS, result);
                break;

            case 1:
                msg = "Serialize, JSON/manual";
                sum += testObjectSer(jsonMapper.getJsonFactory(), item, REPS+REPS, result);
                break;

            case 2:
                msg = "Serialize, JsonNode";
                sum += testNodeSer(jsonMapper, root, REPS+REPS, result);
//                sum += testNodeSer(smileMapper, root, REPS+REPS, result);
                break;

                /*
            case 3:
                msg = "Serialize, Smile";
                sum += testObjectSer(smileMapper, item, REPS, result);
                break;

            case 4:
                msg = "Serialize, Smile/manual";
                sum += testObjectSer(smileFactory, item, REPS+REPS, result);
                break;
*/
                

            /*
             case 4:
                msg = "Serialize, BSON";
                sum += testObjectSer(new ObjectMapper(new BsonFactory()), item, REPS, result);
                break;
                */

            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (round == 0) {  System.out.println(); }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("+(sum & 0xFF)+").");
            if ((i & 0x1F) == 0) { // GC every 64 rounds
                System.out.println("[GC]");
                Thread.sleep(20L);
                System.gc();
                Thread.sleep(20L);
            }
        }
    }

    protected int testObjectSer(ObjectMapper mapper, Object value, int reps, ByteArrayOutputStream result)
        throws Exception
    {
        for (int i = 0; i < reps; ++i) {
            result.reset();
            mapper.writeValue(result, value);
        }
        return result.size(); // just to get some non-optimizable number
    }

    protected int testNodeSer(ObjectMapper mapper, JsonNode value, int reps, ByteArrayOutputStream result)
            throws Exception
        {
            for (int i = 0; i < reps; ++i) {
                result.reset();
                mapper.writeValue(result, value);
            }
            return result.size(); // just to get some non-optimizable number
        }
    
    protected int testObjectSer(JsonFactory jf, MediaItem value, int reps, ByteArrayOutputStream result)
        throws Exception
    {
        for (int i = 0; i < reps; ++i) {
            result.reset();
            JsonGenerator jgen = jf.createJsonGenerator(result, JsonEncoding.UTF8);
            value.serialize(jgen);
            jgen.close();
        }
        return result.size(); // just to get some non-optimizable number
    }
    
    public static void main(String[] args) throws Exception
    {
        new TestSerPerf().test();
    }
}
