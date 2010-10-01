package perf;
import java.io.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Micro-benchmark for comparing performance of bean deserialization
 * using method-based method or Creator-based (constructor or
 * factory method)
 */
public final class TestCreatorPerf
{
    /*
    /////////////////////////////////////////////////////
    // Bean classes
    /////////////////////////////////////////////////////
     */

    final static class MethodBean
    {
        int x;
        long y;
        boolean state;

        protected MethodBean() { }

        public void setX(int v) { x = v; }
        public void setY(long v) { y = v; }
        public void setState(boolean v) { state = v; }

        @Override
        public int hashCode() { return x ^ (int) y ^ (state ? 1 : -1); }
    }

    final static class ConstructorBean
    {
        final int x;
        final long y;
        final boolean state;

        @JsonCreator public ConstructorBean(@JsonProperty("x") int a1,
                                            @JsonProperty("y") long a2,
                                            @JsonProperty("state") boolean a4)
        {
            x = a1;
            y = a2;
            state = a4;
        }

        @Override
        public int hashCode() { return x ^ (int) y ^ (state ? 1 : -1); }
    }

    final static class FactoryBean
    {
        int x;
        long y;
        boolean state;

        private FactoryBean() { }

        @JsonCreator public static FactoryBean buildIt(@JsonProperty("x") int a1,
                                                       @JsonProperty("y") long a2,
                                                       @JsonProperty("state") boolean a4)
        {
            FactoryBean bean = new FactoryBean();
            bean.x = a1;
            bean.y = a2;
            bean.state = a4;
            return bean;
        }

        @Override
        public int hashCode() { return x ^ (int) y ^ (state ? 1 : -1); }
    }


    private final int REPS;
    private final ObjectMapper _mapper;
    private final byte[] _data;
    
    public TestCreatorPerf()
        throws Exception
    {
        // Let's try to guestimate suitable size, to spend enough (but not too much) time per round
        REPS = 13000;
        _mapper = new ObjectMapper();
        _data = "{ \"x\" : -15980, \"y\" : 1234567890123, \"state\" : true }".getBytes("UTF-8");
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
                msg = "Jackson, setters";
                sum += testDeser(REPS, in, MethodBean.class);
                break;
            case 1:
                msg = "Jackson, constructor";
                sum += testDeser(REPS, in, ConstructorBean.class);
                break;
            case 2:
                msg = "Jackson, factory";
                sum += testDeser(REPS, in, FactoryBean.class);
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
        new TestCreatorPerf().test();
    }
}
