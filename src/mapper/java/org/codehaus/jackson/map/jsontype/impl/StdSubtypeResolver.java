package org.codehaus.jackson.map.jsontype.impl;

import java.util.*;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.introspect.*;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;

public class StdSubtypeResolver extends SubtypeResolver
{
    protected LinkedHashSet<NamedType> _registeredSubtypes;

    public StdSubtypeResolver() { }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public void registerSubtypes(NamedType... types)
    {
        if (_registeredSubtypes == null) {
            _registeredSubtypes = new LinkedHashSet<NamedType>();
        }
        for (NamedType type : types) {
            _registeredSubtypes.add(type);
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
        HashMap<NamedType, NamedType> subtypes = new HashMap<NamedType, NamedType>();
        // [JACKSON-257] then consider registered subtypes (which have precedence over annotations)
        if (_registeredSubtypes != null) {
            Class<?> rawBase = type.getRawType();
            for (NamedType subtype : _registeredSubtypes) {
                // is it a subtype of root type?
                if (rawBase.isAssignableFrom(subtype.getType())) { // yes
                    AnnotatedClass curr = AnnotatedClass.constructWithoutSuperTypes(subtype.getType(), ai, config);
                    _collectAndResolve(curr, subtype, config, ai, subtypes);
                }
            }
        }
        // and then check subtypes via annotations from base type (recursively)
        NamedType rootType = new NamedType(type.getRawType(), null);
        _collectAndResolve(type, rootType, config, ai, subtypes);
        return new ArrayList<NamedType>(subtypes.values());
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
    protected Collection<NamedType> _collectAndResolve(AnnotatedMember property, MapperConfig<?> config,
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
        // 14-Aug-2010, tatu: Should we consider registered subtypes [JACKSON-257]?
        //   For now assume not, such that annotation is assumed to be complete
        return subtypes;
    }

    /**
     * Method called to find subtypes for a specific type (class)
     * 
     * @param property Member (method, field) to proces
     */
    protected void _collectAndResolve(AnnotatedClass annotatedType, NamedType namedType,
            MapperConfig<?> config, AnnotationIntrospector ai, HashMap<NamedType, NamedType> collectedSubtypes)
    {
        if (!namedType.hasName()) {
            String name = ai.findTypeName(annotatedType);
            if (name != null) {
                namedType = new NamedType(namedType.getType(), name);
            }
        }

        // First things first: is base type itself included?
        if (collectedSubtypes.containsKey(namedType)) {
            // if so, no recursion; however, may need to update name?
            if (namedType.hasName()) {
                NamedType prev = collectedSubtypes.get(namedType);
                if (!prev.hasName()) {
                    collectedSubtypes.put(namedType, namedType);
                }
            }
            return;
        }
        // if it wasn't, add and check subtypes recursively
        collectedSubtypes.put(namedType, namedType);
        Collection<NamedType> st = ai.findSubtypes(annotatedType);
        if (st != null && !st.isEmpty()) {
            for (NamedType subtype : st) {
                AnnotatedClass subtypeClass = AnnotatedClass.constructWithoutSuperTypes(subtype.getType(), ai, config);
                // One more thing: name may be either in reference, or in subtype:
                if (!subtype.hasName()) {
                    subtype = new NamedType(subtype.getType(), ai.findTypeName(subtypeClass));
                }
                _collectAndResolve(subtypeClass, subtype, config, ai, collectedSubtypes);
            }
        }
    }

    /*
    protected Collection<NamedType> _collectAndResolve(AnnotatedClass rootType,
            MapperConfig<?> config, AnnotationIntrospector ai, Collection<NamedType> subtypeList)
    {
        // Hmmh. Can't iterate over collection and modify it, need to make a copy
        HashMap<NamedType,NamedType> subtypes = new HashMap<NamedType,NamedType>(4 + 2 * subtypeList.size());
        for (NamedType type : subtypeList) {
            subtypes.put(type, type);
        }
    
        // Plus root type can have name of its own...
        NamedType rootNamedType = new NamedType(rootType.getRawType(), ai.findTypeName(rootType));                
        subtypes.put(rootNamedType, rootNamedType);

        // Check subtypes reachable via annotations first:
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

        return subtypes.values();
    }
    */
}
