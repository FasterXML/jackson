package test;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestIndenter
{
    public static void main(String[] args)
        throws IOException
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... TestIndenter [file]");
            System.exit(1);
        }
        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(new File(args[0]));
        JsonNode jn = new JsonTypeMapper().read(jp);

        StringWriter sw = new StringWriter(200);
        JsonGenerator jg = f.createJsonGenerator(sw);

        jg.useDefaultPrettyPrinter();

        jn.writeTo(jg);
        jg.close();

        System.out.println("DOC-><");
        System.out.println(sw.toString());
        System.out.println(">");
    }
}
