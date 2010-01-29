package org.codehaus.jackson.map.jsontype;

/**
 * Simple container class for types with optional logical name, used
 * as external identifier
 * 
 * @author tatu
 * @since 1.5
 */
public final class NamedType
{
    protected final Class<?> _class;
    protected final String _name;
    protected final int _hashCode;
    
    public NamedType(Class<?> c) { this(c, null); }
    
    public NamedType(Class<?> c, String name)
    {
        _class = c;
        _name = (name == null || name.length() == 0) ? null : name;
        _hashCode = c.getName().hashCode();
    }

    public Class<?> getType() { return _class; }
    public String getName() { return _name; }
    
    /**
     * Equality is defined based on class only, not on name
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        return _class == ((NamedType) o)._class;
    }

    @Override
    public int hashCode() { return _hashCode; }
}
