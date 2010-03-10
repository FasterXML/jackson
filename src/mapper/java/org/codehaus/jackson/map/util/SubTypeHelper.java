package org.codehaus.jackson.map.util;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.jsontype.NamedType;

/**
 * Helper class used to encapsulate details of resolving information about
 * subtypes, for Polymorphic Type Handling.
 * 
 * @author tatu
 * @since 1.5
 */
public class SubTypeHelper
{
    public final static SubTypeHelper instance = new SubTypeHelper();

    private SubTypeHelper() { }

    /**
     * 
     * @param base Base member to use for type resolution: either annotated type (class),
     *    or property (field, getter/setter)
     */
    public static List<NamedType> collectAndResolveSubtypes(Annotated base,
        MapperConfig<?> config, AnnotationIntrospector ai)
    {
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> st = ai.findSubtypes(base);
        AnnotatedClass ac = (base instanceof AnnotatedClass) ?
                (AnnotatedClass) base : null;
        // If no explicit definitions, base itself might have name
        if (st == null || st.isEmpty()) {
            if (ac != null) {
                String name = ai.findTypeName(ac);
                if (name != null) {
                    ArrayList<NamedType> result = new ArrayList<NamedType>();
                    result.add(new NamedType(ac.getRawType(), name));
                    return result;
                }
            }
            return null;
        }
        return instance._collectAndResolve(ac, config, ai, st);
    }
    
    /**
     * 
     * @param rootType If type resolution started with a root type, definition of
     *   that type; null if 
     * @return
     */
    protected List<NamedType> _collectAndResolve(AnnotatedClass rootType,
                MapperConfig<?> config, AnnotationIntrospector ai, Collection<NamedType> subtypeList)
    {
        // Hmmh. Can't iterate over collection and modify it, so:
        HashSet<NamedType> seen = new HashSet<NamedType>(subtypeList);          
        ArrayList<NamedType> subtypes = new ArrayList<NamedType>(subtypeList);

        // Plus root type can have name of its own...
        NamedType rootNamedType = (rootType == null) ? null
                : new NamedType(rootType.getRawType(), ai.findTypeName(rootType));                
        if (rootNamedType != null) {
            seen.add(rootNamedType);
        }
        
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
        if (rootNamedType != null) {
            subtypes.add(rootNamedType);
        }
        return subtypes;
    }
}
