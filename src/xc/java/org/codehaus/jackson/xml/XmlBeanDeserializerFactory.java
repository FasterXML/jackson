package org.codehaus.jackson.xml;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.*;
import org.codehaus.jackson.map.util.ArrayBuilders;

public class XmlBeanDeserializerFactory  extends BeanDeserializerFactory
{
    /*
    /**********************************************************
    /* Life-cycle: creation, configuration
    /**********************************************************
     */
    
    protected XmlBeanDeserializerFactory(Deserializers[] allAdditionalDeserializers)
    {
        super(allAdditionalDeserializers);
    }

    @Override
    public XmlBeanDeserializerFactory withAdditionalDeserializers(Deserializers additional)
    {
        if (additional == null) {
            throw new IllegalArgumentException("Can not pass null Serializers");
        }
        // Same as super-class, we require all sub-classes to override this method:
        if (getClass() != XmlBeanDeserializerFactory.class) {
            throw new IllegalStateException("Subtype of XmlBeanSerializerFactory ("+getClass().getName()
                    +") has not properly overridden method 'withAdditionalSerializers': can not instantiate subtype with "
                    +"additional serializer definitions");
        }
        
        Deserializers[] s = ArrayBuilders.insertInList(_additionalDeserializers, additional);
        return new XmlBeanDeserializerFactory(s);
    }

    /*
    /**********************************************************
    /* Overridden methods from standard bean deserializer factory
    /**********************************************************
     */

}
