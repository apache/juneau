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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link Listifiers}.
 */
class Listifiers_Test extends TestBase {

	@Nested
	class A_collectionListifier extends TestBase {

		@Test
		void a01_listifyList() {
			var listifier = Listifiers.collectionListifier();
			var input = List.of("a", "b", "c");
			var result = listifier.apply(null, input);

			assertNotSame(input, result); // Should create new list
			assertList(result, "a", "b", "c");
		}

		@Test
		void a02_listifySet() {
			var listifier = Listifiers.collectionListifier();
			var input = Set.of("z", "a", "m"); // Unordered input
			var result = listifier.apply(null, input);

			// TreeSet conversion ensures natural ordering
			assertList(result, "a", "m", "z");
		}

		@Test
		void a02a_listifySetTypes() {
			var listifier = Listifiers.collectionListifier();

			// HashSet (unordered) -> converted to TreeSet for natural ordering
			var hashSet = new HashSet<>(Arrays.asList("z", "a", "m"));
			var hashResult = listifier.apply(null, hashSet);
			assertList(hashResult, "a", "m", "z");

			// LinkedHashSet preserves insertion order
			var linkedSet = new LinkedHashSet<>(Arrays.asList("z", "a", "m"));
			var linkedResult = listifier.apply(null, linkedSet);
			assertList(linkedResult, "z", "a", "m"); // Insertion order

			// TreeSet already sorted, preserves its order
			var treeSet = new TreeSet<>(Arrays.asList("z", "a", "m"));
			var treeResult = listifier.apply(null, treeSet);
			assertList(treeResult, "a", "m", "z"); // Natural order
		}

		@Test
		void a03_listifyQueue() {
			var listifier = Listifiers.collectionListifier();
			var input = new LinkedList<>(List.of("first", "second"));
			var result = listifier.apply(null, input);

			assertList(result, "first", "second");
		}

		@Test
		void a04_listifyEmptyCollection() {
			var listifier = Listifiers.collectionListifier();
			var input = List.of();
			var result = listifier.apply(null, input);

			assertTrue(result.isEmpty());
		}

		@Test
		void a05_listifyWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var listifier = Listifiers.collectionListifier();
			var input = List.of(1, 2, 3);
			var result = listifier.apply(converter, input);

			assertList(result, 1, 2, 3);
		}
	}

	@Nested
	class B_iterableListifier extends TestBase {

		@Test
		void b01_listifyIterable() {
			var listifier = Listifiers.iterableListifier();
			Iterable<String> input = List.of("a", "b", "c");
			var result = listifier.apply(null, input);

			assertList(result, "a", "b", "c");
		}

		@Test
		void b02_listifyCustomIterable() {
			var listifier = Listifiers.iterableListifier();
			// Create a simple custom iterable
			Iterable<Integer> input = () -> IntStream.range(1, 4).iterator();
			var result = listifier.apply(null, input);

			assertList(result, 1, 2, 3);
		}

		@Test
		void b03_listifyEmptyIterable() {
			var listifier = Listifiers.iterableListifier();
			Iterable<String> input = List.of();
			var result = listifier.apply(null, input);

			assertTrue(result.isEmpty());
		}

		@Test
		void b04_listifyWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var listifier = Listifiers.iterableListifier();
			Iterable<String> input = Set.of("x", "y");
			var result = listifier.apply(converter, input);

			assertEquals(2, result.size());
			assertTrue(result.contains("x"));
			assertTrue(result.contains("y"));
		}
	}

	@Nested
	class C_iteratorListifier extends TestBase {

		@Test
		void c01_listifyIterator() {
			var listifier = Listifiers.iteratorListifier();
			var input = List.of("a", "b", "c").iterator();
			var result = listifier.apply(null, input);

			assertList(result, "a", "b", "c");
			// Iterator should be exhausted
			assertFalse(input.hasNext());
		}

		@Test
		void c02_listifyEmptyIterator() {
			var listifier = Listifiers.iteratorListifier();
			var input = List.of().iterator();
			var result = listifier.apply(null, input);

			assertTrue(result.isEmpty());
		}

		@Test
		void c03_listifyLargeIterator() {
			var listifier = Listifiers.iteratorListifier();
			var input = IntStream.range(0, 1000).iterator();
			var result = listifier.apply(null, input);

			assertEquals(1000, result.size());
			assertEquals(0, result.get(0));
			assertEquals(999, result.get(999));
		}

		@Test
		void c04_listifyWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var listifier = Listifiers.iteratorListifier();
			var input = List.of("x", "y", "z").iterator();
			var result = listifier.apply(converter, input);

			assertList(result, "x", "y", "z");
		}
	}

	@Nested
	class D_enumerationListifier extends TestBase {

		@Test
		void d01_listifyEnumeration() {
			var listifier = Listifiers.enumerationListifier();
			var vector = new Vector<>(List.of("a", "b", "c"));
			var input = vector.elements();
			var result = listifier.apply(null, input);

			assertList(result, "a", "b", "c");
			// Enumeration should be exhausted
			assertFalse(input.hasMoreElements());
		}

		@Test
		void d02_listifyEmptyEnumeration() {
			var listifier = Listifiers.enumerationListifier();
			var vector = new Vector<String>();
			var input = vector.elements();
			var result = listifier.apply(null, input);

			assertTrue(result.isEmpty());
		}

		@Test
		void d03_listifyHashtableKeys() {
			var listifier = Listifiers.enumerationListifier();
			var hashtable = new Hashtable<String, String>();
			hashtable.put("key1", "value1");
			hashtable.put("key2", "value2");
			var input = hashtable.keys();
			var result = listifier.apply(null, input);

			assertEquals(2, result.size());
			assertTrue(result.contains("key1"));
			assertTrue(result.contains("key2"));
		}

		@Test
		void d04_listifyWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var listifier = Listifiers.enumerationListifier();
			var vector = new Vector<>(List.of(1, 2, 3));
			var input = vector.elements();
			var result = listifier.apply(converter, input);

			assertList(result, 1, 2, 3);
		}
	}

	@Nested
	class E_streamListifier extends TestBase {

		@Test
		void e01_listifyStream() {
			var listifier = Listifiers.streamListifier();
			var input = Stream.of("a", "b", "c");
			var result = listifier.apply(null, input);

			assertList(result, "a", "b", "c");
		}

		@Test
		void e02_listifyEmptyStream() {
			var listifier = Listifiers.streamListifier();
			var input = Stream.empty();
			var result = listifier.apply(null, input);

			assertTrue(result.isEmpty());
		}

		@Test
		void e03_listifyFilteredStream() {
			var listifier = Listifiers.streamListifier();
			var input = IntStream.range(1, 10)
				.filter(n -> n % 2 == 0)
				.boxed();
			var result = listifier.apply(null, input);

			assertList(result, 2, 4, 6, 8);
		}

		@Test
		void e04_listifyMappedStream() {
			var listifier = Listifiers.streamListifier();
			var input = Stream.of("hello", "world")
				.map(String::toUpperCase);
			var result = listifier.apply(null, input);

			assertList(result, "HELLO", "WORLD");
		}

		@Test
		void e05_listifyWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var listifier = Listifiers.streamListifier();
			var input = Stream.of(1, 2, 3);
			var result = listifier.apply(converter, input);

			assertList(result, 1, 2, 3);
		}
	}

	@Nested
	class F_mapListifier extends TestBase {

		@Test
		void f01_listifyMap() {
			var listifier = Listifiers.mapListifier();
			var input = Map.of("z", "value1", "a", "value2"); // Unordered input
			var result = listifier.apply(null, input);

			assertEquals(2, result.size());
			// Result should contain Map.Entry objects
			assertTrue(result.stream().allMatch(obj -> obj instanceof Map.Entry));

			// TreeMap conversion ensures natural key ordering
			var entries = result.stream()
				.map(obj -> (Map.Entry<?, ?>) obj)
				.toList();
			assertEquals("a", entries.get(0).getKey());
			assertEquals("z", entries.get(1).getKey());
		}

		@Test
		void f01a_listifyMapTypes() {
			var listifier = Listifiers.mapListifier();

			// HashMap (unordered) -> converted to TreeMap for natural key ordering
			var hashMap = new HashMap<String, String>();
			hashMap.put("z", "value1");
			hashMap.put("a", "value2");
			hashMap.put("m", "value3");
			var hashResult = listifier.apply(null, hashMap);
			var hashEntries = hashResult.stream()
				.map(obj -> (Map.Entry<?, ?>) obj)
				.toList();
			assertEquals("a", hashEntries.get(0).getKey());
			assertEquals("m", hashEntries.get(1).getKey());
			assertEquals("z", hashEntries.get(2).getKey());

			// LinkedHashMap preserves insertion order
			var linkedMap = new LinkedHashMap<String, String>();
			linkedMap.put("z", "value1");
			linkedMap.put("a", "value2");
			linkedMap.put("m", "value3");
			var linkedResult = listifier.apply(null, linkedMap);
			var linkedEntries = linkedResult.stream()
				.map(obj -> (Map.Entry<?, ?>) obj)
				.toList();
			assertEquals("z", linkedEntries.get(0).getKey()); // Insertion order
			assertEquals("a", linkedEntries.get(1).getKey());
			assertEquals("m", linkedEntries.get(2).getKey());

			// TreeMap already sorted, preserves its order
			var treeMap = new TreeMap<String, String>();
			treeMap.put("z", "value1");
			treeMap.put("a", "value2");
			treeMap.put("m", "value3");
			var treeResult = listifier.apply(null, treeMap);
			var treeEntries = treeResult.stream()
				.map(obj -> (Map.Entry<?, ?>) obj)
				.toList();
			assertEquals("a", treeEntries.get(0).getKey()); // Natural order
			assertEquals("m", treeEntries.get(1).getKey());
			assertEquals("z", treeEntries.get(2).getKey());
		}

		@Test
		void f02_listifyEmptyMap() {
			var listifier = Listifiers.mapListifier();
			var input = Map.of();
			var result = listifier.apply(null, input);

			assertTrue(result.isEmpty());
		}

		@Test
		void f03_listifyMapWithNullValues() {
			var listifier = Listifiers.mapListifier();
			var input = new HashMap<String, String>();
			input.put("key1", "value1");
			input.put("key2", null);
			var result = listifier.apply(null, input);

			assertEquals(2, result.size());
			// Check that we have the expected entries
			var hasKey1Entry = result.stream()
				.filter(obj -> obj instanceof Map.Entry)
				.map(obj -> (Map.Entry<?, ?>) obj)
				.anyMatch(entry -> "key1".equals(entry.getKey()) && "value1".equals(entry.getValue()));
			assertTrue(hasKey1Entry);

			var hasKey2Entry = result.stream()
				.filter(obj -> obj instanceof Map.Entry)
				.map(obj -> (Map.Entry<?, ?>) obj)
				.anyMatch(entry -> "key2".equals(entry.getKey()) && entry.getValue() == null);
			assertTrue(hasKey2Entry);
		}

		@Test
		void f04_listifyWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var listifier = Listifiers.mapListifier();
			var input = Map.of("a", 1, "b", 2);
			var result = listifier.apply(converter, input);

			assertEquals(2, result.size());
			assertTrue(result.stream().allMatch(obj -> obj instanceof Map.Entry));
		}
	}

	@Nested
	class G_integration extends TestBase {

		@Test
		void g01_useInBasicBeanConverter() {
			// Test various listifiable objects
			assertList(List.of("a", "b"), "a", "b");
			assertList(new LinkedHashSet<>(Arrays.asList("x", "y")), "x", "y");
			assertSize(3, Stream.of(1, 2, 3));
			assertEmpty(Optional.empty());
		}

		@Test
		void g02_customListifierRegistration() {
			// Test that custom registration works
			assertList(List.of("custom"), "custom");
		}

		@Test
		void g03_listifierChaining() {
			// Test that listified objects can be processed by other listifiers
			// Stream of optionals
			var streamOfOptionals = Stream.of(Optional.of("a"), Optional.empty(), Optional.of("b"));
			assertSize(3, streamOfOptionals);
		}
	}
}
