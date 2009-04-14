package org.codehaus.jackson.map.type;

import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicClassIntrospector;
import org.codehaus.jackson.map.introspect.JacksonAnnotationFilter;

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

    /*
    //////////////////////////////////////////////
    // Test methods
    //////////////////////////////////////////////
     */

    public void testSimple()
    {
        AnnotatedClass ac = AnnotatedClass.constructFull
            (SubClass.class, JacksonAnnotationFilter.instance, true, BasicClassIntrospector.GetterMethodFilter.instance);

        assertNotNull(ac.getDefaultConstructor());
        assertEquals(1, ac.getSingleArgConstructors().size());
        assertEquals(0, ac.getSingleArgStaticMethods().size());
        assertEquals(2, ac.getMemberMethods().size());
        for (AnnotatedMethod am : ac.getMemberMethods()) {
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
    }

    /**
     * Another simple test to verify that the (concrete) type information
     * from a sub-class is used instead of abstract one from superclass.
     */
    public void testGenericsWithSetter()
    {
        AnnotatedClass ac = AnnotatedClass.constructFull
            (NumberBean.class, JacksonAnnotationFilter.instance, true, BasicClassIntrospector.SetterMethodFilter.instance);
        Collection<AnnotatedMethod> methods = ac.getMemberMethods();
        assertEquals(1, methods.size());

        AnnotatedMethod am = methods.iterator().next();

        assertEquals("setX", am.getName());
        // should be one from sub-class
        assertEquals(NumberBean.class, am.getDeclaringClass());
        Type[] types = am.getGenericParameterTypes();
        assertEquals(Integer.class, types[0]);
    }
}
