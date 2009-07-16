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

    /**
     * Object that knows mapping of mix-in classes (ones that contain
     * annotations to add) with their target classes (ones that
     * get these additional annotations "mixed in").
     */
    final MixInResolver _mixInResolver;

    /**
     * Primary mix-in class; one to use for the annotated class
     * itself. Can be null.
     */
    final Class<?> _primaryMixIn;

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
        _primaryMixIn = (_mixInResolver == null) ? null
            : _mixInResolver.findMixInClassFor(_class);
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

    /*
    ///////////////////////////////////////////////////////
    // Methods for resolving class annotations
    // (resolution consisting of inheritance, overrides,
    // and injection of mix-ins as necessary)
    ///////////////////////////////////////////////////////
     */

    /**
     * Initialization method that will recursively collect Jackson
     * annotations for this class and all super classes and
     * interfaces.
     *<p>
     * Starting with 1.2, it will also apply mix-in annotations,
     * as per [JACKSON-76]
     */
    protected void resolveClassAnnotations()
    {
        _classAnnotations = new AnnotationMap();
        // add mix-in annotations first (overrides)
        if (_primaryMixIn != null) {
            _addClassMixIns(_classAnnotations, _class, _primaryMixIn);
        }
        // first, annotations from the class itself:
        for (Annotation a : _class.getDeclaredAnnotations()) {
            if (_annotationIntrospector.isHandled(a)) {
                _classAnnotations.addIfNotPresent(a);
            }
        }

        // and then from super types
        for (Class<?> cls : _superTypes) {
            // and mix mix-in annotations in-between
            _addClassMixIns(_classAnnotations, cls);
            for (Annotation a : cls.getDeclaredAnnotations()) {
                if (_annotationIntrospector.isHandled(a)) {
                    _classAnnotations.addIfNotPresent(a);
                }
            }
        }

        /* and finally... any annotations there might be for plain
         * old Object.class: separate because for all other purposes
         * it is just ignored (not included in super types)
         */
        /* 12-Jul-2009, tatu: Should this be done for interfaces too?
         *   For now, yes, seems useful for some cases, and not harmful
         *   for any?
         */
        _addClassMixIns(_classAnnotations, Object.class);
    }

    /**
     * Helper method for adding any mix-in annotations specified
     * class might have.
     */
    protected void _addClassMixIns(AnnotationMap annotations, Class<?> toMask)
    {
        if (_mixInResolver != null) {
            _addClassMixIns(annotations, toMask, _mixInResolver.findMixInClassFor(toMask));
        }
    }

    protected void _addClassMixIns(AnnotationMap annotations, Class<?> toMask,
                                   Class<?> mixin)
    {
        if (mixin == null) {
            return;
        }
        // Ok, first: annotations from mix-in class itself:
        for (Annotation a : mixin.getDeclaredAnnotations()) {
            if (_annotationIntrospector.isHandled(a)) {
                annotations.addIfNotPresent(a);
            }
        }
        /* And then from its supertypes, if any. But note that we will
         *  only consider super-types up until reaching the masked
         * class (if found); this because often mix-in class
         * is a sub-class (for convenience reasons). And if so, we
         * absolutely must NOT include super types of masked class,
         * as that would inverse precedence of annotations.
         */
        for (Class<?> parent : ClassUtil.findSuperTypes(mixin, toMask)) {
            for (Annotation a : mixin.getDeclaredAnnotations()) {
                if (_annotationIntrospector.isHandled(a)) {
                    annotations.addIfNotPresent(a);
                }
            }
        }
    }

    /*
    /////////////////////////////////////////////////////////////
    // Methods for populating creator (ctor, factory) information
    /////////////////////////////////////////////////////////////
     */

    /**
     * Initialization method that will find out all constructors
     * and potential static factory methods the class has.
     *<p>
     * Starting with 1.2, it will also apply mix-in annotations,
     * as per [JACKSON-76]
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
        // and if need be, augment with mix-ins
        if (_primaryMixIn != null) {
            if (_defaultConstructor != null || _singleArgConstructors != null) {
                _addConstructorMixIns(_primaryMixIn);
            }
        }


        /* And then... let's remove all constructors that are
         * deemed to be ignorable after all annotations have been
         * properly collapsed.
         */
        if (_defaultConstructor != null) {
            if (_annotationIntrospector.isIgnorableConstructor(_defaultConstructor)) {
                _defaultConstructor = null;
            }
        }
        if (_singleArgConstructors != null) {
            // count down to allow safe removal
            for (int i = _singleArgConstructors.size(); --i >= 0; ) {
                if (_annotationIntrospector.isIgnorableConstructor(_singleArgConstructors.get(i))) {
                    _singleArgConstructors.remove(i);
                }
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
            // mix-ins to mix in?
            if (_primaryMixIn != null && _singleArgStaticMethods != null) {
                _addFactoryMixIns(_primaryMixIn);
            }
            // anything to ignore at this point?
            if (_singleArgStaticMethods != null) {
                // count down to allow safe removal
                for (int i = _singleArgStaticMethods.size(); --i >= 0; ) {
                    if (_annotationIntrospector.isIgnorableMethod(_singleArgStaticMethods.get(i))) {
                        _singleArgStaticMethods.remove(i);
                    }
                }
            }
        }
    }

    protected void _addConstructorMixIns(Class<?> mixin)
    {
        MemberKey[] ctorKeys = null;
        int ctorCount = (_singleArgConstructors == null) ? 0 : _singleArgConstructors.size();
        for (Constructor<?> ctor : mixin.getDeclaredConstructors()) {
            switch (ctor.getParameterTypes().length) {
            case 0:
                if (_defaultConstructor != null) {
                    _addMixOvers(ctor, _defaultConstructor);
                }
                break;
            case 1:
                if (ctorKeys == null) {
                    ctorKeys = new MemberKey[ctorCount];
                    for (int i = 0; i < ctorCount; ++i) {
                        ctorKeys[i] = new MemberKey(_singleArgConstructors.get(i).getAnnotated());
                    }
                }
                MemberKey key = new MemberKey(ctor);
                for (int i = 0; i < ctorCount; ++i) {
                    if (key.equals(ctorKeys[i])) {
                        _addMixOvers(ctor, _singleArgConstructors.get(i));
                        break;
                    }
                    break;
                }
            }
        }
    }

    protected void _addFactoryMixIns(Class<?> mixin)
    {
        MemberKey[] methodKeys = null;
        int methodCount = _singleArgStaticMethods.size();

        for (Method m : mixin.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (m.getParameterTypes().length != 1) {
                continue;
            }
            if (methodKeys == null) {
                methodKeys = new MemberKey[methodCount];
                for (int i = 0; i < methodCount; ++i) {
                    methodKeys[i] = new MemberKey(_singleArgStaticMethods.get(i).getAnnotated());
                }
            }
            MemberKey key = new MemberKey(m);
            for (int i = 0; i < methodCount; ++i) {
                if (key.equals(methodKeys[i])) {
                    _addMixOvers(m, _singleArgStaticMethods.get(i));
                    break;
                }
                break;
            }
        }
    }

    /*
    ///////////////////////////////////////////////////////
    // Methods for populating method information
    ///////////////////////////////////////////////////////
     */

    public void resolveMemberMethods(MethodFilter methodFilter)
    {
        _memberMethods = new AnnotatedMethodMap();
        AnnotatedMethodMap mixins = new AnnotatedMethodMap();
        // first: methods from the class itself
        _addMemberMethods(_class, _memberMethods, _primaryMixIn, mixins);

        // and then augment these with annotations from super-types:
        for (Class<?> cls : _superTypes) {
            Class<?> mixin = (_mixInResolver == null) ? null : _mixInResolver.findMixInClassFor(cls);
            _addMemberMethods(cls, _memberMethods, mixin, mixins);
        }
        // Special case: mix-ins for Object.class? (to apply to ALL classes)
        if (_mixInResolver != null) {
            Class<?> mixin = _mixInResolver.findMixInClassFor(Object.class);
            if (mixin != null) {
                _addMethodMixIns(_memberMethods, mixin, mixins);
            }
        }

        /* Any unmatched mix-ins? Most likely error cases (not matching
         * any method); but there is one possible real use case:
         * exposing Object#hashCode (alas, Object#getClass can NOT be
         * exposed, see [JACKSON-140])
         */
        if (!mixins.isEmpty()) {
            Iterator<AnnotatedMethod> it = mixins.iterator();
            while (it.hasNext()) {
                AnnotatedMethod mixIn = it.next();
                try {
                    Method m = Object.class.getDeclaredMethod(mixIn.getName(), mixIn.getParameterClasses());
                    if (m != null) {
                        AnnotatedMethod am = _constructMethod(m);
                        _addMixOvers(mixIn.getAnnotated(), am);
                        _memberMethods.add(am);
                    }
                } catch (Exception e) { }
            }
        }

        /* And last but not least: let's remove all methods that are
         * deemed to be ignorable after all annotations have been
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

    protected void _addMemberMethods(Class<?> cls, AnnotatedMethodMap methods,
                                     Class<?> mixInCls, AnnotatedMethodMap mixIns)
    {
        // first, mixIns, since they have higher priority then class methods
        if (mixInCls != null) {
            _addMethodMixIns(methods, mixInCls, mixIns);
        }

        if (cls != null) {
            // then methods from the class itself
            for (Method m : cls.getDeclaredMethods()) {
                if (!_isIncludableMethod(m)) {
                    continue;
                }
                AnnotatedMethod old = methods.find(m);
                if (old == null) {
                    AnnotatedMethod newM = _constructMethod(m);
                    methods.add(newM);
                    // Ok, but is there a mix-in to connect now?
                    old = mixIns.remove(m);
                    if (old != null) {
                        _addMixOvers(old.getAnnotated(), newM);
                    }
                } else {
                    /* If sub-class already has the method, we only want
                     * to augment annotations with entries that are not
                     * masked by sub-class:
                     */
                    _addMixUnders(m, old);
                }
            }
        }
    }

    protected void _addMethodMixIns(AnnotatedMethodMap methods,
                                    Class<?> mixInCls, AnnotatedMethodMap mixIns)
    {
        for (Method m : mixInCls.getDeclaredMethods()) {
            if (!_isIncludableMethod(m)) {
                continue;
            }
            AnnotatedMethod am = methods.find(m);
            /* Do we already have a method to augment (from sub-class
             * that will mask this mixIn)? If so, add if visible
             * without masking (no such annotation)
             */
            if (am != null) {
                _addMixUnders(m, am);
                /* Otherwise will have precedence, but must wait
                 * until we find the real method (mixIn methods are
                 * just placeholder, can't be called)
                 */
            } else {
                mixIns.add(_constructMethod(m));
            }
        }
    }

    /*
    ///////////////////////////////////////////////////////
    // Methods for populating field information
    ///////////////////////////////////////////////////////
     */

    /**
     * Method that will collect all member (non-static) fields
     * that are either public, or have at least a single annotation
     * associated with them.
     */
    public void resolveFields()
    {
        _fields = new ArrayList<AnnotatedField>();
        _addFields(_fields, _class);

        /* And last but not least: let's remove all fields that are
         * deemed to be ignorable after all annotations have been
         * properly collapsed.
         */
        Iterator<AnnotatedField> it = _fields.iterator();
        while (it.hasNext()) {
            AnnotatedField f = it.next();
            if (_annotationIntrospector.isIgnorableField(f)) {
                it.remove();
            }
        }
    }

    protected void _addFields(List<AnnotatedField> fields, Class<?> c)
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
             * (being handled meaning an exception gets
             */
            _addFields(fields, parent);
            for (Field f : c.getDeclaredFields()) {
                // static fields not included, nor transient
                if (!_isIncludableField(f)) {
                    continue;
                }
                /* Ok now: we can (and need) not filter out ignorable fields
                 * at this point; partly because mix-ins haven't been
                 * added, and partly because logic can be done when
                 * determining get/settability of the field.
                 */
                fields.add(_constructField(f));
            }
            // And then... any mix-in overrides?
            if (_mixInResolver != null) {
                Class<?> mixin = _mixInResolver.findMixInClassFor(c);
                if (mixin != null) {
                    _addFieldMixIns(mixin, fields);
                }
            }
        }
    }

    protected void _addFieldMixIns(Class<?> mixin, List<AnnotatedField> fields)
    {
        for (Field f : mixin.getDeclaredFields()) {
            /* there are some dummy things (static, synthetic); better
             * ignore
             */
            if (!_isIncludableField(f)) {
                continue;
            }
            String name = f.getName();
            // anything to mask?
            for (AnnotatedField af : fields) {
                if (name.equals(af.getName())) {
                    for (Annotation a : f.getDeclaredAnnotations()) {
                        if (_annotationIntrospector.isHandled(a)) {
                            af.addOrOverride(a);
                        }
                    }
                    break;
                }
            }
        }
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
    // Helper methods, inclusion filtering
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

    /*
    ///////////////////////////////////////////////////////
    // Helper methods, attaching annotations
    ///////////////////////////////////////////////////////
     */

    protected void _addMixOvers(Constructor mixin, AnnotatedConstructor target)
    {
        for (Annotation a : mixin.getDeclaredAnnotations()) {
            if (_annotationIntrospector.isHandled(a)) {
                target.addOrOverride(a);
            }
        }
    }

    protected void _addMixOvers(Method mixin, AnnotatedMethod target)
    {
        for (Annotation a : mixin.getDeclaredAnnotations()) {
            if (_annotationIntrospector.isHandled(a)) {
                target.addOrOverride(a);
            }
        }
    }

    protected void _addMixUnders(Method mixin, AnnotatedMethod target)
    {
        for (Annotation a : mixin.getDeclaredAnnotations()) {
            if (_annotationIntrospector.isHandled(a)) {
                target.addIfNotPresent(a);
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

