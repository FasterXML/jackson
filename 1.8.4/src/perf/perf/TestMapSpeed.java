package perf;

import java.util.*;

public class TestMapSpeed
{
    public final static String STRING1 = "abcd";
    public final static String STRING2 = "efghij";
    public final static String STRING3 = "12345";
    public final static String STRING4 = "098765";
    public final static String STRING5 = "x";
    public final static String STRING6 = "abd1234";
    public final static String STRING7 = "ratherlong";
    public final static String STRING8 = "whatever!";

    public final static String[] KEYS = new String[] {
        STRING1, STRING2, STRING3, STRING4
        ,STRING5, STRING6
       ,STRING7, STRING8
    };
    
    /*
    /**********************************************************
    /* Test beans
    /**********************************************************
     */
    
    abstract static class MyMap
    {
        public abstract Integer find(String key);
    }

    final static class SmallMyMap extends MyMap
    {
        final String str1;
        final String str2;
        final String str3;
        final String str4;
        final String str5;
        final String str6;
        final String str7;
        final String str8;

        final Integer int1;
        final Integer int2;
        final Integer int3;
        final Integer int4;
        final Integer int5;
        final Integer int6;
        final Integer int7;
        final Integer int8;
        
        public SmallMyMap(String str1, Integer int1,
                String str2, Integer int2,
                String str3, Integer int3,
                String str4, Integer int4,
                String str5, Integer int5,
                String str6, Integer int6,
                String str7, Integer int7,
                String str8, Integer int8
                )
        {
            this.str1 = str1;
            this.int1 = int1;
            this.str2 = str2;
            this.int2 = int2;
            this.str3 = str3;
            this.int3 = int3;
            this.str4 = str4;
            this.int4 = int4;
            this.str5 = str5;
            this.int5 = int5;
            this.str6 = str6;
            this.int6 = int6;
            this.str7 = str7;
            this.int7 = int7;
            this.str8 = str8;
            this.int8 = int8;
        }

        @Override
        public Integer find(String key)
        {
//            int hash = key.hashCode();
            if (key == str1) return int1;
            if (key == str2) return int2;
            if (key == str3) return int3;
            if (key == str4) return int4;
            if (key == str5) return int5;
            if (key == str6) return int6;
            if (key == str7) return int7;
            if (key == str8) return int8;
            return null;
        }
    }

    final static class BigMyMap extends MyMap
    {
        private final int size;
        private final String[] strings;
        private final Integer[] ints;
        
        public BigMyMap(String[] keys)
        {
            size = keys.length;
            strings = new String[size];
            System.arraycopy(keys, 0, strings, 0, size);
            ints = new Integer[size];
            for (int i = 0; i < size; ++i) {
                ints[i] = Integer.valueOf(i+1);
            }
        }

        @Override
        public Integer find(String key)
        {
            final int len = size;
            for (int i = 0; i < len; ++i) {
                if (strings[i] == key) return ints[i];
            }
            return null;
        }
    }

    /* Implementation similar to regular HashMap, only slightly
     * more optimized
     */
    final static class MyBucketMap extends MyMap
    {
        private final Bucket[] _buckets;
        
        public MyBucketMap(String[] keys)
        {
            Bucket[] buckets = new Bucket[16];
            final int size = keys.length;
            for (int i = 0; i < size; ++i) {
                int hash = keys[i].hashCode();
                int index = hash & 0xF;
                buckets[index] = new Bucket(buckets[index], keys[i], Integer.valueOf(i+1));
            }
            _buckets = buckets;
        }

        @Override
        public Integer find(String key)
        {
            int index = key.hashCode() & (_buckets.length-1);
            Bucket bucket = _buckets[index];
            while (bucket != null) {
                if (bucket.key == key) {
                    return bucket.value;
                }
                bucket = bucket.next;
            }
            return find2(key, index);
        }

        private Integer find2(String key, int index)
        {
            Bucket bucket = _buckets[index];
            while (bucket != null) {
                if (key.equals(bucket.key)) {
                    return bucket.value;
                }
                bucket = bucket.next;
            }
            return null;
        }
        
        private final static class Bucket
        {
            public final Bucket next;
            public final String key;
            public final Integer value;
            
            public Bucket(Bucket next, String key, Integer value)
            {
                this.next = next;
                this.key = key;
                this.value = value;
            }
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static int REPS = 299500;

    private final HashMap<String,Integer> stdMap;

    private final MyMap smallMap;
    private final MyMap bigMap;

    private final MyBucketMap bucketMap;
    
    public TestMapSpeed()
    {
        stdMap = new HashMap<String,Integer>();
        for (int i = 0, len = KEYS.length; i < len; ++i) {
            stdMap.put(KEYS[i], Integer.valueOf(i+1));
        }
        smallMap = new SmallMyMap(
                STRING1, Integer.valueOf(1),
                STRING2, Integer.valueOf(2),
                STRING3, Integer.valueOf(3),
                STRING4, Integer.valueOf(4),
                STRING5, Integer.valueOf(5),
                STRING6, Integer.valueOf(6),
                STRING7, Integer.valueOf(7),
                STRING8, Integer.valueOf(8)
                );
        bigMap = new BigMyMap(KEYS);
        bucketMap = new MyBucketMap(KEYS);
    }
    
    public void test() throws Exception
    {
        int i = 0;
        int sum = 0;

        while (true) {
            Thread.sleep(100L);
            int round = (i++ % 4);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);
            int result;

            switch (round) {

            case 0:
                msg = "JDK HashMap";
                result = testWithMap(REPS, stdMap);
                break;
            case 1:
                msg = "Custom (small)";
                result = testWithCustom(REPS, smallMap);
                break;
            case 2:
                msg = "Custom (big)";
                result = testWithCustom(REPS, bigMap);
                break;
            case 3:
                msg = "Custom (HashMap)";
                result = testWithBucketMap(REPS, bucketMap);
                break;

            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            sum += result;
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("+(result & 0xFFFF)+" -> "+sum+").");
            Thread.sleep(100L);
        }
    }

    private final int testWithMap(int reps, HashMap<String,Integer> ints)
    {
        int sum = 0;
        for (int i = 0; i < reps; ++i) {
            for (int index = 0, len = KEYS.length; index < len; ++index) {
                Integer I = ints.get(KEYS[index]);
                sum += I.intValue();
            }
        }
        return sum;
    }

    private final int testWithCustom(int reps, MyMap ints)
    {
        int sum = 0;
        for (int i = 0; i < reps; ++i) {
            for (int index = 0, len = KEYS.length; index < len; ++index) {
                String key = KEYS[index];
                Integer I = ints.find(key);
                sum += I.intValue();
            }
        }
        return sum;
    }

    private final int testWithBucketMap(int reps, MyBucketMap buckets)
    {
        int sum = 0;
        for (int i = 0; i < reps; ++i) {
            for (int index = 0, len = KEYS.length; index < len; ++index) {
                String key = KEYS[index];
                Integer I = buckets.find(key);
                sum += I.intValue();
            }
        }
        return sum;
    }
    
    public static void main(String[] args) throws Exception
    {
        new TestMapSpeed().test();
    }

}
