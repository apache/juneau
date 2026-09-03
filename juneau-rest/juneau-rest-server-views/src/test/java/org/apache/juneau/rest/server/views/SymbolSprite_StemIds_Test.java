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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pins the exact set of {@code <symbol>} stem ids shipped in {@code juneau-symbols.svg}.
 *
 * <p>
 * A third-party host page can override one of these glyphs by looking the stem id up by name and swapping in a
 * replacement drawing for it. That lookup is name-based only, so if a stem id here is renamed - or quietly added
 * or removed - without a matching change on the consuming side, the override stops finding its target. The
 * result is not an error: the glyph simply renders as nothing, with no signal anywhere in this module. Guarding
 * against that silent failure is the entire point of this test, so the check below is set equality rather than a
 * one-directional containment check - a containment check would happily let a new id slip in unnoticed.
 */
class SymbolSprite_StemIds_Test extends TestBase {

	/**
	 * The complete set of stem ids as of the time this guard was written. This set is intentionally frozen:
	 * any difference from what is actually shipped must be a deliberate, reviewed decision, not an accident of
	 * an artwork or naming change.
	 */
	private static final Set<String> EXPECTED_IDS = Set.of(
		"juneau-sym-cancel", "juneau-sym-check", "juneau-sym-chevrondown", "juneau-sym-chevronright",
		"juneau-sym-close", "juneau-sym-collapse_all", "juneau-sym-columns", "juneau-sym-copy", "juneau-sym-csv",
		"juneau-sym-download", "juneau-sym-edit", "juneau-sym-filter", "juneau-sym-new", "juneau-sym-pdf",
		"juneau-sym-print", "juneau-sym-refresh", "juneau-sym-search", "juneau-sym-settings",
		"juneau-sym-spreadsheet", "juneau-sym-toggle-deleted", "juneau-sym-toggle_column_search"
	);

	private static final Pattern SYMBOL_ID_PATTERN = Pattern.compile("<symbol\\s+id=\"([^\"]+)\"");

	private static Set<String> actualIds() throws IOException {
		String svg;
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.SYMBOLS_SVG_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.SYMBOLS_SVG_RESOURCE);
			svg = new String(in.readAllBytes(), UTF_8);
		}
		var ids = new LinkedHashSet<String>();
		var m = SYMBOL_ID_PATTERN.matcher(svg);
		while (m.find())
			ids.add(m.group(1));
		return ids;
	}

	@Test void a01_stemIdSetIsPinned() throws Exception {
		var actual = actualIds();
		if (actual.equals(EXPECTED_IDS))
			return;

		var removed = new TreeSet<>(EXPECTED_IDS);
		removed.removeAll(actual);
		var added = new TreeSet<>(actual);
		added.removeAll(EXPECTED_IDS);

		fail("juneau-symbols.svg <symbol> stem ids no longer match the pinned set - removed: " + removed
			+ ", added: " + added + ". A stem id is a lookup key a host page can override a glyph by; changing,"
			+ " adding, or removing one here (even one renamed to something equivalent-looking) breaks that"
			+ " lookup silently, with no error on either side. If this change is intentional, update"
			+ " EXPECTED_IDS to match.");
	}
}
