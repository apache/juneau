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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * {@link Badge} factory / setter coverage and {@link Badge#validate()} edge cases.
 */
class Badge_Test extends TestBase {

	@Test void a01_count_factory() {
		var b = Badge.count(3).tone(StatusTone.ERROR).max(99).label("unread");
		assertBean(b, "count,tone,max,label", "3,ERROR,99,unread");
		b.validate();
	}

	@Test void a02_dot_factory() {
		var b = Badge.dot().tone(StatusTone.WARNING);
		assertTrue(b.dot);
		b.validate();
	}

	@Test void a03_bothCountAndDot_rejected() {
		var b = Badge.count(1);
		b.dot = true;
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a04_neitherCountNorDot_rejected() {
		var b = new Badge();
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a05_negativeCount_rejected() {
		var b = Badge.count(-1);
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a06_maxBelowOne_rejected() {
		var b = Badge.count(5).max(0);
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a07_zeroCount_ok() {
		Badge.count(0).validate();
	}

	@Test void a08_tone_serializesAsEnumName_lowercaseIsWireToken() {
		for (var t : StatusTone.values()) {
			var json = Json.of(Badge.dot().tone(t));
			assertBean(Json.to(json, Map.class), "tone", t.name());
			assertEquals(t.wire(), t.name().toLowerCase(Locale.ROOT));
		}
		assertBean(Json.to(Json.of(Badge.count(1).tone(StatusTone.WARNING)), Map.class), "tone", "WARNING");
	}

	@Test void a09_nullTone_validates() {
		var b = Badge.count(2);
		assertNull(b.tone);
		b.validate();
		b.tone(StatusTone.INFO).tone(null);
		assertNull(b.tone);
		b.validate();
	}
}
