import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public final class TestDeserPerf
{
    /*
    /////////////////////////////////////////////////////
    // Bean classes
    /////////////////////////////////////////////////////
     */

    static class Bean {
        Bean2 bean, bean2, bean3, bean4;

        public void setBean(Bean2 v) { bean = v; }
        public void setBean2(Bean2 v) { bean2 = v; }
        public void setBean3(Bean2 v) { bean3 = v; }
        public void setBean4(Bean2 v) { bean4 = v; }
    }

    static class Bean2 {
        int x;
        String name;

        public void setX(int v) { x = v; }
        public void setName(String v) { name = v; }
    }

    final static class FieldBean {
        public FieldBean2 bean, bean2, bean3, bean4;
    }

    final static class FieldBean2 {
        public int x;
        public String name;
    }

    private final int REPS;
    private final ObjectMapper _mapper;
    private final byte[] _data;

    public TestDeserPerf()
        throws Exception
    {
        // Let's try to guestimate suitable size, to spend enough (but not too much) time per round
        REPS = 60000;
        _mapper = new ObjectMapper();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        _mapper.writeValue(result, new FieldBean());
        _data = result.toByteArray();
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        System.out.println("START: content size "+_data.length+" bytes");
        ByteArrayInputStream in = new ByteArrayInputStream(_data);

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 3);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Jackson, object+SET";
                sum += testDeser(REPS, in, Bean.class);
                break;
            case 1:
                msg = "Jackson, object+Field";
                sum += testDeser(REPS, in, FieldBean.class);
                break;
            case 2:
                msg = "Jackson, tree";
                sum += testDeser(REPS, in, JsonNode.class);
                break;
            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");

        }
    }

    protected <T> int testDeser(int reps, ByteArrayInputStream in, Class<T> beanType)
        throws Exception
    {
        T result = null;
        for (int i = 0; i < reps; ++i) {
            in.reset();
            result = _mapper.readValue(in, beanType);
        }
        return result.hashCode(); // just to get some non-optimizable number
    }

    public static void main(String[] args) throws Exception
    {
        new TestDeserPerf().test();
    }
}
