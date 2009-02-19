import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.util.BufferRecycler;

public final class TestSerPerf
{
    /*
    /////////////////////////////////////////////////////
    // Bean classes
    /////////////////////////////////////////////////////
     */

    class NonFinalBean {
        final NonFinalBean2 _bean = new NonFinalBean2();

        public NonFinalBean2 getBean() { return _bean; }
        public NonFinalBean2 getBean2() { return _bean; }
        public NonFinalBean2 getBean3() { return _bean; }
        public NonFinalBean2 getBean4() { return _bean; }
    }

    class NonFinalBean2 {
        public int getX() { return 3; }
        public String getName() { return "foobar"; }
    }

    final class FinalBean {
        final FinalBean2 _bean = new FinalBean2();

        public FinalBean2 getBean() { return _bean; }
        public FinalBean2 getBean2() { return _bean; }
        public FinalBean2 getBean3() { return _bean; }
        public FinalBean2 getBean4() { return _bean; }
    }

    final class FinalBean2 extends NonFinalBean2 { }

    private final int REPS;

    private final static int TEST_PER_GC = 15;

    final Object _finalBean = new FinalBean();
    final Object _nonFinalBean = new NonFinalBean();

    public TestSerPerf()
        throws Exception
    {
        // Let's try to guestimate suitable size... to get to 50 megs processed
        REPS = 40000;
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        // Let's create tree from objects
        testObjectSer(_finalBean, 1, result);
        final JsonNode _tree = new TreeMapper().readTree(result.toByteArray());

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 3);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Jackson, object, final";
                sum += testObjectSer(_finalBean, REPS, result);
                break;
            case 1:
                msg = "Jackson, object, nonfinal";
                sum += testObjectSer(_nonFinalBean, REPS, result);
                break;
            case 2:
                msg = "Jackson, tree";
                sum += testTreeSer(_tree, REPS, result);
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


            if ((i % TEST_PER_GC) == 0) {
                System.out.println("[GC]");
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
                System.gc();
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            }
        }
    }

    protected int testObjectSer(Object value, int reps, ByteArrayOutputStream result)
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < reps; ++i) {
            result.reset();
            mapper.writeValue(result, value);
        }
        return mapper.hashCode(); // just to get some non-optimizable number
    }

    protected int testTreeSer(JsonNode value, int reps, ByteArrayOutputStream result)
        throws Exception
    {
        TreeMapper mapper = new TreeMapper();
        for (int i = 0; i < reps; ++i) {
            result.reset();
            mapper.writeTree(value, result);
        }
        return mapper.hashCode(); // just to get some non-optimizable number
    }

    public static void main(String[] args) throws Exception
    {
        new TestSerPerf().test();
    }
}
