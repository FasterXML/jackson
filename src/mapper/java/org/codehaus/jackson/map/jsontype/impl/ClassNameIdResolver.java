package org.codehaus.jackson.map.jsontype.impl;

import java.util.EnumSet;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.type.TypeFactory;

public class ClassNameIdResolver
    extends TypeIdResolverBase
{
    public ClassNameIdResolver(JavaType baseType) {
        super(baseType);
    }

    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.CLASS; }
    
    public String idFromValue(Object value)
    {
        String str = value.getClass().getName();
        /* 25-Jan-2009, tatus: There are some internal classes that
         *   we can not access as is. We need better mechanism; for
         *   now this has to do...
         */
        if (str.startsWith("java.")) {
            if (value instanceof EnumSet<?>) { // Regular- and JumboEnumSet...
                str = EnumSet.class.getName();
            }
        }
        return str;
    }

    public JavaType typeFromId(String id)
    {
        try {
            Class<?> cls = Class.forName(id);
            return TypeFactory.specialize(_baseType, cls);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid type id '"+id+"' (for id type 'Id.class'): no such class found");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid type id '"+id+"' (for id type 'Id.class'): "+e.getMessage());
        }
    }

}
