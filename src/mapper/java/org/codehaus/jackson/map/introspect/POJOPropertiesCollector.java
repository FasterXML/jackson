package org.codehaus.jackson.map.introspect;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;


/* TODO:
 * 
 * - Naming strategy: probably only after otherwise reconciling (i.e. done by BasicBeanDescription)?
 * 
 */

/**
 * Helper class used for aggregating information about all possible
 * properties of a POJO.
 * 
 * @since 1.9
 */
public class POJOPropertiesCollector
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */
    
    /**
     * Configuration settings
     */
    protected final MapperConfig<?> _config;

    /**
     * True if introspection is done for serialization (giving
     *   precedence for serialization annotations), or not (false, deserialization)
     */
    protected final boolean _forSerialization;
    
    /**
     * Type of POJO being introspected
     */
    protected final JavaType _type;

    /**
     * Low-level introspected class information (methods, fields etc)
     */
    protected final AnnotatedClass _classDef;

    protected final VisibilityChecker<?> _visibilityChecker;

    protected final AnnotationIntrospector _annotationIntrospector;
    
    /*
    /**********************************************************
    /* Collected information
    /**********************************************************
     */

    /**
     * Set of logical property information collected so far
     */
    protected final LinkedHashMap<String, POJOPropertyCollector> _properties = new LinkedHashMap<String, POJOPropertyCollector>();

    protected LinkedList<AnnotatedMethod> _anyGetters = null;

    protected LinkedList<AnnotatedMethod> _anySetters = null;

    /**
     * Method(s) marked with 'JsonValue' annotation
     */
    protected LinkedList<AnnotatedMethod> _jsonValueGetters = null;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    private POJOPropertiesCollector(MapperConfig<?> config, boolean forSerialization,
            JavaType type, AnnotatedClass classDef)
    {
        _config = config;
        _forSerialization = forSerialization;
        _type = type;
        _classDef = classDef;
        _annotationIntrospector = config.isAnnotationProcessingEnabled() ?
                _config.getAnnotationIntrospector() : null;
        if (_annotationIntrospector == null) {
            _visibilityChecker = _config.getDefaultVisibilityChecker();
        } else {
            _visibilityChecker = _annotationIntrospector.findAutoDetectVisibility(classDef, _config.getDefaultVisibilityChecker());
        }
    }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public static POJOPropertiesCollector collect(MapperConfig<?> config, boolean forSerialization,
            JavaType type, AnnotatedClass classDef)
    {
        POJOPropertiesCollector coll = new POJOPropertiesCollector(config, forSerialization, type, classDef);
        
        // First: gather basic data
        coll._addFields();
        coll._addMethods();

        // Second: remove ignored properties, individual entries
        coll._removeIgnoredProperties();
        // Third: rename remaining properties
        coll._renameProperties();
        return coll;
    }

    public MapperConfig<?> getConfig() {
        return _config;
    }

    public JavaType getType() {
        return _type;
    }
    
    public AnnotatedClass getClassDef() {
        return _classDef;
    }

    public AnnotationIntrospector getAnnotationIntrospector() {
        return _annotationIntrospector;
    }
    
    public List<POJOPropertyCollector> getProperties() {
        return new ArrayList<POJOPropertyCollector>(_properties.values());
    }

    public AnnotatedMethod getJsonValueMethod()
    {
        // If @JsonValue defined, must have a single one
        if (_jsonValueGetters != null) {
            if (_jsonValueGetters.size() > 1) {
                reportProblem("Multiple value properties defined ("+_jsonValueGetters.get(0)+" vs "
                        +_jsonValueGetters.get(1)+")");
            }
            // otherwise we won't greatly care
            return _jsonValueGetters.get(0);
        }
        return null;
    }

    public AnnotatedMethod getAnyGetterMethod()
    {
        if (_anyGetters != null) {
            if (_anyGetters.size() > 1) {
                reportProblem("Multiple 'any-getters' defined ("+_anyGetters.get(0)+" vs "
                        +_anyGetters.get(1)+")");
            }
            return _anyGetters.getFirst();
        }        
        return null;
    }

    public AnnotatedMethod getAnySetterMethod()
    {
        if (_anySetters != null) {
            if (_anySetters.size() > 1) {
                reportProblem("Multiple 'any-setters' defined ("+_anySetters.get(0)+" vs "
                        +_anySetters.get(1)+")");
            }
            return _anySetters.getFirst();
        }
        return null;
    }
    
    // for unit tests:
    protected Map<String, POJOPropertyCollector> getPropertyMap() {
        return _properties;
    }

    
    /*
    /**********************************************************
    /* Internal methods; main-level collection
    /**********************************************************
     */

    /**
     * Method for collecting basic information on all fields found
     */
    protected void _addFields()
    {
        final AnnotationIntrospector ai = _annotationIntrospector;
        
        for (AnnotatedField f : _classDef.fields()) {
            String implName = f.getName();
            String explName;
            if (ai == null) {
                explName = null;
            } else if (_forSerialization) {
                /* 18-Aug-2011, tatu: As per existing unit tests, we should only
                 *   use serialization annotation (@JsonSerializer) when serializing
                 *   fields, and similarly for deserialize-only annotations... so
                 *   no fallbacks in this particular case.
                 */
                explName = ai.findSerializablePropertyName(f);
            } else {
                explName = ai.findDeserializablePropertyName(f);
            }
            if ("".equals(explName)) { // empty String meaning "use default name", here just means "same as field name"
                explName = implName;
            }
            // having explicit name means that field is visible; otherwise need to check the rules
            boolean visible = (explName != null);
            if (!visible) {
                visible = _visibilityChecker.isFieldVisible(f);
            }
            if (visible) {
                // and finally, may also have explicit ignoral
                boolean ignored = (ai != null) && ai.hasIgnoreMarker(f);
                _property(implName).addField(f, explName, ignored);
            }
        }
    }

    /**
     * Method for collecting basic information on constructor(s) found
     */
    @SuppressWarnings("unused")
    protected void _addConstructors()
    {
        for (AnnotatedConstructor ctor : _classDef.getConstructors()) {
        }
        for (AnnotatedMethod factory : _classDef.getStaticMethods()) {
        }
    }

    /**
     * Method for collecting basic information on all fields found
     */
    protected void _addMethods()
    {
        final AnnotationIntrospector ai = _annotationIntrospector;
        
        for (AnnotatedMethod m : _classDef.memberMethods()) {
            String explName; // from annotation(s)
            String implName; // from naming convention
            
            /* For methods, handling differs between getters and setters; and
             * we will also only consider entries that either follow the bean
             * naming convention or are explicitly marked: just being visible
             * is not enough (unlike with fields)
             */
            int argCount = m.getParameterCount();
            boolean visible;
            
            if (argCount == 0) { // getters (including 'any getter')
                // any getter?
                if (ai != null) {
                    if (ai.hasAnyGetterAnnotation(m)) {
                        if (_anyGetters == null) {
                            _anyGetters = new LinkedList<AnnotatedMethod>();
                        }
                        _anyGetters.add(m);
                        continue;
                    }
                    // @JsonValue?
                    if (ai.hasAsValueAnnotation(m)) {
                        if (_jsonValueGetters == null) {
                            _jsonValueGetters = new LinkedList<AnnotatedMethod>();
                        }
                        _jsonValueGetters.add(m);
                        continue;
                    }
                }
                
                explName = (ai == null) ? null : ai.findGettablePropertyName(m);
                if (explName == null) { // no explicit name; must follow naming convention
                    implName = okNameForRegularGetter(m, m.getName());
                    if (implName == null) { // if not, must skip
                        implName = okNameForIsGetter(m, m.getName());
                        if (implName == null) {
                            continue;
                        }
                        visible = _visibilityChecker.isIsGetterVisible(m);
                    } else {
                        visible = _visibilityChecker.isGetterVisible(m);
                    }
                } else { // explicit indication of inclusion, but may be empty
                    if (explName.length() == 0) { 
                        explName = okNameForGetter(m);
                        if (explName == null) {
                            explName = m.getName();
                        }
                    }
                    implName = explName;
                    visible = true;
                }
                if (visible) {
                    boolean ignore = (ai == null) ? false : ai.hasIgnoreMarker(m);
                    _property(implName).addGetter(m, explName, ignore);
                }
            } else if (argCount == 1) { // setters
                explName = (ai == null) ? null : ai.findSettablePropertyName(m);
                if (explName == null) { // no explicit name; must follow naming convention
                    implName = okNameForSetter(m);
                    if (implName == null) { // if not, must skip
                        continue;
                    }
                    visible = _visibilityChecker.isSetterVisible(m);
                } else { // explicit indication of inclusion, but may be empty
                    if (explName.length() == 0) { 
                        explName = okNameForSetter(m);
                        if (explName == null) {
                            explName = m.getName();
                        }
                    }
                    implName = explName;
                    visible = true;
                }
                if (visible) {
                    boolean ignore = (ai == null) ? false : ai.hasIgnoreMarker(m);
                    _property(implName).addSetter(m, explName, ignore);
                }
            } else if (argCount == 2) { // any getter?
                if (ai != null  && ai.hasAnySetterAnnotation(m)) {
                    if (_anySetters == null) {
                        _anySetters = new LinkedList<AnnotatedMethod>();
                    }
                    _anySetters.add(m);
                }
            }
        }

    }

    /*
    /**********************************************************
    /* Internal methods; removing ignored properties
    /**********************************************************
     */

    protected void _removeIgnoredProperties()
    {
        Iterator<Map.Entry<String,POJOPropertyCollector>> it = _properties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, POJOPropertyCollector> entry = it.next();
            POJOPropertyCollector prop = entry.getValue();
            if (prop.anyIgnorals()) {
                // first: if one or more ignorals, and no explicit markers, remove the whole thing
                if (!prop.anyExplicitNames()) {
                    it.remove();
                    continue;
                }
                // otherwise just remove ones marked to be ignored
                prop.removeIgnored();
            }
        }
    }
    
    /*
    /**********************************************************
    /* Internal methods; renaming properties
    /**********************************************************
     */

    protected void _renameProperties()
    {
        // With renaming need to do in phases: first, find properties to rename
        Iterator<Map.Entry<String,POJOPropertyCollector>> it = _properties.entrySet().iterator();
        LinkedList<POJOPropertyCollector> renamed = null;
        while (it.hasNext()) {
            Map.Entry<String, POJOPropertyCollector> entry = it.next();
            POJOPropertyCollector prop = entry.getValue();
            String newName = prop.findNewName();
            if (newName != null) {
                if (renamed == null) {
                    renamed = new LinkedList<POJOPropertyCollector>();
                }
                renamed.add(prop.withName(newName));
                it.remove();
            }
        }

        // and if any were renamed, merge back in...
        if (renamed != null) {
            for (POJOPropertyCollector prop : renamed) {
                String name = prop.getName();
                POJOPropertyCollector old = _properties.get(name);
                if (old == null) {
                    _properties.put(name, prop);
                } else {
                    old.addAll(prop);
                }
            }
        }
    }
    
    /*
    /**********************************************************
    /* Internal methods: handling "getter" names
    /**********************************************************
     */

    protected static String okNameForGetter(AnnotatedMethod am)
    {
        String name = am.getName();
        String str = okNameForIsGetter(am, name);
        if (str == null) {
            str = okNameForRegularGetter(am, name);
        }
        return str;
    }

    protected static String okNameForRegularGetter(AnnotatedMethod am, String name)
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

    protected static String okNameForIsGetter(AnnotatedMethod am, String name)
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
    protected static String mangleGetterName(Annotated a, String basename)
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
    protected static boolean isCglibGetCallbacks(AnnotatedMethod am)
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
    protected static boolean isGroovyMetaClassSetter(AnnotatedMethod am)
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
    protected static boolean isGroovyMetaClassGetter(AnnotatedMethod am)
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

    /**
     * Method called to figure out name of the property, given 
     * corresponding suggested name based on a method or field name.
     *
     * @param basename Name of accessor/mutator method, not including prefix
     *  ("get"/"is"/"set")
     */
    protected static String manglePropertyName(String basename)
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
    
    /*
    /**********************************************************
    /* Internal methods: handling "setter" names
    /**********************************************************
     */

    public static String okNameForSetter(AnnotatedMethod am)
    {
        String name = am.getName();
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
    protected static String mangleSetterName(Annotated a, String basename)
    {
        return manglePropertyName(basename);
    }
    
    /*
    /**********************************************************
    /* Internal methods; helpers
    /**********************************************************
     */

    protected void reportProblem(String msg) {
        throw new IllegalArgumentException("Problem with definition of "+_classDef+": "+msg);
    }
    
    protected POJOPropertyCollector _property(String implName)
    {
        POJOPropertyCollector prop = _properties.get(implName);
        if (prop == null) {
            prop = new POJOPropertyCollector(implName);
            _properties.put(implName, prop);
        }
        return prop;
    }
}
