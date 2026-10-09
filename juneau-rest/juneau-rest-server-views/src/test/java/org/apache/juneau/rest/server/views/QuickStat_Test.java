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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** {@link QuickStat}: one tile in a table's quick-stats strip. */
class QuickStat_Test extends TestBase {

	@Test void a01_of_toMap_minimal() {
		var m = QuickStat.of("unavailable", "Work data", "unavailable").toMap();
		assertEquals("unavailable", m.get("id"));
		assertEquals("Work data", m.get("label"));
		assertEquals("unavailable", m.get("value"));
		assertFalse(m.containsKey("tone"));
	}

	@Test void a02_of_blankId_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> QuickStat.of(" ", "Work data", 1));
		assertEquals("QuickStat id must not be null or blank.", ex.getMessage());
	}

	@Test void a03_of_nullId_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> QuickStat.of(null, "Work data", 1));
		assertEquals("QuickStat id must not be null or blank.", ex.getMessage());
	}

	@Test void a04_tone_toMap() {
		var m = QuickStat.of("unavailable", "Work data", "unavailable").tone(QuickStat.Tone.ERROR).toMap();
		assertEquals("error", m.get("tone"));
	}

	@Test void a05_tone_everyConstant_lowercaseWireToken() {
		assertEquals("neutral", QuickStat.of("a", "A", 1).tone(QuickStat.Tone.NEUTRAL).toMap().get("tone"));
		assertEquals("info", QuickStat.of("a", "A", 1).tone(QuickStat.Tone.INFO).toMap().get("tone"));
		assertEquals("success", QuickStat.of("a", "A", 1).tone(QuickStat.Tone.SUCCESS).toMap().get("tone"));
		assertEquals("warning", QuickStat.of("a", "A", 1).tone(QuickStat.Tone.WARNING).toMap().get("tone"));
		assertEquals("error", QuickStat.of("a", "A", 1).tone(QuickStat.Tone.ERROR).toMap().get("tone"));
	}

	@Test void a06_tone_null_unsets() {
		assertFalse(QuickStat.of("a", "A", 1).tone(QuickStat.Tone.ERROR).tone(null).toMap().containsKey("tone"));
	}

	@Test void a07_of_numericValue_preservedAsIs() {
		assertEquals(42, QuickStat.of("count", "Total", 42).toMap().get("value"));
	}
}
