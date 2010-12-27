package perf;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerator;

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
