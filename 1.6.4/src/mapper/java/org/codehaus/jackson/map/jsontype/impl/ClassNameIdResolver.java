package org.codehaus.jackson.map.jsontype.impl;

import java.util.Collections;
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
        Class<?> cls = value.getClass();

        // [JACKSON-380] Need to ensure that "enum subtypes" work too
        if (Enum.class.isAssignableFrom(cls)) {
            if (!cls.isEnum()) { // means that it's sub-class of base enum, so:
                cls = cls.getSuperclass();
            }
        }
        String str = cls.getName();
        if (str.startsWith("java.util")) {
            /* 25-Jan-2009, tatus: There are some internal classes that
             *   we can not access as is. We need better mechanism; for
             *   now this has to do...
             */
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
            } else {
                String end = str.substring(9);
                if ((end.startsWith(".Arrays$") || end.startsWith(".Collections$"))
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
            /* [JACKSON-350]: Default Class.forName() won't work too well; context class loader
             *    seems like slightly better choice
             */
//          Class<?> cls = Class.forName(id);
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> cls = Class.forName(id, true, loader);
            return TypeFactory.specialize(_baseType, cls);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid type id '"+id+"' (for id type 'Id.class'): no such class found");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid type id '"+id+"' (for id type 'Id.class'): "+e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws Exception
    {
        System.err.println("DEBUG: class == "+Collections.emptyList().getClass().getName());
        
        Object[] obs = new Object[] {
                "foo",
                Integer.valueOf(3),
                new String[1],
                new boolean[1],
        };
        ClassNameIdResolver resolver = new ClassNameIdResolver(TypeFactory.type(Object.class));

        for (Object ob : obs) {
            String name = resolver.idFromValue(ob);
            // first, with default class loader
            System.out.println("Load/def '"+name+"' -> "+Class.forName(name));
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (Object ob : obs) {
            String name = resolver.idFromValue(ob);
            /*
            name = name.replace('.', '/');
            name = "L"+name+";";
            */
            // first, with default class loader
            System.out.println("Load/CL '"+name+"' -> "+Class.forName(name, true, loader));
        }
    }
}
