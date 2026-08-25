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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link Badge} factory / setter coverage and {@link Badge#validate()} edge cases.
 */
class Badge_Test extends TestBase {

	@Test void a01_count_factory() {
		var b = Badge.count(3).tone(Tone.DANGER).max(99).label("unread");
		assertEquals(3, b.count);
		assertEquals(Tone.DANGER, b.tone);
		assertEquals(99, b.max);
		b.validate();
	}

	@Test void a02_dot_factory() {
		var b = Badge.dot().tone(Tone.WARN);
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
		assertThrows(IllegalArgumentException.class, () -> b.validate());
	}

	@Test void a05_negativeCount_rejected() {
		var b = Badge.count(-1);
		assertThrows(IllegalArgumentException.class, () -> b.validate());
	}

	@Test void a06_maxBelowOne_rejected() {
		var b = Badge.count(5).max(0);
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a07_zeroCount_ok() {
		Badge.count(0).validate();
	}
}
