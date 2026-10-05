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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

class RelativeDuration_Test extends TestBase {

	@Test
	void a01_of_negative() {
		var d = RelativeDuration.of(-24, RelativeDuration.Unit.H);
		assertBean(d, "amount,unit", "-24,H");
	}

	@Test
	void a02_of_positive() {
		var d = RelativeDuration.of(7, RelativeDuration.Unit.D);
		assertBean(d, "amount,unit", "7,D");
	}

	@Test
	void a03_unit_token() {
		assertEquals("ms", RelativeDuration.Unit.MS.token());
		assertEquals("s", RelativeDuration.Unit.S.token());
		assertEquals("m", RelativeDuration.Unit.M.token());
		assertEquals("h", RelativeDuration.Unit.H.token());
		assertEquals("d", RelativeDuration.Unit.D.token());
	}

	@Test
	void a04_nullUnit_throws() {
		assertThrows(IllegalArgumentException.class, () -> RelativeDuration.of(1, null));
	}

	@Test
	void a05_equalsAndHashCode() {
		assertEquals(RelativeDuration.of(-24, RelativeDuration.Unit.H), RelativeDuration.of(-24, RelativeDuration.Unit.H));
		assertNotEquals(RelativeDuration.of(-24, RelativeDuration.Unit.H), RelativeDuration.of(24, RelativeDuration.Unit.H));
	}

	@Test
	void a06_shorthandFactories_matchOf() {
		assertEquals(RelativeDuration.of(1, RelativeDuration.Unit.MS), RelativeDuration.ofMillis(1));
		assertEquals(RelativeDuration.of(-2, RelativeDuration.Unit.S), RelativeDuration.ofSeconds(-2));
		assertEquals(RelativeDuration.of(3, RelativeDuration.Unit.M), RelativeDuration.ofMinutes(3));
		assertEquals(RelativeDuration.of(-24, RelativeDuration.Unit.H), RelativeDuration.ofHours(-24));
		assertEquals(RelativeDuration.of(7, RelativeDuration.Unit.D), RelativeDuration.ofDays(7));
	}
}
