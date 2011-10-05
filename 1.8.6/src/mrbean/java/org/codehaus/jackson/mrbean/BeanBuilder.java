package org.codehaus.jackson.mrbean;

import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.jackson.org.objectweb.asm.*;

import static org.codehaus.jackson.org.objectweb.asm.Opcodes.*;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.type.TypeFactory;

/**
 * Heavy lifter of mr Bean package: class that keeps track of logical POJO properties,
 * and figures out how to create an implementation class.
 * 
 * @since 1.6
 */
public class BeanBuilder
{
    protected Map<String, Property> _beanProperties = new LinkedHashMap<String,Property>();
    protected LinkedHashMap<String,Method> _unsupportedMethods = new LinkedHashMap<String,Method>();

    /**
     * Abstract class or interface that the bean is created to extend or implement.
     */
    protected final Class<?> _implementedType;

    protected final TypeFactory _typeFactory;
    
    public BeanBuilder(DeserializationConfig config, Class<?> implType)
    {
        _implementedType = implType;
        _typeFactory = config.getTypeFactory();
    }

    /*
    /**********************************************************
    /* Core public API
    /**********************************************************
     */

    /**
     * @param failOnUnrecognized If true, and an unrecognized (non-getter, non-setter)
     *   method is encountered, will throw {@link IllegalArgumentException}; if false,
     *   will implement bogus method that will throw {@link UnsupportedOperationException}
     *   if called.
     */
    public BeanBuilder implement(boolean failOnUnrecognized)
    {
        ArrayList<Class<?>> implTypes = new ArrayList<Class<?>>();
        // First: find all supertypes:
        implTypes.add(_implementedType);
        BeanUtil.findSuperTypes(_implementedType, Object.class, implTypes);
        
        for (Class<?> impl : implTypes) {
            // and then find all getters, setters, and other non-concrete methods therein:
            for (Method m : impl.getDeclaredMethods()) {
                String methodName = m.getName();
                int argCount = m.getParameterTypes().length;
    
                if (argCount == 0) { // getter?
                    if (methodName.startsWith("get") || methodName.startsWith("is") && returnsBoolean(m)) {
                        addGetter(m);
                        continue;
                    }
                } else if (argCount == 1 && methodName.startsWith("set")) {
                    addSetter(m);
                    continue;
                }
                // Otherwise, if concrete, or already handled, skip:
                // !!! note: won't handle overloaded methods properly
                if (BeanUtil.isConcrete(m) || _unsupportedMethods.containsKey(methodName)) {
                    continue;
                }
                if (failOnUnrecognized) {
                    throw new IllegalArgumentException("Unrecognized abstract method '"+methodName
                            +"' (not a getter or setter) -- to avoid exception, disable AbstractTypeMaterializer.Feature.FAIL_ON_UNMATERIALIZED_METHOD");
                }
                _unsupportedMethods.put(methodName, m);
            }
        }

        return this;
    }
    
    /**
     * Method that generates byte code for class that implements abstract
     * types requested so far.
     * 
     * @param className Fully-qualified name of the class to generate
     * @return Byte code Class instance built by this builder
     */
    public byte[] build(String className)
    {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String internalClass = getInternalClassName(className);
        String implName = getInternalClassName(_implementedType.getName());
        
        // muchos important: level at least 1.5 to get generics!!!
        // Also: abstract class vs interface...
        String superName;
        if (_implementedType.isInterface()) {
            superName = "java/lang/Object";
            cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, internalClass, null,
                    superName, new String[] { implName });
        } else {
            superName = implName;
            cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, internalClass, null,
                    implName, null);
        }
        cw.visitSource(className + ".java", null);
        BeanBuilder.generateDefaultConstructor(cw, superName);
        for (Property prop : _beanProperties.values()) {
            // First: determine type to use; preferably setter (usually more explicit); otherwise getter
            TypeDescription type = prop.selectType(_typeFactory);
            createField(cw, prop, type);
            // since some getters and/or setters may be implemented, check:
            if (!prop.hasConcreteGetter()) {
                createGetter(cw, internalClass, prop, type);
            }
            if (!prop.hasConcreteSetter()) {
                createSetter(cw, internalClass, prop, type);
            }
        }
        for (Method m : _unsupportedMethods.values()) {            
            createUnimplementedMethod(cw, internalClass, m);
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    /*
    /**********************************************************
    /* Internal methods, property discovery
    /**********************************************************
     */

    private static String getPropertyName(String methodName)
    {
        int prefixLen = methodName.startsWith("is") ? 2 : 3;
        String body = methodName.substring(prefixLen);
        StringBuilder sb = new StringBuilder(body);
        sb.setCharAt(0, Character.toLowerCase(body.charAt(0)));
        return sb.toString();
    }
    
    private static String buildGetterName(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase()+ fieldName.substring(1);
    }

    private static String buildSetterName(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase()+ fieldName.substring(1);
    }

    private static String getInternalClassName(String className) {
        return className.replace(".", "/");
    }

    private void addGetter(Method m)
    {
        Property prop = findProperty(getPropertyName(m.getName()));
        // only set if not yet set; we start with super class:
        if (prop.getGetter() == null) {
            prop.setGetter(m);        
        }
    }

    private void addSetter(Method m)
    {
        Property prop = findProperty(getPropertyName(m.getName()));
        if (prop.getSetter() == null) {
            prop.setSetter(m);
        }
    }

    private Property findProperty(String propName)
    {
        Property prop = _beanProperties.get(propName);
        if (prop == null) {
            prop = new Property(propName);
            _beanProperties.put(propName, prop);
        }
        return prop;
    }
    
    private final static boolean returnsBoolean(Method m)
    {
        Class<?> rt = m.getReturnType();
        return (rt == Boolean.class || rt == Boolean.TYPE);
    }
    
    /*
    /**********************************************************
    /* Internal methods, bytecode generation
    /**********************************************************
     */

    private static void generateDefaultConstructor(ClassWriter cw, String superName)
    {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0); // don't care (real values: 1,1)
        mv.visitEnd();
    }

    private static void createField(ClassWriter cw, Property prop, TypeDescription type)
    {
        String sig = type.hasGenerics() ? type.genericSignature() : null;
        String desc = type.erasedSignature();
        FieldVisitor fv = cw.visitField(ACC_PUBLIC, prop.getFieldName(), desc, sig, null);
        fv.visitEnd();
    }

    private static void createSetter(ClassWriter cw, String internalClassName,
            Property prop, TypeDescription propertyType)
    {
        String methodName;
        String desc;
        Method setter = prop.getSetter();
        if (setter != null) { // easy, copy as is
            desc = Type.getMethodDescriptor(setter);
            methodName = setter.getName();
        } else { // otherwise need to explicitly construct from property type (close enough)
            desc = "("+ propertyType.erasedSignature() + ")V";
            methodName = buildSetterName(prop.getName());
        }
        String sig = propertyType.hasGenerics() ? ("("+propertyType.genericSignature()+")V") : null;
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, desc, sig, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(propertyType.getLoadOpcode(), 1);
        mv.visitFieldInsn(PUTFIELD, internalClassName, prop.getFieldName(), propertyType.erasedSignature());
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0); // don't care (real values: 2, 2)
        mv.visitEnd();
    }

    private static void createGetter(ClassWriter cw, String internalClassName,
            Property prop, TypeDescription propertyType)
    {
        String methodName;
        String desc;
        Method getter = prop.getGetter();
        if (getter != null) { // easy, copy as is
            desc = Type.getMethodDescriptor(getter);
            methodName = getter.getName();
        } else { // otherwise need to explicitly construct from property type (close enough)
            desc = "()"+propertyType.erasedSignature();
            methodName = buildGetterName(prop.getName());
        }
        
        String sig = propertyType.hasGenerics() ? ("()"+propertyType.genericSignature()) : null;
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, desc, sig, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0); // load 'this'
        mv.visitFieldInsn(GETFIELD, internalClassName, prop.getFieldName(), propertyType.erasedSignature());
        mv.visitInsn(propertyType.getReturnOpcode());
        mv.visitMaxs(0, 0); // don't care (real values: 1,1)
        mv.visitEnd();
    }

    /**
     * Builder for methods that just throw an exception, basically "unsupported
     * operation" implementation.
     */
    private static void createUnimplementedMethod(ClassWriter cw, String internalClassName,
            Method method)
    {
        String exceptionName = getInternalClassName(UnsupportedOperationException.class.getName());        
        String sig = Type.getMethodDescriptor(method);
        String name = method.getName();
        // should we try to pass generic information?
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, name, sig, null, null);
        mv.visitTypeInsn(NEW, exceptionName);
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Unimplemented method '"+name+"' (not a setter/getter, could not materialize)");
        mv.visitMethodInsn(INVOKESPECIAL, exceptionName, "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(ATHROW);
        mv.visitMaxs(0, 0);  // don't care (real values: 3, 1 + method.getParameterTypes().length);
        mv.visitEnd();
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Bean that contains information about a single logical
     * property, which consists of a getter and/or setter,
     * and is used to generate getter, setter and matching
     * backing field.
     */
    private static class Property
    {
        protected final String _name;
        protected final String _fieldName;
        
        protected Method _getter;
        protected Method _setter;
        
        public Property(String name)
        {
            _name = name;
            // Let's just prefix field name with single underscore for fun...
            _fieldName = "_"+name;
        }

        public String getName() { return _name; }
        
        public void setGetter(Method m) { _getter = m; }
        public void setSetter(Method m) { _setter = m; }
        
        public Method getGetter() { return _getter; }
        public Method getSetter() { return _setter; }

        public String getFieldName() {
            return _fieldName;
        }

        /*
        private static boolean isConcrete(Method m)
        {
            return m.getModifiers()
        }
        */
        
        public boolean hasConcreteGetter() {
            return (_getter != null) && BeanUtil.isConcrete(_getter);
        }

        public boolean hasConcreteSetter() {
            return (_setter != null) && BeanUtil.isConcrete(_setter);
        }

        private TypeDescription getterType(TypeFactory tf)
        {
            Class<?> context = _getter.getDeclaringClass();
            return new TypeDescription(tf.constructType(_getter.getGenericReturnType(), context));
        }

        private TypeDescription setterType(TypeFactory tf)
        {
            Class<?> context = _setter.getDeclaringClass();
            return new TypeDescription(tf.constructType(_setter.getGenericParameterTypes()[0], context));
        }
        
        public TypeDescription selectType(TypeFactory tf)
        {
            // First: if only know setter, or getter, use that one:
            if (_getter == null) {
                return setterType(tf);
            }
            if (_setter == null) {
                return getterType(tf);
            }
            /* Otherwise must ensure they are compatible, choose more specific
             * (most often setter - type)
             */
            TypeDescription st = setterType(tf);
            TypeDescription gt = getterType(tf);
            TypeDescription specificType = TypeDescription.moreSpecificType(st, gt);
            if (specificType == null) { // incompatible...
                throw new IllegalArgumentException("Invalid property '"+getName()
                        +"': incompatible types for getter/setter ("
                        +gt+" vs "+st+")");

            }
            return specificType;
        }
    }
    
    /**
     * Helper bean used to encapsulate most details of type handling
     */
    private static class TypeDescription
    {
        private final Type _asmType;
        private JavaType _jacksonType;

        /*
        /**********************************************************
        /* Construction
        /**********************************************************
         */
        
        public TypeDescription(JavaType type)
        {
            _jacksonType = type;
            _asmType = Type.getType(type.getRawClass());
        }

        /*
        /**********************************************************
        /* Accessors
        /**********************************************************
         */

        public Class<?> getRawClass() { return _jacksonType.getRawClass(); }
        
        public String erasedSignature() {
            return _jacksonType.getErasedSignature();
        }

        public String genericSignature() {
            return _jacksonType.getGenericSignature();
        }

        /**
         * @return True if type has direct generic declaration (which may need
         *   to be copied)
         */
        public boolean hasGenerics() {
            return _jacksonType.hasGenericTypes();
        }
        
        /*
        public boolean isPrimitive() {
            return _signature.length() == 1;
        }
        */

        /*
        public int getStoreOpcode() {
            return _signatureType.getOpcode(ISTORE);
        }
        */

        public int getLoadOpcode() {
            return _asmType.getOpcode(ILOAD);
        }

        public int getReturnOpcode() {
            return _asmType.getOpcode(IRETURN);
        }
        
        @Override
        public String toString() {
            return _jacksonType.toString();
        }

        /*
        /**********************************************************
        /* Other methods
        /**********************************************************
         */

        
        public static TypeDescription moreSpecificType(TypeDescription desc1, TypeDescription desc2)
        {
            Class<?> c1 = desc1.getRawClass();
            Class<?> c2 = desc2.getRawClass();

            if (c1.isAssignableFrom(c2)) { // c2 more specific than c1
                return desc2;
            }
            if (c2.isAssignableFrom(c1)) { // c1 more specific than c2
                return desc1;
            }
            // not compatible, so:
            return null;
        }
    }
}
