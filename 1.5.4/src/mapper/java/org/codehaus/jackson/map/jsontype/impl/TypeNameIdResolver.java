package org.codehaus.jackson.map.jsontype.impl;

import java.util.*;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

public class TypeNameIdResolver
    extends TypeIdResolverBase
{
    /**
     * Mappings from class name to type id, used for serialization
     */
    protected final HashMap<String, String> _typeToId;

    /**
     * Mappings from type id to JavaType, used for deserialization
     */
    protected final HashMap<String, JavaType> _idToType;
    
    protected TypeNameIdResolver(JavaType baseType,
            HashMap<String, String> typeToId,
            HashMap<String, JavaType> idToType)
    {
        super(baseType);
        _typeToId = typeToId;
        _idToType = idToType;
    }
 
    public static TypeNameIdResolver construct(JavaType baseType,
            Collection<NamedType> subtypes, boolean forSer, boolean forDeser)
    {
        // sanity check
        if (forSer == forDeser) throw new IllegalArgumentException();
        
        HashMap<String, String> typeToId = null;
        HashMap<String, JavaType> idToType = null;

        if (forSer) {
            typeToId = new HashMap<String, String>();
        }
        if (forDeser) {
            idToType = new HashMap<String, JavaType>();
        }
        if (subtypes != null) {
            for (NamedType t : subtypes) {
                /* no name? Need to figure out default; for now, let's just
                 * use non-qualified class name
                 */
                Class<?> cls = t.getType();
                String id = t.hasName() ? t.getName() : _defaultTypeId(cls);
                if (forSer) {
                    typeToId.put(cls.getName(), id);
               }
                if (forDeser) {
                    // In case of name collisions, let's make sure first one wins:
                    if (!idToType.containsKey(id)) {
                        idToType.put(id, TypeFactory.type(cls));
                    }
                }
            }
        }
        return new TypeNameIdResolver(baseType, typeToId, idToType);
    }

    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }
    
    public String idFromValue(Object value)
    {
        Class<?> cls = value.getClass();
        String name = _typeToId.get(cls.getName());
        // can either throw an exception, or use default name...
        if (name == null) {
            // let's choose default?
            name = _defaultTypeId(cls);
        }
        return name;
    }

    public JavaType typeFromId(String id)
        throws IllegalArgumentException
    {
        JavaType t = _idToType.get(id);
        /* Now: if no type is found, should we try to locate it by
         * some other means? (specifically, if in same package as base type,
         * could just try Class.forName)
         * For now let's not add any such workarounds; can add if need be
         */
        return t;
    }    

    /*
    /*********************************************************
    /* Helper methods
    /*********************************************************
     */
    
    /**
     * If no name was explicitly given for a class, we will just
     * use non-qualified class name
     */
    protected static String _defaultTypeId(Class<?> cls)
    {
        String n = cls.getName();
        int ix = n.lastIndexOf('.');
        return (ix < 0) ? n : n.substring(ix+1);
    }
}
