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
package org.apache.juneau.junit;

import static org.apache.juneau.junit.Assertions2.*;
import static org.apache.juneau.junit.BasicBeanConverter.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

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
	class a_BuilderTest {

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
			assertEquals("[1,2,3]", converter.stringify(Arrays.asList(1, 2, 3)));

			// Test that default settings are applied
			assertEquals("<null>", converter.stringify(null));
		}

		@Test
		@DisplayName("a04_addStringifier() adds custom stringifier")
		void a04_addStringifier_addsCustomStringifier() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addStringifier(LocalDate.class, (conv, date) -> date.format(DateTimeFormatter.ISO_LOCAL_DATE))
				.build();

			var date = LocalDate.of(2023, 12, 25);
			assertEquals("2023-12-25", converter.stringify(date));
		}

		@Test
		@DisplayName("a05_addListifier() adds custom listifier")
		void a05_addListifier_addsCustomListifier() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addListifier(String.class, (conv, str) -> Arrays.asList((Object[])str.split(",")))
				.build();

			var result = converter.listify("a,b,c");
			assertEquals(Arrays.asList("a", "b", "c"), result);
		}

		@Test
		@DisplayName("a06_addSwapper() adds custom swapper")
		void a06_addSwapper_addsCustomSwapper() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSwapper(Optional.class, (conv, opt) -> ((Optional<?>)opt).orElse(null))
				.build();

			assertEquals("test", converter.stringify(Optional.of("test")));
			assertEquals("<null>", converter.stringify(Optional.empty()));
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

			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addPropertyExtractor(extractor)
				.build();

			var bean = new TestBean("John", 30);
			assertEquals("custom value", converter.getProperty(bean, "custom"));
		}

		@Test
		@DisplayName("a08_addSetting() adds custom setting")
		void a08_addSetting_addsCustomSetting() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting(SETTING_nullValue, "NULL")
				.build();

			assertEquals("NULL", converter.stringify(null));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Core Functionality Tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Core Functionality")
	class b_CoreFunctionalityTest {

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
			assertEquals("[1,2,3]", converter.stringify(Arrays.asList(1, 2, 3)));
			assertEquals("[]", converter.stringify(Collections.emptyList()));
			// Set order is not guaranteed, so check length and content
			var setResult = converter.stringify(Set.of("a", "b", "c"));
			assertEquals(7, setResult.length()); // Should be "[x,y,z]" format
			assertTrue(setResult.contains("a"));
			assertTrue(setResult.contains("b"));
			assertTrue(setResult.contains("c"));
		}

		@Test
		@DisplayName("b05_stringify() handles maps")
		void b05_stringify_handlesMaps() {
			var map = Map.of("name", "John", "age", 30);
			var result = converter.stringify(map);
			assertTrue(result.contains("name=John"));
			assertTrue(result.contains("age=30"));
			assertTrue(result.startsWith("{"));
			assertTrue(result.endsWith("}"));
		}

		@Test
		@DisplayName("b06_stringify() handles arrays")
		void b06_stringify_handlesArrays() {
			assertEquals("[1,2,3]", converter.stringify(new int[]{1, 2, 3}));
			assertEquals("[a,b,c]", converter.stringify(new String[]{"a", "b", "c"}));
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
			var result = converter.listify(new String[]{"a", "b", "c"});
			assertEquals(Arrays.asList("a", "b", "c"), result);
		}

		@Test
		@DisplayName("b09_listify() handles collections")
		void b09_listify_handlesCollections() {
			var set = Set.of("a", "b", "c");
			var result = converter.listify(set);
			assertEquals(3, result.size());
			assertTrue(result.containsAll(Arrays.asList("a", "b", "c")));
		}

		@Test
		@DisplayName("b10_listify() handles null")
		void b10_listify_handlesNull() {
			assertNull(converter.listify(null));
		}

		@Test
		@DisplayName("b11_canListify() returns correct values")
		void b11_canListify_returnsCorrectValues() {
			assertTrue(converter.canListify(Arrays.asList(1, 2, 3)));
			assertTrue(converter.canListify(new int[]{1, 2, 3}));
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
	class c_PropertyAccessTest {

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

			assertThrows(RuntimeException.class, () ->
				converter.getProperty(bean, "unknown"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Settings Tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Settings")
	class d_SettingsTest {

		@Test
		@DisplayName("d01_nullValue setting changes null representation")
		void d01_nullValue_changesNullRepresentation() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting(SETTING_nullValue, "NULL")
				.build();

			assertEquals("NULL", converter.stringify(null));
		}

		@Test
		@DisplayName("d02_fieldSeparator setting changes delimiter")
		void d02_fieldSeparator_changesDelimiter() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting(SETTING_fieldSeparator, " | ")
				.build();

			assertEquals("[1 | 2 | 3]", converter.stringify(Arrays.asList(1, 2, 3)));
		}

		@Test
		@DisplayName("d03_collection prefix/suffix settings change brackets")
		void d03_collectionBrackets_changeBrackets() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting(SETTING_collectionPrefix, "(")
				.addSetting(SETTING_collectionSuffix, ")")
				.build();

			assertEquals("(1,2,3)", converter.stringify(Arrays.asList(1, 2, 3)));
		}

		@Test
		@DisplayName("d04_map prefix/suffix settings change brackets")
		void d04_mapBrackets_changeBrackets() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting(SETTING_mapPrefix, "<")
				.addSetting(SETTING_mapSuffix, ">")
				.build();

			var map = Map.of("a", 1);
			var result = converter.stringify(map);
			assertTrue(result.startsWith("<"));
			assertTrue(result.endsWith(">"));
		}

		@Test
		@DisplayName("d05_mapEntrySeparator setting changes key-value separator")
		void d05_mapEntrySeparator_changesKeyValueSeparator() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting(SETTING_mapEntrySeparator, ":")
				.build();

			var map = Map.of("name", "John");
			var result = converter.stringify(map);
			assertTrue(result.contains("name:John"));
		}

		@Test
		@DisplayName("d06_classNameFormat setting changes class name format")
		void d06_classNameFormat_changesFormat() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting(SETTING_classNameFormat, "full")
				.build();

			var bean = new TestBean("John", 30);
			var result = converter.stringify(bean);
			assertTrue(result.contains("org.apache.juneau.junit.BasicBeanConverter_Test$TestBean"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error Handling Tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Error Handling")
	class e_ErrorHandlingTest {

		private BasicBeanConverter converter;

		@BeforeEach
		void setUp() {
			converter = BasicBeanConverter.builder().defaultSettings().build();
		}

		@Test
		@DisplayName("e01_getProperty() with invalid property throws RuntimeException")
		void e01_getProperty_invalidProperty_throwsException() {
			var bean = new TestBean("John", 30);

			var ex = assertThrows(RuntimeException.class, () ->
				converter.getProperty(bean, "invalidProperty"));
			assertContains("Property invalidProperty not found", ex.getMessage());
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
}
