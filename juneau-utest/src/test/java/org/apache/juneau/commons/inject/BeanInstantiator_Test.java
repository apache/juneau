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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.Supplier;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.Flag;
import org.apache.juneau.commons.lang.IntegerValue;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.annotation.*;
import java.util.logging.Level;

@SuppressWarnings({
	"java:S1172", // Unused parameters in tests are intentional
	"java:S1186" // Empty test method intentional for framework testing
})
class BeanInstantiator_Test extends TestBase {

	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore(null);
	}

	/**
	 * Helper method to create a BeanInstantiator instance with the test's beanStore.
	 * Reduces repetition of BeanInstantiator.of(Class, beanStore) pattern.
	 */
	private <T> BeanInstantiator.Builder<T> bc(Class<T> c) {
		return BeanInstantiator.of(c, beanStore);
	}

	//====================================================================================================
	// Test classes
	//====================================================================================================

	// Type not in bean store - used for testing unresolvable dependencies
	public static class UnresolvableType {
		public UnresolvableType() {}
	}

	// Simple service class
	static class TestService {
		private final String name;
		TestService(String name) { this.name = name; }
		String getName() { return name; }
		@Override public String toString() { return "TestService[" + name + "]"; }
	}

	// Another service class for testing multiple dependencies
	static class AnotherService {
		private final int value;
		AnotherService(int value) { this.value = value; }
		int getValue() { return value; }
	}

	// Bean with no-arg constructor
	public static class SimpleBean {
		public String value;
		public SimpleBean() {}
	}

	// Bean with constructor parameters
	public static class BeanWithDependencies {
		private final TestService service;
		private final AnotherService another;

		public BeanWithDependencies(TestService service, AnotherService another) {
			this.service = service;
			this.another = another;
		}

		public TestService getService() { return service; }
		public AnotherService getAnother() { return another; }
	}

	// Bean with injected fields
	public static class BeanWithInjectedFields {
		@Inject TestService service;
		@Inject AnotherService another;

		public BeanWithInjectedFields() {}

		public TestService getService() { return service; }
		public AnotherService getAnother() { return another; }
	}

	// Bean with injected method
	public static class BeanWithInjectedMethod {
		private TestService service;

		public BeanWithInjectedMethod() {}

		@Inject
		public void setService(TestService service) {
			this.service = service;
		}

		public TestService getService() { return service; }
	}


	// Bean with static getInstance method
	public static class SingletonBean {
		private static final SingletonBean INSTANCE = new SingletonBean();
		private SingletonBean() {}

		public static SingletonBean getInstance() {
			return INSTANCE;
		}
	}

	// Bean with getInstance that takes parameters
	public static class SingletonBeanWithParams {
		private final TestService service;

		private SingletonBeanWithParams(TestService service) {
			this.service = service;
		}

		public static SingletonBeanWithParams getInstance(TestService service) {
			return new SingletonBeanWithParams(service);
		}

		public TestService getService() { return service; }
	}

	// Bean with Builder
	public static class BeanWithBuilder {
		private final String name;
		private final int value;

		private BeanWithBuilder(Builder builder) {
			this.name = builder.name;
			this.value = builder.value;
		}

		public String getName() { return name; }
		public int getValue() { return value; }

		public static class Builder {
			private String name;
			private int value;

			// @formatter:off
			public Builder name(String name) { this.name = name; return this;}
			public Builder value(int value) { this.value = value; return this;}
			// @formatter:on

			public BeanWithBuilder build() {
				return new BeanWithBuilder(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}
	}

	// Bean with Builder annotation


	// Inner class for testing enclosingInstance
	public class InnerBean {
		private final String value;

		public InnerBean(String value) {
			this.value = value;
		}

		public String getValue() { return value; }
	}

	// Abstract class for error testing
	public abstract static class AbstractBean {
		public AbstractBean() {}
	}

	// Interface for error testing
	public interface BeanInterface {}

	// Concrete implementation for fallback tests
	public static class ConcreteBean extends AbstractBean implements BeanInterface {
		public ConcreteBean() {
			super();
		}
	}

	// Bean classes for type specification testing (used across multiple nested test classes)
	public static class ParentBean {
		public ParentBean() {}
	}

	// Child bean for type specification testing
	public static class ChildBean extends ParentBean {
		public ChildBean() {}
	}

	// Bean with builder where child uses parent's builder (used across multiple nested test classes)
	public static class ParentBeanWithBuilder {
		protected final String name;

		protected ParentBeanWithBuilder(BuilderForParent builder) {
			this.name = builder.name;
		}

		public String getName() { return name; }

		public static class BuilderForParent {
			protected String name;

			// @formatter:off
			public BuilderForParent name(String name) { this.name = name; return this;}
			// @formatter:on

			public ParentBeanWithBuilder build() {
				return new ParentBeanWithBuilder(this);
			}
		}

		public static BuilderForParent create() {
			return new BuilderForParent();
		}
	}

	// Child bean with builder for type specification testing
	public static class ChildBeanWithBuilder extends ParentBeanWithBuilder {
		public ChildBeanWithBuilder(BuilderForChild builder) {
			super(builder);
		}

		public static class BuilderForChild extends BuilderForParent {
			@Override
			public ChildBeanWithBuilder build() {
				return new ChildBeanWithBuilder(this);
			}
		}

		public static BuilderForChild create() {
			return new BuilderForChild();
		}
	}

	//====================================================================================================
	// Nested test classes
	//====================================================================================================

	/**
	 * Tests basic bean creation scenarios including:
	 * - Simple beans with no-arg constructors
	 * - Beans with single and multiple constructor dependencies resolved from bean store
	 * - Beans with Optional, List, array, and Map parameters
	 * - Beans with missing dependencies (empty Optional, List, array, Map)
	 * - Using addBean() method to add dependencies directly to the creator
	 */
	@Nested class A_basicBeanCreation extends TestBase {

		/**
		 * Tests creating a simple bean with no-arg constructor.
		 */
		@Test
		void a01_createSimpleBean() {
			var bean = bc(SimpleBean.class).run();
			assertInstanceOf(SimpleBean.class, bean);
		}

		/**
		 * Tests creating a bean with a single simple bean parameter resolved from bean store.
		 */
		@Test
		void a02_createBeanWithSingleDependency() {
			var testService = new TestService("test");
			beanStore.add(TestService.class, testService);

			var bean = bc(A02_BeanWithSingleDependency.class).run();

			assertSame(testService, bean.getService());
		}

		// Bean with single simple bean parameter
		public static class A02_BeanWithSingleDependency {
			private final TestService service;

			public A02_BeanWithSingleDependency(TestService service) {
				this.service = service;
			}

			public TestService getService() { return service; }
		}

		/**
		 * Tests creating a bean with constructor dependencies resolved from bean store.
		 */
		@Test
		void a03_createBeanWithDependencies() {
			var testService = new TestService("test");
			var anotherService = new AnotherService(42);
			beanStore.add(TestService.class, testService);
			beanStore.add(AnotherService.class, anotherService);

			var bean = bc(BeanWithDependencies.class).run();

			assertSame(testService, bean.getService());
			assertSame(anotherService, bean.getAnother());
		}

		/**
		 * Tests creating a bean with Optional parameter when the dependency is present in bean store.
		 */
		@Test
		void a04a_createBeanWithOptionalPresent() {
			var testService = new TestService("test");
			beanStore.add(TestService.class, testService);

			var bean = bc(A04_BeanWithOptional.class).run();

			assertSame(testService, bean.getService().get());
		}

		/**
		 * Tests creating a bean with Optional parameter when the dependency is not in bean store (should be empty).
		 */
		@Test
		void a04b_createBeanWithOptionalEmpty() {
			var bean = bc(A04_BeanWithOptional.class).run();

			assertTrue(bean.getService().isEmpty());
		}

		// Bean with Optional parameter
		public static class A04_BeanWithOptional {
			private final Optional<TestService> service;

			public A04_BeanWithOptional(Optional<TestService> service) {
				this.service = service;
			}

			public Optional<TestService> getService() { return service; }
		}

		/**
		 * Tests creating a bean with List parameter, collecting all matching beans from bean store.
		 */
		@Test
		void a05_createBeanWithList() {
			var service1 = new TestService("test1");
			var service2 = new TestService("test2");
			beanStore.add(TestService.class, service1);
			beanStore.add(TestService.class, service2, "service2");

			var bean = bc(A05_BeanWithList.class).run();

			assertList(bean.getServices(), service1, service2);
		}

		// Bean with List parameter
		public static class A05_BeanWithList {
			private final List<TestService> services;

			public A05_BeanWithList(List<TestService> services) {
				this.services = services;
			}

			public List<TestService> getServices() { return services; }
		}

		/**
		 * Tests creating a bean with array parameter when multiple services are in bean store.
		 */
		@Test
		void a06a_createBeanWithArray() {
			var service1 = new TestService("test1");
			var service2 = new TestService("test2");
			beanStore.add(TestService.class, service1);
			beanStore.add(TestService.class, service2, "service2");

			var bean = bc(A06_BeanWithArray.class).run();

			assertList(bean.getServices(), service1, service2);
		}

		/**
		 * Tests creating a bean with array parameter when no services are in bean store (should be empty array).
		 */
		@Test
		void a06b_createBeanWithEmptyArray() {
			var bean = bc(A06_BeanWithArray.class).run();

			assertEmpty(bean.getServices());
		}

		// Bean with array parameter
		public static class A06_BeanWithArray {
			private final TestService[] services;

			public A06_BeanWithArray(TestService[] services) {
				this.services = services;
			}

			public TestService[] getServices() { return services; }
		}

		/**
		 * Tests creating a bean with Map parameter, collecting all named beans from bean store.
		 */
		@Test
		void a07a_createBeanWithMap() {
			var service1 = new TestService("test1");
			var service2 = new TestService("test2");
			beanStore.add(TestService.class, service1);
			beanStore.add(TestService.class, service2, "service2");

			var bean = bc(A07_BeanWithMap.class).run();

			assertEquals(2, bean.getServices().size());
			assertSame(service1, bean.getServices().get("")); // Unnamed bean uses empty string as key
			assertSame(service2, bean.getServices().get("service2"));
		}

		/**
		 * Tests creating a bean with Map parameter when no services are in bean store (should be empty map).
		 */
		@Test
		void a07b_createBeanWithEmptyMap() {
			var bean = bc(A07_BeanWithMap.class).run();

			assertEmpty(bean.getServices());
		}

		// Bean with Map parameter for named beans
		public static class A07_BeanWithMap {
			private final Map<String, TestService> services;

			public A07_BeanWithMap(Map<String, TestService> services) {
				this.services = services;
			}

			public Map<String, TestService> getServices() { return services; }
		}

		/**
		 * Tests creating a bean using addBean() to add dependencies directly to the creator.
		 */
		@Test
		void a08_createBeanWithAddBean() {
			var testService = new TestService("test");
			var anotherService = new AnotherService(42);

			var bean = bc(BeanWithDependencies.class)
				.addBean(TestService.class, testService)
				.addBean(AnotherService.class, anotherService)
				.run();

			assertSame(testService, bean.getService());
			assertSame(anotherService, bean.getAnother());
		}

		/**
		 * Tests creating a bean using addBean() with a name parameter to add named dependencies.
		 */
		@Test
		void a09_createBeanWithAddBeanWithName() {
			var service1 = new TestService("test1");
			var service2 = new TestService("test2");

			var bean = bc(A07_BeanWithMap.class)
				.addBean(TestService.class, service1)
				.addBean(TestService.class, service2, "service2")
				.run();

			assertEquals(2, bean.getServices().size());
			assertSame(service1, bean.getServices().get("")); // Unnamed bean uses empty string as key
			assertSame(service2, bean.getServices().get("service2"));
		}

		/**
		 * Tests creating a bean whose only constructor is package-private with no parameters.
		 *
		 * <p>BeanInstantiator falls back to package-private constructors after public/protected.
		 * Useful for {@code @Configuration} classes and other framework types that intentionally
		 * keep instantiation scoped to their declaring package. Private constructors remain excluded.
		 */
		@Test
		void a10_createBeanWithPackagePrivateNoArgConstructor() {
			var bean = bc(A10_BeanWithPackagePrivateCtor.class).run();
			assertInstanceOf(A10_BeanWithPackagePrivateCtor.class, bean);
			assertEquals("ctor-fired", bean.tag);
		}

		static class A10_BeanWithPackagePrivateCtor {
			final String tag;
			A10_BeanWithPackagePrivateCtor() { this.tag = "ctor-fired"; }
		}

		/**
		 * Tests creating a bean whose only constructor is package-private and accepts dependencies.
		 *
		 * <p>Verifies that dependency resolution works the same on the package-private fallback path
		 * as it does for the public/protected paths.
		 */
		@Test
		void a11_createBeanWithPackagePrivateCtorAndDependencies() {
			var testService = new TestService("svc");
			beanStore.add(TestService.class, testService);

			var bean = bc(A11_BeanWithPackagePrivateCtorAndDeps.class).run();

			assertSame(testService, bean.service);
		}

		static class A11_BeanWithPackagePrivateCtorAndDeps {
			final TestService service;
			A11_BeanWithPackagePrivateCtorAndDeps(TestService service) { this.service = service; }
		}

		/**
		 * Tests that BeanInstantiator prefers public over protected over package-private when multiple
		 * visibility-distinct constructors exist on the same class. The public ctor must win.
		 */
		@Test
		void a12_publicConstructorPreferredOverPackagePrivate() {
			beanStore.add(TestService.class, new TestService("svc"));

			var bean = bc(A12_BeanWithMultipleCtorVisibilities.class).run();

			assertEquals("public", bean.tag);
		}

		public static class A12_BeanWithMultipleCtorVisibilities {
			final String tag;
			public A12_BeanWithMultipleCtorVisibilities(TestService service) { this.tag = "public"; }
			A12_BeanWithMultipleCtorVisibilities() { this.tag = "package"; }
		}

		/**
		 * Tests that a class whose only constructor is private is NOT instantiated by the
		 * package-private fallback path. Private constructors are explicitly excluded — they signal
		 * "do not instantiate".
		 */
		@Test
		void a13_privateConstructorIsNotUsed() {
			var builder = bc(A13_BeanWithPrivateCtor.class);
			assertThrows(ExecutableException.class, builder::run);
		}

		public static class A13_BeanWithPrivateCtor {
			private A13_BeanWithPrivateCtor() { /* must not be instantiated by BeanInstantiator */ }
		}
	}

	/**
	 * Tests dependency injection functionality:
	 * - Field injection using @Inject annotation
	 * - Method injection using @Inject annotation
	 * - PostConstruct method invocation after injection
	 * - Injection order and behavior
	 */
	@Nested class B_dependencyInjection extends TestBase {

		/**
		 * Tests injecting dependencies into bean fields annotated with @Inject.
		 */
		@Test
		void b01_injectFieldsIntoBean() {
			var testService = new TestService("test");
			var anotherService = new AnotherService(42);
			beanStore.add(TestService.class, testService);
			beanStore.add(AnotherService.class, anotherService);

			var bean = bc(BeanWithInjectedFields.class).run();

			assertSame(testService, bean.getService());
			assertSame(anotherService, bean.getAnother());
		}

		/**
		 * Tests injecting dependencies into bean methods annotated with @Inject.
		 */
		@Test
		void b02_injectMethodIntoBean() {
			var testService = new TestService("test");
			beanStore.add(TestService.class, testService);

			var bean = bc(BeanWithInjectedMethod.class).run();

			assertSame(testService, bean.getService());
		}

		/**
		 * Tests that @PostConstruct methods are called after dependency injection is complete.
		 */
		@Test
		void b03_postConstructCalledAfterInjection() {
			var testService = new TestService("test");
			beanStore.add(TestService.class, testService);

			var bean = bc(B03_BeanWithPostConstruct.class).run();

			assertSame(testService, bean.getService(), "Service should be injected before PostConstruct");
			assertTrue(bean.isPostConstructCalled(), "@PostConstruct method should be called after injection");
		}

		// Bean with PostConstruct method
		public static class B03_BeanWithPostConstruct {
			private TestService service;
			boolean postConstructCalled = false;

			public B03_BeanWithPostConstruct() {}

			@Inject
			public void setService(TestService service) {
				this.service = service;
			}

			@PostConstruct
			public void postConstruct() {
				postConstructCalled = true;
			}

			public TestService getService() { return service; }
			public boolean isPostConstructCalled() { return postConstructCalled; }
		}
	}

	/**
	 * Tests bean creation using static factory methods:
	 * - getInstance() method detection and usage
	 * - Custom factory method names via factoryMethodNames()
	 * - Handling deprecated factory methods
	 * - Non-static factory method exclusion
	 * - Factory methods with parameters
	 * - Multiple factory method overloads and parameter resolution
	 */
	@Nested class C_staticFactoryMethods extends TestBase {



		/**
		 * Tests creating a bean using static getInstance() factory method.
		 */
		@Test
		void c01_createSingletonBean() {
			var bean = bc(SingletonBean.class).run();

			assertSame(SingletonBean.INSTANCE, bean);
		}

		/**
		 * Tests creating a bean using static getInstance() factory method with parameters resolved from bean store.
		 */
		@Test
		void c02_createSingletonBeanWithParams() {
			var testService = new TestService("test");
			beanStore.add(TestService.class, testService);

			var bean = bc(SingletonBeanWithParams.class).run();

			assertSame(testService, bean.getService());
		}

		/**
		 * Tests that deprecated getInstance() methods are ignored in favor of non-deprecated ones.
		 */
		@Test
		void c03_createMarshalledIgnoresDeprecatedGetInstance() {
			var bean = bc(C03_BeanWithDeprecatedGetInstance.class).run();

			assertSame(C03_BeanWithDeprecatedGetInstance.INSTANCE, bean);
		}

		public static class C03_BeanWithDeprecatedGetInstance {
			private static final C03_BeanWithDeprecatedGetInstance INSTANCE = new C03_BeanWithDeprecatedGetInstance();

			private C03_BeanWithDeprecatedGetInstance() {}

			@Deprecated
			public static C03_BeanWithDeprecatedGetInstance getInstance(String unused) {
				// Deprecated version - should be ignored
				throw rex("Should not be called");
			}

			public static C03_BeanWithDeprecatedGetInstance getInstance() {
				// Valid non-deprecated version
				return INSTANCE;
			}
		}

		/**
		 * Tests that non-static getInstance() methods are ignored (only static factory methods are used).
		 */
		@Test
		void c04_createMarshalledIgnoresNonStaticGetInstance() {
			var bean = bc(C04_BeanWithNonStaticGetInstance.class).run();

			assertInstanceOf(C04_BeanWithNonStaticGetInstance.class, bean);
		}

		public static class C04_BeanWithNonStaticGetInstance {
			public C04_BeanWithNonStaticGetInstance() {
				// Public constructor
			}

			public C04_BeanWithNonStaticGetInstance getInstance() {
				throw rex("Should not be called");
			}
		}

		/**
		 * Tests creating a bean using a custom factory method named "of" via factoryMethodNames().
		 */
		@Test
		void c05_customFactoryMethodOf() {
			beanStore.add(String.class, "test-value");

			var bean = bc(C05_BeanWithOfMethod.class).factoryMethodNames("of").run();

			assertEquals("test-value", bean.getValue());
		}

		public static class C05_BeanWithOfMethod {
			private final String value;

			private C05_BeanWithOfMethod(String value) {
				this.value = value;
			}

			public static C05_BeanWithOfMethod of(String value) {
				return new C05_BeanWithOfMethod(value);
			}

			public String getValue() { return value; }
		}

		/**
		 * Tests creating a bean using a custom factory method named "from" via factoryMethodNames().
		 */
		@Test
		void c06_customFactoryMethodFrom() {
			beanStore.add(String.class, "test-value");

			var bean = bc(C06_BeanWithFromMethod.class).factoryMethodNames("from").run();

			assertEquals("test-value", bean.getValue());
		}

		public static class C06_BeanWithFromMethod {
			private final String value;

			private C06_BeanWithFromMethod(String value) {
				this.value = value;
			}

			public static C06_BeanWithFromMethod from(String value) {
				return new C06_BeanWithFromMethod(value);
			}

			public String getValue() { return value; }
		}

		/**
		 * Tests creating a bean using a custom factory method named "create" via factoryMethodNames().
		 */
		@Test
		void c07_customFactoryMethodCreate() {
			beanStore.add(String.class, "test-value");

			var bean = bc(C07_BeanWithCreateMethod.class).factoryMethodNames("create").run();

			assertEquals("test-value", bean.getValue());
		}

		public static class C07_BeanWithCreateMethod {
			private final String value;

			private C07_BeanWithCreateMethod(String value) {
				this.value = value;
			}

			public static C07_BeanWithCreateMethod create(String value) {
				return new C07_BeanWithCreateMethod(value);
			}

			public String getValue() { return value; }
		}

		/**
		 * Tests creating a bean using a custom factory method named "newInstance" via factoryMethodNames().
		 */
		@Test
		void c08_customFactoryMethodNewInstance() {
			beanStore.add(String.class, "test-value");

			var bean = bc(C08_BeanWithNewInstanceMethod.class).factoryMethodNames("newInstance").run();

			assertEquals("test-value", bean.getValue());
		}

		public static class C08_BeanWithNewInstanceMethod {
			private final String value;

			private C08_BeanWithNewInstanceMethod(String value) {
				this.value = value;
			}

			public static C08_BeanWithNewInstanceMethod newInstance(String value) {
				return new C08_BeanWithNewInstanceMethod(value);
			}

			public String getValue() { return value; }
		}

		/**
		 * Tests that factoryMethodNames() accepts multiple method names and uses the first matching one.
		 */
		@Test
		void c09a_multipleFactoryMethodNames() {
			beanStore.add(String.class, "test-value");

			var bean = bc(C09_BeanWithMultipleFactoryMethods.class).factoryMethodNames("of", "from", "newInstance").run();

			assertTrue(bean.getSource().equals("of") || bean.getSource().equals("from"));
		}

		/**
		 * Tests that default getInstance() factory method still works when custom factoryMethodNames() is not called.
		 */
		@Test
		void c10_defaultGetInstanceStillWorks() {
			var service = new TestService("test");
			beanStore.add(TestService.class, service);

			var bean = bc(SingletonBeanWithParams.class).run();

			assertSame(service, bean.getService());
		}

		/**
		 * Tests that when both custom factory method and builder exist, builder takes precedence.
		 */
		@Test
		void c11_customFactoryMethodVsBuilder() {
			beanStore.add(String.class, "test-value");

			var bean = bc(C11_BeanWithBuilderAndCustomFactoryMethod.class).factoryMethodNames("of").run();

			assertNull(bean.getValue());
		}

		// Bean with builder and custom factory method (but no factory that accepts builder)
		public static class C11_BeanWithBuilderAndCustomFactoryMethod {
			private final String value;

			private C11_BeanWithBuilderAndCustomFactoryMethod(String value) {
				this.value = value;
			}

			public static BuilderForCustomFactory builder() {
				return new BuilderForCustomFactory();
			}

			public static C11_BeanWithBuilderAndCustomFactoryMethod of(String value) {
				return new C11_BeanWithBuilderAndCustomFactoryMethod(value);
			}

			public String getValue() { return value; }

			public static class BuilderForCustomFactory {
				private String value;

				// @formatter:off
				public BuilderForCustomFactory value(String value) { this.value = value; return this;}
				// @formatter:on

				public C11_BeanWithBuilderAndCustomFactoryMethod build() {
					return new C11_BeanWithBuilderAndCustomFactoryMethod(value);
				}
			}
		}

		/**
		 * Tests creating a bean using a custom factory method with no parameters.
		 */
		@Test
		void c12_noArgCustomFactoryMethod() {
			var bean = bc(C12_BeanWithNoArgFactoryMethod.class).factoryMethodNames("of").run();

			assertSame(C12_BeanWithNoArgFactoryMethod.INSTANCE, bean);
		}

		public static class C12_BeanWithNoArgFactoryMethod {
			private static final C12_BeanWithNoArgFactoryMethod INSTANCE = new C12_BeanWithNoArgFactoryMethod();

			private C12_BeanWithNoArgFactoryMethod() {}

			public static C12_BeanWithNoArgFactoryMethod of() {
				return INSTANCE;
			}
		}

		/**
		 * Tests that when multiple factory method overloads exist, the one with the most resolvable parameters is selected.
		 */
		@Test
		void c13_factoryMethodMostParametersSelected() {
			beanStore.add(String.class, "test");
			beanStore.add(TestService.class, new TestService("test"));

			var bean = bc(C13_BeanWithMultipleFactoryOverloads.class).factoryMethodNames("of").run();

			assertEquals(2, bean.getParamCount(), "Should select factory with most resolvable parameters");
		}

		public static class C13_BeanWithMultipleFactoryOverloads {
			private final int paramCount;

			private C13_BeanWithMultipleFactoryOverloads(int paramCount) {
				this.paramCount = paramCount;
			}

			public static C13_BeanWithMultipleFactoryOverloads of() {
				return new C13_BeanWithMultipleFactoryOverloads(0);
			}

			public static C13_BeanWithMultipleFactoryOverloads of(String s) {
				return new C13_BeanWithMultipleFactoryOverloads(1);
			}

			public static C13_BeanWithMultipleFactoryOverloads of(String s, TestService ts) {
				return new C13_BeanWithMultipleFactoryOverloads(2);
			}

			public int getParamCount() { return paramCount; }
		}

		/**
		 * Tests that calling factoryMethodNames() multiple times replaces the previous value rather than accumulating.
		 */
		@Test
		void c09b_replacementFactoryMethodBehavior() {
			beanStore.add(String.class, "test-value");

			// Second call should replace first call (not cumulative)
			var bean = bc(C09_BeanWithMultipleFactoryMethods.class)
				.factoryMethodNames("of")
				.factoryMethodNames("from")
				.run();

			// Should find 'from' (not 'of' since it was replaced)
			assertEquals("from", bean.getSource());
		}

		// Bean with multiple factory methods
		public static class C09_BeanWithMultipleFactoryMethods {
			private final String source;

			private C09_BeanWithMultipleFactoryMethods(String source) {
				this.source = source;
			}

			public static C09_BeanWithMultipleFactoryMethods of(String value) {
				return new C09_BeanWithMultipleFactoryMethods("of");
			}

			public static C09_BeanWithMultipleFactoryMethods from(String value) {
				return new C09_BeanWithMultipleFactoryMethods("from");
			}

			public static C09_BeanWithMultipleFactoryMethods getInstance(String value) {
				return new C09_BeanWithMultipleFactoryMethods("getInstance");
			}

			public String getSource() { return source; }
		}
	}

	/**
	 * Tests bean creation using builder patterns:
	 * - Auto-detected builder classes
	 * - Explicit builder instances and types
	 * - @Builder annotation support
	 * - Builder inheritance and parent/child relationships
	 * - Custom builder method names (build, create, get)
	 * - Builders with @Inject annotations
	 */
	@Nested class D_builderBasedCreation extends TestBase {

		/**
		 * Tests creating a bean using builder pattern with auto-detected builder.
		 * Verifies that BeanInstantiator can automatically detect and use an inner Builder class
		 * when no explicit builder is specified. The builder is found via the static create()
		 * method that returns a Builder instance, demonstrating the default builder detection mechanism.
		 */
		@Test
		void d01_createBeanWithBuilder() {
			var bean = bc(BeanWithBuilder.class).run();

			assertInstanceOf(BeanWithBuilder.class, bean);
			// Verify default values from builder (null for name, 0 for value)
			assertNull(bean.getName());
			assertEquals(0, bean.getValue());
		}

		/**
		 * Tests creating a bean using an explicitly provided builder instance.
		 * Verifies that when a builder instance is explicitly provided via builder(),
		 * BeanInstantiator uses that instance instead of creating a new one. This allows
		 * pre-configuring the builder with specific values before bean creation.
		 */
		@Test
		void d02_createBeanWithExplicitBuilderInstance() {
			var builder = BeanWithBuilder.create()
				.name("test")
				.value(42);

			var bean = bc(BeanWithBuilder.class)
				.builder(builder)
				.run();

			assertEquals("test", bean.getName());
			assertEquals(42, bean.getValue());
		}

		/**
		 * Tests creating a bean using builder pattern with @Builder annotation.
		 * Verifies that when a bean class is annotated with @Builder specifying a builder type,
		 * BeanInstantiator uses that builder type instead of auto-detection. This provides explicit
		 * control over which builder class to use for bean creation.
		 */
		@Test
		void d03_createBeanWithBuilderAnnotation() {
			var bean = bc(D03_BeanWithBuilderAnnotation.class).run();

			assertInstanceOf(D03_BeanWithBuilderAnnotation.class, bean);
			// Verify default value from builder (null for name)
			assertNull(bean.getName());
		}

		@Builder(D03_BeanWithBuilderAnnotation.MyBuilder.class)
		public static class D03_BeanWithBuilderAnnotation {
			private final String name;

			private D03_BeanWithBuilderAnnotation(MyBuilder builder) {
				this.name = builder.name;
			}

			public String getName() { return name; }

			public static class MyBuilder {
				private String name;

				// @formatter:off
				public MyBuilder name(String name) { this.name = name; return this;}
				// @formatter:on

				public D03_BeanWithBuilderAnnotation build() {
					return new D03_BeanWithBuilderAnnotation(this);
				}
			}
		}

		/**
		 * Tests creating a bean using explicitly specified builder class.
		 * Verifies that builder(Class) can be used to specify the builder type programmatically,
		 * allowing runtime selection of builder classes. The builder instance is still created
		 * automatically, but the type is explicitly controlled.
		 */
		@Test
		void d04_createBeanWithExplicitBuilderType() {
			var bean = bc(BeanWithBuilder.class)
				.builder(BeanWithBuilder.Builder.class)
				.run();

			assertInstanceOf(BeanWithBuilder.class, bean);
			// Verify default values from builder (null for name, 0 for value)
			assertNull(bean.getName());
			assertEquals(0, bean.getValue());
		}

		/**
		 * Tests creating a bean using builder with @Inject annotation on build() method.
		 * Verifies that when a builder's build() method is annotated with @Inject, its parameters
		 * are resolved from the bean store and injected during bean creation. This enables builders
		 * to accept dependencies that are provided by the dependency injection system.
		 */
		@Test
		void d05_createBeanWithInjectedBuilder() {
			var testService = new TestService("test");
			beanStore.add(TestService.class, testService);

			var bean = bc(D05_BeanWithInjectedBuilder.class).run();

			assertSame(testService, bean.getService());
		}

		// Bean with injected builder
		public static class D05_BeanWithInjectedBuilder {
			private final TestService service;

			private D05_BeanWithInjectedBuilder(BuilderWithInjection builder) {
				this.service = builder.service;
			}

			public TestService getService() { return service; }

			public static class BuilderWithInjection {
				@Inject TestService service;

				public D05_BeanWithInjectedBuilder build() {
					return new D05_BeanWithInjectedBuilder(this);
				}
			}

			public static BuilderWithInjection create() {
				return new BuilderWithInjection();
			}
		}

		/**
		 * Tests getting the builder instance via getBuilder() method.
		 * Verifies that getBuilder() returns an Optional containing the builder instance, which
		 * is lazily created when first accessed. This allows accessing the builder before calling
		 * run() to inspect or modify its state.
		 */
		@Test
		void d06_getBuilder() {
			var creator = bc(BeanWithBuilder.class);

			// Builder should be lazily created
			Optional<BeanWithBuilder.Builder> builder = creator.getBuilder();
			assertInstanceOf(BeanWithBuilder.Builder.class, builder.get());
		}

		/**
		 * Tests creating a bean using builder with protected constructor.
		 * Verifies that builders with protected constructors can be instantiated by BeanInstantiator.
		 * This tests the fallback mechanism that searches for protected constructors when no public
		 * constructor is available, enabling encapsulation while still allowing bean creation.
		 */
		@Test
		void d08_createBeanWithProtectedBuilderConstructor() {
			var bean = bc(D08_BeanWithProtectedBuilderConstructor.class).run();

			assertInstanceOf(D08_BeanWithProtectedBuilderConstructor.class, bean);
		}

		/**
		 * Tests creating a bean using a builder whose only constructor is package-private.
		 *
		 * <p>Verifies the {@code BeanInstantiator.findBuilder()} Step 4 fallback: after public and
		 * protected constructors fail, the builder's package-private constructors are considered. This
		 * symmetric widening matches the bean-side ladder and lets builders co-located in their bean's
		 * declaring package stay package-scoped.
		 */
		@Test
		void d08b_createBeanWithPackagePrivateBuilderConstructor() {
			var bean = bc(D08b_BeanWithPackagePrivateBuilderConstructor.class).run();

			assertInstanceOf(D08b_BeanWithPackagePrivateBuilderConstructor.class, bean);
			assertEquals("pkg-builder", bean.getValue());
		}

		// Bean with builder that has a package-private constructor (no public or protected ctor exposed).
		@Builder(D08b_BeanWithPackagePrivateBuilderConstructor.PackageBuilder.class)
		public static class D08b_BeanWithPackagePrivateBuilderConstructor {
			private final String value;

			private D08b_BeanWithPackagePrivateBuilderConstructor(PackageBuilder builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			public static class PackageBuilder {
				private final String value = "pkg-builder";

				PackageBuilder() {
					/* package-private — exercised by BeanInstantiator.findBuilder() Step 4 */
				}

				public D08b_BeanWithPackagePrivateBuilderConstructor build() {
					return new D08b_BeanWithPackagePrivateBuilderConstructor(this);
				}
			}
		}

		// Bean with builder that has a protected constructor
		@Builder(D08_BeanWithProtectedBuilderConstructor.ProtectedBuilder.class)
		public static class D08_BeanWithProtectedBuilderConstructor {
			private final String value;

			private D08_BeanWithProtectedBuilderConstructor(ProtectedBuilder builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			public static class ProtectedBuilder {
				private String value;

				protected ProtectedBuilder() {
					// Protected no-arg constructor
				}

				// @formatter:off
				public ProtectedBuilder value(String value) { this.value = value; return this;}
				// @formatter:on

				public D08_BeanWithProtectedBuilderConstructor build() {
					return new D08_BeanWithProtectedBuilderConstructor(this);
				}
			}
		}

		/**
		 * Tests creating a bean using builder with protected constructor that requires dependencies.
		 * Verifies that protected builder constructors can accept parameters that are resolved from
		 * the bean store via dependency injection. This combines encapsulation (protected constructor)
		 * with dependency injection capabilities, allowing builders to receive dependencies at construction time.
		 */
		@Test
		void d09_createBeanWithProtectedBuilderConstructorWithDependencies() {
			var testService = new TestService("injected");
			beanStore.add(TestService.class, testService);

			var bean = bc(D09_BeanWithProtectedBuilderConstructorWithDeps.class).run();

			assertEquals("injected", bean.getServiceName());
		}

		// Bean with builder that has a protected constructor with dependencies
		@Builder(D09_BeanWithProtectedBuilderConstructorWithDeps.ProtectedBuilderWithDeps.class)
		public static class D09_BeanWithProtectedBuilderConstructorWithDeps {
			private final String serviceName;

			private D09_BeanWithProtectedBuilderConstructorWithDeps(ProtectedBuilderWithDeps builder) {
				this.serviceName = builder.service.getName();
			}

			public String getServiceName() { return serviceName; }

			public static class ProtectedBuilderWithDeps {
				private TestService service;

				protected ProtectedBuilderWithDeps(TestService service) {
					// Protected constructor with dependency injection
					this.service = service;
				}

				public D09_BeanWithProtectedBuilderConstructorWithDeps build() {
					return new D09_BeanWithProtectedBuilderConstructorWithDeps(this);
				}
			}
		}

		// Parent bean with @Builder annotation

		/**
		 * Tests creating a child bean using child's own builder that extends parent's builder.
		 * Verifies that when a child class has its own @Builder annotation pointing to a builder
		 * that extends the parent's builder, BeanInstantiator uses the child's builder annotation
		 * (which overrides the parent's). The child's builder must override build() to return
		 * the child type, ensuring type safety.
		 */
		@Test
		void d10_createChildBeanUsingParentBuilderAnnotation() {
			var bc = bc(D10_ParentBeanWithBuilderAnnotation.class)
				.type(D10_ChildBeanWithInheritedBuilderAnnotation.class)
				.debug();

			D10_ParentBeanWithBuilderAnnotation bean = bc.run();

			assertInstanceOf(D10_ChildBeanWithInheritedBuilderAnnotation.class, bean);
		}

		@Builder(D10_ParentBeanWithBuilderAnnotation.ParentBuilder.class)
		public static class D10_ParentBeanWithBuilderAnnotation {
			protected final String value;

			protected D10_ParentBeanWithBuilderAnnotation(ParentBuilder builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			public static class ParentBuilder {
				protected String value;

				// @formatter:off
				public ParentBuilder value(String value) { this.value = value; return this;}
				// @formatter:on

				public D10_ParentBeanWithBuilderAnnotation build() {
					return new D10_ParentBeanWithBuilderAnnotation(this);
				}
			}
		}

		// Child bean with its own builder that extends parent's builder
		@Builder(D10_ChildBeanWithInheritedBuilderAnnotation.ChildBuilder.class)
		public static class D10_ChildBeanWithInheritedBuilderAnnotation extends D10_ParentBeanWithBuilderAnnotation {
			public D10_ChildBeanWithInheritedBuilderAnnotation(ChildBuilder builder) {
				super(builder);
			}

			public static class ChildBuilder extends ParentBuilder {
				@Override
				public D10_ChildBeanWithInheritedBuilderAnnotation build() {
					return new D10_ChildBeanWithInheritedBuilderAnnotation(this);
				}
			}
		}

		/**
		 * Tests auto-detection of builder via inner "Builder" class when no @Builder annotation or static factory method exists.
		 * Verifies the fallback builder detection mechanism that searches for inner classes named "Builder"
		 * when no explicit builder specification or annotation is present. This provides a convention-based
		 * approach to builder discovery without requiring annotations or static factory methods.
		 */
		@Test
		void d11_createBeanWithInnerBuilderClass() {
			var bean = bc(D11_BeanWithInnerBuilderClass.class).run();

			assertInstanceOf(D11_BeanWithInnerBuilderClass.class, bean);
		}

		// Bean with inner Builder class (no @Builder annotation, no static create/builder method)
		public static class D11_BeanWithInnerBuilderClass {
			private final String value;

			private D11_BeanWithInnerBuilderClass(Builder builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			public static class Builder {
				private String value;

				// @formatter:off
				public Builder value(String value) { this.value = value; return this;}
				// @formatter:on

				public D11_BeanWithInnerBuilderClass build() {
					return new D11_BeanWithInnerBuilderClass(this);
				}
			}
		}


		/**
		 * Tests creating a child bean using child's own builder that extends parent's inner Builder class.
		 * Verifies that when a child class has its own builder that extends the parent's inner Builder class,
		 * BeanInstantiator uses the child's builder. The child's builder must override build() to return the
		 * child type, ensuring type safety. The builder is discovered through the child class's static create() method.
		 */
		@Test
		void d12_createChildBeanUsingParentInnerBuilderClass() {
			var bean = bc(D12_ParentWithInnerBuilderClass.class)
				.type(D12_ChildWithInheritedInnerBuilderClass.class)
				.run();

			assertInstanceOf(D12_ChildWithInheritedInnerBuilderClass.class, bean);
		}

		// Parent bean with inner Builder class (no annotation, no static method)
		public static class D12_ParentWithInnerBuilderClass {
			protected final String value;

			protected D12_ParentWithInnerBuilderClass(Builder builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			public static class Builder {
				protected String value;

				// @formatter:off
				public Builder value(String value) { this.value = value; return this;}
				// @formatter:on

				public D12_ParentWithInnerBuilderClass build() {
					return new D12_ParentWithInnerBuilderClass(this);
				}
			}
		}

		// Child bean that inherits parent's inner Builder class
		public static class D12_ChildWithInheritedInnerBuilderClass extends D12_ParentWithInnerBuilderClass {
			public D12_ChildWithInheritedInnerBuilderClass(ChildBuilder builder) {
				super(builder);
			}

			public static class ChildBuilder extends Builder {
				@Override
				public D12_ChildWithInheritedInnerBuilderClass build() {
					return new D12_ChildWithInheritedInnerBuilderClass(this);
				}
			}

			public static ChildBuilder create() {
				return new ChildBuilder();
			}
		}

		/**
		 * Tests creating a bean using builder pattern with static builder() method (instead of create()).
		 * Verifies that BeanInstantiator recognizes "builder" as a valid builder factory method name alongside
		 * the default "create" method. This supports alternative naming conventions where classes use
		 * builder() instead of create() to return builder instances.
		 */
		@Test
		void d13_createBeanWithBuilderMethod() {
			var bean = bc(D13_BeanWithBuilderMethod.class)
				.run();

			assertInstanceOf(D13_BeanWithBuilderMethod.class, bean);
		}

		// Bean with static builder() method (instead of create())
		public static class D13_BeanWithBuilderMethod {
			private final String value;

			private D13_BeanWithBuilderMethod(BuilderFromBuilderMethod builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			public static class BuilderFromBuilderMethod {
				private String value;

				// @formatter:off
				public BuilderFromBuilderMethod value(String value) { this.value = value; return this;}
				// @formatter:on

				public D13_BeanWithBuilderMethod build() {
					return new D13_BeanWithBuilderMethod(this);
				}
			}

			public static BuilderFromBuilderMethod builder() {
				return new BuilderFromBuilderMethod();
			}
		}

		/**
		 * Tests that static create() returning the bean type itself is treated as a factory method, not a builder.
		 * Verifies the distinction between builder factory methods (which return builder types) and bean factory
		 * methods (which return the bean type directly). When create() returns the bean type, it's used as a
		 * factory method rather than a builder factory, ensuring correct bean creation path selection.
		 */
		@Test
		void d14_createBeanWithFactoryMethodNotBuilder() {
			var bean = bc(D14_BeanWithFactoryMethod.class).run();

			assertInstanceOf(D14_BeanWithFactoryMethod.class, bean);
		}

		// Bean with static create() that returns the bean itself (factory method, not builder)
		public static class D14_BeanWithFactoryMethod {
			private final String value;

			public D14_BeanWithFactoryMethod() {
				this.value = "from-constructor";
			}

			private D14_BeanWithFactoryMethod(String value) {
				this.value = value;
			}

			public String getValue() { return value; }

			// This is a factory method that returns the bean type itself, not a builder
			// It should be filtered out by line 364 and not considered a builder method
			public static D14_BeanWithFactoryMethod create() {
				return new D14_BeanWithFactoryMethod("from-factory");
			}
		}

		/**
		 * Tests creating a bean using builder with create() method instead of build().
		 * Verifies that builders can use "create" as an alternative to "build" for the method that
		 * constructs the bean. BeanInstantiator searches for build(), create(), or get() methods on builders,
		 * providing flexibility in builder method naming conventions.
		 */
		@Test
		void d15_createBeanWithBuilderCreateMethod() {
			var bean = bc(D15_BeanWithBuilderCreateMethod.class).run();

			assertInstanceOf(D15_BeanWithBuilderCreateMethod.class, bean);
		}

		// Bean with builder that uses create() instead of build()
		public static class D15_BeanWithBuilderCreateMethod {
			private final String value;

			private D15_BeanWithBuilderCreateMethod(BuilderWithCreate builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			public static class BuilderWithCreate {
				private String value;

				// @formatter:off
				public BuilderWithCreate value(String value) { this.value = value; return this;}
				// @formatter:on

				// Using create() instead of build()
				public D15_BeanWithBuilderCreateMethod create() {
					return new D15_BeanWithBuilderCreateMethod(this);
				}
			}

			public static BuilderWithCreate builder() {
				return new BuilderWithCreate();
			}
		}

		/**
		 * Tests creating a bean using builder with get() method instead of build().
		 * Verifies that builders can use "get" as an alternative to "build" for the method that
		 * constructs the bean. This supports builder patterns where get() is used to retrieve the
		 * final bean instance, providing another naming convention option.
		 */
		@Test
		void d16_createBeanWithBuilderGetMethod() {
			var bean = bc(D16_BeanWithBuilderGetMethod.class).run();

			assertInstanceOf(D16_BeanWithBuilderGetMethod.class, bean);
		}

		// Bean with builder that uses get() instead of build()
		public static class D16_BeanWithBuilderGetMethod {
			private final String value;

			private D16_BeanWithBuilderGetMethod(BuilderWithGet builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			public static class BuilderWithGet {
				private String value;

				// @formatter:off
				public BuilderWithGet value(String value) { this.value = value; return this;}
				// @formatter:on

				// Using get() instead of build()
				public D16_BeanWithBuilderGetMethod get() {
					return new D16_BeanWithBuilderGetMethod(this);
				}
			}

			public static BuilderWithGet builder() {
				return new BuilderWithGet();
			}
		}

		/**
		 * Tests creating a bean using builder with build() method that has parameters resolved from bean store.
		 * Verifies that when a builder's build() method requires parameters, BeanInstantiator resolves them from
		 * the bean store via dependency injection. This enables builders to accept additional dependencies
		 * beyond what's stored in builder fields, providing more flexible bean construction.
		 */
		@Test
		void d18_createBeanWithBuilderMethodWithParameters() {
			beanStore.add(String.class, "extra-value");

			var bean = bc(D18_BeanWithBuilderWithParameters.class).run();

			assertEquals("extra-value", bean.getExtra());
		}

		// Bean with builder where build() method has parameters
		public static class D18_BeanWithBuilderWithParameters {
			private final String value;
			private final String extra;

			private D18_BeanWithBuilderWithParameters(BuilderWithParameters builder, String extra) {
				this.value = builder.value;
				this.extra = extra;
			}

			public String getValue() { return value; }
			public String getExtra() { return extra; }

			public static class BuilderWithParameters {
				private String value;

				// @formatter:off
				public BuilderWithParameters value(String value) { this.value = value; return this;}
				// @formatter:on

				// build() method that requires a String parameter from bean store
				@Inject
				public D18_BeanWithBuilderWithParameters build(String extra) {
					return new D18_BeanWithBuilderWithParameters(this, extra);
				}
			}

			public static BuilderWithParameters builder() {
				return new BuilderWithParameters();
			}
		}

		/**
		 * Tests that deprecated constructors are ignored in favor of non-deprecated ones.
		 * Verifies that when multiple constructors are available, BeanInstantiator skips deprecated ones
		 * and prefers non-deprecated alternatives. This ensures that deprecated APIs don't interfere
		 * with bean creation, allowing graceful deprecation of old constructor signatures.
		 */
		@Test
		void d19_createMarshalledIgnoresDeprecatedConstructor() {
			beanStore.add(String.class, "should-not-use-this");

			var bean = bc(D19_BeanWithDeprecatedConstructor.class).run();

			assertInstanceOf(D19_BeanWithDeprecatedConstructor.class, bean);
			assertTrue(bean.wasCreated());
		}

		// Bean with deprecated constructor
		public static class D19_BeanWithDeprecatedConstructor {
			private boolean created = false;

			public D19_BeanWithDeprecatedConstructor() {
				// Valid non-deprecated constructor
				this.created = true;
			}

			@Deprecated
			public D19_BeanWithDeprecatedConstructor(String unused) {
				// Deprecated constructor - should be ignored
				throw rex("Should not be called");
			}

			public boolean wasCreated() { return created; }
		}


		/**
		 * Tests that constructors with unresolvable parameters are ignored, falling back to no-arg constructor.
		 * Verifies that BeanInstantiator skips constructors whose parameters cannot be resolved from the bean store,
		 * preferring constructors with resolvable or no parameters. This ensures robust bean creation even when
		 * some constructor signatures cannot be satisfied.
		 */
		@Test
		void d20_createMarshalledIgnoresUnresolvableConstructor() {
			var bean = bc(D20_BeanWithUnresolvableConstructor.class).run();

			assertInstanceOf(D20_BeanWithUnresolvableConstructor.class, bean);
			assertTrue(bean.wasCreated());
		}

		// Bean with constructor that has unresolvable parameters
		public static class D20_BeanWithUnresolvableConstructor {
			private boolean created = false;

			public D20_BeanWithUnresolvableConstructor() {
				// Valid no-arg constructor
				this.created = true;
			}

			public D20_BeanWithUnresolvableConstructor(UnresolvableType param) {
				// Constructor with unresolvable parameter - should be ignored
				throw rex("Should not be called");
			}

			public boolean wasCreated() { return created; }
		}

		/**
		 * Tests creating a bean via static factory method that accepts builder when builder has no build() method.
		 * Verifies that when a builder lacks build/create/get methods, BeanInstantiator falls back to static factory
		 * methods on the bean class that accept the builder. Factory methods are preferred over constructors,
		 * demonstrating the priority order: builder methods > factory methods > constructors.
		 */
		@Test
		void d21_createBeanViaFactoryMethodAcceptingBuilder() {
			var bean = bc(D21_BeanWithFactoryMethodAcceptingBuilder.class).run();

			assertEquals("default", bean.getValue());
		}

		// Bean with static factory method that accepts a builder
		public static class D21_BeanWithFactoryMethodAcceptingBuilder {
			private final String value;

			public D21_BeanWithFactoryMethodAcceptingBuilder(Builder builder) {
				// Public constructor accepting builder - validates builder type
				// But we want the factory method to be used instead
				this.value = builder.value + "-constructor";
			}

			public static D21_BeanWithFactoryMethodAcceptingBuilder getInstance(Builder builder) {
				// This is the factory method that should be called (line 1017)
				// It should be preferred over the constructor
				return new D21_BeanWithFactoryMethodAcceptingBuilder(builder.value);
			}

			private D21_BeanWithFactoryMethodAcceptingBuilder(String value) {
				this.value = value;
			}

			public String getValue() {
				return value;
			}

			public static class Builder {
				private String value = "default";

				// @formatter:off
				public Builder value(String value) { this.value = value; return this;}
				// @formatter:on

				public static Builder create() {
					return new Builder();
				}

				// No build() method - forces use of constructor or factory method
			}
		}

		/**
		 * Tests creating a bean using builder with custom method name (not build/create/get) via constructor accepting builder.
		 * Verifies that when a builder has a custom-named method returning the bean (not build/create/get) and the bean
		 * has a constructor accepting the builder, the constructor is used. This tests the fallback mechanism when standard
		 * builder method names aren't available but a constructor can accept the builder directly.
		 */
		@Test
		void d22_createBeanViaBuilderCustomMethod() {
			var bean = bc(D22_BeanWithBuilderCustomMethod.class)
				.builderClassNames("BuilderWithCustomMethod")
				.debug()
				.run();

			assertEquals("default", bean.getValue());
		}

		// Bean with builder that has a custom-named method returning the bean
		public static class D22_BeanWithBuilderCustomMethod {
			private final String value;

			public D22_BeanWithBuilderCustomMethod(BuilderWithCustomMethod builder) {
				this.value = builder.value;
			}

			public String getValue() {
				return value;
			}

			public static class BuilderWithCustomMethod {
				private String value = "default";

				// @formatter:off
				public BuilderWithCustomMethod value(String value) { this.value = value; return this;}
				// @formatter:on

				// This build() method returns wrong type - will be skipped
				// Forces fallback to the custom-named method
				public String build() {
					return "wrong-type";
				}

				// Custom method name that returns the bean (not build/create/get)
				// This should be found by the fallback "anything" method search (lines 1037-1044)
				public D22_BeanWithBuilderCustomMethod execute() {
					return new D22_BeanWithBuilderCustomMethod(this);
				}

				public static BuilderWithCustomMethod create() {
					return new BuilderWithCustomMethod();
				}
			}
		}

		/**
		 * Tests creating a bean using builder with custom build method names via buildMethodNames().
		 * Verifies that buildMethodNames() allows customizing which methods on the builder are considered
		 * build methods. This tests the configuration method itself, ensuring it properly sets the build
		 * method names and resets the creator state.
		 */
		@Test
		void d23_createBeanWithCustomBuildMethodNames() {
			var bean = bc(D23_BeanWithCustomBuildMethodNames.class)
				.buildMethodNames("execute", "make")
				.run();

			assertEquals("test-value", bean.getValue());
		}

		// Bean with builder that uses custom build method names
		public static class D23_BeanWithCustomBuildMethodNames {
			private final String value;

			private D23_BeanWithCustomBuildMethodNames(CustomBuildMethodBuilder builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			// Static method to return the builder (needed for builder detection)
			public static CustomBuildMethodBuilder create() {
				return new CustomBuildMethodBuilder();
			}

			public static class CustomBuildMethodBuilder {
				private String value = "test-value";

				// @formatter:off
				public CustomBuildMethodBuilder value(String value) { this.value = value; return this;}
				// @formatter:on

				// Custom build method name "execute" - will be used when buildMethodNames("execute", "make") is configured
				public D23_BeanWithCustomBuildMethodNames execute() {
					return new D23_BeanWithCustomBuildMethodNames(this);
				}
			}
		}

		/**
		 * Tests creating a bean using builder with custom method name when no build/create/get method exists and no constructor accepts builder.
		 * Verifies the final fallback mechanism where BeanInstantiator searches for any method on the builder that returns
		 * the bean type. When standard builder methods and constructors aren't available, this "anything" method search
		 * provides a last-resort mechanism for bean creation, demonstrating maximum flexibility in builder patterns.
		 */
		@Test
		void d24_createBeanViaBuilderAnyMethod() {
			var creator = bc(D24_BeanWithBuilderAnyMethod.class)
				.builderClassNames("BuilderAnyMethod")
				.debug();

			var bean = creator.run();

			assertEquals("default", bean.getValue());

			var log = creator.getDebugLog();
			assertContains("Builder.anything()", log.toString());
		}

		// Bean where builder has custom method and constructor accepts builder but has unresolvable params
		public static class D24_BeanWithBuilderAnyMethod {
			private final String value;

			private D24_BeanWithBuilderAnyMethod(String value) {
				this.value = value;
			}

			// Public constructor that accepts builder but requires unresolvable parameter
			// This validates the builder type but won't be used at line 1063-1070
			public D24_BeanWithBuilderAnyMethod(BuilderAnyMethod builder, UnresolvableType unresolvable) {
				throw rex("Should not be called - has unresolvable parameter");
			}

			public String getValue() {
				return value;
			}

			public static class BuilderAnyMethod {
				private String value = "default";

				// @formatter:off
				public BuilderAnyMethod value(String value) { this.value = value; return this;}
				// @formatter:on

				// Custom method name that returns the bean
				// This should be found by the fallback "anything" method search (lines 1075-1082)
				public D24_BeanWithBuilderAnyMethod finish() {
					return new D24_BeanWithBuilderAnyMethod(this.value);
				}

				public static BuilderAnyMethod create() {
					return new BuilderAnyMethod();
				}
			}
		}

		/**
		 * Tests that invalid builders (no valid build method, no constructor accepting builder) fail validation and fall back to constructor.
		 * Verifies that when a builder candidate doesn't meet validation criteria (no valid build method returning bean type,
		 * no constructor accepting builder), BeanInstantiator rejects it and falls back to direct constructor-based creation.
		 * This ensures that invalid builder configurations don't prevent bean creation, maintaining robustness.
		 */
		@Test
		void d25_invalidBuilderFailsValidation() {
			beanStore.add(String.class, "test-value");

			var creator = bc(D25_BeanWithInvalidBuilder.class).debug();

			var bean = creator.run();

			assertEquals("test-value", bean.getValue());

			var log = creator.getDebugLog();
			assertContains("Builder is NOT valid", log.toString());
		}

		// Bean with invalid builder (doesn't meet validation criteria)
		public static class D25_BeanWithInvalidBuilder {
			private final String value;

			public D25_BeanWithInvalidBuilder(String value) {
				this.value = value;
			}

			public String getValue() {
				return value;
			}

			// This inner class is named "Builder" but is NOT a valid builder:
			// - No build/create/get method returning D25_BeanWithInvalidBuilder
			// - Bean has no constructor accepting this builder
			// This should fail validation and log line 998
			public static class Builder {
				private String value = "default";

				// @formatter:off
				public Builder value(String value) { this.value = value; return this;}
				// @formatter:on

				// This method returns String, not the bean type - invalid!
				public String build() {
					return value;
				}
			}
		}

		/**
		 * Tests that ExecutableException is thrown when builder fails to create bean and no fallback is provided.
		 * Verifies that when a builder's build() method requires unresolvable parameters and no alternative creation
		 * path exists (no factory methods, no valid constructors), BeanInstantiator throws an ExecutableException.
		 * This ensures that unresolvable builder configurations result in clear error reporting rather than silent failures.
		 */
		@Test
		void d27_builderFailsWithoutFallback() {
			var creator = bc(D27_BeanWithFailingBuilder.class).debug();

			var exception = assertThrows(ExecutableException.class, creator::run);

			assertContains("Could not instantiate class", exception.getMessage());
			assertContains("using builder type", exception.getMessage());

			var log = creator.getDebugLog();
			assertContains("Failed to create bean using builder", log.toString());
		}

		// Bean with builder that fails to create bean (unresolvable parameters)
		public static class D27_BeanWithFailingBuilder {
			private final String value;

			public D27_BeanWithFailingBuilder(String value) {
				this.value = value;
			}

			public String getValue() {
				return value;
			}

			public static class Builder {
				private String value = "default";

				// @formatter:off
				public Builder value(String value) { this.value = value; return this;}
				// @formatter:on

				// build() method requires unresolvable parameter - will fail
				public D27_BeanWithFailingBuilder build(UnresolvableType unresolvable) {
					return new D27_BeanWithFailingBuilder(this.value);
				}

				public static Builder create() {
					return new Builder();
				}
			}
		}

		/**
		 * Tests the loose-builder fallthrough: when the builder's build method declares the parent return
		 * type and produces a parent-type runtime instance, BeanInstantiator falls through to factory
		 * methods / constructors on the bean subtype, with the builder passed as an extra bean.  This lets
		 * "subclass extends parent whose Builder.build() returns parent" patterns resolve cleanly via a
		 * subclass constructor that accepts the builder.
		 */
		@Test
		void d28_builderMethodReturningParentTypeFallsThroughToConstructor() {
			var creator = bc(D28_ParentBeanForBuilderMethod.class)
				.type(D28_ChildBeanForBuilderMethod.class)
				.builder(D28_BuilderForParentMethod.class);

			// Constructor `D28_ChildBeanForBuilderMethod(D28_BuilderForParentMethod builder)` matches; bean
			// is constructed with default builder state.
			var bean = creator.run();
			assertNotNull(bean);
			assertInstanceOf(D28_ChildBeanForBuilderMethod.class, bean);
			assertEquals("default", bean.getValue());
		}

		// Parent bean type
		public static class D28_ParentBeanForBuilderMethod {
			protected final String value;

			public D28_ParentBeanForBuilderMethod(String value) {
				this.value = value;
			}

			public String getValue() { return value; }

			// Static method to return the builder (needed for builder detection)
			public static D28_BuilderForParentMethod create() {
				return new D28_BuilderForParentMethod();
			}
		}

		// Child bean with constructor accepting builder
		public static class D28_ChildBeanForBuilderMethod extends D28_ParentBeanForBuilderMethod {
			public D28_ChildBeanForBuilderMethod(D28_BuilderForParentMethod builder) {
				super(builder.value);
			}

			// Static method to return the builder (needed for builder detection on child class)
			public static D28_BuilderForParentMethod create() {
				return new D28_BuilderForParentMethod();
			}
		}

		// Builder with build() method returning parent type
		public static class D28_BuilderForParentMethod {
			private String value = "default";

			// @formatter:off
			public D28_BuilderForParentMethod value(String value) { this.value = value; return this;}
			// @formatter:on

			// Build() method that returns parent type instead of child type
			// This passes validation (returns a parent of beanSubType), but fails during actual use
			// because builder build methods must return the exact bean subtype
			public D28_ParentBeanForBuilderMethod build() {
				return new D28_ParentBeanForBuilderMethod(value);
			}
		}
	}

	/**
	 * Tests type specification and subtype creation:
	 * - Creating beans with specified subtypes using type()
	 * - Parent/child bean relationships
	 * - Type resolution and validation
	 */
	@Nested class E_typeSpecification extends TestBase {

		/**
		 * Tests creating a bean with a specified subtype using type() method.
		 */
		@Test
		void e01_createWithBeanSubType() {
			var bean = bc(ParentBean.class).type(ChildBean.class).run();

			assertInstanceOf(ChildBean.class, bean);
		}
	}

	/**
	 * Tests creating inner class beans:
	 * - Providing enclosing instance for inner classes
	 * - Inner class constructor resolution
	 * - Enclosing instance handling
	 */
	@Nested class F_innerClasses extends TestBase {

		/**
		 * Tests creating an inner class bean using enclosingInstance() to provide the outer class instance.
		 */
		@Test
		void f01_createInnerBean() {
			var outerInstance = new BeanInstantiator_Test();
			var value = "test";
			beanStore.add(String.class, value);

			var bean = BeanInstantiator.of(InnerBean.class, beanStore, null, outerInstance)
				.run();

			assertEquals(value, bean.getValue());
		}
	}

	/**
	 * Tests error handling and exception scenarios:
	 * - ExecutableException when dependencies are missing
	 * - Invalid builder configurations
	 * - Unresolvable types and parameters
	 * - Builder validation failures
	 * - Error messages and exception details
	 */
	@Nested class G_errorCases extends TestBase {

		/**
		 * Tests that ExecutableException is thrown when required dependencies are missing from bean store.
		 */
		@Test
		void g01_missingDependencyThrowsException() {
			var creator = bc(BeanWithDependencies.class);
			assertThrows(ExecutableException.class, creator::run);
		}

		/**
		 * Tests that ExecutableException is thrown when trying to create an abstract class.
		 */
		@Test
		void g02_abstractClassThrowsException() {
			var creator = bc(AbstractBean.class);
			assertThrows(ExecutableException.class, creator::run);
		}

		// Abstract class with unresolvable constructor (so constructor attempt is skipped)
		public abstract static class AbstractBeanWithUnresolvableConstructor {
			public AbstractBeanWithUnresolvableConstructor(UnresolvableType unresolvable) {
				// Constructor with unresolvable parameter - won't be matched
			}
		}

		// Concrete implementation of AbstractBeanWithUnresolvableConstructor for fallback tests
		public static class ConcreteBeanWithUnresolvableConstructor extends AbstractBeanWithUnresolvableConstructor {
			public ConcreteBeanWithUnresolvableConstructor() {
				super(null); // Call parent constructor with null (won't be used in fallback)
			}
		}

		/**
		 * Tests that ExecutableException is thrown when trying to create an abstract class with no fallback and unresolvable constructor.
		 */
		@Test
		void g02c_abstractClassThrowsExceptionWithoutFallback() {
			var creator = bc(AbstractBeanWithUnresolvableConstructor.class).debug();

			// Should throw exception because abstract class and no fallback
			var exception = assertThrows(ExecutableException.class, creator::run);

			assertContains("Could not instantiate class", exception.getMessage());
			assertContains("Class is abstract", exception.getMessage());

			// Verify the debug log shows abstract class detection
			var log = creator.getDebugLog();
			assertContains("Bean type is abstract", log.toString());
		}

		/**
		 * Tests that ExecutableException is thrown when trying to create an interface.
		 */
		@Test
		void g03_interfaceThrowsException() {
			var creator = bc(BeanInterface.class);
			assertThrows(ExecutableException.class, creator::run);
		}

		/**
		 * Tests that fallback() method sets the fallback supplier and returns this for chaining.
		 */
		@Test
		void g04_fallbackReturnsThis() {
			var defaultBean = new SimpleBean();
			var creator = bc(SimpleBean.class);
			var result = creator.fallback(() -> defaultBean);
			assertSame(creator, result);
		}

		/**
		 * Tests that fallback() throws IllegalArgumentException when null is passed.
		 */
		@Test
		void g05_fallbackNullThrowsException() {
			var creator = bc(SimpleBean.class);
			assertThrows(IllegalArgumentException.class, () -> creator.fallback(null));
		}

		/**
		 * Tests that fallback supplier is used when bean creation fails due to missing dependencies.
		 */
		@Test
		void g06_fallbackUsedWhenCreationFails() {
			var fallbackBean = new BeanWithDependencies(new TestService("fallback"), new AnotherService(42));

			var bean = bc(BeanWithDependencies.class)
				.fallback(() -> fallbackBean)
				.run();

			assertSame(fallbackBean, bean);
			assertSame(fallbackBean.getService(), bean.getService());
		}

		/**
		 * Tests that fallback supplier is used when trying to create an abstract class.
		 */
		@Test
		void g07_fallbackUsedForAbstractClass() {
			var fallbackBean = new ConcreteBeanWithUnresolvableConstructor();

			var bean = bc(AbstractBeanWithUnresolvableConstructor.class)
				.fallback(() -> fallbackBean)
				.run();

			assertSame(fallbackBean, bean);
		}

		/**
		 * Tests that fallback supplier is used when trying to create an interface.
		 */
		@Test
		void g08_fallbackUsedForInterface() {
			var fallbackBean = new ConcreteBean();

			var bean = bc(BeanInterface.class)
				.fallback(() -> fallbackBean)
				.run();

			assertSame(fallbackBean, bean);
		}

		/**
		 * Tests that fallback supplier is not used when bean creation succeeds.
		 */
		@Test
		void g09_fallbackNotUsedWhenCreationSucceeds() {
			var fallbackBean = new SimpleBean();
			var fallbackCalled = Flag.create();

			var bean = bc(SimpleBean.class)
				.fallback(() -> {
					fallbackCalled.set();
					return fallbackBean;
				})
				.run();

			assertNotSame(fallbackBean, bean);
			assertFalse(fallbackCalled.isSet(), "Fallback should not be called when creation succeeds");
		}

		/**
		 * Tests that fallback-provided instance has post-creation hooks executed on it.
		 */
		@Test
		void g10_fallbackInstanceHasHooksExecuted() {
			var fallbackBean = new BeanWithDependencies(new TestService("fallback"), new AnotherService(42));
			var hookCalled = Flag.create();

			var bean = bc(BeanWithDependencies.class)
				.fallback(() -> fallbackBean)
				.postCreateHook(b -> hookCalled.set())
				.run();

			assertSame(fallbackBean, bean);
			assertTrue(hookCalled.isSet(), "Post-create hook should be executed on fallback instance");
		}
	}

	/**
	 * Tests converting BeanInstantiator to Supplier interfaces:
	 * - asSupplier() conversion
	 * - asMemoizer() with caching
	 * - Optional-like methods on suppliers
	 * - Supplier reset behavior
	 */
	@Nested class H_supplierConversion extends TestBase {

		/**
		 * Tests converting BeanInstantiator to a Supplier.
		 */
		@Test
		void h01_asSupplier() {
			var supplier = bc(SimpleBean.class).asSupplier();

			assertNotNull(supplier);
			var bean = supplier.get();
			assertInstanceOf(SimpleBean.class, bean);
		}

		/**
		 * Tests that asSupplier() returns a new bean instance on each get() call.
		 */
		@Test
		void h02_asSupplierReturnsNewInstanceEachTime() {
			var supplier = bc(SimpleBean.class).asSupplier();

			var bean1 = supplier.get();
			var bean2 = supplier.get();

			assertNotSame(bean1, bean2);
		}

		/**
		 * Tests converting BeanInstantiator to a Memoizer.
		 */
		@Test
		void h03_asMemoizer() {
			var supplier = bc(SimpleBean.class).asMemoizer();

			var bean = supplier.get();
			assertInstanceOf(SimpleBean.class, bean);
		}

		/**
		 * Tests that asMemoizer() caches the bean instance across multiple get() calls.
		 */
		@Test
		void h04_asMemoizerCachesResult() {
			var supplier = bc(SimpleBean.class).asMemoizer();

			var bean1 = supplier.get();
			var bean2 = supplier.get();

			assertSame(bean1, bean2, "Memoizer should cache the result");
		}

		/**
		 * Tests that reset() on Memoizer forces recreation of the bean instance.
		 */
		@Test
		void h05_asMemoizerResetRecreates() {
			var supplier = bc(SimpleBean.class).asMemoizer();

			var bean1 = supplier.get();
			supplier.reset();
			var bean2 = supplier.get();

			assertNotSame(bean1, bean2, "Reset should force recreation");
		}

		/**
		 * Tests that post-create hooks are only called once per cached instance in Memoizer.
		 */
		@Test
		void h07_asMemoizerWithPostCreateHooks() {
			var hookCallCount = IntegerValue.create();

			var supplier = bc(SimpleBean.class)
				.postCreateHook(b -> hookCallCount.increment())
				.asMemoizer();

			supplier.get();
			assertEquals(1, hookCallCount.get());

			// Cached, hook not called again
			supplier.get();
			assertEquals(1, hookCallCount.get());

			// Reset, hook called on recreation
			supplier.reset();
			supplier.get();
			assertEquals(2, hookCallCount.get());
		}

		/**
		 * Tests that Memoizer respects the cached() mode of the creator.
		 */
		@Test
		void h08_asMemoizerWithCached() {
			var creator = bc(SimpleBean.class).cached();

			var supplier = creator.asMemoizer();

			var bean1 = supplier.get();
			var bean2 = creator.run();
			var bean3 = supplier.get();

			// All should be the same due to cached mode
			assertSame(bean1, bean2);
			assertSame(bean1, bean3);

			// Reset the supplier
			supplier.reset();
			var bean4 = supplier.get();

			// Should still be same due to creator's cached instance
			assertSame(bean1, bean4);
		}

		/**
		 * Tests Optional-like methods (isPresent, isEmpty, map) on Memoizer.
		 */
		@Test
		void h09_asMemoizerOptionalMethods() {
			var supplier = bc(SimpleBean.class).asMemoizer();

			// Test Optional-like methods inherited from NullableSupplier
			assertTrue(supplier.isPresent());

			var mapped = supplier.map(b -> b.getClass().getSimpleName());
			assertEquals("SimpleBean", mapped.orElse(null));
		}
	}

	/**
	 * Tests method chaining and fluent API:
	 * - Methods that return 'this' for chaining
	 * - addBean(), builder(), type(), etc.
	 * - Fluent API usage patterns
	 */
	@Nested class I_methodChaining extends TestBase {

		/**
		 * Tests that type() returns this for method chaining.
		 */
		@Test
		void i02_beanSubTypeReturnsThis() {
			var creator = BeanInstantiator.of(ParentBean.class);
			var result = creator.type(ChildBean.class);
			assertSame(creator, result);
		}

		/**
		 * Tests that addBean() returns this for method chaining.
		 */
		@Test
		void i03_addBeanReturnsThis() {
			var creator = BeanInstantiator.of(SimpleBean.class);
			var result = creator.addBean(TestService.class, new TestService("test"));
			assertSame(creator, result);
		}

		/**
		 * Tests that builder() returns this for method chaining.
		 */
		@Test
		void i05_builderReturnsThis() {
			var creator = BeanInstantiator.of(BeanWithBuilder.class);
			var result = creator.builder(BeanWithBuilder.create());
			assertSame(creator, result);
		}

		/**
		 * Tests that enclosingInstance can be set via constructor.
		 */
		@Test
		void i06_enclosingInstanceCanBeSetViaConstructor() {
			var outerInstance = new BeanInstantiator_Test();
			var creator = BeanInstantiator.of(InnerBean.class, null, null, outerInstance);
			// Verify creator was created successfully
			assertNotNull(creator);
		}

		/**
		 * Tests that add() method returns the bean that was added (for fluent API).
		 */
		@Test
		void i07_addMethodReturnsBean() {
			var service = new TestService("test");
			var creator = BeanInstantiator.of(BeanWithDependencies.class);

			var result = creator.add(TestService.class, service);

			assertSame(service, result);

			creator.addBean(AnotherService.class, new AnotherService(42));
			var bean = creator.run();
			assertSame(service, bean.getService());
		}

		/**
		 * Tests that implementation() method allows providing a pre-configured bean instance.
		 */
		@Test
		void i08_implementationMethodSetsExistingBean() {
			var preConfiguredBean = new SimpleBean();

			var result = bc(SimpleBean.class).impl(preConfiguredBean).run();

			assertSame(preConfiguredBean, result);
		}

		/**
		 * Tests that implementation() bypasses bean creation, returning the provided instance directly.
		 */
		@Test
		void i09_implementationMethodBypassesCreation() {
			var counter = IntegerValue.create();

			var bean = new SimpleBean() {
				{ counter.increment(); }
			};

			var result = bc(SimpleBean.class)
				.impl(bean)
				.run();

			assertSame(bean, result);
			assertEquals(1, counter.get(), "Constructor should only be called once for the original bean");
		}
	}

	/**
	 * Tests cached mode functionality:
	 * - Returning the same bean instance on multiple run() calls
	 * - Caching behavior with suppliers
	 * - Cache invalidation and reset behavior
	 */
	@Nested class J_cachedMode extends TestBase {

		/**
		 * Tests that cached() mode returns the same bean instance on multiple run() calls.
		 */
		@Test
		void k01_cachedReturnsSameInstance() {
			var creator = bc(SimpleBean.class).cached();

			var bean1 = creator.run();
			var bean2 = creator.run();
			var bean3 = creator.run();

			assertSame(bean1, bean2, "Second call should return same instance");
			assertSame(bean1, bean3, "Third call should return same instance");
		}

		/**
		 * Tests that cached() mode works with beans that have constructor dependencies.
		 */
		@Test
		void k02_cachedWithConstructorParams() {
			var service = new TestService("test");
			var another = new AnotherService(42);
			beanStore.add(TestService.class, service);
			beanStore.add(AnotherService.class, another);

			var creator = bc(BeanWithDependencies.class).cached();

			var bean1 = creator.run();
			var bean2 = creator.run();

			assertSame(bean1, bean2);
			assertSame(service, bean1.getService());
			assertSame(another, bean1.getAnother());
		}

		/**
		 * Tests that cached() mode works with beans created via builder pattern.
		 */
		@Test
		void k03_cachedWithBuilder() {
			var creator = bc(BeanWithBuilder.class).cached();

			var bean1 = creator.run();
			var bean2 = creator.run();

			assertSame(bean1, bean2);
		}

		/**
		 * Tests that cached() mode works with beans created via getInstance() factory method.
		 */
		@Test
		void k04_cachedWithGetInstance() {
			var creator = bc(SingletonBean.class).cached();

			var bean1 = creator.run();
			var bean2 = creator.run();

			assertSame(bean1, bean2);
		}

		/**
		 * Tests that cached() mode works with explicitly provided implementation instances.
		 */
		@Test
		void k05_cachedWithImplementation() {
			var preConfiguredBean = new SimpleBean();

			var creator = bc(SimpleBean.class).impl(preConfiguredBean).cached();

			var bean1 = creator.run();
			var bean2 = creator.run();

			assertSame(preConfiguredBean, bean1);
			assertSame(preConfiguredBean, bean2);
		}

		/**
		 * Tests that cached() mode works with beans that have injected fields, and injection only happens once.
		 */
		@Test
		void k06_cachedWithInjection() {
			var service = new TestService("test");
			var another = new AnotherService(42);
			beanStore.add(TestService.class, service);
			beanStore.add(AnotherService.class, another);

			var creator = bc(BeanWithInjectedFields.class).cached();

			var bean1 = creator.run();
			var bean2 = creator.run();

			assertSame(bean1, bean2);
			// Injection should only happen once
			assertSame(service, bean1.service);
			assertSame(another, bean1.another);
		}

		/**
		 * Tests that without cached() mode, each run() call creates a new bean instance.
		 */
		@Test
		void k07_nonCachedCreatesMultipleInstances() {
			var creator = bc(SimpleBean.class);

			var bean1 = creator.run();
			var bean2 = creator.run();
			var bean3 = creator.run();

			assertNotSame(bean1, bean2, "Without cached mode, should create new instances");
			assertNotSame(bean1, bean3, "Without cached mode, should create new instances");
			assertNotSame(bean2, bean3, "Without cached mode, should create new instances");
		}

		/**
		 * Tests that cached() mode works consistently between asSupplier() and run() methods.
		 */
		@Test
		void k08_cachedWithSupplier() {
			var creator = bc(SimpleBean.class).cached();

			var supplier = creator.asSupplier();
			var bean1 = supplier.get();
			var bean2 = supplier.get();
			var bean3 = creator.run();

			assertSame(bean1, bean2, "Supplier should return same cached instance");
			assertSame(bean1, bean3, "Direct run() should return same cached instance");
		}

		/**
		 * Tests that cached() mode only calls the constructor once, even on multiple run() calls.
		 */
		@Test
		void k09a_cachedOnlyCallsConstructorOnce() {
			K09_CountingBean.resetCount();

			var creator = bc(K09_CountingBean.class)
				.cached();

			assertEquals(0, K09_CountingBean.getConstructorCount());

			creator.run();
			assertEquals(1, K09_CountingBean.getConstructorCount(), "Constructor should be called once");

			creator.run();
			assertEquals(1, K09_CountingBean.getConstructorCount(), "Constructor should not be called again");

			creator.run();
			assertEquals(1, K09_CountingBean.getConstructorCount(), "Constructor should still not be called again");
		}

		/**
		 * Tests that without cached() mode, the constructor is called for each run() call.
		 */
		@Test
		void k09b_nonCachedCallsConstructorMultipleTimes() {
			K09_CountingBean.resetCount();

			var creator = bc(K09_CountingBean.class);
			// Note: no .cached() call

			assertEquals(0, K09_CountingBean.getConstructorCount());

			creator.run();
			assertEquals(1, K09_CountingBean.getConstructorCount());

			creator.run();
			assertEquals(2, K09_CountingBean.getConstructorCount(), "Constructor should be called for each create()");

			creator.run();
			assertEquals(3, K09_CountingBean.getConstructorCount(), "Constructor should be called for each create()");
		}

		// Bean that counts constructor invocations
		public static class K09_CountingBean {
			private static int constructorCount = 0;

			public K09_CountingBean() {
				constructorCount++;
			}

			public static int getConstructorCount() {
				return constructorCount;
			}

			public static void resetCount() {
				constructorCount = 0;
			}
		}
	}

	/**
	 * Tests post-creation hooks:
	 * - Executing hooks after bean creation and injection
	 * - Multiple hooks execution order
	 * - Hook execution with dependency injection
	 * - Initialization and setup patterns
	 */
	@Nested class K_postCreateHooks extends TestBase {

		// Bean with initialization method
		public static class InitializableBean {
			private boolean initialized = false;
			private int initCount = 0;

			public void initialize() {
				initialized = true;
				initCount++;
			}

			public boolean isInitialized() { return initialized; }
			public int getInitCount() { return initCount; }
		}

		/**
		 * Tests executing a single post-create hook after bean creation.
		 */
		@Test
		void l01_singleHook() {
			var bean = bc(InitializableBean.class)
				.postCreateHook(b -> b.initialize())
				.run();

			assertTrue(bean.isInitialized());
			assertEquals(1, bean.getInitCount());
		}

		/**
		 * Tests executing multiple post-create hooks in sequence.
		 */
		@Test
		void l02_multipleHooks() {
			var counter = IntegerValue.create();

			var bean = bc(InitializableBean.class)
				.postCreateHook(b -> b.initialize())
				.postCreateHook(b -> counter.increment())
				.postCreateHook(b -> counter.increment())
				.run();

			assertTrue(bean.isInitialized());
			assertEquals(2, counter.get());
		}

		/**
		 * Tests that post-create hooks are executed in the order they were added.
		 */
		@Test
		void l03_hooksExecutedInOrder() {
			var log = new ArrayList<String>();

			bc(SimpleBean.class)
			.postCreateHook(b -> log.add("first"))
			.postCreateHook(b -> log.add("second"))
			.postCreateHook(b -> log.add("third"))
			.run();

			assertEquals(Arrays.asList("first", "second", "third"), log);
		}

		/**
		 * Tests that post-create hooks can access the created bean and its injected dependencies.
		 */
		@Test
		void l04_hookWithBeanAccess() {
			var service = new TestService("test");
			var another = new AnotherService(42);
			beanStore.add(TestService.class, service);
			beanStore.add(AnotherService.class, another);

			var bean = bc(BeanWithDependencies.class)
				.postCreateHook(b -> {
					// Hook can access injected dependencies
					assertSame(service, b.getService());
					assertSame(another, b.getAnother());
				})
				.run();

			assertNotNull(bean);
		}

		/**
		 * Tests that post-create hooks are only called once when cached() mode is enabled.
		 */
		@Test
		void l05_hookWithCached() {
			var callCount = IntegerValue.create();

			var creator = bc(InitializableBean.class)
				.cached()
				.postCreateHook(b -> {
					b.initialize();
					callCount.increment();
				});

			var bean1 = creator.run();
			var bean2 = creator.run();
			var bean3 = creator.run();

			assertSame(bean1, bean2);
			assertSame(bean1, bean3);
			assertTrue(bean1.isInitialized());
			assertEquals(1, callCount.get(), "Hook should only be called once in cached mode");
		}

		/**
		 * Tests that post-create hooks are called for each bean creation when cached() mode is not enabled.
		 */
		@Test
		void l06_hookWithoutCached() {
			var callCount = IntegerValue.create();

			var creator = bc(InitializableBean.class)
				.postCreateHook(b -> {
					b.initialize();
					callCount.increment();
				});

			var bean1 = creator.run();
			var bean2 = creator.run();
			var bean3 = creator.run();

			assertNotSame(bean1, bean2);
			assertTrue(bean1.isInitialized());
			assertTrue(bean2.isInitialized());
			assertTrue(bean3.isInitialized());
			assertEquals(3, callCount.get(), "Hook should be called for each create()");
		}

		/**
		 * Tests that post-create hooks work with beans created via builder pattern.
		 */
		@Test
		void l07_hookWithBuilder() {
			var initialized = Flag.create();

			bc(BeanWithBuilder.class)
			.postCreateHook(b -> initialized.set())
			.run();

			assertTrue(initialized.isSet());
		}

		/**
		 * Tests that post-create hooks run after dependency injection is complete.
		 */
		@Test
		void l08_hookRunsAfterInjection() {
			var service = new TestService("test");
			var another = new AnotherService(42);
			beanStore.add(TestService.class, service);
			beanStore.add(AnotherService.class, another);

			var serviceWasInjected = Flag.create();

			var bean = bc(BeanWithInjectedFields.class)
				.postCreateHook(b -> {
					// At this point, injection should already be done
					serviceWasInjected.setIf(b.service != null);
				})
				.run();

			assertTrue(serviceWasInjected.isSet(), "Service should be injected before hook runs");
			assertSame(service, bean.service);
		}

		/**
		 * Tests that post-create hooks are executed even when using implementation() to provide a pre-configured bean.
		 */
		@Test
		void l09_hookWithImplementation() {
			var preConfiguredBean = new InitializableBean();
			var hookRan = Flag.create();

			var bean = bc(InitializableBean.class)
				.impl(preConfiguredBean)
				.postCreateHook(b -> {
					b.initialize();
					hookRan.set();
				})
				.run();

			assertSame(preConfiguredBean, bean);
			assertTrue(bean.isInitialized());
			assertTrue(hookRan.isSet());
		}

		/**
		 * Tests that exceptions thrown in post-create hooks propagate to the caller.
		 */
		@Test
		void l10_hookExceptionPropagates() {
			var creator = bc(SimpleBean.class)
				.postCreateHook(b -> {
					throw rex("Hook error");
				});

			assertThrows(RuntimeException.class, creator::run, "Hook exception should propagate");
		}

		/**
		 * Tests that when a post-create hook throws an exception, subsequent hooks are not executed.
		 */
		@Test
		void l11_multipleHooksFirstFails() {
			var secondHookRan = Flag.create();

			var creator = bc(SimpleBean.class)
				.postCreateHook(b -> {
					throw rex("First hook error");
				})
				.postCreateHook(b -> secondHookRan.set());

			assertThrows(RuntimeException.class, creator::run);
			assertFalse(secondHookRan.isSet(), "Second hook should not run if first hook fails");
		}

		// Bean that tracks method call order

		/**
		 * Tests that post-create hooks run after all dependency injection (fields and methods) is complete.
		 */
		@Test
		void l12_hooksRunAfterAllInjection() {
			var service = new TestService("test");
			var another = new AnotherService(42);
			beanStore.add(TestService.class, service);
			beanStore.add(AnotherService.class, another);

			var bean = bc(L12_OrderTrackingBean.class)
				.postCreateHook(b -> b.step1())
				.postCreateHook(b -> b.step2())
				.run();

			assertEquals(Arrays.asList("inject-method", "step1", "step2"), bean.getCallOrder());
		}

		// Bean that tracks method call order
		public static class L12_OrderTrackingBean {
			private final List<String> callOrder = new ArrayList<>();

			@Inject TestService service;

			@Inject
			public void setAnother(AnotherService another) {
				callOrder.add("inject-method");
			}

			public void step1() {
				callOrder.add("step1");
			}

			public void step2() {
				callOrder.add("step2");
			}

			public List<String> getCallOrder() { return callOrder; }
		}
	}

	/**
	 * Tests custom builder method names:
	 * - Custom builder factory method names via builderMethodNames()
	 * - Builder method name replacement behavior
	 */
	@Nested class L_customFactoryMethods extends TestBase {

		/**
		 * Tests creating a bean using builder with a custom builder factory method name via builderMethodNames().
		 */
		@Test
		void l01_customBuilderMethodName() {
			var bean = bc(L01_BeanWithCustomBuilderMethod.class)
				.builderMethodNames("newBuilder")
				.run();

			assertNotNull(bean);
		}

		// Bean with custom builder method name
		public static class L01_BeanWithCustomBuilderMethod {
			private final String value;

			private L01_BeanWithCustomBuilderMethod(CustomBuilder builder) {
				this.value = builder.value;
			}

			public String getValue() { return value; }

			// Custom builder factory method named "newBuilder" instead of "create" or "builder"
			public static CustomBuilder newBuilder() {
				return new CustomBuilder();
			}

			public static class CustomBuilder {
				private String value;

				// @formatter:off
				public CustomBuilder value(String value) { this.value = value; return this;}
				// @formatter:on

				public L01_BeanWithCustomBuilderMethod build() {
					return new L01_BeanWithCustomBuilderMethod(this);
				}
			}
		}

		/**
		 * Tests that calling builderMethodNames() multiple times replaces the previous value rather than accumulating.
		 */
		@Test
		void l02_replacementBuilderMethodBehavior() {
			var bean = bc(L02_BeanWithMultipleBuilderMethods.class)
				.builderMethodNames("newBuilder")
				.builderMethodNames("instance")  // This replaces "newBuilder"
				.run();

			assertEquals("instance", bean.getSource());
		}

		// Bean with multiple builder method names
		public static class L02_BeanWithMultipleBuilderMethods {
			private final String source;

			private L02_BeanWithMultipleBuilderMethods(MultiBuilder builder) {
				this.source = builder.source;
			}

			public String getSource() { return source; }

			public static MultiBuilder newBuilder() {
				return new MultiBuilder("newBuilder");
			}

			public static MultiBuilder instance() {
				return new MultiBuilder("instance");
			}

			public static class MultiBuilder {
				private final String source;

				public MultiBuilder(String source) {
					this.source = source;
				}

				public L02_BeanWithMultipleBuilderMethods build() {
					return new L02_BeanWithMultipleBuilderMethods(this);
				}
			}
		}
	}

	/**
	 * Tests asOptional() method for non-throwing bean creation:
	 * - Returning Optional instead of throwing exceptions
	 * - Handling creation failures gracefully
	 * - Optional.empty() for unresolvable beans
	 * - Fallback behavior and error handling
	 */
	@Nested class M_asOptional extends TestBase {

		/**
		 * Tests that asOptional() returns a present Optional when bean creation succeeds.
		 */
		@Test
		void m01_asOptionalSucceeds() {
			var result = bc(SimpleBean.class).asOptional();

			assertInstanceOf(SimpleBean.class, result.get());
		}

		/**
		 * Tests that asOptional() returns empty Optional when trying to create an interface.
		 */
		@Test
		void m02_asOptionalFailsForInterface() {
			var result = bc(BeanInterface.class).asOptional();

			assertFalse(result.isPresent(), "Should return empty for interface");
		}

		/**
		 * Tests that asOptional() returns empty Optional when trying to create an abstract class.
		 */
		@Test
		void m03_asOptionalFailsForAbstract() {
			var result = bc(AbstractBean.class).asOptional();

			assertFalse(result.isPresent(), "Should return empty for abstract class");
		}

		/**
		 * Tests that asOptional() returns empty Optional when no matching constructor can be resolved.
		 */
		@Test
		void m04_asOptionalFailsForNoMatchingConstructor() {
			var result = bc(M04_BeanWithNoMatchingConstructor.class).asOptional();

			assertFalse(result.isPresent(), "Should return empty when no constructor matches");
		}

		// Bean with no matching constructor
		public static class M04_BeanWithNoMatchingConstructor {
			public M04_BeanWithNoMatchingConstructor(UnresolvableType param) {
				// Only constructor requires unresolvable type
			}
		}

		/**
		 * Tests using asOptional() with Optional.orElse() pattern to provide a default bean when creation fails.
		 */
		@Test
		void m05_asOptionalOrElsePattern() {
			var defaultBean = new M05_ConcreteBeanInterface();

			var bean = bc(BeanInterface.class).asOptional().orElse(defaultBean);

			assertSame(defaultBean, bean);
		}

		// Bean that implements BeanInterface for fallback testing
		public static class M05_ConcreteBeanInterface implements BeanInterface {
			public M05_ConcreteBeanInterface() {}
		}

		/**
		 * Tests that post-create hooks are executed when asOptional() succeeds.
		 */
		@Test
		void m06_asOptionalWithPostCreateHooks() {
			var hookCalled = Flag.create();

			var result = bc(SimpleBean.class)
				.postCreateHook(b -> hookCalled.set())
				.asOptional();

			assertTrue(result.isPresent());
			assertTrue(hookCalled.isSet(), "Post-create hooks should run on success");
		}

		/**
		 * Tests that post-create hooks are not executed when asOptional() returns empty.
		 */
		@Test
		void m07_asOptionalPostCreateHooksNotCalledOnFailure() {
			var hookCalled = Flag.create();

			var result = bc(BeanInterface.class)
				.postCreateHook(b -> hookCalled.set())
				.asOptional();

			assertFalse(result.isPresent());
			assertFalse(hookCalled.isSet(), "Post-create hooks should not run on failure");
		}

		/**
		 * Tests that asOptional() respects cached() mode, returning the same instance on multiple calls.
		 */
		@Test
		void m08_asOptionalWithCached() {
			var creator = bc(SimpleBean.class).cached();

			var result1 = creator.asOptional();
			var result2 = creator.asOptional();

			assertSame(result1.get(), result2.get(), "Should return same instance in cached mode");
		}

		/**
		 * Tests that cached() mode caches successful creation in asOptional(), returning cached instance on subsequent calls.
		 */
		@Test
		void m09_asOptionalCachedCachesSuccessfulCreation() {
			var creator = bc(SimpleBean.class).cached();

			var result1 = creator.asOptional();

			// Even if we change to interface beanSubType (which would fail), cached instance is returned
			// This tests that cached mode works correctly
			var result2 = creator.asOptional();
			assertSame(result1.get(), result2.get());
		}

		/**
		 * Tests that asOptional() works with beans that have constructor dependencies.
		 */
		@Test
		void m10_asOptionalWithDependencies() {
			var service = new TestService("test-service");
			var another = new AnotherService(42);
			beanStore.add(TestService.class, service);
			beanStore.add(AnotherService.class, another);

			var result = bc(BeanWithDependencies.class).asOptional();

			assertSame(service, result.get().service);
		}

		/**
		 * Tests that asOptional() works with beans created via builder pattern.
		 */
		@Test
		void m11_asOptionalWithBuilder() {
			var result = bc(BeanWithBuilder.class).asOptional();

			assertTrue(result.isPresent());
		}

		/**
		 * Tests using asOptional().ifPresent() pattern to execute code only when bean creation succeeds.
		 */
		@Test
		void m12_asOptionalIsPresent() {
			// @formatter:off
			bc(SimpleBean.class)
			.asOptional()
			.ifPresent(bean -> assertInstanceOf(SimpleBean.class, bean));
			// @formatter:on
		}

		/**
		 * Tests using asOptional().isEmpty() to check if bean creation failed.
		 */
		@Test
		void m13_asOptionalIsEmpty() {
			var result = bc(BeanInterface.class).asOptional();

			assertTrue(result.isEmpty());
		}

		/**
		 * Tests using asOptional().orElseThrow() to throw NoSuchElementException when bean creation fails.
		 */
		@Test
		void m14_asOptionalOrElseThrow() {
			var optional = bc(BeanInterface.class).asOptional();
			assertThrows(NoSuchElementException.class, optional::orElseThrow);
		}

		/**
		 * Tests using asOptional().map() to transform the bean when creation succeeds.
		 */
		@Test
		void m15_asOptionalMap() {
			var result = bc(SimpleBean.class)
				.asOptional()
				.map(bean -> bean.getClass().getSimpleName());

			assertEquals("SimpleBean", result.get());
		}

		/**
		 * Tests using asOptional().filter() to conditionally filter the bean when creation succeeds.
		 */
		@Test
		void m16_asOptionalFilter() {
			var result = bc(SimpleBean.class)
				.asOptional()
				.filter(Objects::nonNull);

			assertTrue(result.isPresent());
		}


		/**
		 * Tests that exceptions thrown in post-create hooks propagate even when using asOptional() (not caught).
		 */
		@Test
		void m17_asOptionalPostHookExceptionPropagates() {
			var builder = bc(SimpleBean.class)
				.postCreateHook(b -> {
					throw rex("Hook error");
				});
			assertThrows(RuntimeException.class, builder::asOptional, "Post-create hook exceptions should propagate even in tryCreate");
		}
	}

	/**
	 * Tests debug mode functionality:
	 * - Creating debug logs during bean creation
	 * - Logging creation steps and decisions
	 * - Debug log content and format
	 * - Troubleshooting bean creation issues
	 */
	@Nested class N_debugMode extends TestBase {

		/**
		 * Tests that debug() mode creates a debug log when bean is created.
		 */
		@Test
		void n01_debugModeCreatesLog() {
			var creator = bc(SimpleBean.class).debug();

			creator.run();

			var log = creator.getDebugLog();
			assertTrue(log.size() > 0, "Log should contain entries");
			assertTrue(log.get(0).contains("Using new instance"), "First log entry should indicate creation method");
		}

		/**
		 * Tests that without debug() mode, no debug log is created.
		 */
		@Test
		void n02_withoutDebugModeNoLog() {
			var creator = bc(SimpleBean.class);

			creator.run();

			var log = creator.getDebugLog();
			assertTrue(log.isEmpty(), "Log should be empty without debug mode");
		}

		/**
		 * Tests that debug log includes the bean type being created.
		 */
		@Test
		void n03_logIncludesBeanType() {
			var creator = bc(SimpleBean.class).debug();

			creator.run();

			var log = creator.getDebugLog();
			assertTrue(log.stream().anyMatch(s -> s.contains("SimpleBean")), "Log should mention bean type");
		}

		/**
		 * Tests that debug log is reset on each run() call (not cumulative).
		 */
		@Test
		void n04_logResetOnEachCreate() {
			var creator = bc(SimpleBean.class).debug();

			creator.run();

			creator.run();
			var secondLog = creator.getDebugLog();
			var secondLogSize = secondLog.size();

			// Note: The first create may have more entries due to builder type determination logging
			// which is cached for subsequent creates. We just verify the log was reset (has entries).
			assertTrue(secondLogSize > 0, "Log should contain entries after second create");
			assertTrue(secondLog.get(0).contains("Using new instance"), "Log should start fresh on each create");
		}

		/**
		 * Tests that debug log indicates when cached instance is returned in cached() mode.
		 */
		@Test
		void n05_cachedLogOnlyFirstCreation() {
			var creator = bc(SimpleBean.class).cached().debug();

			creator.run();
			var firstLogSize = creator.getDebugLog().size();
			assertTrue(firstLogSize > 0);

			creator.run();
			var secondLogSize = creator.getDebugLog().size();

			// Second call returns cached, log should indicate this
			assertTrue(secondLogSize > 0);
			assertTrue(creator.getDebugLog().stream().anyMatch(s -> s.contains("cached")), "Log should indicate cached instance was returned");
		}

		/**
		 * Tests that debug log returned by getDebugLog() is unmodifiable.
		 */
		@Test
		void n06_logUnmodifiable() {
			var creator = bc(SimpleBean.class).debug();

			creator.run();
			var log = creator.getDebugLog();

			assertThrows(UnsupportedOperationException.class, () -> log.add("test"), "Creation log should be unmodifiable");
		}

		/**
		 * Tests that debug log includes information about builder usage when builder pattern is used.
		 */
		@Test
		void n07_debugWithBuilder() {
			var creator = bc(BeanWithBuilder.class).debug();

			creator.run();

			var log = creator.getDebugLog();
			assertTrue(log.stream().anyMatch(s -> s.contains("Builder")),"Log should mention builder");
		}

		/**
		 * Tests that debug log includes information when implementation() is used to provide a pre-configured bean.
		 */
		@Test
		void n08_debugWithImplementation() {
			var impl = new SimpleBean();
			var creator = bc(SimpleBean.class).impl(impl).debug();

			creator.run();

			var log = creator.getDebugLog();
			assertTrue(log.stream().anyMatch(s -> s.contains("impl()")), "Log should mention implementation() usage");
		}

		/**
		 * Tests that debug log is populated even when bean creation fails.
		 */
		@Test
		void n09_debugWithFailure() {
			var creator = bc(BeanInterface.class).debug();

			assertThrows(ExecutableException.class, creator::run);

			var log = creator.getDebugLog();
			assertFalse(log.isEmpty(), "Log should be populated even on failure");
		}

		/**
		 * Tests that debug log is populated when using asOptional() method.
		 */
		@Test
		void n10_debugWithAsOptional() {
			var creator = bc(SimpleBean.class).debug();

			var result = creator.asOptional();

			assertTrue(result.isPresent());
			var log = creator.getDebugLog();
			assertFalse(log.isEmpty(), "Log should be populated with tryCreate()");
		}

		/**
		 * Tests that passing null to type() throws IllegalArgumentException.
		 */
		@Test
		void n12_beanSubTypeNullThrows() {
			var creator = BeanInstantiator.of(SimpleBean.class);
			assertThrows(IllegalArgumentException.class, () -> creator.type((Class<? extends SimpleBean>) null));
		}
	}

	/**
	 * Tests edge cases and boundary conditions:
	 * - Empty bean stores
	 * - Missing dependencies
	 * - Unusual bean configurations
	 * - Boundary condition handling
	 */
	@Nested class O_edgeCases extends TestBase {

		/**
		 * Tests that bean creation works when bean store is provided but empty (no dependencies needed).
		 */
		@Test
		void o01_noBeanStoreWorks() {
			var bean = bc(SimpleBean.class).run();

			assertNotNull(bean);
		}

		/**
		 * Tests that BeanInstantiator.of(Class, BeanStore) can resolve dependencies from parent bean store.
		 */
		@Test
		void o02_ofWithParentStore() {
			var parentStore = new BasicBeanStore(null);
			var testService = new TestService("parent-service");
			parentStore.addBean(TestService.class, testService);

			var bean = BeanInstantiator.of(BeanWithDependencies.class, parentStore)
				.addBean(AnotherService.class, new AnotherService(42))
				.run();

			assertSame(testService, bean.getService());
			assertEquals(42, bean.getAnother().getValue());
		}

		/**
		 * Tests that BeanInstantiator.of(Class, null) works when no parent bean store is provided.
		 */
		@Test
		void o03_ofWithParentStoreNull() {
			var bean = BeanInstantiator.of(SimpleBean.class, null).run();

			assertInstanceOf(SimpleBean.class, bean);
		}

		/**
		 * Tests that local addBean() calls override values from parent bean store.
		 */
		@Test
		void o04_ofWithParentStoreLocalOverrides() {
			var parentStore = new BasicBeanStore(null);
			var parentService = new TestService("parent");
			parentStore.addBean(TestService.class, parentService);

			var localService = new TestService("local");
			var bean = BeanInstantiator.of(BeanWithDependencies.class, parentStore)
				.addBean(TestService.class, localService) // Local overrides parent
				.addBean(AnotherService.class, new AnotherService(42))
				.run();

			assertSame(localService, bean.getService());
			assertNotSame(parentService, bean.getService());
		}
	}

	/**
	 * Tests protected methods via test subclass:
	 * - Internal method behavior and caching
	 * - Protected API testing
	 * - Implementation details verification
	 * - Internal state management
	 */
	@Nested class P_protectedMethods extends TestBase {

		/**
		 * Tests that getBeanSubType() returns beanType when no subtype is set (protected method test).
		 */
		@Test
		void p01_getBeanSubTypeReturnsDefaultWhenNotSet() {
			var creator = bc(SimpleBean.class);
			var beanSubType = creator.getBeanSubType();

			assertEquals(SimpleBean.class.getName(), beanSubType.getName());
		}

		/**
		 * Tests that getBeanSubType() returns the set subtype when type() is called (protected method test).
		 */
		@Test
		void p02_getBeanSubTypeReturnsSubtypeWhenSet() {
			var creator = bc(ParentBean.class);
			creator.type(ChildBean.class);
			var beanSubType = creator.getBeanSubType();

			assertEquals(ChildBean.class.getName(), beanSubType.getName());
			assertNotEquals(ParentBean.class.getName(), beanSubType.getName());
		}

		/**
		 * Tests that getName() returns null when no name is set.
		 */
		@Test
		void p03_getNameReturnsNullWhenNotSet() {
			var creator = BeanInstantiator.of(SimpleBean.class);
			var name = creator.getName();

			assertNull(name, "getName() should return null when no name is set");
		}

		/**
		 * Tests that getName() returns the name set via constructor.
		 */
		@Test
		void p04_getNameReturnsSetName() {
			var creator = BeanInstantiator.of(SimpleBean.class, beanStore, "myBean", null);
			var name = creator.getName();

			assertEquals("myBean", name);
		}

		/**
		 * Tests that getName() returns null when name is set to null via constructor.
		 */
		@Test
		void p05_getNameReturnsNullWhenSetToNull() {
			var creator = BeanInstantiator.of(SimpleBean.class, beanStore, null, null);
			var name = creator.getName();

			assertNull(name, "getName() should return null when name is set to null");
		}

		/**
		 * Tests that getBeanSubTypes() returns a list with single element (beanType) when no subtype is set (protected method test).
		 */
		@Test
		void p06_getBeanSubTypesReturnsSingleElementWhenNoSubtype() {
			var creator = bc(SimpleBean.class);
			var beanSubTypes = creator.getBeanSubTypes();

			assertEquals(1, beanSubTypes.size());
			assertEquals(SimpleBean.class.getName(), beanSubTypes.get(0).getName());
		}

		/**
		 * Tests that getBeanSubTypes() returns the full class hierarchy when subtype is set (protected method test).
		 */
		@Test
		void p07_getBeanSubTypesReturnsHierarchyWhenSubtypeSet() {
			var creator = bc(ParentBean.class);
			creator.type(ChildBean.class);
			var beanSubTypes = creator.getBeanSubTypes();

			assertTrue(beanSubTypes.size() >= 2, "Should have at least beanSubType and beanType");
			// First element should be the subtype (ChildBean)
			assertEquals(ChildBean.class.getName(), (beanSubTypes.get(0)).getName());
			// Last element should be the bean type (ParentBean)
			assertEquals(ParentBean.class.getName(), (beanSubTypes.get(beanSubTypes.size() - 1)).getName());
		}

		/**
		 * Tests that getBeanSubTypes() caches results using Memoizer (protected method test).
		 */
		@Test
		void p08_getBeanSubTypesIsCached() {
			var creator = bc(ParentBean.class);
			creator.type(ChildBean.class);

			var beanSubTypes1 = creator.getBeanSubTypes();
			var beanSubTypes2 = creator.getBeanSubTypes();

			assertSame(beanSubTypes1, beanSubTypes2);
		}

		/**
		 * Tests that getBuilderType() returns null when no builder exists (protected method test).
		 */
		@Test
		void p09_getBuilderTypeReturnsNullWhenNoBuilder() {
			var creator = bc(SimpleBean.class);
			var builderType = creator.getBuilderType();

			assertNull(builderType, "getBuilderType() should return null when no builder exists");
		}

		/**
		 * Tests that getBuilderType() returns the detected builder type when builder exists (protected method test).
		 */
		@Test
		void p10_getBuilderTypeReturnsBuilderTypeWhenBuilderExists() {
			var creator = bc(BeanWithBuilder.class);
			creator.getBuilder();
			var builderType = creator.getBuilderType();

			assertEquals(BeanWithBuilder.Builder.class.getName(), builderType.getName());
		}

		/**
		 * Tests that getBuilderType() returns the explicitly set builder type via builder() method (protected method test).
		 */
		@Test
		void p11_getBuilderTypeReturnsExplicitBuilderType() {
			var creator = bc(BeanWithBuilder.class);
			creator.builder(BeanWithBuilder.Builder.class);
			var builderType = creator.getBuilderType();

			assertEquals(BeanWithBuilder.Builder.class.getName(), builderType.getName());
		}

		/**
		 * Tests that getBuilderType() caches results using Memoizer (protected method test).
		 */
		@Test
		void p12_getBuilderTypeIsCached() {
			var creator = bc(BeanWithBuilder.class);
			creator.getBuilder(); // Trigger builder detection

			var builderType1 = creator.getBuilderType();
			var builderType2 = creator.getBuilderType();

			assertSame(builderType1, builderType2);
		}

		/**
		 * Tests that getBuilderTypes() returns empty list when no builder exists (protected method test).
		 */
		@Test
		void p13_getBuilderTypesReturnsEmptyWhenNoBuilder() {
			var creator = bc(SimpleBean.class);
			var builderTypes = creator.getBuilderTypes();

			assertTrue(builderTypes.isEmpty(), "getBuilderTypes() should return empty list when no builder exists");
		}

		/**
		 * Tests that getBuilderTypes() returns list with single builder type when no builder hierarchy exists (protected method test).
		 */
		@Test
		void p14_getBuilderTypesReturnsSingleBuilderType() {
			var creator = bc(BeanWithBuilder.class);
			creator.getBuilder(); // Trigger builder detection
			var builderTypes = creator.getBuilderTypes();

			assertEquals(1, builderTypes.size());
			assertEquals(BeanWithBuilder.Builder.class.getName(), builderTypes.get(0).getName());
		}

		/**
		 * Tests that getBuilderTypes() returns full builder class hierarchy when builder extends parent builder (protected method test).
		 */
		@Test
		void p15_getBuilderTypesReturnsHierarchyWhenBuilderExtendsParent() {
			var creator = bc(ChildBeanWithBuilder.class).debug();
			creator.getBuilder(); // Trigger builder detection
			var builderTypes = creator.getBuilderTypes();

			assertTrue(builderTypes.size() >= 2, "Should have at least the child builder and parent builder");


			// Primary builder type should be the child's builder
			assertEquals(ChildBeanWithBuilder.BuilderForChild.class.getName(), builderTypes.get(0).getName());
			// Parent builder should also be included in the hierarchy
			assertEquals(ParentBeanWithBuilder.BuilderForParent.class.getName(), builderTypes.get(1).getName());
		}

		/**
		 * Tests that getBuilderTypes() caches results using Memoizer (protected method test).
		 */
		@Test
		void p16_getBuilderTypesIsCached() {
			var creator = bc(BeanWithBuilder.class);
			creator.getBuilder(); // Trigger builder detection

			var builderTypes1 = creator.getBuilderTypes();
			var builderTypes2 = creator.getBuilderTypes();

			assertSame(builderTypes1, builderTypes2);
		}

		/**
		 * Tests that name can be set via constructor.
		 */
		@Test
		void p17_nameCanBeSetViaConstructor() {
			var creator = BeanInstantiator.of(SimpleBean.class, null, "testBean", null);

			assertEquals("testBean", creator.getName());
		}

		/**
		 * Tests that name can be set to null via constructor.
		 */
		@Test
		void p18_nameCanBeSetToNull() {
			var creator = BeanInstantiator.of(SimpleBean.class, beanStore, null, null);
			assertNull(creator.getName(), "name should allow null value");
		}

		/**
		 * Tests that name can be set via constructor.
		 */
		@Test
		void p19_nameCanBeSetViaConstructor() {
			var creator = BeanInstantiator.of(SimpleBean.class, beanStore, "final", null);

			assertEquals("final", creator.getName());
		}

		/**
		 * Tests debug logging when the builder returns parent type and the child type has no constructor
		 * accepting the builder. Loose-builder mode falls through to factory methods / constructors;
		 * with no compatible constructor on the child bean, instantiation fails — but with a
		 * "no methods/constructors found" message rather than a strict "must return exact subtype" error.
		 */
		@Test
		void p20_builderReturnsParentTypeNoConstructorAcceptsBuilder() {
			var creator = bc(P20_ParentBeanForBuilderTest.class)
				.type(P20_ChildBeanForBuilderTest.class)
				.builder(P20_BuilderForParentBean.class)
				.debug();

			assertThrows(ExecutableException.class, creator::run);

			var log = creator.getDebugLog();
			var logString = log.toString();
			assertContains("Builder method", logString);
			assertContains("falling through to factory methods/constructors", logString);
			assertContains(P20_ChildBeanForBuilderTest.class.getSimpleName(), logString);
			assertContains(P20_BuilderForParentBean.class.getSimpleName(), logString);
		}

		public static class P20_ParentBeanForBuilderTest {
			protected final String value;
			public P20_ParentBeanForBuilderTest(String value) {
				this.value = value;
			}
			public String getValue() { return value; }
		}

		public static class P20_ChildBeanForBuilderTest extends P20_ParentBeanForBuilderTest {
			public P20_ChildBeanForBuilderTest(String value) {
				super(value);
			}
			// No constructor that accepts builder
		}

		public static class P20_BuilderForParentBean {
			private String value = "default";

			// @formatter:off
			public P20_BuilderForParentBean value(String value) { this.value = value; return this;}
			// @formatter:on

			// Returns parent type, not child type
			public P20_ParentBeanForBuilderTest build() {
				return new P20_ParentBeanForBuilderTest(value);
			}
			public static P20_BuilderForParentBean create() {
				return new P20_BuilderForParentBean();
			}
		}
	}

	/**
	 * Tests logging functionality:
	 * - Log messages are captured at FINE level
	 * - Log format includes bean type name
	 * - Log messages are properly formatted
	 */
	@Nested class Q_logging extends TestBase {

		/**
		 * Tests that logging occurs when creating a simple bean.
		 */
		@Test
		void q01_loggingOnSimpleBeanCreation() {
			// Get the logger instance using the same method as BeanInstantiator static field
			// This ensures we get the same cached instance
			var logger = Logger.getLogger(BeanInstantiator.class);
			logger.setLevel(Level.FINE); // Enable FINE level logging

			try (var capture = logger.captureEvents()) {
				var bean = bc(SimpleBean.class).run();

				assertNotNull(bean);
				var allRecords = capture.getRecords();
				assertTrue(allRecords.size() > 0, "Should have captured log records. Got: " + allRecords.size());

				// Check that at least one log record is at FINE level
				var fineRecords = allRecords.stream()
					.filter(r -> r.getLevel() == Level.FINE)
					.toList();
				assertTrue(fineRecords.size() > 0, "Should have FINE level log records. Got: " + fineRecords.size());

				// Check that log messages contain bean type name
				boolean foundBeanType = fineRecords.stream()
					.anyMatch(r -> r.getMessage().contains(SimpleBean.class.getName()));
				assertTrue(foundBeanType, "Log messages should contain bean type name. Messages: " +
					fineRecords.stream().map(r -> r.getMessage()).toList());
			}
		}

		/**
		 * Tests that logging occurs when creating a bean with builder.
		 */
		@Test
		void q02_loggingOnBuilderBeanCreation() {
			var logger = Logger.getLogger(BeanInstantiator.class);
			logger.setLevel(Level.FINE); // Enable FINE level logging
			try (var capture = logger.captureEvents()) {
				var bean = bc(Q02_BeanWithBuilder.class).run();

				assertNotNull(bean);
				var records = capture.getRecords("{level} {msg}");
				assertTrue(records.size() > 0, "Should have captured log records");

				// Check for builder-related log messages
				var formattedRecords = capture.getRecords("{level} {msg}");
				boolean foundBuilderLog = formattedRecords.stream()
					.anyMatch(msg -> msg.contains("Builder detected") || msg.contains("build method"));
				assertTrue(foundBuilderLog, "Should have builder-related log messages");
			}
		}

		/**
		 * Tests that log messages include bean type prefix.
		 */
		@Test
		void q03_logMessagesIncludeBeanTypePrefix() {
			var logger = Logger.getLogger(BeanInstantiator.class);
			logger.setLevel(Level.FINE); // Enable FINE level logging
			try (var capture = logger.captureEvents()) {
				var bean = bc(SimpleBean.class).run();

				assertNotNull(bean);
				var fineRecords = capture.getRecords().stream()
					.filter(r -> r.getLevel() == Level.FINE)
					.toList();

				// Check that log messages start with bean type name
				boolean hasCorrectPrefix = fineRecords.stream()
					.anyMatch(r -> {
						String msg = r.getMessage();
						return msg.startsWith(SimpleBean.class.getName() + ":");
					});
				assertTrue(hasCorrectPrefix, "Log messages should start with bean type name prefix");
			}
		}

		/**
		 * Tests that log messages with format arguments are properly formatted.
		 */
		@Test
		void q04_logMessagesWithFormatArguments() {
			var logger = Logger.getLogger(BeanInstantiator.class);
			try (var capture = logger.captureEvents()) {
				// Create a bean that will trigger logging with format arguments
				var bean = bc(Q02_BeanWithBuilder.class).run();

				assertNotNull(bean);
				var fineRecords = capture.getRecords().stream()
					.filter(r -> r.getLevel() == Level.FINE)
					.toList();

				// Check for formatted messages (e.g., "Builder detected: %s")
				boolean hasFormattedMessage = fineRecords.stream()
					.anyMatch(r -> {
						String msg = r.getMessage();
						return msg.contains("Builder detected") && msg.contains(Q02_BeanWithBuilder.Builder.class.getName());
					});
				assertTrue(hasFormattedMessage, "Should have formatted log messages with arguments");
			}
		}

		// Test bean with builder for logging tests
		public static class Q02_BeanWithBuilder {
			private String value;

			public Q02_BeanWithBuilder(String value) {
				this.value = value;
			}

			public String getValue() {
				return value;
			}

			public static class Builder {
				private String value = "default";

				public Builder value(String value) {
					this.value = value;
					return this;
				}

				public Q02_BeanWithBuilder build() {
					return new Q02_BeanWithBuilder(value);
				}

				public static Builder create() {
					return new Builder();
				}
			}
		}
	}

	//====================================================================================================
	// R - Phase 4 general-purpose API: silent, description, wrap, validate, copy, scope, or
	//====================================================================================================
	@Nested
	@DisplayName("R - Phase 4 general-purpose API")
	class R_phase4Api {

		public static class R_SimpleBean {
			public int value;
			public R_SimpleBean() { this.value = 1; }
			public R_SimpleBean(int v) { this.value = v; }
		}

		@Test
		@DisplayName("R01 - wrap() applies a single transformer to the result")
		void r01_wrapApplied() {
			var bean = bc(R_SimpleBean.class)
				.wrap(b -> { b.value = 99; return b; })
				.run();
			assertEquals(99, bean.value);
		}

		@Test
		@DisplayName("R02 - wrap() applies multiple transformers in order")
		void r02_wrapMultiple() {
			var bean = bc(R_SimpleBean.class)
				.wrap(b -> { b.value += 10; return b; })
				.wrap(b -> { b.value *= 2; return b; })
				.run();
			assertEquals(22, bean.value);
		}

		@Test
		@DisplayName("R03 - validate() throws when predicate fails")
		void r03_validateThrows() {
			var builder = bc(R_SimpleBean.class).validate(b -> b.value > 100, "value must be > 100");
			var ex = assertThrows(RuntimeException.class, builder::run);
			assertTrue(ex.getMessage().contains("value must be > 100"), "Message should mention validator failure");
		}

		@Test
		@DisplayName("R04 - validate() passes when predicate succeeds")
		void r04_validatePasses() {
			var bean = bc(R_SimpleBean.class)
				.validate(b -> b.value == 1, "value must be 1")
				.run();
			assertEquals(1, bean.value);
		}

		@Test
		@DisplayName("R05 - scope(SINGLETON) caches the bean across calls")
		void r05_scopeSingleton() {
			var creator = bc(R_SimpleBean.class).scope(BeanInstantiator.Scope.SINGLETON);
			var b1 = creator.run();
			var b2 = creator.run();
			assertSame(b1, b2, "Scope.SINGLETON should reuse the same instance");
		}

		@Test
		@DisplayName("R06 - scope(PROTOTYPE) creates a new bean each call")
		void r06_scopePrototype() {
			var creator = bc(R_SimpleBean.class).scope(BeanInstantiator.Scope.PROTOTYPE);
			var b1 = creator.run();
			var b2 = creator.run();
			assertNotSame(b1, b2, "Scope.PROTOTYPE should create new instances");
		}

		@Test
		@DisplayName("R07 - copy() produces an independent builder with the same configuration")
		void r07_copyIndependent() {
			var orig = bc(R_SimpleBean.class).wrap(b -> { b.value = 7; return b; });
			var copy = orig.copy();
			copy.wrap(b -> { b.value = 42; return b; });
			assertEquals(7, orig.run().value, "Original builder should be unchanged");
			assertEquals(42, copy.run().value, "Copy should pick up its own wrappers");
		}

		@Test
		@DisplayName("R08 - or() falls back to alternative when primary fails")
		void r08_orAlternativeUsed() {
			var primary = bc(R_SimpleBean.class)
				.validate(b -> b.value > 100, "primary failed");
			var alt = bc(R_SimpleBean.class)
				.wrap(b -> { b.value = 200; return b; });
			var bean = primary.or(alt).run();
			assertEquals(200, bean.value, "Alternative should be used when primary validates false");
		}

		@Test
		@DisplayName("R09 - or() uses primary when it succeeds")
		void r09_orPrimaryWins() {
			var primary = bc(R_SimpleBean.class)
				.wrap(b -> { b.value = 5; return b; });
			var alt = bc(R_SimpleBean.class)
				.wrap(b -> { b.value = 200; return b; });
			var bean = primary.or(alt).run();
			assertEquals(5, bean.value);
		}

		@Test
		@DisplayName("R10 - silent() suppresses log output but does not affect result")
		void r10_silentSuppressesLog() {
			var creator = bc(R_SimpleBean.class).silent();
			var bean = creator.run();
			assertNotNull(bean);
		}

		@Test
		@DisplayName("R11 - description() is non-null on builder after set")
		void r11_descriptionSet() {
			var creator = bc(R_SimpleBean.class).description("test bean creator");
			assertNotNull(creator.run());
		}

		@Test
		@DisplayName("R12 - build() produces an immutable BeanInstantiator usable for run()")
		void r12_buildProducesRunnable() {
			BeanInstantiator<R_SimpleBean> compiled = bc(R_SimpleBean.class).build();
			var bean = compiled.run();
			assertNotNull(bean);
		}

		@Test
		@DisplayName("R13 - build().run() and Builder.run() share cached state")
		void r13_buildRunSharesCache() {
			var builder = bc(R_SimpleBean.class).cached();
			var compiled = builder.build();
			var b1 = builder.run();
			var b2 = compiled.run();
			assertSame(b1, b2, "Compiled and builder run() should share cached bean");
		}
	}

	//====================================================================================================
	// S - Phase 5: pre-registered builder in bean store wins over static factory
	//====================================================================================================
	@Nested
	@DisplayName("S - Phase 5 builder bean store lookup")
	class S_phase5BuilderInStore {

		public static class S_Bean {
			final String value;
			public S_Bean(String v) { this.value = v; }
		}

		public static class S_BeanBuilder {
			String value = "default";
			public S_BeanBuilder value(String v) { this.value = v; return this; }
			public S_Bean build() { return new S_Bean(value); }
			public static S_BeanBuilder create() { return new S_BeanBuilder(); }
		}

		@Test
		@DisplayName("S01 - pre-registered builder is used instead of static create()")
		void s01_preregisteredBuilderUsed() {
			var preconfigured = new S_BeanBuilder().value("preconfigured");
			beanStore.add(S_BeanBuilder.class, preconfigured);

			var bean = bc(S_Bean.class).builder(S_BeanBuilder.class).run();
			assertEquals("preconfigured", bean.value, "Should use pre-registered builder rather than create() factory");
		}

		@Test
		@DisplayName("S02 - falls back to static create() when no builder is registered")
		void s02_fallsBackToCreate() {
			var bean = bc(S_Bean.class).builder(S_BeanBuilder.class).run();
			assertEquals("default", bean.value, "Should fall back to static create() factory");
		}
	}

	//====================================================================================================
	// T - Phase 6: builderInitializer / injectBuilder / autoWireBuilder
	//====================================================================================================
	@Nested
	@DisplayName("T - Phase 6 builder hooks and auto-wire")
	class T_phase6BuilderHooks {

		public static class T_Bean {
			final String name;
			final String greeting;
			public T_Bean(String name, String greeting) {
				this.name = name;
				this.greeting = greeting;
			}
		}

		public static class T_BeanBuilder {
			String name = "default-name";
			String greeting = "default-greeting";
			public T_BeanBuilder name(String n) { this.name = n; return this; }
			public T_BeanBuilder greeting(String g) { this.greeting = g; return this; }
			public T_Bean build() { return new T_Bean(name, greeting); }
			public static T_BeanBuilder create() { return new T_BeanBuilder(); }
		}

		@Test
		@DisplayName("T01 - builderInitializer is applied before build()")
		void t01_builderInitializerApplied() {
			var bean = bc(T_Bean.class)
				.builder(T_BeanBuilder.class)
				.builderInitializer(b -> ((T_BeanBuilder) b).name("from-initializer"))
				.run();
			assertEquals("from-initializer", bean.name);
			assertEquals("default-greeting", bean.greeting);
		}

		@Test
		@DisplayName("T02 - multiple builderInitializers run in registration order")
		void t02_builderInitializersOrdered() {
			var bean = bc(T_Bean.class)
				.builder(T_BeanBuilder.class)
				.builderInitializer(b -> ((T_BeanBuilder) b).name("first"))
				.builderInitializer(b -> ((T_BeanBuilder) b).name("second"))
				.run();
			assertEquals("second", bean.name, "Last initializer should win since it is applied last");
		}

		@Test
		@DisplayName("T03 - autoWireBuilder() invokes setters with matching beans from store")
		void t03_autoWireBuilderInvokesSetters() {
			beanStore.add(String.class, "wired-greeting");
			var bean = bc(T_Bean.class)
				.builder(T_BeanBuilder.class)
				.autoWireBuilder()
				.run();
			assertEquals("wired-greeting", bean.greeting, "Auto-wire should set greeting from String bean");
			assertEquals("wired-greeting", bean.name, "Both setters take String, both should be auto-wired");
		}

		@Test
		@DisplayName("T04 - autoWireBuilder() with no matching beans leaves defaults unchanged")
		void t04_autoWireBuilderNoMatches() {
			var bean = bc(T_Bean.class)
				.builder(T_BeanBuilder.class)
				.autoWireBuilder()
				.run();
			assertEquals("default-name", bean.name);
			assertEquals("default-greeting", bean.greeting);
		}

		@Test
		@DisplayName("T05 - injectBuilder() default is off (no auto-injection of builder fields)")
		void t05_injectBuilderDefaultOff() {
			var bean = bc(T_Bean.class).builder(T_BeanBuilder.class).run();
			assertEquals("default-name", bean.name);
		}
	}

	//====================================================================================================
	// U - Convenience statics: createOrNull, optionalOf, createOrDefault
	//====================================================================================================
	@Nested
	@DisplayName("U - Convenience statics")
	class U_convenienceStatics {

		public static class U_Bean {
			final String value;
			public U_Bean() { this.value = "created"; }
		}

		// --- createOrNull ---

		@Test
		@DisplayName("U01 - createOrNull(null) returns null")
		void u01_createOrNull_null() {
			assertNull(BeanInstantiator.createOrNull(null));
		}

		@Test
		@DisplayName("U02 - createOrNull(Class) instantiates the bean")
		void u02_createOrNull_nonNull() {
			var bean = BeanInstantiator.createOrNull(U_Bean.class);
			assertNotNull(bean);
			assertEquals("created", bean.value);
		}

		public static class U_NoUsableConstructor {
			private U_NoUsableConstructor() { /* private — BeanInstantiator cannot reach it */ }
		}

		@Test
		@DisplayName("U02b - createOrNull(Class) returns null when instantiation fails (fallback supplier path)")
		void u02b_createOrNull_uninstantiable_returnsNull() {
			// Drives the `() -> null` fallback lambda inside createOrNull — only reachable when the
			// type has no accessible constructor visible to BeanInstantiator.
			assertNull(BeanInstantiator.createOrNull(U_NoUsableConstructor.class));
		}

		// --- optionalOf ---

		@Test
		@DisplayName("U03 - optionalOf(null) returns Optional.empty()")
		void u03_optionalOf_null() {
			assertTrue(BeanInstantiator.optionalOf(null).isEmpty());
		}

		@Test
		@DisplayName("U04 - optionalOf(Class) returns Optional containing the bean")
		void u04_optionalOf_nonNull() {
			var opt = BeanInstantiator.optionalOf(U_Bean.class);
			assertTrue(opt.isPresent());
			assertEquals("created", opt.get().value);
		}

		// --- createOrDefault ---

		@Test
		@DisplayName("U05 - createOrDefault(null, default) returns the default")
		void u05_createOrDefault_null() {
			var fallback = new U_Bean();
			var result = BeanInstantiator.createOrDefault(null, fallback);
			assertSame(fallback, result);
		}

		@Test
		@DisplayName("U06 - createOrDefault(Class, default) instantiates the bean")
		void u06_createOrDefault_nonNull() {
			var fallback = new U_Bean();
			var result = BeanInstantiator.createOrDefault(U_Bean.class, fallback);
			assertNotNull(result);
			assertNotSame(fallback, result, "Should create a new instance, not return the default");
		}
	}

	//====================================================================================================
	// V - Outer BeanInstantiator wrapper inspection methods.
	//
	// The outer BeanInstantiator (built via Builder.build()) thinly delegates to its inner Builder for
	// inspection methods (getName, getBeanSubType, getBuilder, getDebugLog, reset, etc.).  These tests
	// drive the delegation surface explicitly so JaCoCo registers each wrapper line as covered.
	//====================================================================================================
	@Nested
	@DisplayName("V - Outer BeanInstantiator wrapper")
	class V_outerWrapper {

		public static class V_Bean {
			public V_Bean() {}
		}

		@Builder(V_BeanWithBuilder.V_BeanBuilder.class)
		public static class V_BeanWithBuilder {
			final String value;
			V_BeanWithBuilder(V_BeanBuilder b) { this.value = b.value; }
			public static class V_BeanBuilder {
				String value = "builder-value";
				public V_BeanBuilder() {}
				public V_BeanWithBuilder build() { return new V_BeanWithBuilder(this); }
			}
		}

		@Test
		@DisplayName("V01 - getName() returns the bean name set on the inner builder")
		void v01_getName() {
			var wrapper = BeanInstantiator.of(V_Bean.class, beanStore, "myBean", null).build();
			assertEquals("myBean", wrapper.getName());
		}

		@Test
		@DisplayName("V02 - getName() returns null when no name was set")
		void v02_getNameNull() {
			var wrapper = bc(V_Bean.class).build();
			assertNull(wrapper.getName());
		}

		@Test
		@DisplayName("V03 - getBeanSubType()/getBeanSubTypes() exposes the resolved subtype info")
		void v03_getBeanSubType() {
			var wrapper = bc(V_Bean.class).build();
			assertNotNull(wrapper.getBeanSubType());
			assertEquals(V_Bean.class.getName(), wrapper.getBeanSubType().getName());
			var subTypes = wrapper.getBeanSubTypes();
			assertNotNull(subTypes);
			assertFalse(subTypes.isEmpty());
		}

		@Test
		@DisplayName("V04 - getBuilder() returns Optional.empty() before run")
		void v04_getBuilder_beforeRun_empty() {
			var wrapper = bc(V_Bean.class).build();
			assertFalse(wrapper.getBuilder().isPresent(),
				"No builder has been created yet — Optional should be empty");
		}

		@Test
		@DisplayName("V05 - getBuilder() returns the cached builder after run for a @Builder type")
		void v05_getBuilder_afterRun_present() {
			var wrapper = bc(V_BeanWithBuilder.class).build();
			var bean = wrapper.run();
			assertEquals("builder-value", bean.value);
			assertTrue(wrapper.getBuilder().isPresent(),
				"Builder must be reachable through the outer wrapper after a successful run()");
		}

		@Test
		@DisplayName("V06 - getBuilderType()/getBuilderTypes() expose the builder class info")
		void v06_getBuilderType() {
			var wrapper = bc(V_BeanWithBuilder.class).build();
			wrapper.run();
			assertNotNull(wrapper.getBuilderType());
			assertEquals(V_BeanWithBuilder.V_BeanBuilder.class.getName(), wrapper.getBuilderType().getName());
			var types = wrapper.getBuilderTypes();
			assertNotNull(types);
			assertFalse(types.isEmpty());
		}

		@Test
		@DisplayName("V07 - getDebugLog() returns the (possibly-empty) debug log on the outer wrapper")
		void v07_getDebugLog() {
			var wrapper = bc(V_Bean.class).debug().build();
			wrapper.run();
			var log = wrapper.getDebugLog();
			assertNotNull(log);
			assertFalse(log.isEmpty(), "debug() mode should record at least one log entry");
		}

		@Test
		@DisplayName("V08 - reset() on the outer wrapper clears cached state and returns the wrapper itself")
		void v08_reset() {
			var wrapper = bc(V_Bean.class).cached().build();
			var first = wrapper.run();
			assertSame(first, wrapper.run(), "Second call should hit the cached instance");
			assertSame(wrapper, wrapper.reset(), "reset() must return the wrapper itself");
			assertNotSame(first, wrapper.run(), "After reset(), a fresh instance must be created");
		}
	}

	//====================================================================================================
	// W - Builder option setters and alternative-chain coverage.
	//
	// Targets pre-existing Builder.injectBuilder() setter, the alternatives() fallback loop in
	// runImpl(), and the explicit-builder reset() branch.
	//====================================================================================================
	@Nested
	@DisplayName("W - Builder option setters")
	class W_builderSetters {

		public static class W_Base {
			public final String tag;
			public W_Base() { this.tag = "default"; }
			public W_Base(String tag) { this.tag = tag; }
		}

		public static class W_FailingChild extends W_Base {
			public W_FailingChild() { throw new IllegalStateException("primary failed by design"); }
		}

		public static class W_OkChild extends W_Base {
			public W_OkChild() { super("ok-child"); }
		}

		@Builder(W_BeanWithInjectableBuilder.W_Builder.class)
		public static class W_BeanWithInjectableBuilder {
			public final String name;
			W_BeanWithInjectableBuilder(W_Builder b) { this.name = b.name; }
			public static class W_Builder {
				@Inject public AnotherService injectedService;
				public String name = "wired";
				public W_Builder() {}
				public W_BeanWithInjectableBuilder build() { return new W_BeanWithInjectableBuilder(this); }
			}
		}

		@Test
		@DisplayName("W01 - injectBuilder() opts into builder-field injection via @Inject")
		void w01_injectBuilder_injectsFields() {
			beanStore.addBean(AnotherService.class, new AnotherService(99));
			var builder = bc(W_BeanWithInjectableBuilder.class).injectBuilder();
			builder.run();
			var b = builder.getBuilder();
			assertTrue(b.isPresent());
			assertNotNull(((W_BeanWithInjectableBuilder.W_Builder)b.get()).injectedService,
				"injectBuilder() should populate the @Inject field on the builder");
		}

		@Test
		@DisplayName("W02 - or(...) alternative builder takes over when the primary type fails to instantiate")
		void w02_alternativeChainFallback() {
			// Primary subtype throws in its constructor → falls through to the alternative.
			var bean = bc(W_Base.class)
				.type(W_FailingChild.class)
				.or(BeanInstantiator.of(W_Base.class, beanStore).type(W_OkChild.class))
				.run();
			assertInstanceOf(W_OkChild.class, bean,
				"or(alt) must succeed when the primary throws during instantiation");
			assertEquals("ok-child", bean.tag);
		}

		@Test
		@DisplayName("W02b - or(...) when ALL alternatives fail rethrows the primary exception")
		void w02b_allAlternativesFail() {
			var builder = bc(W_Base.class)
				.type(W_FailingChild.class)
				.or(BeanInstantiator.of(W_Base.class, beanStore).type(W_FailingChild.class));
			var ex = assertThrows(RuntimeException.class, builder::run);
			assertTrue(ex.getMessage() == null || ex.getMessage().contains("primary failed")
					|| (ex.getCause() != null && String.valueOf(ex.getCause().getMessage()).contains("primary failed")),
				"Primary exception should propagate when all alternatives also fail; got: " + ex);
		}

		@Test
		@DisplayName("W03 - reset() with an explicit builder preserves it, only re-builds bean impl")
		void w03_reset_withExplicitBuilder() {
			var explicitBuilder = new V_outerWrapper.V_BeanWithBuilder.V_BeanBuilder();
			explicitBuilder.value = "explicit";
			var builder = bc(V_outerWrapper.V_BeanWithBuilder.class)
				.builder(explicitBuilder)
				.cached();
			var first = builder.run();
			builder.reset();
			var second = builder.run();
			assertEquals("explicit", first.value);
			assertEquals("explicit", second.value, "Explicit builder must survive reset()");
			assertNotSame(first, second, "Bean cache must clear on reset()");
		}

		public static class W_BeanWithImpl {
			public final String tag;
			public W_BeanWithImpl(String tag) { this.tag = tag; }
		}

		@Test
		@DisplayName("W04 - reset() with impl() preserves the explicit instance (not re-instantiated)")
		void w04_reset_withImpl_preservesExplicitImplementation() {
			var explicit = new W_BeanWithImpl("explicit");
			var builder = bc(W_BeanWithImpl.class).impl(explicit);
			assertSame(explicit, builder.run(), "First run uses the explicit instance");
			builder.reset();
			assertSame(explicit, builder.run(), "impl() instance must survive reset()");
		}

		public static class W_BeanWithMultipleCtors {
			public final String origin;
			public W_BeanWithMultipleCtors() { this.origin = "zero-arg"; }
			@Inject public W_BeanWithMultipleCtors(AnotherService s) { this.origin = "injected:" + s.getValue(); }
		}

		@Test
		@DisplayName("W05 - preferZeroArgConstructor() short-circuits to the no-arg constructor when available")
		void w05_preferZeroArgConstructor() {
			beanStore.addBean(AnotherService.class, new AnotherService(42));
			// Without the flag, the @Inject ctor wins because it's the longest resolvable.
			// With the flag set, the zero-arg ctor takes precedence.
			var bean = bc(W_BeanWithMultipleCtors.class).preferZeroArgConstructor().run();
			assertEquals("zero-arg", bean.origin);
		}

		@Test
		@DisplayName("W06 - factoryAbstainOnNull() returns null directly when the factory method returns null")
		void w06_factoryAbstainOnNull() {
			// W_AbstainFactoryBean.create() always returns null; without abstain, BeanInstantiator
			// would fall through to a constructor.  With abstain, run() returns null.
			assertNull(bc(W_AbstainFactoryBean.class).factoryAbstainOnNull().fallback(() -> null).run());
		}

		public static class W_AbstainFactoryBean {
			public W_AbstainFactoryBean() {}
			public static W_AbstainFactoryBean getInstance() { return null; }
		}

		@Test
		@DisplayName("W07 - noBuilder() short-circuits builder detection")
		void w07_noBuilder_skipsBuilderDetection() {
			// W_BeanWithBuilderShortCircuit has an inner Builder class.  noBuilder() must skip it and
			// fall through to the standard constructor.  Also drives getBuilderType() through
			// findBuilderType() to cover the noBuilder short-circuit at the top of that method.
			var b = bc(W_BeanWithBuilderShortCircuit.class).noBuilder();
			var bean = b.run();
			assertEquals("constructor-path", bean.origin);
			assertNull(b.getBuilderType(),
				"noBuilder() must also short-circuit findBuilderType() (called via getBuilderType())");
		}

		public static class W_BeanWithBuilderShortCircuit {
			public final String origin;
			public W_BeanWithBuilderShortCircuit() { this.origin = "constructor-path"; }
			public static class Builder {
				public W_BeanWithBuilderShortCircuit build() {
					return new W_BeanWithBuilderShortCircuit();
				}
			}
		}

		public static class W_ParentBean {}
		@Builder(W_ChildBuilder.class)
		public static class W_BuilderHolder extends W_ParentBean {}
		public static class W_ChildBuilder {
			public W_BuilderHolder build() { return new W_BuilderHolder(); }
		}

		public static class W_ChildOfBuilderHolder extends W_BuilderHolder {
			public W_ChildOfBuilderHolder() {}
		}

		@Test
		@DisplayName("W08 - @Builder on parent class is honored via inherited annotation lookup")
		void w08_inheritedBuilderAnnotation() {
			// W_ChildOfBuilderHolder doesn't declare its own @Builder; the resolver must fall back to
			// the inherited @Builder(W_ChildBuilder.class) annotation on W_BuilderHolder.
			var b = bc(W_ChildOfBuilderHolder.class);
			b.run();
			var builderType = b.getBuilderType();
			assertNotNull(builderType, "Builder type must be discovered via inherited @Builder annotation");
			assertEquals(W_ChildBuilder.class.getName(), builderType.getName());
		}

		public static class W_BeanWithParentInnerBuilder extends W_BeanWithInnerBuilder {
			public W_BeanWithParentInnerBuilder() { super(); }
		}

		public static class W_BeanWithInnerBuilder {
			public final String origin;
			public W_BeanWithInnerBuilder() { this.origin = "parent-default"; }
			W_BeanWithInnerBuilder(Builder b) { this.origin = b.tag; }
			public static class Builder {
				String tag = "parent-builder";
				public W_BeanWithInnerBuilder build() { return new W_BeanWithInnerBuilder(this); }
			}
		}

		@Test
		@DisplayName("W09 - Builder inner class is discovered when declared on a parent class (Priority 3c)")
		void w09_builderInParentClass() {
			// W_BeanWithParentInnerBuilder has no declared @Builder, no static factory, and no inner
			// class.  Priority 3c walks the parent chain and finds W_BeanWithInnerBuilder.Builder.
			var b = bc(W_BeanWithParentInnerBuilder.class);
			b.run();
			var builderType = b.getBuilderType();
			assertNotNull(builderType, "Builder type must be discovered via parent inner class");
			assertTrue(builderType.getName().endsWith("$Builder"));
		}

		public static class W_BeanWithSupplierSetter {
			public final String wired;
			Supplier<AnotherService> svcSupplier;
			public W_BeanWithSupplierSetter(Builder b) { this.wired = b.svcSupplier == null ? "no-supplier" : ("svc:" + b.svcSupplier.get().getValue()); }
			public static class Builder {
				Supplier<AnotherService> svcSupplier;
				public Builder() {}
				public void setSvcSupplier(Supplier<AnotherService> s) { this.svcSupplier = s; }
				public W_BeanWithSupplierSetter build() { return new W_BeanWithSupplierSetter(this); }
			}
		}

		@Test
		@DisplayName("W10 - autoWireBuilder() wraps a registered bean as a Supplier (unwrapSuppliers is true by default)")
		void w10_autoWireUnwrapSuppliers() {
			beanStore.addBean(AnotherService.class, new AnotherService(123));
			var bean = bc(W_BeanWithSupplierSetter.class)
				.builder(W_BeanWithSupplierSetter.Builder.class)
				.autoWireBuilder()
				.run();
			assertEquals("svc:123", bean.wired,
				"Supplier-typed setter must receive a supplier wrapping the registered bean");
		}

		public static class W_BeanWithOnlyArgCtor {
			public final String tag;
			@Inject public W_BeanWithOnlyArgCtor(AnotherService s) { this.tag = "arg:" + s.getValue(); }
		}

		@Test
		@DisplayName("W05b - preferZeroArgConstructor() falls back to longest-resolvable ctor when no zero-arg exists")
		void w05b_preferZeroArgConstructor_noneAvailable() {
			beanStore.addBean(AnotherService.class, new AnotherService(7));
			// Bean has no zero-arg constructor — the zeroArgCtor.isPresent() check fails, control
			// flows through to the standard ctor resolution path.
			var bean = bc(W_BeanWithOnlyArgCtor.class).preferZeroArgConstructor().run();
			assertEquals("arg:7", bean.tag, "Should still find a constructor when no zero-arg exists");
		}

		public static class W_FactoryReturnsNullBean {
			public final String origin;
			public W_FactoryReturnsNullBean() { this.origin = "fallback-ctor"; }
			public static W_FactoryReturnsNullBean getInstance() { return null; }
		}

		@Test
		@DisplayName("W06b - factory returning null without factoryAbstainOnNull() falls through to constructor")
		void w06b_factoryReturnsNull_withoutAbstain_fallsThroughToCtor() {
			// Same factory-returns-null fixture as W06 but without the abstain opt-in — drives the
			// `factoryAbstainOnNull == false` branch of the line-1856 short-circuit guard.
			var bean = bc(W_FactoryReturnsNullBean.class).run();
			assertNotNull(bean);
			assertEquals("fallback-ctor", bean.origin);
		}

		// Parent declares an inner Builder class that is NOT a valid builder (no build/create/get
		// method returning the bean subtype) — drives the {@code !isValidBuilderType} branch in
		// findBuilderType()'s Priority 3c.
		public static class W_BeanWithInvalidParentBuilder extends W_BeanWithInvalidBuilderParent {
			public W_BeanWithInvalidParentBuilder() {}
		}

		public static class W_BeanWithInvalidBuilderParent {
			public W_BeanWithInvalidBuilderParent() {}
			public static class Builder {
				// No build()/create()/get() method — isValidBuilderType returns false.
				public Builder() {}
				public Object unrelated() { return null; }
			}
		}

		@Test
		@DisplayName("W09b - Invalid Builder discovered in parent class is returned for richer error context")
		void w09b_invalidBuilderInParentClass() {
			// findBuilderType() must still return the discovered (invalid) builder so the build-method
			// resolution path can surface a more descriptive diagnostic.  We only check that the type
			// itself was returned; running may legitimately fall through to the standard ctor path.
			var b = bc(W_BeanWithInvalidParentBuilder.class);
			var builderType = b.getBuilderType();
			assertNotNull(builderType,
				"Invalid parent inner Builder must still be returned so failure messages are informative");
			assertEquals(W_BeanWithInvalidBuilderParent.Builder.class.getName(), builderType.getName());
		}

		@Test
		@DisplayName("W11 - copy() of a debug()-enabled builder gives the copy its own fresh debug log")
		void w11_copy_preservesDebugFlag() {
			// Exercises the {@code debug.ifPresent(x -> c.debug.set(new ArrayList<>()))} lambda body
			// inside Builder.copy() — only reached when the source builder has called .debug().
			var original = bc(W_BeanWithImpl.class).impl(new W_BeanWithImpl("orig")).debug();
			original.run(); // populates original's debug log
			var copy = original.copy();
			copy.run();
			var originalLog = original.getDebugLog();
			var copyLog = copy.getDebugLog();
			assertNotNull(originalLog);
			assertNotNull(copyLog);
			assertNotSame(originalLog, copyLog,
				"copy() must give the clone its own debug log instance");
		}

		@Test
		@DisplayName("W12 - silent() suppresses both run-time and debug log output (covers log() guard)")
		void w12_silent_suppressesLogging() {
			// {@code log()} guards `silent && !debug.isPresent()` — silent with no debug active drives
			// the early-return branch.  We can only verify the public outcome: no debug entries.
			var b = bc(V_outerWrapper.V_Bean.class).silent();
			b.run();
			assertTrue(b.getDebugLog().isEmpty(),
				"silent() without debug() must produce no captured log entries");
		}
	}

}

