package perf;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xml.XmlFactory;
import org.codehaus.jackson.xml.XmlMapper;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

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
        REPS = 10000;
    }

    private MediaItem buildItem()
    {
        MediaItem.Content content = new MediaItem.Content();
        content.setPlayer(MediaItem.Content.Player.FLASH);
        content.setUri("http://www.cowtowncoder.com");
        content.setTitle("CowTown Blog Rulez Ok");
        content.setWidth(640);
        content.setHeight(480);
        content.setFormat("mpeg");
        content.setDuration(360000L);
        content.setSize(256000000L);
        content.setBitrate(320);
        content.setCopyright("GPL");
        content.addPerson("Cowtowncoder");
        content.addPerson("Billy-bob");

        MediaItem item = new MediaItem(content);

        item.addPhoto(new MediaItem.Photo("http://images.r.us/foobar.jpeg", "Fabulous FUBAR incident", 320, 256, MediaItem.Photo.Size.SMALL));
        item.addPhoto(new MediaItem.Photo("http://images.r.us/total-cluster.jpeg", "More of the same -- add keywords for SEO",
                640, 480, MediaItem.Photo.Size.LARGE));
        item.addPhoto(new MediaItem.Photo("http://images.r.us/barf.png", "One more angle, gotta see this", 320, 256, MediaItem.Photo.Size.SMALL));

        return item;
    }
    
    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        final MediaItem item = buildItem();
        final ObjectMapper jsonMapper = new ObjectMapper();
//        jsonMapper.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        final XmlMapper xmlMapper = new XmlMapper(new XmlFactory(null,
                new WstxInputFactory(), new WstxOutputFactory()));

        byte[] json = jsonMapper.writeValueAsBytes(item);
        System.out.println("Warmed up: data size is "+json.length+" bytes; "+REPS+" reps -> "
                +((REPS * json.length) >> 10)+" kB per iteration");
        System.out.println();
        byte[] xml = xmlMapper.writeValueAsBytes(item);
        System.out.println(" xml size: "+xml.length+" bytes");
        
        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 1);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Deserialize, JSON";
                sum += testDeser(jsonMapper, json, REPS);
                break;

            case 1:
                msg = "Deserialize, xml";
                sum += testDeser(xmlMapper, xml, REPS);
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

    protected int testDeser(ObjectMapper mapper, byte[] input, int reps)
        throws Exception
    {
        MediaItem item = null;
        for (int i = 0; i < reps; ++i) {
            item = mapper.readValue(input, 0, input.length, MediaItem.class);
        }
        return item.hashCode(); // just to get some non-optimizable number
    }


    public static void main(String[] args) throws Exception
    {
        new TestDeserPerf().test();
    }
}
