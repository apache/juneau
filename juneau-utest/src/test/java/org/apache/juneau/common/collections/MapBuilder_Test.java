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
package org.apache.juneau.common.collections;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.Converter;
import org.junit.jupiter.api.*;

class MapBuilder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var b = MapBuilder.create(String.class, Integer.class);
		assertNotNull(b);
	}

	@Test
	void a02_addSingle() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.build();

		assertMap(map, "a=1");
	}

	@Test
	void a03_addMultiple() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", 2)
			.add("c", 3)
			.build();

		assertMap(map, "a=1", "b=2", "c=3");
	}

	@Test
	void a04_addAll() {
		var existing = new LinkedHashMap<String,Integer>();
		existing.put("x", 10);
		existing.put("y", 20);

		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.addAll(existing)
			.add("b", 2)
			.build();

		assertMap(map, "a=1", "x=10", "y=20", "b=2");
	}

	@Test
	void a05_addAllNull() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.addAll(null)
			.add("b", 2)
			.build();

		assertMap(map, "a=1", "b=2");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Add pairs
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_addPairs() {
		var map = MapBuilder.create(String.class, String.class)
			.addPairs("host", "localhost", "port", "8080", "protocol", "https")
			.build();

		assertMap(map, "host=localhost", "port=8080", "protocol=https");
	}

	@Test
	void b02_addPairs_oddNumber() {
		var b = MapBuilder.create(String.class, String.class);
		assertThrows(IllegalArgumentException.class, () -> b.addPairs("a", "b", "c"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sorting
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_sorted_naturalOrder() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.sorted()
			.build();

		assertSize(3, map);
		assertList(map.keySet(), "a", "b", "c");
		assertTrue(map instanceof TreeMap);
	}

	@Test
	void c02_sorted_customComparator() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("bb", 2)
			.add("ccc", 3)
			.sorted(Comparator.comparing(String::length))
			.build();

		assertList(map.keySet(), "a", "bb", "ccc");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sparse mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_sparse_empty() {
		var map = MapBuilder.create(String.class, Integer.class)
			.sparse()
			.build();

		assertNull(map);
	}

	@Test
	void d02_sparse_notEmpty() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.sparse()
			.build();

		assertNotNull(map);
		assertSize(1, map);
	}

	@Test
	void d03_notSparse_empty() {
		var map = MapBuilder.create(String.class, Integer.class)
			.build();

		assertNotNull(map);
		assertEmpty(map);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unmodifiable
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_unmodifiable() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", 2)
			.unmodifiable()
			.build();

		assertSize(2, map);
		assertThrows(UnsupportedOperationException.class, () -> map.put("c", 3));
	}

	@Test
	void e02_modifiable() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.build();

		map.put("b", 2);
		assertSize(2, map);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Copy mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_copy() {
		var original = new LinkedHashMap<String,Integer>();
		original.put("a", 1);

		var map = MapBuilder.create(String.class, Integer.class)
			.to(original)
			.add("b", 2)
			.copy()
			.add("c", 3)
			.build();

		assertSize(3, map);
		assertSize(2, original);  // Original has "a" and "b" added before copy()
		assertNotSame(original, map);  // After copy(), they're different maps
	}

	@Test
	void f02_noCopy() {
		var original = new LinkedHashMap<String,Integer>();
		original.put("a", 1);

		var map = MapBuilder.create(String.class, Integer.class)
			.to(original)
			.add("b", 2)
			.build();

		assertSize(2, map);
		assertSize(2, original);  // Original modified
		assertSame(original, map);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Complex scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_multipleOperations() {
		var existing = new LinkedHashMap<String,Integer>();
		existing.put("x", 10);
		existing.put("y", 20);

		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.addAll(existing)
			.add("b", 2)
			.sorted()
			.build();

		assertSize(4, map);
		// Should be sorted by key
		assertList(map.keySet(), "a", "b", "x", "y");
	}

	@Test
	void g02_sortedAndUnmodifiable() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.sorted()
			.unmodifiable()
			.build();

		assertSize(3, map);
		assertList(map.keySet(), "a", "b", "c");
		assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
	}

	@Test
	void g03_sparseAndSorted() {
		var map1 = MapBuilder.create(String.class, Integer.class)
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.sorted()
			.sparse()
			.build();

		assertNotNull(map1);
		assertSize(3, map1);

		var map2 = MapBuilder.create(String.class, Integer.class)
			.sorted()
			.sparse()
			.build();

		assertNull(map2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_buildEmptyMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.build();

		assertNotNull(map);
		assertEmpty(map);
	}

	@Test
	void h02_addNullKey() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add(null, 1)
			.add("a", 2)
			.build();

		assertMap(map, "<null>=1", "a=2");
	}

	@Test
	void h03_addNullValue() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", null)
			.add("c", 3)
			.build();

		assertMap(map, "a=1", "b=<null>", "c=3");
	}

	@Test
	void h04_duplicateKeys() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("a", 2)  // Overwrites first value
			.add("a", 3)  // Overwrites again
			.build();

		assertSize(1, map);  // Only one entry for key "a"
		assertMap(map, "a=3");  // Last value wins
	}

	@Test
	void h05_toExistingMap() {
		var existing = new LinkedHashMap<String,Integer>();
		existing.put("x", 10);

		var map = MapBuilder.create(String.class, Integer.class)
			.to(existing)
			.add("y", 20)
			.build();

		assertSize(2, map);
		assertMap(map, "x=10", "y=20");
		assertSame(existing, map);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Filtering
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_filtered_customPredicate() {
		var map = MapBuilder.create(String.class, String.class)
			.filtered(x -> x != null && !x.equals(""))
			.add("a", "foo")
			.add("b", null)     // Not added
			.add("c", "")       // Not added
			.add("d", "bar")
			.build();

		assertMap(map, "a=foo", "d=bar");
	}

	@Test
	void i02_filtered_defaultFilter() {
		var map = MapBuilder.create(String.class, Object.class)
			.filtered()
			.add("name", "John")
			.add("age", -1)              // Not added
			.add("enabled", false)       // Not added
			.add("tags", new String[0])  // Not added
			.add("emptyMap", new LinkedHashMap<>())  // Not added
			.add("emptyList", new ArrayList<>())     // Not added
			.add("value", 42)
			.build();

		assertMap(map, "name=John", "value=42");
	}

	@Test
	void i03_filtered_rejectsValue() {
		var map = MapBuilder.create(String.class, Integer.class)
			.filtered(x -> x != null && (Integer)x > 0)
			.add("a", 1)
			.add("b", -1)  // Not added
			.add("c", 0)   // Not added
			.add("d", 2)
			.build();

		assertMap(map, "a=1", "d=2");
	}

	@Test
	void i04_add_withFilter() {
		var map = MapBuilder.create(String.class, String.class)
			.filtered(x -> x != null && x instanceof String && ((String)x).length() > 2)
			.add("a", "foo")   // Added
			.add("b", "ab")    // Not added (length <= 2)
			.add("c", "bar")   // Added
			.build();

		assertMap(map, "a=foo", "c=bar");
	}

	@Test
	void i05_addAll_withFilter() {
		var existing = new LinkedHashMap<String,String>();
		existing.put("x", "longvalue");
		existing.put("y", "ab");  // Will be filtered out
		existing.put("z", "another");

		var map = MapBuilder.create(String.class, String.class)
			.filtered(x -> x != null && x instanceof String && ((String)x).length() > 2)
			.addAll(existing)
			.build();

		assertMap(map, "x=longvalue", "z=another");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Converters
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_converters_emptyArray() {
		var map = MapBuilder.create(String.class, Integer.class)
			.converters()  // Empty array
			.add("a", 1)
			.build();

		assertMap(map, "a=1");
	}

	@Test
	void j02_converters_withConverter() {
		Converter converter = new Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				if (type == Integer.class && o instanceof String) {
					return type.cast(Integer.parseInt((String)o));
				}
				return null;
			}
		};

		var inputMap = new LinkedHashMap<String,String>();
		inputMap.put("a", "1");
		inputMap.put("b", "2");

		var map = MapBuilder.create(String.class, Integer.class)
			.converters(converter)
			.addAny(inputMap)
			.build();

		assertMap(map, "a=1", "b=2");
	}

	@Test
	void j03_converters_multipleConverters() {
		Converter converter1 = new Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				return null;  // Doesn't handle this
			}
		};

		Converter converter2 = new Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				if (type == Integer.class && o instanceof String) {
					return type.cast(Integer.parseInt((String)o));
				}
				return null;
			}
		};

		var inputMap = new LinkedHashMap<String,String>();
		inputMap.put("a", "1");

		var map = MapBuilder.create(String.class, Integer.class)
			.converters(converter1, converter2)
			.addAny(inputMap)
			.build();

		assertMap(map, "a=1");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// AddAny
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_addAny_withMap() {
		var inputMap = new LinkedHashMap<String,Integer>();
		inputMap.put("a", 1);
		inputMap.put("b", 2);

		var map = MapBuilder.create(String.class, Integer.class)
			.addAny(inputMap)
			.build();

		assertMap(map, "a=1", "b=2");
	}

	@Test
	void k02_addAny_withMultipleMaps() {
		var map1 = new LinkedHashMap<String,Integer>();
		map1.put("a", 1);
		map1.put("b", 2);

		var map2 = new LinkedHashMap<String,Integer>();
		map2.put("c", 3);
		map2.put("d", 4);

		var map = MapBuilder.create(String.class, Integer.class)
			.addAny(map1, map2)
			.build();

		assertMap(map, "a=1", "b=2", "c=3", "d=4");
	}

	@Test
	void k03_addAny_withNullMap() {
		var inputMap = new LinkedHashMap<String,Integer>();
		inputMap.put("a", 1);
		inputMap.put("b", 2);

		var map = MapBuilder.create(String.class, Integer.class)
			.addAny(inputMap, null)  // null map should be ignored
			.build();

		assertMap(map, "a=1", "b=2");
	}

	@Test
	void k03b_addAny_withNullValueInMap() {
		// addAny uses toType which doesn't handle null values
		var inputMap = new LinkedHashMap<String,Integer>();
		inputMap.put("a", 1);
		inputMap.put("b", null);

		assertThrows(RuntimeException.class, () -> {
			MapBuilder.create(String.class, Integer.class)
				.addAny(inputMap)
				.build();
		});
	}

	@Test
	void k04_addAny_withTypeConversion() {
		var converter = new Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				if (type == String.class && o instanceof Integer) {
					return type.cast(String.valueOf(o));
				}
				if (type == Integer.class && o instanceof String) {
					return type.cast(Integer.parseInt((String)o));
				}
				return null;
			}
		};

		var inputMap = new LinkedHashMap<Integer,String>();
		inputMap.put(1, "10");
		inputMap.put(2, "20");

		var map = MapBuilder.create(String.class, Integer.class)
			.converters(converter)
			.addAny(inputMap)
			.build();

		assertMap(map, "1=10", "2=20");
	}

	@Test
	void k05_addAny_withConverterToMap() {
		var converter = new Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				if (type == Map.class && o instanceof String) {
					var m = new LinkedHashMap<String,String>();
					// Simple parsing: "key1=value1,key2=value2"
					var s = (String)o;
					for (var pair : s.split(",")) {
						var kv = pair.split("=");
						if (kv.length == 2) {
							m.put(kv[0], kv[1]);
						}
					}
					return type.cast(m);
				}
				return null;
			}
		};

		var map = MapBuilder.create(String.class, String.class)
			.converters(converter)
			.addAny("a=1,b=2")
			.build();

		assertMap(map, "a=1", "b=2");
	}

	@Test
	void k06_addAny_withConverterToMap_recursive() {
		var converter = new Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				if (type == Map.class && o instanceof String) {
					var m = new LinkedHashMap<String,String>();
					var s = (String)o;
					for (var pair : s.split(",")) {
						var kv = pair.split("=");
						if (kv.length == 2) {
							m.put(kv[0], kv[1]);
						}
					}
					return type.cast(m);
				}
				return null;
			}
		};

		var map = MapBuilder.create(String.class, String.class)
			.converters(converter)
			.addAny("x=foo", "y=bar")
			.build();

		assertMap(map, "x=foo", "y=bar");
	}

	@Test
	void k07_addAny_noKeyOrValueType() {
		var builder = new MapBuilder<String,Integer>(null, null);
		assertThrows(IllegalStateException.class, () -> builder.addAny(new LinkedHashMap<>()));
	}

	@Test
	void k08_addAny_conversionFailure() {
		// When converters is null and we try to add a non-Map, it will throw NPE
		assertThrows(NullPointerException.class, () -> {
			MapBuilder.create(String.class, Integer.class)
				.addAny("not-a-map")
				.build();
		});
	}

	@Test
	void k09_addAny_converterReturnsNull() {
		// Converter exists but returns null (can't convert)
		var converter = new Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				return null;  // Can't convert
			}
		};

		// Should throw RuntimeException when converter can't convert non-Map object
		assertThrows(RuntimeException.class, () -> {
			MapBuilder.create(String.class, Integer.class)
				.converters(converter)
				.addAny("not-a-map")
				.build();
		});
	}

	@Test
	void k10_addAny_toType_conversionFailure() {
		var converter = new Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				return null;  // Can't convert
			}
		};

		var inputMap = new LinkedHashMap<String,String>();
		inputMap.put("a", "not-an-integer");

		assertThrows(RuntimeException.class, () -> {
			MapBuilder.create(String.class, Integer.class)
				.converters(converter)
				.addAny(inputMap)
				.build();
		});
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Build edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_build_sparseWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.sparse()
			.build();

		assertNull(map);
	}

	@Test
	void l02_build_sparseWithEmptyMap() {
		var existing = new LinkedHashMap<String,Integer>();

		var map = MapBuilder.create(String.class, Integer.class)
			.to(existing)
			.sparse()
			.build();

		assertNull(map);
	}

	@Test
	void l03_build_notSparseWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.build();

		assertNotNull(map);
		assertEmpty(map);
	}

	@Test
	void l04_build_sortedWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.sorted()
			.build();

		assertNotNull(map);
		assertTrue(map instanceof TreeMap);
		assertEmpty(map);
	}

	@Test
	void l05_build_unmodifiableWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.unmodifiable()
			.build();

		assertNotNull(map);
		assertThrows(UnsupportedOperationException.class, () -> map.put("a", 1));
	}
}