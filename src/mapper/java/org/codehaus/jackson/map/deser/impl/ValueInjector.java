package org.codehaus.jackson.map.deser.impl;

import java.io.IOException;

import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.SettableBeanProperty;

/**
 * Class that encapsulates details of value injection that occurs before
 * deserialization of a POJO. Details include information needed to find
 * injectable value (logical id) as well as method used for assigning
 * value (setter or field)
 * 
 * @since 1.9
 */
public class ValueInjector
{
    protected final Object _valueId;

    protected final SettableBeanProperty _property;
    
    public ValueInjector(Object valueId, SettableBeanProperty prop)
    {
        _valueId = valueId;
        _property = prop;
    }

    public Object findValue(DeserializationContext context, Object beanInstance)
    {
        return context.findInjectableValue(_valueId, _property, beanInstance);
    }
    
    public void inject(DeserializationContext context, Object beanInstance)
        throws IOException
    {
        _property.set(beanInstance, findValue(context, beanInstance));
    }
}