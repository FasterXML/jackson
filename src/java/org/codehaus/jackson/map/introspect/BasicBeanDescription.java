package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.BeanDescription;
import org.codehaus.jackson.map.annotate.OutputProperties;
import org.codehaus.jackson.map.util.ClassUtil;

public class BasicBeanDescription extends BeanDescription
{
    /*
    ///////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////
     */

    /**
     * Information collected about the class introspected.
     */
    final AnnotatedClass _classInfo;

    final AnnotationIntrospector _annotationIntrospector;

    /*
    ///////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////
     */

    public BasicBeanDescription(Class<?> forClass, AnnotatedClass ac,
                                AnnotationIntrospector ai)

    {
    	super(forClass);
    	_classInfo = ac;
        _annotationIntrospector = ai;
    }

    /*
    ///////////////////////////////////////////////////////
    // Simple accessors
    ///////////////////////////////////////////////////////
     */

    public AnnotatedClass getClassInfo() { return _classInfo; }

    public AnnotatedMethod findMethod(String name, Class<?>[] paramTypes)
    {
        return _classInfo.findMethod(name, paramTypes);
    }

    /*
    ///////////////////////////////////////////////////////
    // Basic API
    ///////////////////////////////////////////////////////
     */
    
    /**
     * @param autodetect Whether to use Bean naming convention to
     *   automatically detect bean properties; if true will do that,
     *   if false will require explicit annotations.
     *
     * @return Ordered Map with logical property name as key, and
     *    matching getter method as value.
     */
    public LinkedHashMap<String,AnnotatedMethod> findGetters(boolean autodetect,
                                                             Collection<String> ignoredProperties)
    {
        /* As part of [JACKSON-52] we'll use baseline settings for
         * auto-detection, but also see if the class might override
         * that setting.
         */
        Boolean classAD = _annotationIntrospector.findGetterAutoDetection(_classInfo);
        if (classAD != null) {
            autodetect = classAD.booleanValue();
        }

        LinkedHashMap<String,AnnotatedMethod> results = new LinkedHashMap<String,AnnotatedMethod>();
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
                /* As per [JACKSON-64], let's still use mangled
                 * name if possible; and only if not use unmodified
                 * method name
                 */
                if (propName.length() == 0) { 
                    propName = okNameForGetter(am);
                    if (propName == null) {
                        propName = am.getName();
                    }
                }
            } else { // nope, but is public bean-getter name?
                if (!autodetect) {
                    continue;
                }
                /* For getters (but not for setters), auto-detection
                 * requires method to be public:
                 */
                if (!am.isPublic()) {
                    continue;
                }
                propName = okNameForGetter(am);
                if (propName == null) { // null means 'not valid'
                    continue;
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
    ///////////////////////////////////////////////////////
    // Introspection for serialization (write Json), factories
    ///////////////////////////////////////////////////////
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
     * Method that can be called to locate a single-arg constructor that
     * takes specified exact type (will not accept supertype constructors)
     *
     * @param argTypes Type(s) of the argument that we are looking for
     */
    public Constructor<?> findSingleArgConstructor(Class<?>... argTypes)
    {
        for (AnnotatedConstructor ac : _classInfo.getSingleArgConstructors()) {
            // This list is already filtered to only include accessible
            Class<?>[] args = ac.getParameterTypes();
            /* (note: for now this is a redundant check; but in future
             * that'll change; thus leaving here for now)
             */
            if (args.length == 1) {
                Class<?> actArg = args[0];
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
        for (AnnotatedMethod am : _classInfo.getSingleArgStaticMethods()) {
            // First: return type must be the introspected class
            if (am.getReturnType() != _class) {
                continue;
            }
            /* Then: must be a recognized factory, meaning:
             * (a) public "valueOf", OR
             * (b) marked with @JsonCreator annotation
             */
            if (_annotationIntrospector.hasCreatorAnnotation(am)) {
                ;
            } else if ("valueOf".equals(am.getName())) {
                ;
            } else { // not recognized, skip
                continue;
            }

            // And finally, must take one of expected arg types (or supertype)
            /* !!! 18-May-2009, tatu: should it complain for explicitly
             *   marked method that has incompatible signature?
             */
            Class<?> actualArgType = am.getParameterTypes()[0];
            for (Class<?> expArgType : expArgTypes) {
                // And one that matches what we would pass in
                if (actualArgType.isAssignableFrom(expArgType)) {
                    return am.getAnnotated();
                }
            }
        }
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Introspection for deserialization, setters:
    ///////////////////////////////////////////////////////
     */

    /**
     * 
     * @return Ordered Map with logical property name as key, and
     *    matching setter method as value.
     */
    public LinkedHashMap<String,AnnotatedMethod> findSetters(boolean autodetect)
    {
        /* As part of [JACKSON-52] we'll use baseline settings for
         * auto-detection, but also see if the class might override
         * that setting.
         */
        Boolean classAD = _annotationIntrospector.findSetterAutoDetection(_classInfo);
        if (classAD != null) {
            autodetect = classAD.booleanValue();
        }

        LinkedHashMap<String,AnnotatedMethod> results = new LinkedHashMap<String,AnnotatedMethod>();
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
                }
            } else { // nope, but is public bean-setter name?
                if (!autodetect) {
                    continue;
                }
                propName = okNameForSetter(am);
                if (propName == null) { // null means 'not valid'
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
                throw new IllegalArgumentException("Conflicting setter definitions for property \""+propName+"\": "+oldDesc+" vs "+newDesc);
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
            Class<?> type = am.getParameterTypes()[0];
            if (type != String.class && type != Object.class) {
                throw new IllegalArgumentException("Invalid 'any-setter' annotation on method "+am.getName()+"(): first argument not of type String or Object, but "+type.getName());
            }
            found = am;
        }
        return found;
    }

    /*
    ///////////////////////////////////////////////////////
    // Introspection for serialization
    ///////////////////////////////////////////////////////
     */

    /**
     * Method for determining whether null properties should be written
     * out for a Bean of introspected type. This is based on global
     * feature (lowest priority, passed as argument)
     * and per-class annotation (highest priority).
     */
    public OutputProperties findSerializationInclusion(OutputProperties defValue)
    {
        return _annotationIntrospector.findSerializationInclusion(_classInfo, defValue);
    }

    /*
    ///////////////////////////////////////////////////////
    // Helper methods for getters
    ///////////////////////////////////////////////////////
     */

    protected String okNameForGetter(AnnotatedMethod am)
    {
        String name = am.getName();
        if (name.startsWith("get")) {
            /* 16-Feb-2009, tatu: To handle [JACKSON-53], need to block
             *   CGLib-provided method "getCallbacks". Not sure of exact
             *   safe critieria to get decent coverage without false matches;
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
                if (isGroovyMeta(am)) {
                    return null;
                }
            }
            return mangleGetterName(am, name.substring(3));
        }
        if (name.startsWith("is")) {
            // plus, must return boolean...
            Class<?> rt = am.getReturnType();
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
     */
    protected boolean isCglibGetCallbacks(AnnotatedMethod am)
    {
        Class<?> rt = am.getReturnType();
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
        if (pkg != null && pkg.getName().startsWith("net.sf.cglib")) {
            return true;
        }
        return false;
    }

    /**
     * Similar to {@link #isCglibGetCallbacks}, need to suppress
     * a cyclic reference to resolve [JACKSON-103]
     */
    protected boolean isGroovyMeta(AnnotatedMethod am)
    {
        Class<?> rt = am.getReturnType();
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
    ///////////////////////////////////////////////////////
    // Helper methods for setters
    ///////////////////////////////////////////////////////
     */

    protected String okNameForSetter(AnnotatedMethod am)
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
    //////////////////////////////////////////////////////////
    // Property name manging (getFoo -> foo)
    //////////////////////////////////////////////////////////
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
        if (elem instanceof Class) {
            return "class "+((Class<?>) elem).getName();
        }
        if (elem instanceof Method) {
            Method m = (Method) elem;
            return "method "+m.getName()+" (from class "+m.getDeclaringClass().getName()+")";
        }
        if (elem instanceof Constructor) {
            Constructor<?> ctor = (Constructor<?>) elem;
            // should indicate number of args?
            return "constructor() (from class "+ctor.getDeclaringClass().getName()+")";
        }
        // what else?
        return "unknown type ["+elem.getClass()+"]";
    }
}
