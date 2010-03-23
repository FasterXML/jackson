import java.io.*;
import java.util.*;

import org.codehaus.jackson.map.*;

public class TestSerializationError
{
    final static class OkBean {
        ArrayList<NonBean[]> _beans;

        public OkBean() {
            _beans = new ArrayList<NonBean[]>();
            _beans.add(new NonBean[] { new NonBean() });;
        }

        public List<NonBean[]> getNonBeans() { return _beans; }
    }

    final static class NonBean {
        public NonBean() { }
    }

    public static void main(String[] args)
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<OkBean> list = new ArrayList<OkBean>();
        list.add(new OkBean());
        HashMap<String,Object> map = new HashMap<String,Object>();
        map.put("foo", list);
        // should error out, but what do we see there?
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, map);

        System.out.println("Odd, didn't fail, got: "+sw.toString());
    }
}
