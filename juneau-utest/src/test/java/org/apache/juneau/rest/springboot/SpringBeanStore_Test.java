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
package org.apache.juneau.rest.springboot;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.*;
import org.springframework.context.*;

/**
 * Tests for SpringBeanStore fluent setter overrides.
 */
@SuppressWarnings({
	"unchecked" // Mockito generic type warnings
})
class SpringBeanStore_Test extends TestBase {

	static class TestService {
		private final String name;
		TestService(String name) { this.name = name; }
		@Override public String toString() { return "TestService[" + name + "]"; }
	}

	@Test
	void a01_fluentChaining_clear() {
		var store = new SpringBeanStore(opte(), opte(), null);

		SpringBeanStore result = store.clear();

		assertSame(store, result);
		assertInstanceOf(SpringBeanStore.class, result);
	}

	@Test
	void b01_getBean_noAppContext_notFound() {
		var store = new SpringBeanStore(opte(), opte(), null);

		var result = store.getBean(TestService.class);

		assertFalse(result.isPresent());
	}

	@Test
	void b02_getBean_fromLocalStore() {
		var store = new SpringBeanStore(opte(), opte(), null);
		var service = new TestService("local");
		store.addBean(TestService.class, service);

		var result = store.getBean(TestService.class);

		assertTrue(result.isPresent());
		assertSame(service, result.get());
	}

	@Test
	void b03_getBean_fromSpringContext() {
		var mockAppContext = mock(ApplicationContext.class);
		var service = new TestService("spring");
		var provider = mock(ObjectProvider.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenReturn(provider);
		when(provider.getIfAvailable()).thenReturn(service);

		var store = new SpringBeanStore(opt(mockAppContext), opte(), null);
		var result = store.getBean(TestService.class);

		assertTrue(result.isPresent());
		assertSame(service, result.get());
	}

	@Test
	void b04_getBean_springReturnsNull() {
		var mockAppContext = mock(ApplicationContext.class);
		var provider = mock(ObjectProvider.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenReturn(provider);
		when(provider.getIfAvailable()).thenReturn(null);

		var store = new SpringBeanStore(opt(mockAppContext), opte(), null);
		var result = store.getBean(TestService.class);

		assertFalse(result.isPresent());
	}

	@Test
	void b05_getBean_springThrowsException() {
		var mockAppContext = mock(ApplicationContext.class);
		when(mockAppContext.getBeanProvider(TestService.class)).thenThrow(new RuntimeException("Error"));

		var store = new SpringBeanStore(opt(mockAppContext), opte(), null);
		var result = store.getBean(TestService.class);

		assertFalse(result.isPresent());
	}

	@Test
	void c01_getBeanNamed_noAppContext_notFound() {
		var store = new SpringBeanStore(opte(), opte(), null);

		var result = store.getBean(TestService.class, "myBean");

		assertFalse(result.isPresent());
	}

	@Test
	void c02_getBeanNamed_fromLocalStore() {
		var store = new SpringBeanStore(opte(), opte(), null);
		var service = new TestService("local");
		store.addBean(TestService.class, service, "myBean");

		var result = store.getBean(TestService.class, "myBean");

		assertTrue(result.isPresent());
		assertSame(service, result.get());
	}

	@Test
	void c03_getBeanNamed_withNameFromSpring() {
		var mockAppContext = mock(ApplicationContext.class);
		var service = new TestService("spring");
		when(mockAppContext.containsBean("myBean")).thenReturn(true);
		when(mockAppContext.getBean("myBean", TestService.class)).thenReturn(service);

		var store = new SpringBeanStore(opt(mockAppContext), opte(), null);
		var result = store.getBean(TestService.class, "myBean");

		assertTrue(result.isPresent());
		assertSame(service, result.get());
	}

	@Test
	void c04_getBeanNamed_nameNotInSpring() {
		var mockAppContext = mock(ApplicationContext.class);
		when(mockAppContext.containsBean("myBean")).thenReturn(false);
		when(mockAppContext.getBean(TestService.class)).thenReturn(null);

		var store = new SpringBeanStore(opt(mockAppContext), opte(), null);
		var result = store.getBean(TestService.class, "myBean");

		assertFalse(result.isPresent());
	}

	@Test
	void c05_getBeanNamed_nullName_fromSpring() {
		var mockAppContext = mock(ApplicationContext.class);
		var service = new TestService("spring");
		when(mockAppContext.getBean(TestService.class)).thenReturn(service);

		var store = new SpringBeanStore(opt(mockAppContext), opte(), null);
		var result = store.getBean(TestService.class, null);

		assertTrue(result.isPresent());
		assertSame(service, result.get());
	}

	@Test
	void c06_getBeanNamed_springThrowsException() {
		var mockAppContext = mock(ApplicationContext.class);
		when(mockAppContext.containsBean("myBean")).thenThrow(new RuntimeException("Error"));

		var store = new SpringBeanStore(opt(mockAppContext), opte(), null);
		var result = store.getBean(TestService.class, "myBean");

		assertFalse(result.isPresent());
	}
}