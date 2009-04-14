package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;

/**
 * This is the class that can be registered (via
 * {@link DeserializationConfig} object owner by
 * {@link ObjectMapper}) to get calledn when a potentially
 * recoverable problem is encountered during deserialization
 * process. Handlers can try to resolve the problem, throw
 * an exception or do nothing.
 *<p>
 * Default implementations for all methods implemented minimal
 * "do nothing" functionality, which is roughly equivalent to
 * not having a registered listener at all. This allows for
 * only implemented handler methods one is interested in, without
 * handling other cases.
 * 
 * @author tatu
 */
public abstract class DeserializationProblemHandler
{
    /**
     * Method called when a Json Map ("Object") entry with an unrecognized
     * name is encountered.
     * Content (supposedly) matching the property are accessible via
     * parser that can be obtained from the content.
     * Handler can also choose to skip the content; if so, it MUST return
     * true to indicate it did handle property succesfully.
     * Skipping is usually done like so:
     *<pre>
     *  ctxt.getParser().skipChildren();
     *</pre>
     * 
     * @return True if the problem was succesfully resolved (and content available
     *    used or skipped); false if listen
     */
    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonDeserializer<?> deserializer,
                                         Object bean, String propertyName)
        throws IOException, JsonProcessingException
    {
        return false;
    }
}
