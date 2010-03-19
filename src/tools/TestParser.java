

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
        // [JACKSON-259]: ability to suppress canonicalization
        f.disable(JsonParser.Feature.CANONICALIZE_FIELD_NAMES);

        JsonParser jp = f.createJsonParser(new File(args[0]));
        //JsonParser jp = f.createJsonParser(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
        jp.enable(JsonParser.Feature.ALLOW_COMMENTS);

        System.out.println("Parser: "+jp);

        JsonToken t;

        while ((t = jp.nextToken()) != null) {
            System.out.print("Token: "+t);
            if (t == JsonToken.FIELD_NAME) {
                //String name = new String(jp.getTextCharacters(), jp.getTextOffset(), jp.getTextLength());
                String name = jp.getCurrentName();
                System.out.print(", name = '"+name+"' (len: "+name.length()+")");
                // Troubleshoot:
                int ix = name.indexOf('\0');
                if (ix >= 0) {
                    throw new RuntimeException("Null byte for name, at index #"+ix);
                }
            } else if (t.toString().startsWith("VALUE")) {
                if (t == JsonToken.VALUE_STRING) {
                    System.out.print(" [len: "+jp.getTextLength()+"]");
                }
                System.out.print(", value = \""+jp.getText()+"\"");
            }
            System.out.println();
        }

        jp.close();
    }
}
