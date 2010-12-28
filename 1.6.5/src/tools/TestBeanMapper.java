

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestBeanMapper
{
    private TestBeanMapper() { }

    private void test() throws Exception
    {
        JsonFactory f = new JsonFactory();
        ObjectMapper jmap = new ObjectMapper();
        Object foo = new MyObject(5, -90, "Desc");
        StringWriter sw = new StringWriter();
        JsonGenerator jg = f.createJsonGenerator(sw);
        try {
            jmap.writeValue(jg, foo);
        } catch (Exception e) {
            try { jg.flush(); } catch (IOException ioe) { }
            System.err.println("Error, intermediate result = |"+sw+"|");
            throw e;
        }
        jg.close();

        System.out.println("Write result: <"+sw.toString()+">");
    }

    public static void main(String[] args) throws Exception
    {
        new TestBeanMapper().test();
    }

    @SuppressWarnings("unused")
    private final class MyObject
    {
        final int _x, _y;

        final String _desc;

        public MyObject(int x, int y, String desc)
        {
            _x = x;
            _y = y;
            _desc = desc;
        }

        public String getDesc() { return _desc; }
        public int getX() { return _x; }
        public int getY() { return _y; }

        public int[] getFoobar() { return new int[] { 1, 2, 3 }; }

        public MyObject2 getNext() {
            return new MyObject2();
        }

        public Collection<?> getMisc() {
            return new ArrayList<Object>();
        }
    }

    final static class MyObject2 {
        public String getName() { return "dummy"; }
    }
}

