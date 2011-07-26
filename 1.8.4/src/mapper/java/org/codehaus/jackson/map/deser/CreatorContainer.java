package org.codehaus.jackson.map.deser;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import org.codehaus.jackson.map.introspect.AnnotatedConstructor;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Container for set of Creators (constructors, factory methods)
 */
public class CreatorContainer
{
    /// Type of bean being created
    final BasicBeanDescription _beanDesc;

//    final Class<?> _beanClass;
    final boolean _canFixAccess;

    protected Constructor<?> _defaultConstructor;
    
    AnnotatedMethod _strFactory, _intFactory, _longFactory;
    AnnotatedMethod _delegatingFactory;
    AnnotatedMethod _propertyBasedFactory;
    SettableBeanProperty[] _propertyBasedFactoryProperties = null;

    AnnotatedConstructor _strConstructor, _intConstructor, _longConstructor;
    AnnotatedConstructor _delegatingConstructor;
    AnnotatedConstructor _propertyBasedConstructor;
    SettableBeanProperty[] _propertyBasedConstructorProperties = null;

    public CreatorContainer(BasicBeanDescription beanDesc, boolean canFixAccess)
    {
        _beanDesc = beanDesc;
        _canFixAccess = canFixAccess;
//        _beanClass = beanClass;
    }

    /*
    /**********************************************************
    /* Setters
    /**********************************************************
     */

    public void setDefaultConstructor(Constructor<?> ctor) {
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
        _delegatingConstructor = verifyNonDup(ctor, _delegatingConstructor, "long");
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
        _delegatingFactory = verifyNonDup(factory, _delegatingFactory, "long");
    }

    public void addPropertyFactory(AnnotatedMethod factory, SettableBeanProperty[] properties)
    {
        _propertyBasedFactory = verifyNonDup(factory, _propertyBasedFactory, "property-based");
        _propertyBasedFactoryProperties = properties;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public Constructor<?> getDefaultConstructor() { return _defaultConstructor; }
    
    public Creator.StringBased stringCreator()
    {
        if (_strConstructor == null &&  _strFactory == null) {
            return null;
        }
        return new Creator.StringBased(_beanDesc.getBeanClass(), _strConstructor, _strFactory);
    }

    public Creator.NumberBased numberCreator()
    {
        if (_intConstructor == null && _intFactory == null
            && _longConstructor == null && _longFactory == null) {
            return null;
        }
        return new Creator.NumberBased(_beanDesc.getBeanClass(), _intConstructor, _intFactory,
                                       _longConstructor, _longFactory);
    }

    public Creator.Delegating delegatingCreator()
    {
        if (_delegatingConstructor == null && _delegatingFactory == null) {
            return null;
        }
        return new Creator.Delegating(_beanDesc, _delegatingConstructor, _delegatingFactory);
    }

    public Creator.PropertyBased propertyBasedCreator()
    {
        if (_propertyBasedConstructor == null && _propertyBasedFactory == null) {
            return null;
        }
        return new Creator.PropertyBased(_propertyBasedConstructor, _propertyBasedConstructorProperties,
                                         _propertyBasedFactory, _propertyBasedFactoryProperties);
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



