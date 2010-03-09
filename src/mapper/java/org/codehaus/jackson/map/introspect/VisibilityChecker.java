package org.codehaus.jackson.map.introspect;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Interface for object used for determine which property elements
 * (methods, fields, constructors) can be auto-detected, with respect
 * to their visibility modifiers.
 *<p>
 * Note on type declaration: funky recursive type is necessary to
 * support builder/fluid pattern.
 * 
 * @author tatu
 * @since 1.5
 */
public interface VisibilityChecker<T extends VisibilityChecker<T>>
{
    // // Builder methods

    /**
     * Builder method that will return an instance that has same
     * settings as this instance has, except for values that
     * given annotation overrides.
     */
    public T with(JsonAutoDetect ann);
    
    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for regular ("getXxx") getters.
     */
    public T withGetterVisibility(Visibility v);

    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for "is-getters" ("isXxx").
     */
    public T withIsGetterVisibility(Visibility v);
    
    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for setters.
     */
    public T withSetterVisibility(Visibility v);

    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for creator methods
     * (constructors, factory methods)
     */
    public T withCreatorVisibility(Visibility v);

    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for fields.
     */
    public T withFieldVisibility(Visibility v);
	
    // // Accessors
	
    /**
     * Method for checking whether given method is auto-detectable
     * as regular getter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isGetterVisible(Method m);
    public boolean isGetterVisible(AnnotatedMethod m);

    /**
     * Method for checking whether given method is auto-detectable
     * as is-getter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isIsGetterVisible(Method m);
    public boolean isIsGetterVisible(AnnotatedMethod m);
    
    /**
     * Method for checking whether given method is auto-detectable
     * as setter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isSetterVisible(Method m);
    public boolean isSetterVisible(AnnotatedMethod m);

    /**
     * Method for checking whether given method is auto-detectable
     * as Creator, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isCreatorVisible(Member m);
    public boolean isCreatorVisible(AnnotatedMember m);

    /**
     * Method for checking whether given field is auto-detectable
     * as property, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isFieldVisible(Field f);
    public boolean isFieldVisible(AnnotatedField f);

    /*
    /********************************************************
    /* Standard implementation suitable for basic use
    /********************************************************
    */

   /**
    * Default standard implementation is purely based on visibility
    * modifier of given class members, and its configured minimum
    * levels.
    * Implemented using "builder" (aka "Fluid") pattern, whereas instances
    * are immutable, and configuration is achieved by chainable factory
    * methods. As a result, type is declared is funky recursive generic
    * type, to allow for sub-classing of build methods with property type
    * co-variance.
    *<p>
    * Note on <code>JsonAutoDetect</code> annotation: it is used to
    * access default minimum visibility access definitions.
    */
    @JsonAutoDetect(
        getterVisibility = Visibility.PUBLIC_ONLY,
        isGetterVisibility = Visibility.PUBLIC_ONLY,
        setterVisibility = Visibility.ANY,
        /**
         * By default, all matching single-arg constructed are found,
         * regardless of visibility. Does not apply to factory methods,
         * they can not be auto-detected; ditto for multiple-argument
         * constructors.
         */
        creatorVisibility = Visibility.ANY,
        fieldVisibility = Visibility.PUBLIC_ONLY
    )
    public static class Std
        implements VisibilityChecker<Std>
    {
        /**
         * This is the canonical base instance, configured with default
         * visibility values
         */
        protected final static Std DEFAULT = new Std(Std.class.getAnnotation(JsonAutoDetect.class));
        
        protected final Visibility _getterMinLevel;
        protected final Visibility _isGetterMinLevel;
        protected final Visibility _setterMinLevel;
        protected final Visibility _creatorMinLevel;
        protected final Visibility _fieldMinLevel;
		
        public static Std defaultInstance() { return DEFAULT; }
        
        /**
         * Constructor used for building instance that has minumum visibility
         * levels as indicated by given annotation instance
         * 
         * @param ann Annotations to use for determining minimum visibility levels
         */
	public Std(JsonAutoDetect ann)
	{
	    JsonMethod[] incl = ann.value();
	    // let's combine checks for enabled/disabled, with minimimum level checks:
	    _getterMinLevel = hasMethod(incl, JsonMethod.GETTER) ? ann.getterVisibility() : Visibility.NONE;
            _isGetterMinLevel = hasMethod(incl, JsonMethod.IS_GETTER) ? ann.isGetterVisibility() : Visibility.NONE;
            _setterMinLevel = hasMethod(incl, JsonMethod.SETTER) ? ann.setterVisibility() : Visibility.NONE;
            _creatorMinLevel = hasMethod(incl, JsonMethod.CREATOR) ? ann.creatorVisibility() : Visibility.NONE;
            _fieldMinLevel = hasMethod(incl, JsonMethod.FIELD) ? ann.fieldVisibility() : Visibility.NONE;
	}

	/**
	 * Constructor that allows directly specifying minimum visibility levels to use
	 */
	public Std(Visibility getter, Visibility isGetter, Visibility setter, Visibility creator, Visibility field)
        {
	    _getterMinLevel = getter;
	    _isGetterMinLevel = isGetter;
	    _setterMinLevel = setter;
	    _creatorMinLevel = creator;
	    _fieldMinLevel = field;
        }
		
        /*
        /********************************************************
	/* Builder/fluid methods for instantiating configured
	/* instances
	/********************************************************
	 */

	public Std with(JsonAutoDetect ann)
	{
	    if (ann == null) return this;
	    Std curr = this;

	    JsonMethod[] incl = ann.value();
	    Visibility v;

	    v = hasMethod(incl, JsonMethod.GETTER) ? ann.getterVisibility() : Visibility.NONE;
	    curr = curr.withGetterVisibility(v);
	    v = hasMethod(incl, JsonMethod.IS_GETTER) ? ann.isGetterVisibility() : Visibility.NONE;
	    curr = curr.withIsGetterVisibility(v);
	    v = hasMethod(incl, JsonMethod.SETTER) ? ann.setterVisibility() : Visibility.NONE;
            curr  = curr.withSetterVisibility(v);
            v = hasMethod(incl, JsonMethod.CREATOR) ? ann.creatorVisibility() : Visibility.NONE;
            curr = curr.withCreatorVisibility(v);
            v = hasMethod(incl, JsonMethod.FIELD) ? ann.fieldVisibility() : Visibility.NONE;
            curr = curr.withFieldVisibility(v);
	    return curr;
	}

	public Std withGetterVisibility(Visibility v) {
	    if (v == Visibility.DEFAULT)  v = DEFAULT._getterMinLevel;
            if (_getterMinLevel == v) return this;
	    return new Std(v, _isGetterMinLevel, _setterMinLevel, _creatorMinLevel, _fieldMinLevel);
	}

        public Std withIsGetterVisibility(Visibility v) {
            if (v == Visibility.DEFAULT)  v = DEFAULT._isGetterMinLevel;
            if (_isGetterMinLevel == v) return this;
            return new Std(_getterMinLevel, v, _setterMinLevel, _creatorMinLevel, _fieldMinLevel);
        }
		
	public Std withSetterVisibility(Visibility v) {
            if (v == Visibility.DEFAULT)  v = DEFAULT._setterMinLevel;
            if (_setterMinLevel == v) return this;
            return new Std(_getterMinLevel, _isGetterMinLevel, v, _creatorMinLevel, _fieldMinLevel);
	}

	public Std withCreatorVisibility(Visibility v) {
            if (v == Visibility.DEFAULT)  v = DEFAULT._creatorMinLevel;
            if (_creatorMinLevel == v) return this;
	    return new Std(_getterMinLevel, _isGetterMinLevel, _setterMinLevel, v, _fieldMinLevel);
	}

	public Std withFieldVisibility(Visibility v) {
            if (v == Visibility.DEFAULT)  v = DEFAULT._fieldMinLevel;
            if (_fieldMinLevel == v) return this;
	    return new Std(_getterMinLevel, _isGetterMinLevel, _setterMinLevel, _creatorMinLevel, v);
	}
		
        /*
        /********************************************************
        /* Public API impl
        /********************************************************
         */

	public boolean isCreatorVisible(Member m) {
	    return _creatorMinLevel.isVisible(m);
    	}
    
    	public boolean isCreatorVisible(AnnotatedMember m) {
    	    return isCreatorVisible(m.getMember());
    	}
    
    	public boolean isFieldVisible(Field f) {
    	    return _fieldMinLevel.isVisible(f);
    	}
    
    	public boolean isFieldVisible(AnnotatedField f) {
    	    return isFieldVisible(f.getAnnotated());
    	}
    
    	public boolean isGetterVisible(Method m) {
    	    return _getterMinLevel.isVisible(m);
    	}    
    	public boolean isGetterVisible(AnnotatedMethod m) {
    	    return isGetterVisible(m.getAnnotated());
    	}

        public boolean isIsGetterVisible(Method m) {
            return _isGetterMinLevel.isVisible(m);
        }    

        public boolean isIsGetterVisible(AnnotatedMethod m) {
            return isIsGetterVisible(m.getAnnotated());
        }

        public boolean isSetterVisible(Method m) {
            return _setterMinLevel.isVisible(m);
        }
    
        public boolean isSetterVisible(AnnotatedMethod m) {
            return isSetterVisible(m.getAnnotated());
        }

        /*
        /********************************************************
        /* Helper methods
        /********************************************************
         */
    
        private static boolean hasMethod(JsonMethod[] methods, JsonMethod method)
        {
            for (JsonMethod curr : methods) {
                if (curr == method || curr == JsonMethod.ALL) return true;
            }
            return false;
        }

        /*
        /********************************************************
        /* Standard methods
        /********************************************************
         */
        @Override
        public String toString() {
            return new StringBuilder("[Visibility:")
                .append(" getter: ").append(_getterMinLevel)
                .append(", isGetter: ").append(_isGetterMinLevel)
                .append(", setter: ").append(_setterMinLevel)
                .append(", creator: ").append(_creatorMinLevel)
                .append(", field: ").append(_fieldMinLevel)
                .append("]").toString();
        }
    }
}
