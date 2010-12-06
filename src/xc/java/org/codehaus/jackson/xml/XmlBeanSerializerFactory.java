package org.codehaus.jackson.xml;

import java.util.ArrayList;
import java.util.List;

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
    
    protected XmlBeanSerializerFactory(Serializers[] allAdditionalSerializers)
    {
        super(allAdditionalSerializers);
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
        
        Serializers[] s = ArrayBuilders.insertInList(_additionalSerializers, additional);
        return new XmlBeanSerializerFactory(s);
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
        // Ok: how many attributes do we have to write?
        int attrCount = 0;

        for (BeanPropertyWriter bpw : properties) {
            if (!_isAttribute(bpw)) {
                break;
            }
            ++attrCount;
        }
        return new XmlBeanSerializer(beanDesc.getBeanClass(), writers, null, attrCount);
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
        BeanPropertyWriter propertyWriter = super._constructWriter(config, typeContext, pb, staticTyping, name, propertyMember);
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        String ns = intr.findNamespace(propertyMember);
        Boolean isAttribute = intr.isOutputAsAttribute(propertyMember);
        propertyWriter.setInternalSetting(KEY_XML_INFO, new XmlInfo(isAttribute, ns));
        return propertyWriter;
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    protected boolean _isAttribute(BeanPropertyWriter bpw)
    {
        XmlInfo info = (XmlInfo) bpw.getInternalSetting(KEY_XML_INFO);
        return (info != null && info.isAttribute());
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
