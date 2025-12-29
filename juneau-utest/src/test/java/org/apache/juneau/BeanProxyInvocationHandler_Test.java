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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.junit.jupiter.api.*;

class BeanProxyInvocationHandler_Test extends TestBase {

	public interface TestInterface {
		String getName();
		void setName(String name);
		int getAge();
		void setAge(int age);
		boolean isActive();
		void setActive(boolean active);
	}

	// Helper class with methods that have same names as Object methods but wrong signatures
	// We can't put these in an interface because they conflict with Object methods
	public static class HelperClassWithWrongSignatures {
		public boolean equals(String other) { return false; }  // Wrong: should be equals(Object)
		public int hashCode(int dummy) { return 0; }  // Wrong: should be hashCode()
		public String toString(String format) { return ""; }  // Wrong: should be toString()
	}

	BeanContext bc = BeanContext.DEFAULT;

	//====================================================================================================
	// Constructor
	//====================================================================================================

	@Test void a01_constructor() {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		assertNotNull(handler);
	}

	//====================================================================================================
	// invoke() - equals() method
	//====================================================================================================

	@Test void b01_equals_null() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		var method = Object.class.getMethod("equals", Object.class);
		var result = handler.invoke(proxy, method, new Object[]{null});
		assertFalse((Boolean)result);
	}

	@Test void b02_equals_sameProxy() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		var method = Object.class.getMethod("equals", Object.class);
		var result = handler.invoke(proxy, method, new Object[]{proxy});
		assertTrue((Boolean)result);
	}

	@Test void b03_equals_sameClassDifferentHandler() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler1 = new BeanProxyInvocationHandler<>(bm);
		var handler2 = new BeanProxyInvocationHandler<>(bm);
		var proxy1 = createProxy(TestInterface.class, handler1);
		var proxy2 = createProxy(TestInterface.class, handler2);

		// Set same values
		proxy1.setName("John");
		proxy1.setAge(25);
		proxy2.setName("John");
		proxy2.setAge(25);

		var method = Object.class.getMethod("equals", Object.class);
		var result = handler1.invoke(proxy1, method, new Object[]{proxy2});
		assertTrue((Boolean)result);
	}

	@Test void b04_equals_differentValues() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler1 = new BeanProxyInvocationHandler<>(bm);
		var handler2 = new BeanProxyInvocationHandler<>(bm);
		var proxy1 = createProxy(TestInterface.class, handler1);
		var proxy2 = createProxy(TestInterface.class, handler2);

		// Set different values
		proxy1.setName("John");
		proxy1.setAge(25);
		proxy2.setName("Jane");
		proxy2.setAge(30);

		var method = Object.class.getMethod("equals", Object.class);
		var result = handler1.invoke(proxy1, method, new Object[]{proxy2});
		assertFalse((Boolean)result);
	}

	@Test void b05_equals_withBeanMap() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		proxy.setName("John");
		proxy.setAge(25);

		// Create a regular bean with same values
		var bean = new TestBean();
		bean.name = "John";
		bean.age = 25;

		var method = Object.class.getMethod("equals", Object.class);
		var result = handler.invoke(proxy, method, new Object[]{bean});
		assertTrue((Boolean)result);
	}

	public static class TestBean {
		public String name;
		public int age;
	}

	//====================================================================================================
	// invoke() - hashCode() method
	//====================================================================================================

	@Test void c01_hashCode() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		proxy.setName("John");
		proxy.setAge(25);

		var method = Object.class.getMethod("hashCode");
		var result = handler.invoke(proxy, method, null);
		assertNotNull(result);
		assertTrue(result instanceof Integer);
	}

	@Test void c02_hashCode_sameValues() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler1 = new BeanProxyInvocationHandler<>(bm);
		var handler2 = new BeanProxyInvocationHandler<>(bm);
		var proxy1 = createProxy(TestInterface.class, handler1);
		var proxy2 = createProxy(TestInterface.class, handler2);

		proxy1.setName("John");
		proxy1.setAge(25);
		proxy2.setName("John");
		proxy2.setAge(25);

		var method = Object.class.getMethod("hashCode");
		var hashCode1 = handler1.invoke(proxy1, method, null);
		var hashCode2 = handler2.invoke(proxy2, method, null);
		assertEquals(hashCode1, hashCode2);
	}

	//====================================================================================================
	// invoke() - toString() method
	//====================================================================================================

	@Test void d01_toString() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		proxy.setName("John");
		proxy.setAge(25);
		proxy.setActive(true);

		var method = Object.class.getMethod("toString");
		var result = handler.invoke(proxy, method, null);
		assertNotNull(result);
		assertTrue(result instanceof String);
		var str = (String)result;
		assertTrue(str.contains("John") || str.contains("25") || str.contains("true"));
	}

	@Test void d02_toString_empty() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		var method = Object.class.getMethod("toString");
		var result = handler.invoke(proxy, method, null);
		assertNotNull(result);
		assertTrue(result instanceof String);
	}

	//====================================================================================================
	// invoke() - getter methods
	//====================================================================================================

	@Test void e01_getter_string() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		proxy.setName("John");

		var method = TestInterface.class.getMethod("getName");
		var result = handler.invoke(proxy, method, null);
		assertEquals("John", result);
	}

	@Test void e02_getter_int() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		proxy.setAge(25);

		var method = TestInterface.class.getMethod("getAge");
		var result = handler.invoke(proxy, method, null);
		assertEquals(25, result);
	}

	@Test void e03_getter_boolean() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		proxy.setActive(true);

		var method = TestInterface.class.getMethod("isActive");
		var result = handler.invoke(proxy, method, null);
		assertEquals(true, result);
	}

	@Test void e04_getter_null() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		// Don't set name, should return null
		var method = TestInterface.class.getMethod("getName");
		var result = handler.invoke(proxy, method, null);
		assertNull(result);
	}

	//====================================================================================================
	// invoke() - setter methods
	//====================================================================================================

	@Test void f01_setter_string() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		var method = TestInterface.class.getMethod("setName", String.class);
		var result = handler.invoke(proxy, method, new Object[]{"John"});
		assertNull(result);

		// Verify value was set
		assertEquals("John", proxy.getName());
	}

	@Test void f02_setter_int() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		var method = TestInterface.class.getMethod("setAge", int.class);
		var result = handler.invoke(proxy, method, new Object[]{25});
		assertNull(result);

		// Verify value was set
		assertEquals(25, proxy.getAge());
	}

	@Test void f03_setter_boolean() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		var method = TestInterface.class.getMethod("setActive", boolean.class);
		var result = handler.invoke(proxy, method, new Object[]{true});
		assertNull(result);

		// Verify value was set
		assertTrue(proxy.isActive());
	}

	@Test void f04_setter_null() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		var method = TestInterface.class.getMethod("setName", String.class);
		var result = handler.invoke(proxy, method, new Object[]{null});
		assertNull(result);

		// Verify null was set
		assertNull(proxy.getName());
	}

	//====================================================================================================
	// invoke() - unsupported method
	//====================================================================================================

	@Test void g01_unsupportedMethod() throws Exception {
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		// Create a method that's not a getter, setter, equals, hashCode, or toString
		var method = Object.class.getMethod("getClass");
		var exception = assertThrows(UnsupportedOperationException.class, () -> {
			handler.invoke(proxy, method, null);
		});
		assertNotNull(exception.getMessage());
		assertTrue(exception.getMessage().contains("Unsupported bean method"));
	}

	//====================================================================================================
	// invoke() - methods with same name but wrong signatures (lines 59, 74, 77 coverage)
	//====================================================================================================

	@Test void h01_equals_wrongSignature() throws Exception {
		// Test equals(String) - should not match the equals(Object) check on line 59
		// Get the method from a helper class since we can't put it in an interface
		var method = HelperClassWithWrongSignatures.class.getMethod("equals", String.class);

		// Create handler with TestInterface (any interface will do for testing)
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		// This should fall through to getter/setter/unsupported logic since it doesn't match line 59
		// (has name "equals" but wrong parameter type - String instead of Object)
		var exception = assertThrows(UnsupportedOperationException.class, () -> {
			handler.invoke(proxy, method, new Object[]{"test"});
		});
		assertNotNull(exception.getMessage());
		assertTrue(exception.getMessage().contains("Unsupported bean method"));
	}

	@Test void h02_hashCode_wrongSignature() throws Exception {
		// Test hashCode(int) - should not match the hashCode() check on line 74
		// Get the method from a helper class since we can't put it in an interface
		var method = HelperClassWithWrongSignatures.class.getMethod("hashCode", int.class);

		// Create handler with TestInterface (any interface will do for testing)
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		// This should fall through to getter/setter/unsupported logic since it doesn't match line 74
		// (has name "hashCode" but wrong parameter count - 1 instead of 0)
		var exception = assertThrows(UnsupportedOperationException.class, () -> {
			handler.invoke(proxy, method, new Object[]{42});
		});
		assertNotNull(exception.getMessage());
		assertTrue(exception.getMessage().contains("Unsupported bean method"));
	}

	@Test void h03_toString_wrongSignature() throws Exception {
		// Test toString(String) - should not match the toString() check on line 77
		// Get the method from a helper class since we can't put it in an interface
		var method = HelperClassWithWrongSignatures.class.getMethod("toString", String.class);

		// Create handler with TestInterface (any interface will do for testing)
		var bm = bc.getBeanMeta(TestInterface.class);
		var handler = new BeanProxyInvocationHandler<>(bm);
		var proxy = createProxy(TestInterface.class, handler);

		// This should fall through to getter/setter/unsupported logic since it doesn't match line 77
		// (has name "toString" but wrong parameter count - 1 instead of 0)
		var exception = assertThrows(UnsupportedOperationException.class, () -> {
			handler.invoke(proxy, method, new Object[]{"format"});
		});
		assertNotNull(exception.getMessage());
		assertTrue(exception.getMessage().contains("Unsupported bean method"));
	}

	//====================================================================================================
	// Helper methods
	//====================================================================================================

	private static <T> T createProxy(Class<T> interfaceClass, InvocationHandler handler) {
		return (T)Proxy.newProxyInstance(
			interfaceClass.getClassLoader(),
			new Class[]{interfaceClass},
			handler
		);
	}
}

