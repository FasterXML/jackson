import java.math.BigInteger;

/**
 * Simple test to see what is the performance difference between converting
 * simple int values into different wrapper types.
 */
public final class CheckNumberSpeed
{
    final int[] data;

    final static int REPS = 2000;

    private CheckNumberSpeed() {
        data = new int[1200];
        for (int i = 0; i < data.length; ++i) {
            // mostly small positive numbers, some negative
            data[i] = i - 100;
        }
    }

    public void test() throws Exception
    {
        int nr = 0;

        while (true) {
            int type = (nr++) % 4;
            long start = System.nanoTime();
            String str;
            Object o;

            switch (type) {
            case 0:
                o = testIntsValueOf(data);
                str = "int/share";
                break;
            case 1:
                o = testIntsCreate(data);
                str = "int/create";
                break;
            case 2:
                o = testLongs(data);
                str = "longs";
                break;
            default:
                o = testBigInts(data);
                str = "BigInts";
                break;
            }

            long time = System.nanoTime() - start;
            time = time >> 10; // from nano to micro

            System.out.println("Time for '"+str+"' -> "+time+" us (hash: "+o.hashCode()+")");
            Thread.sleep(200L);
        }
    }

    Integer testIntsValueOf(int[] ints)
    {
        Integer v = null;
        int reps = REPS;
        while (--reps > 0) {
            for (int value : ints) {
                v = Integer.valueOf(value);
            }
        }
        return v;
    }

    Integer testIntsCreate(int[] ints)
    {
        Integer v = null;
        int reps = REPS;
        while (--reps > 0) {
            for (int value : ints) {
                v = new Integer(value);
            }
        }
        return v;
    }

    Long testLongs(int[] ints)
    {
        Long v = null;
        int reps = REPS;
        while (--reps > 0) {
            for (int value : ints) {
                v = Long.valueOf(value);
            }
        }
        return v;
    }

    BigInteger testBigInts(int[] ints)
    {
        BigInteger v = null;
        int reps = REPS;
        while (--reps > 0) {
            for (int value : ints) {
                v = BigInteger.valueOf(value);
            }
        }
        return v;
    }

    public static void main(String[] args) throws Exception
    {
        new CheckNumberSpeed().test();
    }
}
