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
     * Enumeration that defines togglable features that guide
     * the serialization feature.
     */
    public enum Feature {
        /**
         * Feature that determines what happens if an "unrecognized"
         * (non-getter, non-setter) abstract method is encountered: if set to
         * true, will throw an exception during materialization; if false,
         * will materialize method that throws exception only if called.
         */
        FAIL_ON_UNMATERIALIZED_METHOD(false)
        ;

        final boolean _defaultState;

        // Method that calculates bit set (flags) of all features that are enabled by default.
        protected static int collectDefaults() {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
                
        private Feature(boolean defaultState) { _defaultState = defaultState; }
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return (1 << ordinal()); }
    }

    /**
     * Bitfield (set of flags) of all Features that are enabled
     * by default.
     */
    protected final static int DEFAULT_FEATURE_FLAGS = Feature.collectDefaults();
    
    /**
     * We will use per-materializer class loader for now; would be nice
     * to find a way to reduce number of class loaders (and hence
     * number of generated classes!) constructed...
     */
    protected final MyClassLoader _classLoader;

    /**
     * Bit set that contains all enabled features
     */
    protected int _featureFlags = DEFAULT_FEATURE_FLAGS;
    
    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */
    
    public AbstractTypeMaterializer() {
        _classLoader = new MyClassLoader(getClass().getClassLoader());
    }

    /**
     * Method for checking whether given feature is enabled or not
     */
    public final boolean isEnabled(Feature f) {
        return (_featureFlags & f.getMask()) != 0;
    }

    /**
     * Method for enabling specified  feature.
     */
    public void enable(Feature f) {
        _featureFlags |= f.getMask();
    }

    /**
     * Method for disabling specified feature.
     */
    public void disable(Feature f) {
        _featureFlags &= ~f.getMask();
    }

    /**
     * Method for enabling or disabling specified feature.
     */
    public void set(Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
    }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */
    
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
        byte[] bytecode = builder.implement(cls, isEnabled(Feature.FAIL_ON_UNMATERIALIZED_METHOD)).build(newName);
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
