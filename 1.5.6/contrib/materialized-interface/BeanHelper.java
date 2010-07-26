package org.codehaus.jackson.map.deser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BeanHelper implements Opcodes {
	public static class BeanBuilder {
		protected Map<String, Class> properties = new LinkedHashMap<String, Class>();
		protected Map<String, ThrowMethodType> throwMethods = new LinkedHashMap<String, ThrowMethodType>();
		protected List<Class> implementing = new ArrayList<Class>();
		protected String className;
		protected String internalClass;

		public BeanBuilder(String className) {
			this.className = className;
			this.internalClass = getInternalClassName(className);
		}

		public BeanBuilder implement(Class parent) {
			this.implementing.add(parent);

			for (Method m : parent.getMethods()) {
				if (m.getName().startsWith("get")
						|| m.getName().startsWith("set")) {
					String name = getFieldName(m.getName());
					Class propType = m.getName().startsWith("get") ? m
							.getReturnType() : m.getParameterTypes()[0];

					if (this.properties.containsKey(name)
							&& !this.properties.get(name).equals(propType)) {
						throw new IllegalArgumentException("Duplicate property");
					}

					addProperty(name, propType);
				} else {
					addThrow(m.getName(), m.getParameterTypes(), m
							.getReturnType(),
							UnsupportedOperationException.class);
				}
			}

			return this;
		}

		public BeanBuilder addProperty(String name, Class type) {
			properties.put(name, type);

			return this;
		}

		public BeanBuilder addThrow(String name, Class[] paramTypes,
				Class returnType, Class exceptionType) {
			this.throwMethods.put(name, new ThrowMethodType(name, paramTypes,
					returnType, exceptionType));

			return this;
		}

		public Class load() {
			ClassWriter cw = new ClassWriter(0);

			String[] parents = new String[implementing.size()];
			for (int i = 0; i < implementing.size(); i++) {
				parents[i] = getInternalClassName(implementing.get(i).getName());
			}
			cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER, internalClass, null,
					"java/lang/Object", parents);
			cw.visitSource(className + ".java", null);
			BeanHelper.generateDefaultConstructor(cw);

			for (Map.Entry<String, Class> propEntry : this.properties
					.entrySet()) {
				String propName = propEntry.getKey();
				Class propClass = propEntry.getValue();

				BeanHelper.createField(cw, propName, propClass);
				BeanHelper.createGetter(cw, internalClass, propName, propClass);
				BeanHelper.createSetter(cw, internalClass, propName, propClass);
			}

			for (Map.Entry<String, ThrowMethodType> throwEntry : this.throwMethods
					.entrySet()) {
				ThrowMethodType thr = throwEntry.getValue();

				BeanHelper.createThrow(cw, this.internalClass, throwEntry
						.getKey(), thr.paramTypes, thr.returnType,
						thr.exceptionType);
			}

			cw.visitEnd();

			return loadClass(className, cw.toByteArray());
		}
	}

	private static void generateDefaultConstructor(ClassWriter cw) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null,
				null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private static void createField(ClassWriter cw, String fieldName,
			Class fieldType) {
		String javaType = getLValue(fieldType);
		FieldVisitor fv = cw.visitField(0, fieldName, javaType, null, null);
		fv.visitEnd();
	}

	private static void createSetter(ClassWriter cw, String internalClassName,
			String fieldName, Class fieldType) {
		String methodName = getSetterName(fieldName);
		String returnType = getLValue(fieldType);
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "("
				+ returnType + ")V", null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitFieldInsn(PUTFIELD, internalClassName, fieldName, returnType);
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
	}

	private static void createGetter(ClassWriter cw, String internalClassName,
			String fieldName, Class fieldType) {
		String methodName = getGetterName(fieldName);
		String returnType = getLValue(fieldType);
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "()"
				+ returnType, null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, internalClassName, fieldName, returnType);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private static void createThrow(ClassWriter cw, String internalClassName,
			String methodName, Class[] inTypes, Class returnType,
			Class exceptionType) {
		String rTypeName = getLValue(returnType);
		String exceptionName = getInternalClassName(exceptionType.getName());

		String sig = "(" + getArgumentsType(inTypes) + ")" + rTypeName;

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, sig, null,
				null);

		mv.visitTypeInsn(NEW, exceptionName);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, exceptionName, "<init>", "()V");
		mv.visitInsn(ATHROW);
		mv.visitMaxs(2, 1 + inTypes.length);
		mv.visitEnd();
	}

	private static Class loadClass(String className, byte[] b) {
		// override classDefine (as it is protected) and define the class.
		Class clazz = null;
		try {
			ClassLoader loader = ClassLoader.getSystemClassLoader();
			Class cls = Class.forName("java.lang.ClassLoader");
			java.lang.reflect.Method method = cls.getDeclaredMethod(
					"defineClass", new Class[] { String.class, byte[].class,
							int.class, int.class });

			// protected method invocaton
			method.setAccessible(true);
			try {
				Object[] args = new Object[] { className, b, new Integer(0),
						new Integer(b.length) };
				clazz = (Class) method.invoke(loader, args);
			} finally {
				method.setAccessible(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return clazz;
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

	private static String getLValue(Class fieldType) {
		if (fieldType == null || fieldType.equals(void.class)) {
			return "V";
		}

		String plainR = fieldType.getName();
		String rType = getInternalClassName(plainR);
		String javaType = "L" + rType + ";";
		return javaType;
	}

	private static String getGetterName(String fieldName) {
		return "get" + fieldName.substring(0, 1).toUpperCase()
				+ fieldName.substring(1);
	}

	private static String getSetterName(String fieldName) {
		return "set" + fieldName.substring(0, 1).toUpperCase()
				+ fieldName.substring(1);
	}

	private static String getArgumentsType(Class[] inTypes) {
		StringBuilder list = new StringBuilder();

		for (Class clazz : inTypes) {
			list.append(getLValue(clazz));
		}

		return list.toString();
	}

	private static class ThrowMethodType {
		public final String name;
		public final Class[] paramTypes;
		public final Class returnType;
		public final Class exceptionType;

		public ThrowMethodType(String name, Class[] paramTypes,
				Class returnType, Class exceptionType) {
			this.name = name;
			this.paramTypes = paramTypes;
			this.returnType = returnType;
			this.exceptionType = exceptionType;
		}
	}
}
