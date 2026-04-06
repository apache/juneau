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
package org.apache.juneau.commons.function;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link BeanFactory}, {@link BeanConsumer}, {@link BeanSupplier},
 * {@link BeanChannel}, and {@link ListBeanChannel}.
 */
class BeanChannel_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// BeanFactory tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_beanFactory_create() throws Exception {
		BeanFactory<String> a = () -> "hello";
		assertEquals("hello", a.create());
	}

	@Test @SuppressWarnings("rawtypes") void a02_beanFactory_void_throws() throws Exception {
		var ctor = BeanFactory.Void.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		var instance = (BeanFactory) ctor.newInstance();
		assertThrows(UnsupportedOperationException.class, instance::create);
	}

	//------------------------------------------------------------------------------------------------------------------
	// BeanConsumer tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_beanConsumer_defaultLifecycle() throws Exception {
		var received = new ArrayList<String>();
		BeanConsumer<String> a = item -> received.add(item);
		a.begin();
		a.acceptThrows("x");
		a.acceptThrows("y");
		a.complete();
		assertEquals(List.of("x", "y"), received);
	}

	@Test void b02_beanConsumer_onError_rethrows_by_default() {
		BeanConsumer<String> a = item -> {};
		assertThrows(RuntimeException.class, () -> a.onError(new RuntimeException("oops")));
	}

	@Test void b03_beanConsumer_accept_wraps_checked_exception() {
		BeanConsumer<String> a = item -> { throw new Exception("checked"); };
		assertThrows(RuntimeException.class, () -> a.accept("x"));
	}

	@Test void b04_beanConsumer_onError_absorb_allows_continue() throws Exception {
		var skipped = new ArrayList<String>();
		BeanConsumer<String> a = new BeanConsumer<>() {
			@Override public void acceptThrows(String item) throws Exception {
				if ("bad".equals(item)) throw new Exception("bad item");
			}
			@Override public void onError(Exception e) {
				skipped.add(e.getMessage());
			}
		};
		a.begin();
		a.acceptThrows("good");
		try { a.acceptThrows("bad"); } catch (Exception e) { a.onError(e); }
		a.acceptThrows("good2");
		a.complete();
		assertEquals(List.of("bad item"), skipped);
	}

	//------------------------------------------------------------------------------------------------------------------
	// BeanSupplier tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_beanSupplier_defaultLifecycle() throws Exception {
		var data = List.of("a", "b", "c");
		BeanSupplier<String> a = () -> data.iterator();
		a.begin();
		var result = new ArrayList<String>();
		a.iterator().forEachRemaining(result::add);
		a.complete();
		assertEquals(data, result);
	}

	@Test void c02_beanSupplier_onError_rethrows_by_default() {
		BeanSupplier<String> a = () -> Collections.<String>emptyList().iterator();
		assertThrows(RuntimeException.class, () -> a.onError(new RuntimeException("oops")));
	}

	@Test void c03_beanSupplier_isIterable() {
		BeanSupplier<String> a = () -> List.of("x").iterator();
		assertTrue(a instanceof Iterable);
	}

	//------------------------------------------------------------------------------------------------------------------
	// BeanChannel tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_beanChannel_extendsConsumerAndSupplier() {
		ListBeanChannel<String> a = new ListBeanChannel<>();
		assertTrue(a instanceof BeanConsumer);
		assertTrue(a instanceof BeanSupplier);
		assertTrue(a instanceof BeanChannel);
		assertTrue(a instanceof Iterable);
	}

	@Test void d02_beanChannel_defaultLifecycleMethods() throws Exception {
		BeanChannel<String> a = new BeanChannel<>() {
			@Override public Iterator<String> iterator() { return Collections.emptyIterator(); }
			@Override public void acceptThrows(String item) {}
		};
		// begin and complete are no-ops by default
		a.begin();
		a.complete();
		// onError rethrows by default
		var x = new Exception("x");
		var thrown = assertThrows(Exception.class, () -> a.onError(x));
		assertSame(x, thrown);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ListBeanChannel tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_listBeanChannel_collectAndIterate() throws Exception {
		var a = new ListBeanChannel<String>();

		a.acceptThrows("one");
		a.acceptThrows("two");
		a.acceptThrows("three");

		assertEquals(List.of("one", "two", "three"), a.getList());

		var result = new ArrayList<String>();
		a.iterator().forEachRemaining(result::add);
		assertEquals(List.of("one", "two", "three"), result);
	}

	@Test void e02_listBeanChannel_emptyChannel() throws Exception {
		var a = new ListBeanChannel<String>();
		assertTrue(a.getList().isEmpty());
		assertFalse(a.iterator().hasNext());
	}

	@Test void e03_listBeanChannel_defaultLifecycle() throws Exception {
		var a = new ListBeanChannel<String>();
		a.begin();
		a.acceptThrows("a");
		a.complete();
		assertEquals(List.of("a"), a.getList());
	}

	@Test void e04_listBeanChannel_onError_rethrows_by_default() {
		var a = new ListBeanChannel<String>();
		assertThrows(Exception.class, () -> a.onError(new Exception("oops")));
	}
}
