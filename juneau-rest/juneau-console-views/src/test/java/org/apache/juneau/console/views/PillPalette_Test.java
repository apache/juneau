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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link PillPalette#slot(String)}: FNV-1a over UTF-16 units, one-based slot.  The vectors are the same ones the
 * JavaScript harness asserts.
 */
class PillPalette_Test extends TestBase {

	@Test void a01_size() {
		assertEquals(10, PillPalette.SIZE);
	}

	@Test void a02_knownVectors() {
		var expected = Map.ofEntries(
			Map.entry("", 2), Map.entry("DOG", 10), Map.entry("CAT", 6), Map.entry("BIRD", 3), Map.entry("FISH", 6),
			Map.entry("MOUSE", 3), Map.entry("RABBIT", 10), Map.entry("SNAKE", 4), Map.entry("Pet", 1),
			Map.entry("Order", 6), Map.entry("User", 1));
		for (var e : expected.entrySet())
			assertEquals((int)e.getValue(), PillPalette.slot(e.getKey()), e.getKey());
	}

	@Test void a03_nullIsEmptyString() {
		assertEquals(PillPalette.slot(""), PillPalette.slot(null));
	}

	@Test void a04_caseSensitive() {
		assertEquals(PillPalette.slot("Dog"), PillPalette.slot(new String("Dog")));
	}

	@Test void a05_alwaysInRange() {
		for (var v : new String[] {"a", "b", "caf\u00e9", "\ud83d\udc36", "a long value with spaces 1234567890"}) {
			var s = PillPalette.slot(v);
			assertTrue(s >= 1 && s <= PillPalette.SIZE, () -> v + " -> " + s);
		}
	}
}
