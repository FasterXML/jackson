package org.codehaus.jackson.map.deser;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.type.JavaType;

/**
 * Bean property that can be deserialized, that is, there is
 * a setter, field or constructor parameter that can be used
 * to set value of the property.
 */
public final class DeserializableBeanProperty implements BeanProperty
{
    protected final String _name;
    protected final JavaType _type;

    /**
     * Physical entity (field, method or constructor argument) that
     * is used to mutate value of property (or in case of constructor
     * property, just placeholder)
     */
    protected final AnnotatedMember _mutator;
    
    public DeserializableBeanProperty(String name, JavaType type, AnnotatedMember mutator)
    {
        _name = name;
        _type = type;
        _mutator = mutator;
    }

    public DeserializableBeanProperty withType(JavaType type) {
        return new DeserializableBeanProperty(_name, type, _mutator);
    }
    
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return _mutator.getAnnotation(acls);
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
        return _mutator;
    }
}
