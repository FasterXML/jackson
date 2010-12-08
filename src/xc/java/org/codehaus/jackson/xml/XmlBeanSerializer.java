package org.codehaus.jackson.xml;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializer;

/**
 * Specific sub-class of {@link BeanSerializer} needed to take care
 * of some xml-specific aspects, such as distinction between attributes
 * and elements.
 */
public class XmlBeanSerializer extends BeanSerializer
{
    /**
     * Number of attributes to write; these will have been ordered to be the first
     * properties to write.
     */
    protected final int _attributeCount;

    /**
     * Array that contains namespace URIs associated with properties, if any;
     * null if no namespace definitions have been assigned
     */
    protected final String[] _namespaces;
    
    public XmlBeanSerializer(Class<?> type, BeanPropertyWriter[] props,
            BeanPropertyWriter[] filteredProps, int attrCount, String[] ns)
    {
        super(type, props, filteredProps);
        _attributeCount = attrCount;
        _namespaces = ns;
    }

    @Override
    public BeanSerializer withFiltered(BeanPropertyWriter[] filtered)
    {
        if (filtered == null) {
            return this;
        }
        return new XmlBeanSerializer(_class, _props, filtered, _attributeCount, _namespaces);
    }

    /**
     * Main serialization method needs to be overridden to allow XML-specific
     * extra handling, such as indication of whether to write attributes or
     * elements.
     */
    @Override
    protected void serializeFields(Object bean, JsonGenerator jgen0, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final ToXmlGenerator jgen = (ToXmlGenerator) jgen0;
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getSerializationView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
    
        final int attrCount = _attributeCount;
        if (attrCount > 0) {
            jgen.setNextIsAttribute(true);
        }
        final String[] namespaces = _namespaces;
        
        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                String ns = (namespaces == null) ? null : namespaces[i];
                if (i == attrCount) {
                    jgen.setNextIsAttribute(false);
                }
                jgen.setNextNamespace(ns);
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    prop.serializeAsField(bean, jgen, provider);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndSerialize(bean, jgen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(e, bean, name);
        } catch (StackOverflowError e) {
            /* 04-Sep-2009, tatu: Dealing with this is tricky, since we do not
             *   have many stack frames to spare... just one or two; can't
             *   make many calls.
             */
            JsonMappingException mapE = new JsonMappingException("Infinite recursion (StackOverflowError)");
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }

}
