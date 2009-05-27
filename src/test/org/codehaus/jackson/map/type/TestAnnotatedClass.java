package org.codehaus.jackson.map.type;

import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
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
    //////////////////////////////////////////////
    // Test methods
    //////////////////////////////////////////////
     */

    public void testSimple()
    {
        AnnotatedClass ac = AnnotatedClass.construct(SubClass.class, new JacksonAnnotationIntrospector());
        ac.resolveMemberMethods(BasicClassIntrospector.GetterMethodFilter.instance);
        ac.resolveCreators(true);
        ac.resolveFields();

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
                assertNotNull(am.getAnnotation(JsonProperty.class));
            } else {
                fail("Unexpected method: "+name);
            }
        }
        assertEquals(1, ac.getFieldCount());
        assertEquals("foo", ac.getFields().iterator().next().getName());
    }

    /**
     * Another simple test to verify that the (concrete) type information
     * from a sub-class is used instead of abstract one from superclass.
     */
    public void testGenericsWithSetter()
    {
        AnnotatedClass ac = AnnotatedClass.construct(NumberBean.class, new JacksonAnnotationIntrospector());
        ac.resolveMemberMethods(BasicClassIntrospector.SetterMethodFilter.instance);
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
        AnnotatedClass ac = AnnotatedClass.construct(FieldBean.class, new JacksonAnnotationIntrospector());
        ac.resolveFields();
        assertEquals(1, ac.getFieldCount());
        // only one discoverable field property...
        assertEquals("props", ac.getFields().iterator().next().getName());
    }
}
