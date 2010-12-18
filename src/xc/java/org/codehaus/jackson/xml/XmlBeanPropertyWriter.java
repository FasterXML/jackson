package org.codehaus.jackson.xml;

import javax.xml.namespace.QName;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.impl.PropertySerializerMap;

/**
 * Property writer sub-class used for handling element wrapping needed for serializing
 * collection (array, Collection; possibly Map) types.
 * 
 * @since 1.7
 */
public class XmlBeanPropertyWriter
    extends BeanPropertyWriter
{
    /*
    /**********************************************************
    /* Config settings
    /**********************************************************
     */

    /**
     * Element name used as wrapper for collection.
     */
    protected final QName _wrapperName;

    /**
     * Element name used for items in the collection
     */
    protected final QName _wrappedName;
    
    /*
    /**********************************************************
    /* Life-cycle: construction, configuration
    /**********************************************************
     */

    public XmlBeanPropertyWriter(BeanPropertyWriter wrapped, QName wrapperName, QName wrappedName)
    {
        super(wrapped);
        _wrapperName = wrapperName;
        _wrappedName = wrappedName;
    }

    public XmlBeanPropertyWriter(BeanPropertyWriter wrapped, QName wrapperName, QName wrappedName,
            JsonSerializer<Object> serializer)
    {
        super(wrapped, serializer);
        _wrapperName = wrapperName;
        _wrappedName = wrappedName;
    }
    
    @Override
    public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
    {
        // sanity check to ensure sub-classes override...
        if (getClass() != XmlBeanPropertyWriter.class) {
            throw new IllegalStateException("Sub-class does not override 'withSerializer()'; needs to!");
        }
        return new XmlBeanPropertyWriter(this, _wrapperName, _wrappedName, ser);
    }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    /**
     * 
     */
    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        Object value = get(bean);
        /* Hmmh. Does the default null serialization work ok here? For now let's assume
         * it does; can change later if not.
         */
        if (value == null) {
            if (!_suppressNulls) {
                jgen.writeFieldName(_name);
                prov.defaultSerializeNull(jgen);
            }
            return;
        }
        // For non-nulls, first: simple check for direct cycles
        if (value == bean) {
            _reportSelfReference(bean);
        }
        if (_suppressableValue != null && _suppressableValue.equals(value)) {
            return;
        }

        // Ok then; addition we want to do is to add wrapper element, and that's what happens here
        ToXmlGenerator xmlGen = (ToXmlGenerator) jgen;
        xmlGen.startWrappedValue(_wrapperName, _wrappedName);
        
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, prov);
            }
        }
        jgen.writeFieldName(_name);
        if (_typeSerializer == null) {
            ser.serialize(value, jgen, prov);
        } else {
            ser.serializeWithType(value, jgen, prov, _typeSerializer);
        }

        xmlGen.finishWrappedValue(_wrapperName, _wrappedName);
    }
    
}
