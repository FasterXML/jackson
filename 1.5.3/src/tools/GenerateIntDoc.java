

import java.io.*;
import java.util.Random;

import org.codehaus.jackson.*;

public class GenerateIntDoc
{
    final static int AVG_ARRAY_LEN = 32;

    private GenerateIntDoc() { }

    private void generate(OutputStream out, int kbSize)
        throws IOException
    {
        int bsize = kbSize * 1000;

        // Let's first buffer in memory, to know exact length
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bsize + 500);
        Random r = new Random(kbSize);
        JsonGenerator gen = new JsonFactory().createJsonGenerator(bos, JsonEncoding.UTF8);
        gen.writeStartArray(); // outermost array
        gen.writeStartArray(); // inner array

        do {
            // First, do we close current, start new array?
            if (r.nextInt(AVG_ARRAY_LEN) == 3) { // to get average array length of 16
                gen.writeEndArray();
                if (r.nextBoolean()) {
                    gen.writeRaw("\n");
                }
                gen.writeStartArray();
                gen.flush();
            }
            // Then need to calculate number to output
            int nr = r.nextInt(32);
            if (r.nextBoolean()) {
                nr *= r.nextInt(256); // up to 8k
                if (r.nextBoolean()) {
                    nr *= r.nextInt(0x20000); // up to 1G
                }
            }
            if (r.nextBoolean()) {
                nr = -nr;
            }
            gen.writeNumber(nr);
        } while (bos.size() < bsize);

        gen.writeEndArray();
        gen.writeEndArray();
        gen.writeRaw("\n");
        gen.close();

        bos.writeTo(out);
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java test.GenerateIntDoc <size-in-kbytes>");
            System.exit(1);
        }
        new GenerateIntDoc().generate(System.out, Integer.parseInt(args[0]));
        System.out.flush();
    }
}

