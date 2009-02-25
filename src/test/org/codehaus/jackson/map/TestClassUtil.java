package org.codehaus.jackson.map;

import java.util.*;

import static org.junit.Assert.*;

import org.codehaus.jackson.map.util.ClassUtil;

public class TestClassUtil
    extends BaseMapTest
{
    /* Test classes and interfaces needed for testing class util
     * methods
     */
    static abstract class BaseClass implements Comparable<BaseClass>,
        BaseInt
    {
    }

    interface BaseInt { }

    interface SubInt extends BaseInt { }

    static abstract class SubClass
        extends BaseClass
        implements SubInt { }

    public void testSimple()
    {
        Collection<Class<?>> result = ClassUtil.findSuperTypes(SubClass.class, null);
        Class[] classes = result.toArray(new Class[result.size()]);
        Class[] exp = new Class[] {
            SubInt.class, BaseInt.class,
            BaseClass.class,
            Comparable.class
        };
        assertArrayEquals(exp, classes);
    }
}
