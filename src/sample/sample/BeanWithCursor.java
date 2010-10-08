package sample;
// no package, i.e. at root of sample/extra

import java.io.*;

import org.codehaus.jackson.*;

public class BeanWithCursor
{
    // // Helper bean

    public static class TwitterEntry
    {
        long _id;  
        String _text;
        int _fromUserId, _toUserId;
        String _languageCode;
        
        public TwitterEntry() { }
        
        public void setId(long id) { _id = id; }
        public void setText(String text) { _text = text; }
        public void setFromUserId(int id) { _fromUserId = id; }
        public void setToUserId(int id) { _toUserId = id; }
        public void setLanguageCode(String languageCode) { _languageCode = languageCode; }
        
        public long getId() { return _id; }
        public String getText() { return _text; }
        public int getFromUserId() { return _fromUserId; }
        public int getToUserId() { return _toUserId; }
        public String getLanguageCode() { return _languageCode; }
        
        @Override
        public String toString() {
            return "[Tweet, id: "+_id+", text='"+_text+"', from: "+_fromUserId+", to: "+_toUserId+", lang: "+_languageCode+"]";
        }
    }

    // // Main test code

    public BeanWithCursor() { }

    private TwitterEntry read(JsonParser jp)
        throws IOException
    {
        // First: verify that we got "Json Object":
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected data to start with an Object");
        }
        TwitterEntry result = new TwitterEntry();
        // Iterate over object fields:
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            // Let's move to value
            jp.nextToken();
            if (fieldName.equals("id")) {
                result.setId(jp.getLongValue());
            } else if (fieldName.equals("text")) {
                result.setText(jp.getText());
            } else if (fieldName.equals("fromUserId")) {
                result.setFromUserId(jp.getIntValue());
            } else if (fieldName.equals("toUserId")) {
                result.setToUserId(jp.getIntValue());
            } else if (fieldName.equals("languageCode")) {
                result.setLanguageCode(jp.getText());
            } else {
                // ignore, or signal error
                throw new IOException("Unrecognized field '"+fieldName+"'");
            }
        }
        jp.close(); // important to close both parser and underlying File reader
        return result;
    }
    
    private void write(JsonGenerator jg, TwitterEntry entry)
        throws IOException
    {
        jg.writeStartObject();
        // can either do "jg.writeFieldName(...) + jg.writeNumber()", or this:
        jg.writeNumberField("id", entry.getId());
        jg.writeStringField("text", entry.getText());
        jg.writeNumberField("fromUserId", entry.getFromUserId());
        jg.writeNumberField("toUserId", entry.getToUserId());
        jg.writeStringField("langugeCode", entry.getLanguageCode());
        jg.writeEndObject();
        jg.close();
    }

    private void process(File input)
        throws IOException
    {
        JsonFactory jsonF = new JsonFactory();
        JsonParser jp = jsonF.createJsonParser(input);
        TwitterEntry entry = read(jp);

        // let's write to a file, using UTF-8 encoding (only sensible one)
        StringWriter strw = new StringWriter();
        JsonGenerator jg = jsonF.createJsonGenerator(strw);
        jg.useDefaultPrettyPrinter(); // enable indentation just to make debug/testing easier

        // Here we would modify it... for now, will just (re)indent it

        write(jg, entry);

        System.out.println("Result = ["+strw.toString()+"]");
    }

    public static void main(String[] args)
        throws IOException
    {
        if (args.length != 1) {
            System.err.println("Usage: java BeanWithCursor [input-file]");
            System.exit(1);
        }
        File f = new File(args[0]);
        new BeanWithCursor().process(f);
    }
}
