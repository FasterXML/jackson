package org.codehaus.jackson.map.m10r;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Nifty class for pulling implementations of classes out of thin air.
 * 
 * @author tatu
 * @author sunny
 */
public class AbstractTypeMaterializer
    extends AbstractTypeResolver
{
    public AbstractTypeMaterializer() { }

    @Override
    public JavaType resolveAbstractType(DeserializationConfig config, JavaType type)
    {
        Class<?> cls = type.getRawClass();
        // Need to have proper name mangling in future, but for now...
        String newName = "materialized."+cls.getName();
        BeanBuilder builder = new BeanBuilder(newName);
        builder.implement(cls);
        Class<?> impl = builder.load();
        return TypeFactory.type(impl);
    }
}
