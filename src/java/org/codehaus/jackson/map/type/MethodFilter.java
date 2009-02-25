package org.codehaus.jackson.map.type;

import java.lang.reflect.Method;

/**
 * Simple interface that defines API used to filter out irrelevant
 * methods
 */
public interface MethodFilter
{
    public boolean includeMethod(Method m);
}
