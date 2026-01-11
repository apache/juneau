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
package org.apache.juneau.rest.springboot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.*;
import org.springframework.context.*;

/**
 * Tests for {@link SpringBeanStore2}.
 */
class SpringBeanStore2_Test extends TestBase {

	private ApplicationContext mockAppContext;
	private SpringBeanStore2 beanStore;

	@BeforeEach
	void setUp() {
		mockAppContext = mock(ApplicationContext.class);
		beanStore = new SpringBeanStore2(mockAppContext, null);
	}

	//====================================================================================================
	// Test classes
	//====================================================================================================

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

	interface ServiceInterface {}

	static class ServiceImpl implements ServiceInterface {
		private final String id;
		ServiceImpl(String id) { this.id = id; }
		String getId() { return id; }
	}

	//====================================================================================================
	// Tests - Constructor
	//====================================================================================================

	@Test
	void a01_constructorWithNullContext() {
		var store = new SpringBeanStore2(null, null);
		assertNotNull(store);
	}

	@Test
	void a02_constructorWithNullParent() {
		var store = new SpringBeanStore2(mockAppContext, null);
		assertNotNull(store);
	}

	@Test
	void a03_constructorWithParent() {
		var parent = new BasicBeanStore2(null);
		var store = new SpringBeanStore2(mockAppContext, parent);
		assertNotNull(store);
	}

	//====================================================================================================
	// Tests - getBean(Class) - Local store
	//====================================================================================================

	@Test
	void b01_getBean_fromLocalStore() {
		var service = new TestService("local");
		beanStore.addBean(TestService.class, service);

		var result = beanStore.getBean(TestService.class);

		assertTrue(result.isPresent());
		assertSame(service, result.get());
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void b02_getBean_fromParentStore() {
		var parent = new BasicBeanStore2(null);
		var service = new TestService("parent");
		parent.addBean(TestService.class, service);

		var store = new SpringBeanStore2(mockAppContext, parent);
		var result = store.getBean(TestService.class);

		assertTrue(result.isPresent());
		assertSame(service, result.get());
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void b03_getBean_localOverridesParent() {
		var parent = new BasicBeanStore2(null);
		var parentService = new TestService("parent");
		parent.addBean(TestService.class, parentService);

		var localService = new TestService("local");
		var store = new SpringBeanStore2(mockAppContext, parent);
		store.addBean(TestService.class, localService);

		var result = store.getBean(TestService.class);

		assertTrue(result.isPresent());
		assertSame(localService, result.get());
		verifyNoInteractions(mockAppContext);
	}

	//====================================================================================================
	// Tests - getBean(Class) - Spring context
	//====================================================================================================

	@Test
	void c01_getBean_fromSpringContext() {
		var service = new TestService("spring");
		var provider = mock(ObjectProvider.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenReturn(provider);
		when(provider.getIfAvailable()).thenReturn(service);

		var result = beanStore.getBean(TestService.class);

		assertTrue(result.isPresent());
		assertSame(service, result.get());
		verify(mockAppContext).getBeanProvider(TestService.class);
	}

	@Test
	void c02_getBean_notFoundInSpring() {
		var provider = mock(ObjectProvider.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenReturn(provider);
		when(provider.getIfAvailable()).thenReturn(null);

		var result = beanStore.getBean(TestService.class);

		assertFalse(result.isPresent());
	}

	@Test
	void c03_getBean_springThrowsException() {
		when(mockAppContext.getBeanProvider(TestService.class)).thenThrow(new RuntimeException("Bean not found"));

		var result = beanStore.getBean(TestService.class);

		assertFalse(result.isPresent());
	}

	@Test
	void c04_getBean_nullAppContext() {
		var store = new SpringBeanStore2(null, null);

		var result = store.getBean(TestService.class);

		assertFalse(result.isPresent());
	}

	@Test
	void c05_getBean_localOverridesSpring() {
		var localService = new TestService("local");
		beanStore.addBean(TestService.class, localService);

		var springService = new TestService("spring");
		var provider = mock(ObjectProvider.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenReturn(provider);
		when(provider.getIfAvailable()).thenReturn(springService);

		var result = beanStore.getBean(TestService.class);

		assertTrue(result.isPresent());
		assertSame(localService, result.get());
		verifyNoInteractions(mockAppContext);
	}

	//====================================================================================================
	// Tests - getBean(Class, String) - Local store
	//====================================================================================================

	@Test
	void d01_getBeanNamed_fromLocalStore() {
		var service = new TestService("named");
		beanStore.addBean(TestService.class, service, "myBean");

		var result = beanStore.getBean(TestService.class, "myBean");

		assertTrue(result.isPresent());
		assertSame(service, result.get());
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void d02_getBeanNamed_fromParentStore() {
		var parent = new BasicBeanStore2(null);
		var service = new TestService("parent");
		parent.addBean(TestService.class, service, "myBean");

		var store = new SpringBeanStore2(mockAppContext, parent);
		var result = store.getBean(TestService.class, "myBean");

		assertTrue(result.isPresent());
		assertSame(service, result.get());
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void d03_getBeanNamed_nullName() {
		var service = new TestService("unnamed");
		beanStore.addBean(TestService.class, service);

		var result = beanStore.getBean(TestService.class, null);

		assertTrue(result.isPresent());
		assertSame(service, result.get());
	}

	//====================================================================================================
	// Tests - getBean(Class, String) - Spring context
	//====================================================================================================

	@Test
	void e01_getBeanNamed_fromSpringContext() {
		var service = new TestService("spring");
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.getBean("myBean", TestService.class)).thenReturn(service);

		var result = beanStore.getBean(TestService.class, "myBean");

		assertTrue(result.isPresent());
		assertSame(service, result.get());
		verify(mockAppContext).containsBean("myBean");
		verify(mockAppContext).getBean("myBean", TestService.class);
	}

	@Test
	void e02_getBeanNamed_notFoundInSpring() {
		when(mockAppContext.containsBean("myBean")).thenReturn(false);

		var result = beanStore.getBean(TestService.class, "myBean");

		assertFalse(result.isPresent());
		verify(mockAppContext).containsBean("myBean");
		verify(mockAppContext, never()).getBean(anyString(), any(Class.class));
	}

	@Test
	void e03_getBeanNamed_springThrowsException() {
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.getBean("myBean", TestService.class)).thenThrow(new RuntimeException("Bean error"));

		var result = beanStore.getBean(TestService.class, "myBean");

		assertFalse(result.isPresent());
	}

	@Test
	void e04_getBeanNamed_nullNameSkipsSpring() {
		var store = new SpringBeanStore2(mockAppContext, null);

		var result = store.getBean(TestService.class, null);

		assertFalse(result.isPresent());
		verifyNoInteractions(mockAppContext);
	}

	//====================================================================================================
	// Tests - getBeansOfType(Class)
	//====================================================================================================

	@Test
	void f01_getBeansOfType_fromLocalStore() {
		var service1 = new TestService("service1");
		var service2 = new TestService("service2");
		beanStore.addBean(TestService.class, service1, "bean1");
		beanStore.addBean(TestService.class, service2, "bean2");

		var result = beanStore.getBeansOfType(TestService.class);

		assertEquals(2, result.size());
		assertSame(service1, result.get("bean1"));
		assertSame(service2, result.get("bean2"));
	}

	@Test
	void f02_getBeansOfType_fromSpringContext() {
		var springService1 = new TestService("spring1");
		var springService2 = new TestService("spring2");
		var springBeans = Map.of("bean1", springService1, "bean2", springService2);
		when(mockAppContext.getBeansOfType(TestService.class)).thenReturn(springBeans);

		var result = beanStore.getBeansOfType(TestService.class);

		assertEquals(2, result.size());
		assertSame(springService1, result.get("bean1"));
		assertSame(springService2, result.get("bean2"));
	}

	@Test
	void f03_getBeansOfType_localOverridesSpring() {
		var localService = new TestService("local");
		beanStore.addBean(TestService.class, localService, "bean1");

		var springService1 = new TestService("spring1");
		var springService2 = new TestService("spring2");
		var springBeans = Map.of("bean1", springService1, "bean2", springService2);
		when(mockAppContext.getBeansOfType(TestService.class)).thenReturn(springBeans);

		var result = beanStore.getBeansOfType(TestService.class);

		assertEquals(2, result.size());
		assertSame(localService, result.get("bean1")); // Local overrides Spring
		assertSame(springService2, result.get("bean2"));
	}

	@Test
	void f04_getBeansOfType_mergesLocalAndSpring() {
		var localService = new TestService("local");
		beanStore.addBean(TestService.class, localService, "local");

		var springService = new TestService("spring");
		var springBeans = Map.of("spring", springService);
		when(mockAppContext.getBeansOfType(TestService.class)).thenReturn(springBeans);

		var result = beanStore.getBeansOfType(TestService.class);

		assertEquals(2, result.size());
		assertSame(localService, result.get("local"));
		assertSame(springService, result.get("spring"));
	}

	@Test
	void f05_getBeansOfType_includesParent() {
		var parent = new BasicBeanStore2(null);
		var parentService = new TestService("parent");
		parent.addBean(TestService.class, parentService, "parent");

		var store = new SpringBeanStore2(mockAppContext, parent);
		var localService = new TestService("local");
		store.addBean(TestService.class, localService, "local");

		var result = store.getBeansOfType(TestService.class);

		assertEquals(2, result.size());
		assertSame(parentService, result.get("parent"));
		assertSame(localService, result.get("local"));
	}

	@Test
	void f06_getBeansOfType_emptyWhenNotFound() {
		when(mockAppContext.getBeansOfType(TestService.class)).thenReturn(Map.of());

		var result = beanStore.getBeansOfType(TestService.class);

		assertTrue(result.isEmpty());
	}

	@Test
	void f07_getBeansOfType_springThrowsException() {
		when(mockAppContext.getBeansOfType(TestService.class)).thenThrow(new RuntimeException("Error"));

		var result = beanStore.getBeansOfType(TestService.class);

		assertTrue(result.isEmpty());
	}

	//====================================================================================================
	// Tests - hasBean(Class)
	//====================================================================================================

	@Test
	void g01_hasBean_inLocalStore() {
		beanStore.addBean(TestService.class, new TestService("local"));

		assertTrue(beanStore.hasBean(TestService.class));
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void g02_hasBean_inParentStore() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestService.class, new TestService("parent"));

		var store = new SpringBeanStore2(mockAppContext, parent);

		assertTrue(store.hasBean(TestService.class));
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void g03_hasBean_inSpringContext() {
		when(mockAppContext.getBeanNamesForType(TestService.class)).thenReturn(new String[]{"bean1"});

		assertTrue(beanStore.hasBean(TestService.class));
	}

	@Test
	void g04_hasBean_notFound() {
		when(mockAppContext.getBeanNamesForType(TestService.class)).thenReturn(new String[]{});

		assertFalse(beanStore.hasBean(TestService.class));
	}

	@Test
	void g05_hasBean_springThrowsException() {
		when(mockAppContext.getBeanNamesForType(TestService.class)).thenThrow(new RuntimeException("Error"));

		assertFalse(beanStore.hasBean(TestService.class));
	}

	//====================================================================================================
	// Tests - hasBean(Class, String)
	//====================================================================================================

	@Test
	void h01_hasBeanNamed_inLocalStore() {
		beanStore.addBean(TestService.class, new TestService("local"), "myBean");

		assertTrue(beanStore.hasBean(TestService.class, "myBean"));
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void h02_hasBeanNamed_inParentStore() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestService.class, new TestService("parent"), "myBean");

		var store = new SpringBeanStore2(mockAppContext, parent);

		assertTrue(store.hasBean(TestService.class, "myBean"));
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void h03_hasBeanNamed_inSpringContext() {
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.isTypeMatch("myBean", TestService.class)).thenReturn(true);

		assertTrue(beanStore.hasBean(TestService.class, "myBean"));
	}

	@Test
	void h04_hasBeanNamed_notFound() {
		when(mockAppContext.containsBean("myBean")).thenReturn(false);

		assertFalse(beanStore.hasBean(TestService.class, "myBean"));
	}

	@Test
	void h05_hasBeanNamed_wrongType() {
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.isTypeMatch("myBean", TestService.class)).thenReturn(false);

		assertFalse(beanStore.hasBean(TestService.class, "myBean"));
	}

	@Test
	void h06_hasBeanNamed_nullName() {
		beanStore.addBean(TestService.class, new TestService("unnamed"));

		assertTrue(beanStore.hasBean(TestService.class, null));
	}

	@Test
	void h07_hasBeanNamed_springThrowsException() {
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.isTypeMatch("myBean", TestService.class)).thenThrow(new RuntimeException("Error"));

		assertFalse(beanStore.hasBean(TestService.class, "myBean"));
	}

	//====================================================================================================
	// Tests - getBeanSupplier(Class)
	//====================================================================================================

	@Test
	void i01_getBeanSupplier_fromLocalStore() {
		var service = new TestService("local");
		beanStore.addBean(TestService.class, service);

		var supplier = beanStore.getBeanSupplier(TestService.class);

		assertTrue(supplier.isPresent());
		assertSame(service, supplier.get().get());
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void i02_getBeanSupplier_fromSpringContext() {
		var service = new TestService("spring");
		var provider = mock(ObjectProvider.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenReturn(provider);
		when(provider.getIfAvailable()).thenReturn(service);

		var supplier = beanStore.getBeanSupplier(TestService.class);

		assertTrue(supplier.isPresent());
		assertSame(service, supplier.get().get());
	}

	@Test
	void i03_getBeanSupplier_notFoundInSpring() {
		var provider = mock(ObjectProvider.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenReturn(provider);
		when(provider.getIfAvailable()).thenReturn(null);

		var supplier = beanStore.getBeanSupplier(TestService.class);

		assertFalse(supplier.isPresent());
	}

	@Test
	void i04_getBeanSupplier_lazy() {
		var service1 = new TestService("spring1");
		var service2 = new TestService("spring2");
		var service3 = new TestService("spring3");
		var provider = mock(ObjectProvider.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenReturn(provider);
		// First call checks existence, second and third are the actual calls
		when(provider.getIfAvailable()).thenReturn(service1, service2, service3);

		var supplier = beanStore.getBeanSupplier(TestService.class);

		assertTrue(supplier.isPresent());
		assertSame(service2, supplier.get().get()); // First actual call (service1 was consumed checking existence)
		assertSame(service3, supplier.get().get()); // Second call
	}

	@Test
	void i05_getBeanSupplier_springThrowsException() {
		when(mockAppContext.getBeanProvider(TestService.class)).thenThrow(new RuntimeException("Error"));

		var supplier = beanStore.getBeanSupplier(TestService.class);

		assertFalse(supplier.isPresent());
	}

	//====================================================================================================
	// Tests - getBeanSupplier(Class, String)
	//====================================================================================================

	@Test
	void j01_getBeanSupplierNamed_fromLocalStore() {
		var service = new TestService("local");
		beanStore.addBean(TestService.class, service, "myBean");

		var supplier = beanStore.getBeanSupplier(TestService.class, "myBean");

		assertTrue(supplier.isPresent());
		assertSame(service, supplier.get().get());
		verifyNoInteractions(mockAppContext);
	}

	@Test
	void j02_getBeanSupplierNamed_fromSpringContext() {
		var service = new TestService("spring");
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.isTypeMatch("myBean", TestService.class)).thenReturn(true);
		when(mockAppContext.getBean("myBean", TestService.class)).thenReturn(service);

		var supplier = beanStore.getBeanSupplier(TestService.class, "myBean");

		assertTrue(supplier.isPresent());
		assertSame(service, supplier.get().get());
	}

	@Test
	void j03_getBeanSupplierNamed_notFoundInSpring() {
		when(mockAppContext.containsBean("myBean")).thenReturn(false);

		var supplier = beanStore.getBeanSupplier(TestService.class, "myBean");

		assertFalse(supplier.isPresent());
	}

	@Test
	void j04_getBeanSupplierNamed_lazy() {
		var service1 = new TestService("spring1");
		var service2 = new TestService("spring2");
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.isTypeMatch("myBean", TestService.class)).thenReturn(true);
		when(mockAppContext.getBean("myBean", TestService.class)).thenReturn(service1, service2);

		var supplier = beanStore.getBeanSupplier(TestService.class, "myBean");

		assertTrue(supplier.isPresent());
		assertSame(service1, supplier.get().get()); // First call
		assertSame(service2, supplier.get().get()); // Second call
	}

	@Test
	void j05_getBeanSupplierNamed_nullName() {
		var service = new TestService("unnamed");
		beanStore.addBean(TestService.class, service);

		var supplier = beanStore.getBeanSupplier(TestService.class, null);

		assertTrue(supplier.isPresent());
		assertSame(service, supplier.get().get());
	}

	@Test
	void j06_getBeanSupplierNamed_springThrowsException() {
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.isTypeMatch("myBean", TestService.class)).thenThrow(new RuntimeException("Error"));

		var supplier = beanStore.getBeanSupplier(TestService.class, "myBean");

		assertFalse(supplier.isPresent());
	}

	//====================================================================================================
	// Tests - clear()
	//====================================================================================================

	@Test
	void k01_clear_removesLocalBeans() {
		beanStore.addBean(TestService.class, new TestService("local"));

		assertTrue(beanStore.hasBean(TestService.class));

		beanStore.clear();

		assertFalse(beanStore.hasBean(TestService.class));
	}

	@Test
	void k02_clear_doesNotAffectParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestService.class, new TestService("parent"));

		var store = new SpringBeanStore2(mockAppContext, parent);
		store.addBean(AnotherService.class, new AnotherService(42));

		store.clear();

		assertTrue(store.hasBean(TestService.class)); // Parent still has it
		assertFalse(store.hasBean(AnotherService.class)); // Local cleared
	}

	@Test
	void k03_clear_returnsThis() {
		var result = beanStore.clear();

		assertSame(beanStore, result);
	}

	//====================================================================================================
	// Tests - Integration scenarios
	//====================================================================================================

	@Test
	void l01_integration_multiLevelHierarchy() {
		// Set up: grandparent -> parent -> child
		var grandparent = new BasicBeanStore2(null);
		var service1 = new TestService("grandparent");
		grandparent.addBean(TestService.class, service1, "bean1");

		var parent = new SpringBeanStore2(null, grandparent);
		var service2 = new TestService("parent");
		parent.addBean(TestService.class, service2, "bean2");

		var child = new SpringBeanStore2(mockAppContext, parent);
		var service3 = new TestService("child");
		child.addBean(TestService.class, service3, "bean3");

		// Test: child can see all three levels
		var result = child.getBeansOfType(TestService.class);

		assertEquals(3, result.size());
		assertSame(service1, result.get("bean1"));
		assertSame(service2, result.get("bean2"));
		assertSame(service3, result.get("bean3"));
	}

	@Test
	void l02_integration_differentTypes() {
		var testService = new TestService("test");
		var anotherService = new AnotherService(42);
		var serviceImpl = new ServiceImpl("impl");

		beanStore.addBean(TestService.class, testService);
		beanStore.addBean(AnotherService.class, anotherService);
		beanStore.addBean(ServiceInterface.class, serviceImpl);

		assertTrue(beanStore.getBean(TestService.class).isPresent());
		assertTrue(beanStore.getBean(AnotherService.class).isPresent());
		assertTrue(beanStore.getBean(ServiceInterface.class).isPresent());
		assertFalse(beanStore.getBean(String.class).isPresent());
	}

	@Test
	void l03_integration_addMethodReturnsBean() {
		var service = new TestService("test");

		var result = beanStore.add(TestService.class, service);

		assertSame(service, result);
		assertTrue(beanStore.hasBean(TestService.class));
	}
}

