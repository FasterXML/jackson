package org.codehaus.jackson.map.introspect;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.util.Annotations;
import org.codehaus.jackson.type.JavaType;

/**
 * Default {@link BeanDescription} implementation.
 * Can theoretically be subclassed to customize
 * some aspects of property introspection.
 */
public class BasicBeanDescription extends BeanDescription
{
    /*
    /**********************************************************
    /* General configuration
    /**********************************************************
     */

    final protected MapperConfig<?> _config;

    final protected AnnotationIntrospector _annotationIntrospector;
    
    /**
     * Information collected about the class introspected.
     */
    final protected AnnotatedClass _classInfo;
    
    /**
     * We may need type bindings for the bean type. If so, we'll
     * construct it lazily
     */
    protected TypeBindings _bindings;

    /*
    /**********************************************************
    /* Member information
    /**********************************************************
     */
    
    protected final List<POJOPropertyCollector> _properties;

    // // for deserialization
    
    protected AnnotatedMethod _anySetterMethod;

    // // for serialization
    
    protected AnnotatedMethod _jsonValueMethod;

    protected AnnotatedMethod _anyGetterMethod;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public BasicBeanDescription(MapperConfig<?> config, JavaType type,
            AnnotatedClass ac)
    {
        this(config, type, ac, Collections.<POJOPropertyCollector>emptyList());
    }

    public BasicBeanDescription(MapperConfig<?> config, JavaType type,
            AnnotatedClass ac, List<POJOPropertyCollector> properties)
    {
    	super(type);
    	_config = config;
    	_annotationIntrospector = config.getAnnotationIntrospector();
    	_classInfo = ac;
    	_properties = properties;
    }

    public static BasicBeanDescription forDeserialization(POJOPropertiesCollector coll)
    {
        BasicBeanDescription desc = new BasicBeanDescription(coll.getConfig(),
                coll.getType(), coll.getClassDef(), coll.getProperties());
        desc._anySetterMethod = coll.getAnySetterMethod();
        return desc;
    }

    public static BasicBeanDescription forSerialization(POJOPropertiesCollector coll)
    {
        BasicBeanDescription desc = new BasicBeanDescription(coll.getConfig(),
                coll.getType(), coll.getClassDef(), coll.getProperties());
        desc._jsonValueMethod = coll.getJsonValueMethod();
        desc._anyGetterMethod = coll.getAnyGetterMethod();
        return desc;
    }
    
    /*
    /**********************************************************
    /* Simple accessors from BeanDescription
    /**********************************************************
     */

    /**
     * Method for checking whether class being described has any
     * annotations recognized by registered annotation introspector.
     */
    @Override
    public boolean hasKnownClassAnnotations() {
        return _classInfo.hasAnnotations();
    }

    @Override
    public Annotations getClassAnnotations() {
        return _classInfo.getAnnotations();
    }

    @Override
    public TypeBindings bindingsForBeanType()
    {
        if (_bindings == null) {
            _bindings = new TypeBindings(_config.getTypeFactory(), _type);
        }
        return _bindings;
    }

    @Override
    public JavaType resolveType(java.lang.reflect.Type jdkType) {
        return bindingsForBeanType().resolveType(jdkType);
    }
    
    /*
    /**********************************************************
    /* Simple accessors, extended
    /**********************************************************
     */
    
    public AnnotatedClass getClassInfo() { return _classInfo; }

    public AnnotatedMethod findMethod(String name, Class<?>[] paramTypes)
    {
        return _classInfo.findMethod(name, paramTypes);
    }

    /**
     * Method called to create a "default instance" of the bean, currently
     * only needed for obtaining default field values which may be used for
     * suppressing serialization of fields that have "not changed".
     * 
     * @param fixAccess If true, method is allowed to fix access to the
     *   default constructor (to be able to call non-public constructor);
     *   if false, has to use constructor as is.
     *
     * @return Instance of class represented by this descriptor, if
     *   suitable default constructor was found; null otherwise.
     */
    public Object instantiateBean(boolean fixAccess)
    {
        AnnotatedConstructor ac = _classInfo.getDefaultConstructor();
        if (ac == null) {
            return null;
        }
        if (fixAccess) {
            ac.fixAccess();
        }
        try {
            return ac.getAnnotated().newInstance();
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof Error) throw (Error) t;
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            throw new IllegalArgumentException("Failed to instantiate bean of type "+_classInfo.getAnnotated().getName()+": ("+t.getClass().getName()+") "+t.getMessage(), t);
        }
    }
    
    /*
    /**********************************************************
    /* Basic API
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Introspection for serialization (write JSON), getters
    /**********************************************************
     */
    
    @Override
    public LinkedHashMap<String,AnnotatedMethod> findGetters(VisibilityChecker<?> vchecker,
            Collection<String> ignoredProperties)
    {
        LinkedHashMap<String,AnnotatedMethod> results = new LinkedHashMap<String,AnnotatedMethod>();
        for (POJOPropertyCollector property : _properties) {
            AnnotatedMethod m = property.getGetter();
            if (m != null) {
                String name = property.getName();
                if (ignoredProperties != null) {
                    if (ignoredProperties.contains(name)) {
                        continue;
                    }
                }
                results.put(name, m);
            }
        }
        return results;
    }

    /**
     * Method for locating the getter method that is annotated with
     * {@link org.codehaus.jackson.annotate.JsonValue} annotation,
     * if any. If multiple ones are found,
     * an error is reported by throwing {@link IllegalArgumentException}
     */
    public AnnotatedMethod findJsonValueMethod()
    {
        return _jsonValueMethod;
    }

    /*
    /**********************************************************
    /* Introspection for serialization, factories
    /**********************************************************
     */

    /**
     * Method that will locate the no-arg constructor for this class,
     * if it has one, and that constructor has not been marked as
     * ignorable.
     * Method will also ensure that the constructor is accessible.
     */
    public Constructor<?> findDefaultConstructor()
    {
        AnnotatedConstructor ac = _classInfo.getDefaultConstructor();
        if (ac == null) {
            return null;
        }
        return ac.getAnnotated();
    }

    /**
     * @since 1.9
     */
    public AnnotatedConstructor findAnnotatedDefaultConstructor()
    {
        return _classInfo.getDefaultConstructor();
    }
    
    public List<AnnotatedConstructor> getConstructors()
    {
        return _classInfo.getConstructors();
    }

    public List<AnnotatedMethod> getFactoryMethods()
    {
        // must filter out anything that clearly is not a factory method
        List<AnnotatedMethod> candidates = _classInfo.getStaticMethods();
        if (candidates.isEmpty()) {
            return candidates;
        }
        ArrayList<AnnotatedMethod> result = new ArrayList<AnnotatedMethod>();
        for (AnnotatedMethod am : candidates) {
            if (isFactoryMethod(am)) {
                result.add(am);
            }
        }
        return result;
    }

    /**
     * Method that can be called to locate a single-arg constructor that
     * takes specified exact type (will not accept supertype constructors)
     *
     * @param argTypes Type(s) of the argument that we are looking for
     */
    public Constructor<?> findSingleArgConstructor(Class<?>... argTypes)
    {
        for (AnnotatedConstructor ac : _classInfo.getConstructors()) {
            // This list is already filtered to only include accessible
            /* (note: for now this is a redundant check; but in future
             * that may change; thus leaving here for now)
             */
            if (ac.getParameterCount() == 1) {
                Class<?> actArg = ac.getParameterClass(0);
                for (Class<?> expArg : argTypes) {
                    if (expArg == actArg) {
                        return ac.getAnnotated();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Method that can be called to find if introspected class declares
     * a static "valueOf" factory method that returns an instance of
     * introspected type, given one of acceptable types.
     *
     * @param expArgTypes Types that the matching single argument factory
     *   method can take: will also accept super types of these types
     *   (ie. arg just has to be assignable from expArgType)
     */
    public Method findFactoryMethod(Class<?>... expArgTypes)
    {
        // So, of all single-arg static methods:
        for (AnnotatedMethod am : _classInfo.getStaticMethods()) {
            if (isFactoryMethod(am)) {
                // And must take one of expected arg types (or supertype)
                Class<?> actualArgType = am.getParameterClass(0);
                for (Class<?> expArgType : expArgTypes) {
                    // And one that matches what we would pass in
                    if (actualArgType.isAssignableFrom(expArgType)) {
                        return am.getAnnotated();
                    }
                }
            }
        }
        return null;
    }

    protected boolean isFactoryMethod(AnnotatedMethod am)
    {
        /* First: return type must be compatible with the introspected class
         * (i.e. allowed to be sub-class, although usually is the same
         * class)
         */
        Class<?> rt = am.getRawType();
        if (!getBeanClass().isAssignableFrom(rt)) {
            return false;
        }

        /* Also: must be a recognized factory method, meaning:
         * (a) marked with @JsonCreator annotation, or
         * (a) "valueOf" (at this point, need not be public)
         */
        if (_annotationIntrospector.hasCreatorAnnotation(am)) {
            return true;
        }
        if ("valueOf".equals(am.getName())) {
            return true;
        }
        return false;
    }

    /**
     * Method for getting ordered list of named Creator properties.
     * Returns an empty list is none found. If multiple Creator
     * methods are defined, order between properties from different
     * methods is undefined; however, properties for each such
     * Creator are ordered properly relative to each other. For the
     * usual case of just a single Creator, named properties are
     * thus properly ordered.
     */
    public List<String> findCreatorPropertyNames()
    {
        List<String> names = null;

        for (int i = 0; i < 2; ++i) {
            List<? extends AnnotatedWithParams> l = (i == 0)
                ? getConstructors() : getFactoryMethods();
            for (AnnotatedWithParams creator : l) {
                int argCount = creator.getParameterCount();
                if (argCount < 1) continue;
                String name = _annotationIntrospector.findPropertyNameForParam(creator.getParameter(0));
                if (name == null) continue;
                if (names == null) {
                    names = new ArrayList<String>();
                }
                names.add(name);
                for (int p = 1; p < argCount; ++p) {
                    names.add(_annotationIntrospector.findPropertyNameForParam(creator.getParameter(p)));
                }
            }
        }
        if (names == null) {
            return Collections.emptyList();
        }
        return names;
    }

    /*
    /**********************************************************
    /* Introspection for serialization, fields
    /**********************************************************
     */

    public LinkedHashMap<String,AnnotatedField> findSerializableFields(VisibilityChecker<?> vchecker,
                                                                       Collection<String> ignoredProperties)
    {
        return _findPropertyFields(vchecker, ignoredProperties, true);
    }
    
    /*
    /**********************************************************
    /* Introspection for serialization, other
    /**********************************************************
     */

    /**
     * Method for determining whether null properties should be written
     * out for a Bean of introspected type. This is based on global
     * feature (lowest priority, passed as argument)
     * and per-class annotation (highest priority).
     */
    public JsonSerialize.Inclusion findSerializationInclusion(JsonSerialize.Inclusion defValue)
    {
        if (_annotationIntrospector == null) {
            return defValue;
        }
        return _annotationIntrospector.findSerializationInclusion(_classInfo, defValue);
    }

    /*
    /**********************************************************
    /* Introspection for deserialization, setters:
    /**********************************************************
     */

    @Override
    public LinkedHashMap<String,AnnotatedMethod> findSetters(VisibilityChecker<?> vchecker)
    {
        LinkedHashMap<String,AnnotatedMethod> results = new LinkedHashMap<String,AnnotatedMethod>();
        for (POJOPropertyCollector property : _properties) {
            AnnotatedMethod m = property.getSetter();
            if (m != null) {
                results.put(property.getName(), m);
            }
        }
        return results;
    }

    /**
     * Method used to locate the method of introspected class that
     * implements {@link org.codehaus.jackson.annotate.JsonAnySetter}. If no such method exists
     * null is returned. If more than one are found, an exception
     * is thrown.
     * Additional checks are also made to see that method signature
     * is acceptable: needs to take 2 arguments, first one String or
     * Object; second any can be any type.
     */
    public AnnotatedMethod findAnySetter()
        throws IllegalArgumentException
    {
        if (_anySetterMethod != null) {
            /* Also, let's be somewhat strict on how field name is to be
             * passed; String, Object make sense, others not
             * so much.
             */
            /* !!! 18-May-2009, tatu: how about enums? Can add support if
             *  requested; easy enough for devs to add support within
             *  method.
             */
            Class<?> type = _anySetterMethod.getParameterClass(0);
            if (type != String.class && type != Object.class) {
                throw new IllegalArgumentException("Invalid 'any-setter' annotation on method "+_anySetterMethod.getName()+"(): first argument not of type String or Object, but "+type.getName());
            }
        }
        return _anySetterMethod;
    }

    /**
     * Method used to locate the method of introspected class that
     * implements {@link org.codehaus.jackson.annotate.JsonAnyGetter}.
     * If no such method exists null is returned.
     * If more than one are found, an exception is thrown.
     * 
     * @since 1.6
     */
    public AnnotatedMethod findAnyGetter() throws IllegalArgumentException
    {
        if (_anyGetterMethod != null) {
            /* For now let's require a Map; in future can add support for other
             * types like perhaps Iterable<Map.Entry>?
             */
            Class<?> type = _anyGetterMethod.getRawType();
            if (!Map.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Invalid 'any-getter' annotation on method "+_anyGetterMethod.getName()+"(): return type is not instance of java.util.Map");
            }
        }
        return _anyGetterMethod;
    }
    
    /**
     * Method for locating all back-reference properties (setters, fields) bean has
     * 
     * @since 1.6
     */
    public Map<String,AnnotatedMember> findBackReferenceProperties()
    {
        HashMap<String,AnnotatedMember> result = null;
        // First, gather setter methods
        for (AnnotatedMethod am : _classInfo.memberMethods()) {
            if (am.getParameterCount() == 1) {
                AnnotationIntrospector.ReferenceProperty prop = _annotationIntrospector.findReferenceType(am);
                if (prop != null && prop.isBackReference()) {
                    if (result == null) {
                        result = new HashMap<String,AnnotatedMember>();
                    }
                    if (result.put(prop.getName(), am) != null) {
                        throw new IllegalArgumentException("Multiple back-reference properties with name '"+prop.getName()+"'");
                    }
                }
            }
        }
        // then settable fields
        for (AnnotatedField af : _classInfo.fields()) {
            AnnotationIntrospector.ReferenceProperty prop = _annotationIntrospector.findReferenceType(af);
            if (prop != null && prop.isBackReference()) {
                if (result == null) {
                    result = new HashMap<String,AnnotatedMember>();
                }
                if (result.put(prop.getName(), af) != null) {
                    throw new IllegalArgumentException("Multiple back-reference properties with name '"+prop.getName()+"'");
                }
            }
        }
        return result;
    }
    
    /*
    /**********************************************************
    /* Introspection for deserialization, fields:
    /**********************************************************
     */

    public LinkedHashMap<String,AnnotatedField> findDeserializableFields(VisibilityChecker<?> vchecker,
                                                                       Collection<String> ignoredProperties)
    {
        return _findPropertyFields(vchecker, ignoredProperties, false);
    }

    /*
    /**********************************************************
    /* Helper methods for field introspection
    /**********************************************************
     */

    /**
     * @param vchecker (optional) Object that determines whether specific fields
     *   have enough visibility to be considered for inclusion; if null,
     *   auto-detection is disabled
     * @param ignoredProperties (optional) names of properties to ignore;
     *   any fields that would be recognized as one of these properties
     *   is ignored.
     * @param forSerialization If true, will collect serializable property
     *    fields; if false, deserializable
     *
     * @return Ordered Map with logical property name as key, and
     *    matching field as value.
     */
    public LinkedHashMap<String,AnnotatedField> _findPropertyFields(VisibilityChecker<?> vchecker,
            Collection<String> ignoredProperties, boolean forSerialization)
    {
        LinkedHashMap<String,AnnotatedField> results = new LinkedHashMap<String,AnnotatedField>();
        for (POJOPropertyCollector property : _properties) {
            AnnotatedField f = property.getField();
            if (f != null) {
                String name = property.getName();
                if (ignoredProperties != null) {
                    if (ignoredProperties.contains(name)) {
                        continue;
                    }
                }
                results.put(name, f);
            }
        }
        return results;
        /*
        final PropertyNamingStrategy naming = _config.getPropertyNamingStrategy();
            if (propName != null) { // is annotated
                if (propName.length() == 0) { 
                    propName = af.getName();
                    // [JACKSON-178] Also, allow renaming via strategy
                    if (naming != null) {
                        propName = naming.nameForField(_config, af, propName);
                    }
                }
                */
    }

    /*
    /**********************************************************
    /* Property name mangling (getFoo -> foo)
    /**********************************************************
     */

    /**
     * Method called to figure out name of the property, given 
     * corresponding suggested name based on a method or field name.
     *
     * @param basename Name of accessor/mutator method, not including prefix
     *  ("get"/"is"/"set")
     */
    public static String manglePropertyName(String basename)
    {
        int len = basename.length();

        // First things first: empty basename is no good
        if (len == 0) {
            return null;
        }
        // otherwise, lower case initial chars
        StringBuilder sb = null;
        for (int i = 0; i < len; ++i) {
            char upper = basename.charAt(i);
            char lower = Character.toLowerCase(upper);
            if (upper == lower) {
                break;
            }
            if (sb == null) {
                sb = new StringBuilder(basename);
            }
            sb.setCharAt(i, lower);
        }
        return (sb == null) ? basename : sb.toString();
    }

    /**
     * Helper method used to describe an annotated element of type
     * {@link Class} or {@link Method}.
     */
    public static String descFor(AnnotatedElement elem)
    {
        if (elem instanceof Class<?>) {
            return "class "+((Class<?>) elem).getName();
        }
        if (elem instanceof Method) {
            Method m = (Method) elem;
            return "method "+m.getName()+" (from class "+m.getDeclaringClass().getName()+")";
        }
        if (elem instanceof Constructor<?>) {
            Constructor<?> ctor = (Constructor<?>) elem;
            // should indicate number of args?
            return "constructor() (from class "+ctor.getDeclaringClass().getName()+")";
        }
        // what else?
        return "unknown type ["+elem.getClass()+"]";
    }
}
