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
        } else if ("-v".equals(oper)) {
            // need to read twice (encode, verify/compare)
            verify(inputStream(filename), inputStream(filename));
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

    private void verify(InputStream in, InputStream in2) throws IOException
    {
        JsonParser jp = jsonFactory.createJsonParser(in);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(4000);
        JsonGenerator jg = smileFactory.createJsonGenerator(bytes, JsonEncoding.UTF8);

        // First, read, encode in memory buffer
        while ((jp.nextToken()) != null) {
            jg.copyCurrentEvent(jp);
        }
        jp.close();
        jg.close();

        // and then re-read both, verify
        jp = jsonFactory.createJsonParser(in2);
        byte[] smile = bytes.toByteArray();
        JsonParser jp2 = smileFactory.createJsonParser(smile);

        JsonToken t;
        int count = 0;
        while ((t = jp.nextToken()) != null) {
            JsonToken t2 = jp2.nextToken();
            ++count;
            if (t != t2) {
                throw new IOException("Input and encoded differ, token #"+count+"; expected "+t+", got "+t2);
            }
            // also, need to have same texts...
            String text1 = jp.getText();
            String text2 = jp2.getText();
            if (!text1.equals(text2)) {
                throw new IOException("Input and encoded differ, token #"+count+"; expected text '"+text1+"', got '"+text2+"'");
            }
        }

        System.out.println("OK: verified "+count+" tokens (from "+smile.length+" bytes of Smile encoded data), input and encoded contents are identical");
    }
    
    protected void showUsage()
    {
        System.err.println("Usage: java "+getClass().getName()+" -e/-d [file]");
        System.err.println(" (if no file given, reads from stdin -- always writes to stdout)");
        System.err.println(" -d: decode Smile encoded input as JSON");
        System.err.println(" -e: encode JSON (text) input as Smile");
        System.err.println(" -v: encode JSON (text) input as Smile; read back, verify, do not write out");
        System.exit(1);        
    }

    public static void main(String[] args) throws IOException {
        new Tool().process(args);
    }

}
