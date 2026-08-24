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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link BarSlot} bean contract, the sealed {@link BarWidget} permit set (N2), and fail-closed
 * {@link BarSlot#validate()} branches.
 */
class BarSlot_Test extends TestBase {

	@Test void a01_contractVersion_isOne() {
		assertEquals("1", BarSlot.CONTRACT_VERSION);
	}

	@Test void a02_builder_roundTrip() {
		var b = BarSlot.create("bar-main")
			.widgets(
				BarText.of("ctx", "Editing"),
				BarBadge.of("changePending").label("change pending").badge(Badge.count(7).tone(Tone.WARN)))
			.refreshUrl("/bar/counts");
		assertEquals("bar-main", b.id);
		assertEquals(2, b.widgets.size());
		b.validate();
	}

	@Test void a03_sealed_permitsOnlyBarBadgeAndBarText() {
		assertTrue(BarWidget.class.isSealed());
		var permitted = new HashSet<Class<?>>(Arrays.asList(BarWidget.class.getPermittedSubclasses()));
		assertEquals(Set.of(BarBadge.class, BarText.class), permitted);
	}

	@Test void a04_blankId_rejected() {
		assertThrows(IllegalArgumentException.class, () -> BarSlot.create("  ").widgets(BarText.of("t", "x")).validate());
	}

	@Test void a05_emptySlot_rejected() {
		assertThrows(IllegalArgumentException.class, () -> BarSlot.create("b").validate());
		assertThrows(IllegalArgumentException.class, () -> BarSlot.create("b").widgets().validate());
	}

	@Test void a06_duplicateWidgetId_rejected() {
		var b = BarSlot.create("b").widgets(BarText.of("dup", "a"), BarText.of("dup", "b"));
		var e = assertThrows(IllegalArgumentException.class, b::validate);
		assertTrue(e.getMessage().contains("dup"), e::getMessage);
	}

	@Test void a07_barText_blankText_rejected() {
		var b = BarSlot.create("b").widgets(BarText.of("t", "  "));
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a08_barBadge_fansOutToBadge() {
		var b = BarSlot.create("b").widgets(BarBadge.of("x").badge(Badge.count(-1)));
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a09_badContractVersion_rejected() {
		var b = BarSlot.create("b").widgets(BarText.of("t", "x"));
		b.contractVersion = "9";
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a10_refreshUrl_mustBeSameOrigin() {
		var b = BarSlot.create("b").widgets(BarText.of("t", "x")).refreshUrl("http://evil/x");
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a11_isAWidget() {
		assertTrue(Widget.class.isAssignableFrom(BarSlot.class));
	}
}
