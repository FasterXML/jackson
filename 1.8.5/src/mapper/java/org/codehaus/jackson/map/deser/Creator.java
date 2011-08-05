package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.AnnotatedConstructor;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Container for different kinds of Creators; objects capable of instantiating
 * (and possibly partially or completely initializing) POJOs.
 */
abstract class Creator
{
    // Class only used for namespacing, not as base class
    private Creator() { }

    /**
     * Creator implementation that can handle simple deserialization from
     * Json String values.
     */
    final static class StringBased
    {
        protected final Class<?> _valueClass;
        protected final Method _factoryMethod;
        protected final Constructor<?> _ctor;

        public StringBased(Class<?> valueClass, AnnotatedConstructor ctor,
                             AnnotatedMethod factoryMethod)
        {
            _valueClass = valueClass;
            _ctor = (ctor == null) ? null : ctor.getAnnotated();
            _factoryMethod = (factoryMethod == null) ? null : factoryMethod.getAnnotated();
        }

        public Object construct(String value)
        {
            try {
                if (_ctor != null) {
                    return _ctor.newInstance(value);
                }
                if (_factoryMethod != null) {
                    return _factoryMethod.invoke(_valueClass, value);
                }
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e);
            }
            return null;
        }
    }

    /**
     * Creator implementation that can handle simple deserialization from
     * Json Number values.
     */
    final static class NumberBased
    {
        protected final Class<?> _valueClass;

        protected final Constructor<?> _intCtor;
        protected final Constructor<?> _longCtor;

        protected final Method _intFactoryMethod;
        protected final Method _longFactoryMethod;

        public NumberBased(Class<?> valueClass,
                          AnnotatedConstructor intCtor, AnnotatedMethod ifm,
                          AnnotatedConstructor longCtor, AnnotatedMethod lfm)
        {
            _valueClass = valueClass;
            _intCtor = (intCtor == null) ? null : intCtor.getAnnotated(); 
            _longCtor = (longCtor == null) ? null : longCtor.getAnnotated();
            _intFactoryMethod = (ifm == null) ? null : ifm.getAnnotated();
            _longFactoryMethod = (lfm == null) ? null : lfm.getAnnotated();
        }

        public Object construct(int value)
        {
            // First: "native" int methods work best:
            try {
                if (_intCtor != null) {
                    return _intCtor.newInstance(value);
                }
                if (_intFactoryMethod != null) {
                    return _intFactoryMethod.invoke(_valueClass, Integer.valueOf(value));
                }
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e);
            }
            // but if not, can do widening conversion
            return construct((long) value);
        }

        public Object construct(long value)
        {
            /* For longs we don't even try casting down to ints;
             * theoretically could try if value fits... but let's
             * leave that as a future improvement
             */
            try {
                if (_longCtor != null) {
                    return _longCtor.newInstance(value);
                }
                if (_longFactoryMethod != null) {
                    return _longFactoryMethod.invoke(_valueClass, Long.valueOf(value));
                }
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e);
            }
            return null;
        }
    }

    /**
     * Creator implementation used for cases where parts of deserialization
     * are delegated to another serializer, by first binding to an intermediate
     * object, and then passing that object to the delegate creator (and
     * then deserializer).
     */
    final static class Delegating
    {
        /**
         * Annotated creator object (single-argument constructor or
         * single-argument static method) that is used for instantation;
         * as well as for providing contextual information.
         */
        protected final AnnotatedMember _creator;
        
	/**
	 * Type to deserialize JSON to, as well as the type to pass to
	 * creator (constructor, factory method)
	 */
	protected final JavaType _valueType;

        protected final Constructor<?> _ctor;
        protected final Method _factoryMethod;

	/**
	 * Delegate deserializer to use for actual deserialization, before
	 * instantiating value
	 */
	protected JsonDeserializer<Object> _deserializer;

        public Delegating(BasicBeanDescription beanDesc, AnnotatedConstructor ctor, AnnotatedMethod factory)
        {
            TypeBindings bindings = beanDesc.bindingsForBeanType();
            if (ctor != null) {
                _creator = ctor;
                _ctor = ctor.getAnnotated();
                _factoryMethod = null;
                _valueType = bindings.resolveType(ctor.getParameterType(0));
            } else if (factory != null) {
                _creator = factory;
                _ctor = null;
                _factoryMethod = factory.getAnnotated();
                _valueType = bindings.resolveType(factory.getParameterType(0));
            } else {
                throw new IllegalArgumentException("Internal error: neither delegating constructor nor factory method passed");
            }
	}

	public JavaType getValueType() { return _valueType; }

	public AnnotatedMember getCreator() { return _creator; }
	
	public void setDeserializer(JsonDeserializer<Object> deser)
	{
	    _deserializer = deser;
	}

	public Object deserialize(JsonParser jp, DeserializationContext ctxt)
	    throws IOException, JsonProcessingException
	{
	    Object value = _deserializer.deserialize(jp, ctxt);
            try {
                if (_ctor != null) {
                    return _ctor.newInstance(value);
                }
                // static method, 'obj' can be null
		return _factoryMethod.invoke(null, value);
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e);
		return null;
            }
	}
    }

    /**
     * Creator implementation used to handle details of using a "non-default"
     * creator (constructor or factory that takes one or more arguments
     * that represent logical bean properties)
     */
    final static class PropertyBased
    {
        protected final Constructor<?> _ctor;
        protected final Method _factoryMethod;
        
        /**
         * Map that contains property objects for either constructor or factory
         * method (whichever one is null: one property for each
         * parameter for that one), keyed by logical property name
         */
        protected final HashMap<String, SettableBeanProperty> _properties;

        /**
         * If some property values must always have a non-null value (like
         * primitive types do), this array contains such default values.
         */
        protected final Object[]  _defaultValues;
        
        public PropertyBased(AnnotatedConstructor ctor, SettableBeanProperty[] ctorProps,
                AnnotatedMethod factory, SettableBeanProperty[] factoryProps)
        {
            // We will only use one: and constructor has precedence over factory
            SettableBeanProperty[] props;
            if (ctor != null) {
                _ctor = ctor.getAnnotated();
                _factoryMethod = null;
                props = ctorProps;
            } else if (factory != null) {
                _ctor = null;
                _factoryMethod = factory.getAnnotated();
                props = factoryProps;
            } else {
                throw new IllegalArgumentException("Internal error: neither delegating constructor nor factory method passed");
            }
            _properties = new HashMap<String, SettableBeanProperty>();
            // [JACKSON-372]: primitive types need extra care
            Object[] defValues = null;
            for (int i = 0, len = props.length; i < len; ++i) {
                SettableBeanProperty prop = props[i];
                _properties.put(prop.getName(), prop);
                if (prop.getType().isPrimitive()) {
                    if (defValues == null) {
                        defValues = new Object[len];
                    }
                    defValues[i] = ClassUtil.defaultValue(prop.getType().getRawClass());
                }
            }
            _defaultValues = defValues;
        }
        
        public Collection<SettableBeanProperty> properties() {
            return _properties.values();
        }
        
        public SettableBeanProperty findCreatorProperty(String name) {
            return _properties.get(name);
        }
        
        /**
         * Method called when starting to build a bean instance.
         */
        public PropertyValueBuffer startBuilding(JsonParser jp, DeserializationContext ctxt)
        {
            return new PropertyValueBuffer(jp, ctxt, _properties.size());
        }
        
        public Object build(PropertyValueBuffer buffer)
            throws Exception
        {
            Object bean;
            try {
                if (_ctor != null) {
                    bean = _ctor.newInstance(buffer.getParameters(_defaultValues));
                } else {
                    bean =  _factoryMethod.invoke(null, buffer.getParameters(_defaultValues));
                }
            } catch (Exception e) {
                ClassUtil.throwRootCause(e);
                return null; // never gets here
            }
            // Anything buffered?
            for (PropertyValue pv = buffer.buffered(); pv != null; pv = pv.next) {
                pv.assign(bean);
            }
            return bean;
        }
    }
}
