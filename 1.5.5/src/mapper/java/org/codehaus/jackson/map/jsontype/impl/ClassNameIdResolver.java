package org.codehaus.jackson.map.jsontype.impl;

import java.util.EnumMap;
import java.util.EnumSet;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;

public class ClassNameIdResolver
    extends TypeIdResolverBase
{
    public ClassNameIdResolver(JavaType baseType) {
        super(baseType);
    }

    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.CLASS; }

    public void registerSubtype(Class<?> type, String name) {
        // not used with class name - based resolvers
    }
    
    public String idFromValue(Object value)
    {
        String str = value.getClass().getName();
        /* 25-Jan-2009, tatus: There are some internal classes that
         *   we can not access as is. We need better mechanism; for
         *   now this has to do...
         */
        if (str.startsWith("java.util")) {
            /* Enum sets and maps are problematic since we MUST know
             * type of contained enums, to be able to deserialize.
             * In addition, EnumSet is not a concrete type either
             */
            if (value instanceof EnumSet<?>) { // Regular- and JumboEnumSet...
                Class<?> enumClass = ClassUtil.findEnumType((EnumSet<?>) value);
                str = TypeFactory.collectionType(EnumSet.class, enumClass).toCanonical();
            } else if (value instanceof EnumMap<?,?>) {
                Class<?> enumClass = ClassUtil.findEnumType((EnumMap<?,?>) value);
                Class<?> valueClass = Object.class;
                str = TypeFactory.mapType(EnumMap.class, enumClass, valueClass).toCanonical();
            } else if (str.startsWith("java.util.Arrays$")
                       && str.indexOf("List") >= 0) {
                /* 17-Feb-2010, tatus: Another such case: result of
                 *    Arrays.asList() is named like so in Sun JDK...
                 *   Let's just plain old ArrayList in its place
                 * NOTE: chances are there are plenty of similar cases
                 * for other wrappers... (immutable, singleton, synced etc)
                 */
                str = "java.util.ArrayList";
            }
        }
        return str;
    }

    public JavaType typeFromId(String id)
    {
        /* 30-Jan-2010, tatu: Most ids are basic class names; so let's first
         *    check if any generics info is added; and only then ask factory
         *    to do translation when necessary
         */
        if (id.indexOf('<') > 0) {
            JavaType t = TypeFactory.fromCanonical(id);
            // note: may want to try combining with specialization (esp for EnumMap)
            return t;
        }
        try {
            Class<?> cls = Class.forName(id);
            return TypeFactory.specialize(_baseType, cls);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid type id '"+id+"' (for id type 'Id.class'): no such class found");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid type id '"+id+"' (for id type 'Id.class'): "+e.getMessage(), e);
        }
    }

}
