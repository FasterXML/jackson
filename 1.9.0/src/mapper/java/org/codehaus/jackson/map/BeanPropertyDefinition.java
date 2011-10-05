package org.codehaus.jackson.map;

import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.AnnotatedParameter;
import org.codehaus.jackson.map.util.Named;

/**
 * Simple value classes that contain definitions of properties,
 * used during introspection of properties to use for
 * serialization and deserialization purposes.
 * These instances are created before actual {@link BeanProperty}
 * instances are created, i.e. they are used earlier in the process
 * flow.
 *
 * @since 1.9
 */
public abstract class BeanPropertyDefinition
    implements Named
{
    /**
     * Accessor for name used for external representation (in JSON).
     */
    @Override // from Named
    public abstract String getName();

    /**
     * Accessor that can be used to determine implicit name from underlying
     * element(s) before possible renaming. This is the "internal"
     * name derived from accessor ("x" from "getX"), and is not based on
     * annotations or naming strategy.
     */
    public abstract String getInternalName();
    
    public abstract boolean hasGetter();
    public abstract boolean hasSetter();
    public abstract boolean hasField();
    public abstract boolean hasConstructorParameter();

    public boolean couldDeserialize() {
        return getMutator() != null;
    }
    public boolean couldSerialize() {
        return getAccessor() != null;
    }

    public abstract AnnotatedMethod getGetter();
    public abstract AnnotatedMethod getSetter();
    public abstract AnnotatedField getField();
    public abstract AnnotatedParameter getConstructorParameter();

    /**
     * Method used to find accessor (getter, field to access) to use for accessing
     * value of the property.
     * Null if no such member exists.
     */
    public abstract AnnotatedMember getAccessor();

    /**
     * Method used to find mutator (constructor parameter, setter, field) to use for
     * changing value of the property.
     * Null if no such member exists.
     */
    public abstract AnnotatedMember getMutator();
}
