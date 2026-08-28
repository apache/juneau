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
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Asserts that the authoring key sheet ({@code juneau-symbols-key.svg}) still agrees with the sprite it documents
 * ({@code juneau-symbols.svg}).
 *
 * <h5 class='section'>The drift this exists to catch:</h5>
 * <p>
 * The key sheet is not served - only the sprite is a registered servable resource - so nothing else in the build
 * reads it, and nothing else would notice it going stale. It inlines a copy of every {@code <symbol>} definition
 * so the sheet can be opened on its own, which means every artwork change has to be made in two places. Worse,
 * each glyph's {@code viewBox} appears <b>twice</b> in the key sheet: once on the inlined {@code <symbol>} and
 * again on the per-cell wrapper {@code <svg>} that {@code <use>}s it. A glyph normalised in the sprite but not on
 * its wrapper renders at the wrong scale on the sheet only, so the artwork looks wrong in review while being
 * perfectly correct in the shipped sprite - which is the most confusing version of this failure.
 *
 * <h5 class='section'>What this asserts, and what it deliberately does not:</h5>
 * <p>
 * It asserts the <i>property</i>: the two files carry the same {@code <symbol>} elements byte for byte and in the
 * same order, and every per-cell wrapper declares the same {@code viewBox} as the symbol it references. It does
 * <b>not</b> pin the sheet's generated layout. {@code scripts/gen-symbols-key.py --check} does that - it compares
 * the whole file against what it would generate - and it is strictly stronger. The gap is real and is accepted
 * rather than hidden: a hand-edit to the grid arithmetic or the sheet size passes this test and fails that script.
 *
 * <h5 class='section'>Why it reads the source tree rather than the classpath:</h5>
 * <p>
 * The key sheet is not a servable resource, so reading it through {@link ViewsMixin}'s resource loader would
 * assert against a copy that no consumer ever fetches. Both files are read from the module's own
 * {@code src/main/resources} instead, the way {@link RawContentSinkScanner#locateModuleRoot()} already does for
 * this module's shipped assets.
 */
class SymbolsKey_Staleness_Test extends TestBase {

	private static final String RESOURCE_DIR = "src/main/resources/org/apache/juneau/views";
	private static final String SPRITE = "juneau-symbols.svg";
	private static final String KEY = "juneau-symbols-key.svg";

	/** How to put it back, quoted in every failure message rather than left for the reader to find. */
	private static final String REGENERATE = "Regenerate the sheet with `python3 scripts/gen-symbols-key.py`.";

	private static final Pattern SYMBOL = Pattern.compile("<symbol\\s.*?</symbol>", Pattern.DOTALL);
	private static final Pattern SYMBOL_ID = Pattern.compile("<symbol\\s+id=\"([^\"]+)\"");
	private static final Pattern SYMBOL_VIEWBOX = Pattern.compile("<symbol\\s+[^>]*viewBox=\"([^\"]+)\"");

	/** One per-cell wrapper of the key sheet's grid: its own declared viewBox, and the symbol id it references. */
	private static final Pattern CELL_WRAPPER =
		Pattern.compile("<svg\\s+x=\"[^\"]*\"\\s+y=\"[^\"]*\"[^>]*viewBox=\"([^\"]+)\"><use\\s+href=\"#([^\"]+)\"/>");

	private static String read(String fileName) throws IOException {
		var root = RawContentSinkScanner.locateModuleRoot();
		assertNotNull(root, "could not locate the juneau-rest-server-views module root");
		var f = root.resolve(RESOURCE_DIR).resolve(fileName);
		assertTrue(Files.isRegularFile(f), () -> "missing resource: " + f);
		return Files.readString(f, UTF_8);
	}

	/** Every {@code <symbol>} element of an SVG document, verbatim and in document order. */
	private static List<String> symbols(String svg) {
		var out = new ArrayList<String>();
		var m = SYMBOL.matcher(svg);
		while (m.find())
			out.add(m.group());
		return out;
	}

	/** Index of the first symbol that differs between the two documents, or {@code -1} if they agree. */
	private static int firstDrift(List<String> sprite, List<String> key) {
		for (var i = 0; i < Math.min(sprite.size(), key.size()); i++)
			if (!sprite.get(i).equals(key.get(i)))
				return i;
		return sprite.size() == key.size() ? -1 : Math.min(sprite.size(), key.size());
	}

	private static String group(Pattern p, String s) {
		var m = p.matcher(s);
		assertTrue(m.find(), () -> "no match for " + p.pattern() + " in: " + s);
		return m.group(1);
	}

	/**
	 * Appends a redundant {@code Z} to the first path in a document - a byte change that is valid in any path.
	 *
	 * <p>
	 * The needle carries its leading space deliberately: {@code id="} contains {@code d="}, so searching for the
	 * bare attribute name finds every glyph's <i>id</i> first. That still produces a detectable byte change, so
	 * b01 would pass either way - and would be asserting that a renamed stem is detected rather than that changed
	 * artwork is, which is the other test's job.
	 */
	private static String mutateFirstPath(String svg) {
		var at = svg.indexOf(" d=\"");
		assertTrue(at >= 0, "no path data to mutate; this file's anti-vacuous checks cannot run");
		var close = svg.indexOf('"', at + 4);
		assertTrue(close > at, "unterminated path data");
		return svg.substring(0, close) + " Z" + svg.substring(close);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a: the two files agree
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_keySheetInlinesTheSpriteSymbolsByteForByte() throws Exception {
		var sprite = symbols(read(SPRITE));
		var key = symbols(read(KEY));
		assertFalse(sprite.isEmpty(), "no <symbol> elements found in the sprite - this test would be vacuous");
		assertEquals(sprite.size(), key.size(),
			() -> "the key sheet inlines " + key.size() + " symbols but the sprite ships " + sprite.size()
				+ ". " + REGENERATE);
		var at = firstDrift(sprite, key);
		if (at < 0)
			return;
		fail("the key sheet's copy of a glyph has drifted from the sprite at position " + at + " (sprite id="
			+ group(SYMBOL_ID, sprite.get(at)) + ", key id=" + group(SYMBOL_ID, key.get(at))
			+ "). The sprite is the source of truth. " + REGENERATE);
	}

	@Test void a02_everyCellWrapperDeclaresItsSymbolsViewBox() throws Exception {
		var key = read(KEY);
		var declared = new LinkedHashMap<String,String>();
		for (var s : symbols(key))
			declared.put(group(SYMBOL_ID, s), group(SYMBOL_VIEWBOX, s));

		var seen = new ArrayList<String>();
		var m = CELL_WRAPPER.matcher(key);
		while (m.find()) {
			var wrapperViewBox = m.group(1);
			var id = m.group(2);
			seen.add(id);
			assertEquals(declared.get(id), wrapperViewBox,
				() -> "the key sheet's grid cell for " + id + " wraps it in a viewBox its <symbol> does not"
					+ " declare, so it renders at the wrong scale on the sheet only. " + REGENERATE);
		}
		// Without this, a sheet whose grid stopped being emitted at all would satisfy the loop above trivially.
		assertEquals(new ArrayList<>(declared.keySet()), seen,
			"every inlined symbol must appear exactly once in the sheet's grid, in order. " + REGENERATE);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b: the comparisons above can actually fail
	//
	// A staleness test that silently stopped seeing artwork reads as a passing test, which is worse than no test
	// at all - the same discipline RawContentSink_SecurityScan_Test applies to its scanner.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_aOneCharacterArtworkChangeIsDetected() throws Exception {
		var real = read(SPRITE);
		var drifted = symbols(mutateFirstPath(real));
		assertEquals(symbols(real).size(), drifted.size(), "the mutation must not change the symbol count");
		assertEquals(0, firstDrift(symbols(real), drifted),
			"a single added path command went undetected, so a01 is comparing something other than artwork"
				+ " - a whitespace-normalising comparison, or a regex capturing only the opening tag, both look"
				+ " like this");
	}

	@Test void b02_aMissingSymbolIsDetected() throws Exception {
		var sprite = symbols(read(SPRITE));
		var short_ = sprite.subList(0, sprite.size() - 1);
		assertTrue(firstDrift(sprite, short_) >= 0, "a dropped glyph went undetected");
	}

	@Test void b03_theWrapperPatternMatchesEveryCellOfTheRealSheet() throws Exception {
		// a02's assertion is inside a while-loop, so a pattern that stopped matching would make it vacuous.  The
		// count is asserted against the symbol count rather than a literal so it survives a glyph being added.
		var key = read(KEY);
		var count = 0;
		var m = CELL_WRAPPER.matcher(key);
		while (m.find())
			count++;
		assertEquals(symbols(key).size(), count,
			"the per-cell wrapper pattern no longer matches every cell of the real sheet");
	}
}
