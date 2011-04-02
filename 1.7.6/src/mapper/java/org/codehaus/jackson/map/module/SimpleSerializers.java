package org.codehaus.jackson.map.module;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.type.JavaType;

/**
 * Simple implementation {@link Serializers} which allows registration of
 * serializers based on raw (type erased class).
 * It can work well for basic bean and scalar type serializers, but is not
 * a good fit for handling generic types (like {@link Map}s and {@link Collection}s).
 *<p>
 * Type registrations are assumed to be general; meaning that registration of serializer
 * for a super type will also be used for handling subtypes, unless an exact match
 * is found first. As an example, handler for {@link CharSequence} would also be used
 * serializing {@link StringBuilder} instances, unless a direct mapping was found.
 * 
 * @since 1.7
 */
public class SimpleSerializers implements Serializers
{
    /**
     * Class-based mappings that are used both for exact and
     * sub-class matches.
     */
    protected HashMap<ClassKey,JsonSerializer<?>> _classMappings = null;

    /**
     * Interface-based matches.
     */
    protected HashMap<ClassKey,JsonSerializer<?>> _interfaceMappings = null;

    /*
    /**********************************************************
    /* Life-cycle, construction and configuring
    /**********************************************************
     */
    
    public SimpleSerializers() { }

    /**
     * Method for adding given serializer for type that {@link JsonSerializer#handledType}
     * specifies (which MUST return a non-null class; and can NOT be {@link Object}, as a
     * sanity check).
     * For serializers that do not declare handled type, use the variant that takes
     * two arguments.
     * 
     * @param ser
     */
    public void addSerializer(JsonSerializer<?> ser)
    {
        // Interface to match?
        Class<?> cls = ser.handledType();
        if (cls == null || cls == Object.class) {
            throw new IllegalArgumentException("JsonSerializer of type "+ser.getClass().getName()
                    +" does not define valid handledType() (use alternative registration method?)");
        }
        _addSerializer(cls, ser);
    }

    public <T> void addSerializer(Class<? extends T> type, JsonSerializer<T> ser)
    {
        _addSerializer(type, ser);
    }
    
    private void _addSerializer(Class<?> cls, JsonSerializer<?> ser)
    {
        ClassKey key = new ClassKey(cls);
        // Interface or class type?
        if (cls.isInterface()) {
            if (_interfaceMappings == null) {
                _interfaceMappings = new HashMap<ClassKey,JsonSerializer<?>>();
            }
            _interfaceMappings.put(key, ser);
        } else { // nope, class:
            if (_classMappings == null) {
                _classMappings = new HashMap<ClassKey,JsonSerializer<?>>();
            }
            _classMappings.put(key, ser);
        }
    }
    
    /*
    /**********************************************************
    /* Serializers implementation
    /**********************************************************
     */
    
    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
             BeanDescription beanDesc, BeanProperty property)
    {
        Class<?> cls = type.getRawClass();
        ClassKey key = new ClassKey(cls);
        JsonSerializer<?> ser = null;

        // First: direct match?
        if (cls.isInterface()) {
            if (_interfaceMappings != null) {
                ser = _interfaceMappings.get(key);
                if (ser != null) {
                    return ser;
                }
            }
        } else {
            if (_classMappings != null) {
                ser = _classMappings.get(key);
                if (ser != null) {
                    return ser;
                }
                // If not direct match, maybe super-class match?
                for (Class<?> curr = cls; (curr != null); curr = curr.getSuperclass()) {
                    key.reset(curr);
                    ser = _classMappings.get(key);
                    if (ser != null) {
                        return ser;
                    }
                }
            }
        }
        // No direct match? How about super-interfaces?
        if (_interfaceMappings != null) {
            return _findInterfaceMapping(cls, key);
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected JsonSerializer<?> _findInterfaceMapping(Class<?> cls, ClassKey key)
    {
        for (Class<?> iface : cls.getInterfaces()) {
            key.reset(iface);
            JsonSerializer<?> ser = _interfaceMappings.get(key);
            if (ser != null) {
                return ser;
            }
            ser = _findInterfaceMapping(iface, key);
            if (ser != null) {
                return ser;
            }
        }
        return null;
    }
}
