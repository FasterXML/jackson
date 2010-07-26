package org.codehaus.jackson.map.type;

import org.codehaus.jackson.type.JavaType;

public abstract class TypeBase extends JavaType
{
    /**
     * Lazily initialized external representation of the type
     */
    volatile String _canonicalName;

    protected TypeBase(Class<?> raw) {
        super(raw);
    }
    
    @Override
    public String toCanonical()
    {
    	String str = _canonicalName;
    	if (str == null) {
            str = buildCanonicalName();
    	}
    	return str;
    }
    
    protected abstract String buildCanonicalName();

    protected final JavaType copyHandlers(JavaType fromType)
    {
        _valueHandler = fromType.getValueHandler();
        _typeHandler = fromType.getTypeHandler();
        return this;
    }
}
