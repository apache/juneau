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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/** Unit tests for {@link CardBusAttrs}: comma-list forms and entry validation. */
class CardBusAttrs_Test {

	@Test void a01_publishes_commaList_retainSuffix() {
		var l = CardBusAttrs.publications("c", "ssc.focus!retain, ssc.hot ,");
		assertEquals(2, l.size());
		assertEquals("ssc.focus", l.get(0).get("topic"));
		assertEquals(true, l.get(0).get("retain"));
		assertEquals("ssc.hot", l.get(1).get("topic"));
		assertEquals(false, l.get(1).get("retain"));
	}

	@Test void a02_blankString_isANoOp() {
		assertTrue(CardBusAttrs.subscriptions("c", "").isEmpty());
		assertTrue(CardBusAttrs.publications("c", " , ").isEmpty());
	}

	@Test void b01_hashWithoutTopic_isIae() {
		var e1 = assertThrows(IllegalArgumentException.class, () -> CardBusAttrs.subscriptions("c", List.of(Map.of("as", "refresh"))));
		assertEquals("<@card id='c'> subscribes entry requires a topic", e1.getMessage());
		var e2 = assertThrows(IllegalArgumentException.class, () -> CardBusAttrs.publications("c", List.of(Map.of("retain", true))));
		assertEquals("<@card id='c'> publishes entry requires a topic", e2.getMessage());
	}

	@Test void b02_nonBooleanRetain_isIae() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> CardBusAttrs.publications("c", List.of(Map.of("topic", "t.x", "retain", 1))));
		assertEquals("<@card id='c'> publishes entry retain must be a boolean (true or false); got '1'", e.getMessage());
	}
}
