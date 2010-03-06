package org.codehaus.jackson.map.introspect;

import java.lang.reflect.Member;

import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Intermediate base class for annotated entities that are members of
 * a class; fields, methods and constructors. This is a superset
 * of things that can represent logical properties as it contains
 * constructors in addition to fields and methods.
 * 
 * @author tatu
 * @since 1.5
 */
public abstract class AnnotatedMember extends Annotated
{
    protected AnnotatedMember() { super(); }

    public abstract Class<?> getDeclaringClass();

    public abstract Member getMember();
    
    /**
     * Method that can be called to modify access rights, by calling
     * {@link java.lang.reflect.AccessibleObject#setAccessible} on
     * the underlying annotated element.
     */
    public final void fixAccess() {
        ClassUtil.checkAndFixAccess(getMember());
    }
}
