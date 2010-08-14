package org.codehaus.jackson.map.jsontype.impl;

import java.util.*;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.introspect.*;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;

public class StdSubtypeResolver extends SubtypeResolver
{
    protected LinkedHashSet<NamedType> subtypes;

    public StdSubtypeResolver() { }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public void registerSubtypes(NamedType... types)
    {
        if (subtypes == null) {
            subtypes = new LinkedHashSet<NamedType>();
        }
        for (NamedType type : types) {
            subtypes.add(type);
        }
    }

    public void registerSubtypes(Class<?>... classes)
    {
        NamedType[] types = new NamedType[classes.length];
        for (int i = 0, len = classes.length; i < len; ++i) {
            types[i] = new NamedType(classes[i]);
        }
        registerSubtypes(types);
    }
    
    /**
     * 
     * @param property Base member to use for type resolution: either annotated type (class),
     *    or property (field, getter/setter)
     */
    public Collection<NamedType> collectAndResolveSubtypes(AnnotatedMember property,
        MapperConfig<?> config, AnnotationIntrospector ai)
    {
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> st = ai.findSubtypes(property);
        // If no explicit definitions, base itself might have name
        if (st == null || st.isEmpty()) {
            return null;
        }
        return _collectAndResolve(property, config, ai, st);
    }

    public Collection<NamedType> collectAndResolveSubtypes(AnnotatedClass type,
            MapperConfig<?> config, AnnotationIntrospector ai)
    {
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> st = ai.findSubtypes(type);
        // If no explicit definitions, base itself might have name
        if (st == null || st.isEmpty()) {
            String name = ai.findTypeName(type);
            if (name != null) {
                ArrayList<NamedType> result = new ArrayList<NamedType>();
                result.add(new NamedType(type.getRawType(), name));
                return result;
            }
            return null;
        }
        return _collectAndResolve(type, config, ai, st);

    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    /**
     * Method called to find subtypes for a property that has specific annotations
     * (mostly to support JAXB per-property subtype declarations)
     * 
     * @param property Member (method, field) to proces
     */
    protected List<NamedType> _collectAndResolve(AnnotatedMember property, MapperConfig<?> config,
            AnnotationIntrospector ai, Collection<NamedType> subtypeList)
    {
        // Hmmh. Can't iterate over collection and modify it, so:
        HashSet<NamedType> seen = new HashSet<NamedType>(subtypeList);          
        ArrayList<NamedType> subtypes = new ArrayList<NamedType>(subtypeList);

        // collect all subtypes iteratively
        for (int i = 0; i < subtypes.size(); ++i) {
                NamedType type = subtypes.get(i);
            AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(type.getType(), ai, config);
            // but first: does type have a name already?
            if (!type.hasName()) { // if not, let's see if annotations define it
                type.setName(ai.findTypeName(ac));
            }
            // and see if annotations list more subtypes
            List<NamedType> moreTypes = ai.findSubtypes(ac);
            if (moreTypes != null) {
                for (NamedType t2 : moreTypes) {
                    // we want to keep the first reference (may have name)
                    if (seen.add(t2)) {
                        subtypes.add(t2);
                    }
                }
            }
        }
        return subtypes;
    }

    /**
     * Method called to find subtypes for a specific type (class)
     * 
     * @param property Member (method, field) to proces
     */
    protected List<NamedType> _collectAndResolve(AnnotatedClass rootType,
            MapperConfig<?> config, AnnotationIntrospector ai, Collection<NamedType> subtypeList)
    {
        // Hmmh. Can't iterate over collection and modify it, so:
        HashSet<NamedType> seen = new HashSet<NamedType>(subtypeList);          
        ArrayList<NamedType> subtypes = new ArrayList<NamedType>(subtypeList);
    
        // Plus root type can have name of its own...
        NamedType rootNamedType = new NamedType(rootType.getRawType(), ai.findTypeName(rootType));                
        seen.add(rootNamedType);
        
        // collect all subtypes iteratively
        for (int i = 0; i < subtypes.size(); ++i) {
                NamedType type = subtypes.get(i);
            AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(type.getType(), ai, config);
            // but first: does type have a name already?
            if (!type.hasName()) { // if not, let's see if annotations define it
                type.setName(ai.findTypeName(ac));
            }
            // and see if annotations list more subtypes
            List<NamedType> moreTypes = ai.findSubtypes(ac);
            if (moreTypes != null) {
                for (NamedType t2 : moreTypes) {
                    // we want to keep the first reference (may have name)
                    if (seen.add(t2)) {
                        subtypes.add(t2);
                    }
                }
            }
        }
        // and finally, root type with its name too:
        subtypes.add(rootNamedType);
        return subtypes;
    }
    
}
