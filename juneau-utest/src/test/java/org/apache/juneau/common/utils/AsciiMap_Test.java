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
package org.apache.juneau.common.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link AsciiMap}.
 */
class AsciiMap_Test {

	//====================================================================================================
	// append(char, String) - Basic cases
	//====================================================================================================
	@Test
	void a01_append_basic() {
		var map = new AsciiMap();
		var result = map.append('a', "value1");
		assertSame(map, result); // Should return this
		assertEquals("value1", map.get('a'));
	}

	@Test
	void a02_append_multiple() {
		var map = new AsciiMap();
		map.append('a', "value1");
		map.append('b', "value2");
		map.append('c', "value3");
		assertEquals("value1", map.get('a'));
		assertEquals("value2", map.get('b'));
		assertEquals("value3", map.get('c'));
	}

	@Test
	void a03_append_overwrite() {
		var map = new AsciiMap();
		map.append('a', "value1");
		map.append('a', "value2");
		assertEquals("value2", map.get('a'));
	}

	@Test
	void a04_append_nullValue() {
		var map = new AsciiMap();
		map.append('a', null);
		assertNull(map.get('a'));
	}

	@Test
	void a05_append_emptyString() {
		var map = new AsciiMap();
		map.append('a', "");
		assertEquals("", map.get('a'));
	}

	//====================================================================================================
	// append(char, String) - Boundary cases
	//====================================================================================================
	@Test
	void a06_append_asciiBoundary_min() {
		var map = new AsciiMap();
		map.append((char)0, "null");
		assertEquals("null", map.get((char)0));
	}

	@Test
	void a07_append_asciiBoundary_max() {
		var map = new AsciiMap();
		map.append((char)127, "del");
		assertEquals("del", map.get((char)127));
	}

	@Test
	void a08_append_nonAscii_above127() {
		var map = new AsciiMap();
		map.append((char)128, "shouldNotBeStored");
		// Non-ASCII characters are not stored, so contains should return false
		assertFalse(map.contains((char)128));
		// get() will throw ArrayIndexOutOfBoundsException for non-ASCII
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			map.get((char)128);
		});
	}

	@Test
	void a09_append_nonAscii_unicode() {
		var map = new AsciiMap();
		map.append('€', "euro");
		// Non-ASCII characters are not stored, so contains should return false
		assertFalse(map.contains('€'));
		// get() will throw ArrayIndexOutOfBoundsException for non-ASCII
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			map.get('€');
		});
	}

	@Test
	void a10_append_chaining() {
		var map = new AsciiMap();
		map.append('a', "value1")
			.append('b', "value2")
			.append('c', "value3");
		assertEquals("value1", map.get('a'));
		assertEquals("value2", map.get('b'));
		assertEquals("value3", map.get('c'));
	}

	//====================================================================================================
	// contains(char) - Basic cases
	//====================================================================================================
	@Test
	void b01_contains_char_present() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertTrue(map.contains('a'));
	}

	@Test
	void b02_contains_char_notPresent() {
		var map = new AsciiMap();
		assertFalse(map.contains('a'));
	}

	@Test
	void b03_contains_char_nullValue() {
		var map = new AsciiMap();
		map.append('a', null);
		assertFalse(map.contains('a')); // null values are not considered "contained"
	}

	@Test
	void b04_contains_char_emptyString() {
		var map = new AsciiMap();
		map.append('a', "");
		assertTrue(map.contains('a')); // Empty string is still a value
	}

	@Test
	void b05_contains_char_multiple() {
		var map = new AsciiMap();
		map.append('a', "value1");
		map.append('b', "value2");
		map.append('c', "value3");
		assertTrue(map.contains('a'));
		assertTrue(map.contains('b'));
		assertTrue(map.contains('c'));
		assertFalse(map.contains('d'));
	}

	//====================================================================================================
	// contains(char) - Boundary cases
	//====================================================================================================
	@Test
	void b06_contains_char_asciiMin() {
		var map = new AsciiMap();
		map.append((char)0, "null");
		assertTrue(map.contains((char)0));
	}

	@Test
	void b07_contains_char_asciiMax() {
		var map = new AsciiMap();
		map.append((char)127, "del");
		assertTrue(map.contains((char)127));
	}

	@Test
	void b08_contains_char_nonAscii_above127() {
		var map = new AsciiMap();
		assertFalse(map.contains((char)128));
	}

	@Test
	void b09_contains_char_nonAscii_unicode() {
		var map = new AsciiMap();
		assertFalse(map.contains('€'));
	}

	@Test
	void b10_contains_char_negative() {
		var map = new AsciiMap();
		// Negative char values are not valid, but let's test the behavior
		// In Java, char is unsigned, so this would be a large positive value
		assertFalse(map.contains((char)-1));
	}

	//====================================================================================================
	// contains(int) - Basic cases
	//====================================================================================================
	@Test
	void c01_contains_int_present() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertTrue(map.contains((int)'a'));
	}

	@Test
	void c02_contains_int_notPresent() {
		var map = new AsciiMap();
		assertFalse(map.contains((int)'a'));
	}

	@Test
	void c03_contains_int_nullValue() {
		var map = new AsciiMap();
		map.append('a', null);
		assertFalse(map.contains((int)'a'));
	}

	@Test
	void c04_contains_int_emptyString() {
		var map = new AsciiMap();
		map.append('a', "");
		assertTrue(map.contains((int)'a'));
	}

	//====================================================================================================
	// contains(int) - Boundary cases
	//====================================================================================================
	@Test
	void c05_contains_int_asciiMin() {
		var map = new AsciiMap();
		map.append((char)0, "null");
		assertTrue(map.contains(0));
	}

	@Test
	void c06_contains_int_asciiMax() {
		var map = new AsciiMap();
		map.append((char)127, "del");
		assertTrue(map.contains(127));
	}

	@Test
	void c07_contains_int_negative() {
		var map = new AsciiMap();
		assertFalse(map.contains(-1));
	}

	@Test
	void c08_contains_int_above127() {
		var map = new AsciiMap();
		assertFalse(map.contains(128));
	}

	@Test
	void c09_contains_int_largeValue() {
		var map = new AsciiMap();
		assertFalse(map.contains(1000));
	}

	//====================================================================================================
	// contains(CharSequence) - Basic cases
	//====================================================================================================
	@Test
	void d01_contains_CharSequence_present() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertTrue(map.contains("abc"));
	}

	@Test
	void d02_contains_CharSequence_notPresent() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertFalse(map.contains("xyz"));
	}

	@Test
	void d03_contains_CharSequence_null() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertFalse(map.contains((CharSequence)null));
	}

	@Test
	void d04_contains_CharSequence_empty() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertFalse(map.contains(""));
	}

	@Test
	void d05_contains_CharSequence_multipleMatches() {
		var map = new AsciiMap();
		map.append('a', "value1");
		map.append('b', "value2");
		assertTrue(map.contains("abc"));
	}

	@Test
	void d06_contains_CharSequence_firstChar() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertTrue(map.contains("abc"));
	}

	@Test
	void d07_contains_CharSequence_middleChar() {
		var map = new AsciiMap();
		map.append('b', "value2");
		assertTrue(map.contains("abc"));
	}

	@Test
	void d08_contains_CharSequence_lastChar() {
		var map = new AsciiMap();
		map.append('c', "value3");
		assertTrue(map.contains("abc"));
	}

	@Test
	void d09_contains_CharSequence_singleChar() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertTrue(map.contains("a"));
	}

	@Test
	void d10_contains_CharSequence_StringBuilder() {
		var map = new AsciiMap();
		map.append('a', "value1");
		var sb = new StringBuilder("abc");
		assertTrue(map.contains(sb));
	}

	@Test
	void d11_contains_CharSequence_StringBuffer() {
		var map = new AsciiMap();
		map.append('a', "value1");
		var sb = new StringBuffer("abc");
		assertTrue(map.contains(sb));
	}

	//====================================================================================================
	// contains(CharSequence) - Edge cases
	//====================================================================================================
	@Test
	void d12_contains_CharSequence_withNonAscii() {
		var map = new AsciiMap();
		map.append('a', "value1");
		// String contains non-ASCII character, but also contains 'a'
		assertTrue(map.contains("a€"));
	}

	@Test
	void d13_contains_CharSequence_onlyNonAscii() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertFalse(map.contains("€"));
	}

	@Test
	void d14_contains_CharSequence_nullValueInMap() {
		var map = new AsciiMap();
		map.append('a', null);
		// null values don't count as "contained"
		assertFalse(map.contains("a"));
	}

	//====================================================================================================
	// get(char) - Basic cases
	//====================================================================================================
	@Test
	void e01_get_char_present() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertEquals("value1", map.get('a'));
	}

	@Test
	void e02_get_char_notPresent() {
		var map = new AsciiMap();
		assertNull(map.get('a'));
	}

	@Test
	void e03_get_char_nullValue() {
		var map = new AsciiMap();
		map.append('a', null);
		assertNull(map.get('a'));
	}

	@Test
	void e04_get_char_emptyString() {
		var map = new AsciiMap();
		map.append('a', "");
		assertEquals("", map.get('a'));
	}

	@Test
	void e05_get_char_multiple() {
		var map = new AsciiMap();
		map.append('a', "value1");
		map.append('b', "value2");
		map.append('c', "value3");
		assertEquals("value1", map.get('a'));
		assertEquals("value2", map.get('b'));
		assertEquals("value3", map.get('c'));
	}

	//====================================================================================================
	// get(char) - Boundary cases
	//====================================================================================================
	@Test
	void e06_get_char_asciiMin() {
		var map = new AsciiMap();
		map.append((char)0, "null");
		assertEquals("null", map.get((char)0));
	}

	@Test
	void e07_get_char_asciiMax() {
		var map = new AsciiMap();
		map.append((char)127, "del");
		assertEquals("del", map.get((char)127));
	}

	@Test
	void e08_get_char_nonAscii_above127() {
		var map = new AsciiMap();
		// get() doesn't check bounds, so accessing index > 127 will throw ArrayIndexOutOfBoundsException
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			map.get((char)128);
		});
	}

	@Test
	void e09_get_char_unicode() {
		var map = new AsciiMap();
		// Unicode characters > 127 will cause ArrayIndexOutOfBoundsException
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			map.get('€');
		});
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================
	@Test
	void f01_integration_fullAsciiRange() {
		var map = new AsciiMap();
		// Add values for all ASCII characters
		for (int i = 0; i <= 127; i++) {
			map.append((char)i, "value" + i);
		}
		// Verify all can be retrieved
		for (int i = 0; i <= 127; i++) {
			assertEquals("value" + i, map.get((char)i));
			assertTrue(map.contains((char)i));
			assertTrue(map.contains(i));
		}
	}

	@Test
	void f02_integration_containsString() {
		var map = new AsciiMap();
		map.append('a', "value1");
		map.append('b', "value2");
		map.append('c', "value3");
		assertTrue(map.contains("abc"));
		assertTrue(map.contains("xyzabc"));
		assertTrue(map.contains("a"));
		assertFalse(map.contains("xyz"));
		assertFalse(map.contains(""));
	}

	@Test
	void f03_integration_overwriteAndRetrieve() {
		var map = new AsciiMap();
		map.append('a', "value1");
		assertEquals("value1", map.get('a'));
		map.append('a', "value2");
		assertEquals("value2", map.get('a'));
		assertTrue(map.contains('a'));
	}

	@Test
	void f04_integration_mixedOperations() {
		var map = new AsciiMap();
		map.append('a', "value1")
			.append('b', "value2")
			.append('c', "value3");
		assertTrue(map.contains('a'));
		assertTrue(map.contains("abc"));
		assertEquals("value1", map.get('a'));
		assertTrue(map.contains(98)); // 'b' as int
		assertFalse(map.contains('d'));
		assertFalse(map.contains("xyz"));
	}
}

