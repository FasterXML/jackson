package perf;
import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;
import org.codehaus.jackson.util.TokenBuffer;

public final class TestCopyPerf
{
    private final int REPS;

    final JsonFactory _jsonFactory;
    
    final SmileFactory _smileFactory;

    final TokenBuffer _tokens;

    private TestCopyPerf(File f)
        throws Exception
    {
        _jsonFactory = new JsonFactory();
        _smileFactory = new SmileFactory();
        // whether to use back-refs for field names has measurable impact on ser/deser (but different direction):
	//        _smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, false);
        _smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
        _smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        FileInputStream fis = new FileInputStream(f);
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = _jsonFactory.createJsonParser(fis);
        _tokens = mapper.readValue(jp, TokenBuffer.class);

        // Let's try to guestimate suitable size... to get about 20 megs copied
        REPS = (int) ((double) (20 * 1000 * 1000) / (double) f.length());
        
        jp.close();
    }

    public void test()
        throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(2000);
        testCopy(1, bos, 0);
        System.out.println("Output length: "+bos.size()+"; will do "+REPS+" writes per round");
        System.out.println();

        int round = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            testCopy(REPS, bos, round);
            ++round;
        }
    }

    private long testCopy(int reps, ByteArrayOutputStream bos, int round)
        throws IOException
    {
        int mode = (round % 2);
        if (mode == 0) System.out.println();

        JsonGenerator jg = null;
        final long start = System.currentTimeMillis();
        
        while (--reps >= 0) {
            bos.reset();
            switch (mode) {
            case 0:
                jg = _smileFactory.createJsonGenerator(bos);
                break;
            case 1:
                jg = _jsonFactory.createJsonGenerator(bos, JsonEncoding.UTF8);                
                break;
            default:
                throw new Error();
            }
            JsonParser jp = _tokens.asParser();
            while (jp.nextToken() != null) {
                jg.copyCurrentEvent(jp);
            }
            jp.close();
            jg.close();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println("Took "+time+" msecs (round "+round+"; bytes "+bos.size()+") for "+jg.getClass().getName());
        return time;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... <file>");
            System.exit(1);
        }
        new TestCopyPerf(new File(args[0])).test();
    }
}
