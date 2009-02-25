package org.codehaus.jackson.map.type;

import org.codehaus.jackson.map.BaseMapTest;

import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.*;

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

    /*
    //////////////////////////////////////////////
    // Test methods
    //////////////////////////////////////////////
     */

    public void testSimple()
    {
        AnnotatedClass ac = AnnotatedClass.constructFull
            (SubClass.class, JacksonAnnotationFilter.instance, true, ClassIntrospector.GetterMethodFilter.instance);

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

}
