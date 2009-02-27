package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

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
    final AnnotationFilter _annotationFilter;

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
                           AnnotationFilter af)
    {
        _class = cls;
        _superTypes = superTypes;
        _annotationFilter = af;
    }

    /**
     * @param annotationFilter Filter used to define which annotations to
     *    include (for class and member annotations). Can not be null.
     * @param includeCreators Whether to include information about
     *   potential creators (constructors and static factory methods)
     * @param memberFilter Optional filter that defines which member methods
     *   to include; if null, no member method information is to be included.
     */
    public static AnnotatedClass constructFull(Class<?> cls,
                                               AnnotationFilter annotationFilter,
                                               boolean includeCreators,
                                               MethodFilter memberFilter)
    {
        List<Class<?>> st = ClassUtil.findSuperTypes(cls, null);
        AnnotatedClass ac = new AnnotatedClass(cls, st, annotationFilter);
        ac.resolveClassAnnotations();
        if (includeCreators) {
            ac.resolveCreators();
        }
        if (memberFilter != null) {
            ac.resolveMemberMethods(memberFilter);
        }
        return ac;
    }

    /*
    ///////////////////////////////////////////////////////
    // Init methods
    ///////////////////////////////////////////////////////
     */

    /**
     * Initialization method that will recursively collect Jackson
     * annotations for this class and all super classes and
     * interfaces.
     */
    private void resolveClassAnnotations()
    {
        _classAnnotations = new AnnotationMap();
        // first, annotations from the class itself:
        for (Annotation a : _class.getDeclaredAnnotations()) {
            _classAnnotations.add(a);
        }
        // and then from super types
        for (Class<?> cls : _superTypes) {
            for (Annotation a : cls.getDeclaredAnnotations()) {
                _classAnnotations.addIfNotPresent(a);
            }
        }
    }

    /**
     * Initialization method that will find out all constructors
     * and potential static factory methods the class has.
     */
    private void resolveCreators()
    {
        // Then see which constructors we have
        _singleArgConstructors = null;
        for (Constructor<?> ctor : _class.getDeclaredConstructors()) {
            switch (ctor.getParameterTypes().length) {
            case 0:
                _defaultConstructor = new AnnotatedConstructor(ctor);
                break;
            case 1:
                if (_singleArgConstructors == null) {
                    _singleArgConstructors = new ArrayList<AnnotatedConstructor>();
                }
                _singleArgConstructors.add(new AnnotatedConstructor(ctor));
                break;
            }
        }

        _singleArgStaticMethods = null;
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
                    _singleArgStaticMethods.add(new AnnotatedMethod(m));
                }
            }
        }
    }

    private void resolveMemberMethods(MethodFilter methodFilter)
    {
        _memberMethods = new AnnotatedMethodMap();
        for (Method m : _class.getDeclaredMethods()) {
            if (methodFilter.includeMethod(m)) {
                _memberMethods.add(new AnnotatedMethod(m));
            }
        }
        /* and then augment these with annotations from
         * super-types:
         */
        for (Class<?> cls : _superTypes) {
            for (Method m : cls.getDeclaredMethods()) {
                if (methodFilter.includeMethod(m)) {
                    AnnotatedMethod am = _memberMethods.find(m);
                    if (am == null) {
                        am = new AnnotatedMethod(m);
                        _memberMethods.add(am);
                    } else {
                        am.addAnnotationsNotPresent(m);
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

    /*
    ///////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////
     */

    public AnnotatedConstructor getDefaultConstructor() { return _defaultConstructor; }

    public Collection<AnnotatedConstructor> getSingleArgConstructors()
    {
        if (_singleArgConstructors != null) {
            return _singleArgConstructors;
        }
        return Collections.emptyList();
    }

    public Collection<AnnotatedMethod> getSingleArgStaticMethods()
    {
        if (_singleArgStaticMethods != null) {
            return _singleArgStaticMethods;
        }
        return Collections.emptyList();
    }

    public Collection<AnnotatedMethod> getMemberMethods()
    {
        return _memberMethods.getMethods();
    }

    /*
    ///////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////
     */
}

