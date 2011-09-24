package perf;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

public class TestMapMessageDeserPerf
{
    private final static String JSON =
    "[{" +
    " \"successful\":true," +
    " \"id\":\"1\"," +
    " \"clientId\":\"abcdefghijklmnopqrstuvwxyz\"," +
    " \"channel\":\"/meta/connect\"," +
    " \"data\":{" +
    " \"peer\":\"bar\"," +
    " \"chat\":\"woot\"," +
    " \"user\":\"foo\"," +
    " \"room\":\"abc\"" +
    " }," +
    " \"advice\":{" +
    " \"timeout\":0" +
    " }," +
    " \"ext\":{" +
    " \"com.acme.auth\":{" +
    " \"token\":\"0123456789\"" +
    " }" +
    " }" +
    "}]";

    final static int REPS = 29999;

    final ObjectMapper mapper;

    final JavaType mapMessageType;
    
    private TestMapMessageDeserPerf()
    {
        mapper = new ObjectMapper();
        mapMessageType = mapper.constructType(MapMessage[].class);
    }
    
    private void test() throws IOException
    {
        int i = 0;
        int sum = 0;
        int round = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            ++i;
            
//            round = i % 7;
//            int round = 2 + (i % 2);
            round = 0;

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);
            MapMessage result;
            
            switch (round) {

            case 0:
                msg = "Jackson, ObjectMapper"; // byte
                result = testMapper(REPS);
                break;

            default:
                throw new Error("Internal error");
            }

            sum += result.hashCode();
            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' ("+i+") -> "+curr+" msecs"
                    +"("+(sum & 0xFF)+")"
            );
        }
    }

    private MapMessage testMapper(int reps) throws IOException
    {
        MapMessage[] msgs;
        do {
            msgs = mapper.readValue(JSON, MapMessage[].class);
//            msgs = mapper.readValue(JSON, type);
        } while (--reps > 0);
        return msgs[0];
    }
        
    public static void main(String[] args) throws Exception {
        new TestMapMessageDeserPerf().test();
    }

    /*
     * Payload class(es)
     */
    
    static class MapMessage extends HashMap<String, Object>
    {
        private static final long serialVersionUID = 1L;

        public MapMessage() { }

        public String getChannel()
        {
            return (String)get("channel");
        }

        public String getClientId()
        {
            return (String)get("clientId");
        }

        public Object getData()
        {
            return get("data");
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> getExt()
        {
            Object ext = get("ext");
            return (Map<String, Object>)ext;
        }

        public String getId()
        {
            Object id = get("id");
            return id == null ? null : String.valueOf(id);
        }

        public Map<String, Object> getExt(boolean create)
        {
            Map<String, Object> ext = getExt();
            if (create && ext == null)
            {
                ext = new HashMap<String, Object>();
                put("ext", ext);
            }
            return ext;
        }

        public boolean isSuccessful()
        {
            Boolean value = (Boolean)get("succesful");
            return value != null && value;
        }

        public void setChannel(String channel)
        {
            if (channel==null)
                remove("channel");
            else
                put("channel", channel);
        }

        public void setClientId(String clientId)
        {
            if (clientId==null)
                remove("clientId");
            else
                put("clientId", clientId);
        }

        public void setData(Object data)
        {
            if (data==null)
                remove("data");
            else
                put("data", data);
        }

        public void setId(String id)
        {
            if (id==null)
                remove("id");
            else
                put("id", id);
        }

        public void setSuccessful(boolean successful)
        {
            put("successful", successful);
        }
    }
}
