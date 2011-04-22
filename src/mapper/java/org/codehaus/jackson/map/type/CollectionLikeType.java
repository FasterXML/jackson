package org.codehaus.jackson.map.type;

import java.util.Collection;

import org.codehaus.jackson.type.JavaType;

/**
 * Type that represents things that act similar to {@link java.util.Collection};
 * but may or may not be instances of that interface.
 * This specifically allows framework to check for configuration and annotation
 * settings used for Map types, and pass these to custom handlers that may be more
 * familiar with actual type.
 *
 * @since 1.8
 */
public class CollectionLikeType extends TypeBase
{
    /**
     * Type of elements in collection
     */
    protected final JavaType _elementType;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected CollectionLikeType(Class<?> collT, JavaType elemT)
    {
        super(collT,  elemT.hashCode());
        _elementType = elemT;
    }

    @Override
    protected JavaType _narrow(Class<?> subclass) {
        return new CollectionLikeType(subclass, _elementType);
    }

    @Override
    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _elementType.getRawClass()) {
            return this;
        }
        return new CollectionLikeType(_class, _elementType.narrowBy(contentClass)).copyHandlers(this);
    }

    @Override
    public JavaType widenContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _elementType.getRawClass()) {
            return this;
        }
        return new CollectionLikeType(_class, _elementType.widenBy(contentClass)).copyHandlers(this);
    }
    
    public static CollectionLikeType construct(Class<?> rawType, JavaType elemT)
    {
        // nominally component types will be just Object.class
        return new CollectionLikeType(rawType, elemT);
    }

    // Since 1.7:
    @Override
    public CollectionLikeType withTypeHandler(Object h)
    {
        CollectionLikeType newInstance = new CollectionLikeType(_class, _elementType);
        newInstance._typeHandler = h;
        return newInstance;
    }

    // Since 1.7:
    @Override
    public CollectionLikeType withContentTypeHandler(Object h)
    {
        return new CollectionLikeType(_class, _elementType.withTypeHandler(h));
    }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    @Override
    public boolean isContainerType() { return true; }

    @Override
    public boolean isCollectionLikeType() { return true; }
    
    @Override
    public JavaType getContentType() { return _elementType; }

    @Override
    public int containedTypeCount() { return 1; }

    @Override
    public JavaType containedType(int index) {
            return (index == 0) ? _elementType : null;
    }

    /**
     * Not sure if we should count on this, but type names
     * for core interfaces use "E" for element type
     */
    @Override
    public String containedTypeName(int index) {
        if (index == 0) return "E";
        return null;
    }

    @Override
    public StringBuilder getErasedSignature(StringBuilder sb) {
        return _classSignature(_class, sb, true);
    }
    
    @Override
    public StringBuilder getGenericSignature(StringBuilder sb) {
        _classSignature(_class, sb, false);
        sb.append('<');
        _elementType.getGenericSignature(sb);
        sb.append(">;");
        return sb;
    }
    
    @Override
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
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Method that can be used for checking whether this type is a
     * "real" Collection type; meaning whether it represents a parameterized
     * subtype of {@link java.util.Collection} or just something that acts
     * like one.
     * 
     * @since 1.8
     */
    public boolean isTrueCollectionType() {
        return Collection.class.isAssignableFrom(_class);
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        CollectionLikeType other = (CollectionLikeType) o;
        return  (_class == other._class) && _elementType.equals(other._elementType);
    }

    @Override
    public String toString()
    {
        return "[collection-like type; class "+_class.getName()+", contains "+_elementType+"]";
    }

}
