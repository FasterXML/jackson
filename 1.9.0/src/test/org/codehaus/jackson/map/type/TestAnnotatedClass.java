package org.codehaus.jackson.map.type;

import java.util.*;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicClassIntrospector;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;

import org.codehaus.jackson.annotate.*;

/**
 * Unit test for verifying that {@link AnnotatedClass}
 * works as expected.
 */
public class TestAnnotatedClass
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    static class BaseClass
    {
        public int foo;

        public BaseClass(int x, int y) { }

        @JsonProperty public int x() { return 3; }
    }

    static class SubClass extends BaseClass
    {
        public SubClass() { this(1); }
        public SubClass(int x) { super(x, 2); }

        public int y() { return 3; }
    }

    static abstract class GenericBase<T extends Number>
    {
        public abstract void setX(T value);
    }

    static class NumberBean
        extends GenericBase<Integer>
    {
        @Override
        public void setX(Integer value) { }
    }

    /**
     * Test class for checking that field introspection
     * works as expected
     */
    static class FieldBean
    {
        // static, not to be included:
        public static boolean DUMMY;

        // not public, no annotations, shouldn't be included
        @SuppressWarnings("unused")
        private long bar;

        @SuppressWarnings("unused")
        @JsonProperty
        private String props;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @SuppressWarnings("deprecation")
    public void testSimple()
    {
        // null -> no mix-in annotations
        AnnotatedClass ac = AnnotatedClass.construct(SubClass.class, new JacksonAnnotationIntrospector(), null);
        ac.resolveMemberMethods(BasicClassIntrospector.DEFAULT_GETTER_FILTER);
        ac.resolveCreators(true);
        ac.resolveFields();

        assertNotNull(ac.getDefaultConstructor());
        assertEquals(1, ac.getConstructors().size());
        assertEquals(0, ac.getStaticMethods().size());
        assertEquals(2, ac.getMemberMethodCount());
        for (AnnotatedMethod am : ac.memberMethods()) {
            String name = am.getName();
            if ("y".equals(name)) {
                assertEquals(0, am.getAnnotationCount());
            } else if ("x".equals(name)) {
                assertEquals(1, am.getAnnotationCount());
                assertNotNull(am.getAnnotation(JsonProperty.class));
            } else {
                fail("Unexpected method: "+name);
            }
        }
        assertEquals(1, ac.getFieldCount());
        assertEquals("foo", ac.fields().iterator().next().getName());
    }

    /**
     * Another simple test to verify that the (concrete) type information
     * from a sub-class is used instead of abstract one from superclass.
     */
    @SuppressWarnings("deprecation")
    public void testGenericsWithSetter()
    {
        // null -> no mix-in annotations
        AnnotatedClass ac = AnnotatedClass.construct(NumberBean.class, new JacksonAnnotationIntrospector(), null);
        ac.resolveMemberMethods(BasicClassIntrospector.DEFAULT_SETTER_FILTER);
        assertEquals(1, ac.getMemberMethodCount());

        Iterator<AnnotatedMethod> it = ac.memberMethods().iterator();
        AnnotatedMethod am = it.next();

        assertEquals("setX", am.getName());
        // should be one from sub-class
        assertEquals(NumberBean.class, am.getDeclaringClass());
        assertEquals(Integer.class, am.getParameterClass(0));
    }

    public void testFieldIntrospection()
    {
        // null -> no mix-in annotations
        AnnotatedClass ac = AnnotatedClass.construct(FieldBean.class, new JacksonAnnotationIntrospector(), null);
        ac.resolveFields();
        /* 14-Jul-2009, tatu: AnnotatedClass does remove forcibly ignored
         *   entries, but will still contain non-public fields too (earlier
         *   versions didn't, but filtering was moved to a later point)
         */
        assertEquals(2, ac.getFieldCount());
        for (AnnotatedField f : ac.fields()) {
            String fname = f.getName();
            if (!"bar".equals(fname) && !"props".equals(fname)) {
                fail("Unexpected field name '"+fname+"'");
            }
        }
    }
}
