/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.jackson.map.deser;

import java.lang.reflect.Method;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializerProvider;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * A very basic Jackson deserializer factory that extends the BeanSerializer
 * factory to allow deserialization using interfaces.
 */
public class MaterializingDeserializerFactory extends BeanDeserializerFactory {
	public MaterializingDeserializerFactory() {
		super();
	}

	@Override
	public JsonDeserializer<Object> createBeanDeserializer(
			DeserializationConfig config, JavaType type, DeserializerProvider p)
			throws JsonMappingException {
		if (type.isInterface()) {
			String className = "$org.codehaus.jackson.generated$"
					+ type.getRawClass().getName();
			BeanHelper.BeanBuilder builder = new BeanHelper.BeanBuilder(
					className);
			builder.implement(type.getRawClass());

			return new BeanDeserializerProxyImpl(TypeFactory.fromClass(builder
					.load()));
		}

		return super.createBeanDeserializer(config, type, p);
	}

	protected static class BeanDeserializerProxyImpl extends BeanDeserializer {
		public BeanDeserializerProxyImpl(JavaType type) {
			super(type);

			try {
				this.setDefaultConstructor(type.getRawClass().getConstructor());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			try {
				populateSetters(type);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		private void populateSetters(JavaType type)
				throws NoSuchMethodException {
			Class clazz = type.getRawClass();
			for (Method m : clazz.getMethods()) {
				String methodName = m.getName();
				if (methodName.startsWith("set")
						&& m.getParameterTypes() != null
						&& m.getParameterTypes().length == 1) {
					Class fieldClass = m.getParameterTypes()[0];
					JavaType fieldType = TypeFactory.fromClass(fieldClass);

					Method setterMethod = getSetterMethod(clazz, methodName,
							fieldClass);

					this.addProperty(new SettableBeanProperty.MethodProperty(
									getFieldName(methodName), fieldType,
									setterMethod));
				}
			}
		}

		private Method getSetterMethod(Class clazz, String methodName,
				Class returnType) throws NoSuchMethodException {
			return clazz.getMethod("set" + methodName.substring(3), returnType);
		}
	}

	private static String getFieldName(String getterMethodName) {
		char[] name = getterMethodName.substring(3).toCharArray();
		name[0] = Character.toLowerCase(name[0]);
		final String propName = new String(name);

		System.out.println(propName);
		return propName;
	}
}
