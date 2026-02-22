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
 * distributed under the License is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.junit.bct;

import static org.apache.juneau.commons.lang.TriState.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.junit.bct.BctConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.junit.bct.annotations.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link BctConfig} annotation and extension.
 */
@SuppressWarnings({
	"java:S4144", // Identical methods intentional for different test scenarios
	"java:S1172", // Unused parameters kept for API consistency or framework requirements
})
class BctConfig_Test extends TestBase {

	// ====================================================================================================
	// Class-level annotation tests
	// ====================================================================================================

	@Nested
	@BctConfig(sortMaps = TRUE)
	class A_classLevelAnnotation extends TestBase {

		@Test
		void a01_sortMapsEnabled() {
			// Verify sortMaps is enabled from class-level annotation
			assertTrue(BctConfiguration.get(BCT_SORT_MAPS, false));
		}

		@Test
		void a02_sortCollectionsNotEnabled() {
			// Verify sortCollections is not enabled (default UNSET)
			assertFalse(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}
	}

	@Nested
	@BctConfig(sortMaps = TRUE, sortCollections = TRUE)
	class B_classLevelBothEnabled extends TestBase {

		@Test
		void b01_bothEnabled() {
			// Verify both are enabled from class-level annotation
			assertTrue(BctConfiguration.get(BCT_SORT_MAPS, false));
			assertTrue(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}
	}

	// ====================================================================================================
	// Method-level annotation tests
	// ====================================================================================================

	@Nested
	class C_methodLevelAnnotation extends TestBase {

		@Test
		@BctConfig(sortMaps = TRUE)
		void c01_sortMapsEnabled() {
			// Verify sortMaps is enabled from method-level annotation
			assertTrue(BctConfiguration.get(BCT_SORT_MAPS, false));
		}

		@Test
		@BctConfig(sortCollections = TRUE)
		void c02_sortCollectionsEnabled() {
			// Verify sortCollections is enabled from method-level annotation
			assertTrue(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}

		@Test
		void c03_noAnnotation() {
			// Verify no settings are enabled when no annotation is present
			assertFalse(BctConfiguration.get(BCT_SORT_MAPS, false));
			assertFalse(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}
	}

	// ====================================================================================================
	// Method-level overriding class-level tests
	// ====================================================================================================

	@Nested
	@BctConfig(sortMaps = TRUE, sortCollections = TRUE)
	class D_methodOverridesClass extends TestBase {

		@Test
		@BctConfig(sortMaps = FALSE)
		void d01_methodOverridesSortMaps() {
			// Method-level should override class-level for sortMaps
			assertFalse(BctConfiguration.get(BCT_SORT_MAPS, false));
			// sortCollections should still be true from class-level (UNSET in method inherits from class)
			assertTrue(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}

		@Test
		@BctConfig(sortCollections = FALSE)
		void d02_methodOverridesSortCollections() {
			// sortMaps should still be true from class-level (UNSET in method inherits from class)
			assertTrue(BctConfiguration.get(BCT_SORT_MAPS, false));
			// Method-level should override class-level for sortCollections
			assertFalse(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}

		@Test
		@BctConfig(sortMaps = FALSE, sortCollections = FALSE)
		void d03_methodOverridesBoth() {
			// Method-level should override both from class-level
			assertFalse(BctConfiguration.get(BCT_SORT_MAPS, false));
			assertFalse(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}

		@Test
		void d04_inheritsFromClass() {
			// No method annotation, should inherit from class
			assertTrue(BctConfiguration.get(BCT_SORT_MAPS, false));
			assertTrue(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}
	}

	// ====================================================================================================
	// BeanConverter annotation tests
	// ====================================================================================================

	/**
	 * Custom converter for testing.
	 */
	static class TestBeanConverter extends BasicBeanConverter {
		public TestBeanConverter() {
			super(BasicBeanConverter.builder().defaultSettings());
		}
	}

	@Nested
	class E_beanConverterAnnotation extends TestBase {

		@Test
		@BctConfig(beanConverter = TestBeanConverter.class)
		void e01_customConverterSet() {
			// Verify custom converter is set by using it in an assertion
			// The converter should be used by assertBean
			var person = new TestPerson("Alice", 25);
			assertDoesNotThrow(() -> assertBean(person, "name", "Alice"));
			// Verify it's actually the custom converter by checking the class
			var converter = BctConfiguration.getConverter();
			assertNotNull(converter);
			assertEquals(TestBeanConverter.class, converter.getClass());
		}

		@Test
		void e02_defaultConverter() {
			// Verify default converter is used when no annotation
			var converter = BctConfiguration.getConverter();
			assertNotNull(converter);
			assertEquals(BasicBeanConverter.class, converter.getClass());
		}
	}

	@Nested
	@BctConfig(beanConverter = TestBeanConverter.class)
	class F_classLevelBeanConverter extends TestBase {

		@Test
		void f01_inheritsConverterFromClass() {
			// Verify converter is inherited from class-level annotation
			var converter = BctConfiguration.getConverter();
			assertNotNull(converter);
			assertEquals(TestBeanConverter.class, converter.getClass());
		}

		@Test
		@BctConfig(beanConverter = BeanConverter.class)
		void f02_methodFallsBackToClassConverter() {
			// Method-level with BeanConverter.class (default) falls back to class-level converter
			var converter = BctConfiguration.getConverter();
			assertNotNull(converter);
			assertEquals(TestBeanConverter.class, converter.getClass());
		}

		@Test
		@BctConfig(beanConverter = BasicBeanConverter.class)
		void f03_methodExplicitlyUsesDefaultConverter() {
			// Method-level with BasicBeanConverter.class explicitly uses default converter,
			// overriding class-level TestBeanConverter
			var converter = BctConfiguration.getConverter();
			assertNotNull(converter);
			assertEquals(BasicBeanConverter.class, converter.getClass());
		}
	}

	// ====================================================================================================
	// Combined settings tests
	// ====================================================================================================

	@Nested
	@BctConfig(sortMaps = TRUE, beanConverter = TestBeanConverter.class)
	class G_combinedSettings extends TestBase {

		@Test
		void g01_bothSettingsApplied() {
			// Verify both sortMaps and converter are set
			assertTrue(BctConfiguration.get(BCT_SORT_MAPS, false));
			var converter = BctConfiguration.getConverter();
			assertEquals(TestBeanConverter.class, converter.getClass());
		}

		@Test
		@BctConfig(sortCollections = TRUE)
		void g02_methodAddsToClass() {
			// Method adds sortCollections, inherits sortMaps and converter from class
			assertTrue(BctConfiguration.get(BCT_SORT_MAPS, false));
			assertTrue(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
			var converter = BctConfiguration.getConverter();
			assertEquals(TestBeanConverter.class, converter.getClass());
		}
	}

	// ====================================================================================================
	// Clearing after test tests
	// ====================================================================================================

	@Nested
	class H_clearingAfterTest extends TestBase {

		@Test
		@BctConfig(sortMaps = TRUE, sortCollections = TRUE)
		void h01_settingsClearedAfterTest() {
			// Settings should be active during test
			assertTrue(BctConfiguration.get(BCT_SORT_MAPS, false));
			assertTrue(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}

		@AfterEach
		void verifyCleared() {
			// After test, settings should be cleared
			// Note: This runs after the extension's afterEach, so we need to check in the next test
		}

		@Test
		void h02_verifyPreviousTestCleared() {
			// This test runs after h01, so settings should be cleared
			assertFalse(BctConfiguration.get(BCT_SORT_MAPS, false));
			assertFalse(BctConfiguration.get(BCT_SORT_COLLECTIONS, false));
		}
	}

	// ====================================================================================================
	// Error handling tests
	// ====================================================================================================

	@Nested
	class I_errorHandling extends TestBase {

		/**
		 * Converter without no-arg constructor (should fail).
		 */
		static class InvalidConverter extends BasicBeanConverter {
			public InvalidConverter(String arg) {
				super(BasicBeanConverter.builder().defaultSettings());
			}
		}
	}

	// ====================================================================================================
	// Test helper classes
	// ====================================================================================================

	/**
	 * Test person class for testing.
	 */
	static class TestPerson {
		private final String name;
		private final int age;

		TestPerson(String name, int age) {
			this.name = name;
			this.age = age;
		}

		String getName() { return name; }

		int getAge() { return age; }

		@Override
		public String toString() {
			return "TestPerson{name='" + name + "', age=" + age + "}";
		}
	}
}

