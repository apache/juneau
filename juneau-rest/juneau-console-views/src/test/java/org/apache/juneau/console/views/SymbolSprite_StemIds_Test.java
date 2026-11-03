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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.bct.BctAssertions.*;
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
		"juneau-sym-cancel", "juneau-sym-check", "juneau-sym-chevrondown", "juneau-sym-chevronleft",
		"juneau-sym-chevronright", "juneau-sym-chevronup", "juneau-sym-close", "juneau-sym-collapse_all",
		"juneau-sym-columns", "juneau-sym-copy", "juneau-sym-csv", "juneau-sym-download", "juneau-sym-edit",
		"juneau-sym-filter", "juneau-sym-first_page", "juneau-sym-forceStop", "juneau-sym-last_page",
		"juneau-sym-link", "juneau-sym-more", "juneau-sym-new", "juneau-sym-openPr", "juneau-sym-pause", "juneau-sym-pdf",
		"juneau-sym-play", "juneau-sym-plus", "juneau-sym-print",
		"juneau-sym-push", "juneau-sym-refresh", "juneau-sym-restore", "juneau-sym-search", "juneau-sym-settings", "juneau-sym-sort",
		"juneau-sym-spreadsheet", "juneau-sym-stop", "juneau-sym-toggle-deleted",
		"juneau-sym-toggle_column_search", "juneau-sym-tune"
	);

	/** Ids intentionally present in more than one sprite file (the default pack, its authoring key, the material pack). */
	private static final String MATERIAL_RESOURCE = "/org/apache/juneau/views/juneau-symbols-material.svg";

	/** The material pack is not edited by this work; its id set stays as it was. */
	private static final Set<String> MATERIAL_IDS = Set.of(
		"juneau-sym-copy", "juneau-sym-csv", "juneau-sym-spreadsheet", "juneau-sym-pdf",
		"juneau-sym-refresh", "juneau-sym-toggle_column_search", "juneau-sym-collapse_all",
		"juneau-sym-settings", "juneau-sym-first_page", "juneau-sym-chevronleft",
		"juneau-sym-chevronright", "juneau-sym-last_page", "juneau-sym-filter",
		"juneau-sym-chevrondown"
	);

	/** Ids intentionally in more than one file: the material pack overlays the default pack by stem (pack("material")). */
	private static final Set<String> OVERLAY_ALLOWLIST = MATERIAL_IDS;

	private static final Pattern SYMBOL_ID_PATTERN = Pattern.compile("<symbol\\s+id=\"([^\"]+)\"");

	private static List<String> idList(String resource) throws IOException {
		String svg;
		try (var in = ViewsMixin.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource);
			svg = new String(in.readAllBytes(), UTF_8);
		}
		var ids = new ArrayList<String>();
		var m = SYMBOL_ID_PATTERN.matcher(svg);
		while (m.find())
			ids.add(m.group(1));
		return ids;
	}

	private static Set<String> actualIds() throws IOException {
		return new LinkedHashSet<>(idList(ViewsMixin.SYMBOLS_SVG_RESOURCE));
	}

	private static Set<String> duplicates(List<String> ids) {
		var seen = new HashSet<String>();
		var dup = new TreeSet<String>();
		for (var id : ids)
			if (!seen.add(id))
				dup.add(id);
		return dup;
	}

	@SuppressWarnings({
		"java:S4030" // removed/added are read only by the failure-message lambda, which Sonar does not count as a use.
	})
	@Test void a01_stemIdSetIsPinned() throws Exception {
		var actual = actualIds();
		var removed = new TreeSet<>(EXPECTED_IDS);
		removed.removeAll(actual);
		var added = new TreeSet<>(actual);
		added.removeAll(EXPECTED_IDS);

		assertList(() -> "juneau-symbols.svg <symbol> stem ids no longer match the pinned set - removed: " + removed
			+ ", added: " + added + ". A stem id is a lookup key a host page can override a glyph by; changing,"
			+ " adding, or removing one here (even one renamed to something equivalent-looking) breaks that"
			+ " lookup silently, with no error on either side. If this change is intentional, update"
			+ " EXPECTED_IDS to match.", new TreeSet<>(actual), new TreeSet<>(EXPECTED_IDS).toArray());
	}

	@Test void a02_idsAreUniqueWithinEachSpriteFile() throws Exception {
		for (var resource : List.of(ViewsMixin.SYMBOLS_SVG_RESOURCE, MATERIAL_RESOURCE))
			assertEmpty(() -> "duplicate <symbol> ids within " + resource, duplicates(idList(resource)));
	}

	@Test void a03_idsSharedAcrossSpriteFilesAreAllowlisted() throws Exception {
		var shared = new TreeSet<>(actualIds());
		shared.retainAll(new HashSet<>(idList(MATERIAL_RESOURCE)));
		shared.removeAll(OVERLAY_ALLOWLIST);
		assertEmpty(() -> "ids present in more than one sprite file but not in OVERLAY_ALLOWLIST", shared);
	}

	@Test void a04_materialIdSetIsUnchanged() throws Exception {
		assertEquals(MATERIAL_IDS, new HashSet<>(idList(MATERIAL_RESOURCE)));
	}

	@Test void a05_tuneIsTheJuneauSlidersNotTheColumnsGear() throws Exception {
		String svg;
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.SYMBOLS_SVG_RESOURCE)) {
			svg = new String(in.readAllBytes(), UTF_8);
		}
		var tune = Pattern.compile("<symbol\\s+id=\"juneau-sym-tune\".*?</symbol>", Pattern.DOTALL).matcher(svg);
		assertTrue(tune.find());
		var body = tune.group();
		assertEquals(3, body.split("<circle", -1).length - 1, "three slider knobs: " + body);
		assertFalse(body.contains("M9.594"), "tune must not carry the columns gear path: " + body);
	}
}
