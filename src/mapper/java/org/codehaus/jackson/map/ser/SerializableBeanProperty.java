package org.codehaus.jackson.map.ser;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.type.JavaType;

public final class SerializableBeanProperty implements BeanProperty
{
    protected final String _name;
    protected final JavaType _type;

    /**
     * Physical entity (field or method) that
     * is used to access value of property (or in case of constructor
     * property, just placeholder)
     */
    protected final AnnotatedMember _accessor;
    
    public SerializableBeanProperty(String name, JavaType type, AnnotatedMember accessor)
    {
        _name = name;
        _type = type;
        _accessor = accessor;
    }
    
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return _accessor.getAnnotation(acls);
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public JavaType getType() {
        return _type;
    }

    @Override
    public AnnotatedMember getMember() {
        return _accessor;
    }
}
