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
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.widgets.Badge;
import org.apache.juneau.rest.server.widgets.Tone;
import org.junit.jupiter.api.*;

/**
 * {@link Tab} fluent-setter coverage and {@link Tab#badge}'s validation edge cases.
 */
class Tab_Test extends TestBase {

	@Test void a01_badgeFluentSetterReturnsTab() {
		var badge = Badge.count(3);
		var t = Tab.create("t", "T");
		Tab chained = t.badge(badge);   // return type must be Tab, not a supertype - this line fails to compile otherwise
		assertSame(t, chained);
		assertSame(badge, t.badge);
	}

	@Test void a02_badgeDefaultsToNull() {
		var t = Tab.create("t", "T");
		assertNull(t.badge);
	}

	@Test void a03_badgeValidate_validCountBadge_ok() {
		var t = Tab.create("t", "T").content("x").badge(Badge.count(5));
		t.validate();
	}

	@Test void a04_badgeValidate_malformedBadge_rejected() {
		var t = Tab.create("t", "T").content("x").badge(new Badge());   // neither count nor dot
		var e = assertThrows(IllegalArgumentException.class, t::validate);
		assertTrue(e.getMessage().contains("Badge"), e::getMessage);
	}

	@Test void a05_badgeOrthogonalToSubtabsPanelBody() {
		// A badge decorates the tab-bar button, not the panel body, so it co-exists with any panel-body variant.
		var t = Tab.create("t", "T").subtabs(Subtab.create("s", "S").content("x")).badge(Badge.dot());
		t.validate();
	}

	@Test void a06_noBadge_validatesUnchanged() {
		// No-regression pin: a tab that never sets a badge still validates exactly as before.
		var t = Tab.create("t", "T").content("x");
		t.validate();
		assertNull(t.badge);
	}

	@Test void a07_badgeSurvivesJsonRoundTrip() {
		// The @BeanType "id,label,view,subtabs,badge" wire contract must carry a set badge through a full
		// serialize -> parse round trip with no field loss, exactly like every other public bean property here.
		var t = Tab.create("t", "T").content("x").badge(Badge.count(7).max(99).tone(Tone.WARN).label("pending"));
		var parsed = Json.to(Json.of(t), Tab.class);
		assertNotNull(parsed.badge);
		assertEquals(t.id, parsed.id);
		assertEquals(t.label, parsed.label);
		assertEquals(t.badge.count, parsed.badge.count);
		assertEquals(t.badge.max, parsed.badge.max);
		assertEquals(t.badge.tone, parsed.badge.tone);
		assertEquals(t.badge.label, parsed.badge.label);
		assertNull(parsed.badge.dot);
	}
}
