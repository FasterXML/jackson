package org.codehaus.jackson.xml;

import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializer;

/**
 * Specific sub-class of {@link BeanSerializer} needed to take care
 * of some xml-specific aspects, such as distinction between attributes
 * and elements.
 */
public class XmlBeanSerializer extends BeanSerializer
{
    public XmlBeanSerializer(Class<?> type, BeanPropertyWriter[] props,
            BeanPropertyWriter[] filteredProps)
    {
        super(type, props, filteredProps);
    }

    @Override
    public BeanSerializer withFiltered(BeanPropertyWriter[] filtered)
    {
        if (filtered == null) {
            return this;
        }
        return new XmlBeanSerializer(_class, _props, filtered);
    }
}
