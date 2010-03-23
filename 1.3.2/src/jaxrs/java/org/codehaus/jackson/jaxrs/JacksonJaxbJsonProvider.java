package org.codehaus.jackson.jaxrs;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * JSON content type provider automatically configured to use both Jackson
 * and JAXB annotations (in that order of priority). Otherwise functionally
 * same as {@link JacksonJsonProvider}.
 *<p>
 * Typical usage pattern is to just instantiate instance of this
 * provider for JAX-RS and use as is: this will use both Jackson and
 * JAXB annotations (with Jackson annotations having priority).
 * 
 * @since 1.3
 */
public class JacksonJaxbJsonProvider extends JacksonJsonProvider {
    /**
     * Default annotation sets to use, if not explicitly defined during
     * construction: use Jackson annotations if found; if not, use
     * JAXB annotations as fallback.
     */
    public final static Annotations[] DEFAULT_ANNOTATIONS = {
        Annotations.JACKSON, Annotations.JAXB
    };

    /**
     * Default constructor, usually used when provider is automatically
     * configured to be used with JAX-RS implementation.
     */
    public JacksonJaxbJsonProvider()
    {
        this(null, DEFAULT_ANNOTATIONS);
    }

    /**
     * @param annotationsToUse Annotation set(s) to use for configuring
     *    data binding
     */
    public JacksonJaxbJsonProvider(Annotations... annotationsToUse)
    {
        this(null, annotationsToUse);
    }
    
    /**
     * Constructor to use when a custom mapper (usually components
     * like serializer/deserializer factories that have been configured)
     * is to be used.
     */
    public JacksonJaxbJsonProvider(ObjectMapper mapper, Annotations[] annotationsToUse)
    {
        super(mapper, annotationsToUse);
    }
}
