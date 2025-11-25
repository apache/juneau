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

import static org.apache.juneau.common.utils.StateEnum.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link StateEnum}.
 */
class StateEnum_Test extends TestBase {

	//====================================================================================================
	// Enum values() tests
	//====================================================================================================

	@Test
	void a01_values_returnsAllStates() {
		var values = StateEnum.values();
		assertEquals(50, values.length);
		assertEquals(S1, values[0]);
		assertEquals(S50, values[49]);
	}

	@Test
	void a02_values_containsAllStates() {
		var values = StateEnum.values();
		assertEquals(S1, values[0]);
		assertEquals(S2, values[1]);
		assertEquals(S3, values[2]);
		assertEquals(S10, values[9]);
		assertEquals(S25, values[24]);
		assertEquals(S50, values[49]);
	}

	//====================================================================================================
	// valueOf() tests
	//====================================================================================================

	@Test
	void b01_valueOf_validNames() {
		assertEquals(S1, StateEnum.valueOf("S1"));
		assertEquals(S2, StateEnum.valueOf("S2"));
		assertEquals(S10, StateEnum.valueOf("S10"));
		assertEquals(S25, StateEnum.valueOf("S25"));
		assertEquals(S50, StateEnum.valueOf("S50"));
	}

	@Test
	void b02_valueOf_invalidName() {
		assertThrows(IllegalArgumentException.class, () -> {
			StateEnum.valueOf("INVALID");
		});
	}

	@Test
	void b03_valueOf_null() {
		assertThrows(NullPointerException.class, () -> {
			StateEnum.valueOf(null);
		});
	}

	@Test
	void b04_valueOf_caseSensitive() {
		assertThrows(IllegalArgumentException.class, () -> {
			StateEnum.valueOf("s1");
		});
		assertThrows(IllegalArgumentException.class, () -> {
			StateEnum.valueOf("S51");
		});
	}

	//====================================================================================================
	// isAny() tests
	//====================================================================================================

	@Test
	void c01_isAny_singleMatch() {
		assertTrue(S1.isAny(S1));
		assertTrue(S2.isAny(S2));
		assertTrue(S10.isAny(S10));
		assertTrue(S50.isAny(S50));
	}

	@Test
	void c02_isAny_singleNoMatch() {
		assertFalse(S1.isAny(S2));
		assertFalse(S2.isAny(S1));
		assertFalse(S10.isAny(S20));
		assertFalse(S50.isAny(S1));
	}

	@Test
	void c03_isAny_multipleMatch() {
		assertTrue(S1.isAny(S1, S2, S3));
		assertTrue(S2.isAny(S1, S2, S3));
		assertTrue(S3.isAny(S1, S2, S3));
	}

	@Test
	void c04_isAny_multipleNoMatch() {
		assertFalse(S1.isAny(S2, S3, S4));
		assertFalse(S10.isAny(S1, S2, S3));
		assertFalse(S50.isAny(S1, S2, S3));
	}

	@Test
	void c05_isAny_matchInMiddle() {
		assertTrue(S2.isAny(S1, S2, S3));
		assertTrue(S25.isAny(S10, S25, S40));
	}

	@Test
	void c06_isAny_matchAtEnd() {
		assertTrue(S3.isAny(S1, S2, S3));
		assertTrue(S50.isAny(S1, S10, S50));
	}

	@Test
	void c07_isAny_matchAtStart() {
		assertTrue(S1.isAny(S1, S2, S3));
		assertTrue(S1.isAny(S1, S10, S50));
	}

	@Test
	void c08_isAny_emptyArray() {
		assertFalse(S1.isAny());
		assertFalse(S10.isAny());
		assertFalse(S50.isAny());
	}

	@Test
	void c09_isAny_duplicateValues() {
		assertTrue(S1.isAny(S1, S1, S1));
		assertTrue(S2.isAny(S1, S2, S2, S3));
		assertFalse(S3.isAny(S1, S1, S2, S2));
	}

	@Test
	void c10_isAny_allStates() {
		var allStates = StateEnum.values();
		for (var state : allStates) {
			assertTrue(state.isAny(allStates));
		}
	}

	@Test
	void c11_isAny_sameStateMultipleTimes() {
		assertTrue(S1.isAny(S1, S1, S1, S1));
		assertTrue(S25.isAny(S25, S25, S25));
	}

	@Test
	void c12_isAny_largeArray() {
		assertTrue(S10.isAny(S1, S2, S3, S4, S5, S6, S7, S8, S9, S10));
		assertFalse(S11.isAny(S1, S2, S3, S4, S5, S6, S7, S8, S9, S10));
	}

	//====================================================================================================
	// Enum standard methods tests
	//====================================================================================================

	@Test
	void d01_name_returnsCorrectName() {
		assertEquals("S1", S1.name());
		assertEquals("S2", S2.name());
		assertEquals("S10", S10.name());
		assertEquals("S50", S50.name());
	}

	@Test
	void d02_ordinal_returnsCorrectOrdinal() {
		assertEquals(0, S1.ordinal());
		assertEquals(1, S2.ordinal());
		assertEquals(9, S10.ordinal());
		assertEquals(24, S25.ordinal());
		assertEquals(49, S50.ordinal());
	}

	@Test
	void d03_equals_sameInstance() {
		assertEquals(S1, S1);
		assertEquals(S10, S10);
		assertEquals(S50, S50);
	}

	@Test
	void d04_equals_differentInstances() {
		assertNotEquals(S1, S2);
		assertNotEquals(S10, S20);
		assertNotEquals(S1, S50);
	}

	@Test
	void d05_hashCode_consistency() {
		assertEquals(S1.hashCode(), S1.hashCode());
		assertEquals(S10.hashCode(), S10.hashCode());
		assertEquals(S50.hashCode(), S50.hashCode());
	}

	@Test
	void d06_hashCode_differentStates() {
		// Different states may have same hashcode, but same state should have same hashcode
		assertEquals(S1.hashCode(), S1.hashCode());
	}

	@Test
	void d07_compareTo_ordering() {
		assertTrue(S1.compareTo(S2) < 0);
		assertTrue(S2.compareTo(S1) > 0);
		assertTrue(S1.compareTo(S1) == 0);
		assertTrue(S10.compareTo(S20) < 0);
		assertTrue(S50.compareTo(S1) > 0);
	}

	@Test
	void d08_compareTo_allStates() {
		var values = StateEnum.values();
		for (var i = 0; i < values.length - 1; i++) {
			assertTrue(values[i].compareTo(values[i + 1]) < 0);
		}
	}

	@Test
	void d09_toString_returnsName() {
		assertEquals("S1", S1.toString());
		assertEquals("S2", S2.toString());
		assertEquals("S10", S10.toString());
		assertEquals("S50", S50.toString());
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void e01_edgeCase_firstAndLast() {
		assertTrue(S1.isAny(S1, S50));
		assertTrue(S50.isAny(S1, S50));
		assertFalse(S25.isAny(S1, S50));
	}

	@Test
	void e02_edgeCase_allStatesInOrder() {
		var allStates = StateEnum.values();
		for (var i = 0; i < allStates.length; i++) {
			assertTrue(allStates[i].isAny(allStates));
		}
	}

	@Test
	void e03_edgeCase_singleStateArray() {
		assertTrue(S1.isAny(S1));
		assertTrue(S25.isAny(S25));
		assertTrue(S50.isAny(S50));
	}

	@Test
	void e04_edgeCase_sequentialStates() {
		assertTrue(S5.isAny(S1, S2, S3, S4, S5, S6, S7, S8, S9, S10));
		assertFalse(S15.isAny(S1, S2, S3, S4, S5, S6, S7, S8, S9, S10));
	}

	@Test
	void e05_edgeCase_nonSequentialStates() {
		assertTrue(S10.isAny(S1, S5, S10, S15, S20));
		assertFalse(S12.isAny(S1, S5, S10, S15, S20));
	}
}

