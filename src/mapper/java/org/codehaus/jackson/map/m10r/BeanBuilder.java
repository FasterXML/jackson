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
    protected Map<String, TypeDescription> properties = new LinkedHashMap<String, TypeDescription>();
    protected List<Method> throwMethods = new ArrayList<Method>();
    protected List<Class<?>> implementing = new ArrayList<Class<?>>();

    public BeanBuilder() { }

    /*
    /**********************************************************
    /* Core public API
    /**********************************************************
     */

    /**
     * @param parent Interface or abstract class that resulting class should
     *    complete (implement all abstract/interface methods)
     * @return
     */
    public BeanBuilder implement(Class<?> parent)
    {
        this.implementing.add(parent);

        // TODO: recursively check super-interfaces/classes
        for (Method m : parent.getMethods()) {
            TypeDescription type;

            if (m.getName().startsWith("get")) {
                type = TypeDescription.fromReturnType(m);
            } else if (m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
                type = TypeDescription.fromFirstArgType(m);
            } else {
                throwMethods.add(m);
                continue;
            }
            String name = getFieldName(m.getName());
            TypeDescription prevType = properties.get(name);
            if (prevType != null) { // if we already saw a setter or getter, need to check compatibility
                // must be assignable; if so, more specific should be used
                TypeDescription specific = TypeDescription.moreSpecificType(prevType, type);
                if (specific == null) { // not compatible
                    throw new IllegalArgumentException("Invalid property '"+name+"': incompatible types for getter/setter ("
                            +prevType+" vs "+type+")");
                }
                type = specific;
            }
            properties.put(name, type);
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

        String[] parents = new String[implementing.size()];
        for (int i = 0; i < implementing.size(); i++) {
            parents[i] = getInternalClassName(implementing.get(i).getName());
        }
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER, internalClass, null,
                "java/lang/Object", parents);
        cw.visitSource(className + ".java", null);
        BeanBuilder.generateDefaultConstructor(cw);
        for (Map.Entry<String, TypeDescription> propEntry : properties.entrySet()) {
            String propName = propEntry.getKey();
            TypeDescription type = propEntry.getValue();

            createField(cw, propName, type);
            createGetter(cw, internalClass, propName, type);
            createSetter(cw, internalClass, propName, type);
        }

        for (Method m : throwMethods) {
            createUnimplementedMethod(cw, internalClass, m);
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    /*
    /**********************************************************
    /* Internal methods
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

    private static void createField(ClassWriter cw, String fieldName, TypeDescription type)
    {
        FieldVisitor fv = cw.visitField(0, fieldName, type.signature(), null, null);
        fv.visitEnd();
    }

    private static void createSetter(ClassWriter cw, String internalClassName,
            String fieldName, TypeDescription argType)
    {
        String methodName = getSetterName(fieldName);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "("+ argType.signature() + ")V", null, null);
        int loadOp = argType.getLoadOpcode();
        mv.visitVarInsn(loadOp, 0);
        mv.visitVarInsn(loadOp, 1);
        // !!! TODO: probably different for primitive types?
        if (argType.isPrimitive()) {
        } else {
        }
        mv.visitFieldInsn(PUTFIELD, internalClassName, fieldName, argType.signature());
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private static void createGetter(ClassWriter cw, String internalClassName,
            String fieldName, TypeDescription returnType)
    {
        String methodName = getGetterName(fieldName);
        String typeSignature = returnType.signature();
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "()"+ typeSignature, null, null);
        int loadOp = returnType.getLoadOpcode();
        mv.visitVarInsn(loadOp, 0);
        mv.visitFieldInsn(GETFIELD, internalClassName, fieldName, typeSignature);
        mv.visitInsn(returnType.getReturnOpcode());
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
        // should we try to pass generic information?
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, method.getName(), sig, null, null);

        mv.visitTypeInsn(NEW, exceptionName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, exceptionName, "<init>", "()V");
        mv.visitInsn(ATHROW);
        mv.visitMaxs(2, 1 + method.getParameterTypes().length);
        mv.visitEnd();
    }

    private static String getInternalClassName(String className) {
        return className.replace(".", "/");
    }

    private static String getFieldName(String getterMethodName) {
        char[] name = getterMethodName.substring(3).toCharArray();
        name[0] = Character.toLowerCase(name[0]);
        final String propName = new String(name);

        return propName;
    }
    
    private static String getGetterName(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase()+ fieldName.substring(1);
    }

    private static String getSetterName(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase()+ fieldName.substring(1);
    }
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

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
