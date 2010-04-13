package org.codehaus.jackson.map.annotate;

import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;

/**
 * Annotation that can be used to explicitly define custom resolver
 * used for handling serialization and deserialization of type information,
 * needed for handling of polymorphic types (or sometimes just for linking
 * abstract types to concrete types)
 * 
 * @since 1.5
 */
public @interface JsonTypeResolver {
    public Class<? extends TypeResolverBuilder<?>> value();
}
