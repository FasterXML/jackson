package org.codehaus.jackson.mrbean;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.Versioned;
import org.codehaus.jackson.map.AbstractTypeResolver;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.VersionUtil;

/**
 * Nifty class for pulling implementations of classes out of thin air.
 *<p>
 * ... friends call him Mister Bean... :-)
 * 
 * @author tatu
 * @author sunny
 * 
 * @since 1.6
 */
public class AbstractTypeMaterializer
    extends AbstractTypeResolver
    implements Versioned
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
     * Default package to use for generated classes.
     */
    public final static String DEFAULT_PACKAGE_FOR_GENERATED = "org.codehaus.jackson.generated.";
    
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

    /**
     * Package name to use as prefix for generated classes.
     */
    protected String _defaultPackage = DEFAULT_PACKAGE_FOR_GENERATED;
    
    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */
    
    public AbstractTypeMaterializer() {
        this(null);
    }

    public AbstractTypeMaterializer(ClassLoader parentClassLoader)
    {
        if (parentClassLoader == null) {
            parentClassLoader = getClass().getClassLoader();
        }
        _classLoader = new MyClassLoader(parentClassLoader);
    }

    /**
     * Method that will return version information stored in and read from jar
     * that contains this class.
     * 
     * @since 1.6
     */
    public Version version() {
        return VersionUtil.versionFor(getClass());
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

    public void setDefaultPackage(String defPkg)
    {
        if (!defPkg.endsWith(".")) {
            defPkg = defPkg + ".";
        }
        _defaultPackage = defPkg;
    }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */
    
    @Override
    public JavaType resolveAbstractType(DeserializationConfig config, JavaType type)
    {
        /* 19-Feb-2011, tatu: Future plans may include calling of this method for all kinds
         *    of abstract types. So as simple precaution, let's limit kinds of types we
         *    will try materializa implementations for.
         */
        /* We won't be handling any container types (Collections, Maps and arrays),
         * Throwables or enums.
         */
        if (type.isContainerType() || type.isPrimitive() || type.isEnumType() || type.isThrowable()) {
            return null;
        }
        // might want to skip proxies, local types too... but let them be for now:
        //if (intr.findTypeResolver(beanDesc.getClassInfo(), type) == null) {
        return config.constructType(materializeClass(config, type.getRawClass()));
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected Class<?> materializeClass(DeserializationConfig config, Class<?> cls)
    {
        // Need to have proper name mangling in future, but for now...
        String newName = _defaultPackage+cls.getName();
        BeanBuilder builder = new BeanBuilder(config, cls);
        byte[] bytecode = builder.implement(isEnabled(Feature.FAIL_ON_UNMATERIALIZED_METHOD)).build(newName);
        Class<?> result = _classLoader.loadAndResolve(newName, bytecode, cls);
        return result;
    }
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * To support actual dynamic loading of bytecode we need a simple
     * custom classloader.
     */
    private static class MyClassLoader extends ClassLoader
    {
        public MyClassLoader(ClassLoader parent)
        {
            super(parent);
        }

        /**
         * @param targetClass Interface or abstract class that class to load should extend or 
         *   implement
         */
        public Class<?> loadAndResolve(String className, byte[] byteCode, Class<?> targetClass)
            throws IllegalArgumentException
        {
            // First things first: just to be sure; maybe we have already loaded it?
            Class<?> old = findLoadedClass(className);
            if (old != null && targetClass.isAssignableFrom(old)) {
                return old;
            }
            
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

    /* // old loading code that uses system class loader
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
