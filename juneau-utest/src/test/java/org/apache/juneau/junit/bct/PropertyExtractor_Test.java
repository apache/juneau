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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the {@link PropertyExtractor} interface.
 *
 * <p>This test class verifies interface contract compliance, custom implementations,
 * and edge case handling for PropertyExtractor implementations.</p>
 */
class PropertyExtractor_Test extends TestBase {

	// ====================================================================================================
	// Interface Contract Tests
	// ====================================================================================================

	@Nested
	class A_interfaceContract extends TestBase {

		@SuppressWarnings("cast")
		@Test
		void a01_interfaceImplementation() {
			// Verify it's a proper interface with two methods
			PropertyExtractor extractor = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return true;
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					return "EXTRACTED:" + key;
				}
			};

			assertNotNull(extractor);
			assertTrue(extractor instanceof PropertyExtractor);
		}

		@Test
		void a02_anonymousClassImplementation() {
			// Test anonymous class implementation
			PropertyExtractor impl = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return o instanceof String;
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					return o.getClass().getSimpleName() + "." + key;
				}
			};

			BeanConverter converter = BasicBeanConverter.DEFAULT;
			String result = (String) impl.extract(converter, "test", "length");

			assertEquals("String.length", result);
		}

		@Test
		void a03_concreteClassImplementation() {
			// Test concrete class implementation
			var concrete = new PrefixPropertyExtractor();

			var converter = BasicBeanConverter.DEFAULT;
			var result = (String) concrete.extract(converter, "test", "prop");

			assertEquals("PREFIX:prop", result);
		}

		@Test
		void a04_canExtractMethodContract() {
			var extractor = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return true;
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					return key.toUpperCase();
				}
			};

			var converter = BasicBeanConverter.DEFAULT;

			// canExtract method should work correctly
			assertTrue(extractor.canExtract(converter, "test", "any"));
			assertTrue(extractor.canExtract(converter, null, "any"));
			assertTrue(extractor.canExtract(converter, new Object(), "any"));
		}
	}

	// ====================================================================================================
	// Custom Implementation Tests
	// ====================================================================================================

	@Nested
	class B_customImplementations extends TestBase {

		@Test
		void b01_customCanExtractLogic() {
			var selective = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return o instanceof String && key.startsWith("str");
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					return "STRING_PROP:" + key;
				}
			};

			var converter = BasicBeanConverter.DEFAULT;

			// Should extract only for String objects with "str" prefix
			assertTrue(selective.canExtract(converter, "test", "string"));
			assertTrue(selective.canExtract(converter, "test", "str"));
			assertFalse(selective.canExtract(converter, "test", "other"));
			assertFalse(selective.canExtract(converter, 123, "string"));

			assertEquals("STRING_PROP:string", selective.extract(converter, "test", "string"));
		}

		@Test
		void b02_nullHandlingExtractor() {
			var nullSafe = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return true; // Always can extract
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					if (o == null) return "NULL_OBJECT";
					if (key == null) return "NULL_PROPERTY";
					return o.toString() + ":" + key;
				}
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals("NULL_OBJECT", nullSafe.extract(converter, null, "any"));
			assertEquals("NULL_PROPERTY", nullSafe.extract(converter, "obj", null));
			assertEquals("test:prop", nullSafe.extract(converter, "test", "prop"));
		}

		@Test
		void b03_typeSpecificExtractor() {
			var numberExtractor = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return o instanceof Number;
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					if (o instanceof Number) {
						return switch (key) {
							case "doubled" -> ((Number) o).doubleValue() * 2;
							case "string" -> o.toString();
							case "type" -> o.getClass().getSimpleName();
							default -> "UNKNOWN_PROP:" + key;
						};
					}
					return "NOT_A_NUMBER";
				}
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals(84.0, numberExtractor.extract(converter, 42, "doubled"));
			assertEquals("42", numberExtractor.extract(converter, 42, "string"));
			assertEquals("Integer", numberExtractor.extract(converter, 42, "type"));
			assertEquals("UNKNOWN_PROP:other", numberExtractor.extract(converter, 42, "other"));
			assertEquals("NOT_A_NUMBER", numberExtractor.extract(converter, "string", "doubled"));
		}
	}

	// ====================================================================================================
	// Edge Case Tests
	// ====================================================================================================

	@Nested
	class C_edgeCases extends TestBase {

		@Test
		void c01_exceptionHandling() {
			var throwing = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return true;
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					if ("error".equals(key)) {
						throw new RuntimeException("Intentional test exception");
					}
					return "SUCCESS:" + key;
				}
			};

			var converter = BasicBeanConverter.DEFAULT;

			// Normal case should work
			assertEquals("SUCCESS:normal", throwing.extract(converter, "obj", "normal"));

			// Exception case should throw
			assertThrows(RuntimeException.class, () -> throwing.extract(converter, "obj", "error"));
		}

		@Test
		void c02_recursiveExtraction() {
			var recursive = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return o instanceof String && "recursive".equals(key);
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					if ("recursive".equals(key) && o instanceof String) {
						// Use the converter recursively
						String str = (String) o;
						return "RECURSIVE[" + converter.stringify(str.length()) + "]";
					}
					return "NON_RECURSIVE:" + key;
				}
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals("RECURSIVE[4]", recursive.extract(converter, "test", "recursive"));
			assertEquals("NON_RECURSIVE:other", recursive.extract(converter, "test", "other"));
		}

		@Test
		void c03_complexObjectExtraction() {
			var complex = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return o instanceof Map;
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					if (o instanceof Map) {
						Map<?, ?> map = (Map<?, ?>) o;
						return switch (key) {
							case "keys" -> new ArrayList<>(map.keySet());
							case "values" -> new ArrayList<>(map.values());
							case "entries" -> map.entrySet().size();
							default -> map.get(key);
						};
					}
					return "NOT_A_MAP";
				}
			};

			var converter = BasicBeanConverter.DEFAULT;
			var testMap = Map.of("a", "valueA", "b", "valueB");

			var keys = (List<String>) complex.extract(converter, testMap, "keys");
			assertEquals(2, keys.size());
			assertTrue(keys.contains("a"));
			assertTrue(keys.contains("b"));

			assertEquals(2, complex.extract(converter, testMap, "entries"));
			assertEquals("valueA", complex.extract(converter, testMap, "a"));
			assertEquals("NOT_A_MAP", complex.extract(converter, "string", "keys"));
		}
	}

	// ====================================================================================================
	// Integration Tests
	// ====================================================================================================

	@Nested
	class D_integration extends TestBase {

		@Test
		void d01_integrationWithBasicBeanConverter() {
			// Test custom extractor with BasicBeanConverter
			var customExtractor = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return "customProp".equals(key);
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					return "CUSTOM[" + o.getClass().getSimpleName() + "." + key + "]";
				}
			};

			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addPropertyExtractor(customExtractor)
				.build();

			// Test that the custom extractor works
			var bean = new TestBean("test", 42);
			var result = customConverter.getNested(bean, Utils.tokenize("customProp").get(0));

			// Should get our custom result
			assertEquals("CUSTOM[TestBean.customProp]", result);
		}

		@Test
		void d02_multipleExtractorPriority() {
			var first = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return "first".equals(key);
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					return "FIRST_EXTRACTOR";
				}
			};

			var second = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return "second".equals(key);
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					return "SECOND_EXTRACTOR";
				}
			};

			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addPropertyExtractor(first)
				.addPropertyExtractor(second)
				.build();

			// Test that each extractor handles its specific properties
			var bean = new TestBean("test", 42);

			assertEquals("FIRST_EXTRACTOR", converter.getNested(bean, Utils.tokenize("first").get(0)));
			assertEquals("SECOND_EXTRACTOR", converter.getNested(bean, Utils.tokenize("second").get(0)));
		}

		@Test
		void d03_fallbackToDefaultExtractors() {
			var custom = new PropertyExtractor() {
				@Override
				public boolean canExtract(BeanConverter converter, Object o, String key) {
					return "custom".equals(key);
				}

				@Override
				public Object extract(BeanConverter converter, Object o, String key) {
					return "CUSTOM_VALUE";
				}
			};

			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addPropertyExtractor(custom)
				.build();

			var bean = new TestBean("test", 42);

			// Custom property should use our extractor
			assertEquals("CUSTOM_VALUE", converter.getNested(bean, Utils.tokenize("custom").get(0)));

			// Regular properties should use default extractors
			assertEquals("test", converter.getNested(bean, Utils.tokenize("name").get(0)));
			assertEquals("42", converter.getNested(bean, Utils.tokenize("value").get(0)));
		}
	}

	// ====================================================================================================
	// Helper Classes
	// ====================================================================================================

	static class PrefixPropertyExtractor implements PropertyExtractor {
		@Override
		public boolean canExtract(BeanConverter converter, Object o, String key) {
			return true;
		}

		@Override
		public Object extract(BeanConverter converter, Object o, String key) {
			return "PREFIX:" + key;
		}
	}

	static class TestBean {
		final String name;
		final int value;

		TestBean(String name, int value) {
			this.name = name;
			this.value = value;
		}

		public String getName() { return name; }
		public int getValue() { return value; }
	}
}