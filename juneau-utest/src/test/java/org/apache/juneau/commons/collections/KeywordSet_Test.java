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
package org.apache.juneau.commons.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class KeywordSet_Test extends TestBase {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test void a01_test() {
		var ks = new KeywordSet("aaa", "zzz");
		assertTrue(ks.contains("aaa"));
		assertTrue(ks.contains("zzz"));
		assertFalse(ks.contains("xxx"));
		assertFalse(ks.contains("aaaa"));
		assertFalse(ks.contains("zzzz"));
		assertFalse(ks.contains("\u0000\u1000"));
		assertFalse(ks.contains("z"));
		assertFalse(ks.contains(null));
		assertFalse(ks.contains("a|"));
		assertFalse(ks.contains("|a"));
		assertFalse(ks.contains("Aa"));
		assertFalse(ks.contains("aA"));
	}

	//====================================================================================================
	// toString(), equals(), hashCode()
	//====================================================================================================

	@Test
	void w01_toString_standardFormat() {
		var ks = new KeywordSet("apple", "banana", "cherry");
		var result = ks.toString();

		// Should be in sorted order: [apple, banana, cherry]
		assertTrue(result.startsWith("["));
		assertTrue(result.endsWith("]"));
		assertTrue(result.contains("apple"));
		assertTrue(result.contains("banana"));
		assertTrue(result.contains("cherry"));
	}

	@Test
	void w02_toString_emptySet() {
		var ks = new KeywordSet();
		assertEquals("[]", ks.toString());
	}

	@Test
	void w03_toString_singleKeyword() {
		var ks = new KeywordSet("test");
		assertEquals("[test]", ks.toString());
	}

	@Test
	void w04_equals_sameContents() {
		var ks1 = new KeywordSet("apple", "banana", "cherry");
		var ks2 = new KeywordSet("apple", "banana", "cherry");

		assertTrue(ks1.equals(ks2));
		assertTrue(ks2.equals(ks1));
	}

	@Test
	void w05_equals_differentOrder() {
		var ks1 = new KeywordSet("apple", "banana", "cherry");
		var ks2 = new KeywordSet("cherry", "apple", "banana");

		// Should be equal because both are sorted internally
		assertTrue(ks1.equals(ks2));
		assertTrue(ks2.equals(ks1));
	}

	@Test
	void w06_equals_differentContents() {
		var ks1 = new KeywordSet("apple", "banana");
		var ks2 = new KeywordSet("apple", "cherry");

		assertFalse(ks1.equals(ks2));
		assertFalse(ks2.equals(ks1));
	}

	@Test
	void w07_equals_differentSizes() {
		var ks1 = new KeywordSet("apple", "banana");
		var ks2 = new KeywordSet("apple", "banana", "cherry");

		assertFalse(ks1.equals(ks2));
		assertFalse(ks2.equals(ks1));
	}

	@Test
	void w08_equals_notAKeywordSet() {
		var ks = new KeywordSet("apple", "banana");

		assertFalse(ks.equals(null));
	}

	@Test
	void w09_equals_emptySets() {
		var ks1 = new KeywordSet();
		var ks2 = new KeywordSet();

		assertTrue(ks1.equals(ks2));
		assertTrue(ks2.equals(ks1));
	}

	@Test
	void w10_hashCode_sameContents() {
		var ks1 = new KeywordSet("apple", "banana", "cherry");
		var ks2 = new KeywordSet("apple", "banana", "cherry");

		assertEquals(ks1.hashCode(), ks2.hashCode());
	}

	@Test
	void w11_hashCode_differentOrder() {
		var ks1 = new KeywordSet("apple", "banana", "cherry");
		var ks2 = new KeywordSet("cherry", "apple", "banana");

		// Should have same hash code because both are sorted internally
		assertEquals(ks1.hashCode(), ks2.hashCode());
	}

	@Test
	void w12_hashCode_emptySets() {
		var ks1 = new KeywordSet();
		var ks2 = new KeywordSet();

		assertEquals(ks1.hashCode(), ks2.hashCode());
	}
}

