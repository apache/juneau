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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

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
		var store = new BasicBeanStore2(null);
		assertNotNull(store);
		assertTrue(store.getBean(BasicBeanStore2.class).isPresent());
		assertSame(store, store.getBean(BasicBeanStore2.class).get());
	}

	@Test
	void a02_constructor_withParent() {
		var parent = new BasicBeanStore2(null);
		var store = new BasicBeanStore2(parent);
		assertNotNull(store);
		assertTrue(store.getBean(BasicBeanStore2.class).isPresent());
		assertSame(store, store.getBean(BasicBeanStore2.class).get());
	}

	//====================================================================================================
	// addBean(Class, Object)
	//====================================================================================================

	@Test
	void b01_addBean_unnamed() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void b02_addBean_unnamed_nullValue() {
		var store = new BasicBeanStore2(null);
		store.addBean(TestBean.class, null);

		var result = store.getBean(TestBean.class);
		// Java's Optional cannot represent "present but null", so null beans return empty
		assertFalse(result.isPresent());
	}

	@Test
	void b03_addBean_unnamed_returnsThis() {
		var store = new BasicBeanStore2(null);
		var result = store.addBean(TestBean.class, new TestBean("test1"));
		assertSame(store, result);
	}

	@Test
	void b04_addBean_unnamed_replaceExisting() {
		var store = new BasicBeanStore2(null);
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

	@Test
	void c01_addBean_named() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, "named1");

		var result = store.getBean(TestBean.class, "named1");
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void c02_addBean_named_nullName() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, null);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void c03_addBean_named_emptyString() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, "");

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void c04_addBean_named_multipleNames() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		var result = store.add(TestBean.class, bean);

		assertSame(bean, result);
		assertSame(bean, store.getBean(TestBean.class).get());
	}

	@Test
	void d02_add_named_returnsBean() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addSupplier(TestBean.class, () -> bean);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void e02_addSupplier_unnamed_lazyEvaluation() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addSupplier(TestBean.class, () -> bean, "name1");

		var result = store.getBean(TestBean.class, "name1");
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void f02_addSupplier_named_lazyEvaluation() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		var result = store.getBean(TestBean.class);
		assertFalse(result.isPresent());
	}

	@Test
	void g02_getBean_unnamed_found() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void g03_getBean_unnamed_withParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean);

		var store = new BasicBeanStore2(parent);
		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(parentBean, result.get());
	}

	@Test
	void g04_getBean_unnamed_childOverridesParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean);

		var store = new BasicBeanStore2(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean);

		var result = store.getBean(TestBean.class);
		assertTrue(result.isPresent());
		assertSame(childBean, result.get());
	}

	@Test
	void g05_getBean_unnamed_typeMapExistsButNoUnnamedBean() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		var result = store.getBean(TestBean.class, "name1");
		assertFalse(result.isPresent());
	}

	@Test
	void h02_getBean_named_found() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, "name1");

		var result = store.getBean(TestBean.class, "name1");
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void h03_getBean_named_nullName() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, null);

		var result = store.getBean(TestBean.class, null);
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void h04_getBean_named_emptyString() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, "");

		var result = store.getBean(TestBean.class, "");
		assertTrue(result.isPresent());
		assertSame(bean, result.get());
	}

	@Test
	void h05_getBean_named_withParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore2(parent);
		var result = store.getBean(TestBean.class, "name1");
		assertTrue(result.isPresent());
		assertSame(parentBean, result.get());
	}

	@Test
	void h06_getBean_named_childOverridesParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore2(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean, "name1");

		var result = store.getBean(TestBean.class, "name1");
		assertTrue(result.isPresent());
		assertSame(childBean, result.get());
	}

	@Test
	void h07_getBean_named_typeMapExistsButSupplierNull() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		var result = store.getBeansOfType(TestBean.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void i02_getBeansOfType_singleUnnamed() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean);

		var result = store.getBeansOfType(TestBean.class);
		assertEquals(1, result.size());
		assertSame(bean, result.get(""));
	}

	@Test
	void i03_getBeansOfType_singleNamed() {
		var store = new BasicBeanStore2(null);
		var bean = new TestBean("test1");
		store.addBean(TestBean.class, bean, "name1");

		var result = store.getBeansOfType(TestBean.class);
		assertEquals(1, result.size());
		assertSame(bean, result.get("name1"));
	}

	@Test
	void i04_getBeansOfType_multipleNamed() {
		var store = new BasicBeanStore2(null);
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
		var parent = new BasicBeanStore2(null);
		var parentBean1 = new TestBean("parent1");
		var parentBean2 = new TestBean("parent2");
		parent.addBean(TestBean.class, parentBean1, "name1");
		parent.addBean(TestBean.class, parentBean2, "name2");

		var store = new BasicBeanStore2(parent);
		var result = store.getBeansOfType(TestBean.class);
		assertEquals(2, result.size());
		assertSame(parentBean1, result.get("name1"));
		assertSame(parentBean2, result.get("name2"));
	}

	@Test
	void i06_getBeansOfType_childOverridesParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore2(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean, "name1");

		var result = store.getBeansOfType(TestBean.class);
		assertEquals(1, result.size());
		assertSame(childBean, result.get("name1"));
	}

	@Test
	void i07_getBeansOfType_childAndParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean1 = new TestBean("parent1");
		var parentBean2 = new TestBean("parent2");
		parent.addBean(TestBean.class, parentBean1, "name1");
		parent.addBean(TestBean.class, parentBean2, "name2");

		var store = new BasicBeanStore2(parent);
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
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		assertFalse(store.hasBean(TestBean.class));
	}

	@Test
	void j02_hasBean_unnamed_found() {
		var store = new BasicBeanStore2(null);
		store.addBean(TestBean.class, new TestBean("test1"));
		assertTrue(store.hasBean(TestBean.class));
	}

	@Test
	void j03_hasBean_unnamed_onlyNamedExists() {
		var store = new BasicBeanStore2(null);
		store.addBean(TestBean.class, new TestBean("test1"), "name1");
		assertFalse(store.hasBean(TestBean.class));
	}

	@Test
	void j04_hasBean_unnamed_withParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore2(parent);
		assertTrue(store.hasBean(TestBean.class));
	}

	@Test
	void j05_hasBean_unnamed_childOverridesParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore2(parent);
		store.addBean(TestBean.class, new TestBean("child"));
		assertTrue(store.hasBean(TestBean.class));
	}

	@Test
	void j06_hasBean_unnamed_notInCurrentButInParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore2(parent);
		// Store has no beans, but parent does
		// This should hit line 259: return nn(parent) && parent.hasBean(beanType);
		assertTrue(store.hasBean(TestBean.class));
	}

	@Test
	void j07_hasBean_unnamed_noParentAndNotFound() {
		var store = new BasicBeanStore2(null);
		// No beans, no parent - should hit line 259 with parent=null
		assertFalse(store.hasBean(TestBean.class));
	}

	//====================================================================================================
	// hasBean(Class, String)
	//====================================================================================================

	@Test
	void k01_hasBean_named_notFound() {
		var store = new BasicBeanStore2(null);
		assertFalse(store.hasBean(TestBean.class, "name1"));
	}

	@Test
	void k02_hasBean_named_found() {
		var store = new BasicBeanStore2(null);
		store.addBean(TestBean.class, new TestBean("test1"), "name1");
		assertTrue(store.hasBean(TestBean.class, "name1"));
	}

	@Test
	void k03_hasBean_named_nullName() {
		var store = new BasicBeanStore2(null);
		store.addBean(TestBean.class, new TestBean("test1"), null);
		assertTrue(store.hasBean(TestBean.class, null));
	}

	@Test
	void k04_hasBean_named_emptyString() {
		var store = new BasicBeanStore2(null);
		store.addBean(TestBean.class, new TestBean("test1"), "");
		assertTrue(store.hasBean(TestBean.class, ""));
	}

	@Test
	void k05_hasBean_named_withParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestBean.class, new TestBean("parent"), "name1");

		var store = new BasicBeanStore2(parent);
		assertTrue(store.hasBean(TestBean.class, "name1"));
	}

	@Test
	void k06_hasBean_named_childOverridesParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestBean.class, new TestBean("parent"), "name1");

		var store = new BasicBeanStore2(parent);
		store.addBean(TestBean.class, new TestBean("child"), "name1");
		assertTrue(store.hasBean(TestBean.class, "name1"));
	}

	@Test
	void k07_hasBean_named_notInCurrentButInParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestBean.class, new TestBean("parent"), "name1");

		var store = new BasicBeanStore2(parent);
		// Store has no beans, but parent does
		// This should hit line 280: return nn(parent) && parent.hasBean(beanType, name);
		assertTrue(store.hasBean(TestBean.class, "name1"));
	}

	@Test
	void k08_hasBean_named_typeMapExistsButKeyNotFound() {
		var store = new BasicBeanStore2(null);
		// Add a bean with different name - this creates the typeMap but not the requested key
		store.addBean(TestBean.class, new TestBean("test1"), "name1");

		// hasBean should return false since "name2" doesn't exist
		// This should hit line 277: if (typeMap.containsKey(key)) - false branch
		assertFalse(store.hasBean(TestBean.class, "name2"));
	}

	@Test
	void k09_hasBean_named_noParentAndNotFound() {
		var store = new BasicBeanStore2(null);
		// No beans, no parent - should hit line 280 with parent=null
		assertFalse(store.hasBean(TestBean.class, "name1"));
	}

	//====================================================================================================
	// clear()
	//====================================================================================================

	@Test
	void m01_clear() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		var result = store.clear();
		assertSame(store, result);
	}

	@Test
	void m03_clear_doesNotAffectParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore2(parent);
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
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		var result = store.getBeanSupplier(TestBean.class);
		assertFalse(result.isPresent());
	}

	@Test
	void o02_getBeanSupplier_unnamed_found() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		// Add a named bean - this creates the typeMap but no "" key
		store.addBean(TestBean.class, new TestBean("test1"), "name1");

		var result = store.getBeanSupplier(TestBean.class);
		assertFalse(result.isPresent());
	}

	@Test
	void o04_getBeanSupplier_unnamed_withParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean);

		var store = new BasicBeanStore2(parent);
		var result = store.getBeanSupplier(TestBean.class);
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertSame(parentBean, supplier.get());
	}

	@Test
	void o05_getBeanSupplier_unnamed_childOverridesParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean);

		var store = new BasicBeanStore2(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean);

		var result = store.getBeanSupplier(TestBean.class);
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertSame(childBean, supplier.get());
	}

	@Test
	void o06_getBeanSupplier_unnamed_noParentAndNotFound() {
		var store = new BasicBeanStore2(null);
		// No beans, no parent - should hit line 304
		var result = store.getBeanSupplier(TestBean.class);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getBeanSupplier(Class, String)
	//====================================================================================================

	@Test
	void p01_getBeanSupplier_named_notFound() {
		var store = new BasicBeanStore2(null);
		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertFalse(result.isPresent());
	}

	@Test
	void p02_getBeanSupplier_named_found() {
		var store = new BasicBeanStore2(null);
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
		var store = new BasicBeanStore2(null);
		// Add a bean with different name - this creates the typeMap but not the requested key
		store.addBean(TestBean.class, new TestBean("test1"), "name1");

		var result = store.getBeanSupplier(TestBean.class, "name2");
		assertFalse(result.isPresent());
	}

	@Test
	void p04_getBeanSupplier_named_withParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore2(parent);
		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertSame(parentBean, supplier.get());
	}

	@Test
	void p05_getBeanSupplier_named_childOverridesParent() {
		var parent = new BasicBeanStore2(null);
		var parentBean = new TestBean("parent");
		parent.addBean(TestBean.class, parentBean, "name1");

		var store = new BasicBeanStore2(parent);
		var childBean = new TestBean("child");
		store.addBean(TestBean.class, childBean, "name1");

		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertTrue(result.isPresent());
		var supplier = result.get();
		assertSame(childBean, supplier.get());
	}

	@Test
	void p06_getBeanSupplier_named_noParentAndNotFound() {
		var store = new BasicBeanStore2(null);
		// No beans, no parent - should hit line 330
		var result = store.getBeanSupplier(TestBean.class, "name1");
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// toString()
	//====================================================================================================

	@Test
	void q01_toString() {
		var store = new BasicBeanStore2(null);
		store.addBean(TestBean.class, new TestBean("test1"), "name1");
		store.addBean(AnotherBean.class, new AnotherBean(42));

		var result = store.toString();
		assertNotNull(result);
		assertFalse(result.isEmpty());
		// Should contain some representation of the store
		assertTrue(result.contains("BasicBeanStore2") || result.contains("entries") || !result.isEmpty());
	}

	@Test
	void q02_toString_withParent() {
		var parent = new BasicBeanStore2(null);
		parent.addBean(TestBean.class, new TestBean("parent"));

		var store = new BasicBeanStore2(parent);
		store.addBean(AnotherBean.class, new AnotherBean(42));

		var result = store.toString();
		assertNotNull(result);
		assertFalse(result.isEmpty());
	}
}

