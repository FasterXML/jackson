

import java.io.*;
import java.util.Random;

import org.codehaus.jackson.*;

public class GenerateDoubleDoc
{
    final static int AVG_ARRAY_LEN = 32;

    private GenerateDoubleDoc() { }

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
            float f;

            do {
                f = Float.intBitsToFloat(r.nextInt());
            } while (Double.isNaN(f) || Double.isInfinite(f));
            gen.writeNumber(f);
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
            System.err.println("Usage: java test.GenerateDoubleDoc <size-in-kbytes>");
            System.exit(1);
        }
        new GenerateDoubleDoc().generate(System.out, Integer.parseInt(args[0]));
        System.out.flush();
    }
}

