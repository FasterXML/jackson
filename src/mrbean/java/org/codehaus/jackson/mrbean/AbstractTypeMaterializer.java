package org.codehaus.jackson.mrbean;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

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
        Class<?> impl = materializeClass(type.getRawClass());

        // !!! TEST
/*        
        for (Class<?> c : new Class<?>[] { type.getRawClass(), impl } ) {
            System.out.println("Class "+c.getName()+":");
            for (Method m : c.getDeclaredMethods()) {
                System.out.println("  method '"+m+"', returns "+TypeFactory.type(m.getGenericReturnType(), c));
                for (java.lang.reflect.Type t : m.getGenericParameterTypes()) {
                   System.out.println("   param: "+TypeFactory.type(t, c));
                }
            }
        }
*/        
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
        String newName = _defaultPackage+cls.getName();
        BeanBuilder builder = new BeanBuilder();
        byte[] bytecode = builder.implement(cls, isEnabled(Feature.FAIL_ON_UNMATERIALIZED_METHOD)).build(newName);
        Class<?> result = _classLoader.loadAndResolve(newName, bytecode);
        
        // TEST: uncomment for more debugging during development (never for release builds)
        /*
        
System.out.println("DEBUG: "+bytecode.length+" bytes for "+newName); 
try {
    System.out.println("<visit name='"+result.getName()+"'>");
ClassReader cr = new ClassReader(bytecode);
cr.accept(new ClassVisitor() {
    @Override  public void visit(int arg0, int access, String name, String desc ,String sig, String[] arg5) { }
    @Override public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        System.out.println(" Annotation '"+arg0+"'...");
        return null;
    }
    @Override public void visitAttribute(Attribute arg0) { System.out.println(" Attribute '"+arg0+"'"); }
    @Override public void visitEnd() { }
    
    @Override
    public FieldVisitor visitField(int arg0, String name, String desc, String sig, Object arg4) {
        System.out.println(" Field '"+name+"; desc '"+desc+"', sig '"+sig+"', value: "+arg4);
        return null;
    }
    
    @Override
    public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) { }
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        System.out.println("Method '"+name+"' (access 0x"+Integer.toHexString(access)+"): desc "+desc+", signature "+signature);
        return null;
    }
    
    @Override public void visitOuterClass(String arg0, String arg1, String arg2) { }
    
    @Override public void visitSource(String arg0, String arg1) { }
}, 0);
} catch (Exception e) { throw new RuntimeException(e);
}
System.out.println("</visit>");
*/
        return result;
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
