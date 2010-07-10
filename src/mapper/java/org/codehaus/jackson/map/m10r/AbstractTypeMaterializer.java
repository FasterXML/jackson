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
    /**
     * We will use per-materializer class loader for now; would be nice
     * to find a way to reduce number of class loaders (and hence
     * number of generated classes!) constructed...
     */
    protected final MyClassLoader _classLoader;
    
    public AbstractTypeMaterializer() {
        _classLoader = new MyClassLoader(getClass().getClassLoader());
    }

    @Override
    public JavaType resolveAbstractType(DeserializationConfig config, JavaType type)
    {
        Class<?> impl = materializeClass(type.getRawClass());
        return TypeFactory.type(impl);
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected Class<?> materializeClass(Class<?> cls)
    {
        // Need to have proper name mangling in future, but for now...
        String newName = "materialized."+cls.getName();
        BeanBuilder builder = new BeanBuilder();
        byte[] bytecode = builder.implement(cls).build(newName);
        return _classLoader.loadAndResolve(newName, bytecode);
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */
    
    private static class MyClassLoader extends ClassLoader
    {
        public MyClassLoader(ClassLoader parent)
        {
            super(parent);
        }

        public Class<?> loadAndResolve(String className, byte[] byteCode)
            throws IllegalArgumentException
        {
            Class<?> impl;
            try {
                impl = defineClass(className, byteCode, 0, byteCode.length);
            } catch (LinkageError e) {
                throw new IllegalArgumentException("Failed to load class '"+className+"': "+e.getMessage() ,e);
            }
            // important: must also resolve the class...
            resolveClass(impl);
            return impl;
        }
    }

    /* // old loading code
    private static Class<?> loadClass(String className, byte[] b) {
        // override classDefine (as it is protected) and define the class.
        Class<?> clazz = null;
        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            Class<?> cls = Class.forName("java.lang.ClassLoader");
            java.lang.reflect.Method method = cls.getDeclaredMethod(
                    "defineClass", new Class<?>[] { String.class, byte[].class,
                            int.class, int.class });

            // protected method invocation
            method.setAccessible(true);
            try {
                Object[] args = new Object[] { className, b, new Integer(0),
                        new Integer(b.length) };
                clazz = (Class<?>) method.invoke(loader, args);
            } finally {
                method.setAccessible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return clazz;
    }
     */
}
