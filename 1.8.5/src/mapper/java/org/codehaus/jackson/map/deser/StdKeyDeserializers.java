package org.codehaus.jackson.map.deser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.type.JavaType;

/**
 * Helper class used to contain simple/well-known key deserializers.
 * Following kinds of Objects can be handled currently:
 *<ul>
 * <li>Primitive wrappers</li>
 * <li>Enums (usually not needed, since EnumMap doesn't call us)</li>
 * <li>Anything with constructor that takes a single String arg
 *   (if not explicitly @JsonIgnore'd)</li>
 * <li>Anything with 'static T valueOf(String)' factory method
 *   (if not explicitly @JsonIgnore'd)</li>
 *</ul>
 */
class StdKeyDeserializers
{
    final HashMap<JavaType, KeyDeserializer> _keyDeserializers = new HashMap<JavaType, KeyDeserializer>();

    private StdKeyDeserializers()
    {
        add(new StdKeyDeserializer.BoolKD());
        add(new StdKeyDeserializer.ByteKD());
        add(new StdKeyDeserializer.CharKD());
        add(new StdKeyDeserializer.ShortKD());
        add(new StdKeyDeserializer.IntKD());
        add(new StdKeyDeserializer.LongKD());
        add(new StdKeyDeserializer.FloatKD());
        add(new StdKeyDeserializer.DoubleKD());
    }

    private void add(StdKeyDeserializer kdeser)
    {
        Class<?> keyClass = kdeser.getKeyClass();
        /* As with other cases involving primitive types, we can use
         * default TypeFactory ok, even if it's not our first choice:
         */
        _keyDeserializers.put(TypeFactory.defaultInstance().constructType(keyClass), kdeser);
    }

    public static HashMap<JavaType, KeyDeserializer> constructAll()
    {
        return new StdKeyDeserializers()._keyDeserializers;
    }

    /*
    /**********************************************************
    /* Dynamic factory methods
    /**********************************************************
     */

    public static KeyDeserializer constructEnumKeyDeserializer(DeserializationConfig config, JavaType type)
    {
        EnumResolver<?> er = EnumResolver.constructUnsafe(type.getRawClass(), config.getAnnotationIntrospector());
        return new StdKeyDeserializer.EnumKD(er);
    }

    public static KeyDeserializer findStringBasedKeyDeserializer(DeserializationConfig config, JavaType type)
    {
        /* We don't need full deserialization information, just need to
         * know creators.
         */
    	BasicBeanDescription beanDesc = config.introspect(type);
        // Ok, so: can we find T(String) constructor?
        Constructor<?> ctor = beanDesc.findSingleArgConstructor(String.class);
        if (ctor != null) {
            return new StdKeyDeserializer.StringCtorKeyDeserializer(ctor);
        }
        /* or if not, "static T valueOf(String)" (or equivalent marked
         * with @JsonCreator annotation?)
         */
        Method m = beanDesc.findFactoryMethod(String.class);
        if (m != null){
            return new StdKeyDeserializer.StringFactoryKeyDeserializer(m);
        }
        // nope, no such luck...
        return null;
    }
}
