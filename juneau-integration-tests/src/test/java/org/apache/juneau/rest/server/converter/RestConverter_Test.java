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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
	// B - Queryable converter (rebuilt on BeanQuery)
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

	@Test void b01_queryableMapRows_noParams() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		// Just verify we get a valid array with 3 elements; ordering of Map.of() keys not guaranteed.
		c.get("/").run().assertContent().isContains("Alice","Bob","Charlie");
	}

	@Test void b02_queryableMapRows_viewParam() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		c.get("/?view=name").run().assertContent().isContains("Alice","Bob","Charlie");
		c.get("/?view=name").run().assertContent().isNotContains("age");
	}

	@Test void b03_queryableMapRows_searchParam() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		c.get("/?search=name=$contains(li)").run().assertContent().isContains("Alice","Charlie");
		c.get("/?search=name=$contains(li)").run().assertContent().isNotContains("Bob");
	}

	@Test void b04_queryableMapRows_sortAndViewParams() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		c.get("/?sort=age:desc&view=name").run().assertContent("[{\"name\":\"Charlie\"},{\"name\":\"Alice\"},{\"name\":\"Bob\"}]");
	}

	@Test void b05_queryableMapRows_positionAndLimitParams() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		c.get("/?sort=age&position=1&limit=1&view=name").run().assertContent("[{\"name\":\"Alice\"}]");
	}

	@Test void b06_queryableNullResponse() throws Exception {
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

	@Test void b07_queryableEmptyList_unchanged() throws Exception {
		var c = MockRestClient.buildJson(B7.class);
		c.get("/?search=name=x").run().assertContent("[]");
	}

	@Rest(converters=Queryable.class)
	public static class B7 implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<Map<String,Object>> b() {
			return List.of();
		}
	}

	@Test void b08_queryableNonCollection_unchanged() throws Exception {
		var c = MockRestClient.buildJson(B8.class);
		c.get("/?search=foo=bar").run().assertContent("{\"name\":\"Alice\"}");
	}

	@Rest(converters=Queryable.class)
	public static class B8 implements BasicUniversalConfig {
		@RestOp(path="/")
		public Map<String,Object> b() {
			return Map.of("name", "Alice");
		}
	}

	@Test void b09_queryableScalarList_unchanged() throws Exception {
		var c = MockRestClient.buildJson(B9.class);
		// A list of plain strings has no columns to search/sort/view by -- Queryable leaves it completely
		// untouched (it doesn't even apply position/limit), matching pre-10.0 behavior for non-tabular results.
		c.get("/?limit=1").run().assertContent("[\"a\",\"b\",\"c\"]");
	}

	@Rest(converters=Queryable.class)
	public static class B9 implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<String> b() {
			return List.of("a", "b", "c");
		}
	}

	public static class Person {
		private final String name;
		private final int age;
		public Person(String name, int age) { this.name = name; this.age = age; }
		public String getName() { return name; }
		public int getAge() { return age; }
	}

	@Rest(converters=Queryable.class)
	public static class B10 implements BasicUniversalConfig {
		@RestOp(path="/")
		public Person[] b() {
			return new Person[]{new Person("Alice", 30), new Person("Bob", 25), new Person("Charlie", 35)};
		}
	}

	@Test void b10_queryableBeanArrayRows_searchViewSort() throws Exception {
		var c = MockRestClient.buildJson(B10.class);
		c.get("/?search=age=$gte(30)&view=name&sort=name").run().assertContent("[{\"name\":\"Alice\"},{\"name\":\"Charlie\"}]");
	}

	@Test void b11_queryableUnknownColumn_returns400WithCode() throws Exception {
		var c = MockRestClient.create(B10.class).json().ignoreErrors().build();
		// "toString"/"class" are real Object methods but not declared bean columns -- confirms the bean-derived
		// column allow-list, not bare-method-name matching.
		c.get("/?sort=toString").run().assertStatus(400).assertHeader("X-BeanQuery-Error").is("UNKNOWN_COLUMN");
		c.get("/?view=class").run().assertStatus(400).assertHeader("X-BeanQuery-Error").is("UNKNOWN_COLUMN");
	}

	@Test void b12_queryableRegexAllowedByDefault() throws Exception {
		// IRS parity: $regex works out of the box with no QueryableSettings bean registered.
		var c = MockRestClient.buildJson(B10.class);
		c.get("/?search=name=$regex(A.*)&view=name").run().assertContent("[{\"name\":\"Alice\"}]");
	}

	@Rest(converters=Queryable.class)
	public static class B13 implements BasicUniversalConfig {
		@RestOp(path="/")
		public Person[] b() {
			return new Person[]{new Person("Alice", 30), new Person("Bob", 25)};
		}
		@Bean public QueryableSettings queryableSettings() {
			return QueryableSettings.create().allowRegex(false).build();
		}
	}

	@Test void b13_queryableRegexDeniedWithExplicitOptOut() throws Exception {
		var c = MockRestClient.create(B13.class).json().ignoreErrors().build();
		c.get("/?search=name=$regex(A.*)").run().assertStatus(400).assertHeader("X-BeanQuery-Error").is("REGEX_DISABLED");
	}

	@Rest(converters=Queryable.class)
	public static class B14 implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<Person> b() {
			var l = new ArrayList<Person>();
			for (var i = 0; i < 1200; i++)
				l.add(new Person("p" + i, i));
			return l;
		}
	}

	@Test void b14_queryableDefaultLimit_appliedWhenNoLimitGiven() throws Exception {
		var c = MockRestClient.buildJson(B14.class);
		var rows = c.get("/?view=name").run().assertStatus(200).cacheContent().getContent().as(List.class);
		assertSize(100, rows);
	}

	@Test void b15_queryableMaxLimit_clampsOversizedRequest() throws Exception {
		var c = MockRestClient.buildJson(B14.class);
		var rows = c.get("/?limit=5000&view=name").run().assertStatus(200).cacheContent().getContent().as(List.class);
		assertSize(1000, rows);
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
	// See c04_introspectableAllowListedMethod_dispatches() below for the per-resource allow-list opt-in
	// that resolves this for callers who need reflective dispatch.
	@Test void c02_introspectableInvokeMethod_deniedByDefault() throws Exception {
		var c = MockRestClient.create(C.class).json().ignoreErrors().build();
		c.get("/?invokeMethod=getName").run().assertStatus(500).assertContent().isContains("has not been allow-listed");
	}

	@Test void c03_introspectableInvokeToString_deniedByDefault() throws Exception {
		var c = MockRestClient.create(C.class).json().ignoreErrors().build();
		c.get("/?invokeMethod=toString").run().assertStatus(500).assertContent().isContains("has not been allow-listed");
	}

	// Real per-resource allow-list configuration: a resource opts specific methods in by registering an
	// IntrospectableSettings bean in its bean store.  Default (no bean) remains deny-all, covered by c02/c03 above.

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
		c.get("/items?view=name").run().assertContent().isContains("Alice","Bob");
		c.get("/items?view=name").run().assertContent().isNotContains("age");
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

	//------------------------------------------------------------------------------------------------------------------
	// G - Queryable X-BeanQuery-Total / X-BeanQuery-Matched count headers
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_queryableCounts_headersSetWhenRequested() throws Exception {
		var c = MockRestClient.buildJson(B10.class);
		c.get("/?opts=counts=true").run().assertStatus(200)
			.assertHeader("X-BeanQuery-Total").is("3")
			.assertHeader("X-BeanQuery-Matched").is("3");
	}

	@Test void g02_queryableCounts_matchedReflectsSearchFilter() throws Exception {
		var c = MockRestClient.buildJson(B10.class);
		c.get("/?search=age=$gte(30)&opts=counts=true").run().assertStatus(200)
			.assertHeader("X-BeanQuery-Total").is("3")
			.assertHeader("X-BeanQuery-Matched").is("2");
	}

	@Test void g03_queryableCounts_headersOmittedWhenNotRequested() throws Exception {
		var c = MockRestClient.buildJson(B10.class);
		c.get("/").run().assertStatus(200)
			.assertHeader("X-BeanQuery-Total").isNull()
			.assertHeader("X-BeanQuery-Matched").isNull();
	}

	@Test void g04_queryableCounts_matchedOnly() throws Exception {
		var c = MockRestClient.buildJson(B10.class);
		c.get("/?opts=counts=matched&limit=1").run().assertStatus(200)
			.assertHeader("X-BeanQuery-Total").isNull()
			.assertHeader("X-BeanQuery-Matched").is("3");
	}

	@Test void g05_queryableCounts_mapRows() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		c.get("/?search=age=$lt(35)&opts=counts=true").run().assertStatus(200)
			.assertHeader("X-BeanQuery-Total").is("3")
			.assertHeader("X-BeanQuery-Matched").is("2");
	}
	//------------------------------------------------------------------------------------------------------------------
	// H - Queryable per-element-class context cache
	//------------------------------------------------------------------------------------------------------------------

	public static class CapturingQueryable extends Queryable {
		// One converter instance is created per @RestOp, so capture all of them.
		static final List<CapturingQueryable> instances = new java.util.concurrent.CopyOnWriteArrayList<>();
		public CapturingQueryable() { instances.add(this); }
		static Object cached(Class<?> c) {
			return instances.stream().map(q -> q.cachedContext(c)).filter(Objects::nonNull).findFirst().orElse(null);
		}
	}

	public static class Pet {
		public String getKind() { return "cat"; }
	}

	@Rest(converters=CapturingQueryable.class)
	public static class H implements BasicUniversalConfig {
		@RestOp(path="/people")
		public List<Person> people() {
			return List.of(new Person("Alice", 30), new Person("Bob", 25));
		}
		@RestOp(path="/pets")
		public List<Pet> pets() {
			return List.of(new Pet());
		}
	}

	@Test void h01_contextCache_reusedPerElementClass() throws Exception {
		var c = MockRestClient.buildJson(H.class);
		CapturingQueryable.instances.clear();
		c.get("/people").run().assertStatus(200);
		var first = CapturingQueryable.cached(Person.class);
		assertNotNull(first);
		c.get("/people?search=age=$gte(30)").run().assertStatus(200);
		assertSame(first, CapturingQueryable.cached(Person.class));
		assertNull(CapturingQueryable.cached(Pet.class));
		c.get("/pets").run().assertStatus(200);
		assertNotNull(CapturingQueryable.cached(Pet.class));
		assertNotSame(first, CapturingQueryable.cached(Pet.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// I - Queryable rows that aren't getter beans
	//------------------------------------------------------------------------------------------------------------------

	public record Car(String make, int year) {}

	public static class Boat {
		public String name;
		public int length;
		public Boat(String name, int length) { this.name = name; this.length = length; }
	}

	public enum Size { SMALL, LARGE }

	@Rest(converters=Queryable.class)
	public static class I implements BasicUniversalConfig {
		@RestOp(path="/cars")
		public List<Car> cars() {
			return List.of(new Car("Audi", 2020), new Car("Ford", 2010), new Car("Honda", 2015));
		}
		@RestOp(path="/boats")
		public List<Boat> boats() {
			return List.of(new Boat("Argo", 30), new Boat("Mist", 20), new Boat("Wave", 40));
		}
		@RestOp(path="/sizes")
		public List<Map<String,Object>> sizes() {
			return List.of(Map.of("size", Size.SMALL), Map.of("size", Size.LARGE));
		}
	}

	@Test void i01_recordRows_searchViewSort() throws Exception {
		var c = MockRestClient.buildJson(I.class);
		c.get("/cars?search=year=$gte(2015)&view=make&sort=year:desc").run().assertContent("[{\"make\":\"Audi\"},{\"make\":\"Honda\"}]");
	}

	@Test void i02_publicFieldRows_searchViewSort() throws Exception {
		var c = MockRestClient.buildJson(I.class);
		c.get("/boats?search=length=$gte(30)&view=name&sort=length:desc").run().assertContent("[{\"name\":\"Wave\"},{\"name\":\"Argo\"}]");
	}

	@Test void i03_enumMapValue_isTypedEnum() throws Exception {
		var c = MockRestClient.buildJson(I.class);
		// ENUM typing makes an unknown constant a 400 (a TEXT column would just match nothing).
		c.get("/sizes?search=size=SMALL").run().assertContent("[{\"size\":\"SMALL\"}]");
	}
}
