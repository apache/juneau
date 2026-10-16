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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.*;
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

	@Test void a08_meter_toMap_shape() {
		var m = QuickStat.meter("inventory", "Inventory", 12, 40).text("12 of 40").statusTone(StatusTone.SUCCESS).toMap();
		assertEquals(List.of("id", "type", "label", "value", "max", "text", "tone"), new ArrayList<>(m.keySet()));
		assertEquals("meter", m.get("type"));
		assertEquals(12, m.get("value"));
		assertEquals(40, m.get("max"));
		assertEquals("12 of 40", m.get("text"));
		assertEquals("success", m.get("tone"));
	}

	@Test void a09_meter_numbersKeptAsGiven() {
		var m = QuickStat.meter("q", "Q", 2.5d, 10L).toMap();
		assertEquals(2.5d, m.get("value"));
		assertEquals(10L, m.get("max"));
		assertFalse(m.containsKey("text"));
		assertFalse(m.containsKey("tone"));
	}

	@Test void a10_segmented_toMap_shape() {
		var m = QuickStat.segmented("orders", "Orders").segment("Placed", 3).segment("Approved", 5, StatusTone.INFO)
			.segment("Delivered", 9, StatusTone.SUCCESS).toMap();
		assertEquals("segmented", m.get("type"));
		assertFalse(m.containsKey("tone"));
		var segs = (List<?>)m.get("segments");
		assertEquals(3, segs.size());
		var s0 = (Map<?,?>)segs.get(0);
		assertEquals("Placed", s0.get("label"));
		assertEquals(3L, s0.get("count"));
		assertFalse(s0.containsKey("tone"));
		assertEquals("info", ((Map<?,?>)segs.get(1)).get("tone"));
	}

	@Test void a11_segment_neutralEmitted_unsetOmitted() {
		var segs = (List<?>)QuickStat.segmented("o", "O").segment("A", 1, StatusTone.NEUTRAL).segment("B", 2).toMap().get("segments");
		assertEquals("neutral", ((Map<?,?>)segs.get(0)).get("tone"));
		assertFalse(((Map<?,?>)segs.get(1)).containsKey("tone"));
	}

	@Test void a12_tile_hasNoType_byteIdenticalKeys() {
		var m = QuickStat.of("a", "A", 1).toMap();
		assertFalse(m.containsKey("type"));
		assertEquals(List.of("id", "label", "value"), new ArrayList<>(m.keySet()));
		assertTrue(QuickStat.of("a", null, 1).toMap().containsKey("label"));
	}

	@Test void a13_newKinds_requireLabel() {
		for (var label : new String[]{null, "", "  "}) {
			assertEquals("QuickStat 'm': label must not be null or blank.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", label, 1, 2)).getMessage());
			assertEquals("QuickStat 's': label must not be null or blank.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", label)).getMessage());
		}
		assertEquals("M", QuickStat.meter("m", "M", 1, 2).toMap().get("label"));
		assertEquals("S", QuickStat.segmented("s", "S").segment("A", 1).toMap().get("label"));
	}

	@Test void a13b_meter_zeroValue_allowed() {
		assertEquals(0, QuickStat.meter("m", "M", 0, 10).toMap().get("value"));
	}

	@Test void a14_meter_blankText_omitted() {
		assertFalse(QuickStat.meter("m", "M", 1, 2).text("  ").toMap().containsKey("text"));
	}

	@Test void a15_toMap_returnsFreshSegmentMaps() {
		var q = QuickStat.segmented("s", "S").segment("A", 1);
		assertNotSame(((List<?>)q.toMap().get("segments")).get(0), ((List<?>)q.toMap().get("segments")).get(0));
	}

	@Test void a16_meter_nullValueOrMax() {
		assertEquals("QuickStat 'm': meter value and max must not be null.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", null, 2)).getMessage());
		assertEquals("QuickStat 'm': meter value and max must not be null.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", 1, null)).getMessage());
	}

	@Test void a17_meter_negativeOrNonFiniteValue() {
		assertEquals("QuickStat 'm': meter value must be a finite number >= 0.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", -1, 2)).getMessage());
		assertEquals("QuickStat 'm': meter value must be a finite number >= 0.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", Double.NaN, 2)).getMessage());
		assertEquals("QuickStat 'm': meter value must be a finite number >= 0.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", Double.POSITIVE_INFINITY, 2)).getMessage());
	}

	@Test void a18_meter_maxZeroNegativeOrNonFinite() {
		assertEquals("QuickStat 'm': meter max must be a finite number > 0.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", 1, 0)).getMessage());
		assertEquals("QuickStat 'm': meter max must be a finite number > 0.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", 1, -1)).getMessage());
		assertEquals("QuickStat 'm': meter max must be a finite number > 0.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", 1, Double.NaN)).getMessage());
	}

	@Test void a19_meter_valueOverMax_allowed() {
		assertEquals(52, QuickStat.meter("m", "M", 52, 40).toMap().get("value"));
	}

	@Test void a20_blankId() {
		assertEquals("QuickStat id must not be null or blank.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter(" ", "M", 1, 2)).getMessage());
		assertEquals("QuickStat id must not be null or blank.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented(null, "S")).getMessage());
	}

	@Test void a21_segment_blankLabel() {
		assertEquals("QuickStat 's': segment label must not be null or blank.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", "S").segment(" ", 1)).getMessage());
		assertEquals("QuickStat 's': segment label must not be null or blank.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", "S").segment(null, 1)).getMessage());
	}

	@Test void a22_segment_negativeCount() {
		assertEquals("QuickStat 's': segment count must be >= 0.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", "S").segment("A", -1)).getMessage());
	}

	@Test void a23_segment_ninth_rejected() {
		var q = QuickStat.segmented("s", "S");
		for (var i = 0; i < QuickStat.MAX_SEGMENTS; i++)
			q.segment("S" + i, i);
		assertEquals(8, ((List<?>)q.toMap().get("segments")).size());
		assertEquals("QuickStat 's': a segmented item takes at most 8 segments.",
			assertThrows(IllegalArgumentException.class, () -> q.segment("S9", 1)).getMessage());
	}

	@Test void a24_segmented_noSegments_throwsAtToMap() {
		assertEquals("QuickStat 's': a segmented item needs at least one segment.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", "S").toMap()).getMessage());
	}

	@Test void a25_text_onlyOnMeter() {
		assertEquals("QuickStat 't': text(...) is only valid on a meter item.", assertThrows(IllegalArgumentException.class, () -> QuickStat.of("t", "T", 1).text("x")).getMessage());
		assertEquals("QuickStat 's': text(...) is only valid on a meter item.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", "S").text("x")).getMessage());
	}

	@Test void a26_text_null() {
		assertEquals("QuickStat 'm': text must not be null.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", 1, 2).text(null)).getMessage());
	}

	@Test void a27_segment_onlyOnSegmented() {
		assertEquals("QuickStat 't': segment(...) is only valid on a segmented item.", assertThrows(IllegalArgumentException.class, () -> QuickStat.of("t", "T", 1).segment("A", 1)).getMessage());
		assertEquals("QuickStat 'm': segment(...) is only valid on a segmented item.", assertThrows(IllegalArgumentException.class, () -> QuickStat.meter("m", "M", 1, 2).segment("A", 1, StatusTone.INFO)).getMessage());
	}

	@Test void a28_statusTone_onSegmented() {
		assertEquals("QuickStat 's': statusTone(...) is not valid on a segmented item; tone each segment instead.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", "S").statusTone(StatusTone.INFO)).getMessage());
		assertEquals("QuickStat 's': statusTone(...) is not valid on a segmented item; tone each segment instead.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", "S").statusTone(null)).getMessage());
	}

	@Test void a29_tone_onSegmented() {
		assertEquals("QuickStat 's': tone(...) is not valid on a segmented item; tone each segment instead.", assertThrows(IllegalArgumentException.class, () -> QuickStat.segmented("s", "S").tone(QuickStat.Tone.INFO)).getMessage());
	}

	@Test void a30_tone_and_statusTone_sameToken_lastWins() {
		assertEquals("info", QuickStat.of("a", "A", 1).tone(QuickStat.Tone.ERROR).statusTone(StatusTone.INFO).toMap().get("tone"));
		assertEquals("warning", QuickStat.of("a", "A", 1).statusTone(StatusTone.INFO).tone(QuickStat.Tone.WARNING).toMap().get("tone"));
		for (var t : QuickStat.Tone.values()) {
			var st = StatusTone.valueOf(t.name());
			assertEquals(QuickStat.of("a", "A", 1).tone(t).toMap().get("tone"), QuickStat.of("a", "A", 1).statusTone(st).toMap().get("tone"));
		}
	}

	@Test void a31_statusTone_null_clears() {
		assertFalse(QuickStat.meter("m", "M", 1, 2).statusTone(StatusTone.ERROR).statusTone(null).toMap().containsKey("tone"));
	}
}
