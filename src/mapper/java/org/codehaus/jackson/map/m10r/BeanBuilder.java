package org.codehaus.jackson.map.m10r;

import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.jackson.org.objectweb.asm.ClassWriter;
import org.codehaus.jackson.org.objectweb.asm.FieldVisitor;
import org.codehaus.jackson.org.objectweb.asm.MethodVisitor;
import org.codehaus.jackson.org.objectweb.asm.Opcodes;
import static org.codehaus.jackson.org.objectweb.asm.Opcodes.*;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.type.TypeFactory;

public class BeanBuilder
{
    protected Map<String, JavaType> properties = new LinkedHashMap<String, JavaType>();
    protected Map<String, ThrowMethodType> throwMethods = new LinkedHashMap<String, ThrowMethodType>();
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
            JavaType type;

            if (m.getName().startsWith("get")) {
                type = TypeFactory.type(m.getGenericReturnType());                
            } else if (m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
                type = TypeFactory.type(m.getGenericParameterTypes()[0]);
            } else {
                addThrow(m.getName(), m.getParameterTypes(), m.getReturnType(),
                        UnsupportedOperationException.class);
                continue;
            }
            String name = getFieldName(m.getName());
            JavaType prevType = properties.get(name);
            if (prevType != null) { // if we already saw a setter or getter, need to check compatibility
                // must be assignable; if so, more specific should be used
                Class<?> oldRaw = prevType.getRawClass();
                Class<?> newRaw = type.getRawClass();
                if (oldRaw.isAssignableFrom(newRaw)) {
                    // type is more specific
                    properties.put(name, type);
                } else if (newRaw.isAssignableFrom(oldRaw)) {
                    // old type more specific, retain
                    continue;
                } else { // incompatible: error
                    throw new IllegalArgumentException("Invalid property '"+name+"': incompatible types for getter/setter ("
                            +newRaw.getName()+" vs "+oldRaw.getName()+")");
                }
            } else {
                properties.put(name, type);
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
        for (Map.Entry<String, JavaType> propEntry : properties.entrySet()) {
            String propName = propEntry.getKey();
            Class<?> propClass = propEntry.getValue().getRawClass();

            TypeType type = resolveType(propClass);
            createField(cw, propName, type);
            createGetter(cw, internalClass, propName, type);
            createSetter(cw, internalClass, propName, type);
        }

        for (Map.Entry<String, ThrowMethodType> throwEntry : throwMethods.entrySet()) {
            ThrowMethodType thr = throwEntry.getValue();

            createThrow(cw, internalClass, throwEntry.getKey(), thr.paramTypes, thr.returnType,
                    thr.exceptionType);
        }

        cw.visitEnd();
        return cw.toByteArray();
    }
    
    /*
    /**********************************************************
    /* Build methods
    /**********************************************************
     */

    public BeanBuilder addThrow(String name, Class<?>[] paramTypes,
            Class<?> returnType, Class<?> exceptionType) {
        this.throwMethods.put(name, new ThrowMethodType(name, paramTypes,
                returnType, exceptionType));

        return this;
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

    private static void createField(ClassWriter cw, String fieldName, TypeType type)
    {
        FieldVisitor fv = cw.visitField(0, fieldName, type.signature(), null, null);
        fv.visitEnd();
    }

    private static void createSetter(ClassWriter cw, String internalClassName,
            String fieldName, TypeType argType)
    {
        String methodName = getSetterName(fieldName);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "("+ argType.signature() + ")V", null, null);
        int loadOp = argType.loadOpCode();
        mv.visitVarInsn(loadOp, 0);
        mv.visitVarInsn(loadOp, 1);
        mv.visitFieldInsn(PUTFIELD, internalClassName, fieldName, argType.signature());
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private static void createGetter(ClassWriter cw, String internalClassName,
            String fieldName, TypeType returnType)
    {
        String methodName = getGetterName(fieldName);
        String typeSignature = returnType.signature();
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "()"+ typeSignature, null, null);
        int loadOp = returnType.loadOpCode();
        mv.visitVarInsn(loadOp, 0);
        mv.visitFieldInsn(GETFIELD, internalClassName, fieldName, typeSignature);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static void createThrow(ClassWriter cw, String internalClassName,
            String methodName, Class<?>[] inTypes, Class<?> returnType,
            Class<?> exceptionType)
    {
        String returnTypeSignature = resolveType(returnType).signature();
        String exceptionName = getInternalClassName(exceptionType.getName());

        String sig = "(" + getArgumentsType(inTypes) + ")" + returnTypeSignature;

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, sig, null,
                null);

        mv.visitTypeInsn(NEW, exceptionName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, exceptionName, "<init>", "()V");
        mv.visitInsn(ATHROW);
        mv.visitMaxs(2, 1 + inTypes.length);
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

    private static String getArgumentsType(Class<?>[] inTypes) {
        StringBuilder list = new StringBuilder();

        for (Class<?> clazz : inTypes) {
            list.append(resolveType(clazz).signature());
        }

        return list.toString();
    }

    private static TypeType resolveType(Class<?> rawType)
    {
        int arrayCount = 0;
        while (rawType.isArray()) {
            ++arrayCount;
            rawType = rawType.getComponentType();
        }
        TypeType elementType;
        if (!rawType.isPrimitive()) {
            return new RefTypeType(rawType, arrayCount, EnumTypeType.OBJECT);
        }
        if (rawType == Integer.TYPE) {
            rawType = Integer.class;
            elementType = EnumTypeType.OBJECT;
            //elementType = EnumTypeType.INT;
        }
        else if (rawType == Long.TYPE) elementType = EnumTypeType.INT;
        else if (rawType == Boolean.TYPE) elementType = EnumTypeType.INT;
        else if (rawType == Byte.TYPE) elementType = EnumTypeType.INT;
        else if (rawType == Short.TYPE)elementType = EnumTypeType.INT;
        else if (rawType == Character.TYPE) elementType = EnumTypeType.INT;
        else if (rawType == Float.TYPE) elementType = EnumTypeType.INT;
        else if (rawType == Double.TYPE) elementType = EnumTypeType.INT;
        else throw new IllegalArgumentException("Unrecognized primitive type "+rawType.getName());
        if (arrayCount > 0) {
            return new RefTypeType(rawType, arrayCount, elementType);
        }
        return elementType;
    }
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */
    
    private static class ThrowMethodType
    {
        //public final String name;
        public final Class<?>[] paramTypes;
        public final Class<?> returnType;
        public final Class<?> exceptionType;

        public ThrowMethodType(String name, Class<?>[] paramTypes,
                Class<?> returnType, Class<?> exceptionType) {
            //this.name = name;
            this.paramTypes = paramTypes;
            this.returnType = returnType;
            this.exceptionType = exceptionType;
        }
    }

    interface TypeType {
        public String signature();
        public int loadOpCode();
        public int storeOpCode();
    }

    static class RefTypeType
        implements TypeType
    {
        protected final String _signature;
        
        public RefTypeType(Class<?> elementClass, int arrayCount, TypeType elementType)
        {
            StringBuilder sb = new StringBuilder();
            while (--arrayCount >= 0) {
                sb.append('[');
            }
            sb.append(elementType.signature());
            if (!elementClass.isPrimitive()) {
                sb.append(getInternalClassName(elementClass.getName())).append(';');
            }
            _signature = sb.toString();
        }

        public String signature() { return _signature; }
        public int loadOpCode() { return ALOAD; }
        public int storeOpCode() { return ASTORE; }
    }
    
    /**
     * Enumeration used for simplifying handling of primitive vs object (reference)
     * types
     */
    public enum EnumTypeType
        implements TypeType
    {
        VOID('V'), // void
        BOOLEAN('Z', ILOAD, ISTORE), // boolean
        BYTE('B', ILOAD, ISTORE),
        SHORT('S', ILOAD, ISTORE),
        CHAR('C', ILOAD, ISTORE),
        INT('I', ILOAD, ISTORE),
        LONG('J', LLOAD, LSTORE),
        FLOAT('F', FLOAD, FSTORE),
        DOUBLE('D', DLOAD, DSTORE),
        OBJECT('L', ALOAD, ASTORE)
        ;

        private final String _typeDescription;
        private final int _loadOpcode;
        private final int _storeOpcode;
        
        private EnumTypeType(char tc)
        {
            this(tc, ALOAD, ASTORE);
        }

        private EnumTypeType(char tc, int loadOp, int storeOp)
        {
            _typeDescription = String.valueOf(tc);
            _loadOpcode = loadOp;
            _storeOpcode = storeOp;
        }

        public String signature()
        {
            return _typeDescription;
        }
        
        public int loadOpCode() { return _loadOpcode; }
        public int storeOpCode() { return _storeOpcode; }
    }
}
