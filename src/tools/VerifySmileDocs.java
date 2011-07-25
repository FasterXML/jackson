import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;

/**
 * Helper test class that will check to see that we can round-trip
 * JSON-to-Smile-to-JSON, without loss of data.
 */
public class VerifySmileDocs
{
    private final static JsonFactory jsonFactory = new JsonFactory();
    private final static SmileFactory smileFactoryWithNoBackrefs = new SmileFactory();
    {
        smileFactoryWithNoBackrefs.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, false);
        smileFactoryWithNoBackrefs.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, false);
    }
    private final static SmileFactory jsonFactoryWithBackrefs = new SmileFactory();
    {
        jsonFactoryWithBackrefs.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
        jsonFactoryWithBackrefs.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
    }
    
    private VerifySmileDocs() {}
    
    private void verifyAll(File inputDir) throws IOException
    {
        File[] files = inputDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                int i = name.lastIndexOf('.');
                if (i > 0) {
                    String suffix = name.substring(i+1);
                    if ("json".equals(suffix) || "jsn".equals(suffix)) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (files.length == 0) {
            throw new IOException("No files with suffix '.json' or '.jsn' found from under '" +
            		inputDir.getAbsolutePath()+"'");
        }
        System.out.printf("Found %d JSON files to test with...\n", files.length);
        int failures = 0;
        
        for (File file : files) {
            System.out.printf(" verifying file '%s'...", file.getName());
            if (!verify(file, false) || !verify(file, true)) {
                ++failures;
            } else {
                System.out.println(" OK");
            }
        }

        System.out.printf("Done: %d files had issues\n", failures);
    }

    private boolean verify(File inputFile, boolean backrefs) throws IOException
    {
        JsonFactory smileFactory = backrefs ? jsonFactoryWithBackrefs : smileFactoryWithNoBackrefs;
        byte[] smileDoc = convertToSmile(inputFile, smileFactory);
        JsonParser jpWithDiff = compareJsonToSmile(inputFile, smileDoc, smileFactory);
        if (jpWithDiff != null) {
            System.err.printf(" events differ (expected %s), location (backrefs: %s): %s\n",
                    String.valueOf(jpWithDiff.getCurrentToken()),
                    backrefs,
                    inputFile.getName());
            jpWithDiff.close();
            return false;
        }
        return true;
    }

    private byte[] convertToSmile(File inputFile, JsonFactory outputFactory) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        JsonParser jp = jsonFactory.createJsonParser(inputFile);
        JsonGenerator jgen = outputFactory.createJsonGenerator(bytes);
        while (jp.nextToken() != null) {
            jgen.copyCurrentEvent(jp);
        }
        jp.close();
        jgen.close();
        return bytes.toByteArray();
    }

    private JsonParser compareJsonToSmile(File inputFile, byte[] smileBytes,
            JsonFactory smileFactory) throws IOException
    {
        JsonParser jsonParser = jsonFactory.createJsonParser(inputFile);
        JsonParser smileParser = smileFactory.createJsonParser(smileBytes);
        JsonToken t1;

        while ((t1 = jsonParser.nextToken()) != null) {
            JsonToken t2 = smileParser.nextToken();
            // first: token types must match
            if (t1 != t2) {
                return jsonParser;
            }
            // and second, values as well
            switch (t1) {
            case VALUE_STRING:
            case FIELD_NAME:
                if (!jsonParser.getText().equals(smileParser.getText())) {
                    return jsonParser;
                }
                break;
            case VALUE_NUMBER_INT:
                if (jsonParser.getLongValue() != smileParser.getLongValue()) {
                    return jsonParser;
                }
                break;
            case VALUE_NUMBER_FLOAT:
                if (jsonParser.getDoubleValue() != smileParser.getDoubleValue()) {
                    return jsonParser;
                }
                break;
            // others are fine:
            // Boolean values are distinct tokens;
            // Object/Array start/end likewise
            }
        }
        if (t1 == null) {
            if (smileParser.nextToken() != null) {
                return jsonParser;
            }
        }
        jsonParser.close();
        smileParser.close();
        return null;
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... [input-dir]");
            System.exit(1);
        }
        new VerifySmileDocs().verifyAll(new File(args[0]));
    }
}
