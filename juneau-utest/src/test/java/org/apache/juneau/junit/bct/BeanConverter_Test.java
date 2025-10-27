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

import static org.apache.juneau.junit.bct.BctUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the {@link BeanConverter} interface and its contract compliance.
 *
 * <p>This test class verifies interface implementations, contract compliance,
 * and edge case handling for all BeanConverter implementations.</p>
 */
class BeanConverter_Test extends TestBase {

	// ====================================================================================================
	// Contract Compliance Tests
	// ====================================================================================================

	@Nested
	class A_contractCompliance extends TestBase {

		@Test
		void a01_basicContractVerification() {
			var converter = BasicBeanConverter.DEFAULT;

			// Verify basic contract requirements
			assertNotNull(converter, "Default converter should not be null");
			assertNotNull(converter.stringify(null), "stringify(null) should return non-null");
			assertThrows(IllegalArgumentException.class, () -> converter.listify(null), "listify(null) should throw IllegalArgumentException");
			assertNotNull(converter.getNested(new Object(), tokenize("toString").get(0)), "getNested should return non-null");
		}

		@Test
		void a02_stringifyContract() {
			var converter = BasicBeanConverter.DEFAULT;

			// Test basic stringify contract
			assertEquals("<null>", converter.stringify(null));
			assertEquals("test", converter.stringify("test"));
			assertEquals("42", converter.stringify(42));
			assertEquals("true", converter.stringify(true));

			// Test collection stringify
			var list = Arrays.asList("a", "b", "c");
			var result = converter.stringify(list);
			assertTrue(result.contains("a") && result.contains("b") && result.contains("c"));
		}

		@Test
		void a03_listifyContract() {
			var converter = BasicBeanConverter.DEFAULT;

			// Test basic listify contract
			assertThrows(IllegalArgumentException.class, () -> converter.listify(null));
			assertThrows(IllegalArgumentException.class, () -> converter.listify("single")); // Strings are not listifiable by default

			// Test collection listify
			var input = Arrays.asList("a", "b", "c");
			var result = converter.listify(input);
			assertEquals(3, result.size());
			assertEquals("a", result.get(0));
			assertEquals("b", result.get(1));
			assertEquals("c", result.get(2));

			// Test array listify
			String[] array = {"x", "y", "z"};
			var arrayResult = converter.listify(array);
			assertEquals(3, arrayResult.size());
			assertEquals("x", arrayResult.get(0));
		}

		@Test
		void a04_getNestedContract() {
			var converter = BasicBeanConverter.DEFAULT;

			// Test basic property access
			var bean = new TestBean("test", 42);
			assertEquals("test", converter.getNested(bean, tokenize("name").get(0)));
			assertEquals("42", converter.getNested(bean, tokenize("value").get(0)));

			// Test toString access
			assertEquals(bean.toString(), converter.getNested(bean, tokenize("toString").get(0)));

			// Test class access
			assertEquals("{"+bean.getClass().getSimpleName()+"}", converter.getNested(bean, tokenize("class{simpleName}").get(0)));
		}
	}

	// ====================================================================================================
	// Edge Case Tests
	// ====================================================================================================

	@Nested
	class B_edgeCases extends TestBase {

		@Test
		void b01_nullInputHandling() {
			var converter = BasicBeanConverter.DEFAULT;

			// All methods should handle null gracefully - except listify
			assertNotNull(converter.stringify(null));
			assertThrows(IllegalArgumentException.class, () -> converter.listify(null));
			assertFalse(converter.canListify(null));
			assertNotNull(converter.getNested(null, tokenize("anyProperty").get(0)));

			// Null token should throw IllegalArgumentException
			assertThrows(IllegalArgumentException.class, () -> converter.getNested(new Object(), null));
		}

		@Test
		void b02_emptyInputHandling() {
			var converter = BasicBeanConverter.DEFAULT;

			// Empty collections
			assertEquals("[]", converter.stringify(new ArrayList<>()));
			assertEquals(0, converter.listify(new ArrayList<>()).size());

			// Empty strings
			assertEquals("", converter.stringify(""));
			assertThrows(IllegalArgumentException.class, () -> converter.listify("")); // Strings are not listifiable by default

			// Empty arrays
			assertEquals("[]", converter.stringify(new Object[0]));
			assertEquals(0, converter.listify(new Object[0]).size());
		}

		@Test
		void b03_largeInputHandling() {
			var converter = BasicBeanConverter.DEFAULT;

			// Large collection
			var largeList = new ArrayList<Integer>();
			for (int i = 0; i < 1000; i++) {
				largeList.add(i);
			}

			var stringResult = converter.stringify(largeList);
			assertNotNull(stringResult);
			assertTrue(stringResult.length() > 100);

			var listResult = converter.listify(largeList);
			assertEquals(1000, listResult.size());
		}

		@Test
		void b04_specialCharacterHandling() {
			var converter = BasicBeanConverter.DEFAULT;

			// Unicode characters
			var unicode = "测试 🎉 ñoël";
			assertEquals(unicode, converter.stringify(unicode));

			// Special control characters
			String special = "tab\there\nnewline\rcarriage";
			String result = converter.stringify(special);
			assertNotNull(result);

			// Null character
			String nullChar = "before\u0000after";
			assertNotNull(converter.stringify(nullChar));
		}

		@Test
		void b05_circularReferenceHandling() {
			var converter = BasicBeanConverter.DEFAULT;

			// Create circular reference
			var parent = new CircularTestBean("parent");
			var child = new CircularTestBean("child");
			parent.child = child;
			child.parent = parent;

			// Should not cause stack overflow
			var result = converter.stringify(parent);
			assertNotNull(result);

			// Should handle nested access on circular objects
			assertEquals("{child}", converter.getNested(parent, tokenize("child{name}").get(0)));
		}
	}

	// ====================================================================================================
	// Implementation Verification Tests
	// ====================================================================================================

	@Nested
	class C_implementationVerification extends TestBase {

		@Test
		void c01_builderPatternVerification() {
			// Verify builder creates valid converter
			var custom = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("nullValue", "<empty>")
				.build();

			assertNotNull(custom);
			assertEquals("<empty>", custom.stringify(null));
		}

		@Test
		void c02_defaultConverterImmutability() {
			var defaultConverter = BasicBeanConverter.DEFAULT;

			// Multiple calls should return same instance
			var another = BasicBeanConverter.DEFAULT;
			assertSame(defaultConverter, another);
		}

		@Test
		void c03_customSettingsPersistence() {
			var custom = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("nullValue", "CUSTOM_NULL")
				.addSetting("selfValue", "CUSTOM_SELF")
				.build();

			assertEquals("CUSTOM_NULL", custom.stringify(null));

			// Settings should persist across calls
			assertEquals("CUSTOM_NULL", custom.stringify(null));
			assertEquals("CUSTOM_NULL", custom.stringify(null));
		}

		@Test
		void c04_extensibilityVerification() {
			// Verify custom stringifiers work
			var custom = BasicBeanConverter.builder()
				.defaultSettings()
				.addStringifier(TestBean.class, (converter, bean) -> "CUSTOM:" + bean.name)
				.build();

			var bean = new TestBean("test", 42);
			assertEquals("CUSTOM:test", custom.stringify(bean));
		}
	}

	// ====================================================================================================
	// Error Handling Tests
	// ====================================================================================================

	@Nested
	class D_errorHandling extends TestBase {

		@Test
		void d01_invalidPropertyAccess_throwsPropertyNotFoundException() {
			var converter = BasicBeanConverter.DEFAULT;
			var bean = new TestBean("test", 42);

			// Non-existent property should throw PropertyNotFoundException with descriptive message
			var ex = assertThrows(PropertyNotFoundException.class, () ->
				converter.getNested(bean, tokenize("nonExistentProperty").get(0)));

			// Verify the exception message contains useful information
			assertTrue(ex.getMessage().contains("nonExistentProperty"));
			assertTrue(ex.getMessage().contains("TestBean"));
		}

		@Test
		void d02_exceptionInPropertyAccess() {
			var converter = BasicBeanConverter.DEFAULT;
			var bean = new ExceptionThrowingBean();

			// Exception in getter should propagate as RuntimeException (wrapped InvocationTargetException)
			assertThrows(RuntimeException.class, () ->
				converter.getNested(bean, tokenize("throwingProperty").get(0)));
		}

		@Test
		void d03_malformedPropertyPath() {
			var converter = BasicBeanConverter.DEFAULT;
			var bean = new TestBean("test", 42);

			// Invalid property should throw PropertyNotFoundException
			assertThrows(PropertyNotFoundException.class, () -> converter.getNested(bean, tokenize("invalidProperty").get(0)));
			// Test null token handling
			assertThrows(IllegalArgumentException.class, () -> converter.getNested(bean, null));
		}

		@Test
		void d04_typeConversionErrors() {
			var converter = BasicBeanConverter.DEFAULT;

			// Objects that can't be easily converted should still work
			var problematic = new Object() {
				@Override
				public String toString() {
					throw new RuntimeException("Cannot convert to string");
				}
			};

			// Should handle toString exceptions gracefully using safeToString
			String result = converter.stringify(problematic);
			assertNotNull(result);
			assertTrue(result.contains("RuntimeException"));
			assertTrue(result.contains("Cannot convert to string"));
		}
	}

	// ====================================================================================================
	// Helper Classes
	// ====================================================================================================

	static class TestBean {
		final String name;
		final int value;

		TestBean(String name, int value) {
			this.name = name;
			this.value = value;
		}

		String getName() { return name; }
		int getValue() { return value; }

		@Override
		public String toString() {
			return "TestBean(name=" + name + ", value=" + value + ")";
		}
	}

	static class CircularTestBean {
		final String name;
		CircularTestBean parent;
		CircularTestBean child;

		CircularTestBean(String name) {
			this.name = name;
		}

		String getName() { return name; }
		CircularTestBean getParent() { return parent; }
		CircularTestBean getChild() { return child; }
	}

	static class ExceptionThrowingBean {
		public String getThrowingProperty() {
			throw new RuntimeException("Intentional test exception");
		}
	}
}