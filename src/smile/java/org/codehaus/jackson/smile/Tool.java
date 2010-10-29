package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.smile.SmileFactory;

/**
 * Simple command-line utility that can be used to encode JSON as Smile, or
 * decode JSON from Smile: direction is indicated by single command-line
 * option of either "-e" (encode) or "-d" (decode).
 * 
 * @author tatu
 *
 * @since 1.6.2
 */
public class Tool
{
    public final static String SUFFIX = ".lzf";

    public final JsonFactory jsonFactory;
    public final SmileFactory smileFactory;
    
    public Tool()
    {
        jsonFactory = new JsonFactory();
        smileFactory = new SmileFactory();
        // check all shared refs (-> small size); add header, not trailing marker; do not use raw binary
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        smileFactory.configure(SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT, true);
        smileFactory.configure(SmileGenerator.Feature.WRITE_HEADER, true);
        smileFactory.configure(SmileGenerator.Feature.WRITE_END_MARKER, false);
        // also: do not require header
        smileFactory.configure(SmileParser.Feature.REQUIRE_HEADER, false);
    }
    
    private void process(String[] args) throws IOException
    {
        String oper = null;
        String filename = null;

        if (args.length == 2) {
            oper = args[0];
            filename = args[1];
        } else if (args.length == 1) {
            oper = args[0];
        } else {
            showUsage();
        }
            
        boolean encode = "-e".equals(oper);
        if (encode) {
            encode(inputStream(filename));
        } else if ("-d".equals(oper)) {
            decode(inputStream(filename));
        } else {
            showUsage();
        }
    }

    private InputStream inputStream(String filename) throws IOException
    {
        // if no argument given, read from stdin
        if (filename == null) {
            return System.in;
        }
        File src = new File(filename);
        if (!src.exists()) {
            System.err.println("File '"+filename+"' does not exist.");
            System.exit(1);
        }
        return new FileInputStream(src);
    }
    
    private void decode(InputStream in) throws IOException
    {
        JsonParser jp = smileFactory.createJsonParser(in);
        JsonGenerator jg = jsonFactory.createJsonGenerator(System.out, JsonEncoding.UTF8);

        while (true) {
            /* Just one trick: since Smile can have segments (multiple 'documents' in output
             * stream), we should not stop at first end marker, only bail out if two are seen
             */
            if (jp.nextToken() == null) {
                if (jp.nextToken() == null) {
                    break;
                }
            }
            jg.copyCurrentEvent(jp);
        }
        jp.close();
        jg.close();
    }        

    private void encode(InputStream in) throws IOException
    {
        JsonParser jp = jsonFactory.createJsonParser(in);
        JsonGenerator jg = smileFactory.createJsonGenerator(System.out, JsonEncoding.UTF8);
        while ((jp.nextToken()) != null) {
            jg.copyCurrentEvent(jp);
        }
        jp.close();
        jg.close();
    }

    protected void showUsage()
    {
        System.err.println("Usage: java "+getClass().getName()+" -e/-d [file]");
        System.err.println(" (if no file given, reads from stdin -- always writes to stdout)");
        System.err.println(" -d: decode Smile encoded input as JSON");
        System.err.println(" -e: encode JSON (text) input as Smile");
        System.exit(1);        
    }

    public static void main(String[] args) throws IOException {
        new Tool().process(args);
    }

}
