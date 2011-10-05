package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.codehaus.jackson.*;

/**
 * Unit tests for verifying deserialization of Beans.
 */
public class TestObjectMapperBeanDeserializer
    extends BaseTest
{
    /*
    /////////////////////////////////////////////////
    // Helper classes
    /////////////////////////////////////////////////
     */

    @SuppressWarnings("deprecation")
    final static class CtorValueBean
        implements JsonSerializable // so we can output as simple String
    {
        final String _desc;

        public CtorValueBean(String d) { _desc = d; }
        public CtorValueBean(int value) { _desc = String.valueOf(value); }
        public CtorValueBean(long value) { _desc = String.valueOf(value); }

        @Override
        public void serialize(JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(_desc);
        }

        @Override public String toString() { return _desc; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof CtorValueBean)) return false;
            CtorValueBean other = (CtorValueBean) o;
            return _desc.equals(other._desc);
        }
    }

    final static class FactoryValueBean
    {
        final String _desc;

        protected FactoryValueBean(String desc, int dummy) { _desc = desc; }

        public static FactoryValueBean valueOf(String v) { return new FactoryValueBean(v, 0); }
        public static FactoryValueBean valueOf(int v) { return new FactoryValueBean(String.valueOf(v), 0); }
        public static FactoryValueBean valueOf(long v) { return new FactoryValueBean(String.valueOf(v), 0); }

        @Override public String toString() { return _desc; }
    }

    /**
     * Simple test bean
     */
    public final static class TestBean
    {
        int _x;
        long _y;
        String _desc;
        URL _url;
        Collection<?> _misc;

        // Explicit constructor
        public TestBean(int x, long y, String desc, URL url, Collection<?> misc)
        {
            _x = x;
            _y = y;
            _desc = desc;
            _url = url;
            _misc = misc;
        }

        // plus default one that is needed for deserialization
        public TestBean() { }

        public String getDesc() { return _desc; }
        public int getX() { return _x; }
        public long getY() { return _y; }
        public URL getURL() { return _url; }
        public Collection<?> getMisc() { return _misc; }

        public void setDesc(String value) { _desc = value; }
        public void setX(int value) { _x = value; }
        public void setY(long value) { _y = value; }
        public void setURL(URL value) { _url = value; }
        public void setMisc(Collection<?> value) { _misc = value; }

        @Override
        public boolean equals(Object o)
        {
            if (o == null || o.getClass() != getClass()) return false;
            TestBean other = (TestBean) o;
            return (other._x == _x)
                && (other._y == _y)
                && (other._desc.equals(_desc))
                && (other._url.equals(_url))
                && (other._misc.equals(_misc))
                ;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("[TestBean ");
            sb.append("x=").append(_x);
            sb.append(" y=").append(_y);
            sb.append(" desc=").append(_desc);
            sb.append(" url=").append(_url);
            sb.append(" misc=").append(_misc);
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * Another test bean, this one containing a typed list. Needed to ensure
     * that generics type information is properly accessed via mutator methods.
     * Note: List elements must be something other than what 'untyped' mapper
     * would produce from serialization.
     */
    public final static class BeanWithList
    {
        List<CtorValueBean> _beans;

        public BeanWithList() { }
        public BeanWithList(List<CtorValueBean> beans) { _beans = beans; }

        public List<CtorValueBean> getBeans() { return _beans; }

        public void setBeans(List<CtorValueBean> beans) {
            _beans = beans;
        }

        @Override
        public int hashCode() { return (_beans == null) ? -1 : _beans.size(); }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BeanWithList)) return false;
            BeanWithList other = BeanWithList.class.cast(o);
            return _beans.equals(other._beans);
        }

        @Override
            public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[Bean, list ");
            if (_beans == null) {
                sb.append("NULL");
            } else {
                sb.append('(').append(_beans.size()).append('/');
                sb.append(_beans.getClass().getName()).append(") ");
                boolean type = false;
                for (CtorValueBean bean : _beans) {
                    if (!type) {
                        sb.append("(").append(bean.getClass().getSimpleName()).append(")");
                        type = true;
                    }
                    sb.append(bean);
                    sb.append(' ');
                }
            }
            sb.append(']');
            return sb.toString();
        }
    }

    /*
    /////////////////////////////////////////////////
    // Deserialization from simple types (String, int)
    /////////////////////////////////////////////////
     */

    public void testFromStringCtor() throws Exception
    {
        CtorValueBean result = new ObjectMapper().readValue("\"abc\"", CtorValueBean.class);
        assertEquals("abc", result.toString());
    }

    public void testFromIntCtor() throws Exception
    {
        CtorValueBean result = new ObjectMapper().readValue("13", CtorValueBean.class);
        assertEquals("13", result.toString());
    }

    public void testFromLongCtor() throws Exception
    {
        // Must use something that is forced as Long...
        long value = 12345678901244L;
        CtorValueBean result = new ObjectMapper().readValue(""+value, CtorValueBean.class);
        assertEquals(""+value, result.toString());
    }

    public void testFromStringFactory() throws Exception
    {
        FactoryValueBean result = new ObjectMapper().readValue("\"abc\"", FactoryValueBean.class);
        assertEquals("abc", result.toString());
    }

    public void testFromIntFactory() throws Exception
    {
        FactoryValueBean result = new ObjectMapper().readValue("13", FactoryValueBean.class);
        assertEquals("13", result.toString());
    }

    public void testFromLongFactory() throws Exception
    {
        // Must use something that is forced as Long...
        long value = 12345678901244L;
        FactoryValueBean result = new ObjectMapper().readValue(""+value, FactoryValueBean.class);
        assertEquals(""+value, result.toString());
    }

    /*
    /////////////////////////////////////////////////
    // Deserialization from Json Object
    /////////////////////////////////////////////////
     */

    public void testSimpleBean() throws Exception
    {
        ArrayList<Object> misc = new ArrayList<Object>();
        misc.add("xyz");
        misc.add(42);
        misc.add(null);
        misc.add(Boolean.TRUE);
        TestBean bean = new TestBean(13, -900L, "\"test\"", new URL("http://foobar.com"), misc);

        // Hmmh. We probably should use serializer too... easier
        StringWriter sw = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(sw, bean);

        TestBean result = new ObjectMapper().readValue(sw.toString(), TestBean.class);
        assertEquals(bean, result);
    }

    public void testListBean() throws Exception
    {
        final int COUNT = 13;
        ArrayList<CtorValueBean> beans = new ArrayList<CtorValueBean>();
        for (int i = 0; i < COUNT; ++i) {
            beans.add(new CtorValueBean(i));
        }
        BeanWithList bean = new BeanWithList(beans);

        StringWriter sw = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(sw, bean);

        BeanWithList result = new ObjectMapper().readValue(sw.toString(), BeanWithList.class);
        assertEquals(bean, result);
    }

    /**
     * Also, let's verify that unknown fields cause an exception with default
     * settings.
     */
    public void testUnknownFields() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            TestBean bean = mapper.readValue("{ \"foobar\" : 3 }", TestBean.class);
            fail("Expected an exception, got bean: "+bean);
        } catch (JsonMappingException jse) {
            ;
        }
    }
}
