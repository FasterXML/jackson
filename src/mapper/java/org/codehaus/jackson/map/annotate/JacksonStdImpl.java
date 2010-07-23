package org.codehaus.jackson.map.annotate;

/**
 * Marker interface used to indicate implementation classes
 * (serializers, deserializers etc) that are standard ones Jackson
 * uses; not custom ones that application has added. It can be
 * added in cases where certain optimizations can be made if
 * default instances are uses; for example when handling conversions
 * of "natural" JSON types like Strings, booleans and numbers.
 * 
 * @since 1.6
 */
public @interface JacksonStdImpl {

}
