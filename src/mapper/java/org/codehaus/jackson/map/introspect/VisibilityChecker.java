package org.codehaus.jackson.map.introspect;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

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
	// // Builder

	public T withGetterVisibility(Visibility v);
	public T withSetterVisibility(Visibility v);
	public T withCreatorVisibility(Visibility v);
	public T withFieldVisibility(Visibility v);
	
	// // Accessors
	
	public boolean isGetterVisible(Method m);
	public boolean isGetterVisible(AnnotatedMethod m);

	public boolean isSetterVisible(Method m);
	public boolean isSetterVisible(AnnotatedMethod m);

	public boolean isCreatorVisible(Member m);
	public boolean isCreatorVisible(AnnotatedMember m);

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
	 * access default minimum visibility access needed.
	 */
	@JsonAutoDetect()
	public static class Std
	    implements VisibilityChecker<Std>
    {
		protected final Visibility _getterMinLevel;
		protected final Visibility _setterMinLevel;
		protected final Visibility _creatorMinLevel;
		protected final Visibility _fieldMinLevel;
		
		/**
		 * Default constructor uses default {@link JsonAutoDetect}
		 * limits.
		 */
		public Std() {
			this(Std.class.getAnnotation(JsonAutoDetect.class));
		}

		public Std(JsonAutoDetect ann)
		{
			_getterMinLevel = ann.getterVisibility();
			_setterMinLevel = ann.setterVisibility();
			_creatorMinLevel = ann.creatorVisibility();
			_fieldMinLevel = ann.fieldVisibility();			
		}
		
		public Std(Visibility getter, Visibility setter, Visibility creator, Visibility field)
		{
			_getterMinLevel = getter;
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

		public Std withGetterVisibility(Visibility v) {
			return new Std(v, _setterMinLevel, _creatorMinLevel, _fieldMinLevel);
		}

		public Std withSetterVisibility(Visibility v) {
			return new Std(_getterMinLevel, v, _creatorMinLevel, _fieldMinLevel);
		}

		public Std withCreatorVisibility(Visibility v) {
			return new Std(_getterMinLevel, _setterMinLevel, v, _fieldMinLevel);
		}

		public Std withFieldVisibility(Visibility v) {
			return new Std(_getterMinLevel, _setterMinLevel, _creatorMinLevel, v);
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

		public boolean isSetterVisible(Method m) {
			return _setterMinLevel.isVisible(m);
		}

		public boolean isSetterVisible(AnnotatedMethod m) {
			return isSetterVisible(m.getAnnotated());
		}
    }

}
