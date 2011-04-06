package perf;
import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public final class TestSerPerf
{
    /*
    /**********************************************************
    /* Bean classes
    /**********************************************************
     */

    static class NonFinalBean {
        final NonFinalBean2 _bean = new NonFinalBean2();

        public NonFinalBean2 getBean() { return _bean; }
        public NonFinalBean2 getBean2() { return _bean; }
        public NonFinalBean2 getBean3() { return _bean; }
        public NonFinalBean2 getBean4() { return _bean; }
    }

    static class NonFinalBean2 {
        public int getX() { return 3; }
        public String getName() { return "foobar"; }
    }

    final static class FinalBean {
        final FinalBean2 _bean = new FinalBean2();

        public FinalBean2 getBean() { return _bean; }
        public FinalBean2 getBean2() { return _bean; }
        public FinalBean2 getBean3() { return _bean; }
        public FinalBean2 getBean4() { return _bean; }
    }

    final static class FinalBean2 {
        public int getX() { return 3; }
        public String getName() { return "foobar"; }
    }

    final static class FinalFieldBean {
        private final FinalFieldBean2 _bean = new FinalFieldBean2();

        public FinalFieldBean2 fieldBean = _bean;
        public FinalFieldBean2 fieldBean2 = _bean;
        public FinalFieldBean2 fieldBean3 = _bean;
        public FinalFieldBean2 fieldBean4 = _bean;
    }

    final static class FinalFieldBean2 {
        public int x = 3;
        public String name = "foobar";
    }

    private final int REPS;
    private final ObjectMapper _mapper;

    final Object _finalBean = new FinalBean();
    final Object _finalFieldBean = new FinalFieldBean();
    final Object _nonFinalBean = new NonFinalBean();

    private TestSerPerf()
        throws Exception
    {
        _mapper = new ObjectMapper();
        // Let's try to guestimate suitable size... to get to 50 megs processed
        REPS = 10000;
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        // Let's create tree from objects
        testObjectSer(_finalBean, 1, result);
        final JsonNode _tree = _mapper.readTree(new JsonFactory().createJsonParser(result.toByteArray()));

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 3);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Jackson, object+GET, final";
                sum += testObjectSer(_finalBean, REPS, result);
                break;
            case 1:
                msg = "Jackson, object+Field, final";
                sum += testObjectSer(_finalFieldBean, REPS, result);
                break;
            case 2:
                msg = "Jackson, tree";
                sum += testTreeSer(_tree, REPS, result);
                break;

                /*
            case 4:
                msg = "Jackson, object, nonfinal";
                sum += testObjectSer(_nonFinalBean, REPS, result);
                break;
                */


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

    protected int testObjectSer(Object value, int reps, ByteArrayOutputStream result)
        throws Exception
    {
        for (int i = 0; i < reps; ++i) {
            result.reset();
            _mapper.writeValue(result, value);
        }
        return _mapper.hashCode(); // just to get some non-optimizable number
    }

    protected int testTreeSer(JsonNode root, int reps, ByteArrayOutputStream result)
        throws Exception
    {
        for (int i = 0; i < reps; ++i) {
            result.reset();
            _mapper.writeValue(result, root);
        }
        return _mapper.hashCode(); // just to get some non-optimizable number
    }

    public static void main(String[] args) throws Exception
    {
        new TestSerPerf().test();
    }
}
