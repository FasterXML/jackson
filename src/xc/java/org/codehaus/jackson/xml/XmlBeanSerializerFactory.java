package org.codehaus.jackson.xml;

import java.util.List;

import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.Serializers;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializer;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
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
        return new XmlBeanSerializer(beanDesc.getBeanClass(), writers, null);
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

        // !!! TODO: sort
        
        return props;
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */
    
}
