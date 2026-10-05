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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The licensing-drift guard for {@code juneau-symbols.svg}: asserts that the shipped artwork is still the artwork
 * the provenance manifest ({@code juneau-symbols-provenance.md}) approved, and that the sprite still honours the
 * contracts the manifest declares.
 *
 * <h5 class='section'>Why this exists at all</h5>
 * <p>
 * Because origin and fingerprint are facts about today, and the failure they guard is silent. Some glyphs are
 * Juneau-original; others are console-sprite counterparts cleared for artwork copy (operator ruling 2026-09-24 /
 * design §3.4) and recorded as {@code irs-artwork}. The cheapest way for an unreviewed paste to land again is for
 * someone to overwrite a path during an unrelated polish pass. Nothing else in the build would notice: the artwork
 * renders, the ids are unchanged, and {@link SymbolSprite_StemIds_Test} pins <i>names</i> rather than paths. This
 * test makes such a paste fail until the manifest row is deliberately edited, which turns an invisible act into a
 * reviewable one.
 *
 * <h5 class='section'>Deliberately separate from the stem-id guard</h5>
 * <p>
 * {@link SymbolSprite_StemIds_Test} guards correctness &mdash; a rename blanks the Support Console's toolbar,
 * because an app overrides these glyphs by stem name through the page-level override sprite (see
 * {@code juneau-icons.js}), and an unresolved stem draws nothing rather than throwing. This one guards licensing drift. They fail
 * for unrelated reasons and read as unrelated diagnostics, so they are two tests rather than one.
 *
 * <h5 class='section'>What it asserts</h5>
 * <ul>
 * 	<li>The manifest and the sprite describe the same glyph set, in the same order.
 * 	<li>Every glyph's fingerprint still matches its approved value.
 * 	<li>Every glyph's declared origin is either {@code juneau-original} or {@code irs-artwork} &mdash; the only
 * 		origins the manifest authorises. (This test does not read {@code NOTICE}; keeping its attribution in step
 * 		with the {@code irs-artwork} rows is a manual, reviewed act.)
 * 	<li>The three contracts the manifest states as rules: {@code viewBox="0 0 24 24"} on every glyph, paint only
 * 		ever {@code none}, {@code currentColor} or a themable {@code var(--x, currentColor)}, and an explicit
 * 		{@code stroke-width} wherever a stroke is painted.
 * </ul>
 *
 * <h5 class='section'>What it deliberately does not assert</h5>
 * <p>
 * Anything about how the artwork <i>looks</i>. Rasterised review is
 * {@link SymbolSprite_Render_BrowserTest}'s job and it needs a browser, so it sits behind the {@code js-tests}
 * profile. This test is cheap, needs nothing, and therefore runs on every build &mdash; which is the right split,
 * because a provenance guard that only runs under an opt-in profile guards nothing on the default gate.
 */
@SuppressWarnings({
	"java:S8786" // Test regex is intentional; tightening would change match semantics.
})
class SymbolSprite_Provenance_Test extends TestBase {

	/** How to put it back, quoted in every failure message rather than left for the reader to find. */
	private static final String MANIFEST_IS_THE_REVIEW =
		"If the artwork change is intended, update the row in " + SymbolProvenanceScanner.MANIFEST
			+ " - and read that file's `Authoring rules` section first, because editing the row is the reviewed act"
			+ " that this test exists to force.";

	private static final String REQUIRED_VIEWBOX = "0 0 24 24";
	private static final Set<String> ALLOWED_ORIGINS = Set.of("juneau-original", "irs-artwork");

	private static String sprite() throws Exception {
		var s = SymbolProvenanceScanner.read(SymbolProvenanceScanner.SPRITE);
		assertNotNull(s, "could not read " + SymbolProvenanceScanner.SPRITE + " from the module's own resources");
		return s;
	}

	private static String manifest() throws Exception {
		var s = SymbolProvenanceScanner.read(SymbolProvenanceScanner.MANIFEST);
		assertNotNull(s, "could not read " + SymbolProvenanceScanner.MANIFEST + " from the module's own resources");
		return s;
	}

	/**
	 * Appends a redundant {@code Z} to the first path of a document - a byte change that is valid in any path.
	 *
	 * <p>
	 * The needle carries its leading space deliberately: {@code id="} contains {@code d="}, so searching for the
	 * bare attribute name finds every glyph's <i>id</i> first and mutates the stem name instead of the artwork.
	 */
	private static String mutateFirstPath(String svg) {
		var at = svg.indexOf(" d=\"");
		assertTrue(at >= 0, "no path data to mutate; this file's anti-vacuous checks cannot run");
		var close = svg.indexOf('"', at + 4);
		assertTrue(close > at, "unterminated path data");
		return svg.substring(0, close) + "Z" + svg.substring(close);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a: the manifest and the sprite agree
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_manifestCoversExactlyTheShippedGlyphs() throws Exception {
		var shipped = new ArrayList<>(SymbolProvenanceScanner.symbols(sprite()).keySet());
		var declared = new ArrayList<>(SymbolProvenanceScanner.manifestFingerprints(manifest()).keySet());
		assertNotEmpty(() -> "no <symbol> elements found in the sprite - this test would be vacuous", shipped);
		assertList(() -> "the manifest and the sprite describe different glyph sets (or the same set in a different order)."
				+ " A glyph shipped without a manifest row is unpinned artwork; a row without a glyph is a stale"
				+ " approval. " + MANIFEST_IS_THE_REVIEW, declared, shipped.toArray());
	}

	@Test void a02_everyGlyphMatchesItsApprovedFingerprint() throws Exception {
		var shipped = SymbolProvenanceScanner.symbols(sprite());
		var approved = SymbolProvenanceScanner.manifestFingerprints(manifest());
		var drifted = new ArrayList<String>();
		shipped.forEach((stem, element) -> {
			var actual = SymbolProvenanceScanner.fingerprint(element);
			if (!actual.equals(approved.get(stem)))
				drifted.add(stem + ": manifest=" + approved.get(stem) + " actual=" + actual);
		});
		assertEmpty(() -> "the artwork of these glyphs no longer matches what the manifest approved. "
			+ MANIFEST_IS_THE_REVIEW, drifted);
	}

	@Test void a03_everyGlyphDeclaresAnAllowedOrigin() throws Exception {
		// Origins are either Juneau-original or IRS artwork cleared for copy (2026-09-24).  Any other token would
		// leave LICENSE/NOTICE and the dual-hat rule out of sync with the sprite.
		var origins = SymbolProvenanceScanner.manifestOrigins(manifest());
		assertNotEmpty(() -> "no manifest rows parsed - this test would be vacuous", origins);
		var foreign = origins.entrySet().stream()
			.filter(e -> !ALLOWED_ORIGINS.contains(e.getValue()))
			.map(Object::toString)
			.toList();
		assertEmpty(() -> "a glyph declares an origin outside " + ALLOWED_ORIGINS + ". Update the manifest only after the artwork"
				+ " clearance and NOTICE attribution match.", foreign);
	}

	/**
	 * The a04 check as a function of its inputs: every document-family stem must be shipped and declared
	 * {@code irs-artwork}.  Returns one problem string per violation (empty when the family is intact).
	 */
	private static List<String> documentFamilyProblems(Map<String,String> shipped, Map<String,String> origins) {
		var problems = new ArrayList<String>();
		for (var stem : SymbolProvenanceScanner.FRAMED_FAMILY) {
			if (shipped.get(stem) == null)
				problems.add("the document family member " + stem + " is missing from the sprite");
			if (neq(origins.get(stem), "irs-artwork"))
				problems.add("document-family stem " + stem + " is expected to be irs-artwork after the §3.4 replacement"
					+ " but is " + origins.get(stem));
		}
		return problems;
	}

	@Test void a04_documentFamilyMembersRemainShipped() throws Exception {
		// csv / pdf / spreadsheet previously shared a Juneau-original byte-identical frame.  They now ship IRS
		// artwork (design §3.4), so frame-identity is no longer asserted — only that the three stems still exist
		// and keep their irs-artwork origin.
		assertEmpty(() -> "document family drifted. " + MANIFEST_IS_THE_REVIEW,
			documentFamilyProblems(SymbolProvenanceScanner.symbols(sprite()), SymbolProvenanceScanner.manifestOrigins(manifest())));
	}

	@Test void a05_everyGlyphIsNormalisedToTheHostViewBox() throws Exception {
		var offModulus = new ArrayList<String>();
		SymbolProvenanceScanner.symbols(sprite()).forEach((stem, element) -> {
			var vb = SymbolProvenanceScanner.viewBox(element);
			if (neq(vb, REQUIRED_VIEWBOX))
				offModulus.add(stem + ": viewBox=\"" + vb + "\"");
		});
		assertEmpty(() -> "a glyph declares a viewBox other than \"" + REQUIRED_VIEWBOX + "\". juneau-icons.js hard-codes the"
				+ " host <svg> at that modulus, so any other one interposes a scale factor between the author's"
				+ " coordinates and the pixel grid and the glyph's strokes stop landing on pixel boundaries.", offModulus);
	}

	@Test void a06_paintIsOnlyEverNoneOrCurrentColor() throws Exception {
		assertEmpty(() -> "a hard-coded paint value appeared in the sprite. Hover, focus and disabled tinting is a CSS"
				+ " `color` change (or, for a part with a themable `var(--x, currentColor)` paint, a custom-property"
				+ " change), so a glyph painted with a literal colour silently opts out of all of it and needs a"
				+ " second asset to be themed.", SymbolProvenanceScanner.offContractPaints(sprite()));
	}

	@Test void a07_everyStrokedElementDeclaresItsWidth() throws Exception {
		var shipped = SymbolProvenanceScanner.symbols(sprite());
		var underspecified = new ArrayList<String>();
		shipped.forEach((stem, element) ->
			SymbolProvenanceScanner.strokedWithoutWidth(element).forEach(e -> underspecified.add(stem + ": <" + e + ">")));
		assertEmpty(() -> "an element paints a stroke without declaring stroke-width, so its rendered weight depends on"
				+ " where the glyph is used rather than on the glyph.", underspecified);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b: the assertions above can actually fail
	//
	// A provenance guard that silently stopped seeing artwork reads as a passing test, which is strictly worse than
	// no guard - the same discipline RawContentSink_SecurityScan_Test applies to its scanner.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_aOneCharacterArtworkChangeMovesTheFingerprint() throws Exception {
		var real = sprite();
		var mutated = mutateFirstPath(real);
		var before = SymbolProvenanceScanner.symbols(real);
		var after = SymbolProvenanceScanner.symbols(mutated);
		assertList(() -> "the mutation must not change the glyph set", after.keySet(), before.keySet().toArray());
		var stem = before.keySet().iterator().next();
		assertNotEquals(SymbolProvenanceScanner.fingerprint(before.get(stem)),
			SymbolProvenanceScanner.fingerprint(after.get(stem)),
			"a single added path command left the fingerprint unchanged, so a02 is hashing something other than the"
				+ " artwork - a normalised string, or a regex capturing only the opening tag, both look like this");
	}

	@Test void b02_aViewBoxChangeMovesTheFingerprint() throws Exception {
		// The gap this closes was explicit in the item: the stem-id guard never reads viewBox, so before this test
		// a glyph arriving at a foreign modulus passed every guard in the tree silently.
		var real = sprite();
		var mutated = real.replace("viewBox=\"" + REQUIRED_VIEWBOX + "\"", "viewBox=\"0 0 16 16\"");
		assertNotEquals(real, mutated, "the sprite no longer declares the host viewBox, so this check cannot run");
		var before = SymbolProvenanceScanner.symbols(real);
		var after = SymbolProvenanceScanner.symbols(mutated);
		var stem = before.keySet().iterator().next();
		assertNotEquals(SymbolProvenanceScanner.fingerprint(before.get(stem)),
			SymbolProvenanceScanner.fingerprint(after.get(stem)),
			"a viewBox change left the fingerprint unchanged, so the hash is not covering the opening tag");
	}

	@Test void b03_documentFamilyOriginAssertCanFail() throws Exception {
		// a04 pins irs-artwork on the framed-family stems.  Run the SAME check against a mutated origins map and a
		// mutated sprite and require it to report the breach; a vacuous check (or a vacuous origins map) would not.
		var shipped = SymbolProvenanceScanner.symbols(sprite());
		var origins = SymbolProvenanceScanner.manifestOrigins(manifest());
		assertNotEmpty(() -> "precondition: manifest origins parse", origins);
		assertEmpty(() -> "precondition: the unmutated inputs must pass the check", documentFamilyProblems(shipped, origins));

		var first = SymbolProvenanceScanner.FRAMED_FAMILY.get(0);
		var drifted = new LinkedHashMap<>(origins);
		drifted.put(first, "juneau-original");
		assertList(documentFamilyProblems(shipped, drifted),
			"document-family stem " + first + " is expected to be irs-artwork after the §3.4 replacement but is juneau-original");

		var missing = new LinkedHashMap<>(shipped);
		missing.remove(first);
		assertList(documentFamilyProblems(missing, origins), "the document family member " + first + " is missing from the sprite");
	}

	@Test void b04_theManifestRowPatternMatchesEveryRealRow() throws Exception {
		// a01/a02/a03 all read the manifest through one regex.  A manifest reformatted so that regex stopped
		// matching would make all three vacuous at once, and each of them would still pass.
		var declared = SymbolProvenanceScanner.manifestFingerprints(manifest());
		var shipped = SymbolProvenanceScanner.symbols(sprite());
		assertSize(() -> "the manifest's fingerprint-row format no longer parses for every glyph. " + MANIFEST_IS_THE_REVIEW,
			shipped.size(), declared);
	}

	@Test void b05_theScannersDetectAnInjectedBreach() throws Exception {
		var real = sprite();
		assertNotEmpty(() -> "an injected literal colour went undetected, so a06 cannot fail",
			SymbolProvenanceScanner.offContractPaints(real.replace("fill=\"none\"", "fill=\"#1589EE\"")));
		assertNotEmpty(() -> "a literal colour hidden in a var() fallback went undetected, so a06 cannot fail",
			SymbolProvenanceScanner.offContractPaints("<symbol id=\"juneau-sym-x\"><path fill=\"var(--jc-x, #1589EE)\" d=\"M0 0\"/></symbol>"));
		assertEmpty(() -> "a themable var(--x, currentColor) paint must be accepted",
			SymbolProvenanceScanner.offContractPaints("<symbol id=\"juneau-sym-x\"><path fill=\"var(--jc-x, currentColor)\" d=\"M0 0\"/></symbol>"));

		var stripped = real.replaceAll("\\s+stroke-width=\"[^\"]*\"", "");
		assertNotEquals(real, stripped, "the sprite declares no stroke-width at all, so this check cannot run");
		var breaches = SymbolProvenanceScanner.symbols(stripped).values().stream()
			.flatMap(e -> SymbolProvenanceScanner.strokedWithoutWidth(e).stream()).toList();
		assertNotEmpty(() -> "removing every stroke-width went undetected, so a07 cannot fail", breaches);
	}

	@Test void b06_theStrokeWidthScanSeesTheRealStrokedElements() throws Exception {
		// a07 asserts an empty list, which an element pattern that matched nothing would satisfy trivially.  This
		// pins that the sprite really does paint strokes and that they really are being examined.
		var stripped = SymbolProvenanceScanner.read(SymbolProvenanceScanner.SPRITE)
			.replaceAll("\\s+stroke-width=\"[^\"]*\"", "");
		var seen = SymbolProvenanceScanner.symbols(stripped).values().stream()
			.mapToInt(e -> SymbolProvenanceScanner.strokedWithoutWidth(e).size()).sum();
		assertTrue(seen >= 10,
			() -> "only " + seen + " stroked elements were found across the whole sprite after stripping"
				+ " stroke-width - the element pattern has stopped matching the remaining stroked artwork");
	}
}
