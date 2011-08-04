package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonView;

/**
 * Unit tests for verifying JSON view functionality: ability to declaratively
 * suppress subset of properties from being serialized.
 */
public class TestViews
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    // Classes that represent views
    static class ViewA { }
    static class ViewAA extends ViewA { }
    static class ViewB { }
    static class ViewBB extends ViewB { }
    
    static class Bean
    {
        @JsonView(ViewA.class)
        public String a = "1";

        @JsonView({ViewAA.class, ViewB.class})
        public String aa = "2";

        @JsonView(ViewB.class)
        public String getB() { return "3"; }
    }

    /**
     * Bean with mix of explicitly annotated
     * properties, and implicit ones that may or may
     * not be included in views.
     */
    static class MixedBean
    {
        @JsonView(ViewA.class)
        public String a = "1";

        public String getB() { return "2"; }
    }

    /**
     * As indicated by [JACKSON-261], @JsonView should imply
     * that associated element (method, field) is to be considered
     * a property
     */
    static class ImplicitBean {
    	@SuppressWarnings("unused")
		@JsonView(ViewA.class)
    	private int a = 1;
    }

    static class VisibilityBean {
        @JsonProperty protected String id = "id";
    
        @JsonView(ViewA.class)
        public String value = "x";
    }   
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */    
    
    @SuppressWarnings("unchecked")
    public void testSimple() throws IOException
    {
        StringWriter sw = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        // Ok, first, using no view whatsoever; all 3
        Bean bean = new Bean();
        Map<String,Object> map = writeAndMap(mapper, bean);
        assertEquals(3, map.size());

        // Then with "ViewA", just one property
        sw = new StringWriter();
        mapper.writerWithView(ViewA.class).writeValue(sw, bean);
        map = mapper.readValue(sw.toString(), Map.class);
        assertEquals(1, map.size());
        assertEquals("1", map.get("a"));

        // "ViewAA", 2 properties
        sw = new StringWriter();
        mapper.writerWithView(ViewAA.class).writeValue(sw, bean);
        map = mapper.readValue(sw.toString(), Map.class);
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("aa"));

        // "ViewB", 2 prop2
        String json = mapper.writerWithView(ViewB.class).writeValueAsString(bean);
        map = mapper.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals("2", map.get("aa"));
        assertEquals("3", map.get("b"));

        // and "ViewBB", 2 as well
        json = mapper.writerWithView(ViewBB.class).writeValueAsString(bean);
        map = mapper.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals("2", map.get("aa"));
        assertEquals("3", map.get("b"));
    }

    /**
     * Unit test to verify implementation of [JACKSON-232], to
     * allow "opt-in" handling for JSON Views: that is, that
     * default for properties is to exclude unless included in
     * a view.
     */
    @SuppressWarnings("unchecked")
    public void testDefaultExclusion() throws IOException
    {
        MixedBean bean = new MixedBean();
        StringWriter sw = new StringWriter();

        ObjectMapper mapper = new ObjectMapper();
        // default setting: both fields will get included
        mapper.writerWithView(ViewA.class).writeValue(sw, bean);
        Map<String,Object> map = mapper.readValue(sw.toString(), Map.class);
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));

        // but can also change (but not necessarily on the fly...)
        mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION, false);
        // with this setting, only explicit inclusions count:
        String json = mapper.writerWithView(ViewA.class).writeValueAsString(bean);
        map = mapper.readValue(json, Map.class);
        assertEquals(1, map.size());
        assertEquals("1", map.get("a"));
        assertNull(map.get("b"));
    }

    /**
     * As per [JACKSON-261], @JsonView annotation should imply that associated
     * method/field does indicate a property.
     */
    public void testImplicitAutoDetection() throws Exception
    {
    	assertEquals("{\"a\":1}", serializeAsString(new ImplicitBean()));
    }

    public void testVisibility() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        VisibilityBean bean = new VisibilityBean();
        // Without view setting, should only see "id"
        String json = mapper.writerWithView(Object.class).writeValueAsString(bean);
        //json = mapper.writeValueAsString(bean);
        assertEquals("{\"id\":\"id\"}", json);
    }
}
