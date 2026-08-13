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
import org.apache.juneau.commons.inject.*;
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

	// As of 10.0, ObjectIntrospector is secure-by-default (denies reflective dispatch unless the caller has
	// explicitly allow-listed the target method(s)). Introspectable has no allow-list configuration mechanism
	// of its own, so invokeMethod requests are now refused with a 500 rather than dispatched. This closes the
	// REST-exposed "reflective-invoke-over-the-wire" hole that this converter previously opened by default.
	// See TODO-351 B-marshall-6 for the tracked follow-up decision on whether/how to add allow-list
	// configuration to this converter.
	@Test void c02_introspectableInvokeMethod_deniedByDefault() throws Exception {
		var c = MockRestClient.create(C.class).json().ignoreErrors().build();
		c.get("/?invokeMethod=getName").run().assertStatus(500).assertContent().isContains("has not been allow-listed");
	}

	@Test void c03_introspectableInvokeToString_deniedByDefault() throws Exception {
		var c = MockRestClient.create(C.class).json().ignoreErrors().build();
		c.get("/?invokeMethod=toString").run().assertStatus(500).assertContent().isContains("has not been allow-listed");
	}

	// Real per-resource allow-list configuration (TODO-351 B-marshall-6 follow-up): a resource opts specific
	// methods in by registering an IntrospectableSettings bean in its bean store.  Default (no bean) remains
	// deny-all, covered by c02/c03 above.

	@Rest(converters=Introspectable.class)
	public static class C2 implements BasicUniversalConfig {
		@RestOp(path="/")
		public MyBean c2() {
			return new MyBean();
		}
		@Bean public IntrospectableSettings introspectableSettings() {
			return IntrospectableSettings.create().allow(MyBean.class, "getName", "getAge").build();
		}
	}

	@Test void c04_introspectableAllowListedMethod_dispatches() throws Exception {
		var c = MockRestClient.buildJson(C2.class);
		c.get("/?invokeMethod=getName").run().assertStatus(200).assertContent("\"foo\"");
		c.get("/?invokeMethod=getAge").run().assertStatus(200).assertContent("42");
	}

	@Test void c05_introspectableNonAllowListedMethod_refused() throws Exception {
		// toString() was not allow-listed on C2 (only getName/getAge were), so it's still refused.
		var c = MockRestClient.create(C2.class).json().ignoreErrors().build();
		c.get("/?invokeMethod=toString").run().assertStatus(500).assertContent().isContains("has not been allow-listed");
	}

	@Rest(converters=Introspectable.class)
	public static class C3 implements BasicUniversalConfig {
		@RestOp(path="/")
		public MyBean c3() {
			return new MyBean();
		}
		@Bean public IntrospectableSettings introspectableSettings() {
			return IntrospectableSettings.create().allowAll().build();
		}
	}

	@Test void c06_introspectableAllowAll_dispatchesAnyPublicMethod() throws Exception {
		var c = MockRestClient.buildJson(C3.class);
		c.get("/?invokeMethod=getName").run().assertStatus(200).assertContent("\"foo\"");
		c.get("/?invokeMethod=toString").run().assertStatus(200).assertContent("\"MyBean\"");
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
