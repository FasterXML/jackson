package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.TypeSerializer;

/**
 * Intermediate base class for types that contain element(s) of
 * other types. Used for example for List, Map, Object array and
 * Iterator serializers.
 * 
 * @since 1.5
 */
public abstract class ContainerSerializerBase<T>
    extends SerializerBase<T>
{
    /**
     * Type serializer used for values, if any.
     */
    protected TypeSerializer _valueTypeSerializer;    

    /*
    /********************************************* 
    /* Construction
    /********************************************* 
     */
    
    protected ContainerSerializerBase(Class<T> t) {
        super(t);
    }

    /**
     * Alternate constructor that is (alas!) needed to work
     * around kinks of generic type handling
     * 
     * @param t
     */
    protected ContainerSerializerBase(Class<?> t, boolean dummy) {
        super(t, dummy);
    }

    public void setValueTypeSerializer(TypeSerializer valueTypeSer) {
        _valueTypeSerializer = valueTypeSer;
    }
    
    /*
    /********************************************* 
    /* Construction
    /********************************************* 
     */
    
}
