package org.codehaus.jackson.map.introspect;

/**
 * Intermediate base class for annotated entities that are members of
 * a class; fields, methods and constructors. This is a superset
 * of things that can represent logical properties as it contains
 * constructores in addition to fields and methods.
 * 
 * @author tatu
 * @since 1.5
 */
public abstract class AnnotatedMember extends Annotated
{
    protected AnnotatedMember() { super(); }
}
