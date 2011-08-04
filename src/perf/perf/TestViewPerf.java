package perf;
import java.io.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonView;

public final class TestViewPerf
{
    /*
    /////////////////////////////////////////////////////
    // Bean classes
    /////////////////////////////////////////////////////
     */

    static class ViewA { }
    static class ViewB { }

    static class ViewBean
    {
        final Bean2 _bean = new Bean2();

        @JsonView({ViewA.class, ViewB.class})
        public Bean2 getBean() { return _bean; }

        @JsonView({ViewB.class, ViewA.class})
        public String getBean2() { return "abc-def"; }

        @JsonView({ViewB.class, ViewA.class})
        public int getBean3() { return 218985; }

        @JsonView({ViewA.class, ViewB.class})
        public Bean2 getBean4() { return _bean; }

        @JsonView({ViewA.class})
        public String getText2() { return "foobar"; }

        @JsonView({ViewA.class})
        public String getText3() { return ".......\n"; }
    }

    static class NonViewBean
    {
        final Bean2 _bean = new Bean2();

        public Bean2 getBean() { return _bean; }
        public String getBean2() { return "abc-def"; }
        public int getBean3() { return 218985; }
        public Bean2 getBean4() { return _bean; }
        public String getText2() { return "foobar"; }
        public String getText3() { return ".......\n"; }
    }

    static class Bean2 {
        public int getX() { return 3; }
        public String getName() { return "foobar"; }
    }

    private final int REPS;
    private final ObjectMapper _mapper;

    private TestViewPerf()
        throws Exception
    {
        _mapper = new ObjectMapper();
        REPS = 2400;
    }

    public void test()
        throws Exception
    {
        int i = 0;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        final ViewBean viewBean = new ViewBean();
        final NonViewBean nonViewBean = new NonViewBean();

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 3);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);
            int result;

            switch (round) {

            case 0:
                msg = "With view";
                result = testViewSer(viewBean, REPS, out, ViewA.class);
                break;
            case 1:
                msg = "WithOUT view";
                result = testViewSer(viewBean, REPS, out, null);
                break;
            case 2:
                msg = "NO view";
                result = testSer(nonViewBean, REPS, out, null);
                break;

            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +result+").");
        }
    }

    protected int testViewSer(Object value, int reps, ByteArrayOutputStream result, Class<?> view)
        throws Exception
    {
    	ObjectWriter w = _mapper.writerWithView(view);
        for (int i = 0; i < reps; ++i) {
            result.reset();
            w.writeValue(result, value);
            w.writeValue(result, value);
            w.writeValue(result, value);
        }
        return result.size();
    }

    protected int testSer(Object value, int reps, ByteArrayOutputStream result, Class<?> dummyView)
        throws Exception
    {
        for (int i = 0; i < reps; ++i) {
            result.reset();
            _mapper.writeValue(result, value);
            _mapper.writeValue(result, value);
            _mapper.writeValue(result, value);
        }
        return result.size();
    }

    public static void main(String[] args) throws Exception
    {
        new TestViewPerf().test();
    }
}
