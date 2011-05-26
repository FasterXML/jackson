package perf;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Micro-benchmark that can be used to see if use of {@link sun.misc.Unsafe}
 * might allow speeding up serialization and deserialization.
 * 
 * @author tatu
 */
public class TestUnsafePerf
{
    final static class Bean {
        public String name;
        public long id;
        public int count;
        public boolean enabled;
    }
    
    private final int REPS = 90000;

    private final Unsafe unsafe;

    private final long offsetName;
    private final long offsetId;
    private final long offsetCount;
    private final long offsetEnabled;

    private Field fieldName;
    private Field fieldId;
    private Field fieldCount;
    private Field fieldEnabled;

    private Field fastFieldName;
    private Field fastFieldId;
    private Field fastFieldCount;
    private Field fastFieldEnabled;

    private final static String VALUE_NAME = "foobar";
    private final static long VALUE_ID = 123456L;
    private final static int VALUE_COUNT = 17;
    
    public TestUnsafePerf() throws Exception
    {
        // First: access "unsafe" to test
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);

        Class<?> cls = Bean.class;
        fieldName = cls.getDeclaredField("name");
        fastFieldName = cls.getDeclaredField("name");
        fastFieldName.setAccessible(true);
        offsetName = unsafe.objectFieldOffset(fieldName);

        fieldId = cls.getDeclaredField("id");
        fastFieldId = cls.getDeclaredField("id");
        fastFieldId.setAccessible(true);
        offsetId = unsafe.objectFieldOffset(fieldId);

        fieldCount = cls.getDeclaredField("count");
        fastFieldCount = cls.getDeclaredField("count");
        fastFieldCount.setAccessible(true);
        offsetCount = unsafe.objectFieldOffset(fieldCount);

        fieldEnabled = cls.getDeclaredField("enabled");
        fastFieldEnabled = cls.getDeclaredField("enabled");
        fastFieldEnabled.setAccessible(true);
        offsetEnabled = unsafe.objectFieldOffset(fieldEnabled);
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 4);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Reflection/fast";
                sum += testReflectionFast(REPS);
                break;
            case 1:
                msg = "Unsafe";
                sum += testUnsafe(REPS);
                break;
            case 2:
                msg = "Regular/direct";
                sum += testRegular(REPS);
                break;
            case 3:
                msg = "Reflection/slow";
                sum += testReflectionSlow(REPS);
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

    protected int testReflectionSlow(int reps) throws Exception
    {
        Bean ob = new Bean();
        for (int i = 0; i < reps; ++i) {
            fieldName.set(ob, VALUE_NAME);
            fieldId.set(ob, Long.valueOf(VALUE_ID));
            fieldCount.set(ob, Integer.valueOf(VALUE_COUNT));
            fieldEnabled.set(ob, Boolean.TRUE);
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    protected int testReflectionFast(int reps) throws Exception
    {
        Bean ob = new Bean();
        for (int i = 0; i < reps; ++i) {
            fastFieldName.set(ob, VALUE_NAME);
            /*
            fastFieldId.set(ob, Long.valueOf(VALUE_ID));
            fastFieldCount.set(ob, Integer.valueOf(VALUE_COUNT));
            fastFieldEnabled.set(ob, Boolean.TRUE);
            */
            fastFieldId.setLong(ob, VALUE_ID);
            fastFieldCount.setInt(ob, VALUE_COUNT);
            fastFieldEnabled.setBoolean(ob, true);
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    protected int testUnsafe(int reps) throws Exception
    {
        Bean ob = new Bean();
        for (int i = 0; i < reps; ++i) {
            unsafe.putObject(ob, offsetName, VALUE_NAME);
            unsafe.putLong(ob, offsetId, VALUE_ID);
            unsafe.putInt(ob, offsetCount, VALUE_COUNT);
            unsafe.putBoolean(ob, offsetEnabled, true);
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    protected int testRegular(int reps) throws Exception
    {
        Bean ob = new Bean();
        for (int i = 0; i < reps; ++i) {
            ob.name = VALUE_NAME;
            ob.id = VALUE_ID;
            ob.count = VALUE_COUNT;
            ob.enabled = true;
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }
    
    public static void main(String[] args)
        throws Exception
    {
        new TestUnsafePerf().test();
    }
}
