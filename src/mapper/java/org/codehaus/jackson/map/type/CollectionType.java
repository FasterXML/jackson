package org.codehaus.jackson.map.type;

import org.codehaus.jackson.type.JavaType;

/**
 * Type that represents Java Collection types (Lists, Sets).
 */
public final class CollectionType
    extends TypeBase
{
    /**
     * Type of elements in collection
     */
    final JavaType _elementType;

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    private CollectionType(Class<?> collT, JavaType elemT)
    {
        super(collT);
        _elementType = elemT;
        _hashCode += elemT.hashCode();
    }

    protected JavaType _narrow(Class<?> subclass)
    {
        return new CollectionType(subclass, _elementType);
    }

    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _elementType.getRawClass()) {
            return this;
        }
        JavaType newElementType = _elementType.narrowBy(contentClass);
        return new CollectionType(_class, newElementType).copyHandlers(this);
    }

    public static CollectionType construct(Class<?> rawType, JavaType elemT)
    {
        // nominally component types will be just Object.class
        return new CollectionType(rawType, elemT);
    }

    protected String buildCanonicalName() {
        StringBuilder sb = new StringBuilder();
        sb.append(_class.getName());
        if (_elementType != null) {
            sb.append('<');
            sb.append(_elementType.toCanonical());
            sb.append('>');
        }
        return sb.toString();
    }
    
    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    public JavaType getContentType() { return _elementType; }
    public int containedTypeCount() { return 1; }
    public JavaType containedType(int index) {
            return (index == 0) ? _elementType : null;
    }

    /**
     * Not sure if we should count on this, but type names
     * for core interfaces use "E" for element type
     */
    public String containedTypeName(int index) {
        if (index == 0) return "E";
        return null;
    }

    /*
    //////////////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////////////
     */

    public boolean isContainerType() { return true; }

    /*
    //////////////////////////////////////////////////////////
    // Standard methods
    //////////////////////////////////////////////////////////
     */

    @Override
        public String toString()
    {
        return "[collection type; class "+_class.getName()+", contains "+_elementType+"]";
    }

    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        CollectionType other = (CollectionType) o;
        return  (_class == other._class)
            && _elementType.equals(other._elementType);
    }
}
