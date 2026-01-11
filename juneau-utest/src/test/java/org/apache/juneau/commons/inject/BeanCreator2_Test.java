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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.ExecutableException;
import org.junit.jupiter.api.*;

class BeanCreator2_Test extends TestBase {

	private BasicBeanStore2 beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore2(null);
	}

	//====================================================================================================
	// Test classes
	//====================================================================================================

	// Mock annotations for testing (matched by simple class name)
	@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
	@java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD})
	@interface Inject {}

	// Simple service class
	static class TestService {
		private final String name;
		TestService(String name) { this.name = name; }
		String getName() { return name; }
		@Override public String toString() { return "TestService[" + name + "]"; }
	}

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

	// Bean with Optional parameter
	public static class BeanWithOptional {
		private final Optional<TestService> service;

		public BeanWithOptional(Optional<TestService> service) {
			this.service = service;
		}

		public Optional<TestService> getService() { return service; }
	}

	// Bean with List parameter
	public static class BeanWithList {
		private final List<TestService> services;

		public BeanWithList(List<TestService> services) {
			this.services = services;
		}

		public List<TestService> getServices() { return services; }
	}

	// Bean with injected fields
	public static class BeanWithInjectedFields {
		@Inject
		TestService service;

		@Inject
		AnotherService another;

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

			public Builder name(String name) {
				this.name = name;
				return this;
			}

			public Builder value(int value) {
				this.value = value;
				return this;
			}

			public BeanWithBuilder build() {
				return new BeanWithBuilder(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}
	}

	// Bean with Builder annotation
	@org.apache.juneau.commons.annotation.Builder(BeanWithBuilderAnnotation.MyBuilder.class)
	public static class BeanWithBuilderAnnotation {
		private final String name;

		private BeanWithBuilderAnnotation(MyBuilder builder) {
			this.name = builder.name;
		}

		public String getName() { return name; }

		public static class MyBuilder {
			private String name;

			public MyBuilder name(String name) {
				this.name = name;
				return this;
			}

			public BeanWithBuilderAnnotation build() {
				return new BeanWithBuilderAnnotation(this);
			}
		}
	}

	// Bean with injected builder
	public static class BeanWithInjectedBuilder {
		private final TestService service;

		private BeanWithInjectedBuilder(BuilderWithInjection builder) {
			this.service = builder.service;
		}

		public TestService getService() { return service; }

		public static class BuilderWithInjection {
			@Inject
			TestService service;

			public BeanWithInjectedBuilder build() {
				return new BeanWithInjectedBuilder(this);
			}
		}

		public static BuilderWithInjection create() {
			return new BuilderWithInjection();
		}
	}

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

	//====================================================================================================
	// Tests - Basic bean creation via constructor
	//====================================================================================================

	@Test
	void a01_createSimpleBean() {
		var bean = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(SimpleBean.class, bean);
	}

	@Test
	void a02_createBeanWithDependencies() {
		var testService = new TestService("test");
		var anotherService = new AnotherService(42);
		beanStore.add(TestService.class, testService);
		beanStore.add(AnotherService.class, anotherService);

		var bean = BeanCreator2.of(BeanWithDependencies.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertSame(testService, bean.getService());
		assertSame(anotherService, bean.getAnother());
	}

	@Test
	void a03_createBeanWithOptionalPresent() {
		var testService = new TestService("test");
		beanStore.add(TestService.class, testService);

		var bean = BeanCreator2.of(BeanWithOptional.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertTrue(bean.getService().isPresent());
		assertSame(testService, bean.getService().get());
	}

	@Test
	void a04_createBeanWithOptionalEmpty() {
		var bean = BeanCreator2.of(BeanWithOptional.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertTrue(bean.getService().isEmpty());
	}

	@Test
	void a05_createBeanWithList() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.add(TestService.class, service1);
		beanStore.add(TestService.class, service2, "service2");

		var bean = BeanCreator2.of(BeanWithList.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertEquals(2, bean.getServices().size());
		assertTrue(bean.getServices().contains(service1));
		assertTrue(bean.getServices().contains(service2));
	}

	@Test
	void a06_createBeanWithAddBean() {
		var testService = new TestService("test");
		var anotherService = new AnotherService(42);

		var bean = BeanCreator2.of(BeanWithDependencies.class)
			.beanStore(beanStore)
			.addBean(TestService.class, testService)
			.addBean(AnotherService.class, anotherService)
			.create();

		assertNotNull(bean);
		assertSame(testService, bean.getService());
		assertSame(anotherService, bean.getAnother());
	}

	//====================================================================================================
	// Tests - Dependency injection into created beans
	//====================================================================================================

	@Test
	void b01_injectFieldsIntoBean() {
		var testService = new TestService("test");
		var anotherService = new AnotherService(42);
		beanStore.add(TestService.class, testService);
		beanStore.add(AnotherService.class, anotherService);

		var bean = BeanCreator2.of(BeanWithInjectedFields.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertSame(testService, bean.getService());
		assertSame(anotherService, bean.getAnother());
	}

	@Test
	void b02_injectMethodIntoBean() {
		var testService = new TestService("test");
		beanStore.add(TestService.class, testService);

		var bean = BeanCreator2.of(BeanWithInjectedMethod.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertSame(testService, bean.getService());
	}

	//====================================================================================================
	// Tests - Static getInstance methods
	//====================================================================================================

	@Test
	void c01_createSingletonBean() {
		var bean = BeanCreator2.of(SingletonBean.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertSame(SingletonBean.INSTANCE, bean);
	}

	@Test
	void c02_createSingletonBeanWithParams() {
		var testService = new TestService("test");
		beanStore.add(TestService.class, testService);

		var bean = BeanCreator2.of(SingletonBeanWithParams.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertSame(testService, bean.getService());
	}

	// Bean with both deprecated and valid getInstance methods
	public static class BeanWithDeprecatedGetInstance {
		private static final BeanWithDeprecatedGetInstance INSTANCE = new BeanWithDeprecatedGetInstance();

		private BeanWithDeprecatedGetInstance() {}

		@Deprecated
		public static BeanWithDeprecatedGetInstance getInstance(String unused) {
			// Deprecated version - should be ignored
			throw new RuntimeException("Should not be called");
		}

		public static BeanWithDeprecatedGetInstance getInstance() {
			// Valid non-deprecated version
			return INSTANCE;
		}
	}

	@Test
	void c03_createBeanIgnoresDeprecatedGetInstance() {
		// This tests the NOT_DEPRECATED branch in line 484
		var bean = BeanCreator2.of(BeanWithDeprecatedGetInstance.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertSame(BeanWithDeprecatedGetInstance.INSTANCE, bean);
	}

	// Bean with non-static getInstance (should use constructor instead)
	public static class BeanWithNonStaticGetInstance {
		public BeanWithNonStaticGetInstance() {
			// Public constructor
		}

		// Non-static getInstance - should be ignored
		public BeanWithNonStaticGetInstance getInstance() {
			throw new RuntimeException("Should not be called");
		}
	}

	@Test
	void c04_createBeanIgnoresNonStaticGetInstance() {
		// This tests the STATIC branch in line 484
		var bean = BeanCreator2.of(BeanWithNonStaticGetInstance.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(BeanWithNonStaticGetInstance.class, bean);
	}

	//====================================================================================================
	// Tests - Builder-based bean creation
	//====================================================================================================

	@Test
	void d01_createBeanWithBuilder() {
		var bean = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		// Builder created with defaults
	}

	@Test
	void d02_createBeanWithExplicitBuilderInstance() {
		var builder = BeanWithBuilder.create()
			.name("test")
			.value(42);

		var bean = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore)
			.builder(builder)
			.create();

		assertNotNull(bean);
		assertEquals("test", bean.getName());
		assertEquals(42, bean.getValue());
	}

	@Test
	void d03_createBeanWithBuilderAnnotation() {
		var bean = BeanCreator2.of(BeanWithBuilderAnnotation.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
	}

	@Test
	void d04_createBeanWithExplicitBuilderType() {
		var bean = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore)
			.builder(BeanWithBuilder.Builder.class)
			.create();

		assertNotNull(bean);
	}

	@Test
	void d05_createBeanWithInjectedBuilder() {
		var testService = new TestService("test");
		beanStore.add(TestService.class, testService);

		var bean = BeanCreator2.of(BeanWithInjectedBuilder.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertSame(testService, bean.getService());
	}

	@Test
	void d06_getBuilder() {
		var creator = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore);

		// Builder should be lazily created
		Optional<BeanWithBuilder.Builder> builder = creator.getBuilder();
		assertTrue(builder.isPresent());
		assertInstanceOf(BeanWithBuilder.Builder.class, builder.get());
	}

	// Bean with builder where child uses parent's builder
	public static class ParentBeanWithBuilder {
		protected final String name;

		protected ParentBeanWithBuilder(BuilderForParent builder) {
			this.name = builder.name;
		}

		public String getName() { return name; }

		public static class BuilderForParent {
			protected String name;

			public BuilderForParent name(String name) {
				this.name = name;
				return this;
			}

			public ParentBeanWithBuilder build() {
				return new ParentBeanWithBuilder(this);
			}
		}

		public static BuilderForParent create() {
			return new BuilderForParent();
		}
	}

	public static class ChildBeanWithBuilder extends ParentBeanWithBuilder {
		public ChildBeanWithBuilder(BuilderForParent builder) {
			super(builder);
		}
	}

	@Test
	void d07_createChildBeanUsingParentBuilder() {
		// This tests the case where beanSubType != beanType and the child class
		// inherits the parent's static builder method.
		// The builder is found from the child class (which includes inherited static methods).
		@SuppressWarnings("rawtypes")
		var bean = BeanCreator2.of(ParentBeanWithBuilder.class)
			.beanStore(beanStore)
			.beanSubType((Class)ChildBeanWithBuilder.class)
			.create();

		assertNotNull(bean);
		assertInstanceOf(ChildBeanWithBuilder.class, bean);
	}

	// Bean with builder that has a protected constructor
	@org.apache.juneau.commons.annotation.Builder(BeanWithProtectedBuilderConstructor.ProtectedBuilder.class)
	public static class BeanWithProtectedBuilderConstructor {
		private final String value;

		private BeanWithProtectedBuilderConstructor(ProtectedBuilder builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		public static class ProtectedBuilder {
			private String value;

			protected ProtectedBuilder() {
				// Protected no-arg constructor
			}

			public ProtectedBuilder value(String value) {
				this.value = value;
				return this;
			}

			public BeanWithProtectedBuilderConstructor build() {
				return new BeanWithProtectedBuilderConstructor(this);
			}
		}
	}

	@Test
	void d08_createBeanWithProtectedBuilderConstructor() {
		// This tests the case where the builder is found via a protected constructor
		// (lines 280-286)
		var bean = BeanCreator2.of(BeanWithProtectedBuilderConstructor.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(BeanWithProtectedBuilderConstructor.class, bean);
	}

	// Bean with builder that has a protected constructor with dependencies
	@org.apache.juneau.commons.annotation.Builder(BeanWithProtectedBuilderConstructorWithDeps.ProtectedBuilderWithDeps.class)
	public static class BeanWithProtectedBuilderConstructorWithDeps {
		private final String serviceName;

		private BeanWithProtectedBuilderConstructorWithDeps(ProtectedBuilderWithDeps builder) {
			this.serviceName = builder.service.getName();
		}

		public String getServiceName() { return serviceName; }

		public static class ProtectedBuilderWithDeps {
			private TestService service;

			protected ProtectedBuilderWithDeps(TestService service) {
				// Protected constructor with dependency injection
				this.service = service;
			}

			public BeanWithProtectedBuilderConstructorWithDeps build() {
				return new BeanWithProtectedBuilderConstructorWithDeps(this);
			}
		}
	}

	@Test
	void d09_createBeanWithProtectedBuilderConstructorWithDependencies() {
		// This tests the case where the builder has a protected constructor
		// that requires dependencies from the bean store (lines 280-286)
		var testService = new TestService("injected");
		beanStore.add(TestService.class, testService);

		var bean = BeanCreator2.of(BeanWithProtectedBuilderConstructorWithDeps.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertEquals("injected", bean.getServiceName());
	}

	// Parent bean with @Builder annotation
	@org.apache.juneau.commons.annotation.Builder(ParentBeanWithBuilderAnnotation.ParentBuilder.class)
	public static class ParentBeanWithBuilderAnnotation {
		protected final String value;

		protected ParentBeanWithBuilderAnnotation(ParentBuilder builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		public static class ParentBuilder {
			protected String value;

			public ParentBuilder value(String value) {
				this.value = value;
				return this;
			}

			public ParentBeanWithBuilderAnnotation build() {
				return new ParentBeanWithBuilderAnnotation(this);
			}
		}
	}

	// Child bean without @Builder annotation (inherits parent's builder)
	public static class ChildBeanWithInheritedBuilderAnnotation extends ParentBeanWithBuilderAnnotation {
		public ChildBeanWithInheritedBuilderAnnotation(ParentBuilder builder) {
			super(builder);
		}
	}

	@Test
	void d10_createChildBeanUsingParentBuilderAnnotation() {
		// This tests that @Builder annotation is inherited from parent to child.
		// The child sees the parent's @Builder annotation via @Inherited.
		@SuppressWarnings("rawtypes")
		var bean = BeanCreator2.of(ParentBeanWithBuilderAnnotation.class)
			.beanStore(beanStore)
			.beanSubType((Class)ChildBeanWithInheritedBuilderAnnotation.class)
			.create();

		assertNotNull(bean);
		assertInstanceOf(ChildBeanWithInheritedBuilderAnnotation.class, bean);
	}

	// Bean with inner Builder class (no @Builder annotation, no static create/builder method)
	public static class BeanWithInnerBuilderClass {
		private final String value;

		private BeanWithInnerBuilderClass(Builder builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		public static class Builder {
			private String value;

			public Builder value(String value) {
				this.value = value;
				return this;
			}

			public BeanWithInnerBuilderClass build() {
				return new BeanWithInnerBuilderClass(this);
			}
		}
	}

	@Test
	void d11_createBeanWithInnerBuilderClass() {
		// This tests autodetection of builder via inner "Builder" class (line 332)
		// when there's no @Builder annotation and no static create/builder method
		var bean = BeanCreator2.of(BeanWithInnerBuilderClass.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(BeanWithInnerBuilderClass.class, bean);
	}

	// Parent bean with inner Builder class (no annotation, no static method)
	public static class ParentWithInnerBuilderClass {
		protected final String value;

		protected ParentWithInnerBuilderClass(Builder builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		public static class Builder {
			protected String value;

			public Builder value(String value) {
				this.value = value;
				return this;
			}

			public ParentWithInnerBuilderClass build() {
				return new ParentWithInnerBuilderClass(this);
			}
		}
	}

	// Child bean that inherits parent's inner Builder class
	public static class ChildWithInheritedInnerBuilderClass extends ParentWithInnerBuilderClass {
		public ChildWithInheritedInnerBuilderClass(Builder builder) {
			super(builder);
		}
	}

	@Test
	void d12_createChildBeanUsingParentInnerBuilderClass() {
		// This tests autodetection of builder via parent's inner "Builder" class (line 337)
		// when child has no builder and parent has inner Builder class
		@SuppressWarnings("rawtypes")
		var bean = BeanCreator2.of(ParentWithInnerBuilderClass.class)
			.beanStore(beanStore)
			.beanSubType((Class)ChildWithInheritedInnerBuilderClass.class)
			.create();

		assertNotNull(bean);
		assertInstanceOf(ChildWithInheritedInnerBuilderClass.class, bean);
	}

	// Bean with static builder() method (instead of create())
	public static class BeanWithBuilderMethod {
		private final String value;

		private BeanWithBuilderMethod(BuilderFromBuilderMethod builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		public static class BuilderFromBuilderMethod {
			private String value;

			public BuilderFromBuilderMethod value(String value) {
				this.value = value;
				return this;
			}

			public BeanWithBuilderMethod build() {
				return new BeanWithBuilderMethod(this);
			}
		}

		public static BuilderFromBuilderMethod builder() {
			return new BuilderFromBuilderMethod();
		}
	}

	@Test
	void d13_createBeanWithBuilderMethod() {
		// This tests the "builder" branch of hasName() check in line 349
		var bean = BeanCreator2.of(BeanWithBuilderMethod.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(BeanWithBuilderMethod.class, bean);
	}

	// Bean with static create() that returns the bean itself (factory method, not builder)
	public static class BeanWithFactoryMethod {
		private final String value;

		public BeanWithFactoryMethod() {
			this.value = "from-constructor";
		}

		private BeanWithFactoryMethod(String value) {
			this.value = value;
		}

		public String getValue() { return value; }

		// This is a factory method that returns the bean type itself, not a builder
		// It should be filtered out by line 364 and not considered a builder method
		public static BeanWithFactoryMethod create() {
			return new BeanWithFactoryMethod("from-factory");
		}
	}

	@Test
	void d14_createBeanWithFactoryMethodNotBuilder() {
		// This tests the branch in line 364 where static create() returns
		// the bean type itself (factory method) rather than a builder type.
		// Since there's no builder, the bean should be created via constructor.
		var bean = BeanCreator2.of(BeanWithFactoryMethod.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(BeanWithFactoryMethod.class, bean);
		// Note: The factory method should be used to create the bean directly
	}

	// Bean with builder that uses create() instead of build()
	public static class BeanWithBuilderCreateMethod {
		private final String value;

		private BeanWithBuilderCreateMethod(BuilderWithCreate builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		public static class BuilderWithCreate {
			private String value;

			public BuilderWithCreate value(String value) {
				this.value = value;
				return this;
			}

			// Using create() instead of build()
			public BeanWithBuilderCreateMethod create() {
				return new BeanWithBuilderCreateMethod(this);
			}
		}

		public static BuilderWithCreate builder() {
			return new BuilderWithCreate();
		}
	}

	@Test
	void d15_createBeanWithBuilderCreateMethod() {
		// This tests the "create" branch of hasName() check in lines 389-391
		var bean = BeanCreator2.of(BeanWithBuilderCreateMethod.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(BeanWithBuilderCreateMethod.class, bean);
	}

	// Bean with builder that uses get() instead of build()
	public static class BeanWithBuilderGetMethod {
		private final String value;

		private BeanWithBuilderGetMethod(BuilderWithGet builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		public static class BuilderWithGet {
			private String value;

			public BuilderWithGet value(String value) {
				this.value = value;
				return this;
			}

			// Using get() instead of build()
			public BeanWithBuilderGetMethod get() {
				return new BeanWithBuilderGetMethod(this);
			}
		}

		public static BuilderWithGet builder() {
			return new BuilderWithGet();
		}
	}

	@Test
	void d16_createBeanWithBuilderGetMethod() {
		// This tests the "get" branch of hasName() check in lines 389-391
		var bean = BeanCreator2.of(BeanWithBuilderGetMethod.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(BeanWithBuilderGetMethod.class, bean);
	}

	// Bean with builder that has no build/create/get method, but bean has public constructor accepting builder
	public static class BeanWithPublicConstructorAcceptingBuilder {
		private final String value;

		// Public constructor accepting builder
		public BeanWithPublicConstructorAcceptingBuilder(BuilderWithoutBuildMethod builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		public static class BuilderWithoutBuildMethod {
			private String value;

			public BuilderWithoutBuildMethod value(String value) {
				this.value = value;
				return this;
			}

			// No build(), create(), or get() method - bean constructor will be used instead
		}

		public static BuilderWithoutBuildMethod builder() {
			return new BuilderWithoutBuildMethod();
		}
	}

	@Test
	void d17_createBeanWithPublicConstructorAcceptingBuilder() {
		// This tests:
		// - Lines 397-398: Builder validation via public constructor
		// - Lines 457-463: Bean creation via public constructor accepting builder
		var bean = BeanCreator2.of(BeanWithPublicConstructorAcceptingBuilder.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertInstanceOf(BeanWithPublicConstructorAcceptingBuilder.class, bean);
	}

	// Bean with builder where build() method has parameters
	public static class BeanWithBuilderWithParameters {
		private final String value;
		private final String extra;

		private BeanWithBuilderWithParameters(BuilderWithParameters builder, String extra) {
			this.value = builder.value;
			this.extra = extra;
		}

		public String getValue() { return value; }
		public String getExtra() { return extra; }

		public static class BuilderWithParameters {
			private String value;

			public BuilderWithParameters value(String value) {
				this.value = value;
				return this;
			}

			// build() method that requires a String parameter from bean store
			public BeanWithBuilderWithParameters build(String extra) {
				return new BeanWithBuilderWithParameters(this, extra);
			}
		}

		public static BuilderWithParameters builder() {
			return new BuilderWithParameters();
		}
	}

	@Test
	void d18_createBeanWithBuilderMethodWithParameters() {
		// This tests line 440 where builder.build() has parameters that need to be resolved
		beanStore.add(String.class, "extra-value");

		var bean = BeanCreator2.of(BeanWithBuilderWithParameters.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertEquals("extra-value", bean.getExtra());
	}

	// Bean with deprecated constructor
	public static class BeanWithDeprecatedConstructor {
		public BeanWithDeprecatedConstructor() {
			// Valid non-deprecated constructor
		}

		@Deprecated
		public BeanWithDeprecatedConstructor(String unused) {
			// Deprecated constructor - should be ignored
			throw new RuntimeException("Should not be called");
		}
	}

	@Test
	void d19_createBeanIgnoresDeprecatedConstructor() {
		// This tests the NOT_DEPRECATED branch in line 493
		beanStore.add(String.class, "should-not-use-this");

		var bean = BeanCreator2.of(BeanWithDeprecatedConstructor.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
	}

	// Bean with constructor that has unresolvable parameters
	public static class BeanWithUnresolvableConstructor {
		public BeanWithUnresolvableConstructor() {
			// Valid no-arg constructor
		}

		public BeanWithUnresolvableConstructor(UnresolvableType param) {
			// Constructor with unresolvable parameter - should be ignored
			throw new RuntimeException("Should not be called");
		}
	}

	// Type not in bean store
	public static class UnresolvableType {
		public UnresolvableType() {}
	}

	// Bean with ONLY unresolvable constructors (no no-arg, no factory method, not abstract)
	// This will cause all creation attempts to fail and reach final fallback path
	public static class BeanWithOnlyUnresolvableConstructors {
		public BeanWithOnlyUnresolvableConstructors(UnresolvableType param1, UnresolvableType param2) {
			// Only constructor requires unresolvable parameters
		}
	}

	@Test
	void d20_createBeanIgnoresUnresolvableConstructor() {
		// This tests the hasAllParameters branch in line 493
		var bean = BeanCreator2.of(BeanWithUnresolvableConstructor.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
	}

	// Bean with static factory method that accepts a builder
	public static class BeanWithFactoryMethodAcceptingBuilder {
		private final String value;

		public BeanWithFactoryMethodAcceptingBuilder(Builder builder) {
			// Public constructor accepting builder - validates builder type
			// But we want the factory method to be used instead
			this.value = builder.value + "-constructor";
		}

		public static BeanWithFactoryMethodAcceptingBuilder getInstance(Builder builder) {
			// This is the factory method that should be called (line 1017)
			// It should be preferred over the constructor
			return new BeanWithFactoryMethodAcceptingBuilder(builder.value);
		}

		private BeanWithFactoryMethodAcceptingBuilder(String value) {
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

			public static Builder create() {
				return new Builder();
			}

			// No build() method - forces use of constructor or factory method
		}
	}

	@Test
	void d21_createBeanViaFactoryMethodAcceptingBuilder() {
		// This tests line 1017: static factory method on bean that accepts builder
		// The builder has no build() method, so either the constructor or factory method can be used
		// Factory methods are checked before constructors, so getInstance should be called
		var bean = BeanCreator2.of(BeanWithFactoryMethodAcceptingBuilder.class)
			.beanStore(beanStore)
			.debug()
			.create();

		assertNotNull(bean);
		assertEquals("default", bean.getValue());
		// If the constructor was used, value would be "default-constructor"
	}

	// Bean with builder that has a custom-named method returning the bean
	public static class BeanWithBuilderCustomMethod {
		private final String value;

		public BeanWithBuilderCustomMethod(BuilderWithCustomMethod builder) {
			this.value = builder.value;
		}

		public String getValue() {
			return value;
		}

		public static class BuilderWithCustomMethod {
			private String value = "default";

			public BuilderWithCustomMethod value(String value) {
				this.value = value;
				return this;
			}

			// This build() method returns wrong type - will be skipped
			// Forces fallback to the custom-named method
			public String build() {
				return "wrong-type";
			}

			// Custom method name that returns the bean (not build/create/get)
			// This should be found by the fallback "anything" method search (lines 1037-1044)
			public BeanWithBuilderCustomMethod execute() {
				return new BeanWithBuilderCustomMethod(this);
			}

			public static BuilderWithCustomMethod create() {
				return new BuilderWithCustomMethod();
			}
		}
	}

	@Test
	void d22_createBeanViaBuilderCustomMethod() {
		// This tests that the constructor accepting builder is used when available
		var bean = BeanCreator2.of(BeanWithBuilderCustomMethod.class)
			.beanStore(beanStore)
			.builderClass("BuilderWithCustomMethod")
			.debug()
			.create();

		assertNotNull(bean);
		assertEquals("default", bean.getValue());
	}

	// Bean where builder has custom method and constructor accepts builder but has unresolvable params
	public static class BeanWithBuilderAnyMethod {
		private final String value;

		private BeanWithBuilderAnyMethod(String value) {
			this.value = value;
		}

		// Public constructor that accepts builder but requires unresolvable parameter
		// This validates the builder type but won't be used at line 1063-1070
		public BeanWithBuilderAnyMethod(BuilderAnyMethod builder, UnresolvableType unresolvable) {
			throw new RuntimeException("Should not be called - has unresolvable parameter");
		}

		public String getValue() {
			return value;
		}

		public static class BuilderAnyMethod {
			private String value = "default";

			public BuilderAnyMethod value(String value) {
				this.value = value;
				return this;
			}

			// Custom method name that returns the bean
			// This should be found by the fallback "anything" method search (lines 1075-1082)
			public BeanWithBuilderAnyMethod finish() {
				return new BeanWithBuilderAnyMethod(this.value);
			}

			public static BuilderAnyMethod create() {
				return new BuilderAnyMethod();
			}
		}
	}

	@Test
	void d23_createBeanViaBuilderAnyMethod() {
		// This tests lines 1075-1082: Builder has a method returning bean type
		// but build/create/get doesn't work AND no constructor accepts builder
		var creator = BeanCreator2.of(BeanWithBuilderAnyMethod.class)
			.beanStore(beanStore)
			.builderClass("BuilderAnyMethod")
			.debug();

		var bean = creator.create();

		assertNotNull(bean);
		assertEquals("default", bean.getValue());

		// Verify the creation log shows the fallback path was taken
		var log = creator.getDebugLog();
		assertContains("Builder.anything()", log.toString());
	}

	// Bean with invalid builder (doesn't meet validation criteria)
	public static class BeanWithInvalidBuilder {
		private final String value;

		public BeanWithInvalidBuilder(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		// This inner class is named "Builder" but is NOT a valid builder:
		// - No build/create/get method returning BeanWithInvalidBuilder
		// - Bean has no constructor accepting this builder
		// This should fail validation and log line 998
		public static class Builder {
			private String value = "default";

			public Builder value(String value) {
				this.value = value;
				return this;
			}

			// This method returns String, not the bean type - invalid!
			public String build() {
				return value;
			}
		}
	}

	@Test
	void d24_invalidBuilderFailsValidation() {
		// This tests line 998: Builder candidate fails validation
		// The inner "Builder" class doesn't have a build method returning the bean type
		// and the bean doesn't have a constructor accepting the builder
		beanStore.add(String.class, "test-value");

		var creator = BeanCreator2.of(BeanWithInvalidBuilder.class)
			.beanStore(beanStore)
			.debug();

		// Should fall back to using the constructor directly (no builder)
		var bean = creator.create();

		assertNotNull(bean);
		assertEquals("test-value", bean.getValue());

		// Verify the debug log shows the builder validation failed
		var log = creator.getDebugLog();
		assertContains("Builder is NOT valid", log.toString());
	}

	// Bean with builder that fails to create bean (unresolvable parameters)
	public static class BeanWithFailingBuilder {
		private final String value;

		public BeanWithFailingBuilder(String value) {
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

			// build() method requires unresolvable parameter - will fail
			public BeanWithFailingBuilder build(UnresolvableType unresolvable) {
				return new BeanWithFailingBuilder(this.value);
			}

			public static Builder create() {
				return new Builder();
			}
		}
	}

	@Test
	void d25_fallbackWhenBuilderFails() {
		// This tests lines 1131-1137: Fallback supplier used when builder exists but fails
		// The builder's build() method has unresolvable parameters, so creation fails
		// and fallback supplier is used
		var fallback = new BeanWithFailingBuilder("fallback-value");

		var creator = BeanCreator2.of(BeanWithFailingBuilder.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.debug();

		var bean = creator.create();

		assertSame(fallback, bean);

		// Verify the debug log shows fallback was used
		var log = creator.getDebugLog();
		assertContains("Using fallback supplier", log.toString());
	}

	@Test
	void d26_fallbackWhenBuilderFailsWithSingleton() {
		// This tests line 1136: Fallback bean cached in singleton mode when builder fails
		var fallback = new BeanWithFailingBuilder("fallback-value");

		var creator = BeanCreator2.of(BeanWithFailingBuilder.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.singleton()
			.debug();

		var bean1 = creator.create();
		var bean2 = creator.create();

		// Both should return the same cached fallback instance
		assertSame(fallback, bean1);
		assertSame(fallback, bean2);
		assertSame(bean1, bean2);
	}

	@Test
	void d27_builderFailsWithoutFallback() {
		// This tests line 1140: Exception thrown when builder fails and no fallback provided
		var creator = BeanCreator2.of(BeanWithFailingBuilder.class)
			.beanStore(beanStore)
			.debug();

		// Should throw exception because builder fails and no fallback
		var exception = assertThrows(ExecutableException.class, () -> creator.create());

		assertContains("Could not instantiate class", exception.getMessage());
		assertContains("using builder type", exception.getMessage());

		// Verify the debug log shows the failure
		var log = creator.getDebugLog();
		assertContains("Failed to create bean using builder", log.toString());
	}

	@Test
	void d28_finalFallbackWithSingleton() {
		// This tests line 1219: Fallback bean cached in singleton mode when all creation attempts fail
		// BeanWithOnlyUnresolvableConstructors has no valid constructors, no factory method, not abstract
		var fallback = new BeanWithOnlyUnresolvableConstructors(null, null);

		var creator = BeanCreator2.of(BeanWithOnlyUnresolvableConstructors.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.singleton()
			.debug();

		var bean1 = creator.create();

		// Verify the debug log shows fallback was used (check before second call clears log)
		var log = creator.getDebugLog();
		assertContains("Using fallback supplier", log.toString());

		var bean2 = creator.create();

		// Both should return the same cached fallback instance
		assertSame(fallback, bean1);
		assertSame(fallback, bean2);
		assertSame(bean1, bean2);
	}

	//====================================================================================================
	// Tests - Type specification
	//====================================================================================================

	static class ParentBean {
		public ParentBean() {}
	}

	static class ChildBean extends ParentBean {
		public ChildBean() {}
	}

	@Test
	void e01_createWithBeanSubType() {
		@SuppressWarnings("rawtypes")
		var bean = BeanCreator2.of(ParentBean.class)
			.beanStore(beanStore)
			.beanSubType((Class)ChildBean.class)
			.create();

		assertNotNull(bean);
		assertInstanceOf(ChildBean.class, bean);
	}

	//====================================================================================================
	// Tests - Inner classes with enclosingInstance
	//====================================================================================================

	@Test
	void f01_createInnerBean() {
		var outerInstance = new BeanCreator2_Test();
		var value = "test";
		beanStore.add(String.class, value);

		var bean = BeanCreator2.of(InnerBean.class)
			.beanStore(beanStore)
			.enclosingInstance(outerInstance)
			.create();

		assertNotNull(bean);
		assertEquals(value, bean.getValue());
	}

	//====================================================================================================
	// Tests - Error cases
	//====================================================================================================

	@Test
	void g01_missingDependencyThrowsException() {
		assertThrows(ExecutableException.class, () -> {
			BeanCreator2.of(BeanWithDependencies.class)
				.beanStore(beanStore)
				.create();
		});
	}

	@Test
	void g02_abstractClassThrowsException() {
		assertThrows(ExecutableException.class, () -> {
			BeanCreator2.of(AbstractBean.class)
				.beanStore(beanStore)
				.create();
		});
	}

	// Abstract class with unresolvable constructor (so constructor attempt is skipped)
	public static abstract class AbstractBeanWithUnresolvableConstructor {
		public AbstractBeanWithUnresolvableConstructor(UnresolvableType unresolvable) {
			// Constructor with unresolvable parameter - won't be matched
		}
	}

	// Concrete implementation of AbstractBeanWithUnresolvableConstructor
	public static class ConcreteAbstractBean extends AbstractBeanWithUnresolvableConstructor {
		private final String value;

		public ConcreteAbstractBean(String value) {
			super(null);
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	@Test
	void g02a_abstractClassWithFallback() {
		// This tests lines 1200-1208: Fallback supplier used for abstract class
		// Uses AbstractBeanWithUnresolvableConstructor so constructor attempt is skipped
		var fallback = new ConcreteAbstractBean("fallback-value");

		var creator = BeanCreator2.of(AbstractBeanWithUnresolvableConstructor.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.debug();

		var bean = creator.create();

		assertSame(fallback, bean);

		// Verify the debug log shows abstract class detection and fallback usage
		var log = creator.getDebugLog();
		assertContains("Bean type is abstract", log.toString());
		assertContains("Using fallback supplier", log.toString());
	}

	@Test
	void g02b_abstractClassWithFallbackSingleton() {
		// This tests line 1207: Fallback bean cached in singleton mode for abstract class
		// Uses AbstractBeanWithUnresolvableConstructor so constructor attempt is skipped
		var fallback = new ConcreteAbstractBean("fallback-value");

		var creator = BeanCreator2.of(AbstractBeanWithUnresolvableConstructor.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.singleton()
			.debug();

		var bean1 = creator.create();
		var bean2 = creator.create();

		// Both should return the same cached fallback instance
		assertSame(fallback, bean1);
		assertSame(fallback, bean2);
		assertSame(bean1, bean2);
	}

	@Test
	void g02c_abstractClassThrowsExceptionWithoutFallback() {
		// This tests line 1210: Exception thrown when abstract class has no fallback
		// Uses AbstractBeanWithUnresolvableConstructor so constructor attempt is skipped
		var creator = BeanCreator2.of(AbstractBeanWithUnresolvableConstructor.class)
			.beanStore(beanStore)
			.debug();

		// Should throw exception because abstract class and no fallback
		var exception = assertThrows(ExecutableException.class, () -> creator.create());

		assertContains("Could not instantiate class", exception.getMessage());
		assertContains("Class is abstract", exception.getMessage());

		// Verify the debug log shows abstract class detection
		var log = creator.getDebugLog();
		assertContains("Bean type is abstract", log.toString());
	}

	@Test
	void g03_interfaceThrowsException() {
		assertThrows(ExecutableException.class, () -> {
			BeanCreator2.of(BeanInterface.class)
				.beanStore(beanStore)
				.create();
		});
	}

	//====================================================================================================
	// Tests - Supplier conversion
	//====================================================================================================

	@Test
	void h01_asSupplier() {
		var supplier = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asSupplier();

		assertNotNull(supplier);
		var bean = supplier.get();
		assertNotNull(bean);
		assertInstanceOf(SimpleBean.class, bean);
	}

	@Test
	void h02_asSupplierReturnsNewInstanceEachTime() {
		var supplier = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asSupplier();

		var bean1 = supplier.get();
		var bean2 = supplier.get();

		assertNotNull(bean1);
		assertNotNull(bean2);
		assertNotSame(bean1, bean2);
	}

	@Test
	void h03_asResettableSupplier() {
		var supplier = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asResettableSupplier();

		assertNotNull(supplier);
		var bean = supplier.get();
		assertNotNull(bean);
		assertInstanceOf(SimpleBean.class, bean);
	}

	@Test
	void h04_asResettableSupplierCachesResult() {
		var supplier = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asResettableSupplier();

		var bean1 = supplier.get();
		var bean2 = supplier.get();

		assertNotNull(bean1);
		assertNotNull(bean2);
		assertSame(bean1, bean2, "ResettableSupplier should cache the result");
	}

	@Test
	void h05_asResettableSupplierResetRecreates() {
		var supplier = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asResettableSupplier();

		var bean1 = supplier.get();
		supplier.reset();
		var bean2 = supplier.get();

		assertNotNull(bean1);
		assertNotNull(bean2);
		assertNotSame(bean1, bean2, "Reset should force recreation");
	}

	@Test
	void h06_asResettableSupplierWithDependencies() {
		var writableStore = new BasicBeanStore2(null);
		writableStore.addBean(String.class, "initial");

		var supplier = BeanCreator2.of(BeanWithString.class)
			.beanStore(writableStore)
			.asResettableSupplier();

		var bean1 = supplier.get();
		assertEquals("initial", bean1.getValue());

		// Update dependency
		writableStore.addBean(String.class, "updated");

		// Without reset, still returns cached bean with old dependency
		var bean2 = supplier.get();
		assertSame(bean1, bean2);
		assertEquals("initial", bean2.getValue());

		// After reset, creates new bean with updated dependency
		supplier.reset();
		var bean3 = supplier.get();
		assertNotSame(bean1, bean3);
		assertEquals("updated", bean3.getValue());
	}

	@Test
	void h07_asResettableSupplierWithPostCreateHooks() {
		var hookCallCount = new int[]{0};

		var supplier = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.run(b -> hookCallCount[0]++)
			.asResettableSupplier();

		supplier.get();
		assertEquals(1, hookCallCount[0]);

		// Cached, hook not called again
		supplier.get();
		assertEquals(1, hookCallCount[0]);

		// Reset, hook called on recreation
		supplier.reset();
		supplier.get();
		assertEquals(2, hookCallCount[0]);
	}

	@Test
	void h08_asResettableSupplierWithSingleton() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.singleton();

		var supplier = creator.asResettableSupplier();

		var bean1 = supplier.get();
		var bean2 = creator.create();
		var bean3 = supplier.get();

		// All should be the same due to singleton mode
		assertSame(bean1, bean2);
		assertSame(bean1, bean3);

		// Reset the supplier
		supplier.reset();
		var bean4 = supplier.get();

		// Should still be same due to creator's singleton cache
		assertSame(bean1, bean4);
	}

	@Test
	void h09_asResettableSupplierOptionalMethods() {
		var supplier = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asResettableSupplier();

		// Test Optional-like methods inherited from OptionalSupplier
		assertTrue(supplier.isPresent());
		assertFalse(supplier.isEmpty());

		var mapped = supplier.map(b -> b.getClass().getSimpleName());
		assertEquals("SimpleBean", mapped.orElse(null));
	}

	// Helper bean for dependency testing
	public static class BeanWithString {
		private final String value;

		public BeanWithString(String value) {
			this.value = value;
		}

		public String getValue() { return value; }
	}

	//====================================================================================================
	// Tests - Method chaining
	//====================================================================================================

	@Test
	void i01_beanStoreReturnsThis() {
		var creator = BeanCreator2.of(SimpleBean.class);
		var result = creator.beanStore(beanStore);
		assertSame(creator, result);
	}

	@SuppressWarnings("rawtypes")
	@Test
	void i02_beanSubTypeReturnsThis() {
		var creator = BeanCreator2.of(ParentBean.class);
		var result = creator.beanSubType((Class)ChildBean.class);
		assertSame(creator, result);
	}

	@Test
	void i03_addBeanReturnsThis() {
		var creator = BeanCreator2.of(SimpleBean.class);
		var result = creator.addBean(TestService.class, new TestService("test"));
		assertSame(creator, result);
	}

	@Test
	void i05_builderReturnsThis() {
		var creator = BeanCreator2.of(BeanWithBuilder.class);
		var result = creator.builder(BeanWithBuilder.create());
		assertSame(creator, result);
	}

	@Test
	void i06_enclosingInstanceReturnsThis() {
		var creator = BeanCreator2.of(InnerBean.class);
		var result = creator.enclosingInstance(new BeanCreator2_Test());
		assertSame(creator, result);
	}

	@Test
	void i07_addMethodReturnsBean() {
		var service = new TestService("test");
		var creator = BeanCreator2.of(BeanWithDependencies.class);

		var result = creator.add(TestService.class, service);

		assertSame(service, result);

		// Verify the bean was added by using it in bean creation
		creator.addBean(AnotherService.class, new AnotherService(42));
		var bean = creator.create();
		assertSame(service, bean.getService());
	}

	@Test
	void i08_implementationMethodSetsExistingBean() {
		// Create a pre-configured bean instance
		var preConfiguredBean = new SimpleBean();

		// Use implementation() to provide the pre-configured bean
		var result = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.implementation(preConfiguredBean)
			.create();

		// The returned bean should be the same instance we provided
		assertSame(preConfiguredBean, result);
	}

	@Test
	void i09_implementationMethodBypassesCreation() {
		// Create a counter to track if constructor was called
		var counter = new int[]{0};

		// Create a bean with a constructor that increments the counter
		var bean = new SimpleBean() {
			{ counter[0]++; }
		};

		// Use implementation() to provide the bean - constructor should not be called again
		var result = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.implementation(bean)
			.create();

		// The returned bean should be the same instance (constructor only called once for original bean)
		assertSame(bean, result);
		assertEquals(1, counter[0], "Constructor should only be called once for the original bean");
	}

	//====================================================================================================
	// Tests - Singleton mode
	//====================================================================================================

	@Test
	void k01_singletonReturnsSameInstance() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.singleton();

		var bean1 = creator.create();
		var bean2 = creator.create();
		var bean3 = creator.create();

		assertNotNull(bean1);
		assertSame(bean1, bean2, "Second call should return same instance");
		assertSame(bean1, bean3, "Third call should return same instance");
	}

	@Test
	void k02_singletonWithConstructorParams() {
		var service = new TestService("test");
		var another = new AnotherService(42);
		beanStore.add(TestService.class, service);
		beanStore.add(AnotherService.class, another);

		var creator = BeanCreator2.of(BeanWithDependencies.class)
			.beanStore(beanStore)
			.singleton();

		var bean1 = creator.create();
		var bean2 = creator.create();

		assertNotNull(bean1);
		assertSame(bean1, bean2);
		assertSame(service, bean1.getService());
		assertSame(another, bean1.getAnother());
	}

	@Test
	void k03_singletonWithBuilder() {
		var creator = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore)
			.singleton();

		var bean1 = creator.create();
		var bean2 = creator.create();

		assertNotNull(bean1);
		assertSame(bean1, bean2);
	}

	@Test
	void k04_singletonWithGetInstance() {
		var creator = BeanCreator2.of(SingletonBean.class)
			.beanStore(beanStore)
			.singleton();

		var bean1 = creator.create();
		var bean2 = creator.create();

		assertNotNull(bean1);
		assertSame(bean1, bean2);
		// Note: SingletonBean has its own singleton pattern, but our creator's
		// singleton mode should still cache the instance
	}

	@Test
	void k05_singletonWithImplementation() {
		var preConfiguredBean = new SimpleBean();

		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.implementation(preConfiguredBean)
			.singleton();

		var bean1 = creator.create();
		var bean2 = creator.create();

		assertSame(preConfiguredBean, bean1);
		assertSame(preConfiguredBean, bean2);
	}

	@Test
	void k06_singletonWithInjection() {
		var service = new TestService("test");
		var another = new AnotherService(42);
		beanStore.add(TestService.class, service);
		beanStore.add(AnotherService.class, another);

		var creator = BeanCreator2.of(BeanWithInjectedFields.class)
			.beanStore(beanStore)
			.singleton();

		var bean1 = creator.create();
		var bean2 = creator.create();

		assertNotNull(bean1);
		assertSame(bean1, bean2);
		// Injection should only happen once
		assertSame(service, bean1.service);
		assertSame(another, bean1.another);
	}

	@Test
	void k07_nonSingletonCreatesMultipleInstances() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore);
			// Note: no .singleton() call

		var bean1 = creator.create();
		var bean2 = creator.create();
		var bean3 = creator.create();

		assertNotNull(bean1);
		assertNotNull(bean2);
		assertNotNull(bean3);
		assertNotSame(bean1, bean2, "Without singleton mode, should create new instances");
		assertNotSame(bean1, bean3, "Without singleton mode, should create new instances");
		assertNotSame(bean2, bean3, "Without singleton mode, should create new instances");
	}

	@Test
	void k08_singletonWithSupplier() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.singleton();

		var supplier = creator.asSupplier();
		var bean1 = supplier.get();
		var bean2 = supplier.get();
		var bean3 = creator.create();

		assertNotNull(bean1);
		assertSame(bean1, bean2, "Supplier should return same singleton instance");
		assertSame(bean1, bean3, "Direct create() should return same singleton instance");
	}

	// Bean that counts constructor invocations
	public static class CountingBean {
		private static int constructorCount = 0;

		public CountingBean() {
			constructorCount++;
		}

		public static int getConstructorCount() {
			return constructorCount;
		}

		public static void resetCount() {
			constructorCount = 0;
		}
	}

	@Test
	void k09_singletonOnlyCallsConstructorOnce() {
		CountingBean.resetCount();

		var creator = BeanCreator2.of(CountingBean.class)
			.beanStore(beanStore)
			.singleton();

		assertEquals(0, CountingBean.getConstructorCount());

		creator.create();
		assertEquals(1, CountingBean.getConstructorCount(), "Constructor should be called once");

		creator.create();
		assertEquals(1, CountingBean.getConstructorCount(), "Constructor should not be called again");

		creator.create();
		assertEquals(1, CountingBean.getConstructorCount(), "Constructor should still not be called again");
	}

	@Test
	void k10_nonSingletonCallsConstructorMultipleTimes() {
		CountingBean.resetCount();

		var creator = BeanCreator2.of(CountingBean.class)
			.beanStore(beanStore);
			// Note: no .singleton() call

		assertEquals(0, CountingBean.getConstructorCount());

		creator.create();
		assertEquals(1, CountingBean.getConstructorCount());

		creator.create();
		assertEquals(2, CountingBean.getConstructorCount(), "Constructor should be called for each create()");

		creator.create();
		assertEquals(3, CountingBean.getConstructorCount(), "Constructor should be called for each create()");
	}

	//====================================================================================================
	// Tests - Post-creation hooks
	//====================================================================================================

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

	@Test
	void l01_singleHook() {
		var bean = BeanCreator2.of(InitializableBean.class)
			.beanStore(beanStore)
			.run(b -> b.initialize())
			.create();

		assertTrue(bean.isInitialized());
		assertEquals(1, bean.getInitCount());
	}

	@Test
	void l02_multipleHooks() {
		var counter = new int[]{0};

		var bean = BeanCreator2.of(InitializableBean.class)
			.beanStore(beanStore)
			.run(b -> b.initialize())
			.run(b -> counter[0]++)
			.run(b -> counter[0]++)
			.create();

		assertTrue(bean.isInitialized());
		assertEquals(2, counter[0]);
	}

	@Test
	void l03_hooksExecutedInOrder() {
		var log = new ArrayList<String>();

		var bean = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.run(b -> log.add("first"))
			.run(b -> log.add("second"))
			.run(b -> log.add("third"))
			.create();

		assertNotNull(bean);
		assertEquals(Arrays.asList("first", "second", "third"), log);
	}

	@Test
	void l04_hookWithBeanAccess() {
		var service = new TestService("test");
		var another = new AnotherService(42);
		beanStore.add(TestService.class, service);
		beanStore.add(AnotherService.class, another);

		var bean = BeanCreator2.of(BeanWithDependencies.class)
			.beanStore(beanStore)
			.run(b -> {
				// Hook can access injected dependencies
				assertSame(service, b.getService());
				assertSame(another, b.getAnother());
			})
			.create();

		assertNotNull(bean);
	}

	@Test
	void l05_hookWithSingleton() {
		var callCount = new int[]{0};

		var creator = BeanCreator2.of(InitializableBean.class)
			.beanStore(beanStore)
			.singleton()
			.run(b -> {
				b.initialize();
				callCount[0]++;
			});

		var bean1 = creator.create();
		var bean2 = creator.create();
		var bean3 = creator.create();

		assertSame(bean1, bean2);
		assertSame(bean1, bean3);
		assertTrue(bean1.isInitialized());
		assertEquals(1, callCount[0], "Hook should only be called once in singleton mode");
	}

	@Test
	void l06_hookWithoutSingleton() {
		var callCount = new int[]{0};

		var creator = BeanCreator2.of(InitializableBean.class)
			.beanStore(beanStore)
			.run(b -> {
				b.initialize();
				callCount[0]++;
			});

		var bean1 = creator.create();
		var bean2 = creator.create();
		var bean3 = creator.create();

		assertNotSame(bean1, bean2);
		assertTrue(bean1.isInitialized());
		assertTrue(bean2.isInitialized());
		assertTrue(bean3.isInitialized());
		assertEquals(3, callCount[0], "Hook should be called for each create()");
	}

	@Test
	void l07_hookWithBuilder() {
		var initialized = new boolean[]{false};

		var bean = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore)
			.run(b -> initialized[0] = true)
			.create();

		assertNotNull(bean);
		assertTrue(initialized[0]);
	}

	@Test
	void l08_hookRunsAfterInjection() {
		var service = new TestService("test");
		var another = new AnotherService(42);
		beanStore.add(TestService.class, service);
		beanStore.add(AnotherService.class, another);

		var serviceWasInjected = new boolean[]{false};

		var bean = BeanCreator2.of(BeanWithInjectedFields.class)
			.beanStore(beanStore)
			.run(b -> {
				// At this point, injection should already be done
				serviceWasInjected[0] = (b.service != null);
			})
			.create();

		assertTrue(serviceWasInjected[0], "Service should be injected before hook runs");
		assertSame(service, bean.service);
	}

	@Test
	void l09_hookWithImplementation() {
		var preConfiguredBean = new InitializableBean();
		var hookRan = new boolean[]{false};

		var bean = BeanCreator2.of(InitializableBean.class)
			.beanStore(beanStore)
			.implementation(preConfiguredBean)
			.run(b -> {
				b.initialize();
				hookRan[0] = true;
			})
			.create();

		assertSame(preConfiguredBean, bean);
		assertTrue(bean.isInitialized());
		assertTrue(hookRan[0]);
	}

	@Test
	void l10_hookExceptionPropagates() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.run(b -> {
				throw new RuntimeException("Hook error");
			});

		assertThrows(RuntimeException.class, () -> creator.create(), "Hook exception should propagate");
	}

	@Test
	void l11_multipleHooksFirstFails() {
		var secondHookRan = new boolean[]{false};

		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.run(b -> {
				throw new RuntimeException("First hook error");
			})
			.run(b -> secondHookRan[0] = true);

		assertThrows(RuntimeException.class, () -> creator.create());
		assertFalse(secondHookRan[0], "Second hook should not run if first hook fails");
	}

	// Bean that tracks method call order
	public static class OrderTrackingBean {
		private final List<String> callOrder = new ArrayList<>();

		@Inject
		TestService service;

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

	@Test
	void l12_hooksRunAfterAllInjection() {
		var service = new TestService("test");
		var another = new AnotherService(42);
		beanStore.add(TestService.class, service);
		beanStore.add(AnotherService.class, another);

		var bean = BeanCreator2.of(OrderTrackingBean.class)
			.beanStore(beanStore)
			.run(b -> b.step1())
			.run(b -> b.step2())
			.create();

		// Injection should happen first, then hooks
		assertEquals(Arrays.asList("inject-method", "step1", "step2"), bean.getCallOrder());
	}

	//====================================================================================================
	// Tests - Custom factory methods
	//====================================================================================================

	// Bean with 'of' factory method
	public static class BeanWithOfMethod {
		private final String value;

		private BeanWithOfMethod(String value) {
			this.value = value;
		}

		public static BeanWithOfMethod of(String value) {
			return new BeanWithOfMethod(value);
		}

		public String getValue() { return value; }
	}

	@Test
	void m01_customFactoryMethodOf() {
		beanStore.add(String.class, "test-value");

		var bean = BeanCreator2.of(BeanWithOfMethod.class)
			.beanStore(beanStore)
			.factoryMethod("of")
			.create();

		assertNotNull(bean);
		assertEquals("test-value", bean.getValue());
	}

	// Bean with 'from' factory method
	public static class BeanWithFromMethod {
		private final String value;

		private BeanWithFromMethod(String value) {
			this.value = value;
		}

		public static BeanWithFromMethod from(String value) {
			return new BeanWithFromMethod(value);
		}

		public String getValue() { return value; }
	}

	@Test
	void m02_customFactoryMethodFrom() {
		beanStore.add(String.class, "test-value");

		var bean = BeanCreator2.of(BeanWithFromMethod.class)
			.beanStore(beanStore)
			.factoryMethod("from")
			.create();

		assertNotNull(bean);
		assertEquals("test-value", bean.getValue());
	}

	// Bean with 'create' factory method
	public static class BeanWithCreateMethod {
		private final String value;

		private BeanWithCreateMethod(String value) {
			this.value = value;
		}

		public static BeanWithCreateMethod create(String value) {
			return new BeanWithCreateMethod(value);
		}

		public String getValue() { return value; }
	}

	@Test
	void m03_customFactoryMethodCreate() {
		beanStore.add(String.class, "test-value");

		var bean = BeanCreator2.of(BeanWithCreateMethod.class)
			.beanStore(beanStore)
			.factoryMethod("create")
			.create();

		assertNotNull(bean);
		assertEquals("test-value", bean.getValue());
	}

	// Bean with 'newInstance' factory method
	public static class BeanWithNewInstanceMethod {
		private final String value;

		private BeanWithNewInstanceMethod(String value) {
			this.value = value;
		}

		public static BeanWithNewInstanceMethod newInstance(String value) {
			return new BeanWithNewInstanceMethod(value);
		}

		public String getValue() { return value; }
	}

	@Test
	void m04_customFactoryMethodNewInstance() {
		beanStore.add(String.class, "test-value");

		var bean = BeanCreator2.of(BeanWithNewInstanceMethod.class)
			.beanStore(beanStore)
			.factoryMethod("newInstance")
			.create();

		assertNotNull(bean);
		assertEquals("test-value", bean.getValue());
	}

	// Bean with multiple factory methods
	public static class BeanWithMultipleFactoryMethods {
		private final String source;

		private BeanWithMultipleFactoryMethods(String source) {
			this.source = source;
		}

		public static BeanWithMultipleFactoryMethods of(String value) {
			return new BeanWithMultipleFactoryMethods("of");
		}

		public static BeanWithMultipleFactoryMethods from(String value) {
			return new BeanWithMultipleFactoryMethods("from");
		}

		public static BeanWithMultipleFactoryMethods getInstance(String value) {
			return new BeanWithMultipleFactoryMethods("getInstance");
		}

		public String getSource() { return source; }
	}

	@Test
	void m05_multipleFactoryMethodNames() {
		beanStore.add(String.class, "test-value");

		var bean = BeanCreator2.of(BeanWithMultipleFactoryMethods.class)
			.beanStore(beanStore)
			.factoryMethod("of", "from", "newInstance")
			.create();

		assertNotNull(bean);
		// Should find one of the configured methods
		assertTrue(bean.getSource().equals("of") || bean.getSource().equals("from"));
	}

	@Test
	void m06_defaultGetInstanceStillWorks() {
		// Without calling factoryMethod(), getInstance should still work
		var service = new TestService("test");
		beanStore.add(TestService.class, service);

		var bean = BeanCreator2.of(SingletonBeanWithParams.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertSame(service, bean.getService());
	}

	// Bean with builder and custom factory method (but no factory that accepts builder)
	public static class BeanWithBuilderAndCustomFactoryMethod {
		private final String value;

		private BeanWithBuilderAndCustomFactoryMethod(String value) {
			this.value = value;
		}

		public static BuilderForCustomFactory builder() {
			return new BuilderForCustomFactory();
		}

		public static BeanWithBuilderAndCustomFactoryMethod of(String value) {
			return new BeanWithBuilderAndCustomFactoryMethod(value);
		}

		public String getValue() { return value; }

		public static class BuilderForCustomFactory {
			private String value;

			public BuilderForCustomFactory value(String value) {
				this.value = value;
				return this;
			}

			public BeanWithBuilderAndCustomFactoryMethod build() {
				return new BeanWithBuilderAndCustomFactoryMethod(value);
			}
		}
	}

	@Test
	void m07_customFactoryMethodVsBuilder() {
		beanStore.add(String.class, "test-value");

		// When both factory method and builder exist, builder takes precedence
		var bean = BeanCreator2.of(BeanWithBuilderAndCustomFactoryMethod.class)
			.beanStore(beanStore)
			.factoryMethod("of")
			.create();

		assertNotNull(bean);
		// Builder is used (it's found first in creation order)
		assertNull(bean.getValue());
	}

	// Bean with no-arg factory method
	public static class BeanWithNoArgFactoryMethod {
		private static final BeanWithNoArgFactoryMethod INSTANCE = new BeanWithNoArgFactoryMethod();

		private BeanWithNoArgFactoryMethod() {}

		public static BeanWithNoArgFactoryMethod of() {
			return INSTANCE;
		}
	}

	@Test
	void m08_noArgCustomFactoryMethod() {
		var bean = BeanCreator2.of(BeanWithNoArgFactoryMethod.class)
			.beanStore(beanStore)
			.factoryMethod("of")
			.create();

		assertNotNull(bean);
		assertSame(BeanWithNoArgFactoryMethod.INSTANCE, bean);
	}

	// Bean where custom factory has most parameters
	public static class BeanWithMultipleFactoryOverloads {
		private final int paramCount;

		private BeanWithMultipleFactoryOverloads(int paramCount) {
			this.paramCount = paramCount;
		}

		public static BeanWithMultipleFactoryOverloads of() {
			return new BeanWithMultipleFactoryOverloads(0);
		}

		public static BeanWithMultipleFactoryOverloads of(String s) {
			return new BeanWithMultipleFactoryOverloads(1);
		}

		public static BeanWithMultipleFactoryOverloads of(String s, TestService ts) {
			return new BeanWithMultipleFactoryOverloads(2);
		}

		public int getParamCount() { return paramCount; }
	}

	@Test
	void m09_factoryMethodMostParametersSelected() {
		beanStore.add(String.class, "test");
		beanStore.add(TestService.class, new TestService("test"));

		var bean = BeanCreator2.of(BeanWithMultipleFactoryOverloads.class)
			.beanStore(beanStore)
			.factoryMethod("of")
			.create();

		assertNotNull(bean);
		assertEquals(2, bean.getParamCount(), "Should select factory with most resolvable parameters");
	}

	@Test
	void m10_replacementFactoryMethodBehavior() {
		beanStore.add(String.class, "test-value");

		// Second call should replace first call (not cumulative)
		var bean = BeanCreator2.of(BeanWithMultipleFactoryMethods.class)
			.beanStore(beanStore)
			.factoryMethod("of")
			.factoryMethod("from")  // This replaces "of"
			.create();

		assertNotNull(bean);
		// Should find 'from' (not 'of' since it was replaced)
		assertEquals("from", bean.getSource());
	}

	// Bean with custom builder method name
	public static class BeanWithCustomBuilderMethod {
		private final String value;

		private BeanWithCustomBuilderMethod(CustomBuilder builder) {
			this.value = builder.value;
		}

		public String getValue() { return value; }

		// Custom builder factory method named "newBuilder" instead of "create" or "builder"
		public static CustomBuilder newBuilder() {
			return new CustomBuilder();
		}

		public static class CustomBuilder {
			private String value;

			public CustomBuilder value(String value) {
				this.value = value;
				return this;
			}

			public BeanWithCustomBuilderMethod build() {
				return new BeanWithCustomBuilderMethod(this);
			}
		}
	}

	@Test
	void m11_customBuilderMethodName() {
		// Should be able to find builder using custom method name
		var bean = BeanCreator2.of(BeanWithCustomBuilderMethod.class)
			.beanStore(beanStore)
			.builderMethod("newBuilder")
			.create();

		assertNotNull(bean);
	}

	// Bean with multiple builder method names
	public static class BeanWithMultipleBuilderMethods {
		private final String source;

		private BeanWithMultipleBuilderMethods(MultiBuilder builder) {
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

			public BeanWithMultipleBuilderMethods build() {
				return new BeanWithMultipleBuilderMethods(this);
			}
		}
	}

	@Test
	void m12_replacementBuilderMethodBehavior() {
		// Second call should replace first call (not cumulative)
		var bean = BeanCreator2.of(BeanWithMultipleBuilderMethods.class)
			.beanStore(beanStore)
			.builderMethod("newBuilder")
			.builderMethod("instance")  // This replaces "newBuilder"
			.create();

		assertNotNull(bean);
		// Should find 'instance' (not 'newBuilder' since it was replaced)
		assertEquals("instance", bean.getSource());
	}

	//====================================================================================================
	// Tests - Builder customization
	//====================================================================================================

	// Bean with configurable builder
	public static class BeanWithConfigurableBuilder {
		private final int timeout;
		private final int retries;
		private final boolean enabled;

		private BeanWithConfigurableBuilder(ConfigurableBuilder builder) {
			this.timeout = builder.timeout;
			this.retries = builder.retries;
			this.enabled = builder.enabled;
		}

		public static ConfigurableBuilder builder() {
			return new ConfigurableBuilder();
		}

		public int getTimeout() { return timeout; }
		public int getRetries() { return retries; }
		public boolean isEnabled() { return enabled; }

		public static class ConfigurableBuilder {
			private int timeout = 1000;  // default
			private int retries = 1;     // default
			private boolean enabled = false;  // default

			public ConfigurableBuilder timeout(int timeout) {
				this.timeout = timeout;
				return this;
			}

			public ConfigurableBuilder retries(int retries) {
				this.retries = retries;
				return this;
			}

			public ConfigurableBuilder enabled(boolean enabled) {
				this.enabled = enabled;
				return this;
			}

			public BeanWithConfigurableBuilder build() {
				return new BeanWithConfigurableBuilder(this);
			}
		}
	}

	@Test
	void n01_basicBuilderCustomization() {
		var bean = BeanCreator2.of(BeanWithConfigurableBuilder.class)
			.beanStore(beanStore)
			.customizeBuilder(b -> {
				BeanWithConfigurableBuilder.ConfigurableBuilder builder =
					(BeanWithConfigurableBuilder.ConfigurableBuilder)b;
				builder.timeout(5000);
				builder.retries(3);
				builder.enabled(true);
			})
			.create();

		assertNotNull(bean);
		assertEquals(5000, bean.getTimeout());
		assertEquals(3, bean.getRetries());
		assertTrue(bean.isEnabled());
	}

	@Test
	void n02_builderCustomizationWithDefaults() {
		// Without customization, should use builder defaults
		var bean = BeanCreator2.of(BeanWithConfigurableBuilder.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);
		assertEquals(1000, bean.getTimeout(), "Should use builder default");
		assertEquals(1, bean.getRetries(), "Should use builder default");
		assertFalse(bean.isEnabled(), "Should use builder default");
	}

	@Test
	void n03_partialBuilderCustomization() {
		// Customize only some properties
		var bean = BeanCreator2.of(BeanWithConfigurableBuilder.class)
			.beanStore(beanStore)
			.customizeBuilder(b -> {
				BeanWithConfigurableBuilder.ConfigurableBuilder builder =
					(BeanWithConfigurableBuilder.ConfigurableBuilder)b;
				builder.timeout(3000);  // Only customize timeout
			})
			.create();

		assertNotNull(bean);
		assertEquals(3000, bean.getTimeout(), "Should use customized value");
		assertEquals(1, bean.getRetries(), "Should use builder default");
		assertFalse(bean.isEnabled(), "Should use builder default");
	}

	// Bean with builder that has injectable fields
	public static class BeanWithInjectableBuilder {
		private final String value;
		private final int number;

		private BeanWithInjectableBuilder(InjectableBuilder builder) {
			this.value = builder.injectedValue;
			this.number = builder.customNumber;
		}

		public static InjectableBuilder builder() {
			return new InjectableBuilder();
		}

		public String getValue() { return value; }
		public int getNumber() { return number; }

		public static class InjectableBuilder {
			@Inject
			TestService injectedService;

			private String injectedValue;
			private int customNumber = 0;

			public InjectableBuilder number(int number) {
				this.customNumber = number;
				return this;
			}

			public BeanWithInjectableBuilder build() {
				this.injectedValue = injectedService != null ? injectedService.getName() : null;
				return new BeanWithInjectableBuilder(this);
			}
		}
	}

	@Test
	void n04_customizerRunsAfterInjection() {
		var service = new TestService("injected-service");
		beanStore.add(TestService.class, service);

		var bean = BeanCreator2.of(BeanWithInjectableBuilder.class)
			.beanStore(beanStore)
			.customizeBuilder(b -> {
				BeanWithInjectableBuilder.InjectableBuilder builder =
					(BeanWithInjectableBuilder.InjectableBuilder)b;
				// At this point, injection should already be done
				assertNotNull(builder.injectedService, "Service should be injected before customizer runs");
				builder.number(42);
			})
			.create();

		assertNotNull(bean);
		assertEquals("injected-service", bean.getValue());
		assertEquals(42, bean.getNumber());
	}

	@Test
	void n05_customizerWithSingleton() {
		var callCount = new int[]{0};

		var creator = BeanCreator2.of(BeanWithConfigurableBuilder.class)
			.beanStore(beanStore)
			.singleton()
			.customizeBuilder(b -> {
				callCount[0]++;
				BeanWithConfigurableBuilder.ConfigurableBuilder builder =
					(BeanWithConfigurableBuilder.ConfigurableBuilder)b;
				builder.timeout(2000);
			});

		var bean1 = creator.create();
		var bean2 = creator.create();
		var bean3 = creator.create();

		assertSame(bean1, bean2);
		assertSame(bean1, bean3);
		assertEquals(2000, bean1.getTimeout());
		assertEquals(1, callCount[0], "Customizer should only be called once in singleton mode");
	}

	@Test
	void n06_customizerWithoutSingleton() {
		var callCount = new int[]{0};

		var creator = BeanCreator2.of(BeanWithConfigurableBuilder.class)
			.beanStore(beanStore)
			.customizeBuilder(b -> {
				callCount[0]++;
				BeanWithConfigurableBuilder.ConfigurableBuilder builder =
					(BeanWithConfigurableBuilder.ConfigurableBuilder)b;
				builder.timeout(2000);
			});

		var bean1 = creator.create();
		var bean2 = creator.create();
		var bean3 = creator.create();

		assertNotSame(bean1, bean2);
		assertEquals(2000, bean1.getTimeout());
		assertEquals(2000, bean2.getTimeout());
		assertEquals(2000, bean3.getTimeout());
		assertEquals(3, callCount[0], "Customizer should be called for each create()");
	}

	@Test
	void n07_customizerNotCalledWithoutBuilder() {
		var customizerCalled = new boolean[]{false};

		// SimpleBean has no builder, so customizer should not be called
		var bean = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.customizeBuilder(b -> {
				customizerCalled[0] = true;
			})
			.create();

		assertNotNull(bean);
		assertFalse(customizerCalled[0], "Customizer should not be called when no builder is used");
	}

	@Test
	void n08_customizerNotCalledWithImplementation() {
		var customizerCalled = new boolean[]{false};
		var preConfiguredBean = new BeanWithConfigurableBuilder.ConfigurableBuilder()
			.timeout(9999)
			.build();

		var bean = BeanCreator2.of(BeanWithConfigurableBuilder.class)
			.beanStore(beanStore)
			.implementation(preConfiguredBean)
			.customizeBuilder(b -> {
				customizerCalled[0] = true;
			})
			.create();

		assertSame(preConfiguredBean, bean);
		assertFalse(customizerCalled[0], "Customizer should not be called when implementation() is used");
	}

	@Test
	void n09_customizerExceptionPropagates() {
		var creator = BeanCreator2.of(BeanWithConfigurableBuilder.class)
			.beanStore(beanStore)
			.customizeBuilder(b -> {
				throw new RuntimeException("Customizer error");
			});

		assertThrows(RuntimeException.class, () -> creator.create(),
			"Customizer exception should propagate");
	}

	@Test
	void n10_lastCustomizerWins() {
		// Only one customizer can be registered; last one wins
		var bean = BeanCreator2.of(BeanWithConfigurableBuilder.class)
			.beanStore(beanStore)
			.customizeBuilder(b -> {
				BeanWithConfigurableBuilder.ConfigurableBuilder builder =
					(BeanWithConfigurableBuilder.ConfigurableBuilder)b;
				builder.timeout(1111);
			})
			.customizeBuilder(b -> {
				BeanWithConfigurableBuilder.ConfigurableBuilder builder =
					(BeanWithConfigurableBuilder.ConfigurableBuilder)b;
				builder.timeout(2222);
			})
			.create();

		assertNotNull(bean);
		assertEquals(2222, bean.getTimeout(), "Last customizer should win");
	}

	//====================================================================================================
	// Tests - Fallback/default instance
	//====================================================================================================

	// Bean that implements BeanInterface for fallback testing
	public static class ConcreteBeanInterface implements BeanInterface {
		public ConcreteBeanInterface() {}
	}

	@Test
	void o01_fallbackSupplierUsedWhenCreationFails() {
		// BeanInterface is an interface, cannot be instantiated, fallback should be used
		var fallback = new ConcreteBeanInterface();

		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(() -> fallback)
			.create();

		assertNotNull(bean);
		assertSame(fallback, bean);
	}

	@Test
	void o02_fallbackInstanceUsedWhenCreationFails() {
		var fallback = new ConcreteBeanInterface();

		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.create();

		assertNotNull(bean);
		assertSame(fallback, bean);
	}

	@Test
	void o03_fallbackNotUsedWhenCreationSucceeds() {
		var fallbackCalled = new boolean[]{false};

		var bean = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.orElse(() -> {
				fallbackCalled[0] = true;
				return new SimpleBean();
			})
			.create();

		assertNotNull(bean);
		assertInstanceOf(SimpleBean.class, bean);
		assertFalse(fallbackCalled[0], "Fallback should not be called when creation succeeds");
	}

	@Test
	void o04_fallbackWithInterface() {
		// BeanInterface is an interface, cannot be instantiated
		var fallback = new ConcreteBeanInterface();

		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.create();

		assertNotNull(bean);
		assertSame(fallback, bean);
	}

	// Bean with no matching constructor
	public static class BeanWithNoMatchingConstructor {
		public BeanWithNoMatchingConstructor(UnresolvableType param) {
			// Only constructor requires unresolvable type
		}
	}

	@Test
	void o05_fallbackWithNoMatchingConstructor() {
		var fallback = new BeanWithNoMatchingConstructor(null);

		var bean = BeanCreator2.of(BeanWithNoMatchingConstructor.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.create();

		assertNotNull(bean);
		assertSame(fallback, bean);
	}

	@Test
	void o06_fallbackWithSingleton() {
		var fallback = new ConcreteBeanInterface();

		var creator = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.singleton()
			.orElse(fallback);

		var bean1 = creator.create();
		var bean2 = creator.create();
		var bean3 = creator.create();

		assertSame(fallback, bean1);
		assertSame(bean1, bean2);
		assertSame(bean1, bean3);
	}

	@Test
	void o07_fallbackWithPostCreateHooks() {
		var hookCalled = new boolean[]{false};
		var fallback = new ConcreteBeanInterface();

		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.run(b -> hookCalled[0] = true)
			.create();

		assertSame(fallback, bean);
		assertTrue(hookCalled[0], "Post-create hooks should run on fallback");
	}

	@Test
	void o08_fallbackExceptionPropagates() {
		var creator = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(() -> {
				throw new RuntimeException("Fallback error");
			});

		assertThrows(RuntimeException.class, () -> creator.create(),
			"Fallback exception should propagate");
	}

	@Test
	void o09_lastFallbackWins() {
		var fallback1 = new ConcreteBeanInterface();
		var fallback2 = new ConcreteBeanInterface();

		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(fallback1)
			.orElse(fallback2)
			.create();

		assertSame(fallback2, bean, "Last fallback should win");
	}

	@Test
	void o10_noFallbackThrowsException() {
		// Without fallback, should throw exception
		var creator = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore);

		assertThrows(ExecutableException.class, () -> creator.create(),
			"Should throw when no fallback is provided");
	}

	@Test
	void o11_fallbackWithBuilderFailure() {
		// BeanInterface has no builder, cannot be instantiated
		var fallback = new ConcreteBeanInterface();

		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.create();

		assertSame(fallback, bean);
	}

	// Bean that requires specific dependency
	public static class BeanRequiringDependency {
		public BeanRequiringDependency(SpecificDependency dep) {
			// Requires SpecificDependency
		}
	}

	public static class SpecificDependency {
		public SpecificDependency() {}
	}

	public static class FallbackBeanRequiringDependency extends BeanRequiringDependency {
		public FallbackBeanRequiringDependency() {
			super(null);
		}
	}

	@Test
	void o12_fallbackForMissingDependency() {
		// BeanStore doesn't have SpecificDependency
		var fallback = new FallbackBeanRequiringDependency();

		var bean = BeanCreator2.of(BeanRequiringDependency.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.create();

		assertSame(fallback, bean);
	}

	@Test
	void o13_fallbackSupplierCalledEachTime() {
		var callCount = new int[]{0};

		var creator = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(() -> {
				callCount[0]++;
				return new ConcreteBeanInterface();
			});

		creator.create();
		creator.create();
		creator.create();

		assertEquals(3, callCount[0], "Fallback supplier should be called each time (without singleton)");
	}

	@Test
	void o14_fallbackSupplierOnceWithSingleton() {
		var callCount = new int[]{0};

		var creator = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.singleton()
			.orElse(() -> {
				callCount[0]++;
				return new ConcreteBeanInterface();
			});

		creator.create();
		creator.create();
		creator.create();

		assertEquals(1, callCount[0], "Fallback supplier should only be called once with singleton");
	}

	//====================================================================================================
	// Tests - tryCreate() non-throwing variant
	//====================================================================================================

	@Test
	void p01_asOptionalSucceeds() {
		var result = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asOptional();

		assertTrue(result.isPresent());
		assertNotNull(result.get());
		assertInstanceOf(SimpleBean.class, result.get());
	}

	@Test
	void p02_asOptionalFailsForInterface() {
		var result = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.asOptional();

		assertFalse(result.isPresent(), "Should return empty for interface");
	}

	@Test
	void p03_asOptionalFailsForAbstract() {
		var result = BeanCreator2.of(AbstractBean.class)
			.beanStore(beanStore)
			.asOptional();

		assertFalse(result.isPresent(), "Should return empty for abstract class");
	}

	@Test
	void p04_asOptionalFailsForNoMatchingConstructor() {
		var result = BeanCreator2.of(BeanWithNoMatchingConstructor.class)
			.beanStore(beanStore)
			.asOptional();

		assertFalse(result.isPresent(), "Should return empty when no constructor matches");
	}

	@Test
	void p05_asOptionalWithFallback() {
		var fallback = new ConcreteBeanInterface();

		// asOptional with fallback should succeed (fallback is used)
		var result = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.asOptional();

		assertTrue(result.isPresent(), "Should return present when fallback is used");
		assertSame(fallback, result.get());
	}

	@Test
	void p06_asOptionalOrElsePattern() {
		var defaultBean = new ConcreteBeanInterface();

		// Classic Optional.orElse pattern
		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.asOptional()
			.orElse(defaultBean);

		assertSame(defaultBean, bean);
	}

	@Test
	void p07_asOptionalChaining() {
		// Try primary, then fallback strategy with compatible types
		@SuppressWarnings("rawtypes")
		var bean = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.beanSubType((Class)AbstractBean.class)  // Force it to fail
			.asOptional()
			.or(() -> BeanCreator2.of(SimpleBean.class)
				.beanStore(beanStore)
				.asOptional())
			.orElseGet(() -> new SimpleBean());

		assertNotNull(bean);
		assertInstanceOf(SimpleBean.class, bean);
	}

	@Test
	void p08_asOptionalWithPostCreateHooks() {
		var hookCalled = new boolean[]{false};

		var result = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.run(b -> hookCalled[0] = true)
			.asOptional();

		assertTrue(result.isPresent());
		assertTrue(hookCalled[0], "Post-create hooks should run on success");
	}

	@Test
	void p09_asOptionalPostCreateHooksNotCalledOnFailure() {
		var hookCalled = new boolean[]{false};

		var result = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.run(b -> hookCalled[0] = true)
			.asOptional();

		assertFalse(result.isPresent());
		assertFalse(hookCalled[0], "Post-create hooks should not run on failure");
	}

	@Test
	void p10_asOptionalWithSingleton() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.singleton();

		var result1 = creator.asOptional();
		var result2 = creator.asOptional();

		assertTrue(result1.isPresent());
		assertTrue(result2.isPresent());
		assertSame(result1.get(), result2.get(), "Should return same instance in singleton mode");
	}

	@Test
	void p11_asOptionalSingletonCachesSuccessfulCreation() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.singleton();

		// First call succeeds and caches
		var result1 = creator.asOptional();
		assertTrue(result1.isPresent());

		// Even if we change to interface beanSubType (which would fail), cached instance is returned
		// This tests that singleton caching works correctly
		var result2 = creator.asOptional();
		assertTrue(result2.isPresent());
		assertSame(result1.get(), result2.get());
	}

	@Test
	void p12_asOptionalWithDependencies() {
		var service = new TestService("test-service");
		var another = new AnotherService(42);
		beanStore.add(TestService.class, service);
		beanStore.add(AnotherService.class, another);

		var result = BeanCreator2.of(BeanWithDependencies.class)
			.beanStore(beanStore)
			.asOptional();

		assertTrue(result.isPresent());
		assertNotNull(result.get().service);
		assertSame(service, result.get().service);
	}

	@Test
	void p13_asOptionalWithBuilder() {
		var result = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore)
			.asOptional();

		assertTrue(result.isPresent());
		assertNotNull(result.get());
	}

	@Test
	void p14_asOptionalIsPresent() {
		// Test the pattern: if (asOptional().isPresent())
		BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asOptional()
			.ifPresent(bean -> {
				assertNotNull(bean);
				assertInstanceOf(SimpleBean.class, bean);
			});
	}

	@Test
	void p15_asOptionalIsEmpty() {
		// Test the pattern: if (asOptional().isEmpty())
		var result = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.asOptional();

		assertTrue(result.isEmpty());
	}

	@Test
	void p16_asOptionalOrElseThrow() {
		// Pattern: asOptional().orElseThrow()
		assertThrows(NoSuchElementException.class, () -> {
			BeanCreator2.of(BeanInterface.class)
				.beanStore(beanStore)
				.asOptional()
				.orElseThrow();
		});
	}

	@Test
	void p17_asOptionalMap() {
		// Pattern: asOptional().map(...)
		var result = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asOptional()
			.map(bean -> bean.getClass().getSimpleName());

		assertTrue(result.isPresent());
		assertEquals("SimpleBean", result.get());
	}

	@Test
	void p18_asOptionalFilter() {
		// Pattern: asOptional().filter(...)
		var result = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.asOptional()
			.filter(bean -> bean != null);

		assertTrue(result.isPresent());
	}

	@Test
	void p19_asOptionalFallbackExceptionPropagates() {
		// Fallback exceptions should propagate (not caught by asOptional)
		assertThrows(RuntimeException.class, () -> {
			BeanCreator2.of(BeanInterface.class)
				.beanStore(beanStore)
				.orElse(() -> {
					throw new RuntimeException("Fallback error");
				})
				.asOptional();
		}, "Fallback exceptions should propagate even in tryCreate");
	}

	@Test
	void p20_asOptionalPostHookExceptionPropagates() {
		// Post-creation hook exceptions should propagate (not caught by asOptional)
		assertThrows(RuntimeException.class, () -> {
			BeanCreator2.of(SimpleBean.class)
				.beanStore(beanStore)
				.run(b -> {
					throw new RuntimeException("Hook error");
				})
				.asOptional();
		}, "Post-create hook exceptions should propagate even in tryCreate");
	}

	//====================================================================================================
	// Tests - Bean validation
	//====================================================================================================

	// Bean with validation state
	public static class ValidatableBean {
		private boolean valid = true;
		private String name;
		private int value;

		public ValidatableBean() {}

		public boolean isValid() { return valid; }
		public void setValid(boolean valid) { this.valid = valid; }
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public int getValue() { return value; }
		public void setValue(int value) { this.value = value; }
	}

	@Test
	void q01_validateSuccess() {
		var bean = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.validate(b -> b.isValid())
			.create();

		assertNotNull(bean);
		assertTrue(bean.isValid());
	}

	@Test
	void q02_validateFailure() {
		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.run(b -> b.setValid(false))
			.validate(b -> b.isValid());

		var ex = assertThrows(ExecutableException.class, () -> creator.create());
		assertTrue(ex.getMessage().contains("Bean validation failed"));
	}

	@Test
	void q03_validateMultipleConditions() {
		var bean = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.run(b -> {
				b.setName("test");
				b.setValue(42);
			})
			.validate(b -> b.getName() != null && b.getValue() > 0)
			.create();

		assertNotNull(bean);
		assertEquals("test", bean.getName());
		assertEquals(42, bean.getValue());
	}

	@Test
	void q04_validateMultipleConditionsFailure() {
		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.run(b -> {
				b.setName("test");
				b.setValue(-1);  // Invalid value
			})
			.validate(b -> b.getName() != null && b.getValue() > 0);

		assertThrows(ExecutableException.class, () -> creator.create());
	}

	@Test
	void q05_validateRunsAfterPostCreateHooks() {
		var hookOrder = new ArrayList<String>();

		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.run(b -> {
				hookOrder.add("hook");
				b.setValid(false);
			})
			.validate(b -> {
				hookOrder.add("validate");
				return b.isValid();
			});

		assertThrows(ExecutableException.class, () -> creator.create());
		assertEquals(Arrays.asList("hook", "validate"), hookOrder);
	}

	@Test
	void q06_validateRunsAfterInjection() {
		var service = new TestService("test-service");
		var another = new AnotherService(42);
		beanStore.add(TestService.class, service);
		beanStore.add(AnotherService.class, another);

		var bean = BeanCreator2.of(BeanWithDependencies.class)
			.beanStore(beanStore)
			.validate(b -> b.service != null && b.another != null)
			.create();

		assertNotNull(bean.service);
		assertSame(service, bean.service);
	}

	@Test
	void q07_validateWithSingleton() {
		var validationCount = new int[]{0};

		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.singleton()
			.validate(b -> {
				validationCount[0]++;
				return b.isValid();
			});

		creator.create();
		creator.create();
		creator.create();

		assertEquals(1, validationCount[0], "Validation should only run once in singleton mode");
	}

	@Test
	void q08_validateNotCalledWithoutSingleton() {
		var validationCount = new int[]{0};

		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.validate(b -> {
				validationCount[0]++;
				return b.isValid();
			});

		creator.create();
		creator.create();
		creator.create();

		assertEquals(3, validationCount[0], "Validation should run for each create()");
	}

	@Test
	void q09_validateNotCalledOnFallback() {
		var validationCalled = new boolean[]{false};
		var fallback = new ConcreteBeanInterface();

		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.orElse(fallback)
			.validate(b -> {
				validationCalled[0] = true;
				return true;
			})
			.create();

		assertSame(fallback, bean);
		assertFalse(validationCalled[0], "Validation should not be called on fallback");
	}

	@Test
	void q10_validateNotCalledOnImplementation() {
		var validationCalled = new boolean[]{false};
		var impl = new ValidatableBean();

		var bean = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.implementation(impl)
			.validate(b -> {
				validationCalled[0] = true;
				return true;
			})
			.create();

		assertSame(impl, bean);
		assertFalse(validationCalled[0], "Validation should not be called on implementation");
	}

	@Test
	void q11_validateExceptionPropagates() {
		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.validate(b -> {
				throw new IllegalStateException("Validator error");
			});

		var ex = assertThrows(IllegalStateException.class, () -> creator.create());
		assertEquals("Validator error", ex.getMessage());
	}

	@Test
	void q12_lastValidatorWins() {
		var firstCalled = new boolean[]{false};
		var secondCalled = new boolean[]{false};

		var bean = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.validate(b -> {
				firstCalled[0] = true;
				return true;
			})
			.validate(b -> {
				secondCalled[0] = true;
				return true;
			})
			.create();

		assertNotNull(bean);
		assertFalse(firstCalled[0], "First validator should be replaced");
		assertTrue(secondCalled[0], "Second validator should be called");
	}

	@Test
	void q13_validateWithAsOptional() {
		var result = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.run(b -> b.setValid(false))
			.validate(b -> b.isValid())
			.asOptional();

		assertFalse(result.isPresent(), "tryCreate should return empty on validation failure");
	}

	@Test
	void q14_validateWithBuilder() {
		var result = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore)
			.validate(b -> b != null)
			.asOptional();

		assertTrue(result.isPresent());
	}

	@Test
	void q15_validateComplexBusinessRule() {
		// Simulate a complex validation scenario
		var service = new TestService("premium");
		beanStore.add(TestService.class, service);

		var bean = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.run(b -> {
				b.setName("test");
				b.setValue(100);
			})
			.validate(b -> {
				// Complex business rule
				return b.getName() != null &&
				       !b.getName().isEmpty() &&
				       b.getValue() >= 0 &&
				       b.getValue() <= 1000 &&
				       b.isValid();
			})
			.create();

		assertNotNull(bean);
	}

	@Test
	void q16_validateFailsWithDetailedMessage() {
		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.validate(b -> false);

		var ex = assertThrows(ExecutableException.class, () -> creator.create());
		assertTrue(ex.getMessage().contains("ValidatableBean"),
			"Error message should include bean type name");
	}

	@Test
	void q17_validateWithDependencies() {
		var service = new TestService("test");
		var another = new AnotherService(42);
		beanStore.add(TestService.class, service);
		beanStore.add(AnotherService.class, another);

		var bean = BeanCreator2.of(BeanWithDependencies.class)
			.beanStore(beanStore)
			.validate(b -> b.service != null && b.another != null)
			.create();

		assertNotNull(bean);
		assertNotNull(bean.service);
		assertNotNull(bean.another);
	}

	@Test
	void q18_validateBeforeCaching() {
		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.singleton()
			.run(b -> b.setValid(false))
			.validate(b -> b.isValid());

		// First call fails validation
		assertThrows(ExecutableException.class, () -> creator.create());

		// Should not have cached the invalid bean
		// Second call should also fail (not return a cached invalid bean)
		assertThrows(ExecutableException.class, () -> creator.create());
	}

	@Test
	void q19_validateNullCheck() {
		// Ensure null validator is handled gracefully
		var bean = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.create();

		assertNotNull(bean);  // Should succeed without validator
	}

	@Test
	void q20_validateWithCustomPredicate() {
		Predicate<ValidatableBean> customValidator = b -> {
			if (b.getName() == null) return false;
			if (b.getValue() < 0) return false;
			if (!b.isValid()) return false;
			return true;
		};

		var bean = BeanCreator2.of(ValidatableBean.class)
			.beanStore(beanStore)
			.run(b -> {
				b.setName("test");
				b.setValue(10);
			})
			.validate(customValidator)
			.create();

		assertNotNull(bean);
	}

	//====================================================================================================
	// Tests - Debug/Diagnostics mode
	//====================================================================================================

	@Test
	void r01_debugModeCreatesLog() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.debug();

		var bean = creator.create();

		assertNotNull(bean);
		var log = creator.getDebugLog();
		assertFalse(log.isEmpty(), "Log should not be empty in debug mode");
		assertTrue(log.get(0).contains("Starting bean creation"), "First log entry should indicate start");
	}

	@Test
	void r02_withoutDebugModeNoLog() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore);

		var bean = creator.create();

		assertNotNull(bean);
		var log = creator.getDebugLog();
		assertTrue(log.isEmpty(), "Log should be empty without debug mode");
	}

	@Test
	void r03_logIncludesBeanType() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.debug();

		creator.create();

		var log = creator.getDebugLog();
		assertTrue(log.stream().anyMatch(s -> s.contains("SimpleBean")),
			"Log should mention bean type");
	}

	@Test
	void r04_logResetOnEachCreate() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.debug();

		creator.create();

		creator.create();
		var secondLog = creator.getDebugLog();
		var secondLogSize = secondLog.size();

		// The log should be reset on each create() call
		// Note: The first create may have more entries due to builder type determination logging
		// which is cached for subsequent creates. We just verify the log was reset (has entries).
		assertTrue(secondLogSize > 0, "Log should contain entries after second create");
		assertTrue(secondLog.get(0).contains("Starting bean creation"),
			"Log should start fresh on each create");
	}

	@Test
	void r05_singletonLogOnlyFirstCreation() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.singleton()
			.debug();

		creator.create();
		var firstLogSize = creator.getDebugLog().size();
		assertTrue(firstLogSize > 0);

		creator.create();
		var secondLogSize = creator.getDebugLog().size();

		// Second call returns cached, log should indicate this
		assertTrue(secondLogSize > 0);
		assertTrue(creator.getDebugLog().stream().anyMatch(s -> s.contains("cached")),
			"Log should indicate cached singleton was returned");
	}

	@Test
	void r06_logUnmodifiable() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.debug();

		creator.create();
		var log = creator.getDebugLog();

		assertThrows(UnsupportedOperationException.class, () -> log.add("test"),
			"Creation log should be unmodifiable");
	}

	@Test
	void r07_debugWithBuilder() {
		var creator = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(beanStore)
			.debug();

		creator.create();

		var log = creator.getDebugLog();
		assertTrue(log.stream().anyMatch(s -> s.contains("Builder")),
			"Log should mention builder");
	}

	@Test
	void r08_debugWithImplementation() {
		var impl = new SimpleBean();
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.implementation(impl)
			.debug();

		creator.create();

		var log = creator.getDebugLog();
		assertTrue(log.stream().anyMatch(s -> s.contains("impl()")),
			"Log should mention implementation() usage");
	}

	@Test
	void r09_debugWithFailure() {
		var creator = BeanCreator2.of(BeanInterface.class)
			.beanStore(beanStore)
			.debug();

		try {
			creator.create();
			fail("Should have thrown exception");
		} catch (ExecutableException e) {
			// Expected
		}

		var log = creator.getDebugLog();
		assertFalse(log.isEmpty(), "Log should be populated even on failure");
	}

	@Test
	void r10_debugWithAsOptional() {
		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(beanStore)
			.debug();

		var result = creator			.asOptional();

		assertTrue(result.isPresent());
		var log = creator.getDebugLog();
		assertFalse(log.isEmpty(), "Log should be populated with tryCreate()");
	}

	@Test
	void r11_debugWithBeanSubType() {
		// This tests line 956: debug log when beanSubType differs from beanType
		@SuppressWarnings("rawtypes")
		var creator = BeanCreator2.of(ParentBeanWithBuilder.class)
			.beanStore(beanStore)
			.beanSubType((Class)ChildBeanWithBuilder.class)
			.debug();

		creator.create();

		var log = creator.getDebugLog();
		assertContains("Subtype specified", log.toString());
	}

	@Test
	void r12_beanSubTypeNullThrows() {
		// Verify that passing null to beanSubType() throws IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> {
			BeanCreator2.of(SimpleBean.class).beanSubType(null);
		});
	}

	//====================================================================================================
	// Tests - addToStore() functionality
	//====================================================================================================

	@Test
	void s01_addToStoreBasic() {
		var writableStore = new BasicBeanStore2(null);

		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.addToStore()
			.debug();

		var bean = creator.create();

		assertNotNull(bean);

		// Print debug log
		System.out.println("Creation log:");
		creator.getDebugLog().forEach(System.out::println);

		System.out.println("Bean created: " + bean + " @ " + System.identityHashCode(bean));

		var retrieved = writableStore.getBean(SimpleBean.class);
		System.out.println("Retrieved: " + retrieved);
		if (retrieved.isPresent()) {
			System.out.println("Retrieved bean: " + retrieved.get() + " @ " + System.identityHashCode(retrieved.get()));
		}

		assertTrue(retrieved.isPresent(), "Bean should be in store");
		assertSame(bean, retrieved.get());
	}

	@Test
	void s02_addToStoreWithName() {
		var writableStore = new BasicBeanStore2(null);

		var bean = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.addToStore("myBean")
			.create();

		assertNotNull(bean);
		var retrieved = writableStore.getBean(SimpleBean.class, "myBean");
		assertTrue(retrieved.isPresent());
		assertSame(bean, retrieved.get());
	}

	@Test
	void s03_addToStoreMultipleWithDifferentNames() {
		var writableStore = new BasicBeanStore2(null);

		var bean1 = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.run(b -> b.value = "bean1")
			.addToStore("first")
			.create();

		var bean2 = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.run(b -> b.value = "bean2")
			.addToStore("second")
			.create();

		assertNotNull(bean1);
		assertNotNull(bean2);

		var retrieved1 = writableStore.getBean(SimpleBean.class, "first");
		var retrieved2 = writableStore.getBean(SimpleBean.class, "second");

		assertTrue(retrieved1.isPresent());
		assertTrue(retrieved2.isPresent());
		assertSame(bean1, retrieved1.get());
		assertSame(bean2, retrieved2.get());
		assertEquals("bean1", retrieved1.get().value);
		assertEquals("bean2", retrieved2.get().value);
	}

	@Test
	void s04_addToStoreWithSingleton() {
		var writableStore = new BasicBeanStore2(null);

		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.singleton()
			.addToStore();

		var bean1 = creator.create();
		var bean2 = creator.create();

		assertSame(bean1, bean2);

		var retrieved = writableStore.getBean(SimpleBean.class);
		assertTrue(retrieved.isPresent());
		assertSame(bean1, retrieved.get());
	}

	// Simple read-only BeanStore implementation (not WritableBeanStore)
	private static class ReadOnlyBeanStore implements BeanStore {
		@Override
		public <T> Optional<T> getBean(Class<T> type) {
			return Optional.empty();
		}

		@Override
		public <T> Optional<T> getBean(Class<T> type, String name) {
			return Optional.empty();
		}

		@Override
		public <T> Map<String, T> getBeansOfType(Class<T> type) {
			return Map.of();
		}

		@Override
		public boolean hasBean(Class<?> beanType) {
			return false;
		}

		@Override
		public boolean hasBean(Class<?> beanType, String name) {
			return false;
		}

		@Override
		public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType) {
			return Optional.empty();
		}

		@Override
		public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) {
			return Optional.empty();
		}
	}

	@Test
	void s05_addToStoreWithReadOnlyStore() {
		// Regular BeanStore (read-only) should not cause error
		// This tests line 1405: Log when parent store is not WritableBeanStore
		var readOnlyStore = new ReadOnlyBeanStore();

		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(readOnlyStore)  // readOnlyStore is not WritableBeanStore
			.addToStore()
			.debug();

		var bean = creator.create();

		assertNotNull(bean);
		// Bean should still be created, just not added to store

		// Verify the debug log shows the warning
		var log = creator.getDebugLog();
		assertContains("Parent store is not WritableBeanStore", log.toString());
	}

	@Test
	void s06_addToStoreWithImplementation() {
		var writableStore = new BasicBeanStore2(null);
		var impl = new SimpleBean();

		var bean = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.implementation(impl)
			.addToStore()
			.create();

		assertSame(impl, bean);
		var retrieved = writableStore.getBean(SimpleBean.class);
		assertTrue(retrieved.isPresent());
		assertSame(impl, retrieved.get());
	}

	@Test
	void s07_addToStoreWithFallback() {
		var writableStore = new BasicBeanStore2(null);
		var fallback = new ConcreteBeanInterface();

		var bean = BeanCreator2.of(BeanInterface.class)
			.beanStore(writableStore)
			.orElse(fallback)
			.addToStore()
			.create();

		assertSame(fallback, bean);
		var retrieved = writableStore.getBean(BeanInterface.class);
		assertTrue(retrieved.isPresent());
		assertSame(fallback, retrieved.get());
	}

	@Test
	void s08_addToStoreWithBuilder() {
		var writableStore = new BasicBeanStore2(null);

		var bean = BeanCreator2.of(BeanWithBuilder.class)
			.beanStore(writableStore)
			.addToStore()
			.create();

		assertNotNull(bean);
		var retrieved = writableStore.getBean(BeanWithBuilder.class);
		assertTrue(retrieved.isPresent());
		assertSame(bean, retrieved.get());
	}

	@Test
	void s09_addToStoreWithDependencies() {
		var writableStore = new BasicBeanStore2(null);
		var service = new TestService("test");
		var another = new AnotherService(42);
		writableStore.addBean(TestService.class, service);
		writableStore.addBean(AnotherService.class, another);

		var bean = BeanCreator2.of(BeanWithDependencies.class)
			.beanStore(writableStore)
			.addToStore()
			.create();

		assertNotNull(bean);
		assertNotNull(bean.service);

		var retrieved = writableStore.getBean(BeanWithDependencies.class);
		assertTrue(retrieved.isPresent());
		assertSame(bean, retrieved.get());
	}

	@Test
	void s10_addToStoreReplacesExisting() {
		var writableStore = new BasicBeanStore2(null);
		var oldBean = new SimpleBean();
		writableStore.addBean(SimpleBean.class, oldBean);

		var newBean = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.addToStore()
			.create();

		assertNotSame(oldBean, newBean);

		var retrieved = writableStore.getBean(SimpleBean.class);
		assertTrue(retrieved.isPresent());
		assertSame(newBean, retrieved.get(), "New bean should replace old bean");
	}

	@Test
	void s11_addToStoreWithValidation() {
		var writableStore = new BasicBeanStore2(null);

		var bean = BeanCreator2.of(ValidatableBean.class)
			.beanStore(writableStore)
			.run(b -> b.setValid(true))
			.validate(b -> b.isValid())
			.addToStore()
			.create();

		assertNotNull(bean);
		var retrieved = writableStore.getBean(ValidatableBean.class);
		assertTrue(retrieved.isPresent());
		assertSame(bean, retrieved.get());
	}

	@Test
	void s12_addToStoreFailureDoesNotAdd() {
		var writableStore = new BasicBeanStore2(null);

		var creator = BeanCreator2.of(ValidatableBean.class)
			.beanStore(writableStore)
			.run(b -> b.setValid(false))
			.validate(b -> b.isValid())
			.addToStore();

		assertThrows(ExecutableException.class, () -> creator.create());

		// Bean should not be in store due to validation failure
		var retrieved = writableStore.getBean(ValidatableBean.class);
		assertFalse(retrieved.isPresent(), "Failed bean should not be added to store");
	}

	@Test
	void s13_addToStoreWithAsOptional() {
		var writableStore = new BasicBeanStore2(null);

		var result = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.addToStore()
			.asOptional();

		assertTrue(result.isPresent());
		var retrieved = writableStore.getBean(SimpleBean.class);
		assertTrue(retrieved.isPresent());
		assertSame(result.get(), retrieved.get());
	}

	@Test
	void s14_addToStoreWithDebug() {
		var writableStore = new BasicBeanStore2(null);

		var creator = BeanCreator2.of(SimpleBean.class)
			.beanStore(writableStore)
			.addToStore("test")
			.debug();

		creator.create();

		var log = creator.getDebugLog();
		assertTrue(log.stream().anyMatch(s -> s.contains("Adding bean to store")),
			"Debug log should mention adding to store");
	}

	@Test
	void s15_addToStoreNullNameThrows() {
		assertThrows(IllegalArgumentException.class, () -> {
			BeanCreator2.of(SimpleBean.class)
				.beanStore(beanStore)
				.addToStore(null);
		});
	}

	//====================================================================================================
	// Tests - Edge cases
	//====================================================================================================

	@Test
	void j01_nullBeanStoreWorks() {
		var bean = BeanCreator2.of(SimpleBean.class)
			.beanStore(null)
			.create();

		assertNotNull(bean);
	}

	@Test
	void j02_noBeanStoreWorks() {
		var bean = BeanCreator2.of(SimpleBean.class)
			.create();

		assertNotNull(bean);
	}
}

