import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.util.TokenBuffer;
import org.codehaus.jackson.map.ObjectMapper;

public final class TestCopyPerf
{
    private final int REPS;

    final JsonFactory _jsonFactory;

    final TokenBuffer _tokens;

    private TestCopyPerf(File f)
        throws Exception
    {
        _jsonFactory = new JsonFactory();
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

            long curr = System.currentTimeMillis();
            String result = testCopy(REPS, bos, round);
            curr = System.currentTimeMillis() - curr;
            System.out.println("Took "+curr+" msecs (round "+round+", "+result+").");
            ++round;
        }
    }

    private String testCopy(int reps, ByteArrayOutputStream bos, int round)
        throws IOException
    {
        JsonGenerator jg = null;
        
        boolean realUTF8 = (round & 1) == 0;
        
        while (--reps >= 0) {
            bos.reset();
            if (realUTF8) {
                jg = _jsonFactory.createJsonGenerator(bos, JsonEncoding.UTF8);                
            } else {
                jg = _jsonFactory.createJsonGenerator(bos, JsonEncoding.UTF16_BE);
            }
            JsonParser jp = _tokens.asParser();
            while (jp.nextToken() != null) {
                jg.copyCurrentEvent(jp);
            }
            jp.close();
            jg.close();
        }
        return jg.getClass().getName();
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
