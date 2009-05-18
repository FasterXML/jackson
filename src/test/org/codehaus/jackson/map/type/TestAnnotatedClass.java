package org.codehaus.jackson.map.type;

import java.lang.reflect.*;
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
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
     */

    static class BaseClass
    {
        public int foo;

        public BaseClass(int x, int y) { }

        @JsonGetter public int x() { return 3; }
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
        private long bar;

        @JsonProperty
            private String props;
    }

    /*
    //////////////////////////////////////////////
    // Test methods
    //////////////////////////////////////////////
     */

    public void testSimple()
    {
        AnnotatedClass ac = AnnotatedClass.constructFull
            (SubClass.class, new JacksonAnnotationIntrospector(),
             true, BasicClassIntrospector.GetterMethodFilter.instance, true);

        assertNotNull(ac.getDefaultConstructor());
        assertEquals(1, ac.getSingleArgConstructors().size());
        assertEquals(0, ac.getSingleArgStaticMethods().size());
        assertEquals(2, ac.getMemberMethodCount());
        for (AnnotatedMethod am : ac.memberMethods()) {
            String name = am.getName();
            if ("y".equals(name)) {
                assertEquals(0, am.getAnnotationCount());
            } else if ("x".equals(name)) {
                assertEquals(1, am.getAnnotationCount());
                assertNotNull(am.getAnnotation(JsonGetter.class));
            } else {
                fail("Unexpected method: "+name);
            }
        }

        List<AnnotatedField> fields = ac.getFields();
        assertEquals(1, fields.size());
        assertEquals("foo", fields.get(0).getName());
    }

    /**
     * Another simple test to verify that the (concrete) type information
     * from a sub-class is used instead of abstract one from superclass.
     */
    public void testGenericsWithSetter()
    {
        AnnotatedClass ac = AnnotatedClass.constructFull
            (NumberBean.class, new JacksonAnnotationIntrospector(),
             true, BasicClassIntrospector.SetterMethodFilter.instance, false);
        assertEquals(1, ac.getMemberMethodCount());

        Iterator<AnnotatedMethod> it = ac.memberMethods().iterator();
        AnnotatedMethod am = it.next();

        assertEquals("setX", am.getName());
        // should be one from sub-class
        assertEquals(NumberBean.class, am.getDeclaringClass());
        Type[] types = am.getGenericParameterTypes();
        assertEquals(Integer.class, types[0]);
    }

    public void testFieldIntrospection()
    {
        AnnotatedClass ac = AnnotatedClass.constructFull
            (FieldBean.class, new JacksonAnnotationIntrospector(),
             false, BasicClassIntrospector.GetterMethodFilter.instance, true);

        List<AnnotatedField> fields = ac.getFields();
        // only one discoverable property...
        assertEquals(1, fields.size());
        assertEquals("props", fields.get(0).getName());
    }
}
