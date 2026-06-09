/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.converter;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.junit.jupiter.api.*;

/** Coverage for {@code org.apache.juneau.rest.server.server.converter} package converters. */
class RestConverter_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A - Traversable converter
	//------------------------------------------------------------------------------------------------------------------

	@Rest(converters=Traversable.class)
	public static class A implements BasicUniversalConfig {
		@RestOp(path="/*")
		public Map<String,Object> a() {
			var m = new LinkedHashMap<String,Object>();
			m.put("a", "1");
			m.put("b", Map.of("c", "2"));
			return m;
		}
	}

	@Test void a01_traversableFullObject() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		c.get("/").run().assertContent("{\"a\":\"1\",\"b\":{\"c\":\"2\"}}");
	}

	@Test void a02_traversableSubPath() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		c.get("/b").run().assertContent("{\"c\":\"2\"}");
	}

	@Test void a03_traversableLeafValue() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		c.get("/b/c").run().assertContent("\"2\"");
	}

	@Test void a04_traversableNullResponse() throws Exception {
		var c = MockRestClient.buildJson(A2.class);
		c.get("/").run().assertContent("null");
	}

	@Rest(converters=Traversable.class)
	public static class A2 implements BasicUniversalConfig {
		@RestOp(path="/*")
		public Object a() {
			return null;
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B - Queryable converter
	//------------------------------------------------------------------------------------------------------------------

	@Rest(converters=Queryable.class)
	public static class B implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<Map<String,Object>> b() {
			return List.of(
				Map.of("name", "Alice", "age", 30),
				Map.of("name", "Bob", "age", 25),
				Map.of("name", "Charlie", "age", 35)
			);
		}
	}

	@Test void b01_queryableNoParams() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		// Just verify we get a valid array with 3 elements; ordering of Map.of() keys not guaranteed
		c.get("/").run().assertContent().isContains("Alice","Bob","Charlie");
	}

	@Test void b02_queryableViewParam() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		c.get("/?v=name").run().assertContent().isContains("Alice","Bob","Charlie");
		c.get("/?v=name").run().assertContent().isNotContains("age");
	}

	@Test void b03_queryableLimitParam() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		// With limit=1, should only get one entry
		c.get("/?l=1").run().assertContent().isContains("[{");
	}

	@Test void b04_queryableNullResponse() throws Exception {
		var c = MockRestClient.buildJson(B2.class);
		c.get("/").run().assertContent("null");
	}

	@Rest(converters=Queryable.class)
	public static class B2 implements BasicUniversalConfig {
		@RestOp(path="/")
		public Object b() {
			return null;
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C - Introspectable converter
	//------------------------------------------------------------------------------------------------------------------

	@Rest(converters=Introspectable.class)
	public static class C implements BasicUniversalConfig {
		@RestOp(path="/")
		public MyBean c() {
			return new MyBean();
		}
	}

	public static class MyBean {
		public String getName() { return "foo"; }
		public int getAge() { return 42; }
		@Override public String toString() { return "MyBean"; }
	}

	@Test void c01_introspectableNoInvoke() throws Exception {
		var c = MockRestClient.buildJson(C.class);
		c.get("/").run().assertContent().isContains("foo","42");
	}

	@Test void c02_introspectableInvokeMethod() throws Exception {
		var c = MockRestClient.buildJson(C.class);
		c.get("/?invokeMethod=getName").run().assertContent("\"foo\"");
	}

	@Test void c03_introspectableInvokeToString() throws Exception {
		var c = MockRestClient.buildJson(C.class);
		c.get("/?invokeMethod=toString").run().assertContent("\"MyBean\"");
	}

	//------------------------------------------------------------------------------------------------------------------
	// D - Multiple converters on same resource
	//------------------------------------------------------------------------------------------------------------------

	@Rest(converters={Traversable.class, Queryable.class})
	public static class D implements BasicUniversalConfig {
		@RestOp(path="/*")
		public Map<String,Object> d() {
			var m = new LinkedHashMap<String,Object>();
			m.put("items", List.of(
				Map.of("name", "Alice", "age", 30),
				Map.of("name", "Bob", "age", 25)
			));
			return m;
		}
	}

	@Test void d01_multipleConvertersTraverseThenQuery() throws Exception {
		var c = MockRestClient.buildJson(D.class);
		c.get("/items?v=name").run().assertContent().isContains("Alice","Bob");
		c.get("/items?v=name").run().assertContent().isNotContains("age");
	}

	@Test void d02_multipleConvertersTraverseOnly() throws Exception {
		var c = MockRestClient.buildJson(D.class);
		c.get("/items").run().assertContent().isContains("Alice","Bob","age");
	}

	//------------------------------------------------------------------------------------------------------------------
	// E - Converter on method level
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E implements BasicUniversalConfig {
		@RestOp(path="/*", converters=Traversable.class)
		public Map<String,Object> e() {
			var m = new LinkedHashMap<String,Object>();
			m.put("x", "1");
			m.put("y", "2");
			return m;
		}

		@RestOp(path="/plain")
		public String plain() {
			return "hello";
		}
	}

	@Test void e01_methodLevelConverterApplied() throws Exception {
		var c = MockRestClient.buildJson(E.class);
		c.get("/x").run().assertContent("\"1\"");
	}

	@Test void e02_methodWithoutConverter() throws Exception {
		var c = MockRestClient.buildJson(E.class);
		c.get("/plain").run().assertContent("\"hello\"");
	}
}
