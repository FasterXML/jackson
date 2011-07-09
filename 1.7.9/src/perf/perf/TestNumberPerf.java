package perf;

public final class TestNumberPerf
{
    final static int REPS = 45000;
    
    final static long[] NUMBERS = new long[4000];
    static {
        for (int i = 0; i < NUMBERS.length; ++i) {
            NUMBERS[i] = (i % 9);
        }
    }
    
    private TestNumberPerf() { }

    public void test()
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 3);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);
            int count;

            switch (round) {
            case 0:
                msg = "Compare";
                count = testCompare(REPS, NUMBERS);
                break;
            case 1:
                msg = "ByCast";
                count = testByCast(REPS, NUMBERS);
                break;
            case 2:
                msg = "ByShift";
                count = testByShift(REPS, NUMBERS);
                break;

            default:
                throw new Error("Internal error");
            }

            sum += count;
            
            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' ("+count+"/"+(sum & 0xFFF)+") -> "+curr+" msecs");
        }
    }

    public int testCompare(int reps, long[] numbers)
    {
        int result = 0;
        while (--reps >= 0) {
            int count = 0;
            for (int i = 0, len = numbers.length; i < len; ++i) {
                long l = numbers[i];
                if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
                    ++count;
                }
            }
            if (count > result) {
                result = count;
            }
        }
        return result;
    }

    public int testByCast(int reps, long[] numbers)
    {
        int result = 0;
        while (--reps >= 0) {
            int count = 0;
            for (int i = 0, len = numbers.length; i < len; ++i) {
                long l = numbers[i];
                int i2 = (int) l;
                long l2 = (long) i2;
                if (l2 == l) {
                    ++count;
                }
            }
            if (count > result) {
                result = count;
            }
        }
        return result;
    }

    public int testByShift(int reps, long[] numbers)
    {
        int result = 0;
        while (--reps >= 0) {
            int count = 0;
            for (int i = 0, len = numbers.length; i < len; ++i) {
                long l = numbers[i];
                long l2 = ((l << 32) >> 32);
                if (l2 == l) {
                    ++count;
                }
            }
            if (count > result) {
                result = count;
            }
        }
        return result;
    }
    
    public static void main(String[] args) throws Exception
    {
        new TestNumberPerf().test();
    }

}
