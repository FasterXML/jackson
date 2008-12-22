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

    public CollectionType(Class<?> collT, JavaType elemT)
    {
        super(collT);
        _elementType = elemT;
        _hashCode += elemT.hashCode();
    }

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
