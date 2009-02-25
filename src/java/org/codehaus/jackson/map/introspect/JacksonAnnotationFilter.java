package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;

/**
 * Annotation filter that will pass Jackson-owned annotations, as well
 * as those for which we can't access information to make the
 * determination.
 */
public final class JacksonAnnotationFilter
    implements AnnotationFilter
{
    /**
     * To recognize annotations that Jackson package defines, let's
     * just assume they'll all be under {@link org.codehaus.jackson}
     * package. This is only used as an optimization, to reduce number
     * of annotations we keep track of, so that we can ignore
     * annotations we don't care about.
     */
    final static String JACKSON_PKG_PREFIX = "org.codehaus.jackson";
    
    public final static JacksonAnnotationFilter instance = new JacksonAnnotationFilter();
    
    private JacksonAnnotationFilter() { }
    
    public boolean includeAnnotation(Annotation ann)
    {
        Class<? extends Annotation> acls = ann.annotationType();
        Package pkg = acls.getPackage();
            /* Let's be conservative and also include ones where we
             * don't know the package as well; filtering is an optimization,
             * and not a hard limitation
             */
        return (pkg == null) || (pkg.getName().startsWith(JACKSON_PKG_PREFIX));
    }
}
