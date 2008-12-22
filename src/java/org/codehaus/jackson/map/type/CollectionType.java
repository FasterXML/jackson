package org.codehaus.jackson.map.type;

/**
 * Type that represents Java Collection types (Lists, Sets).
 */
public final class CollectionType
    extends JavaType
{
    /**
     * Type of elements in collection
     */
    final JavaType _elementType;

    final boolean _fullyTyped;

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    private CollectionType(Class<?> collT, JavaType elemT,
                           boolean fullyTyped)
    {
        super(collT);
        _elementType = elemT;
        _hashCode += elemT.hashCode();
        _fullyTyped = fullyTyped;
    }

    /**
     * Method called to construct a partially typed instance. Partial
     * means that we can not determine component types, due to type
     * erasure. Resulting type may or may not be acceptable to caller.
     */
    public static CollectionType untyped(Class<?> rawType, JavaType elemT)
    {
        // nominally component types will be just Object.class
        return new CollectionType(rawType, elemT, false);
    }

    public static CollectionType typed(Class<?> rawType, JavaType elemT)
    {
        return new CollectionType(rawType, elemT, elemT.isFullyTyped());
    }

    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    public boolean isFullyTyped() { return _fullyTyped; }

    /*
    //////////////////////////////////////////////////////////
    // Standard methods
    //////////////////////////////////////////////////////////
     */

    @Override
        public String toString()
    {
        return "[collection type for "+_elementType+"]";
    }

    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        CollectionType other = (CollectionType) o;
        return _elementType.equals(other._elementType);
    }
}
