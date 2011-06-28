package org.codehaus.jackson.map.deser.impl;

import java.util.*;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.deser.SettableBeanProperty;
import org.codehaus.jackson.map.deser.ValueInstantiator;
import org.codehaus.jackson.map.introspect.*;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;

/**
 * Container class for storing information on creators (based on annotations,
 * visibility), to be able to build actual instantiator later on.
 */
public class CreatorCollector
{
    /// Type of bean being created
    final BasicBeanDescription _beanDesc;

    final boolean _canFixAccess;

    protected AnnotatedConstructor _defaultConstructor;
    
    AnnotatedMethod _strFactory, _intFactory, _longFactory;
    AnnotatedMethod _delegatingFactory;
    AnnotatedMethod _propertyBasedFactory;
    SettableBeanProperty[] _propertyBasedFactoryProperties = null;

    AnnotatedConstructor _strConstructor, _intConstructor, _longConstructor;
    AnnotatedConstructor _delegatingConstructor;
    AnnotatedConstructor _propertyBasedConstructor;
    SettableBeanProperty[] _propertyBasedConstructorProperties = null;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public CreatorCollector(BasicBeanDescription beanDesc, boolean canFixAccess)
    {
        _beanDesc = beanDesc;
        _canFixAccess = canFixAccess;
    }

    /**
     * @since 1.9.0
     */
    public ValueInstantiator constructValueInstantiator(DeserializationConfig config)
    {
        StdValueInstantiator inst = new StdValueInstantiator(config, _beanDesc.getType());

        // then for with-args ("properties-based") construction
        AnnotatedWithParams withArgsCreator = null;
        SettableBeanProperty[] constructorArgs = null;

        if (_propertyBasedConstructor != null) {
            constructorArgs = _propertyBasedConstructorProperties;
            withArgsCreator = _propertyBasedConstructor;
        } else if (_propertyBasedFactory != null) {
            constructorArgs = _propertyBasedFactoryProperties;
            withArgsCreator = _propertyBasedFactory;
        }

        AnnotatedWithParams delegateCreator = null;
        JavaType delegateType = null;
        TypeBindings bindings = _beanDesc.bindingsForBeanType();
        
        if (_delegatingConstructor != null) {
            delegateCreator = _delegatingConstructor;
            delegateType =  bindings.resolveType(_delegatingConstructor.getParameterType(0));
        } else if (_delegatingFactory != null) {
            delegateCreator = _delegatingFactory;
            delegateType =  bindings.resolveType(_delegatingFactory.getParameterType(0));
        }
        
        inst.configureFromObjectSettings(_defaultConstructor,
                delegateCreator, delegateType,
                withArgsCreator, constructorArgs);
        inst.configureFromStringSettings(_strConstructor, _strFactory);
        inst.configureFromIntSettings(_intConstructor, _intFactory);
        inst.configureFromLongSettings(_longConstructor, _longFactory);
        return inst;
    }
    
    /*
    /**********************************************************
    /* Setters
    /**********************************************************
     */

    public void setDefaultConstructor(AnnotatedConstructor ctor) {
        _defaultConstructor = ctor;
    }
    
    public void addStringConstructor(AnnotatedConstructor ctor) {
        _strConstructor = verifyNonDup(ctor, _strConstructor, "String");
    }
    public void addIntConstructor(AnnotatedConstructor ctor) {
        _intConstructor = verifyNonDup(ctor, _intConstructor, "int");
    }
    public void addLongConstructor(AnnotatedConstructor ctor) {
        _longConstructor = verifyNonDup(ctor, _longConstructor, "long");
    }

    public void addDelegatingConstructor(AnnotatedConstructor ctor) {
        _delegatingConstructor = verifyNonDup(ctor, _delegatingConstructor, "delegate");
    }

    public void addPropertyConstructor(AnnotatedConstructor ctor, SettableBeanProperty[] properties)
    {
        _propertyBasedConstructor = verifyNonDup(ctor, _propertyBasedConstructor, "property-based");
        // [JACKSON-470] Better ensure we have no duplicate names either...
        if (properties.length > 1) {
            HashMap<String,Integer> names = new HashMap<String,Integer>();
            for (int i = 0, len = properties.length; i < len; ++i) {
                String name = properties[i].getName();
                Integer old = names.put(name, Integer.valueOf(i));
                if (old != null) {
                    throw new IllegalArgumentException("Duplicate creator property \""+name+"\" (index "+old+" vs "+i+")");
                }
            }
        }
        _propertyBasedConstructorProperties = properties;
    }

    public void addStringFactory(AnnotatedMethod factory) {
        _strFactory = verifyNonDup(factory, _strFactory, "String");
    }
    public void addIntFactory(AnnotatedMethod factory) {
        _intFactory = verifyNonDup(factory, _intFactory, "int");
    }
    public void addLongFactory(AnnotatedMethod factory) {
        _longFactory = verifyNonDup(factory, _longFactory, "long");
    }

    public void addDelegatingFactory(AnnotatedMethod factory) {
        _delegatingFactory = verifyNonDup(factory, _delegatingFactory, "delegate");
    }

    public void addPropertyFactory(AnnotatedMethod factory, SettableBeanProperty[] properties)
    {
        _propertyBasedFactory = verifyNonDup(factory, _propertyBasedFactory, "property-based");
        _propertyBasedFactoryProperties = properties;
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected AnnotatedConstructor verifyNonDup(AnnotatedConstructor newOne, AnnotatedConstructor oldOne,
            String type)
    {
        if (oldOne != null) {
            throw new IllegalArgumentException("Conflicting "+type+" constructors: already had "+oldOne+", encountered "+newOne);
        }
        if (_canFixAccess) {
            ClassUtil.checkAndFixAccess(newOne.getAnnotated());
        }
        return newOne;
    }
        
    protected AnnotatedMethod verifyNonDup(AnnotatedMethod newOne, AnnotatedMethod oldOne,
                                           String type)
    {
        if (oldOne != null) {
            throw new IllegalArgumentException("Conflicting "+type+" factory methods: already had "+oldOne+", encountered "+newOne);
        }
        if (_canFixAccess) {
            ClassUtil.checkAndFixAccess(newOne.getAnnotated());
        }
        return newOne;
    }
}
