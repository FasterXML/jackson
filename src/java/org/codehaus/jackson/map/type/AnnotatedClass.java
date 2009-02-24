package org.codehaus.jackson.map.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.map.util.ClassUtil;

public final class AnnotatedClass
{
    /**
     * Class for which annotations apply, and that owns other
     * components (constructors, methods)
     */
    Class<?> _class;

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
    private AnnotatedClass(Class<?> cls)
    {
        _class = cls;
    }

    public static AnnotatedClass constructFull(Class<?> cls)
    {
        AnnotatedClass ac = new AnnotatedClass(cls);
        ac.resolveClassAnnotations();
        ac.resolveCreators();
        ac.resolveMemberMethods();
        return ac;
    }

    /**
     * Alternate factory method that will only resolve class
     * annotations. Used when caller doesn't care about method
     * and creator annotations.
     */
    public static AnnotationMap findClassAnnotations(Class<?> cls)
    {
        AnnotatedClass ac = new AnnotatedClass(cls);
        ac.resolveClassAnnotations();
        return ac._classAnnotations;
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
        // And then what super-classes and interfaces have
        HashSet<Class<?>> handledInterfaces = new HashSet<Class<?>>();
        Class<?> curr = _class;

        _classAnnotations = new AnnotationMap();
        do {
            // First direct annotations for the current class
            for (Annotation a : curr.getDeclaredAnnotations()) {
                _classAnnotations.add(a);
            }
            // then interfaces current class directly implements
            for (Class intCls : curr.getInterfaces()) {
                // no need to process interfaces multiple times
                if (handledInterfaces.add(intCls)) {
                    for (Annotation a : intCls.getDeclaredAnnotations()) {
                        _classAnnotations.addIfNotPresent(a);
                    }
                }
            }
            // and then super-class (up until but not include Object)
            curr = curr.getSuperclass();
        } while (curr != null && curr != Object.class);
    }

    /**
     * Initialization method that will find out all constructors
     * and potential static factory methods the class has.
     */
    private void resolveCreators()
    {
        // Then see which constructors we have
        _singleArgConstructors = null;
        for (Constructor ctor : _class.getDeclaredConstructors()) {
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

    private void resolveMemberMethods()
    {
        // Ok, first, 0/1-arg member methods class itself has:
        _memberMethods = new AnnotatedMethodMap();
        for (Method m : _class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) { // skip static ones
                continue;
            }
            int argCount = m.getParameterTypes().length;
            if (argCount == 0 || argCount == 1) {
                _memberMethods.add(new AnnotatedMethod(m));
            }
        }
        /* and then augment these with annotations from
         * super-classes/interfaces
         */
        HashSet<Class<?>> handledInterfaces = new HashSet<Class<?>>();
        Class<?> curr = _class;

        while (true) {
            // first, interfaces current class directly implements
            for (Class intCls : curr.getInterfaces()) {
                // no need to process interfaces multiple times
                if (handledInterfaces.add(intCls)) {
                    _addMethodAnnotationsFromSuper(intCls);
                }
            }
            // and then super-class (up until but not include Object)
            curr = curr.getSuperclass();
            if (curr == null || curr == Object.class) {
                break;
            }
            _addMethodAnnotationsFromSuper(curr);
        }
    }

    /**
     * Method that will add "missing" member methods and annotations
     * from specified class or interface. That is, methods and
     * annotations that are not masked by classes higher up the chain.
     * The main reason to do this is to implement simple "inheritance"
     * for annotations; method annotations are not inherited
     * with regular JDK functionality.
     */
    private void _addMethodAnnotationsFromSuper(Class<?> superClassOrInterface)
    {
        for (Method m : superClassOrInterface.getDeclaredMethods()) {
            // static methods won't inherit, skip
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            int argCount = m.getParameterTypes().length;
            if (argCount == 0 || argCount == 1) {
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

    /*
    ///////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////
     */

    public Class<?> getAnnotated() { return _class; }

    public AnnotatedConstructor getDefaultConstructor() { return _defaultConstructor; }

    public <A extends Annotation> A getClassAnnotation(Class<A> acls)
    {
        if (_classAnnotations == null) {
            return null;
        }
        return _classAnnotations.get(acls);
    }

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

