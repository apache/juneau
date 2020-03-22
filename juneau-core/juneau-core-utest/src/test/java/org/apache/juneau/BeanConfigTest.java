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

import static org.junit.Assert.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@SuppressWarnings({"unchecked","rawtypes"})
public class BeanConfigTest {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {

		BeanSession session = BeanContext.DEFAULT.createSession();

		Person p1 = new Person();
		p1.setName("John Doe");
		p1.setAge(25);

		Address a = new Address("101 Main St.", "Las Vegas", "NV", "89101");
		AddressablePerson p2 = new AddressablePerson();
		p2.setName("Jane Doe");
		p2.setAge(21);
		p2.setAddress(a);

		// setup the reference results
		Map m1 = new LinkedHashMap();
		m1.put("name", p1.getName());
		m1.put("age", new Integer(p1.getAge()));

		Map m2 = new LinkedHashMap();
		m2.put("street", a.getStreet());
		m2.put("city", a.getCity());
		m2.put("state", a.getState());
		m2.put("zip", a.getZip());

		Map m3 = new LinkedHashMap();
		m3.put("name", p2.getName());
		m3.put("age", new Integer(p2.getAge()));
		m3.put("address", p2.getAddress());

		Map pm1 = session.toBeanMap(p1);

		if (pm1.size() != m1.size())
			fail("Bean Map size failed for: " + p1 + " / " + pm1.size()+ " / " + m1.size());

		if (!pm1.keySet().equals(m1.keySet()))
			fail("Bean Map key set equality failed for: " + p1 + " / " + pm1.keySet() + " / " + m1.keySet());

		if (!m1.keySet().equals(pm1.keySet()))
			fail("Bean Map key set reverse equality failed for: " + p1 + " / " + pm1.keySet() + " / " + m1.keySet());

		if (!pm1.equals(m1))
			fail("Bean Map equality failed for: " + p1 + " / " + pm1 + " / " + m1);

		if (!m1.equals(pm1))
			fail("Bean Map reverse equality failed for: " + p1 + " / " + pm1 + " / " + m1);

		BeanMap bm1 = null;
		try {
			bm1 = session.newBeanMap(Address.class);
			fail("Address returned as a new bean type, but shouldn't be since it doesn't have a default constructor.");
		} catch (BeanRuntimeException e) {
			// Good.
		}
		bm1 = session.toBeanMap(new Address("street", "city", "state", "zip"));

		BeanMap bm2 = session.newBeanMap(java.lang.Integer.class);
		if (bm2 != null)
			fail("java.lang.Integer incorrectly desingated as bean type.");

		BeanMap bm3 = session.newBeanMap(java.lang.Class.class);
		if (bm3 != null)
			fail("java.lang.Class incorrectly desingated as bean type.");

		Map m4 = bm1;
		if (m4.keySet().size() != m2.size())
			fail("Bean Adapter map's key set has wrong size: " + a + " / " + m4.keySet().size() + " / " + m2.size());

		Iterator iter = m4.keySet().iterator();
		Set temp = new HashSet();
		int count = 0;
		while (iter.hasNext()) {
			temp.add(iter.next());
			count++;
		}
		if (count != m2.size())
			fail("Iteration count over bean adpater key set failed: " + a + " / " + count + " / " + m2.size());

		if (!m2.keySet().equals(temp))
			fail("Iteration over bean adpater key set failed: " + a + " / " + m4.keySet() + " / " + m2.keySet());

		BeanMap bm4 = session.toBeanMap(p2);
		if (bm4 == null) {
			fail("Failed to identify class as bean type: " + p2.getClass());
			return;
		}

		Map m5 = bm4;
		Set es1 = m5.entrySet();

		if (!es1.equals(m3.entrySet()))
			fail("Entry set equality failed: " + p2 + " / " + es1 + " / " + m3.entrySet());

		if (!m3.entrySet().equals(es1))
			fail("Entry set reverse equality failed: " + p2 + " / " + es1 + " / " + m3.entrySet());

		iter = es1.iterator();
		temp = new HashSet();
		count = 0;
		while (iter.hasNext()) {
			temp.add(iter.next());
			count++;
		}
		if (count != m3.size())
			fail("Iteration count over bean adpater entry set failed: " + a + " / " + count + " / " + m3.size());

		if (!m3.entrySet().equals(temp))
			fail("Iteration over bean adpater entry set failed: " + a + " / " + es1 + " / " + m3.entrySet());
	}

	public static class Person {
		private String name;
		private int age;

		public Person() {
			this.name = null;
			this.age = -1;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		@Override /* Object */
		public String toString() {
			return ("Person(name: " + this.getName() + ", age: "
					+ this.getAge() + ")");
		}
	}

	public static class Address {
		protected String street;
		protected String city;
		protected String state;
		protected String zip;

		public Address(String street, String city, String state, String zip) {
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
		}

		public String getStreet() {
			return this.street;
		}

		public String getCity() {
			return this.city;
		}

		public String getState() {
			return this.state;
		}

		public String getZip() {
			return this.zip;
		}

		@Override /* Object */
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (this == o)
				return true;
			if (this.getClass() != o.getClass())
				return false;
			Address a = (Address) o;

			String v1 = this.getStreet();
			String v2 = a.getStreet();
			if ((v1 == null) ? (v2 != null) : (!v1.equals(v2)))
				return false;

			v1 = this.getCity();
			v2 = a.getCity();
			if ((v1 == null) ? (v2 != null) : (!v1.equals(v2)))
				return false;

			v1 = this.getState();
			v2 = a.getState();
			if ((v1 == null) ? (v2 != null) : (!v1.equals(v2)))
				return false;

			v1 = this.getZip();
			v2 = a.getZip();
			return ((v1 == null) ? (v2 == null) : (v1.equals(v2)));
		}

		@Override /* Object */
		public int hashCode() {
			int code = 0;
			if (this.street != null)
				code ^= this.street.hashCode();
			if (this.city != null)
				code ^= this.city.hashCode();
			if (this.state != null)
				code ^= this.state.hashCode();
			if (this.zip != null)
				code ^= this.zip.hashCode();
			return code;
		}

		@Override /* Object */
		public String toString() {
			return ("Address(street: " + this.getStreet() + ", city: "
					+ this.getCity() + ", state: " + this.getState()
					+ ", zip: " + this.getZip() + ")");
		}
	}

	public static class AddressablePerson extends Person {
		private Address address;

		public AddressablePerson() {
			this.address = null;
		}

		public Address getAddress() {
			return this.address;
		}

		public void setAddress(Address addr) {
			this.address = addr;
		}

		@Override /* Object */
		public String toString() {
			return super.toString() + "@" + this.address;
		}
	}

	//====================================================================================================
	// Exhaustive test of BeanContext.convertToType();
	//====================================================================================================
	@Test
	public void testBeanContextConvertToType() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();
		Object o;

		// Primitive nulls.
		o = null;
		assertEquals(new Integer(0), session.convertToType(o, Integer.TYPE));
		assertEquals(new Short((short) 0), session.convertToType(o, Short.TYPE));
		assertEquals(new Long(0), session.convertToType(o, Long.TYPE));
		assertEquals(new Float(0), session.convertToType(o, Float.TYPE));
		assertEquals(new Double(0), session.convertToType(o, Double.TYPE));
		assertEquals(new Byte((byte) 0), session.convertToType(o, Byte.TYPE));
		assertEquals(new Character((char) 0), session.convertToType(o, Character.TYPE));
		assertEquals(Boolean.FALSE, session.convertToType(o, Boolean.TYPE));

		o = "1";

		assertEquals(new Integer(1), session.convertToType(o, Integer.class));
		assertEquals(new Short((short) 1), session.convertToType(o, Short.class));
		assertEquals(new Long(1), session.convertToType(o, Long.class));
		assertEquals(new Float(1), session.convertToType(o, Float.class));
		assertEquals(new Double(1), session.convertToType(o, Double.class));
		assertEquals(new Byte((byte) 1), session.convertToType(o, Byte.class));
		assertEquals(new Character('1'), session.convertToType(o, Character.class));
		assertEquals(Boolean.FALSE, session.convertToType(o, Boolean.class));

		assertEquals(new Integer(1), session.convertToType(o, Integer.TYPE));
		assertEquals(new Short((short) 1), session.convertToType(o, Short.TYPE));
		assertEquals(new Long(1), session.convertToType(o, Long.TYPE));
		assertEquals(new Float(1), session.convertToType(o, Float.TYPE));
		assertEquals(new Double(1), session.convertToType(o, Double.TYPE));
		assertEquals(new Byte((byte) 1), session.convertToType(o, Byte.TYPE));
		assertEquals(new Character('1'), session.convertToType(o, Character.TYPE));
		assertEquals(Boolean.FALSE, session.convertToType(o, Boolean.TYPE));

		o = new Integer(1);

		assertEquals(new Integer(1), session.convertToType(o, Integer.TYPE));
		assertEquals(new Short((short) 1), session.convertToType(o, Short.TYPE));
		assertEquals(new Long(1), session.convertToType(o, Long.TYPE));
		assertEquals(new Float(1), session.convertToType(o, Float.TYPE));
		assertEquals(new Double(1), session.convertToType(o, Double.TYPE));
		assertEquals(new Byte((byte) 1), session.convertToType(o, Byte.TYPE));
		assertEquals(new Character('1'), session.convertToType(o, Character.TYPE));
		assertEquals(Boolean.TRUE, session.convertToType(o, Boolean.TYPE));

		o = new Integer(0);
		assertEquals(Boolean.FALSE, session.convertToType(o, Boolean.TYPE));

		// Bean
		o = "{name:'x',age:123}";
		assertEquals("x", session.convertToType(o, Person.class).getName());
		assertEquals(123, session.convertToType(o, Person.class).getAge());

		// Read-only bean
		o = "{name:'x',age:123}";
		assertEquals("x", session.convertToType(o, ReadOnlyPerson.class).getName());
		assertEquals(123, session.convertToType(o, ReadOnlyPerson.class).getAge());

		// Class with forString(String) method.
		o = UUID.randomUUID();
		assertEquals(o, session.convertToType(o.toString(), UUID.class));

		// Class with Constructor(String).
		o = "xxx";
		File file = session.convertToType(o, File.class);
		assertEquals("xxx", file.getName());

		// List of ints to array
		o = new ObjectList(1, 2, 3);
		assertEquals(1, session.convertToType(o, int[].class)[0]);

		// List of beans to array
		o = new ObjectList(new ReadOnlyPerson("x", 123));
		assertEquals("x", session.convertToType(o, ReadOnlyPerson[].class)[0].getName());

		// Multi-dimensional array of beans.
		o = new ObjectList().append(new ObjectList(new ReadOnlyPerson("x", 123)));
		assertEquals("x", session.convertToType(o, ReadOnlyPerson[][].class)[0][0].getName());

		// Array of strings to array of ints
		o = new String[] { "1", "2", "3" };
		assertEquals(new Integer(1), session.convertToType(o, Integer[].class)[0]);
		assertEquals(1, session.convertToType(o, int[].class)[0]);

		// Array to list
		o = new Integer[] { 1, 2, 3 };
		assertEquals(new Integer(1), session.convertToType(o, LinkedList.class).get(0));

		// HashMap to TreeMap
		o = AMap.of(1, "foo");
		assertEquals("foo", session.convertToType(o, TreeMap.class).firstEntry().getValue());

		// String to TreeMap
		o = "{1:'foo'}";
		assertEquals("foo", session.convertToType(o, TreeMap.class).firstEntry().getValue());

		// String to generic Map
		assertEquals("foo", session.convertToType(o, Map.class).values().iterator().next());

		// Array to String
		o = new Object[] { "a", 1, false };
		assertEquals("['a',1,false]", session.convertToType(o, String.class));
		o = new Object[][] { { "a", 1, false } };
		assertEquals("[['a',1,false]]", session.convertToType(o, String.class));

	}

	//====================================================================================================
	// Test properties set through a constructor.
	//====================================================================================================
	@Test
	public void testReadOnlyProperties() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();
		Object o;

		// Bean to String
		o = new ReadOnlyPerson("x", 123);
		assertEquals("{name:'x',age:123}", session.convertToType(o, String.class));

		// List of Maps to array of beans.
		o = new ObjectList(new ObjectMap("{name:'x',age:1}"), new ObjectMap("{name:'y',age:2}"));
		assertEquals(1, session.convertToType(o, ReadOnlyPerson[].class)[0].getAge());
	}


	@Bean(bpi="name,age")
	public static class ReadOnlyPerson {
		private final String name;
		private final int age;

		@Beanc(properties="name,age")
		public ReadOnlyPerson(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return this.name;
		}

		public int getAge() {
			return this.age;
		}

		@Override /* Object */
		public String toString() {
			return "toString():name=" + name + ",age=" + age;
		}
	}

	@Test
	public void testReadOnlyProperties_usingConfig() throws Exception {
		BeanSession session = BeanContext.DEFAULT.builder().applyAnnotations(ReadOnlyPerson2.class).build().createSession();
		Object o;

		// Bean to String
		o = new ReadOnlyPerson2("x", 123);
		assertEquals("{name:'x',age:123}", session.convertToType(o, String.class));

		// List of Maps to array of beans.
		o = new ObjectList(new ObjectMap("{name:'x',age:1}"), new ObjectMap("{name:'y',age:2}"));
		assertEquals(1, session.convertToType(o, ReadOnlyPerson2[].class)[0].getAge());
	}


	@BeanConfig(applyBean=@Bean(on="ReadOnlyPerson2",bpi="name,age"),applyBeanc=@Beanc(on="ReadOnlyPerson2(String,int)",properties="name,age"))
	public static class ReadOnlyPerson2 {
		private final String name;
		private final int age;


		public ReadOnlyPerson2(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return this.name;
		}

		public int getAge() {
			return this.age;
		}

		@Override /* Object */
		public String toString() {
			return "toString():name=" + name + ",age=" + age;
		}
	}

	//====================================================================================================
	// testEnums
	//====================================================================================================
	@Test
	public void testEnums() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();
		Object o;

		// Enum
		o = "ENUM2";
		assertEquals(TestEnum.ENUM2, session.convertToType(o, TestEnum.class));
		assertEquals("ENUM2", session.convertToType(TestEnum.ENUM2, String.class));

		// Array of enums
		o = new String[] { "ENUM2" };
		assertEquals(TestEnum.ENUM2, session.convertToType(o, TestEnum[].class)[0]);
	}

	public enum TestEnum {
		ENUM1, ENUM2, ENUM3
	}

	//====================================================================================================
	// testProxyHandler
	//====================================================================================================
	@Test
	public void testProxyHandler() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createBeanSession();

		A f1 = (A) Proxy.newProxyInstance(this.getClass()
				.getClassLoader(), new Class[] { A.class },
				new AHandler());

		BeanMap bm1 = session.toBeanMap(f1);
		if (bm1 == null) {
			fail("Failed to obtain bean adapter for proxy: " + f1);
			return;
		}

		BeanMap bm2 = session.newBeanMap(A.class);
		if (bm2 == null) {
			fail("Failed to create dynamic proxy bean for interface: " + A.class.getName());
			return;
		}
		bm2.put("a", "Hello");
		bm2.put("b", new Integer(50));
		f1.setA("Hello");
		f1.setB(50);

		if (!bm2.get("a").equals("Hello"))
			fail("Failed to set string property 'a' on dynamic proxy bean.  " + bm2);

		if (!bm2.get("b").equals(new Integer(50)))
			fail("Failed to set string property 'b' on dynamic proxy bean.  " + bm2);

		if (!bm1.equals(bm2))
			fail("Failed equality test of dynamic proxies beans: " + bm1 + " / " + bm2);

		if (!bm2.equals(bm1))
			fail("Failed reverse equality test of dynamic proxies beans: " + bm1 + " / " + bm2);
	}

	public static interface A {
		String getA();

		void setA(String a);

		int getB();

		void setB(int b);
	}

	public static class AHandler implements InvocationHandler {
		private Map map;

		public AHandler() {
			this.map = new HashMap();
			this.map.put("a", "");
			this.map.put("b", new Integer(0));
		}

		@Override /* InvocationHandler */
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			String methodName = method.getName();
			if (methodName.equals("getA")) {
				return this.map.get("a");
			}
			if (methodName.equals("setA")) {
				this.map.put("a", args[0]);
				return null;
			}
			if (methodName.equals("getB")) {
				return this.map.get("b");
			}
			if (methodName.equals("setB")) {
				this.map.put("b", args[0]);
				return null;
			}
			if (methodName.equals("toString")) {
				return this.map.toString();
			}
			return null;
		}
	}

	//====================================================================================================
	// testFluentStyleSetters
	//====================================================================================================
	@Test
	public void testFluentStyleSetters() throws Exception {
		B2 t = new B2().init();
		BeanMap m = BeanContext.DEFAULT.createSession().toBeanMap(t);
		m.put("f1", 2);
		assertEquals(t.f1, 2);
	}

	public static class B {
		int f1;
		public int getF1() { return f1; }
		public B setF1(int f1) { this.f1 = f1; return this; }
	}

	public static class B2 extends B {
		@Override /* B */
		public B2 setF1(int f1) { this.f1 = f1; return this; }
		public B2 init() { this.f1 = 1; return this;}
	}

	//====================================================================================================
	// testClassMetaCaching
	//====================================================================================================
	@Test
	public void testClassMetaCaching() throws Exception {
		ParserBuilder p1, p2;

		p1 = JsonParser.create();
		p2 = JsonParser.create();
		assertSameCache(p1, p2);

		p1.beansRequireDefaultConstructor();
		assertDifferentCache(p1, p2);
		p2.beansRequireDefaultConstructor();
		assertSameCache(p1, p2);

		p1.beansRequireSerializable();
		assertDifferentCache(p1, p2);
		p2.beansRequireSerializable();
		assertSameCache(p1, p2);

		p1.beansRequireSettersForGetters();
		assertDifferentCache(p1, p2);
		p2.beansRequireSettersForGetters();
		assertSameCache(p1, p2);

		p1.beansRequireSomeProperties(false);
		assertDifferentCache(p1, p2);
		p2.beansRequireSomeProperties(false);
		assertSameCache(p1, p2);

		p1.beanMapPutReturnsOldValue();
		assertDifferentCache(p1, p2);
		p2.beanMapPutReturnsOldValue();
		assertSameCache(p1, p2);

		p1.beanConstructorVisibility(Visibility.DEFAULT);
		assertDifferentCache(p1, p2);
		p2.beanConstructorVisibility(Visibility.DEFAULT);
		assertSameCache(p1, p2);
		p1.beanConstructorVisibility(Visibility.NONE);
		assertDifferentCache(p1, p2);
		p2.beanConstructorVisibility(Visibility.NONE);
		assertSameCache(p1, p2);
		p1.beanConstructorVisibility(Visibility.PRIVATE);
		assertDifferentCache(p1, p2);
		p2.beanConstructorVisibility(Visibility.PRIVATE);
		assertSameCache(p1, p2);
		p1.beanConstructorVisibility(Visibility.PROTECTED);
		assertDifferentCache(p1, p2);
		p2.beanConstructorVisibility(Visibility.PROTECTED);
		assertSameCache(p1, p2);

		p1.beanClassVisibility(Visibility.DEFAULT);
		assertDifferentCache(p1, p2);
		p2.beanClassVisibility(Visibility.DEFAULT);
		assertSameCache(p1, p2);
		p1.beanClassVisibility(Visibility.NONE);
		assertDifferentCache(p1, p2);
		p2.beanClassVisibility(Visibility.NONE);
		assertSameCache(p1, p2);
		p1.beanClassVisibility(Visibility.PRIVATE);
		assertDifferentCache(p1, p2);
		p2.beanClassVisibility(Visibility.PRIVATE);
		assertSameCache(p1, p2);
		p1.beanClassVisibility(Visibility.PROTECTED);
		assertDifferentCache(p1, p2);
		p2.beanClassVisibility(Visibility.PROTECTED);
		assertSameCache(p1, p2);

		p1.beanFieldVisibility(Visibility.DEFAULT);
		assertDifferentCache(p1, p2);
		p2.beanFieldVisibility(Visibility.DEFAULT);
		assertSameCache(p1, p2);
		p1.beanFieldVisibility(Visibility.NONE);
		assertDifferentCache(p1, p2);
		p2.beanFieldVisibility(Visibility.NONE);
		assertSameCache(p1, p2);
		p1.beanFieldVisibility(Visibility.PRIVATE);
		assertDifferentCache(p1, p2);
		p2.beanFieldVisibility(Visibility.PRIVATE);
		assertSameCache(p1, p2);
		p1.beanFieldVisibility(Visibility.PROTECTED);
		assertDifferentCache(p1, p2);
		p2.beanFieldVisibility(Visibility.PROTECTED);
		assertSameCache(p1, p2);

		p1.beanMethodVisibility(Visibility.DEFAULT);
		assertDifferentCache(p1, p2);
		p2.beanMethodVisibility(Visibility.DEFAULT);
		assertSameCache(p1, p2);
		p1.beanMethodVisibility(Visibility.NONE);
		assertDifferentCache(p1, p2);
		p2.beanMethodVisibility(Visibility.NONE);
		assertSameCache(p1, p2);
		p1.beanMethodVisibility(Visibility.PRIVATE);
		assertDifferentCache(p1, p2);
		p2.beanMethodVisibility(Visibility.PRIVATE);
		assertSameCache(p1, p2);
		p1.beanMethodVisibility(Visibility.PROTECTED);
		assertDifferentCache(p1, p2);
		p2.beanMethodVisibility(Visibility.PROTECTED);
		assertSameCache(p1, p2);

		p1.useJavaBeanIntrospector();
		assertDifferentCache(p1, p2);
		p2.useJavaBeanIntrospector();
		assertSameCache(p1, p2);

		p1.useInterfaceProxies(false);
		assertDifferentCache(p1, p2);
		p2.useInterfaceProxies(false);
		assertSameCache(p1, p2);

		p1.ignoreUnknownBeanProperties();
		assertDifferentCache(p1, p2);
		p2.ignoreUnknownBeanProperties();
		assertSameCache(p1, p2);

		p1.ignoreUnknownNullBeanProperties(false);
		assertDifferentCache(p1, p2);
		p2.ignoreUnknownNullBeanProperties(false);
		assertSameCache(p1, p2);

		p1.ignorePropertiesWithoutSetters(false);
		assertDifferentCache(p1, p2);
		p2.ignorePropertiesWithoutSetters(false);
		assertSameCache(p1, p2);

		p1.ignoreInvocationExceptionsOnGetters();
		assertDifferentCache(p1, p2);
		p2.ignoreInvocationExceptionsOnGetters();
		assertSameCache(p1, p2);

		p1.ignoreInvocationExceptionsOnSetters();
		assertDifferentCache(p1, p2);
		p2.ignoreInvocationExceptionsOnSetters();
		assertSameCache(p1, p2);

		p1.notBeanPackages("foo");
		assertDifferentCache(p1, p2);
		p2.notBeanPackages("foo");
		assertSameCache(p1, p2);
		p1.notBeanPackages("bar");
		assertDifferentCache(p1, p2);
		p2.notBeanPackages("bar");
		assertSameCache(p1, p2);
		p1.notBeanPackages("baz");
		p1.notBeanPackages("bing");
		assertDifferentCache(p1, p2);
		p2.notBeanPackages("bing");
		p2.notBeanPackages("baz");
		assertSameCache(p1, p2);

		p1.notBeanPackagesRemove("bar");
		assertDifferentCache(p1, p2);
		p2.notBeanPackagesRemove("bar");
		assertSameCache(p1, p2);

		p1.pojoSwaps(DummyPojoSwapA.class);
		assertDifferentCache(p1, p2);
		p2.pojoSwaps(DummyPojoSwapA.class);
		assertSameCache(p1, p2);
		p1.pojoSwaps(DummyPojoSwapB.class,DummyPojoSwapC.class);  // Order of filters is important!
		p2.pojoSwaps(DummyPojoSwapC.class,DummyPojoSwapB.class);
		assertDifferentCache(p1, p2);

		p1 = JsonParser.create();
		p2 = JsonParser.create();
		p1.beanFilters(DummyBeanFilterA.class);
		assertDifferentCache(p1, p2);
		p2.beanFilters(DummyBeanFilterA.class);
		assertSameCache(p1, p2);
		p1.beanFilters(DummyBeanFilterB.class,DummyBeanFilterC.class);  // Order of filters is important!
		p2.beanFilters(DummyBeanFilterC.class,DummyBeanFilterB.class);
		assertDifferentCache(p1, p2);
	}

	public static class DummyPojoSwapA extends MapSwap<A> {}
	public static class DummyPojoSwapB extends MapSwap<B> {}
	public static class DummyPojoSwapC extends MapSwap<C> {}
	public static class DummyBeanFilterA extends BeanFilterBuilder<A> {}
	public static class DummyBeanFilterB extends BeanFilterBuilder<B> {}
	public static class DummyBeanFilterC extends BeanFilterBuilder<C> {}
	public static class C {}

	private void assertSameCache(ParserBuilder p1b, ParserBuilder p2b) {
		Parser p1 = p1b.build(), p2 = p2b.build();
		assertTrue(p1.hasSameCache(p2));
		assertTrue(p1.hashCode() == p2.hashCode());
	}

	private void assertDifferentCache(ParserBuilder p1b, ParserBuilder p2b) {
		Parser p1 = p1b.build(), p2 = p2b.build();
		assertFalse(p1.hasSameCache(p2));
		assertFalse(p1.hashCode() == p2.hashCode());
	}

	//====================================================================================================
	// testNotABeanReasons
	//====================================================================================================
	@Test
	public void testNotABeanNonStaticInnerClass() throws Exception {
		BeanContext bc = BeanContext.DEFAULT;
		ClassMeta cm = bc.getClassMeta(C1.class);
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
	@Test(timeout=1000) // Should be around 100ms at most.
	public void testAddingToArrayProperty() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();
		BeanMap<D> bm = session.newBeanMap(D.class);
		for (int i = 0; i < 5000; i++) {
			bm.add("f1", i);
			bm.add("f2", i);
			bm.add("f3", i);
			bm.add("f4", i);
		}
		D d = bm.getBean();
		assertEquals(d.f1.length, 5000);
		assertEquals(d.f2.length, 5000);
		assertEquals(d.f3.length, 5003);
		assertEquals(d.f4.length, 5003);
	}

	public class D {
		public int[] f1;
		private int[] f2;
		public int[] f3 = new int[]{1,2,3};
		private int[] f4 = new int[]{1,2,3};
		public int[] getF2() {return f2;}
		public void setF2(int[] f2) {this.f2 = f2;}
		public int[] getF4() {return f4;}
		public void setF4(int[] f4) {this.f4 = f4;}
	}

	//====================================================================================================
	// testClassClassMeta
	// Make sure we can get ClassMeta objects against the Class class.
	//====================================================================================================
	@Test
	public void testClassClassMeta() throws Exception {
		ClassMeta cm = BeanContext.DEFAULT.getClassMeta(Class.class);
		assertNotNull(cm);

		cm = BeanContext.DEFAULT.getClassMeta(Class[].class);
		assertNotNull(cm);
	}

	//====================================================================================================
	// testBlanks
	//====================================================================================================
	@Test
	public void testBlanks() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();

		// Blanks get interpreted as the default value for primitives and null for boxed objects.
		assertEquals(0, (int)session.convertToType("", int.class));
		assertNull(session.convertToType("", Integer.class));

		// Booleans are handled different since 'new Boolean("")' is valid and resolves to false
		// while 'new Integer("")' produces an exception.
		assertEquals(false, (boolean)session.convertToType("", boolean.class));
		assertEquals(null, session.convertToType("", Boolean.class));
	}
}