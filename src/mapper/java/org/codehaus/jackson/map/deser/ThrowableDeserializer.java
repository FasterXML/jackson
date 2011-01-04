package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Deserializer that builds on basic {@link BeanDeserializer} but
 * override some aspects like instance construction.
 *<p>
 * Note that this deserializer was significantly changed in Jackson 1.7
 * (due to massive changes in {@link BeanDeserializer}).
 */
public class ThrowableDeserializer
    extends BeanDeserializer
{
    protected final static String PROP_NAME_MESSAGE = "message";

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public ThrowableDeserializer(BeanDeserializer baseDeserializer)
    {
        super(baseDeserializer);
    }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // 30-Sep-2010, tatu: Need to allow use of @JsonCreator, so:
        if (_propertyBasedCreator != null) { // proper @JsonCreator
            return _deserializeUsingPropertyBased(jp, ctxt);
        }
        if (_delegatingCreator != null) { // delegate based one (single-arg, no property name)
            return _delegatingCreator.deserialize(jp, ctxt);
        }
        if (_beanType.isAbstract()) { // for good measure, check this too
            throw JsonMappingException.from(jp, "Can not instantiate abstract type "+_beanType
                    +" (need to add/enable type information?)");
        }
        // and finally, verify we do have single-String arg constructor (if no @JsonCreator)
        if (_stringCreator == null) {
            throw new JsonMappingException("Can not deserialize Throwable of type "+_beanType
                    +" without having either single-String-arg constructor; or explicit @JsonCreator");
        }
        
        Object throwable = null;
        Object[] pending = null;
        int pendingIx = 0;

        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            SettableBeanProperty prop = _beanProperties.find(propName);
            jp.nextToken(); // to point to field value

            if (prop != null) { // normal case
                if (throwable != null) {
                    prop.deserializeAndSet(jp, ctxt, throwable);
                    continue;
                }
                // nope; need to defer
                if (pending == null) {
                    int len = _beanProperties.size();
                    pending = new Object[len + len];
                }
                pending[pendingIx++] = prop;
                pending[pendingIx++] = prop.deserialize(jp, ctxt);
                continue;
            }

            // Maybe it's "message"?
            if (PROP_NAME_MESSAGE.equals(propName)) {
                throwable = _stringCreator.construct(jp.getText());
                // any pending values?
                if (pending != null) {
                    for (int i = 0, len = pendingIx; i < len; i += 2) {
                        prop = (SettableBeanProperty)pending[i];
                        prop.set(throwable, pending[i+1]);
                    }
                    pending = null;
                }
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
                continue;
            }
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(jp, ctxt, throwable, propName);
                continue;
            }
            // Unknown: let's call handler method
            handleUnknownProperty(jp, ctxt, throwable, propName);
        }
        // Sanity check: did we find "message"?
        if (throwable == null) {
            /* 15-Oct-2010, tatu: Can't assume missing message is an error, since it may be
             *   suppressed during serialization, as per [JACKSON-388].
             *   
             *   Should probably allow use of default constructor, too...
             */
            //throw new JsonMappingException("No 'message' property found: could not deserialize "+_beanType);
            throwable = _stringCreator.construct(null);
            // any pending values?
            if (pending != null) {
                for (int i = 0, len = pendingIx; i < len; i += 2) {
                    SettableBeanProperty prop = (SettableBeanProperty)pending[i];
                    prop.set(throwable, pending[i+1]);
                }
            }
        }
        return throwable;
    }
}
