package org.codehaus.jackson.map.deser;

import java.io.IOException;
import org.codehaus.jackson.JsonProcessingException;

/**
 * Base class for property values that need to be buffered during
 * deserialization.
 */
abstract class PropertyValue
{
    public final PropertyValue next;
    public final Object value;
    
    protected PropertyValue(PropertyValue next, Object value)
    {
        this.next = next;
        this.value = value;
    }

    /**
     * Method called to assign stored value of this property to specified
     * bean isntance
     */
    public abstract void assign(Object bean)
        throws IOException, JsonProcessingException;

    /*
    /////////////////////////////////////////////////////
    // Concrete property value classes
    /////////////////////////////////////////////////////
    */

    final static class Regular
        extends PropertyValue
    {
        final SettableBeanProperty _property;
        
        public Regular(PropertyValue next, Object value,
                       SettableBeanProperty prop)
        {
            super(next, value);
            _property = prop;
        }

        public void assign(Object bean)
            throws IOException, JsonProcessingException
        {
            _property.set(bean, value);
        }
    }
    
    final static class Any
        extends PropertyValue
    {
        final SettableAnyProperty _property;
        final String _propertyName;
        
        public Any(PropertyValue next, Object value,
                   SettableAnyProperty prop,
                   String propName)
        {
            super(next, value);
            _property = prop;
            _propertyName = propName;
        }

        public void assign(Object bean)
            throws IOException, JsonProcessingException
        {
            _property.set(bean, _propertyName, value);
        }
    }
}
