package sample;

import java.io.*;

import org.codehaus.jackson.*;

public class ParserContextSample
{
    public void run(String[] args) throws IOException
    {
        if (args.length != 1) {
            System.err.println("Usage: java "+getClass().getName());
            System.exit(1);
        }
        JsonParser jp = new JsonFactory().createJsonParser(new File(args[0]));
        JsonToken t;
        while ((t = jp.nextToken()) != null) {
            System.out.println("Token (at "+jp.getTokenLocation()+"): "+t);
            System.out.println("  path = "+getPath(jp.getParsingContext(), t));
        }
    }

    private String getPath(JsonStreamContext ctxt, JsonToken currentToken)
    {
        if (ctxt == null) {
            return "";
        }
        String desc = getPath(ctxt.getParent(), null);
        if (ctxt.inArray()) {
            if (currentToken == JsonToken.START_ARRAY) {
                desc += "[";
            } else {
                desc += "[" + ctxt.getCurrentIndex() + "]";
            }
        } else if (ctxt.inObject()) {
            // we are in object context when object starts, but don't yet have name
            if (currentToken != JsonToken.START_OBJECT) {
                // dot used unless we are at root context
                if (desc.length() > 1) {
                    desc += ".";
                }
                desc += ctxt.getCurrentName();
            }
        } else { // root... what to add?
            desc += "/";
        }
        return desc;
    }
    
    public static void main(String[] args) throws IOException {
        new ParserContextSample().run(args);
    }
}

