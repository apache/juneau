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
package org.apache.juneau.commons.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link AsciiSet}.
 */
class AsciiSet_Test extends TestBase {

	//====================================================================================================
	// of(String) - Static factory
	//====================================================================================================
	@Test
	void a01_of_basic() {
		var set = AsciiSet.of("abc");
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertFalse(set.contains('d'));
	}

	@Test
	void a02_of_empty() {
		var set = AsciiSet.of("");
		assertFalse(set.contains('a'));
	}

	@Test
	void a03_of_withNonAscii() {
		var set = AsciiSet.of("abc\u1234");
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertFalse(set.contains('\u1234')); // Non-ASCII ignored
	}

	@Test
	void a04_of_duplicates() {
		var set = AsciiSet.of("aabbcc");
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
	}

	//====================================================================================================
	// create() - Builder factory
	//====================================================================================================
	@Test
	void a05_create_basic() {
		var builder = AsciiSet.create();
		assertNotNull(builder);
		var set = builder.build();
		assertNotNull(set);
		assertFalse(set.contains('a'));
	}

	//====================================================================================================
	// Builder.chars(char...) - Varargs
	//====================================================================================================
	@Test
	void b01_builderChars_varargs_basic() {
		var set = AsciiSet.create()
			.chars('a', 'b', 'c')
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertFalse(set.contains('d'));
	}

	@Test
	void b02_builderChars_varargs_empty() {
		var set = AsciiSet.create()
			.chars()
			.build();
		assertFalse(set.contains('a'));
	}

	@Test
	void b03_builderChars_varargs_single() {
		var set = AsciiSet.create()
			.chars('a')
			.build();
		assertTrue(set.contains('a'));
		assertFalse(set.contains('b'));
	}

	@Test
	void b04_builderChars_varargs_withNonAscii() {
		var set = AsciiSet.create()
			.chars('a', 'b', '\u1234', 'c')
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertFalse(set.contains('\u1234')); // Non-ASCII ignored
	}

	@Test
	void b05_builderChars_varargs_chaining() {
		var set = AsciiSet.create()
			.chars('a', 'b')
			.chars('c', 'd')
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertTrue(set.contains('d'));
	}

	@Test
	void b06_builderChars_varargs_boundary() {
		var set = AsciiSet.create()
			.chars((char)0, (char)127)
			.build();
		assertTrue(set.contains((char)0));
		assertTrue(set.contains((char)127));
	}

	@Test
	void b07_builderChars_varargs_above127() {
		var set = AsciiSet.create()
			.chars((char)128, (char)255)
			.build();
		assertFalse(set.contains((char)128));
		assertFalse(set.contains((char)255));
	}

	//====================================================================================================
	// Builder.chars(String) - String
	//====================================================================================================
	@Test
	void c01_builderChars_string_basic() {
		var set = AsciiSet.create()
			.chars("abc")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
	}

	@Test
	void c02_builderChars_string_empty() {
		var set = AsciiSet.create()
			.chars("")
			.build();
		assertFalse(set.contains('a'));
	}

	@Test
	void c03_builderChars_string_withNonAscii() {
		var set = AsciiSet.create()
			.chars("abc\u1234")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertFalse(set.contains('\u1234')); // Non-ASCII ignored
	}

	@Test
	void c04_builderChars_string_duplicates() {
		var set = AsciiSet.create()
			.chars("aabbcc")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
	}

	@Test
	void c05_builderChars_string_chaining() {
		var set = AsciiSet.create()
			.chars("ab")
			.chars("cd")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertTrue(set.contains('d'));
	}

	@Test
	void c06_builderChars_string_boundary() {
		var set = AsciiSet.create()
			.chars(String.valueOf((char)0) + (char)127)
			.build();
		assertTrue(set.contains((char)0));
		assertTrue(set.contains((char)127));
	}

	//====================================================================================================
	// Builder.range(char, char) - Range
	//====================================================================================================
	@Test
	void d01_builderRange_basic() {
		var set = AsciiSet.create()
			.range('a', 'c')
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertFalse(set.contains('d'));
	}

	@Test
	void d02_builderRange_single() {
		var set = AsciiSet.create()
			.range('a', 'a')
			.build();
		assertTrue(set.contains('a'));
		assertFalse(set.contains('b'));
	}

	@Test
	void d03_builderRange_reverse() {
		var set = AsciiSet.create()
			.range('c', 'a')
			.build();
		// When start > end, the loop doesn't execute
		assertFalse(set.contains('a'));
		assertFalse(set.contains('b'));
		assertFalse(set.contains('c'));
	}

	@Test
	void d04_builderRange_boundary() {
		var set = AsciiSet.create()
			.range((char)0, (char)127)
			.build();
		assertTrue(set.contains((char)0));
		assertTrue(set.contains((char)127));
		assertTrue(set.contains((char)64)); // Middle
	}

	@Test
	void d05_builderRange_crossingAsciiBoundary() {
		var set = AsciiSet.create()
			.range('a', (char)200)
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains((char)127));
		// Characters > 127 are ignored
		assertFalse(set.contains((char)128));
		assertFalse(set.contains((char)200));
	}

	@Test
	void d06_builderRange_chaining() {
		var set = AsciiSet.create()
			.range('a', 'c')
			.range('x', 'z')
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertTrue(set.contains('x'));
		assertTrue(set.contains('y'));
		assertTrue(set.contains('z'));
		assertFalse(set.contains('d'));
	}

	@Test
	void d07_builderRange_overlapping() {
		var set = AsciiSet.create()
			.range('a', 'd')
			.range('c', 'f')
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertTrue(set.contains('d'));
		assertTrue(set.contains('e'));
		assertTrue(set.contains('f'));
	}

	//====================================================================================================
	// Builder.ranges(String...) - Multiple ranges
	//====================================================================================================
	@Test
	void e01_builderRanges_basic() {
		var set = AsciiSet.create()
			.ranges("a-c", "x-z")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertTrue(set.contains('x'));
		assertTrue(set.contains('y'));
		assertTrue(set.contains('z'));
		assertFalse(set.contains('d'));
	}

	@Test
	void e02_builderRanges_single() {
		var set = AsciiSet.create()
			.ranges("a-c")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
	}

	@Test
	void e03_builderRanges_empty() {
		var set = AsciiSet.create()
			.ranges()
			.build();
		assertFalse(set.contains('a'));
	}

	@Test
	void e04_builderRanges_singleChar() {
		var set = AsciiSet.create()
			.ranges("a-a")
			.build();
		assertTrue(set.contains('a'));
		assertFalse(set.contains('b'));
	}

	@Test
	void e05_builderRanges_chaining() {
		var set = AsciiSet.create()
			.ranges("a-c")
			.ranges("x-z")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertTrue(set.contains('x'));
		assertTrue(set.contains('y'));
		assertTrue(set.contains('z'));
	}

	@Test
	void e06_builderRanges_invalidLength() {
		assertThrows(IllegalArgumentException.class, () -> {
			AsciiSet.create().ranges("ab"); // Too short
		});
		assertThrows(IllegalArgumentException.class, () -> {
			AsciiSet.create().ranges("abcd"); // Too long
		});
	}

	@Test
	void e07_builderRanges_invalidFormat() {
		assertThrows(IllegalArgumentException.class, () -> {
			AsciiSet.create().ranges("a_b"); // Wrong separator
		});
		assertThrows(IllegalArgumentException.class, () -> {
			AsciiSet.create().ranges("a b"); // Space instead of dash
		});
	}

	@Test
	void e08_builderRanges_validFormat() {
		var set = AsciiSet.create()
			.ranges("a-z", "0-9", "A-Z")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('z'));
		assertTrue(set.contains('0'));
		assertTrue(set.contains('9'));
		assertTrue(set.contains('A'));
		assertTrue(set.contains('Z'));
	}

	//====================================================================================================
	// Builder.build() - Build
	//====================================================================================================
	@Test
	void f01_build_empty() {
		var set = AsciiSet.create().build();
		assertNotNull(set);
		assertFalse(set.contains('a'));
	}

	@Test
	void f02_build_immutable() {
		var builder = AsciiSet.create();
		builder.chars('a');
		var set1 = builder.build();
		builder.chars('b');
		var set2 = builder.build();
		// set1 should not be affected by builder changes
		assertTrue(set1.contains('a'));
		assertFalse(set1.contains('b'));
		// set2 should have both
		assertTrue(set2.contains('a'));
		assertTrue(set2.contains('b'));
	}

	//====================================================================================================
	// contains(char) - Character
	//====================================================================================================
	@Test
	void g01_contains_char_present() {
		var set = AsciiSet.of("abc");
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
	}

	@Test
	void g02_contains_char_notPresent() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains('d'));
		assertFalse(set.contains('x'));
	}

	@Test
	void g03_contains_char_boundary() {
		var set = AsciiSet.create()
			.chars((char)0, (char)127)
			.build();
		assertTrue(set.contains((char)0));
		assertTrue(set.contains((char)127));
	}

	@Test
	void g04_contains_char_nonAscii() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains((char)128));
		assertFalse(set.contains('€'));
	}

	@Test
	void g05_contains_char_negative() {
		var set = AsciiSet.of("abc");
		// Negative char values are not valid, but let's test the behavior
		// In Java, char is unsigned, so this would be a large positive value
		assertFalse(set.contains((char)-1));
	}

	//====================================================================================================
	// contains(int) - Integer
	//====================================================================================================
	@Test
	void h01_contains_int_present() {
		var set = AsciiSet.of("abc");
		assertTrue(set.contains((int)'a'));
		assertTrue(set.contains((int)'b'));
		assertTrue(set.contains((int)'c'));
	}

	@Test
	void h02_contains_int_notPresent() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains((int)'d'));
	}

	@Test
	void h03_contains_int_boundary() {
		var set = AsciiSet.create()
			.chars((char)0, (char)127)
			.build();
		assertTrue(set.contains(0));
		assertTrue(set.contains(127));
	}

	@Test
	void h04_contains_int_negative() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains(-1));
	}

	@Test
	void h05_contains_int_above127() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains(128));
		assertFalse(set.contains(1000));
	}

	//====================================================================================================
	// contains(CharSequence) - CharSequence
	//====================================================================================================
	@Test
	void i01_contains_CharSequence_present() {
		var set = AsciiSet.of("abc");
		assertTrue(set.contains("abc"));
		assertTrue(set.contains("xyzabc"));
	}

	@Test
	void i02_contains_CharSequence_notPresent() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains("xyz"));
		assertFalse(set.contains("def"));
	}

	@Test
	void i03_contains_CharSequence_null() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains((CharSequence)null));
	}

	@Test
	void i04_contains_CharSequence_empty() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains(""));
	}

	@Test
	void i05_contains_CharSequence_firstChar() {
		var set = AsciiSet.of("abc");
		assertTrue(set.contains("abc"));
	}

	@Test
	void i06_contains_CharSequence_middleChar() {
		var set = AsciiSet.of("b");
		assertTrue(set.contains("abc"));
	}

	@Test
	void i07_contains_CharSequence_lastChar() {
		var set = AsciiSet.of("c");
		assertTrue(set.contains("abc"));
	}

	@Test
	void i08_contains_CharSequence_singleChar() {
		var set = AsciiSet.of("a");
		assertTrue(set.contains("a"));
	}

	@Test
	void i09_contains_CharSequence_StringBuilder() {
		var set = AsciiSet.of("abc");
		var sb = new StringBuilder("abc");
		assertTrue(set.contains(sb));
	}

	@Test
	void i10_contains_CharSequence_StringBuffer() {
		var set = AsciiSet.of("abc");
		var sb = new StringBuffer("abc");
		assertTrue(set.contains(sb));
	}

	@Test
	void i11_contains_CharSequence_withNonAscii() {
		var set = AsciiSet.of("abc");
		// String contains non-ASCII character, but also contains 'a'
		assertTrue(set.contains("a€"));
	}

	@Test
	void i12_contains_CharSequence_onlyNonAscii() {
		var set = AsciiSet.of("abc");
		assertFalse(set.contains("€"));
	}

	//====================================================================================================
	// containsOnly(String) - Contains only
	//====================================================================================================
	@Test
	void j01_containsOnly_allPresent() {
		var set = AsciiSet.of("abc");
		assertTrue(set.containsOnly("abc"));
		assertTrue(set.containsOnly("aabbcc"));
	}

	@Test
	void j02_containsOnly_someNotPresent() {
		var set = AsciiSet.of("abc");
		assertFalse(set.containsOnly("abcd"));
		assertFalse(set.containsOnly("xyz"));
	}

	@Test
	void j03_containsOnly_null() {
		var set = AsciiSet.of("abc");
		assertFalse(set.containsOnly(null));
	}

	@Test
	void j04_containsOnly_empty() {
		var set = AsciiSet.of("abc");
		// Empty string should return true (all characters in empty string are in the set)
		assertTrue(set.containsOnly(""));
	}

	@Test
	void j05_containsOnly_singleChar() {
		var set = AsciiSet.of("abc");
		assertTrue(set.containsOnly("a"));
		assertFalse(set.containsOnly("d"));
	}

	@Test
	void j06_containsOnly_withNonAscii() {
		var set = AsciiSet.of("abc");
		// Non-ASCII characters are not in the set
		assertFalse(set.containsOnly("abc€"));
	}

	@Test
	void j07_containsOnly_onlyNonAscii() {
		var set = AsciiSet.of("abc");
		// Empty string after filtering non-ASCII, but original is not empty
		// Actually, containsOnly checks each char, so non-ASCII will fail
		assertFalse(set.containsOnly("€"));
	}

	@Test
	void j08_containsOnly_range() {
		var set = AsciiSet.create()
			.range('a', 'z')
			.build();
		assertTrue(set.containsOnly("abcdefghijklmnopqrstuvwxyz"));
		assertFalse(set.containsOnly("abc123"));
	}

	//====================================================================================================
	// copy() - Copy
	//====================================================================================================
	@Test
	void k01_copy_basic() {
		var original = AsciiSet.of("abc");
		var builder = original.copy();
		var copy = builder.build();
		assertTrue(copy.contains('a'));
		assertTrue(copy.contains('b'));
		assertTrue(copy.contains('c'));
	}

	@Test
	void k02_copy_modifyBuilder() {
		var original = AsciiSet.of("abc");
		var builder = original.copy();
		builder.chars('d');
		var modified = builder.build();
		assertTrue(modified.contains('a'));
		assertTrue(modified.contains('b'));
		assertTrue(modified.contains('c'));
		assertTrue(modified.contains('d'));
		// Original should be unchanged
		assertTrue(original.contains('a'));
		assertFalse(original.contains('d'));
	}

	@Test
	void k03_copy_independent() {
		var original = AsciiSet.of("abc");
		var copy1 = original.copy().build();
		var copy2 = original.copy().chars('d').build();
		assertTrue(copy1.contains('a'));
		assertFalse(copy1.contains('d'));
		assertTrue(copy2.contains('a'));
		assertTrue(copy2.contains('d'));
	}

	@Test
	void k04_copy_empty() {
		var original = AsciiSet.create().build();
		var copy = original.copy().build();
		assertFalse(copy.contains('a'));
	}

	@Test
	void k05_copy_fullRange() {
		var original = AsciiSet.create()
			.range('a', 'z')
			.build();
		var copy = original.copy().build();
		for (char c = 'a'; c <= 'z'; c++) {
			assertTrue(copy.contains(c));
		}
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================
	@Test
	void l01_integration_complexBuilder() {
		var set = AsciiSet.create()
			.chars('a', 'b', 'c')
			.chars("def")
			.range('g', 'i')
			.ranges("j-l", "m-o")
			.build();
		assertTrue(set.contains('a'));
		assertTrue(set.contains('b'));
		assertTrue(set.contains('c'));
		assertTrue(set.contains('d'));
		assertTrue(set.contains('e'));
		assertTrue(set.contains('f'));
		assertTrue(set.contains('g'));
		assertTrue(set.contains('h'));
		assertTrue(set.contains('i'));
		assertTrue(set.contains('j'));
		assertTrue(set.contains('k'));
		assertTrue(set.contains('l'));
		assertTrue(set.contains('m'));
		assertTrue(set.contains('n'));
		assertTrue(set.contains('o'));
		assertFalse(set.contains('p'));
	}

	@Test
	void l02_integration_containsAndContainsOnly() {
		var set = AsciiSet.of("abc");
		assertTrue(set.contains("abc"));
		assertTrue(set.containsOnly("abc"));
		assertTrue(set.contains("xyzabc"));
		assertFalse(set.containsOnly("xyzabc"));
	}

	@Test
	void l03_integration_copyAndModify() {
		var original = AsciiSet.create()
			.range('a', 'c')
			.build();
		var modified = original.copy()
			.range('d', 'f')
			.build();
		assertTrue(modified.contains('a'));
		assertTrue(modified.contains('b'));
		assertTrue(modified.contains('c'));
		assertTrue(modified.contains('d'));
		assertTrue(modified.contains('e'));
		assertTrue(modified.contains('f'));
		assertFalse(original.contains('d'));
	}
}

