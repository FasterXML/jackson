package org.codehaus.jackson.xml;

import java.util.*;

import javax.xml.namespace.QName;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.Serializers;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializer;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.codehaus.jackson.map.ser.PropertyBuilder;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.util.ArrayBuilders;

/**
 * Specialized version of {@link BeanSerializerFactory} which is used to
 * add specific overrides to handle XML-specific details such as
 * difference between attributes and elements.
 * 
 * @since 1.7
 */
public class XmlBeanSerializerFactory extends BeanSerializerFactory
{
    /**
     * Marker used for storing associated internal data with {@link BeanPropertyWriter}
     * instances; to mark instances that are to be written out as attributes.
     * Created as separate non-interned String to ensure there are no collisions.
     */
    protected final static String KEY_XML_INFO = new String("xmlInfo");
    
    /*
    /**********************************************************
    /* Life-cycle: creation, configuration
    /**********************************************************
     */
    
    protected XmlBeanSerializerFactory(Config config)
    {
        super(config);
    }

    @Override
    public SerializerFactory withAdditionalSerializers(Serializers additional)
    {
        if (additional == null) {
            throw new IllegalArgumentException("Can not pass null Serializers");
        }
        // Same as super-class, we require all sub-classes to override this method:
        if (getClass() != XmlBeanSerializerFactory.class) {
            throw new IllegalStateException("Subtype of XmlBeanSerializerFactory ("+getClass().getName()
                    +") has not properly overridden method 'withAdditionalSerializers': can not instantiate subtype with "
                    +"additional serializer definitions");
        }
        return new XmlBeanSerializerFactory(_config.withAdditionalSerializers(additional));
    }

    /*
    /**********************************************************
    /* Overridden methods from standard bean serializer factory
    /**********************************************************
     */

    @Override
    protected BeanSerializer instantiateBeanSerializer(SerializationConfig config,
            BasicBeanDescription beanDesc,
            List<BeanPropertyWriter> properties)
    {
        BeanPropertyWriter[] writers = properties.toArray(new BeanPropertyWriter[properties.size()]);
        // Ok: how many attributes do we have to write? namespaces?
        QName[] xmlNames = new QName[properties.size()];
        int attrCount = 0;
        int i = 0;
        for (BeanPropertyWriter bpw : properties) {
            XmlInfo info = (XmlInfo) bpw.getInternalSetting(KEY_XML_INFO);
            String ns = null;
            if (info != null) {
                if (info.isAttribute()) {
                    ++attrCount;
                }
                ns = info.getNamespace();
            }
            xmlNames[i] = new QName((ns == null) ? "" : ns, bpw.getName());
            ++i;
        }
        return new XmlBeanSerializer(beanDesc.getBeanClass(), writers, null,
                findFilterId(config, beanDesc),
                attrCount, xmlNames);
    }

    /**
     * Need to override to sort properties so that we will always start with attributes (if any),
     * followed by elements.
     */
    @Override
    protected List<BeanPropertyWriter> sortBeanProperties(SerializationConfig config, BasicBeanDescription beanDesc,
            List<BeanPropertyWriter> props)
    {
        props = super.sortBeanProperties(config, beanDesc, props);
        // Ok: let's first see if we any attributes; if not, just return lists as is:
        for (BeanPropertyWriter bpw : props) {
            if (_isAttribute(bpw)) { // Yup: let's build re-ordered list then
                props = _orderAttributesFirst(props);
                break;
            }
        }
        return props;
    }
    
    /**
     * Need to override this specific method so that we can inject XML-specific information
     * into bean writer, so that it can be retrieved later on
     */
    @Override
    protected BeanPropertyWriter _constructWriter(SerializationConfig config, TypeBindings typeContext,
            PropertyBuilder pb, boolean staticTyping, String name, AnnotatedMember propertyMember)
    {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        String ns = findNamespaceAnnotation(intr, propertyMember);
        Boolean isAttribute = findIsAttributeAnnotation(intr, propertyMember);

        BeanPropertyWriter propertyWriter = super._constructWriter(config, typeContext, pb, staticTyping, name, propertyMember);
        propertyWriter.setInternalSetting(KEY_XML_INFO, new XmlInfo(isAttribute, ns));

        // Actually: if we have a Collection type, easiest place to add wrapping would be here...
        Class<?> type = propertyMember.getRawType();
        if (_isContainerType(type)) {
            String localName = null, wrapperNs = null;

            QName wrappedName = new QName(ns, propertyWriter.getName());
            QName wrapperName = findWrapperName(intr, propertyMember);
            if (wrapperName != null) {
                localName = wrapperName.getLocalPart();
                wrapperNs = wrapperName.getNamespaceURI();
            }
            /* Empty/missing localName means "use property name as wrapper"; later on
             * should probably make missing (null) mean "don't add a wrapper"
             */
            if (localName == null || localName.length() == 0) {
                wrapperName = wrappedName;
            } else {
                wrapperName = new QName((wrapperNs == null) ? "" : wrapperNs, localName);
            }
            return new XmlBeanPropertyWriter(propertyWriter, wrapperName, wrappedName);
        }
        
        return propertyWriter;
    }
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    /**
     * Helper method used for figuring out if given raw type is a collection ("indexed") type;
     * in which case a wrapper element is typically added.
     */
    private boolean _isContainerType(Class<?> rawType)
    {
        if (rawType.isArray()) {
            // Just one special case; byte[] will be serialized as base64-encoded String, not real array, so:
            if (rawType == byte[].class) {
                return false;
            }
            return true;
        }
        return Map.class.isAssignableFrom(rawType) || Collection.class.isAssignableFrom(rawType);
    }
    
    private String findNamespaceAnnotation(AnnotationIntrospector ai, AnnotatedMember prop)
    {
        for (AnnotationIntrospector intr : ai.allIntrospectors()) {
            if (intr instanceof XmlAnnotationIntrospector) {
                String ns = ((XmlAnnotationIntrospector) intr).findNamespace(prop);
                if (ns != null) {
                    return ns;
                }
            }
        }
        return null;
    }

    private Boolean findIsAttributeAnnotation(AnnotationIntrospector ai, AnnotatedMember prop)
    {
        for (AnnotationIntrospector intr : ai.allIntrospectors()) {
            if (intr instanceof XmlAnnotationIntrospector) {
                Boolean b = ((XmlAnnotationIntrospector) intr).isOutputAsAttribute(prop);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    private QName findWrapperName(AnnotationIntrospector ai, AnnotatedMember prop)
    {
        for (AnnotationIntrospector intr : ai.allIntrospectors()) {
            if (intr instanceof XmlAnnotationIntrospector) {
                QName n = ((XmlAnnotationIntrospector) intr).findWrapperElement(prop);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }
    
    /**
     * Method for re-sorting lists of bean properties such that attributes are strictly
     * written before elements.
     */
    private List<BeanPropertyWriter> _orderAttributesFirst(List<BeanPropertyWriter> props)
    {
        List<BeanPropertyWriter> attrs = new ArrayList<BeanPropertyWriter>();
        List<BeanPropertyWriter> elems = new ArrayList<BeanPropertyWriter>();
        for (BeanPropertyWriter bpw : props) {
            if (_isAttribute(bpw)) {
                attrs.add(bpw);
            } else {
                elems.add(bpw);
            }
        }
        if (elems.size() > 0) {
            attrs.addAll(elems);
        }
        return attrs;
    }    

    private boolean _isAttribute(BeanPropertyWriter bpw)
    {
        XmlInfo info = (XmlInfo) bpw.getInternalSetting(KEY_XML_INFO);
        return (info != null) && info.isAttribute();
    }
    
    /*
    /**********************************************************
    /* Internal helper classes
    /**********************************************************
     */

    /**
     * Helper container class used to contain xml specific information
     * we need to retain to construct proper bean serializer
     */
    protected final static class XmlInfo
    {
        protected final String _namespace;
        protected final boolean _isAttribute;
        
        public XmlInfo(Boolean isAttribute, String ns)
        {
            _isAttribute = (isAttribute == null) ? false : isAttribute.booleanValue();
            _namespace = (ns == null) ? "" : ns;
        }

        public String getNamespace() { return _namespace; }
        public boolean isAttribute() { return _isAttribute; }
    }
}
