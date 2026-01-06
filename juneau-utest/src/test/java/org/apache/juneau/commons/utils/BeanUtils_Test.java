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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.Named;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

class BeanUtils_Test extends TestBase {

	// Test bean classes
	static class TestService {
		private final String name;
		TestService(String name) { this.name = name; }
		String getName() { return name; }
		@Override public String toString() { return "TestService[" + name + "]"; }
		@Override public boolean equals(Object o) { return o instanceof TestService o2 && eq(this, o2, (x,y) -> eq(x.name, y.name)); }
		@Override public int hashCode() { return h(name); }
	}

	static class AnotherService {
		private final int value;
		AnotherService(int value) { this.value = value; }
		int getValue() { return value; }
		@Override public boolean equals(Object o) { return o instanceof AnotherService o2 && eq(this, o2, (x,y) -> eq(x.value, y.value)); }
		@Override public int hashCode() { return value; }
	}

	// Test classes with various constructor/method signatures
	public static class TestClass1 {
		public TestClass1(TestService service) {
			// Single bean parameter
		}
	}

	public static class TestClass2 {
		public TestClass2(@Named("service1") TestService service) {
			// Named bean parameter
		}
	}

	public static class TestClass3 {
		public TestClass3(Optional<TestService> service) {
			// Optional parameter
		}
	}

	public static class TestClass4 {
		public TestClass4(TestService[] services) {
			// Array parameter
		}
	}

	public static class TestClass5 {
		public TestClass5(List<TestService> services) {
			// List parameter
		}
	}

	public static class TestClass6 {
		public TestClass6(Set<TestService> services) {
			// Set parameter
		}
	}

	public static class TestClass7 {
		public TestClass7(Map<String, TestService> services) {
			// Map parameter
		}
	}

	public static class TestClass8 {
		public TestClass8(TestService service1, AnotherService service2) {
			// Multiple parameters
		}
	}

	public static class TestClass9 {
		public TestClass9(TestService service, Optional<AnotherService> optional, List<TestService> list) {
			// Mixed parameter types
		}
	}

	// Inner class for testing outer parameter
	// Note: This is a non-static inner class, so it requires an outer instance
	static class OuterClass {
		public OuterClass() {}

		// Non-static inner class
		class InnerClass {
			public InnerClass(OuterClass outer, TestService service) {
				// First parameter is outer class instance
			}
		}
	}

	// Test method classes
	public static class TestMethodClass {
		public void method1(TestService service) {}
		public void method2(@Named("service1") TestService service) {}
		public void method3(Optional<TestService> service) {}
		public void method4(TestService[] services) {}
		public void method5(List<TestService> services) {}
		public void method6(Set<TestService> services) {}
		public void method7(Map<String, TestService> services) {}
		public static void staticMethod(TestService service) {}
	}

	private BeanUtils utils;
	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		utils = new BeanUtils();
		beanStore = new BasicBeanStore(null);
	}

	//====================================================================================================
	// getMissingParams
	//====================================================================================================

	@Test
	void a01_getMissingParams_allAvailable() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"));
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertNull(result);
	}

	@Test
	void a02_getMissingParams_singleMissing() throws Exception {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertEquals("TestService", result);
	}

	@Test
	void a03_getMissingParams_namedBeanFound() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"), "service1");
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertNull(result);
	}

	@Test
	void a04_getMissingParams_namedBeanMissing() throws Exception {
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertEquals("TestService@service1", result);
	}

	@Test
	void a05_getMissingParams_optionalSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertNull(result); // Optional parameters are skipped
	}

	@Test
	void a06_getMissingParams_arraySkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass4.class).getPublicConstructor(x -> x.hasParameterTypes(TestService[].class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertNull(result); // Arrays are skipped
	}

	@Test
	void a07_getMissingParams_listSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertNull(result); // Lists are skipped
	}

	@Test
	void a08_getMissingParams_setSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass6.class).getPublicConstructor(x -> x.hasParameterTypes(Set.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertNull(result); // Sets are skipped
	}

	@Test
	void a09_getMissingParams_mapSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass7.class).getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertNull(result); // Maps are skipped
	}

	@Test
	void a10_getMissingParams_multipleMissing() throws Exception {
		var constructor = ClassInfo.of(TestClass8.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class, AnotherService.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertTrue(result.contains("AnotherService"));
		assertTrue(result.contains("TestService"));
		// Should be sorted
		assertTrue(result.indexOf("AnotherService") < result.indexOf("TestService"));
	}

	@Test
	void a11_getMissingParams_innerClassOuterSkipped() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"));
		beanStore.addBean(OuterClass.class, new OuterClass()); // Add outer class bean so explicit parameter is available
		var outer = new OuterClass();
		// Use the class literal directly instead of getting it from an instance
		// For non-static inner classes, the constructor has 3 parameters: (implicit outer, explicit outer, TestService)
		// The first parameter (implicit outer) is skipped, but the second (explicit outer) is checked
		var classInfo = ClassInfo.of(OuterClass.InnerClass.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 3 && x.getParameter(2).isType(TestService.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available constructors: " + constructors);
		var constructor = constructorOpt.get();
		var result = utils.getMissingParams(constructor, beanStore, outer);
		assertNull(result); // All parameters should be available (implicit outer skipped, explicit outer and TestService in store)
	}

	//====================================================================================================
	// getParams
	//====================================================================================================

	@Test
	void b01_getParams_singleBean() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertSame(service, params[0]);
	}

	@Test
	void b02_getParams_singleBeanNotFound() throws Exception {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertNull(params[0]);
	}

	@Test
	void b03_getParams_namedBean() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertSame(service1, params[0]);
	}

	@Test
	void b04_getParams_optionalBeanFound() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Optional);
		var opt = (Optional<TestService>) params[0];
		assertTrue(opt.isPresent());
		assertSame(service, opt.get());
	}

	@Test
	void b05_getParams_optionalBeanNotFound() throws Exception {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Optional);
		var opt = (Optional<TestService>) params[0];
		assertFalse(opt.isPresent());
	}

	@Test
	void b06_getParams_array() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass4.class).getPublicConstructor(x -> x.hasParameterTypes(TestService[].class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof TestService[]);
		var array = (TestService[]) params[0];
		assertEquals(2, array.length);
		assertTrue(array[0].equals(service1) || array[0].equals(service2));
		assertTrue(array[1].equals(service1) || array[1].equals(service2));
	}

	@Test
	void b07_getParams_arrayEmpty() throws Exception {
		var constructor = ClassInfo.of(TestClass4.class).getPublicConstructor(x -> x.hasParameterTypes(TestService[].class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof TestService[]);
		var array = (TestService[]) params[0];
		assertEquals(0, array.length);
	}

	@Test
	void b08_getParams_list() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof List);
		var list = (List<TestService>) params[0];
		assertEquals(2, list.size());
		assertTrue(list.contains(service1));
		assertTrue(list.contains(service2));
	}

	@Test
	void b09_getParams_set() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass6.class).getPublicConstructor(x -> x.hasParameterTypes(Set.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Set);
		var set = (Set<TestService>) params[0];
		assertEquals(2, set.size());
		assertTrue(set.contains(service1));
		assertTrue(set.contains(service2));
	}

	@Test
	void b10_getParams_map() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass7.class).getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Map);
		var map = (Map<String, TestService>) params[0];
		assertEquals(2, map.size());
		assertSame(service1, map.get("service1"));
		assertSame(service2, map.get("service2"));
	}

	@Test
	void b11_getParams_mapWithUnnamedBean() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1); // Unnamed
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass7.class).getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Map);
		var map = (Map<String, TestService>) params[0];
		assertEquals(2, map.size());
		assertSame(service1, map.get("")); // Unnamed beans use empty string as key
		assertSame(service2, map.get("service2"));
	}

	@Test
	void b12_getParams_mixedTypes() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		var another = new AnotherService(42);
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		beanStore.addBean(AnotherService.class, another);
		var constructor = ClassInfo.of(TestClass9.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class, Optional.class, List.class)).get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(3, params.length);
		assertSame(service1, params[0]); // Single bean
		assertTrue(params[1] instanceof Optional); // Optional
		var opt = (Optional<AnotherService>) params[1];
		assertTrue(opt.isPresent());
		assertSame(another, opt.get());
		assertTrue(params[2] instanceof List); // List
		var list = (List<TestService>) params[2];
		assertEquals(2, list.size());
	}

	@Test
	void b13_getParams_innerClassOuter() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"));
		beanStore.addBean(OuterClass.class, new OuterClass()); // Add outer class bean for explicit parameter
		var outer = new OuterClass();
		// Use the class literal directly instead of getting it from an instance
		// For non-static inner classes, the constructor has 3 parameters: (implicit outer, explicit outer, TestService)
		// getParams only uses the bean for index 0, so index 1 will be resolved from bean store
		var classInfo = ClassInfo.of(OuterClass.InnerClass.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 3 && x.getParameter(2).isType(TestService.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found");
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, outer);
		assertEquals(3, params.length);
		assertSame(outer, params[0]); // Implicit outer instance (from bean parameter)
		assertNotNull(params[1]); // Explicit outer parameter (from bean store)
		assertNotNull(params[2]); // Service from bean store
	}

	//====================================================================================================
	// hasAllParams
	//====================================================================================================

	@Test
	void c01_hasAllParams_allAvailable() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"));
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(utils.hasAllParams(constructor, beanStore, null));
	}

	@Test
	void c02_hasAllParams_missing() throws Exception {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(utils.hasAllParams(constructor, beanStore, null));
	}

	@Test
	void c03_hasAllParams_optionalSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		assertTrue(utils.hasAllParams(constructor, beanStore, null)); // Optional is skipped
	}

	@Test
	void c04_hasAllParams_collectionsSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		assertTrue(utils.hasAllParams(constructor, beanStore, null)); // Collections are skipped
	}

	@Test
	void c05_hasAllParams_namedBeanFound() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"), "service1");
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(utils.hasAllParams(constructor, beanStore, null));
	}

	@Test
	void c06_hasAllParams_namedBeanMissing() throws Exception {
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(utils.hasAllParams(constructor, beanStore, null));
	}

	//====================================================================================================
	// invoke
	//====================================================================================================

	@Test
	void d01_invoke_constructor() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = utils.invoke(constructor, beanStore, null);
		assertNotNull(result);
		assertTrue(result instanceof TestClass1);
	}

	@Test
	void d02_invoke_method() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var instance = new TestMethodClass();
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		utils.invoke(method, beanStore, instance);
		// Method should execute without exception
	}

	@Test
	void d03_invoke_staticMethod() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("staticMethod") && x.hasParameterTypes(TestService.class)).get();
		utils.invoke(method, beanStore, null); // null for static methods
		// Method should execute without exception
	}

	@Test
	void d04_invoke_constructorWithCollections() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		var result = utils.invoke(constructor, beanStore, null);
		assertNotNull(result);
		assertTrue(result instanceof TestClass5);
	}

	@Test
	void d05_invoke_innerClassConstructor() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"));
		beanStore.addBean(OuterClass.class, new OuterClass()); // Add outer class bean for explicit parameter
		var outer = new OuterClass();
		// Use the class literal directly instead of getting it from an instance
		// For non-static inner classes, the constructor has 3 parameters: (implicit outer, explicit outer, TestService)
		var classInfo = ClassInfo.of(OuterClass.InnerClass.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 3 && x.getParameter(2).isType(TestService.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found");
		var constructor = constructorOpt.get();
		constructor.accessible(); // Make constructor accessible
		var result = utils.invoke(constructor, beanStore, outer);
		assertNotNull(result);
		assertTrue(result.getClass().getName().contains("InnerClass"));
	}

	//====================================================================================================
	// Additional coverage tests
	//====================================================================================================

	// Test line 113: getMissingParams - when bean is null (covers nn(bean) == false branch)
	@Test
	void e01_getMissingParams_beanNull() throws Exception {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, null);
		assertEquals("TestService", result); // Should check bean store since bean is null
	}

	// Test line 113: getMissingParams - when first parameter doesn't match bean type
	@Test
	void e02_getMissingParams_firstParamDoesNotMatchBean() throws Exception {
		var wrongBean = new AnotherService(42);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, wrongBean);
		assertEquals("TestService", result); // Should check bean store since types don't match
	}

	// Test line 173: getParams - Optional collection type
	@Test
	void e03_getParams_optionalCollection() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		// Create a class with Optional<List<TestService>> parameter
		class TestClassWithOptionalList {
			@SuppressWarnings("unused")
			public TestClassWithOptionalList(Optional<List<TestService>> services) {}
		}
		// Find constructor by checking parameter count and that second param (index 1) is Optional
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithOptionalList.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().is(Optional.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Optional parameter
		assertTrue(params[1] instanceof Optional);
		var opt = (Optional<List<TestService>>) params[1];
		assertTrue(opt.isPresent());
		var list = opt.get();
		assertEquals(2, list.size());
	}

	// Test line 247: hasAllParams - when bean is null
	@Test
	void e04_hasAllParams_beanNull() throws Exception {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(utils.hasAllParams(constructor, beanStore, null)); // Should check bean store
	}

	// Test line 247: hasAllParams - when first parameter doesn't match bean type
	@Test
	void e05_hasAllParams_firstParamDoesNotMatchBean() throws Exception {
		var wrongBean = new AnotherService(42);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(utils.hasAllParams(constructor, beanStore, wrongBean)); // Should check bean store
	}

	// Test line 247: hasAllParams - when first parameter matches bean (covers continue loop)
	@Test
	void e07_hasAllParams_firstParamMatchesBean() throws Exception {
		var bean = new TestService("test1");
		// Constructor with TestService as first parameter - should use bean, not check store
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(utils.hasAllParams(constructor, beanStore, bean)); // First param satisfied by bean, no store check needed
	}

	// Test line 247: hasAllParams - when first parameter matches bean but second parameter is missing
	@Test
	void e08_hasAllParams_firstParamMatchesBeanButSecondMissing() throws Exception {
		var bean = new TestService("test1");
		// Constructor with TestService (first) and AnotherService (second) - first satisfied by bean, second missing
		var constructor = ClassInfo.of(TestClass8.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class, AnotherService.class)).get();
		assertFalse(utils.hasAllParams(constructor, beanStore, bean)); // First param satisfied by bean, but second is missing
	}


	// Test line 113: getMissingParams - when i == 0 but bean doesn't match type (covers pt.isInstance(bean) == false when i == 0)
	@Test
	void e06_getMissingParams_firstParamWithWrongBeanType() throws Exception {
		var wrongBean = new AnotherService(42);
		// Use a constructor where first param is TestService but we pass AnotherService
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = utils.getMissingParams(constructor, beanStore, wrongBean);
		assertEquals("TestService", result); // Should check bean store since types don't match
	}

	// Test line 173: getParams - Optional collection (ensure the Optional.of branch is taken)
	@Test
	void e07_getParams_optionalArray() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		// Create a class with Optional<TestService[]> parameter
		class TestClassWithOptionalArray {
			@SuppressWarnings("unused")
			public TestClassWithOptionalArray(Optional<TestService[]> services) {}
		}
		// Find constructor by checking parameter count and that second param (index 1) is Optional
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithOptionalArray.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().is(Optional.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Optional parameter
		assertTrue(params[1] instanceof Optional);
		var opt = (Optional<TestService[]>) params[1];
		assertTrue(opt.isPresent());
		var array = opt.get();
		assertEquals(2, array.length);
	}

	// Test line 311, 313, 315: getElementType - List with raw type (no generic parameters)
	// This tests when parameterizedType is not a ParameterizedType, or when typeArgs is empty
	@Test
	void e08_getParams_rawList() throws Exception {
		// Raw List type (no generic parameters) - this will cause getElementType to return null
		// which means getCollectionValue will return null, so it will fall back to single bean lookup
		class TestClassWithRawList {
			@SuppressWarnings("unused")
			public TestClassWithRawList(@SuppressWarnings("rawtypes") List services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithRawList.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().inner().equals(List.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + List parameter
		// Since getElementType returns null for raw List, it won't be treated as a collection
		// and will try to look up a bean of type List, which won't be found
		assertNull(params[1]);
	}

	// Test line 322, 324, 327: getElementType - Map with wrong key type or non-Class value type
	@Test
	void e09_getParams_mapWithWrongKeyType() throws Exception {
		// Map with Integer key instead of String - should return null from getElementType
		class TestClassWithWrongMap {
			@SuppressWarnings("unused")
			public TestClassWithWrongMap(Map<Integer, TestService> services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithWrongMap.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().inner().equals(Map.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Map parameter
		// Since key type is not String, getElementType returns null
		assertNull(params[1]);
	}

	// Test line 322, 324, 327: getElementType - Map with non-Class value type
	@Test
	void e10_getParams_mapWithNonClassValueType() throws Exception {
		// Map with generic value type (not a Class) - should return null from getElementType
		class TestClassWithGenericMap {
			@SuppressWarnings("unused")
			public TestClassWithGenericMap(Map<String, List<TestService>> services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithGenericMap.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().inner().equals(Map.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Map parameter
		// Since value type is List<TestService> (ParameterizedType, not Class), getElementType returns null
		assertNull(params[1]);
	}

	// Test line 324: getElementType - Map with raw type (no generic parameters)
	@Test
	void e11_getParams_rawMap() throws Exception {
		// Raw Map type (no generic parameters) - this will cause getElementType to return null
		class TestClassWithRawMap {
			@SuppressWarnings("unused")
			public TestClassWithRawMap(@SuppressWarnings("rawtypes") Map services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithRawMap.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().inner().equals(Map.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Map parameter
		// Since getElementType returns null for raw Map, it won't be treated as a collection
		// and will try to look up a bean of type Map, which won't be found
		assertNull(params[1]);
	}

	// Test line 333: getElementType - return null for unsupported types
	@Test
	void e13_getParams_unsupportedCollectionType() throws Exception {
		// Use a type that's not List, Set, Map, or array - should return null
		// This tests the fallback return null at line 333
		class TestClassWithCollection {
			@SuppressWarnings("unused")
			public TestClassWithCollection(Collection<TestService> services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithCollection.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().inner().equals(java.util.Collection.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Collection parameter
		// Collection is not List/Set/Map, so isCollectionType returns false, so getCollectionValue returns null
		// Then it tries to look up a bean of type Collection, which won't be found
		assertNull(params[1]);
	}

	// Test line 324: getElementType - Optional<SomeClass> where SomeClass is not a ParameterizedType
	// This tests the case where we have Optional<List> (raw type) or Optional<SomeClass> (not a collection)
	// but pt is a collection type. This is a rare edge case.
	@Test
	void e09_getElementType_optionalWithNonParameterizedType() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"));
		// Create a class with Optional<List> (raw type, not ParameterizedType)
		class TestClassWithOptionalRawList {
			@SuppressWarnings({"unused", "rawtypes"})
			public TestClassWithOptionalRawList(Optional<List> services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithOptionalRawList.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2) // Outer class + Optional<List> parameter
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Optional<List> parameter
		// Optional<List> (raw) - after unwrapping Optional, we get List (raw)
		// isCollectionType(List.class) returns true, so getCollectionValue is called
		// getElementType is called with pt=List.class and parameterizedType=Optional<List> (raw)
		// When unwrapping Optional, typeArgs[0] is List (raw), which is a Class, not ParameterizedType
		// So line 324 is executed: return null
		// Then getCollectionValue returns null because elementType is null
		// Then it tries to look up a bean of type List, which won't be found
		// Since the parameter is Optional<List>, it wraps the result in Optional.empty()
		assertTrue(params[1] instanceof Optional);
		var opt = (Optional<?>) params[1];
		assertFalse(opt.isPresent()); // Should be Optional.empty()
	}

	// Test Optional<Map<String, TestService>> - covers Optional unwrapping in getCollectionValue (lines 394-403)
	// Note: Line 401 (return null when Optional<SomeClass> where SomeClass is not ParameterizedType) 
	// appears to be unreachable in practice because:
	// - If pt is Map.class (raw), getElementType returns null and we return early (line 383)
	// - If pt is Map<String,T> (parameterized), typeArgs[0] is a ParameterizedType, so line 399 executes, not 401
	// However, the code is kept as a defensive check. This test verifies the normal Optional<Map<String,T>> case.
	@Test
	void e10_getCollectionValue_optionalMap() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		class TestClassWithOptionalMap {
			@SuppressWarnings("unused")
			public TestClassWithOptionalMap(Optional<Map<String, TestService>> services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithOptionalMap.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2) // Outer class + Optional<Map<String, TestService>> parameter
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Optional<Map> parameter
		// This should work normally: Optional unwrapping happens, then Map is populated
		assertTrue(params[1] instanceof Optional);
		var opt = (Optional<Map<String, TestService>>) params[1];
		assertTrue(opt.isPresent());
		var map = opt.get();
		assertEquals(2, map.size());
		assertSame(service1, map.get("service1"));
		assertSame(service2, map.get("service2"));
	}

	// Test line 416: getCollectionValue - Map where parameterizedType is not ParameterizedType after Optional unwrapping
	// Note: Line 416 appears to be unreachable in practice because:
	// - If getElementType returns null (e.g., Map<Integer,T> or raw Map), we return early (line 383)
	// - If getElementType returns non-null (e.g., Map<String,T>), then parameterizedType is a ParameterizedType
	//   and typeArgs[0] is String.class, so line 413 executes, not 416
	// However, kept as a defensive check. This test verifies that Map<Integer,T> returns null early.
	@Test
	void e11_getCollectionValue_mapWithNonStringKey() throws Exception {
		var service1 = new TestService("test1");
		beanStore.addBean(TestService.class, service1);
		// Create a class with Map<Integer, TestService> - key is not String
		class TestClassWithWrongKeyMap {
			@SuppressWarnings("unused")
			public TestClassWithWrongKeyMap(Map<Integer, TestService> services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithWrongKeyMap.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2) // Outer class + Map<Integer, TestService> parameter
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Map parameter
		// Map<Integer, TestService> - getElementType returns null (key is not String)
		// So getCollectionValue returns null early (line 383), never reaching line 416
		assertNull(params[1]);
	}

	// Test line 437: getCollectionValue - fallback return null
	// Note: Line 437 appears to be unreachable in practice because:
	// - isCollectionType only returns true for List, Set, Map, or array
	// - List and Set are handled above (lines 430-434)
	// - Map is handled earlier (lines 390-416)
	// - Arrays are handled earlier (lines 422-426)
	// - If inner() is null, isCollectionType returns false, so we return early (line 379)
	// However, kept as defensive code. This test verifies that Collection types return null early.
	@Test
	void e12_getCollectionValue_unsupportedCollectionType() throws Exception {
		var service1 = new TestService("test1");
		beanStore.addBean(TestService.class, service1);
		// Collection is not List/Set/Map, so isCollectionType returns false
		// So getCollectionValue returns null early (line 379), never reaching line 437
		// Line 437 appears to be unreachable defensive code
		// This test verifies that Collection types are not handled (they return null early)
		class TestClassWithCollection {
			@SuppressWarnings("unused")
			public TestClassWithCollection(Collection<TestService> services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithCollection.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().inner().equals(java.util.Collection.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = utils.getParams(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Collection parameter
		// Collection is not List/Set/Map, so isCollectionType returns false, so getCollectionValue returns null early
		// Then it tries to look up a bean of type Collection, which won't be found
		assertNull(params[1]);
	}
}

