package org.codehaus.jackson.map.type;

import java.lang.annotation.Annotation;

/**
 * Simple interface that defines API used to filter out irrelevant
 * annotations.
 */
public interface AnnotationFilter
{
    public boolean includeAnnotation(Annotation a);
}
