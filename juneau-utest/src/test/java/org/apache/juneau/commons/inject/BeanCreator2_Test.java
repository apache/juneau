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
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.Flag;
import org.apache.juneau.commons.lang.IntegerValue;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.annotation.*;
import java.util.logging.Level;

class BeanCreator2_Test extends TestBase {

	private BasicBeanStore2 beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore2(null);
	}

	/**
	 * Helper method to create a BeanCreator2 instance with the test's beanStore.
	 * Reduces repetition of BeanCreator2.of(Class, beanStore) pattern.
	 */
	private <T> BeanCreator2<T> bc(Class<T> c) {
		return BeanCreator2.of(c, beanStore);
	}

	//====================================================================================================
	// Test classes
	//====================================================================================================

	// Type not in bean store - used for testing unresolvable dependencies
	public static class UnresolvableType {
		public UnresolvableType() {}
	}

	// Mock annotations for testing (matched by simple class name)
	@Retention(RUNTIME)
	@Target({FIELD, METHOD})
	@interface Inject {}

	@Retention(RUNTIME)
	@Target({FIELD, METHOD})
	@interface Autowired {}

	@Retention(RUNTIME)
	@Target({METHOD})
	@interface PostConstruct {}

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
	public static abstract class AbstractBean {
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
		void c03_createBeanIgnoresDeprecatedGetInstance() {
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
		void c04_createBeanIgnoresNonStaticGetInstance() {
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
		 * Verifies that BeanCreator2 can automatically detect and use an inner Builder class
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
		 * BeanCreator2 uses that instance instead of creating a new one. This allows
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
		 * BeanCreator2 uses that builder type instead of auto-detection. This provides explicit
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
		 * Verifies that builders with protected constructors can be instantiated by BeanCreator2.
		 * This tests the fallback mechanism that searches for protected constructors when no public
		 * constructor is available, enabling encapsulation while still allowing bean creation.
		 */
		@Test
		void d08_createBeanWithProtectedBuilderConstructor() {
			var bean = bc(D08_BeanWithProtectedBuilderConstructor.class).run();

			assertInstanceOf(D08_BeanWithProtectedBuilderConstructor.class, bean);
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
		 * that extends the parent's builder, BeanCreator2 uses the child's builder annotation
		 * (which overrides the parent's). The child's builder must override build() to return
		 * the child type, ensuring type safety.
		 */
		@Test
		void d10_createChildBeanUsingParentBuilderAnnotation() {
			var bc = bc(D10_ParentBeanWithBuilderAnnotation.class)
				.beanSubType(D10_ChildBeanWithInheritedBuilderAnnotation.class)
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
		 * BeanCreator2 uses the child's builder. The child's builder must override build() to return the
		 * child type, ensuring type safety. The builder is discovered through the child class's static create() method.
		 */
		@Test
		void d12_createChildBeanUsingParentInnerBuilderClass() {
			var bean = bc(D12_ParentWithInnerBuilderClass.class)
				.beanSubType(D12_ChildWithInheritedInnerBuilderClass.class)
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
		 * Verifies that BeanCreator2 recognizes "builder" as a valid builder factory method name alongside
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
		 * constructs the bean. BeanCreator2 searches for build(), create(), or get() methods on builders,
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
		 * Verifies that when a builder's build() method requires parameters, BeanCreator2 resolves them from
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
		 * Verifies that when multiple constructors are available, BeanCreator2 skips deprecated ones
		 * and prefers non-deprecated alternatives. This ensures that deprecated APIs don't interfere
		 * with bean creation, allowing graceful deprecation of old constructor signatures.
		 */
		@Test
		void d19_createBeanIgnoresDeprecatedConstructor() {
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
		 * Verifies that BeanCreator2 skips constructors whose parameters cannot be resolved from the bean store,
		 * preferring constructors with resolvable or no parameters. This ensures robust bean creation even when
		 * some constructor signatures cannot be satisfied.
		 */
		@Test
		void d20_createBeanIgnoresUnresolvableConstructor() {
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
		 * Verifies that when a builder lacks build/create/get methods, BeanCreator2 falls back to static factory
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
		 * Verifies the final fallback mechanism where BeanCreator2 searches for any method on the builder that returns
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
		 * no constructor accepting builder), BeanCreator2 rejects it and falls back to direct constructor-based creation.
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
		 * path exists (no factory methods, no valid constructors), BeanCreator2 throws an ExecutableException.
		 * This ensures that unresolvable builder configurations result in clear error reporting rather than silent failures.
		 */
		@Test
		void d27_builderFailsWithoutFallback() {
			var creator = bc(D27_BeanWithFailingBuilder.class).debug();

			var exception = assertThrows(ExecutableException.class, () -> creator.run());

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
		 * Tests that an error is thrown when builder's method returns parent type instead of exact bean subtype.
		 * Verifies that Builder build methods must always return the exact bean subtype being created.
		 * This ensures type safety and prevents ambiguous builder behavior.
		 */
		@Test
		void d28_builderMethodReturningParentTypeThrowsError() {
			var creator = bc(D28_ParentBeanForBuilderMethod.class)
				.beanSubType(D28_ChildBeanForBuilderMethod.class)
				.builder(D28_BuilderForParentMethod.class);

			var ex = assertThrows(ExecutableException.class, () -> creator.run());
			assertContains("Builder method", ex.getMessage());
			assertContains("returns", ex.getMessage());
			assertContains("but must return", ex.getMessage());
			assertContains(D28_ChildBeanForBuilderMethod.class.getSimpleName(), ex.getMessage());
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
	 * - Creating beans with specified subtypes using beanSubType()
	 * - Parent/child bean relationships
	 * - Type resolution and validation
	 */
	@Nested class E_typeSpecification extends TestBase {

		/**
		 * Tests creating a bean with a specified subtype using beanSubType() method.
		 */
		@Test
		void e01_createWithBeanSubType() {
			var bean = bc(ParentBean.class).beanSubType(ChildBean.class).run();

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
			var outerInstance = new BeanCreator2_Test();
			var value = "test";
			beanStore.add(String.class, value);

			var bean = bc(InnerBean.class)
				.enclosingInstance(outerInstance)
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
			assertThrows(ExecutableException.class, () -> bc(BeanWithDependencies.class).run());
		}

		/**
		 * Tests that ExecutableException is thrown when trying to create an abstract class.
		 */
		@Test
		void g02_abstractClassThrowsException() {
			assertThrows(ExecutableException.class, () -> bc(AbstractBean.class).run());
		}

		// Abstract class with unresolvable constructor (so constructor attempt is skipped)
		public static abstract class AbstractBeanWithUnresolvableConstructor {
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
			var exception = assertThrows(ExecutableException.class, () -> creator.run());

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
			assertThrows(ExecutableException.class, () -> bc(BeanInterface.class).run());
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
			assertThrows(IllegalArgumentException.class, () -> bc(SimpleBean.class).fallback(null));
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
	 * Tests converting BeanCreator2 to Supplier interfaces:
	 * - asSupplier() conversion
	 * - asResettableSupplier() with caching
	 * - Optional-like methods on suppliers
	 * - Supplier reset behavior
	 */
	@Nested class H_supplierConversion extends TestBase {

		/**
		 * Tests converting BeanCreator2 to a Supplier.
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
		 * Tests converting BeanCreator2 to a ResettableSupplier.
		 */
		@Test
		void h03_asResettableSupplier() {
			var supplier = bc(SimpleBean.class).asResettableSupplier();

			var bean = supplier.get();
			assertInstanceOf(SimpleBean.class, bean);
		}

		/**
		 * Tests that asResettableSupplier() caches the bean instance across multiple get() calls.
		 */
		@Test
		void h04_asResettableSupplierCachesResult() {
			var supplier = bc(SimpleBean.class).asResettableSupplier();

			var bean1 = supplier.get();
			var bean2 = supplier.get();

			assertSame(bean1, bean2, "ResettableSupplier should cache the result");
		}

		/**
		 * Tests that reset() on ResettableSupplier forces recreation of the bean instance.
		 */
		@Test
		void h05_asResettableSupplierResetRecreates() {
			var supplier = bc(SimpleBean.class).asResettableSupplier();

			var bean1 = supplier.get();
			supplier.reset();
			var bean2 = supplier.get();

			assertNotSame(bean1, bean2, "Reset should force recreation");
		}

		/**
		 * Tests that post-create hooks are only called once per cached instance in ResettableSupplier.
		 */
		@Test
		void h07_asResettableSupplierWithPostCreateHooks() {
			var hookCallCount = IntegerValue.create();

			var supplier = bc(SimpleBean.class)
				.postCreateHook(b -> hookCallCount.increment())
				.asResettableSupplier();

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
		 * Tests that ResettableSupplier respects the cached() mode of the creator.
		 */
		@Test
		void h08_asResettableSupplierWithCached() {
			var creator = bc(SimpleBean.class).cached();

			var supplier = creator.asResettableSupplier();

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
		 * Tests Optional-like methods (isPresent, isEmpty, map) on ResettableSupplier.
		 */
		@Test
		void h09_asResettableSupplierOptionalMethods() {
			var supplier = bc(SimpleBean.class).asResettableSupplier();

			// Test Optional-like methods inherited from OptionalSupplier
			assertTrue(supplier.isPresent());

			var mapped = supplier.map(b -> b.getClass().getSimpleName());
			assertEquals("SimpleBean", mapped.orElse(null));
		}
	}

	/**
	 * Tests method chaining and fluent API:
	 * - Methods that return 'this' for chaining
	 * - addBean(), builder(), beanSubType(), etc.
	 * - Fluent API usage patterns
	 */
	@Nested class I_methodChaining extends TestBase {

		/**
		 * Tests that beanSubType() returns this for method chaining.
		 */
		@Test
		void i02_beanSubTypeReturnsThis() {
			var creator = BeanCreator2.of(ParentBean.class);
			var result = creator.beanSubType(ChildBean.class);
			assertSame(creator, result);
		}

		/**
		 * Tests that addBean() returns this for method chaining.
		 */
		@Test
		void i03_addBeanReturnsThis() {
			var creator = BeanCreator2.of(SimpleBean.class);
			var result = creator.addBean(TestService.class, new TestService("test"));
			assertSame(creator, result);
		}

		/**
		 * Tests that builder() returns this for method chaining.
		 */
		@Test
		void i05_builderReturnsThis() {
			var creator = BeanCreator2.of(BeanWithBuilder.class);
			var result = creator.builder(BeanWithBuilder.create());
			assertSame(creator, result);
		}

		/**
		 * Tests that enclosingInstance() returns this for method chaining.
		 */
		@Test
		void i06_enclosingInstanceReturnsThis() {
			var creator = BeanCreator2.of(InnerBean.class);
			var result = creator.enclosingInstance(new BeanCreator2_Test());
			assertSame(creator, result);
		}

		/**
		 * Tests that add() method returns the bean that was added (for fluent API).
		 */
		@Test
		void i07_addMethodReturnsBean() {
			var service = new TestService("test");
			var creator = BeanCreator2.of(BeanWithDependencies.class);

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

			var result = bc(SimpleBean.class).implementation(preConfiguredBean).run();

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
				.implementation(bean)
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

			var creator = bc(SimpleBean.class).implementation(preConfiguredBean).cached();

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
				.implementation(preConfiguredBean)
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

			assertThrows(RuntimeException.class, () -> creator.run(), "Hook exception should propagate");
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

			assertThrows(RuntimeException.class, () -> creator.run());
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
			assertThrows(NoSuchElementException.class, () -> bc(BeanInterface.class).asOptional().orElseThrow());
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
				.filter(bean -> bean != null);

			assertTrue(result.isPresent());
		}


		/**
		 * Tests that exceptions thrown in post-create hooks propagate even when using asOptional() (not caught).
		 */
		@Test
		void m17_asOptionalPostHookExceptionPropagates() {
			assertThrows(RuntimeException.class, () -> {
				bc(SimpleBean.class)
				.postCreateHook(b -> {
					throw rex("Hook error");
				})
				.asOptional();
			}, "Post-create hook exceptions should propagate even in tryCreate");
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
			var creator = bc(SimpleBean.class).implementation(impl).debug();

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

			assertThrows(ExecutableException.class, () -> creator.run());

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
		 * Tests that passing null to beanSubType() throws IllegalArgumentException.
		 */
		@Test
		void n12_beanSubTypeNullThrows() {
			assertThrows(IllegalArgumentException.class, () -> BeanCreator2.of(SimpleBean.class).beanSubType(null));
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
		 * Tests that BeanCreator2.of(Class, BeanStore) can resolve dependencies from parent bean store.
		 */
		@Test
		void o02_ofWithParentStore() {
			var parentStore = new BasicBeanStore2(null);
			var testService = new TestService("parent-service");
			parentStore.addBean(TestService.class, testService);

			var bean = BeanCreator2.of(BeanWithDependencies.class, parentStore)
				.addBean(AnotherService.class, new AnotherService(42))
				.run();

			assertSame(testService, bean.getService());
			assertEquals(42, bean.getAnother().getValue());
		}

		/**
		 * Tests that BeanCreator2.of(Class, null) works when no parent bean store is provided.
		 */
		@Test
		void o03_ofWithParentStoreNull() {
			var bean = BeanCreator2.of(SimpleBean.class, null).run();

			assertInstanceOf(SimpleBean.class, bean);
		}

		/**
		 * Tests that local addBean() calls override values from parent bean store.
		 */
		@Test
		void o04_ofWithParentStoreLocalOverrides() {
			var parentStore = new BasicBeanStore2(null);
			var parentService = new TestService("parent");
			parentStore.addBean(TestService.class, parentService);

			var localService = new TestService("local");
			var bean = BeanCreator2.of(BeanWithDependencies.class, parentStore)
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
		 * Tests that getBeanSubType() returns the set subtype when beanSubType() is called (protected method test).
		 */
		@Test
		void p02_getBeanSubTypeReturnsSubtypeWhenSet() {
			var creator = bc(ParentBean.class);
			creator.beanSubType(ChildBean.class);
			var beanSubType = creator.getBeanSubType();

			assertEquals(ChildBean.class.getName(), beanSubType.getName());
			assertNotEquals(ParentBean.class.getName(), beanSubType.getName());
		}

		/**
		 * Tests that getName() returns null when no name is set.
		 */
		@Test
		void p03_getNameReturnsNullWhenNotSet() {
			var creator = BeanCreator2.of(SimpleBean.class);
			var name = creator.getName();

			assertNull(name, "getName() should return null when no name is set");
		}

		/**
		 * Tests that getName() returns the name set via name() method.
		 */
		@Test
		void p04_getNameReturnsSetName() {
			var creator = bc(SimpleBean.class).name("myBean");
			var name = creator.getName();

			assertEquals("myBean", name);
		}

		/**
		 * Tests that getName() returns null when name is explicitly set to null via name(null).
		 */
		@Test
		void p05_getNameReturnsNullWhenSetToNull() {
			var creator = bc(SimpleBean.class).name("initial").name(null);
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
			creator.beanSubType(ChildBean.class);
			var beanSubTypes = creator.getBeanSubTypes();

			assertTrue(beanSubTypes.size() >= 2, "Should have at least beanSubType and beanType");
			// First element should be the subtype (ChildBean)
			assertEquals(ChildBean.class.getName(), (beanSubTypes.get(0)).getName());
			// Last element should be the bean type (ParentBean)
			assertEquals(ParentBean.class.getName(), (beanSubTypes.get(beanSubTypes.size() - 1)).getName());
		}

		/**
		 * Tests that getBeanSubTypes() caches results using ResettableSupplier (protected method test).
		 */
		@Test
		void p08_getBeanSubTypesIsCached() {
			var creator = bc(ParentBean.class);
			creator.beanSubType(ChildBean.class);

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
		 * Tests that getBuilderType() caches results using ResettableSupplier (protected method test).
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
		 * Tests that getBuilderTypes() caches results using ResettableSupplier (protected method test).
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
		 * Tests that name() sets the name and returns this for method chaining.
		 */
		@Test
		void p17_nameSetsNameAndReturnsThis() {
			var creator = BeanCreator2.of(SimpleBean.class);
			var result = creator.name("testBean");

			// Should return this for method chaining
			assertSame(creator, result);
			assertEquals("testBean", creator.getName());
		}

		/**
		 * Tests that name() can accept null value to clear the name.
		 */
		@Test
		void p18_nameCanBeSetToNull() {
			var creator = bc(SimpleBean.class).name("initial");
			assertEquals("initial", creator.getName());

			creator.name(null);
			assertNull(creator.getName(), "name() should allow null value");
		}

		/**
		 * Tests that name() can be chained multiple times, with later calls overriding earlier ones.
		 */
		@Test
		void p19_nameCanBeChained() {
			var creator = bc(SimpleBean.class)
				.name("first")
				.name("second")
				.name("final");

			assertEquals("final", creator.getName());
		}

		/**
		 * Tests debug logging when builder returns parent type instead of child type.
		 * Verifies that builder build methods must return the exact bean subtype being created.
		 */
		@Test
		void p20_builderReturnsParentTypeNoConstructorAcceptsBuilder() {
			var creator = bc(P20_ParentBeanForBuilderTest.class)
				.beanSubType(P20_ChildBeanForBuilderTest.class)
				.builder(P20_BuilderForParentBean.class)
				.debug();

			assertThrows(ExecutableException.class, () -> creator.run());

			var log = creator.getDebugLog();
			var logString = log.toString();
			assertContains("Builder method", logString);
			assertContains("returns", logString);
			assertContains("but must return", logString);
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
			// Get the logger instance using the same method as BeanCreator2 static field
			// This ensures we get the same cached instance
			var logger = Logger.getLogger(BeanCreator2.class);
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
			var logger = Logger.getLogger(BeanCreator2.class);
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
			var logger = Logger.getLogger(BeanCreator2.class);
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
			var logger = Logger.getLogger(BeanCreator2.class);
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

}

