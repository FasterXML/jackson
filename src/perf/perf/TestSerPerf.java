package perf;

import java.io.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xml.XmlFactory;
import org.codehaus.jackson.xml.XmlMapper;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

public final class TestSerPerf
{
    /*
    /**********************************************************
    /* Actual test
    /**********************************************************
     */

    private final int REPS;

    private TestSerPerf()
        throws Exception
    {
        // Let's try to guestimate suitable size... to get to 50 megs processed
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

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        final MediaItem item = buildItem();
        final ObjectMapper jsonMapper = new ObjectMapper();
        final XmlMapper xmlMapper = new XmlMapper(new XmlFactory(null,
                new WstxInputFactory(), new WstxOutputFactory()));

        // Verify that we can roundtrip
        {
            byte[] stuff = jsonMapper.writeValueAsBytes(item);
            MediaItem back = jsonMapper.readValue(stuff, 0, stuff.length, MediaItem.class);
            System.out.println("Warmed up: data size is "+stuff.length+" bytes; "+REPS+" reps -> "
                    +((REPS * stuff.length) >> 10)+" kB per iteration");
            stuff = xmlMapper.writeValueAsBytes(item);
            System.out.println(" xml size: "+stuff.length+" bytes");
        }
        
        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 2);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Serialize, JSON";
                sum += testObjectSer(jsonMapper, item, REPS, result);
                break;

            case 1:
                msg = "Serialize, xml";
                sum += testObjectSer(xmlMapper, item, REPS, result);
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

    protected int testObjectSer(ObjectMapper mapper, Object value, int reps, ByteArrayOutputStream result)
        throws Exception
    {
        for (int i = 0; i < reps; ++i) {
            result.reset();
            mapper.writeValue(result, value);
        }
        return result.size(); // just to get some non-optimizable number
    }

    public static void main(String[] args) throws Exception
    {
        new TestSerPerf().test();
    }
}
