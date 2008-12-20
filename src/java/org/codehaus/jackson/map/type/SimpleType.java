package org.codehaus.jackson.map.type;

/**
 * Simple types are defined as anything other than one of recognized
 * container types (arrays, Collections, Maps). For our needs we
 * need not know anything further, since we have no way of dealing
 * with generic types other than Collections and Maps.
 */
public final class SimpleType
    extends JavaType
{
    public SimpleType(Class<?> cls)
    {
        super(cls);
    }

    @Override
        public String toString()
    {
        return "[simple type "+_class.getName()+"]";
    }

    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        SimpleType other = (SimpleType) o;

        // Classes must be identical... 
        return (other._class == this._class);
    }
}
