package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Value class used to set bean property values
 */
public final class SettableBeanProperty
{
    final Method _setter;

    public SettableBeanProperty(Method setter)
    {
        _setter = setter;
    }

    public void set(Object instance, Object value)
    {
        try {
            _setter.invoke(instance, value);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            // let's wrap the innermost problem
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            throw new IllegalArgumentException(t.getMessage(), t);
        }
    }
}
