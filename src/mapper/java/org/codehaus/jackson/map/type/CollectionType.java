package org.codehaus.jackson.map.type;

import org.codehaus.jackson.type.JavaType;

/**
 * Type that represents Java Collection types (Lists, Sets).
 */
public final class CollectionType
    extends CollectionLikeType
{
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    private CollectionType(Class<?> collT, JavaType elemT)
    {
        super(collT,  elemT);
    }

    @Override
    protected JavaType _narrow(Class<?> subclass) {
        return new CollectionType(subclass, _elementType);
    }

    @Override
    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _elementType.getRawClass()) {
            return this;
        }
        return new CollectionType(_class, _elementType.narrowBy(contentClass)).copyHandlers(this);
    }

    @Override
    public JavaType widenContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _elementType.getRawClass()) {
            return this;
        }
        return new CollectionType(_class, _elementType.widenBy(contentClass)).copyHandlers(this);
    }
    
    public static CollectionType construct(Class<?> rawType, JavaType elemT)
    {
        // nominally component types will be just Object.class
        return new CollectionType(rawType, elemT);
    }

    // Since 1.7:
    @Override
    public CollectionType withTypeHandler(Object h)
    {
        CollectionType newInstance = new CollectionType(_class, _elementType);
        newInstance._typeHandler = h;
        return newInstance;
    }

    // Since 1.7:
    @Override
    public CollectionType withContentTypeHandler(Object h)
    {
        return new CollectionType(_class, _elementType.withTypeHandler(h));
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public String toString()
    {
        return "[collection type; class "+_class.getName()+", contains "+_elementType+"]";
    }
}
