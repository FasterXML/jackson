package org.codehaus.jackson.map.ser;

import java.lang.reflect.Modifier;
import java.util.*;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerFactory;

/**
 * Serializer factory implementation that allows for defining static
 * mappings between set of classes and/or interfaces, and corresponding
 * serializers. These mappings are checked first; but if no match
 * is found, factory can delegate to configured fallback factory for
 * further lookups. Typically fallback factory used is
 * {@link BeanSerializerFactory} or one of its sub-classes; but can
 * be any other serializer factory or left undefined (set to null)
 * if only static mappings are to be used.
 */
public class CustomSerializerFactory
    extends SerializerFactory
{
    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Factory to use if we do not have a registered serializer for
     * given type
     */
    final SerializerFactory _fallbackFactory;

    /**
     * First, direct mappings that are only used for exact class type
     *  matches, but not for sub-class checks.
     */
    HashMap<ClassKey,JsonSerializer<?>> _directClassMappings = null;

    /**
     * And then class-based mappings that are used both for exact and
     * sub-class matches.
     */
    HashMap<ClassKey,JsonSerializer<?>> _transitiveClassMappings = null;

    /**
     * And finally interface-based matches.
     */
    HashMap<ClassKey,JsonSerializer<?>> _interfaceMappings = null;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle, constructors
    ////////////////////////////////////////////////////
     */

    /**
     * Default constructor will use {@link BeanSerializerFactory}
     * as the fallback serializer factory.
     */
    public CustomSerializerFactory() {
        this(BeanSerializerFactory.instance);
    }

    /**
     * Default constructor will use {@link BeanSerializerFactory}
     * as the fallback serializer factory.
     */
    public CustomSerializerFactory(SerializerFactory fallbackFactory) {
        _fallbackFactory = fallbackFactory;
    }

    /*
    ////////////////////////////////////////////////////
    // Life-cycle, configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Method used to add a generic (transitive) mapping from specified
     * class or its sub-classes into a serializer.
     * When resolving a type into a serializer, explicit class is checked
     * first, then immediate super-class, and so forth along inheritance
     * chain. But if this fails, implemented interfaces are checked;
     * ordering is done such that first interfaces implemented by
     * the exact type are checked (in order returned by
     * {@link Class#getInterfaces}), then super-type's and so forth.
     *<p>
     * Note that adding generic mappings may lead to problems with
     * sub-classing: if sub-classes add new properties, these may not
     * get properly serialized.
     */
    public <T> void addGenericMapping(Class<T> type, JsonSerializer<T> ser)
    {
        // Interface to match?
        ClassKey key = new ClassKey(type);
        if (type.isInterface()) {
            if (_interfaceMappings == null) {
                _interfaceMappings = new HashMap<ClassKey,JsonSerializer<?>>();
            }
            _interfaceMappings.put(key, ser);
        } else { // nope, class:
            if (_transitiveClassMappings == null) {
                _transitiveClassMappings = new HashMap<ClassKey,JsonSerializer<?>>();
            }
            _transitiveClassMappings.put(key, ser);
        }
    }

    /**
     * Method used to add a mapping from specific type -- and only that
     * type -- to specified serializer. This means that binding is not
     * used for sub-types. It also means that no such mappings are to
     * be defined for abstract classes or interfaces: and if an attempt
     * is made, {@link IllegalArgumentException} will be thrown to
     * indicate caller error.
     */
    public <T> void addSpecificMapping(Class<T> forClass, JsonSerializer<T> ser)
    {
        ClassKey key = new ClassKey(forClass);

        /* First, let's ensure it's not an interface or abstract class:
         * as those can not be instantiated, such mappings would never
         * get used.
         */
        if (forClass.isInterface()) {
            throw new IllegalArgumentException("Can not add specific mapping for an interface ("+forClass.getName()+")");
        }
        if (Modifier.isAbstract(forClass.getModifiers())) {
            throw new IllegalArgumentException("Can not add specific mapping for an abstract class ("+forClass.getName()+")");
        }

        if (_directClassMappings == null) {
            _directClassMappings = new HashMap<ClassKey,JsonSerializer<?>>();
        }
        _directClassMappings.put(key, ser);
    }

    /*
    ////////////////////////////////////////////////////
    // JsonSerializerFactory impl
    ////////////////////////////////////////////////////
     */

    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonSerializer<T> createSerializer(Class<T> type)
    {
        JsonSerializer<?> ser = null;
        ClassKey key = new ClassKey(type);

        // First: exact matches
        if (_directClassMappings != null) {
            ser = _directClassMappings.get(key);
        }

        // No match? How about more generic ones?
        // Mappings for super-classes?
        if (ser == null && _transitiveClassMappings != null) {
            for (Class<?> curr = type; (curr != null) && (ser == null); curr = curr.getSuperclass()) {
                key.reset(curr);
                ser = _transitiveClassMappings.get(key);
            }
        }

        // And if still no match, how about interfaces?
        if (ser == null && _interfaceMappings != null) {
            for (Class<?> curr = type; (curr != null) && (ser == null); curr = curr.getSuperclass()) {
                for (Class<?> iface : curr.getInterfaces()) {
                    key.reset(iface);
                    ser = _interfaceMappings.get(key);
                    if (ser != null) {
                        break;
                    }
                }
            }
        }
        if (ser == null && _fallbackFactory != null) {
            return _fallbackFactory.createSerializer(type);
        }
        return (JsonSerializer<T>) ser;
    }
}

