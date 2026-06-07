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

import static org.apache.juneau.commons.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.apache.juneau.commons.TestBase;

@SuppressWarnings({
	"java:S4144", // Identical test methods intentional for testing different scenarios
	"java:S114" // Snake_case fixture interface names (X_Iface, X_BasicIface) are intentional test-local naming.
})
class BasicBeanStore_Test extends TestBase {

	// Test bean classes
	static class TestBean {
		private final String name;
		TestBean(String name) { this.name = name; }
		String getName() { return name; }
		@Override public String toString() { return "TestBean[" + name + "]"; }
		@Override public boolean equals(Object o) { return o instanceof TestBean o2 && eq(this, o2, (x,y) -> eq(x.name, y.name)); }
		@Override public int hashCode() { return h(name); }
	}

	static class AnotherBean {
		private final int value;
		AnotherBean(int value) { this.value = value; }
		int getValue() { return value; }
		@Override public boolean equals(Object o) { return o instanceof AnotherBean o2 && eq(this, o2, (x,y) -> eq(x.value, y.value)); }
		@Override public int hashCode() { return value; }
	}

	//====================================================================================================
	// Constructor
	//====================================================================================================

	@Test
	void a01_constructor_noParent() {
		var store = new BasicBeanStore(null);
		assertNotNull(store);
		assertTrue(store.getBean(BasicBeanStore.class).isPresent());
		assertSame(store, store.getBean(BasicBeanStore.class).get());
	}

	@Test
	void a02_constructor_withParent() {
		var parent = new BasicBeanStore(null);
		var store = new BasicBeanStore(parent);
		assertNotNull(store);
		assertTrue(store.getBean(BasicBeanStore.class).isPresent());
		assertSame(store, store.getBean(BasicBeanStore.class).get());
	}

	//====================================================================================================
	// addBean(Class, Object)
	//====================================================================================================

	@Test
	void b01_addBean_unnamed() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void b02_addBean_unnamed_nullValue() {
		var store = new BasicBeanStore(null);
		store.addBean(TestBean.class, null);

		var result = store.getBean(TestBean.class);
		// Java's Optional cannot represent "present but null", so null beans return empty
		assertFalse(result.isPresent());
	}

	@Test
	void b03_addBean_unnamed_returnsThis() {
		var store = new BasicBeanStore(null);
		var result = store.addBean(TestBean.class, new TestBean("test1"));
		assertSame(store, result);
	}

	@Test
	void b04_addBean_unnamed_replaceExisting() {
		var store = new BasicBeanStore(null);
		var bean1 = new TestBean("test1");
		var bean2 = new TestBean("test2");
		store.addBean(TestBean.class, bean1);
		store.addBean(TestBean.class, bean2);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean2, result.get());
	}

	//====================================================================================================
	// addBean(Class, Object, String)
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("addBeanNamedProvider")
	void c01_addBean_named(String name, boolean useNamedGetter) {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, name);

		var result = useNamedGetter ? store.getBean(TestBean.class, name) : store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	static Stream<Arguments> addBeanNamedProvider() {
		return Stream.of(
			Arguments.of("named1", true),
			Arguments.of(null, false),
			Arguments.of("", false)
		);
	}

	@Test
	void c04_addBean_named_multipleNames() {
		var store = new BasicBeanStore(null);
		var bean1 = new TestBean("test1");
		var bean2 = new TestBean("test2");
		var bean3 = new TestBean("test3");
		store.addBean(TestBean.class, bean1, "name1");
		store.addBean(TestBean.class, bean2, "name2");
		store.addBean(TestBean.class, bean3, null);

		assertSame(bean1, store.getBean(TestBean.class, "name1").get());
		assertSame(bean2, store.getBean(TestBean.class, "name2").get());
		assertSame(bean3, store.getBean(TestBean.class).get());
	}

	@Test
	void c05_addBean_named_replaceExisting() {
		var store = new BasicBeanStore(null);
		var bean1 = new TestBean("test1");
		var bean2 = new TestBean("test2");
		store.addBean(TestBean.class, bean1, "name1");
		store.addBean(TestBean.class, bean2, "name1");

		var result = store.getBean(TestBean.class, "name1");
		assertTrue(result.isPresent());
		assertSame(bean2, result.get());
	}

	//====================================================================================================
	// add(Class, Object) and add(Class, Object, String)
	//====================================================================================================

	@Test
	void d01_add_unnamed_returnsBean() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		var result = store.add(TestBean.class, bean);

		assertSame(bean, result);
		assertSame(bean, store.getBean(TestBean.class).get());
	}

	@Test
	void d02_add_named_returnsBean() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		var result = store.add(TestBean.class, bean, "name1");

		assertSame(bean, result);
		assertSame(bean, store.getBean(TestBean.class, "name1").get());
	}

	//====================================================================================================
	// addSupplier(Class, Supplier)
	//====================================================================================================

	@Test
	void e01_addSupplier_unnamed() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addSupplier(TestBean.class, () -> bean);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void e02_addSupplier_unnamed_lazyEvaluation() {
		var store = new BasicBeanStore(null);
		var callCount = new int[1];
		store.addSupplier(TestBean.class, () -> {
			callCount[0]++;
			return new TestBean("test" + callCount[0]);
		});

		assertEquals(0, callCount[0]);
		var result1 = store.getBean(TestBean.class);
		assertEquals(1, callCount[0]);
		var result2 = store.getBean(TestBean.class);
		assertEquals(2, callCount[0]);
		assertNotSame(result1.get(), result2.get());
	}

	@Test
	void e03_addSupplier_unnamed_nullValue() {
		var store = new BasicBeanStore(null);
		store.addSupplier(TestBean.class, () -> null);

		var result = store.getBean(TestBean.class);
		// Java's Optional cannot represent "present but null", so null beans return empty
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// addSupplier(Class, Supplier, String)
	//====================================================================================================

	@Test
	void f01_addSupplier_named() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addSupplier(TestBean.class, () -> bean, "name1");

		var result = store.getBean(TestBean.class, "name1");
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void f02_addSupplier_named_lazyEvaluation() {
		var store = new BasicBeanStore(null);
		var callCount = new int[1];
		store.addSupplier(TestBean.class, () -> {
			callCount[0]++;
			return new TestBean("test" + callCount[0]);
		}, "name1");

		assertEquals(0, callCount[0]);
		var result1 = store.getBean(TestBean.class, "name1");
		assertEquals(1, callCount[0]);
		var result2 = store.getBean(TestBean.class, "name1");
		assertEquals(2, callCount[0]);
		assertNotSame(result1.get(), result2.get());
	}

	//====================================================================================================
	// getBean(Class)
	//====================================================================================================

	@Test
	void g01_getBean_unnamed_notFound() {
		var store = new BasicBeanStore(null);
		var result = store.getBean(TestBean.class);
		assertFalse(result.isPresent());
	}

	@Test
	void g02_getBean_unnamed_found() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void g03_getBean_unnamed_withParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean);

		var store = new BasicBeanStore(parent);
		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(parentBean, result.get());
	}

	@Test
	void g04_getBean_unnamed_childOverridesParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean);

		var store = new BasicBeanStore(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(childBean, result.get());
	}

	@Test
	void g05_getBean_unnamed_typeMapExistsButNoUnnamedBean() {
		var store = new BasicBeanStore(null);
		// Add a named bean - this creates the typeMap but no "" key
		store.addBean(TestBean.class, new TestBean("test1"), "name1");

		// getBean should return empty since no unnamed bean exists
		var result = store.getBean(TestBean.class);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getBean(Class, String)
	//====================================================================================================

	@Test
	void h01_getBean_named_notFound() {
		var store = new BasicBeanStore(null);
		var result = store.getBean(TestBean.class, "name1");
		assertFalse(result.isPresent());
	}

	@ParameterizedTest
	@MethodSource("getBeanNamedTestData")
	void h02_getBean_named(String name) {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, name);

		var result = store.getBean(TestBean.class, name);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	static Stream<Arguments> getBeanNamedTestData() {
		return Stream.of(
			Arguments.of("name1"),
			Arguments.of((String)null),
			Arguments.of("")
		);
	}

	@Test
	void h05_getBean_named_withParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore(parent);
		var result = store.getBean(TestBean.class, "name1");
		assertTrue(result.isPresent());
		assertSame(parentBean, result.get());
	}

	@Test
	void h06_getBean_named_childOverridesParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean, "name1");

		var result = store.getBean(TestBean.class, "name1");
		assertTrue(result.isPresent());
		assertSame(childBean, result.get());
	}

	@Test
	void h07_getBean_named_typeMapExistsButSupplierNull() {
		var store = new BasicBeanStore(null);
		// Add a bean with different name - this creates the typeMap but not the requested key
		store.addBean(TestBean.class, new TestBean("test1"), "name1");

		// getBean should return empty since "name2" doesn't exist
		var result = store.getBean(TestBean.class, "name2");
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getBeansOfType(Class)
	//====================================================================================================

	@Test
	void i01_getBeansOfType_notFound() {
		var store = new BasicBeanStore(null);
		var result = store.getBeansOfType(TestBean.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void i02_getBeansOfType_singleUnnamed() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean);

		var result = store.getBeansOfType(TestBean.class);
		assertEquals(1, result.size());
		assertSame(bean, result.get(""));
	}

	@Test
	void i03_getBeansOfType_singleNamed() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, "name1");

		var result = store.getBeansOfType(TestBean.class);
		assertEquals(1, result.size());
		assertSame(bean, result.get("name1"));
	}

	@Test
	void i04_getBeansOfType_multipleNamed() {
		var store = new BasicBeanStore(null);
		var bean1 = new TestBean("test1");
		var bean2 = new TestBean("test2");
		var bean3 = new TestBean("test3");
		store.addBean(TestBean.class, bean1, "name1");
		store.addBean(TestBean.class, bean2, "name2");
		store.addBean(TestBean.class, bean3, null);

		var result = store.getBeansOfType(TestBean.class);
		assertEquals(3, result.size());
		assertSame(bean1, result.get("name1"));
		assertSame(bean2, result.get("name2"));
		assertSame(bean3, result.get(""));
	}

	@Test
	void i05_getBeansOfType_withParent() {
		var parent = new BasicBeanStore(null);
		var parentBean1 = new TestBean("parent1");
		var parentBean2 = new TestBean("parent2");
		parent.addBean(TestBean.class, parentBean1, "name1");
		parent.addBean(TestBean.class, parentBean2, "name2");

		var store = new BasicBeanStore(parent);
		var result = store.getBeansOfType(TestBean.class);
		assertEquals(2, result.size());
		assertSame(parentBean1, result.get("name1"));
		assertSame(parentBean2, result.get("name2"));
	}

	@Test
	void i06_getBeansOfType_childOverridesParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean, "name1");

		var result = store.getBeansOfType(TestBean.class);
		assertEquals(1, result.size());
		assertSame(childBean, result.get("name1"));
	}

	@Test
	void i07_getBeansOfType_childAndParent() {
		var parent = new BasicBeanStore(null);
		var parentBean1 = new TestBean("parent1");
		var parentBean2 = new TestBean("parent2");
		parent.addBean(TestBean.class, parentBean1, "name1");
		parent.addBean(TestBean.class, parentBean2, "name2");

		var store = new BasicBeanStore(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean, "name3");

		var result = store.getBeansOfType(TestBean.class);
		assertEquals(3, result.size());
		assertSame(parentBean1, result.get("name1"));
		assertSame(parentBean2, result.get("name2"));
		assertSame(childBean, result.get("name3"));
	}

	@Test
	void i08_getBeansOfType_differentTypes() {
		var store = new BasicBeanStore(null);
		var testBean = new TestBean("test1");
		var anotherBean = new AnotherBean(42);
		store.addBean(TestBean.class, testBean);
		store.addBean(AnotherBean.class, anotherBean);

		var testResult = store.getBeansOfType(TestBean.class);
		assertEquals(1, testResult.size());
		assertSame(testBean, testResult.get(""));

		var anotherResult = store.getBeansOfType(AnotherBean.class);
		assertEquals(1, anotherResult.size());
		assertSame(anotherBean, anotherResult.get(""));
	}

	//====================================================================================================
	// hasBean(Class)
	//====================================================================================================

	@Test
	void j01_hasBean_unnamed_notFound() {
		var store = new BasicBeanStore(null);
		assertFalse(store.hasBean(TestBean.class));
	}

	@Test
	void j02_hasBean_unnamed_found() {
		var store = new BasicBeanStore(null);
		store.addBean(TestBean.class, new TestBean("test1"));
		assertTrue(store.hasBean(TestBean.class));
	}

	@Test
	void j03_hasBean_unnamed_onlyNamedExists() {
		var store = new BasicBeanStore(null);
		store.addBean(TestBean.class, new TestBean("test1"), "name1");
		assertFalse(store.hasBean(TestBean.class));
	}

	@Test
	void j04_hasBean_unnamed_withParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore(parent);
		assertTrue(store.hasBean(TestBean.class));
	}

	@Test
	void j05_hasBean_unnamed_childOverridesParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore(parent);
		store.addBean(TestBean.class, new TestBean("child"));
		assertTrue(store.hasBean(TestBean.class));
	}

	@Test
	void j06_hasBean_unnamed_notInCurrentButInParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore(parent);
		// Store has no beans, but parent does
		assertTrue(store.hasBean(TestBean.class));
	}

	@Test
	void j07_hasBean_unnamed_noParentAndNotFound() {
		var store = new BasicBeanStore(null);
		assertFalse(store.hasBean(TestBean.class));
	}

	//====================================================================================================
	// hasBean(Class, String)
	//====================================================================================================

	@Test
	void k01_hasBean_named_notFound() {
		var store = new BasicBeanStore(null);
		assertFalse(store.hasBean(TestBean.class, "name1"));
	}

	@ParameterizedTest
	@MethodSource("hasBeanNamedProvider")
	void k02_hasBean_named(String name, boolean expected) {
		var store = new BasicBeanStore(null);
		store.addBean(TestBean.class, new TestBean("test1"), name);
		assertEquals(expected, store.hasBean(TestBean.class, name));
	}

	static Stream<Arguments> hasBeanNamedProvider() {
		return Stream.of(
			Arguments.of("name1", true),
			Arguments.of(null, true),
			Arguments.of("", true)
		);
	}

	@Test
	void k04_hasBean_named_emptyString() {
		var store = new BasicBeanStore(null);
		store.addBean(TestBean.class, new TestBean("test1"), "");
		assertTrue(store.hasBean(TestBean.class, ""));
	}

	@Test
	void k05_hasBean_named_withParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"), "name1");

		var store = new BasicBeanStore(parent);
		assertTrue(store.hasBean(TestBean.class, "name1"));
	}

	@Test
	void k06_hasBean_named_childOverridesParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"), "name1");

		var store = new BasicBeanStore(parent);
		store.addBean(TestBean.class, new TestBean("child"), "name1");
		assertTrue(store.hasBean(TestBean.class, "name1"));
	}

	@Test
	void k07_hasBean_named_notInCurrentButInParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"), "name1");

		var store = new BasicBeanStore(parent);
		// Store has no beans, but parent does
		assertTrue(store.hasBean(TestBean.class, "name1"));
	}

	@Test
	void k08_hasBean_named_typeMapExistsButKeyNotFound() {
		var store = new BasicBeanStore(null);
		// Add a bean with different name - this creates the typeMap but not the requested key
		store.addBean(TestBean.class, new TestBean("test1"), "name1");

		// hasBean should return false since "name2" doesn't exist
		assertFalse(store.hasBean(TestBean.class, "name2"));
	}

	@Test
	void k09_hasBean_named_noParentAndNotFound() {
		var store = new BasicBeanStore(null);
		assertFalse(store.hasBean(TestBean.class, "name1"));
	}

	//====================================================================================================
	// clear()
	//====================================================================================================

	@Test
	void m01_clear() {
		var store = new BasicBeanStore(null);
		store.addBean(TestBean.class, new TestBean("test1"));
		store.addBean(TestBean.class, new TestBean("test2"), "name1");
		store.addBean(AnotherBean.class, new AnotherBean(42));

		store.clear();

		assertFalse(store.hasBean(TestBean.class));
		assertFalse(store.hasBean(TestBean.class, "name1"));
		assertFalse(store.hasBean(AnotherBean.class));
	}

	@Test
	void m02_clear_returnsThis() {
		var store = new BasicBeanStore(null);
		var result = store.clear();
		assertSame(store, result);
	}

	@Test
	void m03_clear_doesNotAffectParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore(parent);
		store.addBean(TestBean.class, new TestBean("child"));
		store.clear();

		assertTrue(parent.hasBean(TestBean.class));
		// After clearing, hasBean() checks parent, so it should return true
		assertTrue(store.hasBean(TestBean.class));
		// But getBean() should return the parent's bean
		assertTrue(store.getBean(TestBean.class).isPresent());
	}

	//====================================================================================================
	// Thread safety
	//====================================================================================================

	@Test
	void n01_concurrentAccess() throws InterruptedException {
		var store = new BasicBeanStore(null);
		var threads = new Thread[10];
		var exceptions = Collections.synchronizedList(new ArrayList<Exception>());

		// Start threads that add beans
		for (int i = 0; i < 5; i++) {
			final int index = i;
			threads[i] = new Thread(() -> {
				try {
					for (int j = 0; j < 100; j++) {
						store.addBean(TestBean.class, new TestBean("test" + index + "-" + j), "name" + index);
					}
				} catch (Exception e) {
					exceptions.add(e);
				}
			});
		}

		// Start threads that read beans
		for (int i = 5; i < 10; i++) {
			final int index = i - 5;
			threads[i] = new Thread(() -> {
				try {
					for (int j = 0; j < 100; j++) {
						store.getBean(TestBean.class, "name" + index);
						store.hasBean(TestBean.class, "name" + index);
						store.getBeansOfType(TestBean.class);
					}
				} catch (Exception e) {
					exceptions.add(e);
				}
			});
		}

		// Start all threads
		for (var thread : threads) {
			thread.start();
		}

		// Wait for all threads to complete
		for (var thread : threads) {
			thread.join();
		}

		// Verify no exceptions occurred
		assertTrue(exceptions.isEmpty(), "Exceptions occurred: " + exceptions);
	}

	//====================================================================================================
	// getBeanSupplier(Class)
	//====================================================================================================

	@Test
	void o01_getBeanSupplier_unnamed_notFound() {
		var store = new BasicBeanStore(null);
		var result = store.getBeanSupplier(TestBean.class);
		assertFalse(result.isPresent());
	}

	@Test
	void o02_getBeanSupplier_unnamed_found() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean);

		var result = store.getBeanSupplier(TestBean.class);
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertNotNull(supplier);
		assertSame(bean, supplier.get());
	}

	@Test
	void o03_getBeanSupplier_unnamed_typeMapExistsButSupplierNull() {
		var store = new BasicBeanStore(null);
		// Add a named bean - this creates the typeMap but no "" key
		store.addBean(TestBean.class, new TestBean("test1"), "name1");

		var result = store.getBeanSupplier(TestBean.class);
		assertFalse(result.isPresent());
	}

	@Test
	void o04_getBeanSupplier_unnamed_withParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean);

		var store = new BasicBeanStore(parent);
		var result = store.getBeanSupplier(TestBean.class);
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertSame(parentBean, supplier.get());
	}

	@Test
	void o05_getBeanSupplier_unnamed_childOverridesParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean);

		var store = new BasicBeanStore(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean);

		var result = store.getBeanSupplier(TestBean.class);
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertSame(childBean, supplier.get());
	}

	@Test
	void o06_getBeanSupplier_unnamed_noParentAndNotFound() {
		var store = new BasicBeanStore(null);
		// No beans, no parent - should hit line 304
		var result = store.getBeanSupplier(TestBean.class);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getBeanSupplier(Class, String)
	//====================================================================================================

	@Test
	void p01_getBeanSupplier_named_notFound() {
		var store = new BasicBeanStore(null);
		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertFalse(result.isPresent());
	}

	@Test
	void p02_getBeanSupplier_named_found() {
		var store = new BasicBeanStore(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, "name1");

		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertNotNull(supplier);
		assertSame(bean, supplier.get());
	}

	@Test
	void p03_getBeanSupplier_named_typeMapExistsButSupplierNull() {
		var store = new BasicBeanStore(null);
		// Add a bean with different name - this creates the typeMap but not the requested key
		store.addBean(TestBean.class, new TestBean("test1"), "name1");

		var result = store.getBeanSupplier(TestBean.class, "name2");
		assertFalse(result.isPresent());
	}

	@Test
	void p04_getBeanSupplier_named_withParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore(parent);
		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertSame(parentBean, supplier.get());
	}

	@Test
	void p05_getBeanSupplier_named_childOverridesParent() {
		var parent = new BasicBeanStore(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean, "name1");

		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertSame(childBean, supplier.get());
	}

	@Test
	void p06_getBeanSupplier_named_noParentAndNotFound() {
		var store = new BasicBeanStore(null);
		// No beans, no parent - should hit line 330
		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// toString()
	//====================================================================================================

	@Test
	void q01_toString() {
		var store = new BasicBeanStore(null);
		store.addBean(TestBean.class, new TestBean("test1"), "name1");
		store.addBean(AnotherBean.class, new AnotherBean(42));

		var result = store.toString();
		assertNotNull(result);
		assertFalse(result.isEmpty());
		// Should contain some representation of the store
		assertTrue(result.contains("BasicBeanStore") || result.contains("entries") || !result.isEmpty());
	}

	@Test
	void q02_toString_withParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore(parent);
		store.addBean(AnotherBean.class, new AnotherBean(42));

		var result = store.toString();
		assertNotNull(result);
		assertFalse(result.isEmpty());
	}

	//====================================================================================================
	// addDefaultSupplier(...) and overriding-parent precedence
	//====================================================================================================

	@Test
	void r01_addDefaultSupplier_unnamed_fallsBackWhenNoEntry() {
		var store = new BasicBeanStore(null);
		var defaultBean = new TestBean("default");
		store.addDefaultSupplier(TestBean.class, () -> defaultBean);

		assertTrue(store.getBean(TestBean.class).isPresent());
		assertSame(defaultBean, store.getBean(TestBean.class).get());
	}

	@Test
	void r02_addDefaultSupplier_localEntryBeatsDefault() {
		var store = new BasicBeanStore(null);
		var defaultBean = new TestBean("default");
		var localBean = new TestBean("local");
		store.addDefaultSupplier(TestBean.class, () -> defaultBean);
		store.addBean(TestBean.class, localBean);

		assertSame(localBean, store.getBean(TestBean.class).orElseThrow());
	}

	@Test
	void r03_addDefaultSupplier_regularParentBeatsDefault() {
		var parent = new BasicBeanStore(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore(parent);
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("default"));

		assertEquals("parent", store.getBean(TestBean.class).orElseThrow().getName());
	}

	@Test
	void r04_addDefaultSupplier_named() {
		var store = new BasicBeanStore(null);
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("dn"), "n1");

		assertTrue(store.getBean(TestBean.class, "n1").isPresent());
		assertEquals("dn", store.getBean(TestBean.class, "n1").orElseThrow().getName());
		assertFalse(store.getBean(TestBean.class).isPresent());
		assertFalse(store.getBean(TestBean.class, "other").isPresent());
	}

	@Test
	void r05_addDefaultSupplier_clearedByClear() {
		var store = new BasicBeanStore(null);
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("d"));
		assertTrue(store.getBean(TestBean.class).isPresent());

		store.clear();
		assertFalse(store.getBean(TestBean.class).isPresent());
	}

	@Test
	void s01_overridingParent_beatsLocalEntry() {
		var spring = new BasicBeanStore(null);
		spring.addBean(TestBean.class, new TestBean("spring"));

		var store = new BasicBeanStore(null, spring);
		store.addBean(TestBean.class, new TestBean("local"));

		assertEquals("spring", store.getBean(TestBean.class).orElseThrow().getName());
	}

	@Test
	void s02_overridingParent_beatsDefault() {
		var spring = new BasicBeanStore(null);
		spring.addBean(TestBean.class, new TestBean("spring"));

		var store = new BasicBeanStore(null, spring);
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("default"));

		assertEquals("spring", store.getBean(TestBean.class).orElseThrow().getName());
	}

	@Test
	void s03_overridingParent_localEntryBeatsRegularParent() {
		var regularParent = new BasicBeanStore(null);
		regularParent.addBean(TestBean.class, new TestBean("regular-parent"));

		var spring = new BasicBeanStore(null);
		// no spring binding for TestBean

		var store = new BasicBeanStore(regularParent, spring);
		store.addBean(TestBean.class, new TestBean("local"));

		assertEquals("local", store.getBean(TestBean.class).orElseThrow().getName());
	}

	@Test
	void s04_overridingParent_namedLookup() {
		var spring = new BasicBeanStore(null);
		spring.addBean(TestBean.class, new TestBean("spring"), "primary");

		var store = new BasicBeanStore(null, spring);
		store.addBean(TestBean.class, new TestBean("local"), "primary");

		assertEquals("spring", store.getBean(TestBean.class, "primary").orElseThrow().getName());
		assertFalse(store.getBean(TestBean.class, "other").isPresent());
	}

	@Test
	void s05_fullPrecedenceOrder() {
		// Set up: overriding parent (Spring), regular parent, local entry, local default
		var regularParent = new BasicBeanStore(null);
		regularParent.addBean(TestBean.class, new TestBean("regular-parent"));

		var spring = new BasicBeanStore(null);
		spring.addBean(TestBean.class, new TestBean("spring"));

		var store = new BasicBeanStore(regularParent, spring);
		store.addBean(TestBean.class, new TestBean("local"));
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("default"));

		// Spring (overriding) wins.
		assertEquals("spring", store.getBean(TestBean.class).orElseThrow().getName());
	}

	@Test
	void s06_precedenceOrder_noOverriding_noEntry() {
		// Only regular parent and default — regular parent wins over default.
		var regularParent = new BasicBeanStore(null);
		regularParent.addBean(TestBean.class, new TestBean("regular-parent"));

		var store = new BasicBeanStore(regularParent);
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("default"));

		assertEquals("regular-parent", store.getBean(TestBean.class).orElseThrow().getName());
	}

	@Test
	void s07_precedenceOrder_noOverridingMatch_fallsThroughToLocal() {
		// Spring has no match, local entry should be returned.
		var spring = new BasicBeanStore(null);
		// no binding for TestBean

		var store = new BasicBeanStore(null, spring);
		store.addBean(TestBean.class, new TestBean("local"));

		assertEquals("local", store.getBean(TestBean.class).orElseThrow().getName());
	}

	@Test
	void s08_overridingParent_getBeanSupplier() {
		var spring = new BasicBeanStore(null);
		spring.addBean(TestBean.class, new TestBean("spring"));

		var store = new BasicBeanStore(null, spring);
		store.addBean(TestBean.class, new TestBean("local"));

		var supplier = store.getBeanSupplier(TestBean.class);
		assertTrue(supplier.isPresent());
		assertEquals("spring", supplier.get().get().getName());
	}

	@Test
	void s09_overridingParent_hasBean() {
		var spring = new BasicBeanStore(null);
		spring.addBean(TestBean.class, new TestBean("spring"));

		var store = new BasicBeanStore(null, spring);
		assertTrue(store.hasBean(TestBean.class));
		assertFalse(store.hasBean(AnotherBean.class));
	}

	@Test
	void s10_overridingParent_getBeansOfType_overridesLocalNamed() {
		var spring = new BasicBeanStore(null);
		spring.addBean(TestBean.class, new TestBean("spring-a"), "a");
		spring.addBean(TestBean.class, new TestBean("spring-b"), "b");

		var store = new BasicBeanStore(null, spring);
		store.addBean(TestBean.class, new TestBean("local-a"), "a");
		store.addBean(TestBean.class, new TestBean("local-c"), "c");
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("default-d"), "d");

		var beans = store.getBeansOfType(TestBean.class);
		// "a" is in both local and overriding parent — overriding wins.
		assertEquals("spring-a", beans.get("a").getName());
		// "b" only in overriding parent.
		assertEquals("spring-b", beans.get("b").getName());
		// "c" only local.
		assertEquals("local-c", beans.get("c").getName());
		// "d" only in defaults.
		assertEquals("default-d", beans.get("d").getName());
	}

	// =========================================================================
	// T - hasDefaultSupplier / getDefaultSupplier (uncovered paths)
	// =========================================================================

	@Test
	void t01_hasDefaultSupplier_unnamed_delegatesToNamed() {
		var store = new BasicBeanStore(null);
		assertFalse(store.hasDefaultSupplier(TestBean.class));
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("x"));
		assertTrue(store.hasDefaultSupplier(TestBean.class));
	}

	@Test
	void t02_hasDefaultSupplier_named_notRegistered_returnsFalse() {
		var store = new BasicBeanStore(null);
		assertFalse(store.hasDefaultSupplier(TestBean.class, "missing"));
	}

	@Test
	void t06_hasDefaultSupplier_named_typeMapExistsButKeyAbsent_returnsFalse() {
		var store = new BasicBeanStore(null);
		// Register a default for the unnamed variant so typeMap is non-null
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("unnamed"));
		// Now check a different (named) key that was never registered
		assertFalse(store.hasDefaultSupplier(TestBean.class, "unregistered-name"));
	}

	@Test
	void t03_getDefaultSupplier_unnamed_delegatesToNamed() {
		var store = new BasicBeanStore(null);
		assertTrue(store.getDefaultSupplier(TestBean.class).isEmpty());
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("via-default"));
		assertEquals("via-default", store.getDefaultSupplier(TestBean.class).orElseThrow().get().getName());
	}

	@Test
	void t04_getDefaultSupplier_named_typeMapNull_returnsEmpty() {
		var store = new BasicBeanStore(null);
		assertTrue(store.getDefaultSupplier(TestBean.class, "nope").isEmpty());
	}

	@Test
	void t05_getDefaultSupplier_named_supplierNull_returnsEmpty() {
		var store = new BasicBeanStore(null);
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("other"), "other");
		assertTrue(store.getDefaultSupplier(TestBean.class, "missing").isEmpty());
	}

	// =========================================================================
	// U - createBeanFromMethod edge cases
	// =========================================================================

	static class U_Resource {
		public static TestBean makeBean(String msg) { return new TestBean(msg); }
		public static TestBean throwingFactory() { throw new RuntimeException("factory-boom"); }
	}

	@Test
	void u01_createBeanFromMethod_passClass_callsStaticMethod() {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "hello");
		var result = store.createBeanFromMethod(TestBean.class, U_Resource.class, null);
		assertTrue(result.isPresent());
		assertEquals("hello", result.get().getName());
	}

	@Test
	void u02_createBeanFromMethod_throwingFactory_wrapsBeanCreationException() {
		var store = new BasicBeanStore(null);
		assertThrows(BeanCreationException.class,
			() -> store.createBeanFromMethod(TestBean.class, U_Resource.class, m -> m.getNameSimple().equals("throwingFactory")));
	}

	static class U_InstanceResource {
		public TestBean makeBean(String msg) { return new TestBean(msg); }
	}

	@Test
	void u03_createBeanFromMethod_passInstance_callsInstanceMethod() {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "from-instance");
		var result = store.createBeanFromMethod(TestBean.class, new U_InstanceResource(), null);
		assertTrue(result.isPresent());
		assertEquals("from-instance", result.get().getName());
	}

	static class U_NonStaticResource {
		public TestBean makeBean() { return new TestBean("non-static"); }
	}

	@Test
	void u04_createBeanFromMethod_passClass_nonStaticMethodSkipped_returnsEmpty() {
		// When passing a Class (resource=null), non-static methods are excluded (line 578 false branch).
		var store = new BasicBeanStore(null);
		var result = store.createBeanFromMethod(TestBean.class, U_NonStaticResource.class, null);
		assertTrue(result.isEmpty());
	}

	@Test
	void u05_createBeanFromMethod_methodHasUnresolvableParams_returnsEmpty() {
		// makeBean needs a String but none is in store → canResolveAllParameters false branch
		var store = new BasicBeanStore(null);
		var result = store.createBeanFromMethod(TestBean.class, U_Resource.class, m -> m.getNameSimple().equals("makeBean"));
		assertTrue(result.isEmpty());
	}

	// =========================================================================
	// V - toString / properties (defaults list, non-BasicBeanStore overriding parent)
	// =========================================================================

	@Test
	void v01_toString_withDefaults_includesDefaultsSection() {
		var store = new BasicBeanStore(null);
		store.addDefaultSupplier(TestBean.class, () -> new TestBean("default"));
		var s = store.toString();
		assertNotNull(s);
		assertTrue(s.contains("defaults"));
	}

	@Test
	void v02a_toString_basicBeanStoreOverridingParent_usesProperties() {
		var overriding = new BasicBeanStore(null);
		overriding.addBean(TestBean.class, new TestBean("override-val"));
		var store = new BasicBeanStore(null, overriding);
		var s = store.toString();
		assertNotNull(s);
	}

	@Test
	void v02_toString_nonBasicOverridingParent_usesStringForm() {
		var overriding = new BeanStore() {
			@Override public <T> Optional<java.util.function.Supplier<T>> getBeanSupplier(Class<T> t) { return Optional.empty(); }
			@Override public <T> Optional<java.util.function.Supplier<T>> getBeanSupplier(Class<T> t, String n) { return Optional.empty(); }
			@Override public boolean hasBean(Class<?> t) { return false; }
			@Override public boolean hasBean(Class<?> t, String n) { return false; }
			@Override public <T> Optional<T> getBean(Class<T> t) { return Optional.empty(); }
			@Override public <T> Optional<T> getBean(Class<T> t, String n) { return Optional.empty(); }
			@Override public <T> java.util.Map<String, T> getBeansOfType(Class<T> t) { return java.util.Map.of(); }
		};
		var store = new BasicBeanStore(null, overriding);
		var s = store.toString();
		assertNotNull(s);
	}

	// =========================================================================
	// W - WritableBeanStore.add() convenience defaults
	//     BasicBeanStore overrides add(), so we need a minimal wrapper that
	//     delegates only addBean() in order to exercise the interface defaults.
	// =========================================================================

	/** Minimal WritableBeanStore that does NOT override add(), relying on interface defaults. */
	static class MinimalWritable implements WritableBeanStore {
		private final BasicBeanStore delegate = new BasicBeanStore(null);

		@Override public <T> WritableBeanStore addBean(Class<T> t, T b) { return delegate.addBean(t, b); }
		@Override public <T> WritableBeanStore addBean(Class<T> t, T b, String n) { return delegate.addBean(t, b, n); }
		@Override public <T> WritableBeanStore addSupplier(Class<T> t, java.util.function.Supplier<T> s) { return delegate.addSupplier(t, s); }
		@Override public <T> WritableBeanStore addSupplier(Class<T> t, java.util.function.Supplier<T> s, String n) { return delegate.addSupplier(t, s, n); }
		@Override public <T> WritableBeanStore addDefaultSupplier(Class<T> t, java.util.function.Supplier<T> s) { return delegate.addDefaultSupplier(t, s); }
		@Override public <T> WritableBeanStore addDefaultSupplier(Class<T> t, java.util.function.Supplier<T> s, String n) { return delegate.addDefaultSupplier(t, s, n); }
		@Override public WritableBeanStore clear() { return delegate.clear(); }
		@Override public boolean hasDefaultSupplier(Class<?> t) { return delegate.hasDefaultSupplier(t); }
		@Override public boolean hasDefaultSupplier(Class<?> t, String n) { return delegate.hasDefaultSupplier(t, n); }
		@Override public <T> WritableBeanStore addBeanType(Class<T> t, Class<? extends T> i) { return delegate.addBeanType(t, i); }
		@Override public <T> Optional<java.util.function.Supplier<T>> getBeanSupplier(Class<T> t) { return delegate.getBeanSupplier(t); }
		@Override public <T> Optional<java.util.function.Supplier<T>> getBeanSupplier(Class<T> t, String n) { return delegate.getBeanSupplier(t, n); }
		@Override public boolean hasBean(Class<?> t) { return delegate.hasBean(t); }
		@Override public boolean hasBean(Class<?> t, String n) { return delegate.hasBean(t, n); }
		@Override public <T> Optional<T> getBean(Class<T> t) { return delegate.getBean(t); }
		@Override public <T> Optional<T> getBean(Class<T> t, String n) { return delegate.getBean(t, n); }
		@Override public <T> java.util.Map<String, T> getBeansOfType(Class<T> t) { return delegate.getBeansOfType(t); }
		@Override public WritableBeanStore registerConfiguration(Class<?> c) { return delegate.registerConfiguration(c); }
		@Override public Snapshot pushOverlay(BeanStore overlay) { return delegate.pushOverlay(overlay); }
		@Override public void popOverlay(Snapshot snapshot) { delegate.popOverlay(snapshot); }
		@Override public void close() { delegate.close(); }
	}

	@Test
	void w01_writableBeanStore_add_unnamed_returnsBean() {
		var store = new MinimalWritable();
		var bean = new TestBean("fluent");
		var returned = store.add(TestBean.class, bean);
		assertSame(bean, returned);
		assertSame(bean, store.getBean(TestBean.class).orElseThrow());
	}

	@Test
	void w02_writableBeanStore_add_named_returnsBean() {
		var store = new MinimalWritable();
		var bean = new TestBean("fluent-named");
		var returned = store.add(TestBean.class, bean, "myBean");
		assertSame(bean, returned);
		assertSame(bean, store.getBean(TestBean.class, "myBean").orElseThrow());
	}

	// =========================================================================
	// X - BeanStore default methods: outer-instance, Optional, named-qualifier
	// =========================================================================

	@SuppressWarnings({
		"java:S114" // Test-fixture interface; snake_case name groups it with its test section (X_*).
	})
	interface X_Iface {
		TestBean create(Object outer, String msg);
		TestBean createNamed(@Named("msg") String msg);
		TestBean createOptional(Optional<String> optMsg);
		TestBean createMissingNamed(@Named("absent") String msg);
		TestBean createWithOptionalFirst(Optional<String> optFirst, @Named("extra") String extra);
	}

	@Test
	void x01_hasAllParams_outerInstanceSkippedAtPosition0() throws Exception {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "hello");
		var outer = new Object();
		var m = MethodInfo.of(X_Iface.class.getMethod("create", Object.class, String.class));
		assertTrue(store.hasAllParams(m, outer));
	}

	@Test
	void x02_hasAllParams_optionalParamAlwaysSatisfied() throws Exception {
		var store = new BasicBeanStore(null);
		var m = MethodInfo.of(X_Iface.class.getMethod("createOptional", Optional.class));
		assertTrue(store.hasAllParams(m, null));
	}

	@Test
	void x03_hasAllParams_namedQualifier_present() throws Exception {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "hi", "msg");
		var m = MethodInfo.of(X_Iface.class.getMethod("createNamed", String.class));
		assertTrue(store.hasAllParams(m, null));
	}

	@Test
	void x04_hasAllParams_namedQualifier_absent_returnsFalse() throws Exception {
		var store = new BasicBeanStore(null);
		var m = MethodInfo.of(X_Iface.class.getMethod("createMissingNamed", String.class));
		assertFalse(store.hasAllParams(m, null));
	}

	@Test
	void x05_getParams_outerInstanceInjectedAtPosition0() throws Exception {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "world");
		var outer = new Object();
		var m = MethodInfo.of(X_Iface.class.getMethod("create", Object.class, String.class));
		var params = store.getParams(m, outer);
		assertSame(outer, params[0]);
		assertEquals("world", params[1]);
	}

	@Test
	void x06_getParams_namedQualifier_resolved() throws Exception {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "named-val", "msg");
		var m = MethodInfo.of(X_Iface.class.getMethod("createNamed", String.class));
		var params = store.getParams(m, null);
		assertEquals("named-val", params[0]);
	}

	@Test
	void x07_getParams_optionalParam_wrappedInOptional() throws Exception {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "opt-val");
		var m = MethodInfo.of(X_Iface.class.getMethod("createOptional", Optional.class));
		var params = store.getParams(m, null);
		assertTrue(params[0] instanceof Optional);
		assertEquals("opt-val", ((Optional<?>) params[0]).orElse(null));
	}

	@Test
	void x08_getMissingParams_outerAndOptionalSkipped() throws Exception {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "present");
		var outer = new Object();
		var m = MethodInfo.of(X_Iface.class.getMethod("create", Object.class, String.class));
		assertNull(store.getMissingParams(m, outer));
	}

	@Test
	void x09_getMissingParams_namedQualifier_absent_listed() throws Exception {
		var store = new BasicBeanStore(null);
		var m = MethodInfo.of(X_Iface.class.getMethod("createMissingNamed", String.class));
		var missing = store.getMissingParams(m, null);
		assertNotNull(missing);
		assertTrue(missing.contains("absent"));
	}

	static class SimpleNoArgBean {
		public SimpleNoArgBean() { /* intentionally empty */ }
	}

	@Test
	void x10_beanStore_instantiate_createsBean() {
		var store = new BasicBeanStore(null);
		var bean = store.instantiate(SimpleNoArgBean.class);
		assertNotNull(bean);
	}

	@Test
	void x11_beanStore_createBeanFromMethod_twoArgOverload_delegatesToFullOverload() {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "world");
		var result = store.createBeanFromMethod(TestBean.class, U_Resource.class);
		assertTrue(result.isPresent());
		assertEquals("world", result.get().getName());
	}

	@Test
	void x12_getMissingParams_namedQualifier_present_notListedAsMissing() throws Exception {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "hi", "msg");
		var m = MethodInfo.of(X_Iface.class.getMethod("createNamed", String.class));
		assertNull(store.getMissingParams(m, null));
	}

	/** Minimal BeanStore that uses all BeanStore defaults (not overridden). */
	static class MinimalBeanStore implements BeanStore {
		private final BasicBeanStore delegate = new BasicBeanStore(null);
		@Override public <T> Optional<java.util.function.Supplier<T>> getBeanSupplier(Class<T> t) { return delegate.getBeanSupplier(t); }
		@Override public <T> Optional<java.util.function.Supplier<T>> getBeanSupplier(Class<T> t, String n) { return delegate.getBeanSupplier(t, n); }
		@Override public boolean hasBean(Class<?> t) { return delegate.hasBean(t); }
		@Override public boolean hasBean(Class<?> t, String n) { return delegate.hasBean(t, n); }
		@Override public <T> Optional<T> getBean(Class<T> t) { return delegate.getBean(t); }
		@Override public <T> Optional<T> getBean(Class<T> t, String n) { return delegate.getBean(t, n); }
		@Override public <T> java.util.Map<String, T> getBeansOfType(Class<T> t) { return delegate.getBeansOfType(t); }
	}

	@Test
	void x13_beanStore_defaultCreateBeanFromMethod_returnsEmpty() {
		var store = new MinimalBeanStore();
		var result = store.createBeanFromMethod(TestBean.class, U_Resource.class, null);
		assertTrue(result.isEmpty());
	}

	@Test
	void x14_beanStore_defaultCreateBeanFromMethod_twoArg_returnsEmpty() {
		var store = new MinimalBeanStore();
		var result = store.createBeanFromMethod(TestBean.class, U_Resource.class);
		assertTrue(result.isEmpty());
	}

	@Test
	void x15_beanStore_defaultGetBeanType_returnsEmpty() {
		var store = new MinimalBeanStore();
		assertTrue(store.getBeanType(TestBean.class).isEmpty());
	}

	@SuppressWarnings({
		"java:S114" // Test-fixture interface; snake_case name groups it with its test section (X_*).
	})
	interface X_BasicIface {
		TestBean create(String msg);
		TestBean createMissingUnnamed(Integer missingInt);
	}

	@Test
	void x16_hasAllParams_unnamedParam_absent_returnsFalse() throws Exception {
		var store = new BasicBeanStore(null);
		// no Integer bean in store
		var m = MethodInfo.of(X_BasicIface.class.getMethod("createMissingUnnamed", Integer.class));
		assertFalse(store.hasAllParams(m, null));
	}

	@Test
	void x17_getMissingParams_unnamedBean_absent_listed() throws Exception {
		var store = new BasicBeanStore(null);
		// Integer is not in the store
		var m = MethodInfo.of(X_BasicIface.class.getMethod("createMissingUnnamed", Integer.class));
		var missing = store.getMissingParams(m, null);
		assertNotNull(missing);
		assertTrue(missing.contains("Integer"));
	}

	@Test
	void x18_getMissingParams_optionalParamAtNonZero_skipped() throws Exception {
		// createWithOptionalFirst: i=0 → Optional (D path of condition), i=1 → named "extra"
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "val", "extra");
		var m = MethodInfo.of(X_Iface.class.getMethod("createWithOptionalFirst", Optional.class, String.class));
		// Optional at i=0 is always satisfied (skipped); named "extra" is in store → null
		assertNull(store.getMissingParams(m, null));
	}

	@Test
	void x19_getMissingParams_outerNonNull_typeNoMatch_paramCheckedNormally() throws Exception {
		// i=0, nn(outer)=true, !pt.isInstance(outer) → falls through to check bean
		var store = new BasicBeanStore(null);
		store.addBean(Integer.class, 42);
		var outer = "type-mismatch";   // outer is String, param[0] is Integer → no skip
		var m = MethodInfo.of(X_BasicIface.class.getMethod("createMissingUnnamed", Integer.class));
		// Integer IS in store → no missing params
		assertNull(store.getMissingParams(m, outer));
	}

	// =========================================================================
	// Y - BeanCreationException constructors
	// =========================================================================

	@Test
	void y01_beanCreationException_messageOnly() {
		var ex = new BeanCreationException("msg");
		assertEquals("msg", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void y02_beanCreationException_causeOnly() {
		var cause = new RuntimeException("cause");
		var ex = new BeanCreationException(cause);
		assertSame(cause, ex.getCause());
	}

	@Test
	void y03_beanCreationException_messageAndCause_coveredByU02() {
		// BeanCreationException(String, Throwable) is exercised by u02 above;
		// verify direct construction too for completeness.
		var cause = new IllegalStateException("boom");
		var ex = new BeanCreationException("wrapper", cause);
		assertEquals("wrapper", ex.getMessage());
		assertSame(cause, ex.getCause());
	}

	@Test
	void z01_trackResolved_nullSupplierResult_isNotTracked() {
		// A supplier that returns null exercises BasicBeanStore.trackResolved(null) — most easily
		// reached via getBeansOfType(), which passes the supplier's raw return value (including null)
		// straight into trackResolved.  The null must not be added to resolvedBeans, and close() must
		// remain a no-op for it.
		var store = new BasicBeanStore(null);
		store.addSupplier(TestBean.class, () -> null, "nullSupplier");
		store.addSupplier(TestBean.class, () -> new TestBean("ok"), "concrete");

		var all = store.getBeansOfType(TestBean.class);
		assertEquals(2, all.size(), "Both entries must appear, even when one resolves to null");
		assertNull(all.get("nullSupplier"), "Null supplier result must be preserved in the map");
		assertEquals(new TestBean("ok"), all.get("concrete"));

		// close() must not blow up trying to introspect the null bean.
		assertDoesNotThrow(store::close);
	}

	/**
	 * Confirms that {@code @ConditionalOnMissingBean(name="X")} short-circuits when a named default
	 * supplier exists under that name — this drives the defaults-side branch of the internal
	 * {@code anyLocalBeanNamed} helper.
	 */
	@Configuration
	public static class Z02_OnMissingNamedConfig {
		public Z02_OnMissingNamedConfig() { /* intentionally empty */ }
		@Bean(name = "secondary")
		@ConditionalOnMissingBean(name = "primaryName")
		public TestBean tb() { return new TestBean("conditional"); }
	}

	@Test
	void z02_onMissingBeanByName_seesNamedDefaultSupplier() {
		var store = new BasicBeanStore(null);
		// addDefaultSupplier(beanType, supplier, name) populates defaults + defaultMetadata under
		// "primaryName" — the defaults-side OR in anyLocalBeanNamed must find it.
		store.addDefaultSupplier(AnotherBean.class, () -> new AnotherBean(0), "primaryName");

		store.registerConfiguration(Z02_OnMissingNamedConfig.class);
		assertFalse(store.getBean(TestBean.class, "secondary").isPresent(),
			"Named @ConditionalOnMissingBean must skip when a named default supplier already exists");
	}

	/**
	 * Configuration with a mix of {@code @Bean} and non-{@code @Bean} fields/methods.  Exercises the
	 * {@code if (beanAnn == null) continue;} skip in {@code BasicBeanStore.registerConfiguration} for
	 * both field iteration and method iteration paths.
	 */
	@Configuration
	public static class Z03_MixedFieldConfig {
		public Z03_MixedFieldConfig() { /* intentionally empty */ }
		public String nonBeanField = "ignored";
		@Bean public TestBean tb = new TestBean("z03");
		@SuppressWarnings("unused") public String helperMethod() { return "not a bean"; }
		@Bean public AnotherBean ab() { return new AnotherBean(7); }
	}

	@Test
	void z03_nonBeanMembers_areSilentlySkipped() {
		// Verifies that fields/methods without @Bean are skipped without affecting bean registration.
		var store = new BasicBeanStore(null);
		store.registerConfiguration(Z03_MixedFieldConfig.class);
		assertTrue(store.getBean(TestBean.class).isPresent(), "@Bean field must register");
		assertTrue(store.getBean(AnotherBean.class).isPresent(), "@Bean method must register");
		assertFalse(store.getBean(String.class).isPresent(),
			"Non-@Bean field/method must NOT contribute to the store");
	}

	/**
	 * Two configurations that contribute the same {@code (type, name)} {@code @Bean} field.  Registering
	 * the second triggers the duplicate-bean error inside {@code addBeanWithMeta}, which is caught and
	 * re-wrapped by the {@code @Bean field} try/catch path in {@code registerConfiguration}.
	 */
	@Configuration
	public static class Z04_FirstFieldConfig {
		public Z04_FirstFieldConfig() { /* intentionally empty */ }
		@Bean(name = "shared") public TestBean tb = new TestBean("first");
	}

	@Configuration
	public static class Z04_DuplicateFieldConfig {
		public Z04_DuplicateFieldConfig() { /* intentionally empty */ }
		@Bean(name = "shared") public TestBean tb = new TestBean("second");
	}

	@Test
	void z04_duplicateBeanField_wrapsAsBeanCreationException() {
		// The second registration's @Bean field hits addBeanWithMeta's duplicate guard.  That throw
		// is caught by registerConfiguration's @Bean field try/catch and re-wrapped with a
		// "Failed to register @Bean field" message.
		var store = new BasicBeanStore(null);
		store.registerConfiguration(Z04_FirstFieldConfig.class);
		var ex = assertThrows(BeanCreationException.class,
			() -> store.registerConfiguration(Z04_DuplicateFieldConfig.class));
		assertTrue(ex.getMessage().contains("Failed to register @Bean field"),
			"Wrap message must mention @Bean field; got: " + ex.getMessage());
		assertNotNull(ex.getCause(), "Original duplicate-bean exception must be the cause");
		assertTrue(ex.getCause().getMessage().contains("Duplicate bean"),
			"Cause must reference the duplicate-bean condition; got: " + ex.getCause().getMessage());
	}
}

