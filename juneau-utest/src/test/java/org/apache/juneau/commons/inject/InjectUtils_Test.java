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
package org.apache.juneau.commons.inject;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.commons.inject.InjectUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.Named;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

class InjectUtils_Test extends TestBase {

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

	private BasicBeanStore2 beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore2(null);
	}

	//====================================================================================================
	// getMissingParams
	//====================================================================================================

	@Test
	void a01_getMissingParams_allAvailable() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"));
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertNull(result);
	}

	@Test
	void a02_getMissingParams_singleMissing() throws Exception {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertEquals("TestService", result);
	}

	@Test
	void a03_getMissingParams_namedBeanFound() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"), "service1");
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertNull(result);
	}

	@Test
	void a04_getMissingParams_namedBeanMissing() throws Exception {
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertEquals("TestService@service1", result);
	}

	@Test
	void a05_getMissingParams_optionalSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertNull(result); // Optional parameters are skipped
	}

	@Test
	void a06_getMissingParams_arraySkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass4.class).getPublicConstructor(x -> x.hasParameterTypes(TestService[].class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertNull(result); // Arrays are skipped
	}

	@Test
	void a07_getMissingParams_listSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertNull(result); // Lists are skipped
	}

	@Test
	void a08_getMissingParams_setSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass6.class).getPublicConstructor(x -> x.hasParameterTypes(Set.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertNull(result); // Sets are skipped
	}

	@Test
	void a09_getMissingParams_mapSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass7.class).getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
		assertNull(result); // Maps are skipped
	}

	@Test
	void a10_getMissingParams_multipleMissing() throws Exception {
		var constructor = ClassInfo.of(TestClass8.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class, AnotherService.class)).get();
		var result = getMissingParameters(constructor, beanStore, null);
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
		var result = getMissingParameters(constructor, beanStore, outer);
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
		var params = getParameters(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertSame(service, params[0]);
	}

	@Test
	void b02_getParams_singleBeanNotFound() throws Exception {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertSame(service1, params[0]);
	}

	@Test
	void b04_getParams_optionalBeanFound() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var params = getParameters(constructor, beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Optional);
		var opt = (Optional<TestService>) params[0];
		assertTrue(opt.isPresent());
		assertSame(service, opt.get());
	}

	@Test
	void b05_getParams_optionalBeanNotFound() throws Exception {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, outer);
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
		assertTrue(hasAllParameters(constructor, beanStore, null));
	}

	@Test
	void c02_hasAllParams_missing() throws Exception {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(hasAllParameters(constructor, beanStore, null));
	}

	@Test
	void c03_hasAllParams_optionalSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		assertTrue(hasAllParameters(constructor, beanStore, null)); // Optional is skipped
	}

	@Test
	void c04_hasAllParams_collectionsSkipped() throws Exception {
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		assertTrue(hasAllParameters(constructor, beanStore, null)); // Collections are skipped
	}

	@Test
	void c05_hasAllParams_namedBeanFound() throws Exception {
		beanStore.addBean(TestService.class, new TestService("test1"), "service1");
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(hasAllParameters(constructor, beanStore, null));
	}

	@Test
	void c06_hasAllParams_namedBeanMissing() throws Exception {
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(hasAllParameters(constructor, beanStore, null));
	}

	//====================================================================================================
	// invoke
	//====================================================================================================

	@Test
	void d01_invoke_constructor() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = invoke(constructor, beanStore, null);
		assertNotNull(result);
		assertTrue(result instanceof TestClass1);
	}

	@Test
	void d02_invoke_method() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var instance = new TestMethodClass();
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		invoke(method, beanStore, instance);
		// Method should execute without exception
	}

	@Test
	void d03_invoke_staticMethod() throws Exception {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("staticMethod") && x.hasParameterTypes(TestService.class)).get();
		invoke(method, beanStore, null); // null for static methods
		// Method should execute without exception
	}

	@Test
	void d04_invoke_constructorWithCollections() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		var result = invoke(constructor, beanStore, null);
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
		var result = invoke(constructor, beanStore, outer);
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
		var result = getMissingParameters(constructor, beanStore, null);
		assertEquals("TestService", result); // Should check bean store since bean is null
	}

	// Test line 113: getMissingParams - when first parameter doesn't match bean type
	@Test
	void e02_getMissingParams_firstParamDoesNotMatchBean() throws Exception {
		var wrongBean = new AnotherService(42);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = getMissingParameters(constructor, beanStore, wrongBean);
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
		var params = getParameters(constructor, beanStore, null);
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
		assertFalse(hasAllParameters(constructor, beanStore, null)); // Should check bean store
	}

	// Test line 247: hasAllParams - when first parameter doesn't match bean type
	@Test
	void e05_hasAllParams_firstParamDoesNotMatchBean() throws Exception {
		var wrongBean = new AnotherService(42);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(hasAllParameters(constructor, beanStore, wrongBean)); // Should check bean store
	}

	// Test line 247: hasAllParams - when first parameter matches bean (covers continue loop)
	@Test
	void e07_hasAllParams_firstParamMatchesBean() throws Exception {
		var bean = new TestService("test1");
		// Constructor with TestService as first parameter - should use bean, not check store
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(hasAllParameters(constructor, beanStore, bean)); // First param satisfied by bean, no store check needed
	}

	// Test line 247: hasAllParams - when first parameter matches bean but second parameter is missing
	@Test
	void e08_hasAllParams_firstParamMatchesBeanButSecondMissing() throws Exception {
		var bean = new TestService("test1");
		// Constructor with TestService (first) and AnotherService (second) - first satisfied by bean, second missing
		var constructor = ClassInfo.of(TestClass8.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class, AnotherService.class)).get();
		assertFalse(hasAllParameters(constructor, beanStore, bean)); // First param satisfied by bean, but second is missing
	}


	// Test line 113: getMissingParams - when i == 0 but bean doesn't match type (covers pt.isInstance(bean) == false when i == 0)
	@Test
	void e06_getMissingParams_firstParamWithWrongBeanType() throws Exception {
		var wrongBean = new AnotherService(42);
		// Use a constructor where first param is TestService but we pass AnotherService
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = getMissingParameters(constructor, beanStore, wrongBean);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
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
		var params = getParameters(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Collection parameter
		// Collection is not List/Set/Map, so isCollectionType returns false, so getCollectionValue returns null early
		// Then it tries to look up a bean of type Collection, which won't be found
		assertNull(params[1]);
	}

	//====================================================================================================
	// injectBeans
	//====================================================================================================

	// Mock annotations for testing (matched by simple class name)
	@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
	@java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD})
	@interface Inject {}

	@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
	@java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD})
	@interface Autowired {}

	// Test classes for field injection
	static class TestFieldInjection {
		@Inject
		TestService service;
		@Autowired
		TestService service2;
		@Inject
		Optional<TestService> optionalService;
		@Inject
		List<TestService> serviceList;
		@Inject
		Set<TestService> serviceSet;
		@Inject
		Map<String, TestService> serviceMap;
		@Inject
		TestService[] serviceArray;
		@Inject
		@org.apache.juneau.annotation.Named("service1")
		TestService namedService;
		@Inject
		AnotherService anotherService;
		// Final field - should be skipped
		@Inject
		final TestService finalService = null;
		// Field without annotation - should be skipped
		TestService unannotatedService;
	}

	// Test class for method injection
	static class TestMethodInjection {
		boolean method1Called = false;
		TestService injectedService1;
		boolean method2Called = false;
		TestService injectedService2;
		boolean method3Called = false;
		boolean method4Called = false;
		boolean method5Called = false;
		boolean method6Called = false;
		boolean method7Called = false;
		boolean method8Called = false;
		boolean method9Called = false;
		boolean method10Called = false;
		boolean method11Called = false;
		boolean method12Called = false;
		boolean method13Called = false;

		@Inject
		void method1(TestService service) {
			method1Called = true;
			injectedService1 = service;
		}

		@Autowired
		void method2(TestService service) {
			method2Called = true;
			injectedService2 = service;
		}

		@Inject
		void method3(Optional<TestService> service) {
			method3Called = true;
		}

		@Inject
		void method4(List<TestService> services) {
			method4Called = true;
		}

		@Inject
		void method5(Set<TestService> services) {
			method5Called = true;
		}

		@Inject
		void method6(Map<String, TestService> services) {
			method6Called = true;
		}

		@Inject
		void method7(TestService[] services) {
			method7Called = true;
		}

		@Inject
		void method8(@org.apache.juneau.annotation.Named("service1") TestService service) {
			method8Called = true;
		}

		@Inject
		void method9() {
			method9Called = true; // Zero parameters
		}

		@Inject
		String method10(TestService service) {
			method10Called = true;
			return "result"; // Return value should be ignored
		}

		// Method with type parameters - should be skipped
		@Inject
		@SuppressWarnings("unused")
		<T> void methodWithTypeParams(TestService service) {
			method11Called = true;
		}

		// Method without annotation - should be skipped
		void unannotatedMethod(TestService service) {
			method12Called = true;
		}

		@Inject
		static void staticMethod(TestService service) {
			// Static method - can't access instance fields, so we just verify it executes without error
		}
	}

	// Abstract test class for testing abstract method skipping
	static abstract class TestAbstractMethodInjection {
		@Inject
		abstract void abstractMethod(TestService service);
	}

	@Test
	void f01_injectBeans_fieldSingleBean() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertSame(service, bean.service);
	}

	@Test
	void f02_injectBeans_fieldWithAutowired() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertSame(service, bean.service2);
	}

	@Test
	void f03_injectBeans_fieldOptionalFound() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.optionalService.isPresent());
		assertSame(service, bean.optionalService.get());
	}

	@Test
	void f04_injectBeans_fieldOptionalNotFound() {
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertFalse(bean.optionalService.isPresent());
	}

	@Test
	void f05_injectBeans_fieldList() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertNotNull(bean.serviceList);
		assertEquals(2, bean.serviceList.size());
		assertTrue(bean.serviceList.contains(service1));
		assertTrue(bean.serviceList.contains(service2));
	}

	@Test
	void f06_injectBeans_fieldSet() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertNotNull(bean.serviceSet);
		assertEquals(2, bean.serviceSet.size());
		assertTrue(bean.serviceSet.contains(service1));
		assertTrue(bean.serviceSet.contains(service2));
	}

	@Test
	void f07_injectBeans_fieldMap() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertNotNull(bean.serviceMap);
		assertEquals(2, bean.serviceMap.size());
		assertSame(service1, bean.serviceMap.get("service1"));
		assertSame(service2, bean.serviceMap.get("service2"));
	}

	@Test
	void f08_injectBeans_fieldArray() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertNotNull(bean.serviceArray);
		assertEquals(2, bean.serviceArray.length);
		assertTrue(bean.serviceArray[0].equals(service1) || bean.serviceArray[0].equals(service2));
		assertTrue(bean.serviceArray[1].equals(service1) || bean.serviceArray[1].equals(service2));
	}

	@Test
	void f09_injectBeans_fieldNamedBean() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertSame(service1, bean.namedService);
	}

	@Test
	void f10_injectBeans_fieldFinalSkipped() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		// Final field should not be injected (remains null)
		assertNull(bean.finalService);
	}

	@Test
	void f11_injectBeans_fieldUnannotatedSkipped() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		// Unannotated field should not be injected
		assertNull(bean.unannotatedService);
	}

	@Test
	void f12_injectBeans_fieldNotFound() {
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		// Field should remain null if bean not found
		assertNull(bean.service);
	}

	@Test
	void f13_injectBeans_fieldMultipleTypes() {
		var service = new TestService("test1");
		var another = new AnotherService(42);
		beanStore.addBean(TestService.class, service);
		beanStore.addBean(AnotherService.class, another);
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		assertSame(service, bean.service);
		assertSame(another, bean.anotherService);
	}

	@Test
	void f14_injectBeans_methodSingleBean() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method1Called);
		assertSame(service, bean.injectedService1);
	}

	@Test
	void f15_injectBeans_methodWithAutowired() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method2Called);
		assertSame(service, bean.injectedService2);
	}

	@Test
	void f16_injectBeans_methodOptional() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method3Called);
	}

	@Test
	void f17_injectBeans_methodList() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method4Called);
	}

	@Test
	void f18_injectBeans_methodSet() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method5Called);
	}

	@Test
	void f19_injectBeans_methodMap() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method6Called);
	}

	@Test
	void f20_injectBeans_methodArray() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method7Called);
	}

	@Test
	void f21_injectBeans_methodNamedBean() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method8Called);
	}

	@Test
	void f22_injectBeans_methodZeroParameters() {
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method9Called);
	}

	@Test
	void f23_injectBeans_methodReturnValueIgnored() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		assertTrue(bean.method10Called);
		// Return value is ignored, method just needs to execute
	}

	@Test
	void f24_injectBeans_methodAbstractSkipped() {
		// Abstract methods should be skipped
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		// Create a concrete subclass of the abstract class
		class ConcreteMethodInjection extends TestAbstractMethodInjection {
			@Override
			void abstractMethod(TestService service) {
				// Implementation
			}
		}
		var bean = new ConcreteMethodInjection();
		injectBeans(bean, beanStore);
		// Abstract method should be skipped (not called)
		// Should complete without error
	}

	@Test
	void f25_injectBeans_methodWithTypeParamsSkipped() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		// Method with type parameters should be skipped
		assertFalse(bean.method11Called);
	}

	@Test
	void f26_injectBeans_methodUnannotatedSkipped() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestMethodInjection();
		injectBeans(bean, beanStore);
		// Unannotated method should be skipped
		assertFalse(bean.method12Called);
	}

	@Test
	void f27_injectBeans_methodStatic() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestMethodInjection();
		// Static methods should be called (with null instance)
		// Just verify no exception is thrown
		injectBeans(bean, beanStore);
		// Method should execute without error (can't check instance field from static method)
	}

	@Test
	void f28_injectBeans_returnsSameInstance() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var bean = new TestFieldInjection();
		var result = injectBeans(bean, beanStore);
		// Should return the same instance for method chaining
		assertSame(bean, result);
	}

	@Test
	void f29_injectBeans_fieldAndMethodTogether() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		// Add a method to TestFieldInjection for this test
		class TestCombined {
			@Inject
			TestService field;
			@Inject
			void method(TestService service) {
				// Method should be called
			}
		}
		var combined = new TestCombined();
		injectBeans(combined, beanStore);
		assertSame(service, combined.field);
		// Method should have been called (no exception means it worked)
	}

	@Test
	void f30_injectBeans_fieldListEmpty() {
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		// Empty list should be created
		assertNotNull(bean.serviceList);
		assertTrue(bean.serviceList.isEmpty());
	}

	@Test
	void f31_injectBeans_fieldSetEmpty() {
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		// Empty set should be created
		assertNotNull(bean.serviceSet);
		assertTrue(bean.serviceSet.isEmpty());
	}

	@Test
	void f32_injectBeans_fieldMapEmpty() {
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		// Empty map should be created
		assertNotNull(bean.serviceMap);
		assertTrue(bean.serviceMap.isEmpty());
	}

	@Test
	void f33_injectBeans_fieldArrayEmpty() {
		var bean = new TestFieldInjection();
		injectBeans(bean, beanStore);
		// Empty array should be created
		assertNotNull(bean.serviceArray);
		assertEquals(0, bean.serviceArray.length);
	}

	// Test lines 436-442: getCollectionValue - when pi.getParameterizedType() is not ParameterizedType
	// but pt.innerType() is ParameterizedType (fallback case)
	// This tests the else-if branch where we use pt.innerType() as the parameterizedType
	@Test
	void f34_getCollectionValue_parameterizedTypeFallback() throws Exception {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		// Create a class with Map<String, TestService> parameter
		// The parameterized type should be available from pt.innerType() even if not from pi.getParameterizedType()
		class TestClassWithMapFallback {
			@SuppressWarnings("unused")
			public TestClassWithMapFallback(Map<String, TestService> services) {}
		}
		// Local classes in instance methods are non-static inner classes, so they have outer class as first param
		var classInfo = ClassInfo.of(TestClassWithMapFallback.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 2 && x.getParameter(1).getParameterType().inner().equals(Map.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available: " + constructors);
		var constructor = constructorOpt.get();
		var params = getParameters(constructor, beanStore, null);
		assertEquals(2, params.length); // Outer class + Map parameter
		// This should work: pt.innerType() provides the ParameterizedType even if pi.getParameterizedType() doesn't
		assertTrue(params[1] instanceof Map);
		var map = (Map<String, TestService>) params[1];
		assertEquals(2, map.size());
		assertSame(service1, map.get("service1"));
		assertSame(service2, map.get("service2"));
	}
}

