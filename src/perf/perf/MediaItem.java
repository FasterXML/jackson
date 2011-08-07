package perf;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.io.SerializedString;

/**
 * Value class for performance tests
 */
@JsonPropertyOrder({"content", "images"})
public class MediaItem
{
    final static SerializedString NAME_IMAGES = new SerializedString("images");
    final static SerializedString NAME_CONTENT = new SerializedString("content");
    
    public enum Player { JAVA, FLASH;  }
    public enum Size { SMALL, LARGE; }

    private List<Photo> _photos;
    private Content _content;

    public MediaItem() { }

    public MediaItem(Content c)
    {
        _content = c;
    }

    public void addPhoto(Photo p) {
        if (_photos == null) {
            _photos = new ArrayList<Photo>();
        }
        _photos.add(p);
    }
    
    public List<Photo> getImages() { return _photos; }
    public void setImages(List<Photo> p) { _photos = p; }

    public Content getContent() { return _content; }
    public void setContent(Content c) { _content = c; }

    // Custom deser
    public static MediaItem deserialize(JsonParser jp) throws IOException
    {
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Need START_OBJECT for MediaItem");
        }
        MediaItem item = new MediaItem();
        while (jp.nextToken() == JsonToken.FIELD_NAME) {
            String name = jp.getCurrentName();
            if (name == "images") {
                item._photos = deserializeImages(jp);
            } else if (name == "content") {
                item._content = Content.deserialize(jp);
            } else throw new IOException("Unknown field");
        }
        if (jp.getCurrentToken() != JsonToken.END_OBJECT) {
            throw new IOException("Need END_OBJECT to complete MediaItem");
        }
        return item;
    }
    
    private final static void failName(JsonParser jp, SerializableString str) throws IOException {
        throw new IOException("Expected FIELD_NAME '"+str+"'; got "+jp.getCurrentToken()+" (name '"+jp.getCurrentName()+"')");
    }
    
    /* Alternate deserializer that relies on ordering (just for
     * test purposes -- would not work for real-world use cases)
     */
    public static MediaItem deserializeFaster(JsonParser jp) throws IOException
    {
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Need START_OBJECT for MediaItem");
        }
        MediaItem item = new MediaItem();
        if (!jp.nextFieldName(NAME_CONTENT)) {
            failName(jp, NAME_CONTENT);
        }
        item._content = Content.deserializeFaster(jp);
        if (!jp.nextFieldName(NAME_IMAGES)) {
            failName(jp, NAME_IMAGES);
        }
        item._photos = deserializeImagesFaster(jp);
        if (jp.nextToken() != JsonToken.END_OBJECT) {
            throw new IOException("Need END_OBJECT to complete MediaItem");
        }
        return item;
    }
    
    private static List<Photo> deserializeImages(JsonParser jp) throws IOException
    {
        if (jp.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Need START_ARRAY for List of Photos");
        }
        ArrayList<Photo> images = new ArrayList<Photo>(4);
        while (jp.nextToken() == JsonToken.START_OBJECT) {
            images.add(Photo.deserialize(jp));
        }
        if (jp.getCurrentToken() != JsonToken.END_ARRAY) {
            throw new IOException("Need END_ARRAY to complete List of Photos");
        }
        return images;
    }

    private static List<Photo> deserializeImagesFaster(JsonParser jp) throws IOException
    {
        if (jp.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Need START_ARRAY for List of Photos");
        }
        ArrayList<Photo> images = new ArrayList<Photo>(4);
        while (jp.nextToken() == JsonToken.START_OBJECT) {
            images.add(Photo.deserializeFaster(jp));
        }
        if (jp.getCurrentToken() != JsonToken.END_ARRAY) {
            throw new IOException("Need END_ARRAY to complete List of Photos");
        }
        return images;
    }
    
    // Custom serializer
    public void serialize(JsonGenerator jgen) throws IOException
    {
        jgen.writeStartObject();

        jgen.writeFieldName("content");
        if (_content == null) {
            jgen.writeNull();
        } else {
            _content.serialize(jgen);
        }
        if (_photos == null) {
            jgen.writeNullField("images");
        } else {
            jgen.writeArrayFieldStart("images");
            for (Photo photo : _photos) {
                photo.serialize(jgen);
            }
            jgen.writeEndArray();
        }

        jgen.writeEndObject();
    }
    
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */
    
    @JsonPropertyOrder({"uri","title","width","height","size"})
    public static class Photo
    {
        public final static int F_URI = 1;
        public final static int F_TITLE = 2;
        public final static int F_WIDTH = 3;
        public final static int F_HEIGHT = 4;
        public final static int F_SIZE = 5;
        
        public final static HashMap<String,Integer> sFields = new HashMap<String,Integer>();
        static {
            // MediaItem fields
            sFields.put("uri", F_URI);
            sFields.put("title", F_TITLE);
            sFields.put("width", F_WIDTH);
            sFields.put("height", F_HEIGHT);
            sFields.put("size", F_SIZE);
        }

        final static SerializedString NAME_URI = new SerializedString("uri");
        final static SerializedString NAME_TITLE = new SerializedString("title");
        final static SerializedString NAME_WIDTH = new SerializedString("width");
        final static SerializedString NAME_HEIGHT = new SerializedString("height");
        final static SerializedString NAME_SIZE = new SerializedString("size");
        
      private String _uri;
      private String _title;
      private int _width;
      private int _height;
      private Size _size;
    
      public Photo() {}
      public Photo(String uri, String title, int w, int h, Size s)
      {
          _uri = uri;
          _title = title;
          _width = w;
          _height = h;
          _size = s;
      }
    
      public String getUri() { return _uri; }
      public String getTitle() { return _title; }
      public int getWidth() { return _width; }
      public int getHeight() { return _height; }
      public Size getSize() { return _size; }
    
      public void setUri(String u) { _uri = u; }
      public void setTitle(String t) { _title = t; }
      public void setWidth(int w) { _width = w; }
      public void setHeight(int h) { _height = h; }
      public void setSize(Size s) { _size = s; }

      private static Size findSize(String id)
      {
          if (id.charAt(0) == 'L') {
              if ("LARGE".equals(id)) {
                  return Size.LARGE;
              }
          } else if ("SMALL".equals(id)) {
              return Size.SMALL;
          }
          throw new IllegalArgumentException();
      }
      
      public static Photo deserialize(JsonParser jp) throws IOException
      {
          Photo photo = new Photo();
          while (jp.nextToken() == JsonToken.FIELD_NAME) {
              String name = jp.getCurrentName();
              jp.nextToken();
              Integer I = sFields.get(name);
              if (I != null) {
                  switch (I.intValue()) {
                  case F_URI:
                      photo.setUri(jp.getText());
                      continue;
                  case F_TITLE:
                      photo.setTitle(jp.getText());
                      continue;
                  case F_WIDTH:
                      photo.setWidth(jp.getIntValue());
                      continue;
                  case F_HEIGHT:
                      photo.setHeight(jp.getIntValue());
                      continue;
                  case F_SIZE:
                      photo.setSize(findSize(jp.getText()));
                      continue;
                  }
              }
              throw new IOException("Unknown field '"+name+"'");
          }
          if (jp.getCurrentToken() != JsonToken.END_OBJECT) {
              throw new IOException("Need END_OBJECT to complete Photo");
          }
          return photo;
      }

      public static Photo deserializeFaster(JsonParser jp) throws IOException
      {
          Photo photo = new Photo();

          if (!jp.nextFieldName(NAME_URI)) {
              failName(jp, NAME_URI);
          }
          photo.setUri(jp.nextTextValue());

          if (!jp.nextFieldName(NAME_TITLE)) {
              failName(jp, NAME_TITLE);
          }
          photo.setTitle(jp.nextTextValue());

          if (!jp.nextFieldName(NAME_WIDTH)) {
              failName(jp, NAME_WIDTH);
          }
          photo.setWidth(jp.nextIntValue(-1));
          
          if (!jp.nextFieldName(NAME_HEIGHT)) {
              failName(jp, NAME_HEIGHT);
          }
          photo.setHeight(jp.nextIntValue(-1));

          if (!jp.nextFieldName(NAME_SIZE)) {
              failName(jp, NAME_SIZE);
          }
          photo.setSize(findSize(jp.nextTextValue()));

          if (jp.nextToken() != JsonToken.END_OBJECT) {
              throw new IOException("Need END_OBJECT to complete Photo");
          }

          return photo;
      }
      
      public void serialize(JsonGenerator jgen) throws IOException
      {
          jgen.writeStartObject();
          jgen.writeStringField("uri", _uri);
          jgen.writeStringField("title", _title);
          jgen.writeNumberField("width", _width);
          jgen.writeNumberField("height", _height);
          jgen.writeStringField("size", (_size == null) ? null : _size.name());
          jgen.writeEndObject();
      }          
    }

    @JsonPropertyOrder({"player","uri","title","width","height","format","duration","size","bitrate","persons","copyright"})
    public static class Content
    {
        public final static int F_PLAYER = 0;
        public final static int F_URI = 1;
        public final static int F_TITLE = 2;
        public final static int F_WIDTH = 3;
        public final static int F_HEIGHT = 4;
        public final static int F_FORMAT = 5;
        public final static int F_DURATION = 6;
        public final static int F_SIZE = 7;
        public final static int F_BITRATE = 8;
        public final static int F_PERSONS = 9;
        public final static int F_COPYRIGHT = 10;
        
        public final static HashMap<String,Integer> sFields = new HashMap<String,Integer>();
        static {
            sFields.put("player", F_PLAYER);
            sFields.put("uri", F_URI);
            sFields.put("title", F_TITLE);
            sFields.put("width", F_WIDTH);
            sFields.put("height", F_HEIGHT);
            sFields.put("format", F_FORMAT);
            sFields.put("duration", F_DURATION);
            sFields.put("size", F_SIZE);
            sFields.put("bitrate", F_BITRATE);
            sFields.put("persons", F_PERSONS);
            sFields.put("copyright", F_COPYRIGHT);
        }

        final static SerializedString NAME_PLAYER = new SerializedString("player");
        final static SerializedString NAME_URI = new SerializedString("uri");
        final static SerializedString NAME_TITLE = new SerializedString("title");
        final static SerializedString NAME_WIDTH = new SerializedString("width");
        final static SerializedString NAME_HEIGHT = new SerializedString("height");
        final static SerializedString NAME_FORMAT = new SerializedString("format");
        final static SerializedString NAME_DURATION = new SerializedString("duration");
        final static SerializedString NAME_SIZE = new SerializedString("size");
        final static SerializedString NAME_BITRATE = new SerializedString("bitrate");
        final static SerializedString NAME_PERSONS = new SerializedString("persons");
        final static SerializedString NAME_COPYRIGHT = new SerializedString("copyright");
        
        private Player _player;
        private String _uri;
        private String _title;
        private int _width;
        private int _height;
        private String _format;
        private long _duration;
        private long _size;
        private int _bitrate;
        private List<String> _persons;
        private String _copyright;
    
        public Content() { }

        public void addPerson(String p) {
            if (_persons == null) {
                _persons = new ArrayList<String>();
            }
            _persons.add(p);
        }
        
        public Player getPlayer() { return _player; }
        public String getUri() { return _uri; }
        public String getTitle() { return _title; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }
        public String getFormat() { return _format; }
        public long getDuration() { return _duration; }
        public long getSize() { return _size; }
        public int getBitrate() { return _bitrate; }
        public List<String> getPersons() { return _persons; }
        public String getCopyright() { return _copyright; }
    
        public void setPlayer(Player p) { _player = p; }
        public void setUri(String u) {  _uri = u; }
        public void setTitle(String t) {  _title = t; }
        public void setWidth(int w) {  _width = w; }
        public void setHeight(int h) {  _height = h; }
        public void setFormat(String f) {  _format = f;  }
        public void setDuration(long d) {  _duration = d; }
        public void setSize(long s) {  _size = s; }
        public void setBitrate(int b) {  _bitrate = b; }
        public void setPersons(List<String> p) {  _persons = p; }
        public void setCopyright(String c) {  _copyright = c; }

        private static Player findPlayer(String id)
        {
            if ("JAVA".equals(id)) {
                return Player.JAVA;
            }
            if ("FLASH".equals(id)) {
                return Player.FLASH;
            }
            throw new IllegalArgumentException("Weird Player value of '"+id+"'");
        }
        
        public static Content deserialize(JsonParser jp) throws IOException
        {
            if (jp.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Need START_OBJECT for Content");
            }
            Content content = new Content();

            while (jp.nextToken() == JsonToken.FIELD_NAME) {
                String name = jp.getCurrentName();
                jp.nextToken();
                Integer I = sFields.get(name);
                if (I != null) {
                    switch (I.intValue()) {
                    case F_PLAYER:
                        content.setPlayer(findPlayer(jp.getText()));
                    case F_URI:
                        content.setUri(jp.getText());
                        continue;
                    case F_TITLE:
                        content.setTitle(jp.getText());
                        continue;
                    case F_WIDTH:
                        content.setWidth(jp.getIntValue());
                        continue;
                    case F_HEIGHT:
                        content.setHeight(jp.getIntValue());
                        continue;
                    case F_FORMAT:
                        content.setCopyright(jp.getText());
                        continue;
                    case F_DURATION:
                        content.setDuration(jp.getLongValue());
                        continue;
                    case F_SIZE:
                        content.setSize(jp.getLongValue());
                        continue;
                    case F_BITRATE:
                        content.setBitrate(jp.getIntValue());
                        continue;
                    case F_PERSONS:
                        content.setPersons(deserializePersons(jp));
                        continue;
                    case F_COPYRIGHT:
                        content.setCopyright(jp.getText());
                        continue;
                    }
                }
                throw new IOException("Unknown field '"+name+"'");
            }
            if (jp.getCurrentToken() != JsonToken.END_OBJECT) {
                throw new IOException("Need END_OBJECT to complete Content");
            }
            return content;
        }

        public static Content deserializeFaster(JsonParser jp) throws IOException
        {
            if (jp.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Need START_OBJECT for Content");
            }
            Content content = new Content();
            if (!jp.nextFieldName(NAME_PLAYER)) {
                failName(jp, NAME_PLAYER);
            }
            content.setPlayer(findPlayer(jp.nextTextValue()));

            if (!jp.nextFieldName(NAME_URI)) {
                failName(jp, NAME_URI);
            }
            content.setUri(jp.nextTextValue());
            
            if (!jp.nextFieldName(NAME_TITLE)) {
                failName(jp, NAME_TITLE);
            }
            content.setTitle(jp.nextTextValue());

            if (!jp.nextFieldName(NAME_WIDTH)) {
                failName(jp, NAME_WIDTH);
            }
            content.setWidth(jp.nextIntValue(-1));

            if (!jp.nextFieldName(NAME_HEIGHT)) {
                failName(jp, NAME_HEIGHT);
            }
            content.setHeight(jp.nextIntValue(-1));

            if (!jp.nextFieldName(NAME_FORMAT)) {
                failName(jp, NAME_PLAYER);
            }
            content.setCopyright(jp.nextTextValue());

            if (!jp.nextFieldName(NAME_DURATION)) {
                failName(jp, NAME_DURATION);
            }
            content.setDuration(jp.nextLongValue(-1L));

            if (!jp.nextFieldName(NAME_SIZE)) {
                failName(jp, NAME_SIZE);
            }
            content.setSize(jp.nextLongValue(-1L));
            
            if (!jp.nextFieldName(NAME_BITRATE)) {
                failName(jp, NAME_BITRATE);
            }
            content.setBitrate(jp.nextIntValue(-1));

            if (!jp.nextFieldName(NAME_PERSONS)) {
                failName(jp, NAME_PERSONS);
            }
            jp.nextToken();
            content.setPersons(deserializePersonsFaster(jp));

            if (!jp.nextFieldName(NAME_COPYRIGHT)) {
                failName(jp, NAME_COPYRIGHT);
            }
            content.setCopyright(jp.nextTextValue());

            if (jp.nextToken() != JsonToken.END_OBJECT) {
                throw new IOException("Need END_OBJECT to complete Content");
            }
            
            return content;
        }
        
        private static List<String> deserializePersons(JsonParser jp) throws IOException
        {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw new IOException("Need START_ARRAY for List of Persons (got "+jp.getCurrentToken()+")");
            }
            ArrayList<String> persons = new ArrayList<String>(4);
            while (jp.nextToken() == JsonToken.VALUE_STRING) {
                persons.add(jp.getText());
            }
            if (jp.getCurrentToken() != JsonToken.END_ARRAY) {
                throw new IOException("Need END_ARRAY to complete List of Persons");
            }
            return persons;
        }

        private static List<String> deserializePersonsFaster(JsonParser jp) throws IOException
        {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw new IOException("Need START_ARRAY for List of Persons (got "+jp.getCurrentToken()+")");
            }
            ArrayList<String> persons = new ArrayList<String>(4);
            String str;
            while ((str = jp.nextTextValue()) != null) {
                persons.add(str);
            }
            if (jp.getCurrentToken() != JsonToken.END_ARRAY) {
                throw new IOException("Need END_ARRAY to complete List of Persons");
            }
            return persons;
        }
        
        public void serialize(JsonGenerator jgen) throws IOException
        {
            jgen.writeStartObject();
            jgen.writeStringField("player", (_player == null) ? null : _player.name());
            jgen.writeStringField("uri", _uri);
            jgen.writeStringField("title", _title);
            jgen.writeNumberField("width", _width);
            jgen.writeNumberField("height", _height);
            jgen.writeStringField("format", _format);
            jgen.writeNumberField("duration", _duration);
            jgen.writeNumberField("size", _size);
            jgen.writeNumberField("bitrate", _bitrate);
            jgen.writeStringField("copyright", _copyright);
            if (_persons == null) {
                jgen.writeNullField("persons");
            } else {
                jgen.writeArrayFieldStart("persons");
                for (String p : _persons) {
                    jgen.writeString(p);
                }
                jgen.writeEndArray();
            }
            jgen.writeEndObject();
        }          
    }
}
