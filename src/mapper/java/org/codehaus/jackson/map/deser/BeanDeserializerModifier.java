package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.deser.BeanDeserializer;
import org.codehaus.jackson.map.deser.BeanDeserializerFactory;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;

/**
 * Abstract class that defines API for objects that can be registered (for {@link BeanDeserializerFactory}
 * to participate in constructing {@link BeanDeserializer} instances.
 * This is typically done by modules that want alter some aspects of deserialization
 * process; and is preferable to sub-classing of {@link BeanDeserializerFactory}.
 *<p>
 * Sequence in which callback methods are called is as follows:
 * <ol>
 *  <li>modifyValueInstantiator is called once factory has figured out creators
 *     to use, based on annotations and existence of default constructor
 *   </li>
 *  <li>updateBuilder is called once all initial pieces for building deserializer
 *    have been collected
 *   </li>
 *  <li><code>modifyDeserializer</code> is called after deserializer has been built,
 *    but before it is returned to be used
 *   </li>
 * </ol>
 *<p>
 * Default method implementations are "no-op"s, meaning that methods are implemented
 * but have no effect; this is mostly so that new methods can be added in later
 * versions.
 * 
 * @since 1.7
 */
public abstract class BeanDeserializerModifier
{
    /**
     * Method called by {@link BeanDeserializerFactory} when it has constructed
     * the default value instantiator (initially of type
     * {@link org.codehaus.jackson.map.deser.impl.StdValueInstantiator}, but
     * note that it may already have been replace by another modifier)
     * to use for creating instances of the bean during deserialization.
     * Method can either try to modify instantiator, or replace it with a
     * custom version.
     * 
     * @param config Deserialization configuration
     * @param beanDesc Information about POJO type for which deserializer is being built
     * @param instantiator Default instantiator to use (possibly having been modified
     *   by preceding modifiers)
     * 
     * @return Value instantiator to use for bean
     */
    public ValueInstantiator modifyValueInstantiator(DeserializationConfig config,
            BasicBeanDescription beanDesc, ValueInstantiator instantiator) {
        return instantiator;
    }
    
    /**
     * Method called by {@link BeanDeserializerFactory} when it has collected
     * basic information such as tentative list of properties to deserialize.
     *
     * Implementations may choose to modify state of builder (to affect deserializer being
     * built), or even completely replace it (if they want to build different kind of
     * deserializer). Typically changes mostly concern set of properties to deserialize.
     */
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config,
            BasicBeanDescription beanDesc, BeanDeserializerBuilder builder) {
        return builder;
    }

    /**
     * Method called by {@link BeanDeserializerFactory} after constructing default
     * bean deserializer instance with properties collected and ordered earlier.
     * Implementations can modify or replace given deserializer and return deserializer
     * to use. Note that although initial deserializer being passed is of type
     * {@link BeanDeserializer}, modifiers may return deserializers of other types;
     * and this is why implementations must check for type before casting.
     */
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
            BasicBeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }
}