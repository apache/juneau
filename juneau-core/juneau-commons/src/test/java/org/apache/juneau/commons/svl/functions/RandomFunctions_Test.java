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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/** Tests for {@link RandomFunctions}. Statistical assertions are intentionally weak. */
class RandomFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(RandomFunctions.ALL).build();

	@Test void rand_inRange() {
		for (var i = 0; i < 20; i++) {
			var v = Double.parseDouble(vr.resolve("#{rand()}"));
			assertTrue(v >= 0.0 && v < 1.0);
		}
	}

	@Test void randInt_unaryInRange() {
		for (var i = 0; i < 20; i++) {
			var v = Integer.parseInt(vr.resolve("#{randInt(10)}"));
			assertTrue(v >= 0 && v < 10);
		}
	}

	@Test void randInt_binaryInRange() {
		for (var i = 0; i < 20; i++) {
			var v = Integer.parseInt(vr.resolve("#{randInt(5, 8)}"));
			assertTrue(v >= 5 && v <= 8);
		}
	}

	@Test void randInt_invalidMaxThrows() {
		assertThrows(IllegalArgumentException.class, () -> vr.resolve("#{randInt(0)}"));
	}

	@Test void randLong_inRange() {
		for (var i = 0; i < 10; i++) {
			var v = Long.parseLong(vr.resolve("#{randLong(100, 200)}"));
			assertTrue(v >= 100 && v <= 200);
		}
	}

	@Test void randString_lengthOnly() {
		var s = vr.resolve("#{randString(16)}");
		assertEquals(16, s.length());
		for (var c : s.toCharArray())
			assertTrue(Character.isLetterOrDigit(c));
	}

	@Test void randString_charset() {
		var s = vr.resolve("#{randString(8, abc)}");
		assertEquals(8, s.length());
		for (var c : s.toCharArray())
			assertTrue(c == 'a' || c == 'b' || c == 'c');
	}

	@Test void randChoice() {
		var seen = new HashSet<String>();
		for (var i = 0; i < 50; i++)
			seen.add(vr.resolve("#{randChoice(red, green, blue)}"));
		// With 50 picks of a 3-option choice, we should observe all three with very high probability.
		assertFalse(seen.isEmpty());
		assertTrue(seen.size() <= 3);
		for (var s : seen)
			assertTrue(s.equals("red") || s.equals("green") || s.equals("blue"));
	}

	@Test void uuid_format() {
		var s = vr.resolve("#{uuid()}");
		assertEquals(36, s.length());
		assertEquals('-', s.charAt(8));
		assertEquals('-', s.charAt(13));
		assertEquals('-', s.charAt(18));
		assertEquals('-', s.charAt(23));
	}

	@Test void uuid_freshPerCall() {
		var first = vr.resolve("#{uuid()}");
		var second = vr.resolve("#{uuid()}");
		assertNotEquals(first, second, "uuid() must produce a fresh value per resolve");
	}
}
