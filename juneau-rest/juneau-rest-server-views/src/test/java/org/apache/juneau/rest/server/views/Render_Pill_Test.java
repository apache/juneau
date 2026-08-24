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
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * {@link Render#pill()} / {@link Render#pill(String)} factory sugar: canonical {@code {id:"pill",meta:…}} wire form
 * and boxed-omission rules (dot default on, absent/blank tone omitted).  Pill is cell-path only, so these factories
 * are pure sugar over {@link Render#of(String)} &mdash; no allowlist entry and no {@code BUILTIN_IDS} mutation.
 */
class Render_Pill_Test extends TestBase {

	private static Map<?,?> wire(Render r) {
		return Json.to(Json.of(r), Map.class);
	}

	@Test void a01_pill_noArgs_isBareIdWithNoMeta() {
		var r = Render.pill();
		assertEquals("pill", r.id);
		assertNull(r.meta, "dot default is on in JS; the factory must not emit a dot key");
		assertEquals(Map.of("id", "pill"), wire(r));
	}

	@Test void a02_pill_withTone_emitsToneMetaOnly() {
		for (var tone : new String[]{"ok", "warn", "exceeds", "neutral"}) {
			var r = Render.pill(tone);
			assertEquals("pill", r.id);
			assertEquals(Map.of("tone", tone), r.meta, tone);
			assertEquals(Map.of("id", "pill", "meta", Map.of("tone", tone)), wire(r), tone);
		}
	}

	@Test void a03_pill_nullOrBlankTone_omitsToneMeta() {
		for (var tone : new String[]{null, "", "   "}) {
			var r = Render.pill(tone);
			assertEquals("pill", r.id);
			assertNull(r.meta, () -> "blank tone must be omitted, identical to pill(): " + Json.of(r));
			assertEquals(Map.of("id", "pill"), wire(r));
		}
	}

	@Test void a04_chainedMeta_carriesFieldDotAction() {
		var r = Render.pill("ok").meta("field", "state").meta("dot", "off").meta("action", "ack");
		assertEquals(Map.of("tone", "ok", "field", "state", "dot", "off", "action", "ack"), r.meta);
		assertEquals(
			Map.of("id", "pill", "meta", Map.of("tone", "ok", "field", "state", "dot", "off", "action", "ack")),
			wire(r));
	}

	@Test void a05_stringSugarUnaffected_bareIdAndColonField() {
		// Render.parse still treats "id:field" the same; "pill" gains no special parsing.
		assertEquals("pill", Render.parse("pill").id);
		assertNull(Render.parse("pill").meta);
		var withField = Render.parse("pill:state");
		assertEquals("pill", withField.id);
		assertEquals(Map.of("field", "state"), withField.meta);
	}
}
