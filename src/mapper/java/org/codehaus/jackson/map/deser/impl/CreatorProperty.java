package org.codehaus.jackson.map.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.deser.SettableBeanProperty;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.introspect.AnnotatedParameter;
import org.codehaus.jackson.map.util.Annotations;
import org.codehaus.jackson.type.JavaType;

/**
 * This concrete sub-class implements property that is passed
 * via Creator (constructor or static factory method).
 * It is not a full-featured implementation in that its set method
 * should never be called -- instead, value must separately passed.
 *<p>
 * Note on injectable values (1.9): unlike with other mutators, where
 * deserializer and injecting are separate, here we deal the two as related
 * things. This is necessary to add proper priority, as well as to simplify
 * coordination.
 *<p>
 * Note that this class was moved in Jackson 1.9
 * from being a static sub-class of "org.codehaus.jackson.map.deser.SettableBeanProperty"
 * into separate class, to make it easier to use it for custom creators.
 */
public class CreatorProperty
    extends SettableBeanProperty
{
    /**
     * Placeholder that represents constructor parameter, when it is created
     * from actual constructor.
     * May be null when a synthetic instance is created.
     */
    protected final AnnotatedParameter _annotated;
    
    /**
     * @param name Name of the logical property
     * @param type Type of the property, used to find deserializer
     * @param typeDeser Type deserializer to use for handling polymorphic type
     *    information, if one is needed
     * @param contextAnnotations Contextual annotations (usually by class that
     *    declares creator [constructor, factory method] that includes
     *    this property)
     * @param param Representation of property, constructor or factory
     *    method parameter; used for accessing annotations of the property
     */
    public CreatorProperty(String name, JavaType type, TypeDeserializer typeDeser,
            Annotations contextAnnotations, AnnotatedParameter param,
            int index)
    {
        super(name, type, typeDeser, contextAnnotations);
        _annotated = param;
        _propertyIndex = index;
    }

    protected CreatorProperty(CreatorProperty src, JsonDeserializer<Object> deser) {
        super(src, deser);
        _annotated = src._annotated;
    }

    protected CreatorProperty(CreatorProperty src, Object injectableValueId) {
        super(src, injectableValueId);
        _annotated = src._annotated;
    }
    
    @Override
    public CreatorProperty withValueDeserializer(JsonDeserializer<Object> deser) {
        return new CreatorProperty(this, deser);
    }

    @Override
    public CreatorProperty withInjectableId(Object valueId) {
        return new CreatorProperty(this, _injectableValueId);
    }
    
    /*
    /**********************************************************
    /* BeanProperty impl
    /**********************************************************
     */
    
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        if (_annotated == null) {
            return null;
        }
        return _annotated.getAnnotation(acls);
    }

    @Override public AnnotatedMember getMember() {  return _annotated; }
    
    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                  Object instance)
        throws IOException, JsonProcessingException
    {
        set(instance, deserialize(jp, ctxt));
    }

    @Override
    public void set(Object instance, Object value)
        throws IOException
    {
        /* Hmmmh. Should we return quietly (NOP), or error?
         * For now, let's just bail out without fuss.
         */
        //throw new IllegalStateException("Method should never be called on a "+getClass().getName());
    }
}
