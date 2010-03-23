package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Deserializer that builds on basic {@link BeanDeserializer} but
 * override some aspects like instance construction.
 */
public class ThrowableDeserializer
    extends BeanDeserializer
{
    final static String PROP_NAME_MESSAGE = "message";

    /*
    ///////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////
     */

    public ThrowableDeserializer(JavaType type)
    {
        super(type);
    }

    /*
    ///////////////////////////////////////////////////////
    // Overridden methods
    ///////////////////////////////////////////////////////
     */

    @Override
    public void validateCreators()
    {
        /* Unlike regular beans, exceptions require String constuctor
         *
         * !!! 07-Apr-2009, tatu: Ideally we would try to use String+Throwable
         *   constructor, but for now String one has to do
         */
        if (_stringCreator == null) {
            throw new IllegalArgumentException("Can not create Throwable deserializer for ("+_beanType+"): no single-String Creator (constructor, factory method) found");
        }
    }

    @Override
    public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        Object throwable = null;
        Object[] pending = null;
        int pendingIx = 0;

        while (jp.nextToken() != JsonToken.END_OBJECT) { // otherwise field name
            String propName = jp.getCurrentName();
            SettableBeanProperty prop = _props.get(propName);

            if (prop != null) { // normal case
                if (throwable != null) {
                    prop.deserializeAndSet(jp, ctxt, throwable);
                    continue;
                }
                // nope; need to defer
                if (pending == null) {
                    int len = _props.size();
                    pending = new Object[len + len];
                }
                pending[pendingIx++] = prop;
                pending[pendingIx++] = prop.deserialize(jp, ctxt);
                continue;
            }

            @SuppressWarnings("unused")
			JsonToken t = jp.nextToken();

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
            // Unknown: let's call handler method
            handleUnknownProperty(ctxt, throwable, propName);
        }
        // Sanity check: did we find "message"?
        if (throwable == null) {
            throw new JsonMappingException("No 'message' property found: could not deserialize "+_beanType);
        }
        return throwable;
    }
}
