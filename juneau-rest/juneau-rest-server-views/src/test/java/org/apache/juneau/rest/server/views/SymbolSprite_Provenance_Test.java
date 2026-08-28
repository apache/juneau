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
import org.junit.jupiter.api.*;

/**
 * The licensing-drift guard for {@code juneau-symbols.svg}: asserts that the shipped artwork is still the artwork
 * the provenance manifest ({@code juneau-symbols-provenance.md}) approved, and that the sprite still honours the
 * contracts the manifest declares.
 *
 * <h5 class='section'>Why this exists at all, given every glyph is Juneau-original</h5>
 * <p>
 * Because that is a fact about today, and the failure it guards is silent. Eighteen of these twenty glyphs were
 * redrawn from scratch precisely because their predecessors were byte-identical to a proprietary source, and the
 * cheapest way for that to happen again is for someone to paste a glyph out of a design-system sheet during an
 * unrelated polish pass. Nothing else in the build would notice: the artwork renders, the ids are unchanged, and
 * {@link SymbolSprite_StemIds_Test} pins <i>names</i> rather than paths. This test makes such a paste fail until
 * the manifest row is deliberately edited, which turns an invisible act into a reviewable one.
 *
 * <h5 class='section'>Deliberately separate from the stem-id guard</h5>
 * <p>
 * {@link SymbolSprite_StemIds_Test} guards correctness &mdash; a rename blanks the Support Console's toolbar,
 * because it overrides these glyphs by name through {@code registerIcon} and {@code juneau-icons.js} renders an
 * empty host {@code <svg>} for an unresolved stem rather than throwing. This one guards licensing drift. They fail
 * for unrelated reasons and read as unrelated diagnostics, so they are two tests rather than one.
 *
 * <h5 class='section'>What it asserts</h5>
 * <ul>
 * 	<li>The manifest and the sprite describe the same glyph set, in the same order.
 * 	<li>Every glyph's fingerprint still matches its approved value.
 * 	<li>Every glyph's declared origin is {@code juneau-original} &mdash; the fact that makes {@code LICENSE} and
 * 		{@code NOTICE} silent on this sprite, so it is asserted rather than assumed.
 * 	<li>The document family's page-frame path is byte-identical across {@code csv}, {@code pdf} and
 * 		{@code spreadsheet}. This is the mechanical half of drawing those as one set: a worker redrawing one of
 * 		them alone, against its own predecessor, cannot produce a passing deliverable.
 * 	<li>The three contracts the manifest states as rules: {@code viewBox="0 0 24 24"} on every glyph, paint only
 * 		ever {@code none} or {@code currentColor}, and an explicit {@code stroke-width} wherever a stroke is
 * 		painted.
 * </ul>
 *
 * <h5 class='section'>What it deliberately does not assert</h5>
 * <p>
 * Anything about how the artwork <i>looks</i>. Rasterised review is
 * {@link SymbolSprite_Render_BrowserTest}'s job and it needs a browser, so it sits behind the {@code js-tests}
 * profile. This test is cheap, needs nothing, and therefore runs on every build &mdash; which is the right split,
 * because a provenance guard that only runs under an opt-in profile guards nothing on the default gate.
 */
class SymbolSprite_Provenance_Test extends TestBase {

	/** How to put it back, quoted in every failure message rather than left for the reader to find. */
	private static final String MANIFEST_IS_THE_REVIEW =
		"If the artwork change is intended, update the row in " + SymbolProvenanceScanner.MANIFEST
			+ " - and read that file's `Authoring rules` section first, because editing the row is the reviewed act"
			+ " that this test exists to force.";

	private static final String REQUIRED_VIEWBOX = "0 0 24 24";
	private static final String REQUIRED_ORIGIN = "juneau-original";

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
		assertFalse(shipped.isEmpty(), "no <symbol> elements found in the sprite - this test would be vacuous");
		assertEquals(shipped, declared,
			"the manifest and the sprite describe different glyph sets (or the same set in a different order)."
				+ " A glyph shipped without a manifest row is unpinned artwork; a row without a glyph is a stale"
				+ " approval. " + MANIFEST_IS_THE_REVIEW);
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
		assertEquals(List.of(), drifted,
			"the artwork of these glyphs no longer matches what the manifest approved. " + MANIFEST_IS_THE_REVIEW);
	}

	@Test void a03_everyGlyphDeclaresJuneauOriginalOrigin() throws Exception {
		// Not decoration: "all artwork is Juneau-original" is the reason LICENSE and NOTICE say nothing about this
		// sprite (the item's LD-3).  A row that ever said otherwise would make those two files wrong, silently.
		var origins = SymbolProvenanceScanner.manifestOrigins(manifest());
		assertFalse(origins.isEmpty(), "no manifest rows parsed - this test would be vacuous");
		var foreign = origins.entrySet().stream().filter(e -> !REQUIRED_ORIGIN.equals(e.getValue())).map(Object::toString).toList();
		assertEquals(List.of(), foreign,
			"a glyph declares a non-Juneau origin. That is not a manifest problem to fix here: it means LICENSE and"
				+ " NOTICE now owe an attribution they do not carry.");
	}

	@Test void a04_documentFamilySharesAByteIdenticalFramePath() throws Exception {
		var shipped = SymbolProvenanceScanner.symbols(sprite());
		var frames = new LinkedHashMap<String,String>();
		for (var stem : SymbolProvenanceScanner.FRAMED_FAMILY) {
			var element = shipped.get(stem);
			assertNotNull(element, () -> "the document family member " + stem + " is missing from the sprite");
			var frame = SymbolProvenanceScanner.framePath(element);
			assertNotNull(frame, () -> "the document family member " + stem + " declares no path data");
			frames.put(stem, frame);
		}
		var distinct = new LinkedHashSet<>(frames.values());
		assertEquals(1, distinct.size(),
			() -> "the document family's page-frame path is not byte-identical across "
				+ SymbolProvenanceScanner.FRAMED_FAMILY + ", so the three have stopped being one drawing: "
				+ frames + ". The frame is declared in the manifest's `Document family` section and the members are"
				+ " drawn from it; redrawing one member alone against its own predecessor is what this catches.");
	}

	@Test void a05_everyGlyphIsNormalisedToTheHostViewBox() throws Exception {
		var offModulus = new ArrayList<String>();
		SymbolProvenanceScanner.symbols(sprite()).forEach((stem, element) -> {
			var vb = SymbolProvenanceScanner.viewBox(element);
			if (!REQUIRED_VIEWBOX.equals(vb))
				offModulus.add(stem + ": viewBox=\"" + vb + "\"");
		});
		assertEquals(List.of(), offModulus,
			"a glyph declares a viewBox other than \"" + REQUIRED_VIEWBOX + "\". juneau-icons.js hard-codes the"
				+ " host <svg> at that modulus, so any other one interposes a scale factor between the author's"
				+ " coordinates and the pixel grid and the glyph's strokes stop landing on pixel boundaries.");
	}

	@Test void a06_paintIsOnlyEverNoneOrCurrentColor() throws Exception {
		assertEquals(List.of(), SymbolProvenanceScanner.offContractPaints(sprite()),
			"a hard-coded paint value appeared in the sprite. Hover, focus and disabled tinting is a CSS `color`"
				+ " change, so a glyph painted with a literal colour silently opts out of all of it and needs a"
				+ " second asset to be themed.");
	}

	@Test void a07_everyStrokedElementDeclaresItsWidth() throws Exception {
		var shipped = SymbolProvenanceScanner.symbols(sprite());
		var underspecified = new ArrayList<String>();
		shipped.forEach((stem, element) ->
			SymbolProvenanceScanner.strokedWithoutWidth(element).forEach(e -> underspecified.add(stem + ": <" + e + ">")));
		assertEquals(List.of(), underspecified,
			"an element paints a stroke without declaring stroke-width, so its rendered weight depends on where"
				+ " the glyph is used rather than on the glyph.");
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
		assertEquals(before.keySet(), after.keySet(), "the mutation must not change the glyph set");
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

	@Test void b03_theFrameComparisonCanFail() throws Exception {
		var shipped = SymbolProvenanceScanner.symbols(sprite());
		var frames = SymbolProvenanceScanner.FRAMED_FAMILY.stream()
			.map(s -> SymbolProvenanceScanner.framePath(shipped.get(s))).toList();
		assertEquals(1, new LinkedHashSet<>(frames).size(), "precondition: a04 is passing on the real sprite");

		// One member's frame, mutated the way a solo redraw would produce - a different string of the same shape.
		var drifted = new ArrayList<>(frames);
		drifted.set(0, drifted.get(0) + "Z");
		assertNotEquals(1, new LinkedHashSet<>(drifted).size(),
			"a mutated frame path still compared as identical, so a04 is comparing something that cannot differ"
				+ " - a normalised form, or the same element three times");
	}

	@Test void b04_theManifestRowPatternMatchesEveryRealRow() throws Exception {
		// a01/a02/a03 all read the manifest through one regex.  A manifest reformatted so that regex stopped
		// matching would make all three vacuous at once, and each of them would still pass.
		var declared = SymbolProvenanceScanner.manifestFingerprints(manifest());
		var shipped = SymbolProvenanceScanner.symbols(sprite());
		assertEquals(shipped.size(), declared.size(),
			"the manifest's fingerprint-row format no longer parses for every glyph. " + MANIFEST_IS_THE_REVIEW);
	}

	@Test void b05_theScannersDetectAnInjectedBreach() throws Exception {
		var real = sprite();
		assertFalse(SymbolProvenanceScanner.offContractPaints(real.replace("fill=\"none\"", "fill=\"#1589EE\"")).isEmpty(),
			"an injected literal colour went undetected, so a06 cannot fail");

		var stripped = real.replaceAll("\\s+stroke-width=\"[^\"]*\"", "");
		assertNotEquals(real, stripped, "the sprite declares no stroke-width at all, so this check cannot run");
		var breaches = SymbolProvenanceScanner.symbols(stripped).values().stream()
			.flatMap(e -> SymbolProvenanceScanner.strokedWithoutWidth(e).stream()).toList();
		assertFalse(breaches.isEmpty(), "removing every stroke-width went undetected, so a07 cannot fail");
	}

	@Test void b06_theStrokeWidthScanSeesTheRealStrokedElements() throws Exception {
		// a07 asserts an empty list, which an element pattern that matched nothing would satisfy trivially.  This
		// pins that the sprite really does paint strokes and that they really are being examined.
		var stripped = SymbolProvenanceScanner.read(SymbolProvenanceScanner.SPRITE)
			.replaceAll("\\s+stroke-width=\"[^\"]*\"", "");
		var seen = SymbolProvenanceScanner.symbols(stripped).values().stream()
			.mapToInt(e -> SymbolProvenanceScanner.strokedWithoutWidth(e).size()).sum();
		assertTrue(seen >= 18,
			() -> "only " + seen + " stroked elements were found across the whole sprite, which is fewer than there"
				+ " are redrawn glyphs - the element pattern has stopped matching the real artwork");
	}
}
