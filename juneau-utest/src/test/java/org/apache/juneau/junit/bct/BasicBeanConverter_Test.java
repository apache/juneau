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
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.junit.bct.BasicBeanConverter.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.junit.bct.BctUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for BasicBeanConverter.
 */
@DisplayName("BasicBeanConverter")
class BasicBeanConverter_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Builder Tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Builder")
	class A_builderTest extends TestBase {

		@Test
		@DisplayName("a01_builder() creates new Builder instance")
		void a01_builder_createsNewInstance() {
			var builder = BasicBeanConverter.builder();
			assertNotNull(builder);
			assertInstanceOf(BasicBeanConverter.Builder.class, builder);
		}

		@Test
		@DisplayName("a02_build() creates BasicBeanConverter instance")
		void a02_build_createsBasicBeanConverter() {
			var converter = BasicBeanConverter.builder().build();
			assertNotNull(converter);
			assertInstanceOf(BasicBeanConverter.class, converter);
		}

		@Test
		@DisplayName("a03_defaultSettings() applies default configuration")
		void a03_defaultSettings_appliesDefaults() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();

			// Test that default stringifiers work
			assertEquals("test", converter.stringify("test"));
			assertEquals("123", converter.stringify(123));
			assertEquals("true", converter.stringify(true));
			assertEquals("[1,2,3]", converter.stringify(l(1, 2, 3)));

			// Test that default settings are applied
			assertEquals("<null>", converter.stringify(null));
		}

		@Test
		@DisplayName("a04_addStringifier() adds custom stringifier")
		void a04_addStringifier_addsCustomStringifier() {
			var converter = BasicBeanConverter.builder().defaultSettings().addStringifier(LocalDate.class, (conv, date) -> date.format(DateTimeFormatter.ISO_LOCAL_DATE)).build();

			var date = LocalDate.of(2023, 12, 25);
			assertEquals("2023-12-25", converter.stringify(date));
		}

		@Test
		@DisplayName("a05_addListifier() adds custom listifier")
		void a05_addListifier_addsCustomListifier() {
			var converter = BasicBeanConverter.builder().defaultSettings().addListifier(String.class, (conv, str) -> l((Object[])str.split(","))).build();

			var result = converter.listify("a,b,c");
			assertEquals(l("a", "b", "c"), result);
		}

		@Test
		@DisplayName("a06_addSwapper() adds custom swapper")
		void a06_addSwapper_addsCustomSwapper() {
			var converter = BasicBeanConverter.builder().defaultSettings().addSwapper(Optional.class, (conv, opt) -> ((Optional<?>)opt).orElse(null)).build();

			assertEquals("test", converter.stringify(opt("test")));
			assertEquals("<null>", converter.stringify(opte()));
		}

		@Test
		@DisplayName("a07_addPropertyExtractor() adds custom property extractor")
		void a07_addPropertyExtractor_addsCustomExtractor() {
			var extractor = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String name) {
					return o instanceof TestBean && "custom".equals(name);
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String name) {
					return "custom value";
				}
			};

			var converter = BasicBeanConverter.builder().defaultSettings().addPropertyExtractor(extractor).build();

			var bean = new TestBean("John", 30);
			assertEquals("custom value", converter.getProperty(bean, "custom"));
		}

		@Test
		@DisplayName("a08_addSetting() adds custom setting")
		void a08_addSetting_addsCustomSetting() {
			var converter = BasicBeanConverter.builder().defaultSettings().addSetting(SETTING_nullValue, "<null>").build();

			assertEquals("<null>", converter.stringify(null));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Core Functionality Tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Core Functionality")
	class B_coreFunctionalityTest extends TestBase {

		private BasicBeanConverter converter;

		@BeforeEach
		void setUp() {
			converter = BasicBeanConverter.builder().defaultSettings().build();
		}

		@Test
		@DisplayName("b01_stringify() handles null values")
		void b01_stringify_handlesNull() {
			assertEquals("<null>", converter.stringify(null));
		}

		@Test
		@DisplayName("b02_stringify() handles primitive types")
		void b02_stringify_handlesPrimitives() {
			assertEquals("123", converter.stringify(123));
			assertEquals("true", converter.stringify(true));
			assertEquals("3.14", converter.stringify(3.14));
			assertEquals("c", converter.stringify('c'));
		}

		@Test
		@DisplayName("b03_stringify() handles strings")
		void b03_stringify_handlesStrings() {
			assertEquals("hello", converter.stringify("hello"));
			assertEquals("", converter.stringify(""));
		}

		@Test
		@DisplayName("b04_stringify() handles collections")
		void b04_stringify_handlesCollections() {
			assertEquals("[1,2,3]", converter.stringify(l(1, 2, 3)));
			assertEquals("[]", converter.stringify(Collections.emptyList()));
			// Set converted to TreeSet for deterministic ordering
			var setResult = converter.stringify(Set.of("z", "a", "m"));
			assertEquals("[a,m,z]", setResult); // TreeSet ensures natural order
		}

		@Test
		@DisplayName("b05_stringify() handles maps")
		void b05_stringify_handlesMaps() {
			var map = m("name", "John", "age", 30);
			var result = converter.stringify(map);
			assertTrue(result.contains("name=John"));
			assertTrue(result.contains("age=30"));
			assertTrue(result.startsWith("{"));
			assertTrue(result.endsWith("}"));
		}

		@Test
		@DisplayName("b06_stringify() handles arrays")
		void b06_stringify_handlesArrays() {
			assertEquals("[1,2,3]", converter.stringify(ints(1, 2, 3)));
			assertEquals("[a,b,c]", converter.stringify(a("a", "b", "c")));
			assertEquals("[]", converter.stringify(new Object[0]));
		}

		@Test
		@DisplayName("b07_stringify() handles dates")
		void b07_stringify_handlesDates() {
			var date = Instant.parse("2023-12-25T10:15:30Z");
			var result = converter.stringify(date);
			assertEquals("2023-12-25T10:15:30Z", result);
		}

		@Test
		@DisplayName("b08_listify() converts arrays to lists")
		void b08_listify_convertsArrays() {
			var result = converter.listify(a("a", "b", "c"));
			assertEquals(l("a", "b", "c"), result);
		}

		@Test
		@DisplayName("b09_listify() handles collections")
		void b09_listify_handlesCollections() {
			var set = Set.of("z", "a", "m");
			var result = converter.listify(set);
			// TreeSet conversion ensures natural ordering
			assertList(result, "a", "m", "z");
		}

		@Test
		@DisplayName("b10_listify() throws IllegalArgumentException for null")
		void b10_listify_throwsForNull() {
			assertThrows(IllegalArgumentException.class, () -> converter.listify(null));
		}

		@Test
		@DisplayName("b11_canListify() returns correct values")
		void b11_canListify_returnsCorrectValues() {
			assertTrue(converter.canListify(l(1, 2, 3)));
			assertTrue(converter.canListify(ints(1, 2, 3)));
			assertTrue(converter.canListify(Set.of("a", "b")));
			assertFalse(converter.canListify("string"));
			assertFalse(converter.canListify(null));
		}

		@Test
		@DisplayName("b12_swap() applies swappers")
		void b12_swap_appliesSwappers() {
			// Test with Future swapper (should be in default settings)
			var future = CompletableFuture.completedFuture("test");
			var result = converter.swap(future);
			// Future swapper should extract the completed value
			assertEquals("test", result);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Property Access Tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Property Access")
	class C_propertyAccessTest extends TestBase {

		private BasicBeanConverter converter;

		@BeforeEach
		void setUp() {
			converter = BasicBeanConverter.builder().defaultSettings().build();
		}

		@Test
		@DisplayName("c01_getProperty() accesses bean properties")
		void c01_getProperty_accessesBeanProperties() {
			var bean = new TestBean("John", 30);

			assertEquals("John", converter.getProperty(bean, "name"));
			assertEquals(30, converter.getProperty(bean, "age"));
		}

		@Test
		@DisplayName("c02_getProperty() handles null objects")
		void c02_getProperty_handlesNull() {
			assertNull(converter.getProperty(null, "name"));
		}

		@Test
		@DisplayName("c03_getProperty() throws for unknown properties")
		void c03_getProperty_throwsForUnknownProperties() {
			var bean = new TestBean("John", 30);

			assertThrows(RuntimeException.class, () -> converter.getProperty(bean, "unknown"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Settings Tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Settings")
	class D_settingsTest extends TestBase {

		@Test
		@DisplayName("d01_nullValue setting changes null representation")
		void d01_nullValue_changesNullRepresentation() {
			var converter = BasicBeanConverter.builder().defaultSettings().addSetting(SETTING_nullValue, "<null>").build();

			assertEquals("<null>", converter.stringify(null));
		}

		@Test
		@DisplayName("d02_fieldSeparator setting changes delimiter")
		void d02_fieldSeparator_changesDelimiter() {
			var converter = BasicBeanConverter.builder().defaultSettings().addSetting(SETTING_fieldSeparator, " | ").build();

			assertEquals("[1 | 2 | 3]", converter.stringify(l(1, 2, 3)));
		}

		@Test
		@DisplayName("d03_collection prefix/suffix settings change brackets")
		void d03_collectionBrackets_changeBrackets() {
			var converter = BasicBeanConverter.builder().defaultSettings().addSetting(SETTING_collectionPrefix, "(").addSetting(SETTING_collectionSuffix, ")").build();

			assertEquals("(1,2,3)", converter.stringify(l(1, 2, 3)));
		}

		@Test
		@DisplayName("d04_map prefix/suffix settings change brackets")
		void d04_mapBrackets_changeBrackets() {
			var converter = BasicBeanConverter.builder().defaultSettings().addSetting(SETTING_mapPrefix, "<").addSetting(SETTING_mapSuffix, ">").build();

			var map = m("a", 1);
			var result = converter.stringify(map);
			assertTrue(result.startsWith("<"));
			assertTrue(result.endsWith(">"));
		}

		@Test
		@DisplayName("d05_mapEntrySeparator setting changes key-value separator")
		void d05_mapEntrySeparator_changesKeyValueSeparator() {
			var converter = BasicBeanConverter.builder().defaultSettings().addSetting(SETTING_mapEntrySeparator, ":").build();

			var map = m("name", "John");
			var result = converter.stringify(map);
			assertTrue(result.contains("name:John"));
		}

		@Test
		@DisplayName("d06_classNameFormat setting changes class name format")
		void d06_classNameFormat_changesFormat() {
			var converter = BasicBeanConverter.builder().defaultSettings().addSetting(SETTING_classNameFormat, "full").build();

			var bean = new TestBean("John", 30);
			var result = converter.stringify(bean);
			assertContains(getClass().getDeclaringClass().getName() + "$TestBean", result);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error Handling Tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Error Handling")
	class E_errorHandlingTest extends TestBase {

		private BasicBeanConverter converter;

		@BeforeEach
		void setUp() {
			converter = BasicBeanConverter.builder().defaultSettings().build();
		}

		@Test
		@DisplayName("e01_getProperty() with invalid property throws RuntimeException")
		void e01_getProperty_invalidProperty_throwsException() {
			var bean = new TestBean("John", 30);

			var ex = assertThrows(RuntimeException.class, () -> converter.getProperty(bean, "invalidProperty"));
			assertContains("Property 'invalidProperty' not found", ex.getMessage());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test Helper Classes
	//------------------------------------------------------------------------------------------------------------------

	public static class TestBean {
		private String name;
		private int age;

		public TestBean(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() { return name; }

		public int getAge() { return age; }

		void setName(String name) { this.name = name; }

		void setAge(int age) { this.age = age; }
	}

	public static class TestPerson {
		private String name;
		private int age;

		public TestPerson(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() { return name; }

		public int getAge() { return age; }

		void setName(String name) { this.name = name; }

		void setAge(int age) { this.age = age; }
	}

	// ====================================================================================================
	// Enhanced Edge Case Tests
	// ====================================================================================================

	@Nested
	class H_enhancedEdgeCases extends TestBase {

		@Test
		void h01_listifyWithMixedArrayTypes() {
			var converter = builder().defaultSettings().build();

			// Test different array types
			var stringArray = a("a", "b", "c");
			var intArray = ints(1, 2, 3);
			var booleanArray = booleans(true, false, true);

			var stringList = converter.listify(stringArray);
			assertSize(3, stringList);
			assertEquals("a", stringList.get(0));

			var intList = converter.listify(intArray);
			assertSize(3, intList);
			assertEquals(1, intList.get(0));

			var booleanList = converter.listify(booleanArray);
			assertSize(3, booleanList);
			assertEquals(true, booleanList.get(0));
		}

		@Test
		void h02_swapWithSingleRegistration() {
			var converter = builder().defaultSettings().addSwapper(TestPerson.class, (conv, person) -> "Person:" + person.getName()).build();

			var person = new TestPerson("john", 30);

			// Should apply swapper: Person -> String
			assertEquals("Person:john", converter.stringify(person));
		}

		@Test
		void h03_canListifyWithEdgeCases() {
			var converter = builder().defaultSettings().build();

			// Test various types that can/cannot be listified
			assertTrue(converter.canListify(l("a", "b")));
			assertTrue(converter.canListify(a("a", "b")));
			assertTrue(converter.canListify(Set.of("a", "b")));
			assertFalse(converter.canListify(null));
			assertFalse(converter.canListify("simple string"));
			assertFalse(converter.canListify(42));
			assertFalse(converter.canListify(new TestPerson("test", 25)));
		}

		@Test
		void h04_performanceWithLargeObjects() {
			var converter = builder().defaultSettings().build();

			// Test performance with larger objects
			var largeList = list();
			for (var i = 0; i < 1000; i++) {
				largeList.add("item_" + i);
			}

			var start = System.nanoTime();
			var result = converter.stringify(largeList);
			var end = System.nanoTime();

			assertNotNull(result);
			assertTrue(result.length() > 1000, "Should generate substantial output");

			var durationMs = (end - start) / 1_000_000;
			assertTrue(durationMs < 100, "Should complete quickly for 1000 items, took: " + durationMs + "ms");
		}

		@Test
		void h05_missingPropertyExtractorThrowsException() {
			// Test line 305: orElseThrow when no property extractor is found
			var converter = builder().build(); // No default extractors
			var obj = new TestPerson("John", 30);

			// Should throw RuntimeException when no extractor can handle the property
			var ex = assertThrows(RuntimeException.class, () -> converter.getProperty(obj, "name"));
			assertContains("Could not find extractor for object of type", ex.getMessage());
		}

		@Test
		void h06_iterationSyntaxWithCollections() {
			// Test lines 316-317: #{...} syntax for iterating over collections/arrays
			var converter = builder().defaultSettings().build();

			// Test with list of objects
			var people = l(m("name", "John", "age", 30), m("name", "Jane", "age", 25));

			assertEquals("[{John},{Jane}]", converter.getNested(people, tokenize("#{name}").get(0)));
			assertEquals("[{30},{25}]", converter.getNested(people, tokenize("#{age}").get(0)));
			assertEquals("[{John,30},{Jane,25}]", converter.getNested(people, tokenize("#{name,age}").get(0)));
		}

		@Test
		void h07_getNested_earlyReturnConditions() {
			// Test line 331: early return when e == null || !token.hasNested()
			var converter = builder().defaultSettings().build();
			var obj = new HashMap<String,Object>();
			obj.put("key", "value");
			obj.put("nullKey", null);

			// Case 1: e == null (property value is null) and no nested tokens
			assertEquals("<null>", converter.getNested(obj, tokenize("nullKey").get(0)));

			// Case 2: e != null but token has no nested content
			assertEquals("value", converter.getNested(obj, tokenize("key").get(0)));

			// Case 3: e == null and token has nested content (should still return early)
			assertEquals("<null>", converter.getNested(obj, tokenize("nullKey{nested}").get(0)));
		}

		@Test
		void h08_interfaceBasedStringifierLookup() {
			// Test lines 343-344: interface checking in findStringifier()

			// Create a custom interface and class to test interface lookup
			interface CustomStringifiable {
				String getCustomString();
			}

			class CustomObject implements CustomStringifiable {
				@Override
				public String getCustomString() { return "custom"; }
			}

			var converter = builder().defaultSettings().addStringifier(CustomStringifiable.class, (conv, obj) -> "CUSTOM:" + obj.getCustomString()).build();

			// CustomObject implements CustomStringifiable, so should find the interface-based stringifier
			assertEquals("CUSTOM:custom", converter.stringify(new CustomObject()));
		}

		@Test
		void h09_interfaceBasedListifierLookup() {
			// Test lines 357-358: interface checking in findListifier()

			// Create an interface hierarchy where we register for a deeper interface
			interface BaseInterface {
				String getBase();
			}

			interface MiddleInterface {
				String getMiddle();
			}

			// Class that implements multiple unrelated interfaces
			class MultiInterfaceClass implements BaseInterface, MiddleInterface {
				@Override
				public String getBase() { return "base"; }

				@Override
				public String getMiddle() { return "middle"; }
			}

			var converter = builder().defaultSettings()
				// Register listifier only for MiddleInterface, not BaseInterface or the class
				.addListifier(MiddleInterface.class, (conv, obj) -> l("FROM_MIDDLE_INTERFACE", obj.getMiddle())).build();

			// MultiInterfaceClass won't directly match, BaseInterface won't match,
			// but MiddleInterface will match during interface iteration
			var result = converter.listify(new MultiInterfaceClass());
			assertEquals("FROM_MIDDLE_INTERFACE", result.get(0));
			assertEquals("middle", result.get(1));
		}

		@Test
		void h10_interfaceBasedSwapperLookup() {
			// Test lines 371-372: interface checking in findSwapper()

			// Create multiple unrelated interfaces
			interface FirstInterface {
				String getFirst();
			}

			interface SecondInterface {
				String getSecond();
			}

			// Class that implements multiple unrelated interfaces
			class MultiInterfaceWrapper implements FirstInterface, SecondInterface {
				@Override
				public String getFirst() { return "first"; }

				@Override
				public String getSecond() { return "second"; }
			}

			var converter = builder().defaultSettings()
				// Register swapper only for SecondInterface, not FirstInterface or the class
				.addSwapper(SecondInterface.class, (conv, obj) -> "SWAPPED:" + obj.getSecond()).build();

			// MultiInterfaceWrapper won't directly match, FirstInterface won't match,
			// but SecondInterface will match during interface iteration
			assertEquals("SWAPPED:second", converter.stringify(new MultiInterfaceWrapper()));
		}
	}
}