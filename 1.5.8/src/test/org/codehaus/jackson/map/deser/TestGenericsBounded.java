package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.TypeReference;

import java.io.Serializable;

public class TestGenericsBounded
    extends BaseMapTest
{
    /*
    /*******************************************************
    /* Helper types
    /*******************************************************
     */

    @SuppressWarnings("serial")
    static class Range<E extends Comparable<E>> implements Serializable
    {
         protected E start, end;

         public Range(){ }
         public Range(E start, E end) {
             this.start = start;
             this.end = end;
         }

         public E getEnd() { return end; }
         public void setEnd(E e) { end = e; }

         public E getStart() { return start; }
         public void setStart(E s) {
             start = s;
         }
    }

    @SuppressWarnings("serial")
    static class DoubleRange extends Range<Double> {
        public DoubleRange() { }
        public DoubleRange(Double s, Double e) { super(s, e); }
    }
     
    static class BoundedWrapper<A extends Serializable>
    {
        public List<A> values;
    }

    @SuppressWarnings("serial")
    static class IntBean implements Serializable
    {
        public int x;
    }

    static class IntBeanWrapper<T extends IntBean> {
        public T wrapped;
    }
    
    /*
    /*******************************************************
    /* Unit tests
    /*******************************************************
     */

    public void testLowerBound() throws Exception
    {
        IntBeanWrapper<?> result = new ObjectMapper().readValue("{\"wrapped\":{\"x\":3}}",
                IntBeanWrapper.class);
        assertNotNull(result);
        assertEquals(IntBean.class, result.wrapped.getClass());
        assertEquals(3, result.wrapped.x);
    }
    
    /**
     * Test related to type bound handling problem within
     * [JACKSON-190]
     */
    public void testBounded() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        BoundedWrapper<IntBean> result = mapper.readValue
            ("{\"values\":[ {\"x\":3} ] } ", new TypeReference<BoundedWrapper<IntBean>>() {});
        List<?> list = result.values;
        assertEquals(1, list.size());
        Object ob = list.get(0);
        assertEquals(IntBean.class, ob.getClass());
        assertEquals(3, result.values.get(0).x);
    }

    public void testGenericsComplex() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        DoubleRange in = new DoubleRange(-0.5, 0.5);
        String json = m.writeValueAsString(in);
        DoubleRange out = m.readValue(json, DoubleRange.class);
        assertNotNull(out);
        assertEquals(-0.5, out.start);
        assertEquals(0.5, out.end);
    }
}
