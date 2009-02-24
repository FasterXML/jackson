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
    final AnnotationMap _classAnnotations = new AnnotationMap();

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
    AnnotatedMethodMap  _memberMethods = new AnnotatedMethodMap();

    /*
    ///////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////
     */

    /**
     * During construction we can figure out baseline settings
     * for all annotated things (direct class annotations,
     * constructors, potential static factory methods and
     * other methods)
     * But we will not do any checking for
     * inheritance (or interface implementation), nor annotation
     * overrides ("mix-in annotations"); those need to be handled
     * by the caller.
     */
    public AnnotatedClass(Class<?> cls)
    {
        _class = cls;
        // First let's find annotations we already have
        for (Annotation a : cls.getDeclaredAnnotations()) {
            _classAnnotations.add(a);
        }
        // Then see which constructors we have
        for (Constructor ctor : cls.getDeclaredConstructors()) {
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

        /* Then methods: single-arg static methods (potential factory
         * methods), and 0/1-arg member methods (getters, setters)
         */
        for (Method m : cls.getDeclaredMethods()) {
            int argCount = m.getParameterTypes().length;
            if (Modifier.isStatic(m.getModifiers())) {
                if (argCount == 1) {
                    if (_singleArgStaticMethods == null) {
                        _singleArgStaticMethods = new ArrayList<AnnotatedMethod>();
                    }
                    _singleArgStaticMethods.add(new AnnotatedMethod(m));
                }
            } else { // non-static
                if (argCount == 0 || argCount == 1) {
                    _memberMethods.add(new AnnotatedMethod(m));
                }
            }
        }
    }

    /**
     * Method called to add relevant annotation information from
     * interfaces implemented by this class as well as all of its
     * super-classes; all in expected order.
     */
    public void addAnnotationsFromSupers()
    {
        // Let's try to avoid extra work...
        HashSet<Class<?>> handledInterfaces = new HashSet<Class<?>>();
        Class<?> curr = _class;

        while (true) {
            // first, interfaces current class directly implements
            for (Class intCls : curr.getInterfaces()) {
                // no need to process interfaces multiple times
                if (handledInterfaces.add(intCls)) {
                    addAnnotationsFromSuper(intCls);
                }
            }
            // and then super-class (up until but not include Object)
            curr = curr.getSuperclass();
            if (curr == null || curr == Object.class) {
                break;
            }
            addAnnotationsFromSuper(curr);
        }
    }

    /**
     * Method that will add missing class and member method annotations
     * from specified class or interface. Other things (static methods,
     * constructors) are not included since they are not inheritable.
     */
    public void addAnnotationsFromSuper(Class<?> superClassOrInterface)
    {
        // First, class annotations:
        for (Annotation a : superClassOrInterface.getDeclaredAnnotations()) {
            _classAnnotations.addIfNotPresent(a);
        }

        // And then annotations for member methods:
        for (Method m : superClassOrInterface.getDeclaredMethods()) {
            // static methods won't inherit, so skip
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            int argCount = m.getParameterTypes().length;
            if (argCount == 0 || argCount == 1) {
                AnnotatedMethod am = _memberMethods.find(m);
                /* 23-Feb-2009, tatu: This may get tricky with interfaces
                 *  wrt. whether to add method for interfaces. For now
                 *  let's allow that -- assumption is that someone
                 *  somewhere is still bound to implement them.
                 */
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

