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
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Rasterises every glyph in {@code juneau-symbols.svg} at its real render sizes and writes a set of review sheets
 * plus a per-glyph metric report under {@code target/symbol-sprite/}.
 *
 * <h5 class='section'>Why a rasteriser and not a source assertion:</h5>
 * <p>
 * {@code juneau-icons.js} paints an icon by injecting a host {@code <svg viewBox="0 0 24 24">} carrying a single
 * {@code <use href="#juneau-sym-{stem}"/>}, and it deliberately does <b>not</b> fall back when a stem fails to
 * resolve - the host element is simply left empty. So a mangled, mis-scaled or entirely unresolvable glyph
 * renders as <b>nothing, silently</b>, with no error anywhere. Reading the sprite's ids catches a rename;
 * fingerprinting its bytes catches a paste; neither can tell a drawing from a blank square. Only a rasteriser
 * can, and that is what {@link #a01_everyStemRendersWithInkAtBothRenderSizes()} asserts.
 *
 * <h5 class='section'>The other half is a report, not an assertion, and that is deliberate:</h5>
 * <p>
 * These glyphs are painted at 16px (ribbon) and 12px (paging pill). At that size a stroke either lands on the
 * pixel grid or smears across it, and no source-level review can see which. The sheets and metrics written here
 * exist so a human can compare a redrawn set against its predecessor <i>as pixels</i>, and - the part per-glyph
 * review structurally cannot do - see the four document glyphs beside each other, where family drift lives.
 * <p>
 * <b>Do not add a threshold assertion on any of the four per-glyph metrics above (ink/solid/gradient/mush).</b>
 * Antialiasing coverage is a property of the Chromium build doing the rasterising. The {@code js-tests} profile
 * pins that build, which makes these numbers <i>reproducible</i> today but not <i>stable</i> across a future pin
 * bump - so a numeric gate on one of them would convert a routine Playwright version bump into a false artwork
 * regression, and the reflex fix for a false regression is to loosen the number until it passes, which leaves a
 * gate that asserts nothing. The numbers are for a human to read.
 * <p>
 * <b>The one exception is {@link #c01_fourLockedGlyphsStayMutuallyDistinguishable()}</b> ({@code
 * [TODO-J0451]}), which thresholds the prober's {@code adjacencyDiffs} - the mean per-pixel luminance
 * difference <i>between two glyphs rasterised in the same run, same size, same Chromium build</i>, not either
 * glyph's absolute coverage. A pin bump moves both sides of that comparison together, so the pairwise diff does
 * not carry the non-reproducibility problem the paragraph above describes; it is safe to threshold precisely
 * because it is relative, not absolute. Before this class, `cancel`, `columns`, `edit` and `settings` had zero
 * glyph-specific automated coverage - only the generic non-vacuous ink check every stem gets.
 *
 * <h5 class='section'>What is real and what is restated:</h5>
 * <p>
 * The sprite, the stylesheet and the icon registry are all read from the <b>real</b> classpath resources
 * {@link ViewsMixin} serves, and the host markup for every glyph comes from {@code juneau-icons.js}'s own
 * {@code resolveIcon()} at runtime rather than being re-spelled here - every stem happens to be registered under
 * its own name, which is what makes that possible. The render sizes are read out of
 * {@code juneau-views.css}'s {@code --jc-chrome-glyph-size} / {@code -small} custom properties rather than typed
 * as literals, because those values are expected to move. The one restatement is the set of CSS class names used
 * to build the two UI-context strips ({@code juneau-view-ribbon-btn}, the paging-pill classes, and
 * {@code juneau-sym-flip-x}); those live in the prober, because building them from the real emitters would mean
 * loading the whole view runtime to look at eight buttons.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests}
 * Maven profile does. Run it with
 * {@code mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test}. See
 * {@code src/test/js/symbol-sprite-render.cjs} for the prober this drives.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SymbolSprite_StemIds_Test} &mdash; pins the stem id set, always on.
 * 	<li class='jc'>{@link SymbolSprite_Provenance_Test} &mdash; pins the approved artwork bytes, always on.
 * </ul>
 */
@EnabledIfSystemProperty(named=SymbolSprite_Render_BrowserTest.GATE, matches="true",
	disabledReason="Rasterisation harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class SymbolSprite_Render_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	/**
	 * A stem that is deliberately not in the sprite.  The prober measures it like any other, and it must come
	 * back as zero ink - see {@link #a02_theInkMeasurementCanActuallyFail()}.
	 */
	private static final String BOGUS_STEM = "not-a-real-glyph-anti-vacuous-probe";

	private static final Pattern GLYPH_SIZE =
		Pattern.compile("--jc-chrome-glyph-size:\\s*(\\d+(?:\\.\\d+)?)px");
	private static final Pattern GLYPH_SIZE_SMALL =
		Pattern.compile("--jc-chrome-glyph-size-small:\\s*(\\d+(?:\\.\\d+)?)px");
	private static final Pattern SYMBOL_ID = Pattern.compile("<symbol\\s+id=\"juneau-sym-([^\"]+)\"");

	private static Map<?,?> report;
	private static Path outputDir;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Every stem in the sprite, in document order - i.e. the key sheet's grid order. */
	private static List<String> stems(String sprite) {
		var out = new ArrayList<String>();
		var m = SYMBOL_ID.matcher(sprite);
		while (m.find())
			out.add(m.group(1));
		assertFalse(out.isEmpty(), "no stems found in the sprite");
		return out;
	}

	/** A render size read out of the real stylesheet rather than typed here, because these values move. */
	private static double cssPx(Pattern p, String css) {
		var m = p.matcher(css);
		assertTrue(m.find(), () -> "no match for " + p.pattern() + " in juneau-views.css; the render sizes are"
			+ " read from the stylesheet on purpose - if the custom property was renamed, follow it here");
		return Double.parseDouble(m.group(1));
	}

	/**
	 * The fixture: the real stylesheet, the real sprite, and the real icon registry.
	 *
	 * <p>
	 * The sprite is parked in an inert {@code <script type="text/plain">} and handed to the registry through a
	 * stubbed {@code fetch}, so {@code loadSymbolSprite()}'s real parse-and-inject path runs. A {@code file://}
	 * page cannot fetch a sibling file, and inlining the sprite directly would skip that path entirely and log a
	 * console error the prober would then have to learn to ignore.
	 */
	private static String fixture(String css, String sprite, String iconsJs) {
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ css
			+ "\n</style></head><body style=\"margin:0;background:#ffffff;color:#16325c\">\n"
			+ "<script type=\"text/plain\" id=\"sprite-xml\">" + sprite + "</script>\n"
			+ "<script>\n"
			+ "window.fetch = function () {\n"
			+ "  var xml = document.getElementById('sprite-xml').textContent;\n"
			+ "  return Promise.resolve({ ok: true, text: function () { return Promise.resolve(xml); } });\n"
			+ "};\n"
			+ "</script>\n"
			+ "<script>\n" + iconsJs + "\n</script>\n"
			+ "<div id=\"stage\"></div>\n"
			+ "</body></html>";
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent()
			.resolve("symbol-sprite-render.cjs");

		var sprite = resource(ViewsMixin.SYMBOLS_SVG_RESOURCE);
		var css = resource(ViewsMixin.VIEWS_CSS_RESOURCE);
		var iconsJs = resource(ViewsMixin.ICONS_JS_RESOURCE);

		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("symbol-sprite.html");
		Files.write(fixtureFile, fixture(css, sprite, iconsJs).getBytes(UTF_8));

		// Beside target/js rather than inside it: target/js is the profile's node/browser scratch space and is
		// what a developer deletes to force a re-provision, while these sheets are the deliverable.
		outputDir = Files.createDirectories(dir.getParent().resolve("symbol-sprite"));

		var request = Json.of(Map.of(
			"outputDir", outputDir.toString(),
			"stems", stems(sprite),
			"bogusStem", BOGUS_STEM,
			"sizes", List.of(cssPx(GLYPH_SIZE_SMALL, css), cssPx(GLYPH_SIZE, css), 24.0),
			"ribbonSize", cssPx(GLYPH_SIZE, css),
			"pillSize", cssPx(GLYPH_SIZE_SMALL, css),
			// The four-glyph document family plus the one glyph the redraw gave a new meaning, whose 16px
			// distinguishability against both settings and spreadsheet is a named review check.
			"family", List.of("csv", "pdf", "spreadsheet", "copy"),
			// [TODO-J0451] LD-1: every pairwise combination of the four locked-scope glyphs (cancel, columns,
			// edit, settings), plus the pre-existing columns-spreadsheet review-only pair (columns-settings is
			// reused rather than duplicated). See c01_fourLockedGlyphsStayMutuallyDistinguishable() below.
			"adjacencies", List.of(
				List.of("columns", "spreadsheet"),
				List.of("cancel", "columns"),
				List.of("cancel", "edit"),
				List.of("cancel", "settings"),
				List.of("columns", "edit"),
				List.of("columns", "settings"),
				List.of("edit", "settings"))));

		report = Json.to(run(dir, harness, fixtureFile, request), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixture, String request) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(),
			fixture.toString(), request);
		var stdout = dir.resolve("symbol-sprite-render-stdout.json");
		var stderr = dir.resolve("symbol-sprite-render-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(5, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 5m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	/** {@code report.glyphs[stem]["ink12"]} and friends, as a number. */
	private static double metric(String stem, String key) {
		var glyphs = (Map<?,?>) report.get("glyphs");
		assertNotNull(glyphs, () -> "prober report has no glyphs section: " + report);
		var g = (Map<?,?>) glyphs.get(stem);
		assertNotNull(g, () -> "prober report has no entry for stem " + stem + ": " + glyphs.keySet());
		var v = (Number) g.get(key);
		assertNotNull(v, () -> "prober report has no " + key + " for stem " + stem + ": " + g);
		return v.doubleValue();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a: nothing renders blank  (the silent-blank guard - the only durable assertion in this class)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_everyStemRendersWithInkAtBothRenderSizes() {
		var stems = (List<?>) report.get("stems");
		assertNotNull(stems, () -> "prober report has no stem list: " + report);
		assertFalse(stems.isEmpty(), "prober measured no stems at all");
		for (var s : stems) {
			var stem = (String) s;
			assertTrue(metric(stem, "ink16") > 0,
				() -> stem + " rasterised to an empty 16px box; juneau-icons.js renders an unresolvable or"
					+ " mangled glyph as nothing at all, with no error on either side");
			assertTrue(metric(stem, "ink12") > 0,
				() -> stem + " rasterised to an empty 12px box");
		}
	}

	@Test void a02_theInkMeasurementCanActuallyFail() {
		// Without this, a prober that silently stopped rendering would report zero ink for nothing and pass a01
		// for every stem - a harness that measures nothing reads exactly like a harness that measures a clean set.
		assertEquals(0.0, metric(BOGUS_STEM, "ink16"),
			"a stem that is not in the sprite measured as having ink, so the ink measurement is not measuring"
				+ " the sprite");
		assertEquals(0.0, metric(BOGUS_STEM, "ink12"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c: the four [TODO-J0451] LD-1 glyphs stay mutually distinguishable  (real coverage, not a human PNG review)
	//------------------------------------------------------------------------------------------------------------------

	/** {@code [TODO-J0451]} LD-1's exact locked scope - these four stems, and no others. */
	private static final List<String> FOUR_GLYPHS = List.of("cancel", "columns", "edit", "settings");

	/**
	 * Minimum acceptable mean per-pixel luminance diff (0..255 scale; see the prober's {@code PIXEL_DIFF}, in
	 * {@code symbol-sprite-render.cjs}) between any two of {@link #FOUR_GLYPHS}, at either render size.
	 *
	 * <p>
	 * Chosen against the six pairwise diffs measured on {@code HEAD} at filing time, which ranged
	 * <b>30.66 - 50.45</b> (the tightest pair was {@code columns} vs {@code settings} at the 12px paging-pill
	 * size). This floor sits well under half the smallest of those - loose enough that ordinary antialiasing
	 * jitter from a future Playwright/Chromium pin bump should not trip it (both sides of the comparison move
	 * together across a pin bump; see the class javadoc), but tight enough that two glyphs actually drifting
	 * into each other visually - the real risk this check exists for, per the closed {@code Q2} scare where
	 * {@code columns} briefly appeared to have been left as a copy of the {@code settings} cog - would have to
	 * land far closer to identical than any plausible accidental redraw before this goes green.
	 */
	private static final double MIN_PAIR_PIXEL_DIFF = 15.0;

	/** Looks up the prober's {@code adjacencyDiffs} entry for an unordered glyph pair. */
	private static Map<?,?> adjacencyDiff(String a, String b) {
		var diffs = (List<?>) report.get("adjacencyDiffs");
		assertNotNull(diffs, () -> "prober report has no adjacencyDiffs section: " + report);
		for (var d : diffs) {
			var m = (Map<?,?>) d;
			var names = (List<?>) m.get("names");
			if (names != null && names.size() == 2 && names.contains(a) && names.contains(b))
				return m;
		}
		return null;
	}

	@Test void c01_fourLockedGlyphsStayMutuallyDistinguishable() {
		for (var i = 0; i < FOUR_GLYPHS.size(); i++) {
			for (var j = i + 1; j < FOUR_GLYPHS.size(); j++) {
				var a = FOUR_GLYPHS.get(i);
				var b = FOUR_GLYPHS.get(j);
				var pair = adjacencyDiff(a, b);
				assertNotNull(pair, () -> "no adjacencyDiffs entry for " + a + " vs " + b + "; the prober's"
					+ " adjacencies request list must carry every pairwise combination of " + FOUR_GLYPHS);
				var ribbon = ((Number) pair.get("pixelDiffRibbon")).doubleValue();
				var small = ((Number) pair.get("pixelDiffSmall")).doubleValue();
				assertTrue(ribbon >= MIN_PAIR_PIXEL_DIFF, () -> a + " and " + b + " are only " + ribbon
					+ " luminance levels apart at the ribbon (16px) render size - below the "
					+ MIN_PAIR_PIXEL_DIFF + " distinguishability floor, i.e. they risk reading as the same glyph");
				assertTrue(small >= MIN_PAIR_PIXEL_DIFF, () -> a + " and " + b + " are only " + small
					+ " luminance levels apart at the paging-pill (12px) render size - below the "
					+ MIN_PAIR_PIXEL_DIFF + " distinguishability floor");
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// b: the review package exists  (it is a deliverable, so its absence is a failure and not a warning)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_everyReviewSheetWasWritten() {
		for (var name : List.of("sheet.png", "family.png", "contexts.png", "metrics.txt")) {
			var f = outputDir.resolve(name);
			assertTrue(Files.isRegularFile(f), () -> "review artifact not written: " + f);
			assertTrue(f.toFile().length() > 0, () -> "review artifact is empty: " + f);
		}
	}

	@Test void b02_theRuntimeRanCleanly() {
		// The registry logs a console error rather than throwing when the sprite fails to load, so a fixture whose
		// stub fetch had broken would still produce sheets - of twenty empty boxes.
		assertEquals(List.of(), report.get("jsFailures"), () -> "the page logged errors: " + report.get("jsFailures"));
	}
}
