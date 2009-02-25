package org.codehaus.jackson.map.introspect;

import java.lang.reflect.Method;

/**
 * Helper class needed to be able to efficiently store {@link Method}s
 * in {@link java.util.Map}s.
 */
public final class MethodKey
{
    final String _name;
    final Class<?>[] _argTypes;

    public MethodKey(Method m)
    {
        _name = m.getName();
        _argTypes = m.getParameterTypes();
    }

    @Override
    public String toString() {
        return _name + "(" + _argTypes.length+"-args)";
    }

    @Override
    public int hashCode()
    {
        return _name.hashCode() + _argTypes.length;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) {
            return false;
        }
        MethodKey other = (MethodKey) o;
        if (!_name.equals(other._name)) {
            return false;
        }
        Class<?>[] otherArgs = other._argTypes;
        int len = _argTypes.length;
        if (otherArgs.length != len) {
            return false;
        }
        for (int i = 0; i < len; ++i) {
            /* 23-Feb-2009, tatu: Are there any cases where we would have to
             *   consider some narrowing conversions or such? For now let's
             *   assume exact type match is enough
             */
            if (otherArgs[i] != _argTypes[i]) {
                return false;
            }
        }
        return true;
    }
}
