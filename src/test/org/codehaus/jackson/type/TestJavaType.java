package org.codehaus.jackson.type;

import java.util.*;

import main.BaseTest;

import org.codehaus.jackson.type.*;
import org.codehaus.jackson.map.type.*;

/**
 * Simple tests to verify that {@link JavaType} types work to
 * some degree
 */
public class TestJavaType
    extends BaseTest
{
    static class BaseType { }

    //static class SubType extends BaseType { }

    public void testSubclassing() {
        JavaType baseType = TypeFactory.fromType(BaseType.class);
        assertSame(BaseType.class, baseType.getRawClass());
        assertTrue(baseType.hasRawClass(BaseType.class));

        assertFalse(baseType.isArrayType());
        assertFalse(baseType.isContainerType());
        assertFalse(baseType.isEnumType());
        assertFalse(baseType.isInterface());
        assertFalse(baseType.isPrimitive());

        assertNull(baseType.findVariableType("foobar"));
        assertNull(baseType.getContentType());
        assertNull(baseType.getHandler());

        /* both narrow and widen just return type itself (exact, not just
         * equal)
         * (also note that widen/narrow wouldn't work on basic simple
         * class type otherwise)
         */
        assertSame(baseType, baseType.narrowBy(BaseType.class));
        assertSame(baseType, baseType.widenBy(BaseType.class));

        // Also, let's try assigning bogus handler
        baseType.setHandler("xyz"); // untyped
        assertEquals("xyz", baseType.getHandler());
        // illegal to re-set
        try {
            baseType.setHandler("foobar");
            fail("Shouldn't allow re-setting handler");
        } catch (IllegalStateException iae) {
            verifyException(iae, "Trying to reset");
        }
    }
}

