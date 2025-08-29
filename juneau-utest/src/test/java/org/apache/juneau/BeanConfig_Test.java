// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.Visibility.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.junit.jupiter.api.*;

@SuppressWarnings("rawtypes")
class BeanConfig_Test extends SimpleTestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@SuppressWarnings("unchecked")
	@Test void a01_basic() {

		var bc = BeanContext.DEFAULT;

		var p1 = new Person();
		p1.setName("John Doe");
		p1.setAge(25);

		var a = new Address("101 Main St.", "Las Vegas", "NV", "89101");
		var p2 = new AddressablePerson();
		p2.setName("Jane Doe");
		p2.setAge(21);
		p2.setAddress(a);

		// setup the reference results
		var m1 = new LinkedHashMap<String,Object>();
		m1.put("name", p1.getName());
		m1.put("age", Integer.valueOf(p1.getAge()));

		var m2 = new LinkedHashMap<String,Object>();
		m2.put("street", a.getStreet());
		m2.put("city", a.getCity());
		m2.put("state", a.getState());
		m2.put("zip", a.getZip());

		var m3 = new LinkedHashMap<String,Object>();
		m3.put("name", p2.getName());
		m3.put("age", Integer.valueOf(p2.getAge()));
		m3.put("address", p2.getAddress());

		var pm1 = bc.toBeanMap(p1);

		assertEquals(pm1.size(), m1.size(), fs("Bean Map size failed for: {0} / {1} / {2}", p1, pm1.size(), m1.size()));
		assertEquals(pm1.keySet(), m1.keySet(), fs("Bean Map key set equality failed for: {0} / {1} / {2}", p1, pm1.keySet() , m1.keySet()));
		assertEquals(m1.keySet(), pm1.keySet(), fs("Bean Map key set reverse equality failed for: {0} / {1} / {2}", p1, pm1.keySet(), m1.keySet()));
		assertEquals(pm1, m1, fs("Bean Map equality failed for: {0} / {1} / {2}", p1, pm1, m1));  // NOSONAR
		assertThrows(BeanRuntimeException.class, ()->bc.newBeanMap(Address.class));  // Address returned as a new bean type, but shouldn't be since it doesn't have a default constructor.
		assertNull(bc.newBeanMap(java.lang.Integer.class), "java.lang.Integer incorrectly designated as bean type.");
		assertNull(bc.newBeanMap(java.lang.Class.class), "java.lang.Class incorrectly designated as bean type.");

		var bm1 = bc.toBeanMap(new Address("street", "city", "state", "zip"));

		assertEquals(bm1.size(), m2.size(), fs("Bean Adapter map's key set has wrong size: {0} / {1} / {2}", a, bm1.size(), m2.size()));

		var iter = bm1.keySet().iterator();
		var temp = new HashSet<>();
		var count = 0;
		while (iter.hasNext()) {
			temp.add(iter.next());
			count++;
		}

		assertEquals(count, m2.size(), fs("Iteration count over bean adpater key set failed: {0} / {1} / {2}", a, count, m2.size()));
		assertEquals(m2.keySet(), temp, fs("Iteration over bean adpater key set failed: {0} / {1} / {2}", a, bm1.keySet(), m2.keySet()));
		assertNotNull(bc.toBeanMap(p2), fs("Failed to identify class as bean type: {0}", p2.getClass()));

		var m5 = bc.toBeanMap(p2);
		@SuppressWarnings("cast")
		var es1 = (Set)m5.entrySet();

		assertEquals(es1, m3.entrySet(), fs("Entry set equality failed: {0} / {1} / {2}", p2, es1, m3.entrySet()));
		assertEquals(m3.entrySet(), es1, fs("Entry set reverse equality failed: {0} / {1} / {2}", p2, es1, m3.entrySet()));

		iter = es1.iterator();
		temp = new HashSet<>();
		count = 0;
		while (iter.hasNext()) {
			temp.add(iter.next());
			count++;
		}

		assertEquals(count, m3.size(), fs("Iteration count over bean adpater entry set failed: {0} / {1} / {2}", a, count, m3.size()));
		assertEquals(m3.entrySet(), temp, fs("Iteration over bean adpater entry set failed: {0} / {1} / {2}", a, es1, m3.entrySet()));
	}

	public static class Person {

		public Person() {
			name = null;
			age = -1;
		}

		private String name;
		public String getName() { return name; }
		public void setName(String v) { name = v; }

		private int age;
		public int getAge() { return age; }
		public void setAge(int v) { age = v; }

		@Override /* Object */
		public String toString() {
			return f("Person(name: {0}, age: {1})", name, age);
		}
	}

	public static class Address {

		public Address(String street, String city, String state, String zip) {
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
		}

		protected String street;
		public String getStreet() { return street; }

		protected String city;
		public String getCity() { return city; }

		protected String state;
		public String getState() { return state; }

		protected String zip;
		public String getZip() { return zip; }

		@Override /* Object */
		public boolean equals(Object o) {
			return eq(this, (Address)o, (x,y) -> eq(x.getStreet(), y.getStreet()) && eq(x.getCity(), y.getCity()) && eq(x.getState(), y.getState()) && eq(x.getZip(), y.getZip()));
		}

		@Override /* Object */
		public int hashCode() {
			return hash(street, city, state, zip);
		}

		@Override /* Object */
		public String toString() {
			return f("Address(street: {0}, city: {1}, state: {2}, zip: {3})", street, city, state, zip);
		}
	}

	public static class AddressablePerson extends Person {

		public AddressablePerson() {
			this.address = null;
		}

		private Address address;
		public Address getAddress() { return address; }
		public void setAddress(Address v) { address = v; }

		@Override /* Object */
		public String toString() {
			return super.toString() + "@" + this.address;
		}
	}

	//====================================================================================================
	// Exhaustive test of BeanContext.convertToType()
	//====================================================================================================
	@Test void a01_beanContextConvertToType() throws Exception {
		var bc = BeanContext.DEFAULT;
		var o = (Object)null;

		// Primitive nulls.
		assertEquals(Integer.valueOf(0), bc.convertToType(o, Integer.TYPE));
		assertEquals(Short.valueOf((short) 0), bc.convertToType(o, Short.TYPE));
		assertEquals(Long.valueOf(0), bc.convertToType(o, Long.TYPE));
		assertEquals(Float.valueOf(0), bc.convertToType(o, Float.TYPE));
		assertEquals(Double.valueOf(0), bc.convertToType(o, Double.TYPE));
		assertEquals(Byte.valueOf((byte) 0), bc.convertToType(o, Byte.TYPE));
		assertEquals(Character.valueOf((char) 0), bc.convertToType(o, Character.TYPE));
		assertEquals(Boolean.FALSE, bc.convertToType(o, Boolean.TYPE));

		o = "1";

		assertEquals(Integer.valueOf(1), bc.convertToType(o, Integer.class));
		assertEquals(Short.valueOf((short) 1), bc.convertToType(o, Short.class));
		assertEquals(Long.valueOf(1), bc.convertToType(o, Long.class));
		assertEquals(Float.valueOf(1), bc.convertToType(o, Float.class));
		assertEquals(Double.valueOf(1), bc.convertToType(o, Double.class));
		assertEquals(Byte.valueOf((byte) 1), bc.convertToType(o, Byte.class));
		assertEquals(Character.valueOf('1'), bc.convertToType(o, Character.class));
		assertEquals(Boolean.FALSE, bc.convertToType(o, Boolean.class));

		assertEquals(Integer.valueOf(1), bc.convertToType(o, Integer.TYPE));
		assertEquals(Short.valueOf((short) 1), bc.convertToType(o, Short.TYPE));
		assertEquals(Long.valueOf(1), bc.convertToType(o, Long.TYPE));
		assertEquals(Float.valueOf(1), bc.convertToType(o, Float.TYPE));
		assertEquals(Double.valueOf(1), bc.convertToType(o, Double.TYPE));
		assertEquals(Byte.valueOf((byte) 1), bc.convertToType(o, Byte.TYPE));
		assertEquals(Character.valueOf('1'), bc.convertToType(o, Character.TYPE));
		assertEquals(Boolean.FALSE, bc.convertToType(o, Boolean.TYPE));

		o = Integer.valueOf(1);

		assertEquals(Integer.valueOf(1), bc.convertToType(o, Integer.TYPE));
		assertEquals(Short.valueOf((short) 1), bc.convertToType(o, Short.TYPE));
		assertEquals(Long.valueOf(1), bc.convertToType(o, Long.TYPE));
		assertEquals(Float.valueOf(1), bc.convertToType(o, Float.TYPE));
		assertEquals(Double.valueOf(1), bc.convertToType(o, Double.TYPE));
		assertEquals(Byte.valueOf((byte) 1), bc.convertToType(o, Byte.TYPE));
		assertEquals(Character.valueOf('1'), bc.convertToType(o, Character.TYPE));
		assertEquals(Boolean.TRUE, bc.convertToType(o, Boolean.TYPE));

		o = Integer.valueOf(0);
		assertEquals(Boolean.FALSE, bc.convertToType(o, Boolean.TYPE));

		// Bean
		o = "{name:'x',age:123}";
		assertBean(bc.convertToType(o, Person.class), "name,age", "x,123");

		// Read-only bean
		o = "{name:'x',age:123}";
		assertBean(bc.convertToType(o, ReadOnlyPerson.class), "name,age", "x,123");

		// Class with forString(String) method.
		o = UUID.randomUUID();
		assertEquals(o, bc.convertToType(o.toString(), UUID.class));

		// Class with Constructor(String).
		o = "xxx";
		var file = bc.convertToType(o, File.class);
		assertEquals("xxx", file.getName());

		// List of ints to array
		o = JsonList.of(1, 2, 3);
		assertEquals(1, bc.convertToType(o, int[].class)[0]);

		// List of beans to array
		o = JsonList.of(new ReadOnlyPerson("x", 123));
		assertEquals("x", bc.convertToType(o, ReadOnlyPerson[].class)[0].getName());

		// Multi-dimensional array of beans.
		o = JsonList.ofCollections(JsonList.of(new ReadOnlyPerson("x", 123)));
		assertEquals("x", bc.convertToType(o, ReadOnlyPerson[][].class)[0][0].getName());

		// Array of strings to array of ints
		o = a("1", "2", "3");
		assertEquals(Integer.valueOf(1), bc.convertToType(o, Integer[].class)[0]);
		assertEquals(1, bc.convertToType(o, int[].class)[0]);

		// Array to list
		o = a(1, 2, 3);
		assertEquals(Integer.valueOf(1), bc.convertToType(o, LinkedList.class).get(0));

		// HashMap to TreeMap
		o = map(1, "foo");
		assertEquals("foo", bc.convertToType(o, TreeMap.class).firstEntry().getValue());

		// String to TreeMap
		o = "{1:'foo'}";
		assertEquals("foo", bc.convertToType(o, TreeMap.class).firstEntry().getValue());

		// String to generic Map
		assertEquals("foo", bc.convertToType(o, Map.class).values().iterator().next());

		// Array to String
		o = a("a", 1, false);
		assertEquals("['a',1,false]", bc.convertToType(o, String.class));
		o = new Object[]{a("a", 1, false)};
		assertEquals("[['a',1,false]]", bc.convertToType(o, String.class));
	}

	//====================================================================================================
	// Test properties set through a constructor.
	//====================================================================================================
	@Test void a02_readOnlyProperties() throws Exception {
		var bc = BeanContext.DEFAULT;
		var o = new ReadOnlyPerson("x", 123);

		// Bean to String
		assertEquals("{name:'x',age:123}", bc.convertToType(o, String.class));

		// List of Maps to array of beans.
		var o2 = JsonList.of(JsonMap.ofJson("{name:'x',age:1}"), JsonMap.ofJson("{name:'y',age:2}"));
		assertEquals(1, bc.convertToType(o2, ReadOnlyPerson[].class)[0].getAge());
	}

	@Bean(p="name,age")
	public static class ReadOnlyPerson {
		private final int age;

		@Beanc(properties="name,age")
		public ReadOnlyPerson(String name, int age) {
			this.name = name;
			this.age = age;
		}

		private final String name;
		public String getName() { return name; }
		public int getAge() { return age; }

		@Override /* Object */
		public String toString() {
			return f("toString():name={0},age={1}", name, age);
		}
	}

	@Test void a03_readOnlyProperties_usingConfig() throws Exception {
		var bc = BeanContext.DEFAULT.copy().applyAnnotations(ReadOnlyPerson2Config.class).build();
		var o = new ReadOnlyPerson2("x", 123);

		// Bean to String
		assertEquals("{name:'x',age:123}", bc.convertToType(o, String.class));

		// List of Maps to array of beans.
		var o2 = JsonList.of(JsonMap.ofJson("{name:'x',age:1}"), JsonMap.ofJson("{name:'y',age:2}"));
		assertEquals(1, bc.convertToType(o2, ReadOnlyPerson2[].class)[0].getAge());
	}

	public static class ReadOnlyPerson2 {
		private final int age;

		public ReadOnlyPerson2(String name, int age) {
			this.name = name;
			this.age = age;
		}

		private final String name;
		public String getName() { return name; }
		public int getAge() { return age; }

		@Override /* Object */
		public String toString() {
			return f("toString():name={0},age={1}", name, age);
		}
	}

	@Bean(on="Dummy1",p="dummy")
	@Bean(on="ReadOnlyPerson2",p="name,age")
	@Bean(on="Dummy2",p="dummy")
	@Beanc(on="Dummy1",properties="dummy")
	@Beanc(on="ReadOnlyPerson2(String,int)",properties="name,age")
	@Beanc(on="Dummy2",properties="dummy")
	private static class ReadOnlyPerson2Config {}

	//====================================================================================================
	// testEnums
	//====================================================================================================
	@Test void a04_enums() throws Exception {
		var bc = BeanContext.DEFAULT;
		var o = "ENUM2";

		// Enum
		assertEquals(TestEnum.ENUM2, bc.convertToType(o, TestEnum.class));
		assertEquals("ENUM2", bc.convertToType(TestEnum.ENUM2, String.class));

		// Array of enums
		var o2 = a("ENUM2");
		assertEquals(TestEnum.ENUM2, bc.convertToType(o2, TestEnum[].class)[0]);
	}

	public enum TestEnum {
		ENUM1, ENUM2, ENUM3
	}

	//====================================================================================================
	// testProxyHandler
	//====================================================================================================
	@Test void a05_proxyHandler() {
		var session = BeanContext.DEFAULT_SESSION;

		var f1 = (A) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { A.class }, new AHandler());

		var bm1 = session.toBeanMap(f1);
		assertNotNull(bm1, fs("Failed to obtain bean adapter for proxy: {0}", f1));

		var bm2 = session.newBeanMap(A.class);
		assertNotNull(bm2, fs("Failed to create dynamic proxy bean for interface: {0}", A.class.getName()));

		bm2.put("a", "Hello");
		bm2.put("b", Integer.valueOf(50));
		f1.setA("Hello");
		f1.setB(50);

		assertMap(bm2, "a,b", "Hello,50");
		assertEquals(bm1, bm2, fs("Failed equality test of dynamic proxies beans: {0} / {1}", bm1, bm2));
		assertEquals(bm2, bm1, fs("Failed reverse equality test of dynamic proxies beans: {0} / {1}", bm1, bm2));
	}

	public interface A {
		String getA();
		void setA(String a);

		int getB();
		void setB(int b);
	}

	public static class AHandler implements InvocationHandler {
		private Map<String,Object> map;

		public AHandler() {
			map = new HashMap<>();
			map.put("a", "");
			map.put("b", Integer.valueOf(0));
		}

		@Override /* InvocationHandler */
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			var methodName = method.getName();
			if (methodName.equals("getA")) {
				return map.get("a");
			}
			if (methodName.equals("setA")) {
				map.put("a", args[0]);
				return null;
			}
			if (methodName.equals("getB")) {
				return map.get("b");
			}
			if (methodName.equals("setB")) {
				map.put("b", args[0]);
				return null;
			}
			if (methodName.equals("toString")) {
				return map.toString();
			}
			return null;
		}
	}

	//====================================================================================================
	// testFluentStyleSetters
	//====================================================================================================
	@Test void a06_fluentStyleSetters() {
		var t = new B2().init();
		var m = BeanContext.DEFAULT.toBeanMap(t);
		m.put("f1", 2);
		assertEquals(2, t.f1);
	}

	public static class B {
		int f1;
		public int getF1() { return f1; }
		public B setF1(int v) { f1 = v; return this; }
	}

	public static class B2 extends B {
		@Override /* B */
		public B2 setF1(int v) { f1 = v; return this; }
		public B2 init() { f1 = 1; return this;}
	}

	//====================================================================================================
	// testClassMetaCaching
	//====================================================================================================
	@Test void a07_classMetaCaching() {
		var p1 = JsonParser.create();
		var p2 = JsonParser.create();
		assertSameCache(p1, p2);

		assertDifferentCache(p1.beansRequireDefaultConstructor(), p2);
		assertSameCache(p1, p2.beansRequireDefaultConstructor());

		assertDifferentCache(p1.beansRequireSerializable(), p2);
		assertSameCache(p1, p2.beansRequireSerializable());

		assertDifferentCache(p1.beansRequireSettersForGetters(), p2);
		assertSameCache(p1, p2.beansRequireSettersForGetters());

		assertDifferentCache(p1.disableBeansRequireSomeProperties(), p2);
		assertSameCache(p1, p2.disableBeansRequireSomeProperties());

		assertDifferentCache(p1.beanMapPutReturnsOldValue(), p2);
		assertSameCache(p1, p2.beanMapPutReturnsOldValue());

		assertDifferentCache(p1.beanConstructorVisibility(DEFAULT), p2);
		assertSameCache(p1, p2.beanConstructorVisibility(DEFAULT));
		assertDifferentCache(p1.beanConstructorVisibility(NONE), p2);
		assertSameCache(p1, p2.beanConstructorVisibility(NONE));
		assertDifferentCache(p1.beanConstructorVisibility(PRIVATE), p2);
		assertSameCache(p1, p2.beanConstructorVisibility(PRIVATE));
		assertDifferentCache(p1.beanConstructorVisibility(PROTECTED), p2);
		assertSameCache(p1, p2.beanConstructorVisibility(PROTECTED));

		assertDifferentCache(p1.beanClassVisibility(DEFAULT), p2);
		assertSameCache(p1, p2.beanClassVisibility(DEFAULT));
		assertDifferentCache(p1.beanClassVisibility(NONE), p2);
		assertSameCache(p1, p2.beanClassVisibility(NONE));
		assertDifferentCache(p1.beanClassVisibility(PRIVATE), p2);
		assertSameCache(p1, p2.beanClassVisibility(PRIVATE));
		assertDifferentCache(p1.beanClassVisibility(PROTECTED), p2);
		assertSameCache(p1, p2.beanClassVisibility(PROTECTED));

		assertDifferentCache(p1.beanFieldVisibility(DEFAULT), p2);
		assertSameCache(p1, p2.beanFieldVisibility(DEFAULT));
		assertDifferentCache(p1.beanFieldVisibility(NONE), p2);
		assertSameCache(p1, p2.beanFieldVisibility(NONE));
		assertDifferentCache(p1.beanFieldVisibility(PRIVATE), p2);
		assertSameCache(p1, p2.beanFieldVisibility(PRIVATE));
		assertDifferentCache(p1.beanFieldVisibility(PROTECTED), p2);
		assertSameCache(p1, p2.beanFieldVisibility(PROTECTED));

		assertDifferentCache(p1.beanMethodVisibility(DEFAULT), p2);
		assertSameCache(p1, p2.beanMethodVisibility(DEFAULT));
		assertDifferentCache(p1.beanMethodVisibility(NONE), p2);
		assertSameCache(p1, p2.beanMethodVisibility(NONE));
		assertDifferentCache(p1.beanMethodVisibility(PRIVATE), p2);
		assertSameCache(p1, p2.beanMethodVisibility(PRIVATE));
		assertDifferentCache(p1.beanMethodVisibility(PROTECTED), p2);
		assertSameCache(p1, p2.beanMethodVisibility(PROTECTED));

		assertDifferentCache(p1.useJavaBeanIntrospector(), p2);
		assertSameCache(p1, p2.useJavaBeanIntrospector());

		assertDifferentCache(p1.disableInterfaceProxies(), p2);
		assertSameCache(p1, p2.disableInterfaceProxies());

		assertDifferentCache(p1.ignoreUnknownBeanProperties(), p2);
		assertSameCache(p1, p2.ignoreUnknownBeanProperties());

		assertDifferentCache(p1.disableIgnoreUnknownNullBeanProperties(), p2);
		assertSameCache(p1, p2.disableIgnoreUnknownNullBeanProperties());

		assertDifferentCache(p1.disableIgnoreMissingSetters(), p2);
		assertSameCache(p1, p2.disableIgnoreMissingSetters());

		assertDifferentCache(p1.ignoreInvocationExceptionsOnGetters(), p2);
		assertSameCache(p1, p2.ignoreInvocationExceptionsOnGetters());

		assertDifferentCache(p1.ignoreInvocationExceptionsOnSetters(), p2);
		assertSameCache(p1, p2.ignoreInvocationExceptionsOnSetters());

		assertDifferentCache(p1.notBeanPackages("foo"), p2);
		assertSameCache(p1, p2.notBeanPackages("foo"));
		assertDifferentCache(p1.notBeanPackages("bar"), p2);
		assertSameCache(p1, p2.notBeanPackages("bar"));
		assertDifferentCache(p1.notBeanPackages("baz").notBeanPackages("bing"), p2);
		assertSameCache(p2.notBeanPackages("bing").notBeanPackages("baz"), p2);

		p1.beanContext().notBeanPackages().remove("bar");
		assertDifferentCache(p1, p2);
		p2.beanContext().notBeanPackages().remove("bar");
		assertSameCache(p1, p2);

		assertDifferentCache(p1.swaps(DummyPojoSwapA.class), p2);
		assertSameCache(p1, p2.swaps(DummyPojoSwapA.class));
		assertDifferentCache(p1.swaps(DummyPojoSwapB.class,DummyPojoSwapC.class), p2.swaps(DummyPojoSwapC.class,DummyPojoSwapB.class));  // Order of filters is important!
	}

	public static class DummyPojoSwapA extends MapSwap<A> {}
	public static class DummyPojoSwapB extends MapSwap<B> {}
	public static class DummyPojoSwapC extends MapSwap<C> {}
	public static class C {}

	private void assertSameCache(Parser.Builder p1b, Parser.Builder p2b) {
		var p1 = p1b.build();
		var p2 = p2b.build();
		assertTrue(p1.getBeanContext().hasSameCache(p2.getBeanContext()));
	}

	private void assertDifferentCache(Parser.Builder p1b, Parser.Builder p2b) {
		var p1 = p1b.build();
		var p2 = p2b.build();
		assertFalse(p1.getBeanContext().hasSameCache(p2.getBeanContext()));
	}

	//====================================================================================================
	// testNotABeanReasons
	//====================================================================================================
	@Test void a08_notABeanNonStaticInnerClass() {
		var bc = BeanContext.DEFAULT;
		var cm = bc.getClassMeta(C1.class);
		assertFalse(cm.canCreateNewInstance());
	}

	public class C1 {
		public int f1;
	}

	//====================================================================================================
	// testAddingToArrayProperty
	// This tests the speed of the BeanMap.add() method against array properties.
	// For performance reasons, array properties are stored as temporary ArrayLists until the
	// BeanMap.getBean() method is called.
	//====================================================================================================
	// Should be around 100ms at most.
	@Test void a09_addingToArrayProperty() {
		assertTimeout(Duration.ofSeconds(1), () -> {
			var bc = BeanContext.DEFAULT;
			var bm = bc.newBeanMap(D.class);
			for (var i = 0; i < 5000; i++) {
				bm.add("f1", i);
				bm.add("f2", i);
				bm.add("f3", i);
				bm.add("f4", i);
			}
			var d = bm.getBean();
			assertBean(d, "f1{length},f2{length},f3{length},f4{length}", "{5000},{5000},{5003},{5003}");
		});
	}

	public class D {
		public int[] f1;
		private int[] f2;
		public int[] f3 = {1,2,3};
		private int[] f4 = {1,2,3};
		public int[] getF2() {return f2;}
		public void setF2(int[] v) {f2 = v;}
		public int[] getF4() {return f4;}
		public void setF4(int[] v) {f4 = v;}
	}

	//====================================================================================================
	// testClassClassMeta
	// Make sure we can get ClassMeta objects against the Class class.
	//====================================================================================================
	@Test void a10_classClassMeta() {
		assertNotNull(BeanContext.DEFAULT.getClassMeta(Class.class));
		assertNotNull(BeanContext.DEFAULT.getClassMeta(Class[].class));
	}

	//====================================================================================================
	// testBlanks
	//====================================================================================================
	@Test void a11_blanks() throws Exception {
		var bc = BeanContext.DEFAULT;

		// Blanks get interpreted as the default value for primitives and null for boxed objects.
		assertEquals(0, (int)bc.convertToType("", int.class));
		assertNull(bc.convertToType("", Integer.class));

		// Booleans are handled different since 'Boolean.valueOf("")' is valid and resolves to false
		// while 'Integer.valueOf("")' produces an exception.
		assertEquals(false, (boolean)bc.convertToType("", boolean.class));
		assertEquals(null, bc.convertToType("", Boolean.class));
	}
}