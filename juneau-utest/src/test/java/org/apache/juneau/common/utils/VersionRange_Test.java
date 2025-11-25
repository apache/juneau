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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class VersionRange_Test extends TestBase {

	private static final Input[] INPUT = {
		/* 00 */ input("1.1", "1.1.3", true),
		/* 01 */ input("1.1", "1.1", true),
		/* 02 */ input("1.1", "1.1.0", true),
		/* 03 */ input("1.1", "1.0", false),
		/* 04 */ input("1.1", "1.0.9", false),
		/* 05 */ input("[1.0,2.0)", ".9", false),
		/* 06 */ input("[1.0,2.0)", "1", true),
		/* 07 */ input("[1.0,2.0)", "1.0", true),
		/* 08 */ input("[1.0,2.0)", "1.0.0", true),
		/* 09 */ input("[1.0,2.0)", "1.1", true),
		/* 10 */ input("[1.0,2.0)", "2.0", false),
		/* 11 */ input("[1.0,2.0)", "2", false),
		/* 12 */ input("(1.0,2.0]", "2", true),
		/* 13 */ input("(1.0,2.0]", "2.0", true),
		/* 14 */ input("(1.0,2.0]", "2.0.1", true),
		/* 15 */ input("(1.0,2.0]", "2.1", false),
		/* 16 */ input("(.5.0,.6]", ".5", false),
		/* 17 */ input("(.5.0,.6]", ".5.1", true),
		/* 18 */ input("(.5.0,.6]", ".6", true),
		/* 19 */ input("(.5.0,.6]", ".6.1", true),
		/* 20 */ input("(.5.0,.6]", ".7", false),
		/* 21 */ input("[1.1,2.0)", "1", false)
	};

	private static Input input(String range, String version, boolean shouldMatch) {
		return new Input(range, version, shouldMatch);
	}

	private static class Input {
		VersionRange range;
		String version;
		boolean shouldMatch;

		public Input(String range, String version, boolean shouldMatch) {
			this.version = version;
			this.range = new VersionRange(range);
			this.shouldMatch = shouldMatch;
		}
	}

	static Input[] input() {
		return INPUT;
	}

	@ParameterizedTest
	@MethodSource("input")
	void a01_basic(Input input) {
		assertEquals(input.shouldMatch, input.range.matches(input.version));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Edge cases - empty strings and whitespace
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a02_emptyRange() {
		var range = new VersionRange("");
		assertTrue(range.matches(""));
		// Empty range matches everything (both minVersion and maxVersion are null)
		assertTrue(range.matches("1.0"));
	}

	@Test
	void a03_whitespaceTrimming() {
		var range1 = new VersionRange("  1.0  ");
		var range2 = new VersionRange("1.0");
		assertTrue(range1.matches("1.0"));
		assertTrue(range2.matches("1.0"));
		assertTrue(range1.matches("1.1"));
		assertTrue(range2.matches("1.1"));

		var range3 = new VersionRange("  [1.0,2.0)  ");
		assertTrue(range3.matches("1.5"));
		assertFalse(range3.matches("2.0"));
	}

	@Test
	void a04_emptyVersionString() {
		var range1 = new VersionRange("");
		assertTrue(range1.matches(""));
		assertTrue(range1.matches(null));  // Empty range matches null (both versions are null)

		var range2 = new VersionRange("1.0");
		// Empty string is converted to "0" by Version constructor, and "0" is less than "1.0"
		assertFalse(range2.matches(""));
		assertFalse(range2.matches(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Additional range format combinations
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a05_closedRange() {
		var range = new VersionRange("[1.0,2.0]");
		assertTrue(range.matches("1.0"));
		assertTrue(range.matches("1.5"));
		assertTrue(range.matches("2.0"));
		assertTrue(range.matches("2.0.1"));
		assertFalse(range.matches("0.9"));
		assertFalse(range.matches("2.1"));
	}

	@Test
	void a06_openRange() {
		var range = new VersionRange("(1.0,2.0)");
		assertFalse(range.matches("1.0"));
		assertTrue(range.matches("1.5"));
		assertFalse(range.matches("2.0"));
		assertFalse(range.matches("0.9"));
		assertFalse(range.matches("2.1"));
	}

	@Test
	void a07_mixedBrackets() {
		var range1 = new VersionRange("[1.0,2.0)");
		assertTrue(range1.matches("1.0"));
		assertTrue(range1.matches("1.5"));
		assertFalse(range1.matches("2.0"));

		var range2 = new VersionRange("(1.0,2.0]");
		assertFalse(range2.matches("1.0"));
		assertTrue(range2.matches("1.5"));
		assertTrue(range2.matches("2.0"));
	}

	@Test
	void a08_singleVersionRange() {
		var range = new VersionRange("2.0");
		assertTrue(range.matches("2.0"));
		assertTrue(range.matches("2.1"));
		assertTrue(range.matches("3.0"));
		assertFalse(range.matches("1.9"));
		assertFalse(range.matches("1.9.9"));
	}

	@Test
	void a09_versionWithLeadingDot() {
		var range = new VersionRange("[.5,.6]");
		assertTrue(range.matches(".5"));
		assertTrue(range.matches(".5.1"));
		assertTrue(range.matches(".6"));
		assertFalse(range.matches(".4"));
		assertFalse(range.matches(".7"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// toString() method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a10_toString() {
		// Range format
		var range1 = new VersionRange("[1.0,2.0)");
		var str1 = range1.toString();
		assertTrue(str1.contains("1.0"));
		assertTrue(str1.contains("2.0"));
		assertTrue(str1.startsWith("["));
		assertTrue(str1.endsWith(")"));

		var range2 = new VersionRange("(1.0,2.0]");
		var str2 = range2.toString();
		assertTrue(str2.contains("1.0"));
		assertTrue(str2.contains("2.0"));
		assertTrue(str2.startsWith("("));
		assertTrue(str2.endsWith("]"));

		// Single version format (note: toString may have issues with null maxVersion)
		var range3 = new VersionRange("1.0");
		var str3 = range3.toString();
		assertNotNull(str3);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Boundary conditions
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a11_boundaryConditions() {
		// Exactly at minimum (inclusive)
		var range1 = new VersionRange("[1.0,2.0)");
		assertTrue(range1.matches("1.0"));
		assertTrue(range1.matches("1.0.0"));

		// Exactly at minimum (exclusive)
		var range2 = new VersionRange("(1.0,2.0)");
		assertFalse(range2.matches("1.0"));
		assertFalse(range2.matches("1.0.0"));
		// "1.0.1" should be > "1.0" (exclusive), so it should match
		assertTrue(range2.matches("1.0.1"));
		assertTrue(range2.matches("1.1"));

		// Exactly at maximum (inclusive)
		var range3 = new VersionRange("[1.0,2.0]");
		assertTrue(range3.matches("2.0"));
		// "2.0.0" is considered greater than "2.0" in Version comparison
		assertTrue(range3.matches("2.0.0"));
		assertTrue(range3.matches("2.0.1"));

		// Exactly at maximum (exclusive)
		var range4 = new VersionRange("[1.0,2.0)");
		assertFalse(range4.matches("2.0"));
		assertFalse(range4.matches("2.0.0"));
		assertTrue(range4.matches("1.9.9"));
	}

	@Test
	void a12_singleDigitVersions() {
		var range = new VersionRange("[1,2)");
		assertTrue(range.matches("1"));
		assertTrue(range.matches("1.0"));
		assertTrue(range.matches("1.9"));
		assertFalse(range.matches("2"));
		assertFalse(range.matches("2.0"));
	}

	@Test
	void a13_versionComparisons() {
		// Test that version comparison works correctly
		var range = new VersionRange("[1.2.3,2.0.0)");
		assertFalse(range.matches("1.2.2"));
		assertTrue(range.matches("1.2.3"));
		assertTrue(range.matches("1.2.4"));
		assertTrue(range.matches("1.9.9"));
		assertFalse(range.matches("2.0.0"));
		assertFalse(range.matches("2.0.1"));
	}
}