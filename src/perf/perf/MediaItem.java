package perf;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;

/**
 * Value class for performance tests
 */
public class MediaItem
{
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
    
    // Custom serializer
    public void serialize(JsonGenerator jgen) throws IOException
    {
        jgen.writeStartObject();

        if (_photos == null) {
            jgen.writeNullField("images");
        } else {
            jgen.writeArrayFieldStart("images");
            for (Photo photo : _photos) {
                photo.serialize(jgen);
            }
            jgen.writeEndArray();
        }
        jgen.writeFieldName("content");
        if (_content == null) {
            jgen.writeNull();
        } else {
            _content.serialize(jgen);
        }

        jgen.writeEndObject();
    }
    
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */
    
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
        
        public enum Size { SMALL, LARGE; }
    
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
                      photo.setSize(Size.valueOf(jp.getText()));
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

        public enum Player { JAVA, FLASH; }
    
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
                        content.setPlayer(Player.valueOf(jp.getText()));
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
        
        public void serialize(JsonGenerator jgen) throws IOException
        {
            jgen.writeStartObject();
            jgen.writeStringField("play", (_player == null) ? null : _player.name());
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
