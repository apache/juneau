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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for PropertyExtractors.
 */
@DisplayName("PropertyExtractors")
class PropertyExtractors_Test extends TestBase {

	private BasicBeanConverter converter;

	@BeforeEach
	void setUp() {
		converter = BasicBeanConverter.builder().defaultSettings().build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// ObjectPropertyExtractor
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("ObjectPropertyExtractor")
	class A_objectPropertyExtractorTest extends TestBase {

		private PropertyExtractors.ObjectPropertyExtractor extractor = new PropertyExtractors.ObjectPropertyExtractor();

		@Test
		@DisplayName("canExtract() - always returns true")
		void a01_canExtract_alwaysReturnsTrue() {
			assertTrue(extractor.canExtract(converter, new TestBean(), "name"));
			assertTrue(extractor.canExtract(converter, "string", "length"));
			assertTrue(extractor.canExtract(converter, 123, "value"));
			assertTrue(extractor.canExtract(converter, null, "anything"));
		}

		@Test
		@DisplayName("extract() - getter methods")
		void a02_extract_getterMethods() {
			var bean = new TestBean("John", 30, true);

			assertEquals("John", extractor.extract(converter, bean, "name"));
			assertEquals(30, extractor.extract(converter, bean, "age"));
			assertEquals(true, extractor.extract(converter, bean, "active"));
		}

		@Test
		@DisplayName("extract() - boolean is* methods")
		void a03_extract_booleanIsMethods() {
			var bean = new TestBean("John", 30, true);

			assertEquals(true, extractor.extract(converter, bean, "active"));
		}

		@Test
		@DisplayName("extract() - public fields")
		void a04_extract_publicFields() {
			var bean = new TestBeanWithFields();
			bean.publicField = "test value";

			assertEquals("test value", extractor.extract(converter, bean, "publicField"));
		}

		@Test
		@DisplayName("extract() - inherited fields")
		void a05_extract_inheritedFields() {
			var bean = new ChildBeanWithFields();
			bean.parentField = "parent value";
			bean.childField = "child value";

			assertEquals("parent value", extractor.extract(converter, bean, "parentField"));
			assertEquals("child value", extractor.extract(converter, bean, "childField"));
		}

		@Test
		@DisplayName("extract() - method with property name")
		void a06_extract_methodWithPropertyName() {
			var bean = new TestBeanWithMethods();

			assertEquals("custom method", extractor.extract(converter, bean, "customMethod"));
		}

		@Test
		@DisplayName("extract() - Map-style get(String) method")
		void a07_extract_mapStyleGetter() {
			var bean = new TestBeanWithMapGetter();

			assertEquals("mapped value", extractor.extract(converter, bean, "key1"));
		}

		@Test
		@DisplayName("extract() - null object returns null")
		void a08_extract_nullObject() {
			assertNull(extractor.extract(converter, null, "anything"));
		}

		@Test
		@DisplayName("extract() - property not found throws RuntimeException")
		void a09_extract_propertyNotFound() {
			var bean = new TestBean("John", 30, true);

			var ex = assertThrows(RuntimeException.class, () ->
			extractor.extract(converter, bean, "nonExistentProperty"));
			assertContains("Property 'nonExistentProperty' not found on object of type TestBean", ex.getMessage());
		}

		@Test
		@DisplayName("extract() - isX methods with parameters are ignored (line 91)")
		void a10_extract_isMethodsWithParametersIgnored() {
			var bean = new TestBeanWithParameterizedMethods();

			// isActive() with no parameters should work
			assertEquals(true, extractor.extract(converter, bean, "active"));

			// isValid(String) with parameters should be ignored and fall back to property not found
			var ex = assertThrows(RuntimeException.class, () ->
				extractor.extract(converter, bean, "valid"));
			assertContains("Property 'valid' not found", ex.getMessage());
		}

		@Test
		@DisplayName("extract() - getX methods with parameters are ignored (line 96)")
		void a11_extract_getMethodsWithParametersIgnored() {
			var bean = new TestBeanWithParameterizedMethods();

			// getName() with no parameters should work
			assertEquals("test", extractor.extract(converter, bean, "name"));

			// getDescription(String) with parameters should be ignored and fall back to property not found
			var ex = assertThrows(RuntimeException.class, () ->
				extractor.extract(converter, bean, "description"));
			assertContains("Property 'description' not found", ex.getMessage());
		}

		@Test
		@DisplayName("extract() - get() methods with wrong signature are ignored (line 101)")
		void a12_extract_getMethodsWithWrongSignatureIgnored() {
			var bean = new TestBeanWithInvalidGetMethods();

			// get(String) with correct signature should work
			assertEquals("mapped_value", extractor.extract(converter, bean, "key"));

			// For a property that doesn't exist, the get(String) method will still be called
			// but should return the result from get("nonExistent")
			assertEquals("mapped_value", extractor.extract(converter, bean, "nonExistent"));

			// Test with a bean that has ONLY invalid get methods (no valid get(String))
			var beanWithoutValidGet = new TestBeanWithOnlyInvalidGetMethods();
			var ex = assertThrows(RuntimeException.class, () ->
				extractor.extract(converter, beanWithoutValidGet, "nonExistent"));
			assertContains("Property 'nonExistent' not found", ex.getMessage());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// ListPropertyExtractor
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("ListPropertyExtractor")
	class B_listPropertyExtractorTest extends TestBase {

		private PropertyExtractors.ListPropertyExtractor extractor = new PropertyExtractors.ListPropertyExtractor();

		@Test
		@DisplayName("canExtract() - returns true for listifiable objects")
		void b01_canExtract_listifiableObjects() {
			assertTrue(extractor.canExtract(converter, l("a", "b", "c"), "0"));
			assertTrue(extractor.canExtract(converter, a("a", "b", "c"), "1"));
			assertTrue(extractor.canExtract(converter, Set.of("a", "b", "c"), "size"));
			assertFalse(extractor.canExtract(converter, "not listifiable", "0"));
		}

		@Test
		@DisplayName("extract() - numeric indices")
		void b02_extract_numericIndices() {
			var list = l("first", "second", "third");

			assertEquals("first", extractor.extract(converter, list, "0"));
			assertEquals("second", extractor.extract(converter, list, "1"));
			assertEquals("third", extractor.extract(converter, list, "2"));
		}

		@Test
		@DisplayName("extract() - negative indices")
		void b03_extract_negativeIndices() {
			var list = l("first", "second", "third");

			assertEquals("third", extractor.extract(converter, list, "-1"));
			assertEquals("second", extractor.extract(converter, list, "-2"));
			assertEquals("first", extractor.extract(converter, list, "-3"));
		}

		@Test
		@DisplayName("extract() - arrays")
		void b04_extract_arrays() {
			var array = a("a", "b", "c");

			assertEquals("a", extractor.extract(converter, array, "0"));
			assertEquals("b", extractor.extract(converter, array, "1"));
			assertEquals("c", extractor.extract(converter, array, "2"));
		}

		@Test
		@DisplayName("extract() - length property")
		void b05_extract_lengthProperty() {
			var list = l("a", "b", "c");
			var array = a("a", "b", "c");

			assertEquals(3, extractor.extract(converter, list, "length"));
			assertEquals(3, extractor.extract(converter, array, "length"));
		}

		@Test
		@DisplayName("extract() - size property")
		void b06_extract_sizeProperty() {
			var list = l("a", "b", "c");
			var set = Set.of("a", "b", "c");

			assertEquals(3, extractor.extract(converter, list, "size"));
			assertEquals(3, extractor.extract(converter, set, "size"));
		}

		@Test
		@DisplayName("extract() - falls back to ObjectPropertyExtractor")
		void b07_extract_fallbackToObjectPropertyExtractor() {
			var list = new ArrayList<>(l("a", "b", "c"));

			// Should fall back to ArrayList.isEmpty() method
			assertEquals(false, extractor.extract(converter, list, "empty"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// MapPropertyExtractor
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("MapPropertyExtractor")
	class C_mapPropertyExtractorTest extends TestBase {

		private PropertyExtractors.MapPropertyExtractor extractor = new PropertyExtractors.MapPropertyExtractor();

		@Test
		@DisplayName("canExtract() - returns true only for Map objects")
		void c01_canExtract_mapObjects() {
			assertTrue(extractor.canExtract(converter, Map.of("key", "value"), "key"));
			assertTrue(extractor.canExtract(converter, new HashMap<>(), "size"));
			assertFalse(extractor.canExtract(converter, l("a", "b"), "0"));
			assertFalse(extractor.canExtract(converter, "not a map", "length"));
		}

		@Test
		@DisplayName("extract() - direct key access")
		void c02_extract_directKeyAccess() {
			var map = Map.of("name", "John", "age", 30, "active", true);

			assertEquals("John", extractor.extract(converter, map, "name"));
			assertEquals(30, extractor.extract(converter, map, "age"));
			assertEquals(true, extractor.extract(converter, map, "active"));
		}

		@Test
		@DisplayName("extract() - size property")
		void c03_extract_sizeProperty() {
			var map = Map.of("a", 1, "b", 2, "c", 3);

			assertEquals(3, extractor.extract(converter, map, "size"));
		}

		@Test
		@DisplayName("extract() - empty map")
		void c04_extract_emptyMap() {
			var map = new HashMap<String, Object>();

			assertEquals(0, extractor.extract(converter, map, "size"));

			// Non-existent key should fall back to ObjectPropertyExtractor and throw exception
			var ex = assertThrows(RuntimeException.class, () ->
			extractor.extract(converter, map, "nonExistentKey"));
			assertContains("Property 'nonExistentKey' not found on object of type HashMap", ex.getMessage());
		}

		@Test
		@DisplayName("extract() - Properties object")
		void c05_extract_propertiesObject() {
			var props = new Properties();
			props.setProperty("config.timeout", "5000");
			props.setProperty("config.enabled", "true");

			assertEquals("5000", extractor.extract(converter, props, "config.timeout"));
			assertEquals("true", extractor.extract(converter, props, "config.enabled"));
		}

		@Test
		@DisplayName("extract() - ConcurrentHashMap")
		void c06_extract_concurrentHashMap() {
			var map = new ConcurrentHashMap<String, Object>();
			map.put("thread-safe", true);
			map.put("capacity", 16);

			assertEquals(true, extractor.extract(converter, map, "thread-safe"));
			assertEquals(16, extractor.extract(converter, map, "capacity"));
			assertEquals(2, extractor.extract(converter, map, "size"));
		}

		@Test
		@DisplayName("extract() - key priority over JavaBean properties")
		void c07_extract_keyPriorityOverJavaBeanProperties() {
			var map = new TestMapWithMethods();
			map.put("customProperty", "map value");

			// Map key should take priority over the getCustomProperty() method
			assertEquals("map value", extractor.extract(converter, map, "customProperty"));
		}

		@Test
		@DisplayName("extract() - null value setting handling")
		void c08_extract_nullValueSettingHandling() {
			// Test the specific line 238 logic where property name matches nullValue setting
			var map = new HashMap<String, Object>();
			map.put(null, "null key value"); // Map with actual null key
			map.put("other", "other value");

			// When property name matches the nullValue setting ("<null>"), it should be converted to null
			assertEquals("null key value", extractor.extract(converter, map, "<null>"));

			// Test with custom nullValue setting
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting(BasicBeanConverter.SETTING_nullValue, "NULL_KEY")
				.build();

			var customExtractor = new PropertyExtractors.MapPropertyExtractor();
			map.put("NULL_KEY", "literal null key string"); // Map with literal "NULL_KEY" string

			// When property name matches custom nullValue setting, it should look for null key
			assertEquals("null key value", customExtractor.extract(customConverter, map, "NULL_KEY"));
		}

		@Test
		@DisplayName("extract() - falls back to ObjectPropertyExtractor")
		void c09_extract_fallbackToObjectPropertyExtractor() {
			var map = new TestMapWithMethods();

			// Should fall back to HashMap.isEmpty() method since "empty" is not a key
			assertEquals(true, extractor.extract(converter, map, "empty"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test Helper Classes
	//------------------------------------------------------------------------------------------------------------------

	public static class TestBean {
		private String name;
		private int age;
		private boolean active;

		public TestBean() {}

		public TestBean(String name, int age, boolean active) {
			this.name = name;
			this.age = age;
			this.active = active;
		}

		public String getName() { return name; }
		public int getAge() { return age; }
		public boolean isActive() { return active; }

		void setName(String name) { this.name = name; }
		void setAge(int age) { this.age = age; }
		void setActive(boolean active) { this.active = active; }
	}

	public static class TestBeanWithFields {
		public String publicField;
		@SuppressWarnings("unused")
		private String privateField = "private";
	}

	public static class ParentBeanWithFields {
		public String parentField;
	}

	public static class ChildBeanWithFields extends ParentBeanWithFields {
		public String childField;
	}

	public static class TestBeanWithMethods {
		public String customMethod() {
			return "custom method";
		}
	}

	public static class TestBeanWithMapGetter {
		private Map<String, String> data = Map.of("key1", "mapped value", "key2", "another value");

		public String get(String key) {
			return data.get(key);
		}
	}

	public static class TestMapWithMethods extends HashMap<String, Object> {
		private static final long serialVersionUID = 1L;

		public String getCustomProperty() {
			return "method value";
		}
	}

	public static class TestBeanWithParameterizedMethods {
		// Valid methods that should work
		public boolean isActive() {
			return true;
		}

		public String getName() {
			return "test";
		}

		// Invalid methods that should be ignored due to parameters (lines 91 & 96)
		public boolean isValid(String criteria) {
			return criteria != null;
		}

		public String getDescription(String language) {
			return "Description in " + language;
		}

		public String getDescription(String language, String format) {
			return "Description in " + language + " format " + format;
		}
	}

	public static class TestBeanWithInvalidGetMethods {
		// Valid get(String) method that should work (line 101)
		public String get(String key) {
			return "mapped_value";
		}

		// Invalid get() methods that should be ignored due to wrong signatures
		public String get() {
			return "no_parameters";
		}

		public String get(int index) {
			return "wrong_parameter_type";
		}

		public String get(String key1, String key2) {
			return "too_many_parameters";
		}

		public Integer get(String key, boolean flag) {
			return 42; // Wrong return type and too many parameters
		}
	}

	public static class TestBeanWithOnlyInvalidGetMethods {
		// Only invalid get() methods that should be ignored due to wrong signatures
		public String get() {
			return "no_parameters";
		}

		public String get(int index) {
			return "wrong_parameter_type";
		}

		public String get(String key1, String key2) {
			return "too_many_parameters";
		}

		public Integer get(String key, boolean flag) {
			return 42; // Wrong return type and too many parameters
		}
	}
}