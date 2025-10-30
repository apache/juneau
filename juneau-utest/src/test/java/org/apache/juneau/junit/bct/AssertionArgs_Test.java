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
package org.apache.juneau.junit.bct;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.text.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link AssertionArgs}.
 *
 * <p>Tests the configuration and behavior of the assertion arguments class including
 * bean converter customization, error message composition, and fluent API functionality.</p>
 */
class AssertionArgs_Test extends TestBase {

	// Test objects for assertions
	static class TestBean {
		private String name;
		private int age;
		private boolean active;

		public TestBean(String name, int age, boolean active) {
			this.name = name;
			this.age = age;
			this.active = active;
		}

		public String getName() { return name; }
		public int getAge() { return age; }
		public boolean isActive() { return active; }
	}

	static class CustomObject {
		private String value;

		public CustomObject(String value) {
			this.value = value;
		}

		public String getValue() { return value; }

		@Override
		public String toString() {
			return "CustomObject[" + value + "]";
		}
	}

	@Test
	void a01_defaultConstruction() {
		var args = new AssertionArgs();

		// Should have no custom converter
		assertEmpty(args.getBeanConverter());

		// Should have no custom message
		assertNull(args.getMessage());
	}

	@Test
	void a02_fluentAPIReturnsThis() {
		var args = new AssertionArgs();
		var mockConverter = createMockConverter();

		// Fluent methods should return the same instance
		assertSame(args, args.setBeanConverter(mockConverter));
		assertSame(args, args.setMessage("test message"));
		assertSame(args, args.setMessage(() -> "dynamic message"));
	}

	@Test
	void b01_beanConverterConfiguration() {
		var args = new AssertionArgs();
		var mockConverter = createMockConverter();

		// Initially empty
		assertEmpty(args.getBeanConverter());

		// Set converter
		args.setBeanConverter(mockConverter);
		assertTrue(args.getBeanConverter().isPresent());
		assertSame(mockConverter, args.getBeanConverter().get());

		// Set to null should clear
		args.setBeanConverter(null);
		assertEmpty(args.getBeanConverter());
	}

	@Test
	void b02_customConverterInAssertion() {
		// Create a mock custom converter for testing
		var customConverter = createCustomConverter();

		var args = args().setBeanConverter(customConverter);
		var obj = new TestBeanWithCustomObject("test", new CustomObject("value"));

		// Should use custom converter for stringification
		assertBean(args, obj, "custom", "CUSTOM:value");
	}

	static class TestBeanWithCustomObject {
		private String name;
		private CustomObject custom;

		public TestBeanWithCustomObject(String name, CustomObject custom) {
			this.name = name;
			this.custom = custom;
		}

		public String getName() { return name; }
		public CustomObject getCustom() { return custom; }
	}

	@Test
	void c01_messageSupplierConfiguration() {
		var args = new AssertionArgs();

		// Initially null
		assertNull(args.getMessage());

		// Set supplier
		Supplier<String> supplier = () -> "test message";
		args.setMessage(supplier);
		assertNotNull(args.getMessage());
		assertEquals("test message", args.getMessage().get());

		// Set different supplier
		args.setMessage(() -> "different message");
		assertEquals("different message", args.getMessage().get());
	}

	@Test
	void c02_parameterizedMessageConfiguration() {
		var args = new AssertionArgs();

		// Simple parameter substitution
		args.setMessage("Hello {0}", "World");
		assertEquals("Hello World", args.getMessage().get());

		// Multiple parameters
		args.setMessage("User {0} has {1} points", "John", 100);
		assertEquals("User John has 100 points", args.getMessage().get());

		// Number formatting
		args.setMessage("Value: {0,number,#.##}", 123.456);
		assertEquals("Value: 123.46", args.getMessage().get());
	}

	@Test
	void c03_dynamicMessageSupplier() {
		var counter = new int[1]; // Mutable counter for testing
		var args = new AssertionArgs();

		args.setMessage(() -> "Call #" + (++counter[0]));

		// Each call should increment the counter
		assertEquals("Call #1", args.getMessage().get());
		assertEquals("Call #2", args.getMessage().get());
		assertEquals("Call #3", args.getMessage().get());
	}

	@Test
	void d01_messageCompositionWithoutCustomMessage() {
		var args = new AssertionArgs();

		// No custom message, should return assertion message as-is
		var composedMessage = args.getMessage("Bean assertion failed");
		assertEquals("Bean assertion failed", composedMessage.get());

		// With parameters
		var composedWithParams = args.getMessage("Element at index {0} did not match", 5);
		assertEquals("Element at index 5 did not match", composedWithParams.get());
	}

	@Test
	void d02_messageCompositionWithCustomMessage() {
		var args = new AssertionArgs();
		args.setMessage("User validation failed");

		// Should compose: custom + assertion
		var composedMessage = args.getMessage("Bean assertion failed");
		assertEquals("User validation failed, Caused by: Bean assertion failed", composedMessage.get());

		// With parameters in assertion message
		var composedWithParams = args.getMessage("Element at index {0} did not match", 3);
		assertEquals("User validation failed, Caused by: Element at index 3 did not match", composedWithParams.get());
	}

	@Test
	void d03_messageCompositionWithParameterizedCustomMessage() {
		var args = new AssertionArgs();
		args.setMessage("Test {0} failed on iteration {1}", "UserValidation", 42);

		var composedMessage = args.getMessage("Bean assertion failed");
		assertEquals("Test UserValidation failed on iteration 42, Caused by: Bean assertion failed", composedMessage.get());
	}

	@Test
	void d04_messageCompositionWithDynamicCustomMessage() {
		var timestamp = Instant.now().toString();
		var args = new AssertionArgs();
		args.setMessage(() -> "Test failed at " + timestamp);

		var composedMessage = args.getMessage("Bean assertion failed");
		assertEquals("Test failed at " + timestamp + ", Caused by: Bean assertion failed", composedMessage.get());
	}

	@Test
	void e01_fluentConfigurationChaining() {
		var converter = createMockConverter();

		// Chain multiple configurations
		var args = new AssertionArgs()
			.setBeanConverter(converter)
			.setMessage("Integration test failed for module {0}", "AuthModule");

		// Verify both configurations applied
		assertTrue(args.getBeanConverter().isPresent());
		assertSame(converter, args.getBeanConverter().get());
		assertEquals("Integration test failed for module AuthModule", args.getMessage().get());
	}

	@Test
	void e02_configurationOverwriting() {
		var args = new AssertionArgs();
		var converter1 = createMockConverter();
		var converter2 = createMockConverter();

		// Set initial values
		args.setBeanConverter(converter1).setMessage("First message");

		// Overwrite with new values
		args.setBeanConverter(converter2).setMessage("Second message");

		// Should have latest values
		assertSame(converter2, args.getBeanConverter().get());
		assertEquals("Second message", args.getMessage().get());
	}

	@Test
	void f01_integrationWithAssertBean() {
		var bean = new TestBean("John", 30, true);
		var args = args().setMessage("User test failed");

		// Should work with custom message
		assertBean(args, bean, "name,age,active", "John,30,true");

		// Test assertion failure message composition
		var exception = assertThrows(AssertionError.class, () -> {
			assertBean(args, bean, "name", "Jane");
		});

		assertTrue(exception.getMessage().contains("User test failed"));
		assertTrue(exception.getMessage().contains("Caused by:"));
	}

	@Test
	void f02_integrationWithAssertBeans() {
		var beans = l(
			new TestBean("Alice", 25, true),
			new TestBean("Bob", 35, false)
		);
		var args = args().setMessage("Batch validation failed");

		// Should work with custom message
		assertBeans(args, beans, "name,age", "Alice,25", "Bob,35");

		// Test assertion failure message composition
		var exception = assertThrows(AssertionError.class, () -> {
			assertBeans(args, beans, "name", "Charlie", "David");
		});

		assertTrue(exception.getMessage().contains("Batch validation failed"));
		assertTrue(exception.getMessage().contains("Caused by:"));
	}

	@Test
	void f03_integrationWithAssertList() {
		var list = l("apple", "banana", "cherry");
		var args = args().setMessage("List validation failed");

		// Should work with custom message
		assertList(args, list, "apple", "banana", "cherry");

		// Test assertion failure message composition
		var exception = assertThrows(AssertionError.class, () -> {
			assertList(args, list, "orange", "banana", "cherry");
		});

		assertTrue(exception.getMessage().contains("List validation failed"));
		assertTrue(exception.getMessage().contains("Caused by:"));
	}

	@Test
	void g01_edgeCaseNullValues() {
		var args = new AssertionArgs();

		// Null converter should work
		args.setBeanConverter(null);
		assertEmpty(args.getBeanConverter());

		// Null message supplier should work
		args.setMessage((Supplier<String>) null);
		assertNull(args.getMessage());
	}

	@Test
	void g02_edgeCaseEmptyMessages() {
		var args = new AssertionArgs();

		// Empty string message
		args.setMessage("");
		assertEquals("", args.getMessage().get());

		// Empty supplier result
		args.setMessage(() -> "");
		assertEquals("", args.getMessage().get());

		// Composition with empty custom message
		var composedMessage = args.getMessage("Bean assertion failed");
		assertEquals(", Caused by: Bean assertion failed", composedMessage.get());
	}

	@Test
	void g03_edgeCaseComplexParameterFormatting() {
		var args = new AssertionArgs();
		var date = new Date();

		// Date formatting
		args.setMessage("Test executed on {0,date,short}", date);
		var expectedDatePart = DateFormat.getDateInstance(DateFormat.SHORT).format(date);
		assertTrue(args.getMessage().get().contains(expectedDatePart));

		// Complex number formatting
		args.setMessage("Processing {0,number,percent} complete", 0.85);
		assertTrue(args.getMessage().get().contains("85%"));
	}

	@Test
	void h01_threadSafetyDocumentationCompliance() {
		// This test documents that AssertionArgs is NOT thread-safe
		// Each thread should create its own instance

		var sharedArgs = new AssertionArgs();
		var results = Collections.synchronizedList(new ArrayList<String>());

		// Simulate multiple threads modifying the same instance
		var threads = new Thread[5];
		for (int i = 0; i < threads.length; i++) {
			final int threadId = i;
			threads[i] = new Thread(() -> {
				sharedArgs.setMessage("Thread " + threadId + " message");
				// Small delay to increase chance of race condition
				try { Thread.sleep(1); } catch (InterruptedException e) {}
				results.add(sharedArgs.getMessage().get());
			});
		}

		// Start all threads
		for (var thread : threads) {
			thread.start();
		}

		// Wait for completion
		for (var thread : threads) {
			try { thread.join(); } catch (InterruptedException e) {}
		}

		// Due to race conditions, we may not get the expected messages
		// This demonstrates why each test should create its own instance
		assertSize(5, results);
		// Note: We don't assert specific values due to race conditions
	}

	@Test
	void h02_recommendedUsagePattern() {
		// Demonstrate the recommended pattern: create new instance per test

		// Test 1: User validation
		var userArgs = args().setMessage("User validation test");
		var user = new TestBean("Alice", 25, true);
		assertBean(userArgs, user, "name,active", "Alice,true");

		// Test 2: Product validation (separate instance)
		var productArgs = args().setMessage("Product validation test");
		var products = l("Laptop", "Phone", "Tablet");
		assertList(productArgs, products, "Laptop", "Phone", "Tablet");

		// Each test has its own configuration without interference
		assertEquals("User validation test", userArgs.getMessage().get());
		assertEquals("Product validation test", productArgs.getMessage().get());
	}

	// Helper method to create a mock converter for testing
	private static BeanConverter createMockConverter() {
		return new BeanConverter() {
			@Override
			public String stringify(Object o) {
				return String.valueOf(o);
			}

		@Override
		public List<Object> listify(Object o) {
			if (o instanceof List) return (List<Object>) o;
			return l(o);
		}

			@Override
			public boolean canListify(Object o) {
				return true;
			}

			@Override
			public Object swap(Object o) {
				return o;
			}

			@Override
			public Object getProperty(Object object, String name) {
				// Simple mock implementation
				if ("name".equals(name) && object instanceof TestBean) {
					return ((TestBean) object).getName();
				}
				if ("custom".equals(name) && object instanceof TestBeanWithCustomObject) {
					return ((TestBeanWithCustomObject) object).getCustom();
				}
				return null;
			}

			@Override
			public <T> T getSetting(String key, T defaultValue) {
				return defaultValue;
			}

			@Override
			public String getNested(Object o, NestedTokenizer.Token token) {
				var propValue = getProperty(o, token.getValue());
				return stringify(propValue);
			}
		};
	}

	// Helper method to create a custom converter for testing
	private static BeanConverter createCustomConverter() {
		return new BeanConverter() {
			@Override
			public String stringify(Object o) {
				if (o instanceof CustomObject) {
					return "CUSTOM:" + ((CustomObject) o).getValue();
				}
				return String.valueOf(o);
			}

		@Override
		public List<Object> listify(Object o) {
			if (o instanceof List) return (List<Object>) o;
			return l(o);
		}

			@Override
			public boolean canListify(Object o) {
				return true;
			}

			@Override
			public Object swap(Object o) {
				return o;
			}

			@Override
			public Object getProperty(Object object, String name) {
				if ("custom".equals(name) && object instanceof TestBeanWithCustomObject) {
					return ((TestBeanWithCustomObject) object).getCustom();
				}
				return null;
			}

			@Override
			public <T> T getSetting(String key, T defaultValue) {
				return defaultValue;
			}

			@Override
			public String getNested(Object o, NestedTokenizer.Token token) {
				var propValue = getProperty(o, token.getValue());
				return stringify(propValue);
			}
		};
	}
}