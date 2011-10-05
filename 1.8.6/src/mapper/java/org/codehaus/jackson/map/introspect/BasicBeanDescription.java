package org.codehaus.jackson.map.introspect;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.BeanDescription;
import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.util.Annotations;
import org.codehaus.jackson.map.util.ClassUtil;
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
    /* Configuration
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
    /* Life-cycle
    /**********************************************************
     */
    
    public BasicBeanDescription(MapperConfig<?> config, JavaType type,
            AnnotatedClass ac)
    {
    	super(type);
    	_config = config;
    	_annotationIntrospector = config.getAnnotationIntrospector();
    	_classInfo = ac;
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
    public LinkedHashMap<String,AnnotatedMethod> findGetters(VisibilityChecker<?> visibilityChecker,
            Collection<String> ignoredProperties)
    {
        LinkedHashMap<String,AnnotatedMethod> results = new LinkedHashMap<String,AnnotatedMethod>();
        final PropertyNamingStrategy naming = _config.getPropertyNamingStrategy();
        for (AnnotatedMethod am : _classInfo.memberMethods()) {
            /* note: signature has already been checked to some degree
             * via filters; however, no checks were done for arg count
             */
            // 16-May-2009, tatu: JsonIgnore processed earlier already
            if (am.getParameterCount() != 0) {
                continue;
            }
            /* So far so good: final check, then; has to either
             * (a) be marked with JsonProperty (/JsonGetter/JsonSerialize) OR
             * (b) be public AND have suitable name (getXxx or isXxx)
             */
            String propName = _annotationIntrospector.findGettablePropertyName(am);
            if (propName != null) {
                /* As per [JACKSON-64], let's still use mangled name if possible;
                 * and only if not use unmodified method name
                 */
                if (propName.length() == 0) { 
                    propName = okNameForAnyGetter(am, am.getName());
                    if (propName == null) {
                        propName = am.getName();
                    }
                    // [JACKSON-178] Also, allow renaming via strategy
                    if (naming != null) {
                        propName = naming.nameForGetterMethod(_config, am, propName);
                    }
                }
            } else {
                propName = am.getName();
                // [JACKSON-166], need to separate getXxx/isXxx methods
                if (propName.startsWith("get")) { // nope, but is public bean-getter name?
                    if (!visibilityChecker.isGetterVisible(am)) {
                        continue;
                    }
                    propName = okNameForGetter(am, propName);
                } else {
                    if (!visibilityChecker.isIsGetterVisible(am)) {
                        continue;
                    }
                    propName = okNameForIsGetter(am, propName);
                }
                // null return value means 'not valid'
                if (propName == null) continue;
                // [JACKSON-384] Plus, should not include "AnyGetter" as regular getter..
                if (_annotationIntrospector.hasAnyGetterAnnotation(am)) continue;

                // [JACKSON-178] Also, allow renaming via strategy
                if (naming != null) {
                    propName = naming.nameForGetterMethod(_config, am, propName);
                }
            }

            if (ignoredProperties != null) {
                if (ignoredProperties.contains(propName)) {
                    continue;
                }
            }
            
            /* Yup, it is a valid name. But now... do we have a conflict?
             * If so, should throw an exception
             */
            AnnotatedMethod old = results.put(propName, am);
            if (old != null) {
                String oldDesc = old.getFullName();
                String newDesc = am.getFullName();
                throw new IllegalArgumentException("Conflicting getter definitions for property \""+propName+"\": "+oldDesc+" vs "+newDesc);
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
        AnnotatedMethod found = null;
        for (AnnotatedMethod am : _classInfo.memberMethods()) {
            // must be marked with "JsonValue" (or similar)
            if (!_annotationIntrospector.hasAsValueAnnotation(am)) {
                continue;
            }
            if (found != null) {
                throw new IllegalArgumentException("Multiple methods with active 'as-value' annotation ("+found.getName()+"(), "+am.getName()+")");
            }
            // Also, must have getter signature
            /* 18-May-2009, tatu: Should this be moved to annotation
             *  introspector, to give better error message(s)? For now
             *  will leave here, may want to reconsider in future.
             */
            if (!ClassUtil.hasGetterSignature(am.getAnnotated())) {
                throw new IllegalArgumentException("Method "+am.getName()+"() marked with an 'as-value' annotation, but does not have valid getter signature (non-static, takes no args, returns a value)");
            }
            found = am;
        }
        return found;
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
        final PropertyNamingStrategy naming = _config.getPropertyNamingStrategy();
        for (AnnotatedMethod am : _classInfo.memberMethods()) {
            // note: signature has already been checked via filters

            // Arg count != 1 (JsonIgnore checked earlier)
            if (am.getParameterCount() != 1) {
                continue;
            }

            /* So far so good: final check, then; has to either
             * (a) be marked with JsonProperty (/JsonSetter/JsonDeserialize) OR
             * (b) have suitable name (setXxx) (NOTE: need not be
             *    public, unlike with getters)
             */
            String propName = _annotationIntrospector.findSettablePropertyName(am);
            if (propName != null) { // annotation was found
                /* As per [JACKSON-64], let's still use mangled name if
                 * possible; and only if not use unmodified method name
                 */
                if (propName.length() == 0) { 
                    propName = okNameForSetter(am);
                    // null means it's not named as a Bean getter; fine, use as is
                    if (propName == null) {
                        propName = am.getName();
                    }
                    // [JACKSON-178] Also, allow renaming via strategy
                    if (naming != null) {
                        propName = naming.nameForSetterMethod(_config, am, propName);
                    }
                }
            } else { // nope, but is public bean-setter name?
                if (!vchecker.isSetterVisible(am)) {
                    continue;
                }
                propName = okNameForSetter(am);
                if (propName == null) { // null means 'not valid'
                    continue;
                }
                // [JACKSON-178] Also, allow renaming via strategy
                if (naming != null) {
                    propName = naming.nameForSetterMethod(_config, am, propName);
                }
            }

            /* Yup, it is a valid name. But now... do we have a conflict?
             * If so, should throw an exception
             */
            AnnotatedMethod old = results.put(propName, am);
            if (old != null) {
            	/* [JACKSON-255] Only throw exception if they are in same class. Must
            	 *   be careful to choose "correct" one; first one should actually
            	 *   have priority
            	 */
            	if (old.getDeclaringClass() == am.getDeclaringClass()) {
	                String oldDesc = old.getFullName();
	                String newDesc = am.getFullName();
	                throw new IllegalArgumentException("Conflicting setter definitions for property \""+propName+"\": "+oldDesc+" vs "+newDesc);
            	}
            	// to put earlier one back
            	results.put(propName, old);
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
        AnnotatedMethod found = null;
        for (AnnotatedMethod am : _classInfo.memberMethods()) {
            if (!_annotationIntrospector.hasAnySetterAnnotation(am)) {
                continue;
            }
            if (found != null) {
                throw new IllegalArgumentException("Multiple methods with 'any-setter' annotation ("+found.getName()+"(), "+am.getName()+")");
            }
            int pcount = am.getParameterCount();
            if (pcount != 2) {
                throw new IllegalArgumentException("Invalid 'any-setter' annotation on method "+am.getName()+"(): takes "+pcount+" parameters, should take 2");
            }
            /* Also, let's be somewhat strict on how field name is to be
             * passed; String, Object make sense, others not
             * so much.
             */
            /* !!! 18-May-2009, tatu: how about enums? Can add support if
             *  requested; easy enough for devs to add support within
             *  method.
             */
            Class<?> type = am.getParameterClass(0);
            if (type != String.class && type != Object.class) {
                throw new IllegalArgumentException("Invalid 'any-setter' annotation on method "+am.getName()+"(): first argument not of type String or Object, but "+type.getName());
            }
            found = am;
        }
        return found;
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
        AnnotatedMethod found = null;
        for (AnnotatedMethod am : _classInfo.memberMethods()) {
            if (!_annotationIntrospector.hasAnyGetterAnnotation(am)) {
                continue;
            }
            if (found != null) {
                throw new IllegalArgumentException("Multiple methods with 'any-getter' annotation ("+found.getName()+"(), "+am.getName()+")");
            }
            /* For now let's require a Map; in future can add support for other
             * types like perhaps Iterable<Map.Entry>?
             */
            Class<?> type = am.getRawType();
            if (!Map.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Invalid 'any-getter' annotation on method "+am.getName()+"(): return type is not instance of java.util.Map");
            }
            found = am;
        }
        return found;
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
    /* Helper methods for getters
    /**********************************************************
     */

    public String okNameForAnyGetter(AnnotatedMethod am, String name)
    {
        String str = okNameForIsGetter(am, name);
        if (str == null) {
            str = okNameForGetter(am, name);
        }
        return str;
    }

    public String okNameForGetter(AnnotatedMethod am, String name)
    {
        if (name.startsWith("get")) {
            /* 16-Feb-2009, tatu: To handle [JACKSON-53], need to block
             *   CGLib-provided method "getCallbacks". Not sure of exact
             *   safe criteria to get decent coverage without false matches;
             *   but for now let's assume there's no reason to use any 
             *   such getter from CGLib.
             *   But let's try this approach...
             */
            if ("getCallbacks".equals(name)) {
                if (isCglibGetCallbacks(am)) {
                    return null;
                }
            } else if ("getMetaClass".equals(name)) {
                /* 30-Apr-2009, tatu: [JACKSON-103], need to suppress
                 *    serialization of a cyclic (and useless) reference
                 */
                if (isGroovyMetaClassGetter(am)) {
                    return null;
                }
            }
            return mangleGetterName(am, name.substring(3));
        }
        return null;
    }

    public String okNameForIsGetter(AnnotatedMethod am, String name)
    {
        if (name.startsWith("is")) {
            // plus, must return boolean...
            Class<?> rt = am.getRawType();
            if (rt != Boolean.class && rt != Boolean.TYPE) {
                return null;
            }
            return mangleGetterName(am, name.substring(2));
        }
        // no, not a match by name
        return null;
    }

    /**
     * @return Null to indicate that method is not a valid accessor;
     *   otherwise name of the property it is accessor for
     */
    protected String mangleGetterName(Annotated a, String basename)
    {
        return manglePropertyName(basename);
    }

    /**
     * This method was added to address [JACKSON-53]: need to weed out
     * CGLib-injected "getCallbacks". 
     * At this point caller has detected a potential getter method
     * with name "getCallbacks" and we need to determine if it is
     * indeed injectect by Cglib. We do this by verifying that the
     * result type is "net.sf.cglib.proxy.Callback[]"
     *<p>
     * Also, see [JACKSON-177]; Hibernate may repackage cglib
     * it uses, so we better catch that too
     */
    protected boolean isCglibGetCallbacks(AnnotatedMethod am)
    {
        Class<?> rt = am.getRawType();
        // Ok, first: must return an array type
        if (rt == null || !rt.isArray()) {
            return false;
        }
        /* And that type needs to be "net.sf.cglib.proxy.Callback".
         * Theoretically could just be a type that implements it, but
         * for now let's keep things simple, fix if need be.
         */
        Class<?> compType = rt.getComponentType();
        // Actually, let's just verify it's a "net.sf.cglib.*" class/interface
        Package pkg = compType.getPackage();
        if (pkg != null) {
            String pname = pkg.getName();
            if (pname.startsWith("net.sf.cglib")
                // also, as per [JACKSON-177]
                || pname.startsWith("org.hibernate.repackage.cglib")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Similar to {@link #isCglibGetCallbacks}, need to suppress
     * a cyclic reference to resolve [JACKSON-103]
     */
    protected boolean isGroovyMetaClassSetter(AnnotatedMethod am)
    {
        Class<?> argType = am.getParameterClass(0);
        Package pkg = argType.getPackage();
        if (pkg != null && pkg.getName().startsWith("groovy.lang")) {
            return true;
        }
        return false;
    }

    /**
     * Another helper method to deal with rest of [JACKSON-103]
     */
    protected boolean isGroovyMetaClassGetter(AnnotatedMethod am)
    {
        Class<?> rt = am.getRawType();
        if (rt == null || rt.isArray()) {
            return false;
        }
        Package pkg = rt.getPackage();
        if (pkg != null && pkg.getName().startsWith("groovy.lang")) {
            return true;
        }
        return false;
    }
 
    /*
    /**********************************************************
    /* Helper methods for setters
    /**********************************************************
     */

    public String okNameForSetter(AnnotatedMethod am)
    {
        String name = am.getName();

        /* For mutators, let's not require it to be public. Just need
         * to be able to call it, i.e. do need to 'fix' access if so
         * (which is done at a later point as needed)
         */
        if (name.startsWith("set")) {
            name = mangleSetterName(am, name.substring(3));
            if (name == null) { // plain old "set" is no good...
                return null;
            }
            if ("metaClass".equals(name)) {
                // 26-Nov-2009 [JACSON-103], need to suppress this internal groovy method
                if (isGroovyMetaClassSetter(am)) {
                    return null;
                }
            }
            return name;
        }
        return null;
    }

    /**
     * @return Null to indicate that method is not a valid accessor;
     *   otherwise name of the property it is accessor for
     */
    protected String mangleSetterName(Annotated a, String basename)
    {
        return manglePropertyName(basename);
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
        final PropertyNamingStrategy naming = _config.getPropertyNamingStrategy();
        for (AnnotatedField af : _classInfo.fields()) {
            /* note: some pre-filtering has been; no static or transient fields 
             * included; nor anything marked as ignorable (@JsonIgnore).
             * Field masking has also been resolved, but it is still possible
             * to get conflicts due to logical name overwrites.
             */

            /* So far so good: final check, then; has to either
             * (a) be marked with JsonProperty (or JsonSerialize) OR
             * (b) be public
             */
            String propName = forSerialization
                ? _annotationIntrospector.findSerializablePropertyName(af)
                : _annotationIntrospector.findDeserializablePropertyName(af)
                ;
            if (propName != null) { // is annotated
                if (propName.length() == 0) { 
                    propName = af.getName();
                    // [JACKSON-178] Also, allow renaming via strategy
                    if (naming != null) {
                        propName = naming.nameForField(_config, af, propName);
                    }
                }
            } else { // nope, but may be visible (usually, public, can be recofingured)
                if (!vchecker.isFieldVisible(af)) {
                    continue;
                }
                propName = af.getName();
                // [JACKSON-178] Also, allow renaming via strategy
                if (naming != null) {
                    propName = naming.nameForField(_config, af, propName);
                }
            }

            if (ignoredProperties != null) {
                if (ignoredProperties.contains(propName)) {
                    continue;
                }
            }

            /* Yup, it is a valid name. But do we have a conflict?
             * Shouldn't usually happen, but it is possible... and for
             * now let's consider it a problem
             */
            AnnotatedField old = results.put(propName, af);
            if (old != null) {
                /* 21-Feb-2010, tatus: Not necessarily a conflict, still; only
                 *    conflict if these are declared in same class (in future, might
                 *    not even be conflict then, if types are different? That would
                 *    allow "union" types. But for now, let's consider that illegale
                 *    to keep things simple.
                 */
                /* Note: we assume that fields are ordered from "oldest" (super-class) to
                 * "newest" (sub-classes); this should guarantee proper ordering
                 */
                if (old.getDeclaringClass() == af.getDeclaringClass()) {
                    String oldDesc = old.getFullName();
                    String newDesc = af.getFullName();
                    throw new IllegalArgumentException("Multiple fields representing property \""+propName+"\": "+oldDesc+" vs "+newDesc);
                }
            }
        }
        return results;
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
