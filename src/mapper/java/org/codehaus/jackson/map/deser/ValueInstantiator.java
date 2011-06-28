package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.introspect.AnnotatedWithParams;
import org.codehaus.jackson.type.JavaType;

/**
 * Class that defines simple API implemented by objects that create value
 * instances.  Some or all of properties of value instances may 
 * be initialized by instantiator, rest being populated by deserializer,
 * to which value instance is passed.
 * Since different kinds of JSON values (structured and scalar)
 * may be bound to Java values, in some cases instantiator
 * fully defines resulting value; this is the case when JSON value
 * is a scalar value (String, number, boolean).
 *<p>
 * Note that this type is not parameterized (even though it would seemingly
 * make sense), because such type information can not be use effectively
 * during runtime: access is always using either wildcard type, or just
 * basic {@link java.lang.Object}; and so adding type parameter seems
 * like unnecessary extra work.
 *<p>
 * Actual implementations are strongly recommended to be based on
 * {@link org.codehaus.jackson.map.deser.impl.StdValueInstantiator}
 * which implements all methods, and as such will be compatible
 * across versions even if new methods were added to this interface.
 * 
 * @since 1.9
 */
public abstract class ValueInstantiator
{
    /*
    /**********************************************************
    /* Metadata accessors
    /**********************************************************
     */

    /**
     * Method that returns value type handled by this instantiator.
     * Used for diagnostics purposes.
     */
    public abstract JavaType getValueType();

    /**
     * Method that can be called to check whether a String-based creator
     * is available for this instantiator
     */
    public boolean canCreateFromString() {
        return false;
    }

    /**
     * Method that can be called to check whether a number-based (int, long or double)
     * creator
     * is available for this instantiator
     */
    public boolean canCreateFromNumber() {
        return false;
    }

    /**
     * Method that can be called to check whether a default creator (constructor,
     * or no-arg static factory method)
     * is available for this instantiator
     */
    public boolean canCreateUsingDefault() {
        return false;
    }

    /**
     * Method that can be called to check whether a delegate-based creator (single-arg
     * constructor or factory method)
     * is available for this instantiator
     */
    public boolean canCreateUsingDelegate() {
        return getDelegateType() != null;
    }

    /**
     * Method that can be called to check whether an argument-taking ("property-based") creator
     * (argument-taking constructor or factory method)
     * is available for this instantiator
     */
    public boolean canCreateWithArgs() {
        return false;
    }

    /**
     * Method called to determine whether instantiation arguments
     * are expected for instantiating values for JSON Object;
     * if this method returns null (or empty List), no arguments
     * are expected (and {@link #createInstanceFromObject}) will
     * be used); otherwise specified arguments are bound
     * from input JSON object (and passed to
     * {@link #createInstanceFromObjectWith}).
     *<p>
     * It is unfortunate that argument information needs to be exposed
     * as {@link SettableBeanProperty} instances; but this can not
     * be avoided due to historical reasons.
     */
    public SettableBeanProperty[] getFromObjectArguments() {
        return null;
    }

    /**
     * Method that can be used to determine what is the type of delegate
     * type to use, if any; if no delegates are used, will return null.
     */
    public JavaType getDelegateType() {
        return null;
    }

    /**
     * Method that can be called to try to access member (constructor,
     * static factory method) that is used as the "delegate creator".
     */
    public AnnotatedWithParams getDelegateCreator() {
        return null;
    }
    
    /*
    /**********************************************************
    /* Instantiation methods for JSON Object
    /**********************************************************
     */

    /**
     * Method called to create value instance from JSON Object when
     * no data needs to passed to creator (constructor, factory method);
     * typically this will call the default constructor of the value object.
     *<p>
     * This method is called if {@link #getFromObjectArguments} returns
     * null or empty List.
     */
    public Object createInstanceFromObject()
        throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "+getValueType()+" from JSON Object (without arguments)");
    }

    /**
     * Method called to create value instance from JSON Object when
     * instantiation arguments are passed; this is done, for example when passing information
     * specified with "Creator" annotations.
     *<p>
     * This method is called if {@link #getFromObjectArguments} returns
     * a non-empty List of arguments.
     */
    public Object createInstanceFromObjectWith(Object[] args)
        throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "+getValueType()+" from JSON Object (with arguments)");
    }

    /**
     * Method to called to create value instance from JSON Object using
     * an intermediate "delegate" value to pass to createor method
     */
    public Object createInstanceFromObjectUsing(Object delegate)
        throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "+getValueType()+" from JSON Object with delegate");
    }
    
    /*
    /**********************************************************
    /* Instantiation methods for JSON scalar types
    /* (String, Number, Boolean)
    /**********************************************************
     */
    
    public Object createFromString(String value) throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "+getValueType()+" from JSON String");
    }
    
    public Object createFromInt(int value) throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "+getValueType()+" from JSON int number");
    }

    public Object createFromLong(long value) throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "+getValueType()+" from JSON long number");
    }

    public Object createFromDouble(double value) throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "+getValueType()+" from JSON floating-point number");
    }
    
    public Object createFromBoolean(boolean value) throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "+getValueType()+" from JSON boolean");
    }
}
