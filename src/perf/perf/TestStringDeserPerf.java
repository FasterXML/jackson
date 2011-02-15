package perf;

public class TestStringDeserPerf
{
    private final static int REPS = 250000;

    final static int LEN = 111;
    
    static final byte[] BYTES = new byte[LEN];
    static {
        for (int i = 0; i < LEN; ++i) {
            BYTES[i] = (byte) (32 + i & 15);
        }
    }
    
    private void test() throws Exception
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 2);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            sum = 0;
            
            switch (round) {

            case 0:
                msg = "Char[]";
                sum += testCharArray();
                break;
            case 1:
                msg = "StringBuilder";
                sum += testStringBuilder();
                break;
            case 2:
                msg = "Char[] x 4";
                sum += testCharArray2();
                break;
            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("+(sum & 0xFF)+").");
        }
    }

    private final int testCharArray()
    {
        String result = null;
        int reps = REPS;
        char[] buffer = new char[1000];
        final byte[] INPUT = BYTES;
        while (--reps >= 0) {
            int i = 0;
            for (int len = INPUT.length; i < len; ++i) {
                buffer[i] = (char) INPUT[i];
            }
            result = new String(buffer, 0, i);
        }
        if (result.length() != INPUT.length) {
            throw new Error();
        }
        return result.hashCode();
    }
    
    private final int testCharArray2()
    {
        String result = null;
        int reps = REPS;
        char[] buffer = new char[1000];
        final byte[] INPUT = BYTES;
        while (--reps >= 0) {
            int i = 0;
            int end = INPUT.length - 4;
            while (i <= end) {
                buffer[i] = (char) INPUT[i];
                ++i;
                buffer[i] = (char) INPUT[i];
                ++i;
                buffer[i] = (char) INPUT[i];
                ++i;
                buffer[i] = (char) INPUT[i];
                ++i;
            }
            end += 4;
            if (i < end) {
                buffer[i] = (char) INPUT[i];
                if (++i < end) {
                    buffer[i] = (char) INPUT[i];
                    if (++i < end) {
                        buffer[i] = (char) INPUT[i];
                    }
                }
            }
            result = new String(buffer, 0, i);
        }
        if (result.length() != INPUT.length) {
            throw new Error();
        }
        return result.hashCode();
    }

/*    
    private final int testStringBuilder()
    {
        String result = null;
        int reps = REPS;
        final byte[] INPUT = BYTES;
        final StringBuilder sb = new StringBuilder(INPUT.length);
        while (--reps >= 0) {
            sb.setLength(0);
            final int len = INPUT.length;
            for (int i = 0; i < len; ++i) {
                sb.append((char) INPUT[i]);
            }
            result = sb.toString();
        }
        return result.hashCode();
    }
    */

    // Version that does not reuse instances
    private final int testStringBuilder()
    {
        String result = null;
        int reps = REPS;
        final byte[] INPUT = BYTES;
        while (--reps >= 0) {
            final int len = INPUT.length;
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; ++i) {
                sb.append((char) INPUT[i]);
            }
            result = sb.toString();
        }
        return result.hashCode();
    }
    
    /*
    private final int testStringBuffer()
    {
        String result = null;
        int reps = REPS;
        final byte[] INPUT = BYTES;
        while (--reps >= 0) {
            final int len = INPUT.length;
            StringBuffer sb = new StringBuffer(len);
            for (int i = 0; i < len; ++i) {
                sb.append((char) INPUT[i]);
            }
            result = sb.toString();
        }
        return result.hashCode();
    }
    */
    
    public static void main(String[] args) throws Exception
    {
        new TestStringDeserPerf().test();
    }
}
