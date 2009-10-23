package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.introspect.AnnotatedConstructor;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Container for set of Creators (constructors, factory methods)
 */
public class CreatorContainer
{
    /// Type of bean being created
    final Class<?> _beanClass;
    final boolean _canFixAccess;

    AnnotatedMethod _strFactory, _intFactory, _longFactory;
    AnnotatedMethod _delegatingFactory;
    AnnotatedMethod _propertyBasedFactory;
    SettableBeanProperty[] _propertyBasedFactoryProperties = null;

    AnnotatedConstructor _strConstructor, _intConstructor, _longConstructor;
    AnnotatedConstructor _delegatingConstructor;
    AnnotatedConstructor _propertyBasedConstructor;
    SettableBeanProperty[] _propertyBasedConstructorProperties = null;

    public CreatorContainer(Class<?> beanClass, boolean canFixAccess) {
        _canFixAccess = canFixAccess;
        _beanClass = beanClass;
    }

    /*
    /////////////////////////////////////////////////////////
    // Setters
    /////////////////////////////////////////////////////////
    */

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
    /////////////////////////////////////////////////////////
    // Accessors
    /////////////////////////////////////////////////////////
    */

    public Creator.StringBased stringCreator()
    {
        if (_strConstructor == null &&  _strFactory == null) {
            return null;
        }
        return new Creator.StringBased(_beanClass, _strConstructor, _strFactory);
    }

    public Creator.NumberBased numberCreator()
    {
        if (_intConstructor == null && _intFactory == null
            && _longConstructor == null && _longFactory == null) {
            return null;
        }
        return new Creator.NumberBased(_beanClass, _intConstructor, _intFactory,
                                       _longConstructor, _longFactory);
    }

    public Creator.Delegating delegatingCreator()
    {
        if (_delegatingConstructor == null && _delegatingFactory == null) {
            return null;
        }
        return new Creator.Delegating(_delegatingConstructor, _delegatingFactory);
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
/////////////////////////////////////////////////////////
// Helper methods
/////////////////////////////////////////////////////////
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



