package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ClassIntrospector.MixInResolver;
import org.codehaus.jackson.map.util.ClassUtil;

public final class AnnotatedClass
    extends Annotated
{
    /*
    ///////////////////////////////////////////////////////
    // Helper classes
    ///////////////////////////////////////////////////////
     */

    /**
     * Filter used to only include methods that have signature that is
     * compatible with "factory" methods: are static, take a single
     * argument, and returns something.
     *<p>
     * <b>NOTE</b>: in future we will probably allow more than one
     * argument, when multi-arg constructors and factory methods
     * are supported (with accompanying annotations to bind args
     * to properties).
     */
    public final static class FactoryMethodFilter
        implements MethodFilter
    {
        public final static FactoryMethodFilter instance = new FactoryMethodFilter();

        public boolean includeMethod(Method m)
        {
            if (!Modifier.isStatic(m.getModifiers())) {
                return false;
            }
            int argCount = m.getParameterTypes().length;
            if (argCount != 1) {
                return false;
            }
            // Can't be a void method
            Class<?> rt = m.getReturnType();
            if (rt == Void.TYPE) {
                return false;
            }
            // Otherwise, potentially ok
            return true;
        }
    }

    /*
    ///////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////
     */

    /**
     * Class for which annotations apply, and that owns other
     * components (constructors, methods)
     */
    final Class<?> _class;

    /**
     * Ordered set of super classes and interfaces of the
     * class itself: included in order of precedence
     */
    final Collection<Class<?>> _superTypes;

    /**
     * Filter used to determine which annotations to gather; used
     * to optimize things so that unnecessary annotations are
     * ignored.
     */
    final AnnotationIntrospector _annotationIntrospector;

    final MixInResolver _mixInResolver;

    /*
    ///////////////////////////////////////////////////////
    // Gathered information
    ///////////////////////////////////////////////////////
     */

    /**
     * Combined list of Jackson annotations that the class has,
     * including inheritable ones from super classes and interfaces
     */
    AnnotationMap _classAnnotations;

    /**
     * Default constructor of the annotated class, if it has one.
     */
    AnnotatedConstructor _defaultConstructor;

    /**
     * Single argument constructors the class has, if any.
     */
    List<AnnotatedConstructor> _singleArgConstructors;

    /**
     * Single argument static methods that might be usable
     * as factory methods
     */
    List<AnnotatedMethod> _singleArgStaticMethods;

    /**
     * Member methods of interest; for now ones with 0 or 1 arguments
     * (just optimization, since others won't be used now)
     */
    AnnotatedMethodMap  _memberMethods;

    /**
     * Member fields of interest: ones that are either public,
     * or have at least one annotation.
     */
    List<AnnotatedField> _fields;

    /*
    ///////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////
     */

    /**
     * Constructor will not do any initializations, to allow for
     * configuring instances differently depending on use cases
     */
    private AnnotatedClass(Class<?> cls, List<Class<?>> superTypes,
                           AnnotationIntrospector aintr,
                           MixInResolver mir)
    {
        _class = cls;
        _superTypes = superTypes;
        _annotationIntrospector = aintr;
        _mixInResolver = mir;
    }

    /**
     * Factory method that instantiates an instance. Returned instance
     * will only be initialized with class annotations, but not with
     * any method information.
     */
    public static AnnotatedClass construct(Class<?> cls,
                                           AnnotationIntrospector aintr,
                                           MixInResolver mir)
    {
        List<Class<?>> st = ClassUtil.findSuperTypes(cls, null);
        AnnotatedClass ac = new AnnotatedClass(cls, st, aintr, mir);
        ac.resolveClassAnnotations();
        return ac;
    }

    /**
     * Initialization method that will recursively collect Jackson
     * annotations for this class and all super classes and
     * interfaces.
     */
    protected void resolveClassAnnotations()
    {
        _classAnnotations = new AnnotationMap();
        // first, annotations from the class itself:
        for (Annotation a : _class.getDeclaredAnnotations()) {
            if (_annotationIntrospector.isHandled(a)) {
                _classAnnotations.add(a);
            }
        }
        // and then from super types
        for (Class<?> cls : _superTypes) {
            for (Annotation a : cls.getDeclaredAnnotations()) {
                if (_annotationIntrospector.isHandled(a)) {
                    _classAnnotations.addIfNotPresent(a);
                }
            }
        }
    }

    /*
    ///////////////////////////////////////////////////////
    // Methods for populating method/field information
    ///////////////////////////////////////////////////////
     */

    /**
     * Initialization method that will find out all constructors
     * and potential static factory methods the class has.
     *
     * @param includeAll If true, includes all creator methods; if false,
     *   will only include the no-arguments "default" constructor
     */
    public void resolveCreators(boolean includeAll)
    {
        // Then see which constructors we have
        _singleArgConstructors = null;
        for (Constructor<?> ctor : _class.getDeclaredConstructors()) {
            switch (ctor.getParameterTypes().length) {
            case 0:
                _defaultConstructor = _constructConstructor(ctor);
                break;
            case 1:
                if (includeAll) {
                    if (_singleArgConstructors == null) {
                        _singleArgConstructors = new ArrayList<AnnotatedConstructor>();
                    }
                    _singleArgConstructors.add(_constructConstructor(ctor));
                }
                break;
            }
        }

        _singleArgStaticMethods = null;

        if (includeAll) {
            /* Then methods: single-arg static methods (potential factory
             * methods), and 0/1-arg member methods (getters, setters)
             */
            for (Method m : _class.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) {
                    int argCount = m.getParameterTypes().length;
                    if (argCount == 1) {
                        if (_singleArgStaticMethods == null) {
                            _singleArgStaticMethods = new ArrayList<AnnotatedMethod>();
                        }
                        _singleArgStaticMethods.add(_constructMethod(m));
                    }
                }
            }
        }
    }

    public void resolveMemberMethods(MethodFilter methodFilter)
    {
        _memberMethods = new AnnotatedMethodMap();
        for (Method m : _class.getDeclaredMethods()) {
            if (_isIncludableMethod(m)) {
                _memberMethods.add(_constructMethod(m));
            }
        }
        /* and then augment these with annotations from
         * super-types:
         */
        for (Class<?> cls : _superTypes) {
            for (Method m : cls.getDeclaredMethods()) {
                if (!_isIncludableMethod(m)) {
                    continue;
                }
                AnnotatedMethod old = _memberMethods.find(m);
                if (old == null) {
                    _memberMethods.add(_constructMethod(m));
                } else {
                    /* If sub-class already has the method, we only want
                     * to augment annotations with entries that are not
                     * masked by sub-class:
                     */
                    for (Annotation a : m.getDeclaredAnnotations()) {
                        if (_annotationIntrospector.isHandled(a)) {
                            old.addIfNotPresent(a);
                        }
                    }
                }
            }
        }

        /* And last but not least: let's remove all methods that are
         * deemed to be ignorable after all annotations has been
         * properly collapsed.
         */
        Iterator<AnnotatedMethod> it = _memberMethods.iterator();
        while (it.hasNext()) {
            AnnotatedMethod am = it.next();
            if (_annotationIntrospector.isIgnorableMethod(am)) {
                it.remove();
            }
        }
    }

    /**
     * Method that will collect all member (non-static) fields
     * that are either public, or have at least a single annotation
     * associated with them.
     */
    public void resolveFields()
    {
        _fields = new ArrayList<AnnotatedField>();
        _addFields(_fields, _class);
    }

    /*
    ///////////////////////////////////////////////////////
    // Helper methods, constructing value types
    ///////////////////////////////////////////////////////
     */

    protected AnnotatedMethod _constructMethod(Method m)
    {
        return new AnnotatedMethod(m, _collectRelevantAnnotations(m.getDeclaredAnnotations()));
    }

    protected AnnotatedConstructor _constructConstructor(Constructor<?> ctor)
    {
        return new AnnotatedConstructor(ctor, _collectRelevantAnnotations(ctor.getDeclaredAnnotations()));
    }

    protected AnnotatedField _constructField(Field f)
    {
        return new AnnotatedField(f, _collectRelevantAnnotations(f.getDeclaredAnnotations()));
    }

    protected AnnotationMap _collectRelevantAnnotations(Annotation[] anns)
    {
        AnnotationMap annMap = new AnnotationMap();
        if (anns != null) {
            for (Annotation a : anns) {
                if (_annotationIntrospector.isHandled(a)) {
                    annMap.add(a);
                }
            }
        }
        return annMap;
    }
 
    /*
    ///////////////////////////////////////////////////////
    // Helper methods, other
    ///////////////////////////////////////////////////////
     */

    protected boolean _isIncludableMethod(Method m)
    {
        /* 07-Apr-2009, tatu: Looks like generics can introduce hidden
         *   bridge and/or synthetic methods. I don't think we want to
         *   consider those...
         */
        if (m.isSynthetic() || m.isBridge()) {
            return false;
        }
        return true;
    }

    private boolean _isIncludableField(Field f)
    {
        /* I'm pretty sure synthetic fields are to be skipped...
         * (methods definitely are)
         */
        if (f.isSynthetic()) {
            return false;
        }
        // Static fields are never included, nor transient
        int mods = f.getModifiers();
        if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) {
            return false;
        }
        return true;
    }

    private void _addFields(List<AnnotatedField> fields, Class<?> c)
    {
        /* First, a quick test: we only care for regular classes (not
         * interfaces, primitive types etc), except for Object.class.
         * A simple check to rule out other cases is to see if there
         * is a super class or not.
         */
        Class<?> parent = c.getSuperclass();
        if (parent != null) {
            /* Let's add super-class' fields first, then ours.
             * Also: we won't be checking for masking (by name); it
             * can happen, if very rarely, but will be handled later
             * on when resolving masking between methods and fields
             */
            _addFields(fields, parent);
            for (Field f : c.getDeclaredFields()) {
                if (!_isIncludableField(f)) {
                    continue;
                }
                /* Need to be public, or have an annotation
                 * (these are required, but not sufficient checks).
                 * Note: can also check for exclusion here, since fields
                 * are not overridable.
                 */
                Annotation[] anns = f.getAnnotations();
                int mods = f.getModifiers();
                if (Modifier.isPublic(mods) || anns.length > 0) {
                    AnnotatedField af = _constructField(f);
                    if (!_annotationIntrospector.isIgnorableField(af)) {
                        fields.add(af);
                    }
                }
            }
        }
    }

    /*
    ///////////////////////////////////////////////////////
    // Annotated impl 
    ///////////////////////////////////////////////////////
     */

    public Class<?> getAnnotated() { return _class; }

    public int getModifiers() { return _class.getModifiers(); }

    public String getName() { return _class.getName(); }

    public <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        if (_classAnnotations == null) {
            return null;
        }
        return _classAnnotations.get(acls);
    }

    public Class<?> getType() {
        return _class;
    }

    /*
    ///////////////////////////////////////////////////////
    // Public API, generic accessors
    ///////////////////////////////////////////////////////
     */

    public AnnotatedConstructor getDefaultConstructor() { return _defaultConstructor; }

    public List<AnnotatedConstructor> getSingleArgConstructors()
    {
        if (_singleArgConstructors == null) {
            return Collections.emptyList();
        }
        return _singleArgConstructors;
    }

    public List<AnnotatedMethod> getSingleArgStaticMethods()
    {
        if (_singleArgStaticMethods == null) {
            return Collections.emptyList();
        }
        return _singleArgStaticMethods;
    }

    public Iterable<AnnotatedMethod> memberMethods()
    {
        return _memberMethods;
    }

    public int getMemberMethodCount()
    {
        return _memberMethods.size();
    }

    public AnnotatedMethod findMethod(String name, Class<?>[] paramTypes)
    {
        return _memberMethods.find(name, paramTypes);
    }

    public int getFieldCount() {
        return (_fields == null) ? 0 : _fields.size();
    }

    public Iterable<AnnotatedField> fields()
    {
        if (_fields == null) {
            List<AnnotatedField> l = Collections.emptyList();
            return l;
        }
        return _fields;
    }

    /*
    ///////////////////////////////////////////////////////
    // Other methods
    ///////////////////////////////////////////////////////
     */

    @Override
    public String toString()
    {
        return "[AnnotedClass "+_class.getName()+"]";
    }
}

