package org.codehaus.jackson.map.m10r;

import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.jackson.org.objectweb.asm.ClassWriter;
import org.codehaus.jackson.org.objectweb.asm.FieldVisitor;
import org.codehaus.jackson.org.objectweb.asm.MethodVisitor;
import org.codehaus.jackson.org.objectweb.asm.Type;
import static org.codehaus.jackson.org.objectweb.asm.Opcodes.*;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.type.TypeFactory;

public class BeanBuilder
{
    protected Map<String, Property> _beanProperties = new LinkedHashMap<String,Property>();
    protected List<Method> _unsupportedMethods = new ArrayList<Method>();
    protected List<Class<?>> _implementedTypes = new ArrayList<Class<?>>();

    public BeanBuilder() { }

    /*
    /**********************************************************
    /* Core public API
    /**********************************************************
     */

    /**
     * @param parent Interface or abstract class that resulting class should
     *    complete (implement all abstract/interface methods)
     * @param failOnUnrecognized If true, and an unrecognized (non-getter, non-setter)
     *   method is encountered, will throw {@link IllegalArgumentException}; if false,
     *   will implement bogus method that will throw {@link UnsupportedOperationException}
     *   if called.
     */
    public BeanBuilder implement(Class<?> parent, boolean failOnUnrecognized)
    {
        _implementedTypes.add(parent);

        // TODO: recursively check super-interfaces/classes
        for (Method m : parent.getMethods()) {
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
            if (failOnUnrecognized) {
                throw new IllegalArgumentException("Unrecognized abstract method '"+methodName
                        +"' (not a getter or setter) -- to avoid exception, disable AbstractTypeMaterializer.Feature.FAIL_ON_UNMATERIALIZED_METHOD");
            }
            _unsupportedMethods.add(m);
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
        ClassWriter cw = new ClassWriter(0);
        String internalClass = getInternalClassName(className);
        
        String[] parents = new String[_implementedTypes.size()];
        for (int i = 0; i < _implementedTypes.size(); i++) {
            parents[i] = getInternalClassName(_implementedTypes.get(i).getName());
        }
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER, internalClass, null,
                "java/lang/Object", parents);
        cw.visitSource(className + ".java", null);
        BeanBuilder.generateDefaultConstructor(cw);
        for (Property prop : _beanProperties.values()) {
            // First: determine type to use; preferably setter (usually more explicit); otherwise getter
            TypeDescription type = prop.selectType();
            createField(cw, prop, type);
            createGetter(cw, internalClass, prop, type);
            createSetter(cw, internalClass, prop, type);
        }
        for (Method m : _unsupportedMethods) {            
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
        if (prop.getGetter() != null) { // dup?
            throw new IllegalArgumentException("Invalid property '"+prop.getName()+"'; multiple getters; '"
                    +m.getName()+"()' vs '"+prop.getGetter().getName()+"()'");
        }
        prop.setGetter(m);        
    }

    private void addSetter(Method m)
    {
        Property prop = findProperty(getPropertyName(m.getName()));
        if (prop.getSetter() != null) { // dup?
            throw new IllegalArgumentException("Invalid property '"+prop.getName()+"'; multiple setters; '"
                    +m.getName()+"()' vs '"+prop.getSetter().getName()+"()'");
        }
        prop.setSetter(m);
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

    private static void generateDefaultConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static void createField(ClassWriter cw, Property prop, TypeDescription type)
    {
        FieldVisitor fv = cw.visitField(0, prop.getFieldName(), type.signature(), null, null);
        fv.visitEnd();
    }

    private static void createSetter(ClassWriter cw, String internalClassName,
            Property prop, TypeDescription propertyType)
    {
        String methodName;
        String sig;
        Method setter = prop.getSetter();
        if (setter != null) { // easy, copy as is
            sig = Type.getMethodDescriptor(setter);
            methodName = setter.getName();
        } else { // otherwise need to explicitly construct from property type (close enough)
            sig = "("+ propertyType.signature() + ")V";
            methodName = buildSetterName(prop.getName());
        }
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, sig, null, null);
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(propertyType.getLoadOpcode(), 1);
        mv.visitFieldInsn(PUTFIELD, internalClassName, prop.getFieldName(), propertyType.signature());
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private static void createGetter(ClassWriter cw, String internalClassName,
            Property prop, TypeDescription propertyType)
    {
        String methodName;
        String sig;
        Method getter = prop.getGetter();
        if (getter != null) { // easy, copy as is
            sig = Type.getMethodDescriptor(getter);
            methodName = getter.getName();
        } else { // otherwise need to explicitly construct from property type (close enough)
            sig = "()"+propertyType.signature();
            methodName = buildGetterName(prop.getName());
        }
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, sig, null, null);
        mv.visitVarInsn(ALOAD, 0); // load 'this'
        mv.visitFieldInsn(GETFIELD, internalClassName, prop.getFieldName(), propertyType.signature());
        mv.visitInsn(propertyType.getReturnOpcode());
        mv.visitMaxs(1, 1);
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
        mv.visitMaxs(3, 1 + method.getParameterTypes().length);
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
        
        public Property(String name) {
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
        
        public TypeDescription selectType()
        {
            // First: if only know setter, or getter, use that one:
            if (_getter == null) {
                return TypeDescription.fromFirstArgType(_setter);
            }
            if (_setter == null) {
                return TypeDescription.fromReturnType(_getter);
            }
            /* Otherwise must ensure they are compatible, choose more specific
             * (most often setter - type)
             */
            TypeDescription st = TypeDescription.fromFirstArgType(_setter);
            TypeDescription gt = TypeDescription.fromReturnType(_getter);
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
        private final Type _signatureType;
        private final java.lang.reflect.Type _rawJavaType;
        private final String _signature;
        private JavaType _jacksonType;

        /*
        /**********************************************************
        /* Construction
        /**********************************************************
         */
        
        public TypeDescription(Type sigType, java.lang.reflect.Type rawType)
        {
            _signatureType = sigType;
            _signature = sigType.getDescriptor();
            _rawJavaType = rawType;
        }
        
        public static TypeDescription fromReturnType(Method m)
        {
            return new TypeDescription(Type.getReturnType(m), m.getGenericReturnType());
        }
        
        public static TypeDescription fromFirstArgType(Method m)
        {
            return new TypeDescription(Type.getArgumentTypes(m)[0], m.getGenericParameterTypes()[0]);
        }

        /*
        /**********************************************************
        /* Accessors
        /**********************************************************
         */
        
        public String signature() {
            return _signature;
        }

        public boolean isPrimitive() {
            return _signature.length() == 1;
        }
        
        protected JavaType getJacksonType()
        {
            if (_jacksonType == null) {
                _jacksonType = TypeFactory.type(_rawJavaType);
            }
            return _jacksonType;
        }
 
        public int getStoreOpcode() {
            return _signatureType.getOpcode(ISTORE);
        }

        public int getLoadOpcode() {
            return _signatureType.getOpcode(ILOAD);
        }

        public int getReturnOpcode() {
            return _signatureType.getOpcode(IRETURN);
        }
        
        @Override
        public String toString() {
            return getJacksonType().toString();
        }

        /*
        /**********************************************************
        /* Other methods
        /**********************************************************
         */

        
        public static TypeDescription moreSpecificType(TypeDescription desc1, TypeDescription desc2)
        {
            Class<?> c1 = desc1.getJacksonType().getRawClass();
            Class<?> c2 = desc2.getJacksonType().getRawClass();

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
