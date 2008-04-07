package test;

import java.io.*;

import org.codehaus.jackson.*;

public class TestParser
{
    public static void main(String[] args)
        throws IOException
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... TestParser [file]");
            System.exit(1);
        }
        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(new File(args[0]));

        System.out.println("Parser: "+jp);

        JsonToken t;

        while ((t = jp.nextToken()) != null) {
            System.out.print("Token: "+t);
            if (t == JsonToken.FIELD_NAME) {
                System.out.print(", name = '"+jp.getText()+"'");
            } else if (t.toString().startsWith("VALUE")) {
                System.out.print(", value = \""+jp.getText()+"\"");
            }
            System.out.println();
        }

        jp.close();
    }
}
