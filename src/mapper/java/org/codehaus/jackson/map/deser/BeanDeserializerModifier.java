package org.codehaus.jackson.map.deser;

import java.util.List;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.deser.*;

/**
 * Abstract class that defines API for objects that can be registered (for {@link BeanDeserializerFactory}
 * to participate in constructing {@link BeanDeserializer} instances.
 * This is typically done by modules that want alter some aspects of deserialization
 * process; and is preferable to sub-classing of {@link BeanDeserializerFactory}.
 *<p>
 * Sequence in which callback methods are called is as follows:
 * <ol>
 * </ol>
 *<p>
 * Default method implementations are "no-op"s, meaning that methods are implemented
 * but have no effect.
 * 
 * @since 1.7
 */
public abstract class BeanDeserializerModifier
{
   /**
    * Method called by {@link BeanDeserializerFactory} with tentative set
    * of discovered properties.
    * Implementations can add, remove or replace any of passed properties.
    *
    * Properties <code>List</code> passed as argument is modifiable, and returned List must
    * likewise be modifiable as it may be passed to multiple registered
    * modifiers.
    */
   public List<?> changeProperties(DeserializationConfig config,
           BasicBeanDescription beanDesc, List<?> beanProperties) {
       return beanProperties;
   }
}