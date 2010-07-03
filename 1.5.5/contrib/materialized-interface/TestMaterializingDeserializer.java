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
package org.example;

import java.io.StringWriter;

import junit.framework.TestCase;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.MaterializingDeserializerFactory;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;

import com.g414.jackson.proxy.ProxySerializerFactory;

/**
 * Exercise the deserializer...
 */
public class TestMaterializingDeserializer extends TestCase {
	public void testExample() throws Exception {
		ExampleImpl impl = new ExampleImpl();

		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializerFactory(new ProxySerializerFactory());

		MaterializingDeserializerFactory df = new MaterializingDeserializerFactory();
		StdDeserializerProvider dp = new StdDeserializerProvider(df);
		mapper.setDeserializerProvider(dp);

		// Testing Example1
		StringWriter w1 = new StringWriter();
		mapper.writeValue(w1, impl.asExample1());
		String w1Value = w1.toString();
		Example1 w1in = mapper.readValue(w1Value, Example1.class);

		assertEquals("4660", w1in.getA().toString());
		assertEquals("Foo", w1in.getB().toString());

		// Testing Example2 ...
		StringWriter w2 = new StringWriter();
		mapper.writeValue(w2, impl.asExample2());
		String w2Value = w2.toString();

		Example2 w2in = mapper.readValue(w2Value, Example2.class);
		assertEquals("305419896", w2in.getC().toString());
	}
}
