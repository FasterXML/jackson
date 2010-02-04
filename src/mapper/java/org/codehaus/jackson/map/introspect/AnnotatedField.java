package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

/**
 * Object that represents non-static (and usually non-transient/volatile)
 * fields of a class.
 * 
 * @author tatu
 */
public final class AnnotatedField
    extends AnnotatedMember
{
    protected final Field _field;

    protected final AnnotationMap _annotations;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedField(Field field, AnnotationMap annMap)
    {
        _field = field;
        _annotations = annMap;
    }

    /**
     * Method called to override an annotation, usually due to a mix-in
     * annotation masking or overriding an annotation 'real' constructor
     * has.
     */
    public void addOrOverride(Annotation a)
    {
        _annotations.add(a);
    }

    /*
    //////////////////////////////////////////////////////
    // Annotated impl
    //////////////////////////////////////////////////////
     */

    public Field getAnnotated() { return _field; }

    public int getModifiers() { return _field.getModifiers(); }

    public String getName() { return _field.getName(); }

    public <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _annotations.get(acls);
    }

    public Type getGenericType() {
        return _field.getGenericType();
    }

    public Class<?> getRawType() {
        return _field.getType();
    }
    
    /*
    //////////////////////////////////////////////////////
    // AnnotatedMember impl
    //////////////////////////////////////////////////////
     */

    public Class<?> getDeclaringClass() { return _field.getDeclaringClass(); }

    public Member getMember() { return _field; }
    
    /*
    //////////////////////////////////////////////////////
    // Extended API, generic
    //////////////////////////////////////////////////////
     */

    public String getFullName() {
        return getDeclaringClass().getName() + "#" + getName();
    }

    public int getAnnotationCount() { return _annotations.size(); }

    public String toString()
    {
        return "[field "+getName()+", annotations: "+_annotations+"]";
    }
}

