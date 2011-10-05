package perf;

public class TestMethodDispatchSpeed
{
    /*
    /**********************************************************
    /* Test beans
    /**********************************************************
     */
    
    public interface SerializableString {
        public byte[] getUTF8();
    }

    public final class SerializableStringImpl implements SerializableString
    {
        private final byte[] stuff;

        public SerializableStringImpl(byte[] in) { stuff = in; }
        
        @Override
        public byte[] getUTF8() { return stuff; }
    }

    public final class SerializedString
    {
        private final byte[] stuff;

        public SerializedString(byte[] in) { stuff = in; }
        
        public byte[] getUTF8() { return stuff; }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static int REPS = 1500;
    
    public void test() throws Exception
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 2);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            byte[] buffer = new byte[64000];
//            final String inputStr = "foobar12345";
            final String inputStr = "abc";
            final byte[] inputBytes = inputStr.getBytes("UTF-8");
            
            switch (round) {

            case 0:
                msg = "Final Class";
                sum += testWithClass(inputBytes, buffer, REPS);
                break;
            case 1:
                msg = "Interface";
                sum += testWithInterface(inputBytes, buffer, REPS);
                break;

            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("+(sum & 0xFF)+").");
            Thread.sleep(100L);
        }
    }

    private final int testWithInterface(byte[] input, byte[] buffer, int reps)
    {
        final SerializedString ss = new SerializedString(input);
        int count = 0;

        for (int i = 0; i < reps; ++i) {
            final int end = buffer.length - ss.getUTF8().length;
            int ptr = 0;
            while (ptr < end) {
                byte[] curr = ss.getUTF8();
                int len = curr.length;
                System.arraycopy(curr, 0, buffer, ptr, len);
                ptr += len;
                ++count;
            }
        }
        return count;
    }

    private final int testWithClass(byte[] input, byte[] buffer, int reps)
    {
        final SerializableString ss = new SerializableStringImpl(input);
        int count = 0;

        for (int i = 0; i < reps; ++i) {
            final int end = buffer.length - ss.getUTF8().length;
            int ptr = 0;
            while (ptr < end) {
                byte[] curr = ss.getUTF8();
                int len = curr.length;
                System.arraycopy(curr, 0, buffer, ptr, len);
                ptr += len;
                ++count;
            }
        }
        return count;
    }
    
    public static void main(String[] args) throws Exception
    {
        new TestMethodDispatchSpeed().test();
    }
}
