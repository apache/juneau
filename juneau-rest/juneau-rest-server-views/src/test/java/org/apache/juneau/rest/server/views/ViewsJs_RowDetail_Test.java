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
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the row-detail JS helpers.  Source-shape always runs; behavioral Node harness runs
 * when {@code node} is on {@code PATH} (skipped otherwise — no {@code -Pjs-tests} required).
 */
class ViewsJs_RowDetail_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.RENDERS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsCss() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_CSS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_CSS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_helpersExportedOnNsInit() throws Exception {
		var body = viewsJs();
		for (var name : new String[]{
			"isSafeDetailUrl: isSafeDetailUrl",
			"substituteDetailUrl: substituteDetailUrl",
			"scalarFieldValue: scalarFieldValue",
			"isSafeMarkdownHref: isSafeMarkdownHref",
			"fillMarkdownSlot: fillMarkdownSlot",
			"fillRenderSlot: fillRenderSlot",
			"fillDetailSlots: fillDetailSlots",
			"resolveDetailHeaderIcon: resolveDetailHeaderIcon",
			"paintActionMessageIntoDetail: paintActionMessageIntoDetail",
			"findRowDetailTemplate: findRowDetailTemplate",
			"detailTabTargetIndex: detailTabTargetIndex",
			"activateDetailTab: activateDetailTab",
			"buildDetailStrip: buildDetailStrip",
			"renderAsyncStatus: renderAsyncStatus",
			"JUNEAU_ROW_DETAIL_CONTRACT_VERSION: JUNEAU_ROW_DETAIL_CONTRACT_VERSION"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
		assertFalse(body.contains("function buildDetailFields("), body);
		assertFalse(body.contains("function buildDetailPanel("), body);
		assertTrue(body.contains("submitRowAction(action, table, parentTr"),
			"write path must target the expanded DataTables row, not expand JSON");
	}

	@Test void a02_loadingRegionUsesTheSharedScaleWithoutDeclaringChromeTokens() throws Exception {
		var css = viewsCss();
		var start = css.indexOf(".juneau-view-loading-region {");
		assertTrue(start >= 0, css);
		var rule = css.substring(start, css.indexOf("}", start));
		assertTrue(rule.contains("min-height: var(--jc-chrome-control-height)"), rule);
		assertTrue(rule.contains("padding: 0 var(--jc-chrome-control-padding-x)"), rule);
		assertTrue(rule.contains("font-size: var(--jc-chrome-font-size-2)"), rule);
		assertTrue(rule.contains("currentColor"), rule);
		assertFalse(rule.matches("(?s).*--jc-chrome-[\\w-]+\\s*:.*"), rule);
		assertFalse(rule.contains("#"), rule);
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			report = Json.to(runNode(harness, viewsFile, rendersFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(rendersFile);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/row-detail.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/row-detail.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/row-detail.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path viewsJs, Path rendersJs) throws Exception {
		var stdout = Files.createTempFile("row-detail-stdout-", ".json");
		var stderr = Files.createTempFile("row-detail-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), viewsJs.toString(), rendersJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("row-detail.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("row-detail.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}

	private static Map<?,?> report() {
		assumeTrue(report != null, "node not available or row-detail.cjs not found — behavioral layer skipped");
		return report;
	}

	/** Compares a numeric report value regardless of whether the JSON parser boxed it as Integer or Long. */
	private static void assertNum(long expected, Object actual) {
		assertInstanceOf(Number.class, actual, () -> "expected a number, got: " + actual);
		assertEquals(expected, ((Number)actual).longValue());
	}

	@Test void b01_urlSafety() {
		var r = report();
		assertEquals(true, r.get("url_pathOk"));
		assertEquals(true, r.get("url_relativeOk"));
		assertEquals(false, r.get("url_absolute"));
		assertEquals(false, r.get("url_protoRel"));
		assertEquals(false, r.get("url_scheme"));
		assertEquals(false, r.get("url_dotdot"));
	}

	@Test void b02_hostileId_isEncoded_notPathTraversal() {
		var r = report();
		assertEquals(true, r.get("sub_hostileEncoded"));
		assertNull(r.get("sub_absoluteTpl"));
		assertEquals("/data/alerts/a1", r.get("sub_plain"));
		var hostileUrl = String.valueOf(r.get("sub_hostile"));
		assertFalse(hostileUrl.contains("../"), hostileUrl);
		assertTrue(hostileUrl.startsWith("/data/alerts/"), hostileUrl);
	}

	@Test void b03_scalarAndBasicFill_usesTextContent() {
		var r = report();
		assertEquals("hi", r.get("scalar_str"));
		assertEquals("7", r.get("scalar_num"));
		assertEquals("true", r.get("scalar_bool"));
		assertEquals("", r.get("scalar_null"));
		assertEquals("", r.get("scalar_obj"));
		assertEquals("", r.get("scalar_arr"));
		assertEquals(true, r.get("fill_xssNotInterpreted"));
		assertTrue(String.valueOf(r.get("fill_xss")).contains("<img"));
		assertEquals("42", r.get("fill_num"));
		assertEquals("", r.get("fill_missing"));
	}

	@Test void b17_markdownSlotFill_xssSafety() {
		var r = report();
		assertEquals(true, r.get("hasFillMarkdown"));
		assertEquals(false, r.get("md_hasScript"));
		assertEquals(false, r.get("md_hasImg"));
		assertEquals(false, r.get("md_jsHref"));
		assertEquals(true, r.get("md_httpsHref"));
		assertEquals(true, r.get("md_textHasOk"));
		assertEquals(true, r.get("md_textHasX"));
		assertEquals(true, r.get("md_textHasY"));
		assertEquals(false, r.get("md_textHasAlert"));
	}

	/**
	 * The hostile half of {@link DetailField.Format#SANITIZED_HTML}: a battery of real XSS vectors painted
	 * through the format's own allowlist copier.  This is the second, independent gate behind the caller's
	 * server-side sanitizer &mdash; if that upstream pass is wrong, nothing here may execute.
	 */
	@Test void b17a_sanitizedHtmlSlotFill_xssVectorsAreStripped() {
		var r = report();
		assertEquals(true, r.get("hasFillSanitizedHtmlSlot"), "fillSanitizedHtmlSlot must be exported");

		// Dangerous elements are dropped WITH their children (a script body must not survive as text).
		assertEquals(false, r.get("sh_hasScript"), "<script> (and <ScRiPt>) must not survive");
		assertEquals(false, r.get("sh_hasSvg"), "<svg/onload> must not survive");
		assertEquals(false, r.get("sh_hasIframe"));
		assertEquals(false, r.get("sh_hasForm"));
		assertEquals(false, r.get("sh_hasInput"));
		assertEquals(false, r.get("sh_hasTemplate"));
		assertEquals(false, r.get("sh_hasNoscript"));
		assertEquals(false, r.get("sh_hasObject"));
		assertEquals(false, r.get("sh_hasEmbed"));
		assertEquals(false, r.get("sh_textHasAlert"), "no alert(n) payload may survive as visible text");

		// The structural guarantee: attributes are copied by an explicit allowlist, so NO on* handler, style,
		// class or id can be present anywhere in the painted subtree - by construction, not by deny-list.
		assertEquals(false, r.get("sh_anyOnAttr"),
			() -> "an on* handler attribute survived; painted attrs were: " + r.get("sh_attrNames"));
		assertEquals(false, r.get("sh_anyStyleAttr"),
			() -> "a style attribute survived; painted attrs were: " + r.get("sh_attrNames"));
		assertEquals(false, r.get("sh_anyClassAttr"));

		// javascript: URLs - both literal and entity-encoded - lose their href, but keep their link text.
		assertNum(2, r.get("sh_anchorCount"));
		assertEquals(false, r.get("sh_anyJsHref"), "no javascript: href may be copied");
		assertEquals(true, r.get("sh_anchorTextKept"));

		// data: and protocol-relative image sources are rejected; the <img> stays as a visible gap.  The count is
		// asserted so this pair cannot pass vacuously by the images having been dropped entirely.
		assertNum(3, r.get("sh_imgCount"));
		assertEquals(false, r.get("sh_dataUriSrc"),
			() -> "a data: img src survived; srcs were: " + r.get("sh_imgSrcs"));
		assertEquals(false, r.get("sh_protoRelSrc"),
			() -> "a protocol-relative img src survived; srcs were: " + r.get("sh_imgSrcs"));

		assertEquals(true, r.get("sh_textHasStyled"), "an element loses its attrs, not its content");
	}

	/**
	 * Benign markup nested inside and beside hostile markup survives intact.  Deliberately its own fixture: when
	 * this rode at the end of the big battery above, an earlier vector's parse swallowed it and the assertion
	 * passed on an empty subtree.
	 */
	@Test void b17a2_sanitizedHtmlSlotFill_benignSurvivesNestedHostileMarkup() {
		var r = report();
		assertEquals(true, r.get("sh_survivorBold"), "<b> beside a dropped <script> must survive");
		assertEquals(true, r.get("sh_textHasSurvivor"));
		assertEquals(true, r.get("sh_nestedDeepKept"), "nesting depth must not defeat the copier");
		assertEquals(true, r.get("sh_nestedNoScript"));
		assertEquals(true, r.get("sh_nestedNoAlert"));
		assertEquals(false, r.get("sh_nestedNoOnAttr"), "a nested onerror must not survive");
	}

	/**
	 * Recursion-depth bound on the copier ({@code appendSanitizedHtmlChild}/{@code copySanitizedHtmlChildren},
	 * mutually recursive, one call-frame pair per level of input nesting). A payload nested far beyond any real
	 * document must degrade safely - dropping everything past the copier's depth cap - rather than exhausting
	 * the JS call stack and throwing an uncaught {@code RangeError}. Asserted two ways: it must not throw, AND
	 * the copied result must be bounded well below the input depth (not "didn't throw because nothing survived
	 * at all," and not "didn't throw because it copied the whole thing" - both of which a weaker assertion would
	 * miss).
	 */
	@Test void b17a3_sanitizedHtmlSlotFill_deepNestingDegradesSafely() {
		var r = report();
		assertEquals(true, r.get("sh_deepNesting_doesNotThrow"),
			"a deeply-nested payload must degrade safely, not throw an uncaught RangeError");
		assertEquals(true, r.get("sh_deepNesting_depthIsPositive"), () -> r.toString());
		assertEquals(true, r.get("sh_deepNesting_depthIsBounded"),
			() -> "copied depth must be bounded well below the input depth: " + r.get("sh_deepNesting_depth"));
	}

	/** The benign half: SANITIZED_HTML must actually render as HTML, not as escaped text. */
	@Test void b17b_sanitizedHtmlSlotFill_benignMarkupSurvives() {
		var r = report();
		assertEquals(true, r.get("sh_ok_hasB"), () -> "tags: " + r.get("sh_ok_tags"));
		assertEquals(true, r.get("sh_ok_hasI"));
		assertEquals(true, r.get("sh_ok_hasList"));
		assertEquals(true, r.get("sh_ok_hasTable"), () -> "tags: " + r.get("sh_ok_tags"));
		assertEquals(true, r.get("sh_ok_hasImg"), "the whole point of this format over MARKDOWN is <img>");
		assertEquals(true, r.get("sh_ok_hasBlockquote"));
		assertEquals(true, r.get("sh_ok_hasPreCode"));
		assertEquals(true, r.get("sh_ok_hasHr"));

		// Rendered as markup, not escaped into the text layer.
		assertEquals(false, r.get("sh_ok_textHasMarkup"), "markup must be elements, not literal angle brackets");
		assertEquals(true, r.get("sh_ok_textHasWords"));

		// Allowlisted attributes are carried through with their values intact.
		assertEquals("https://example.com/x", r.get("sh_ok_href"));
		assertEquals("t", r.get("sh_ok_title"));
		assertEquals("https://cdn.example/p.png", r.get("sh_ok_imgSrc"));
		assertEquals("shot", r.get("sh_ok_imgAlt"));
		assertEquals("640", r.get("sh_ok_imgWidth"));
		assertEquals("2", r.get("sh_ok_colspan"));
		assertEquals("3", r.get("sh_ok_rowspan"));
	}

	/** Bounded-integer attributes reject non-numeric, negative, and out-of-range values rather than copying them. */
	@Test void b17c_sanitizedHtmlSlotFill_boundedIntAttrsRejectJunk() {
		var r = report();
		assertNull(r.get("sh_badColspan"), "colspan=\"abc\" must not be copied");
		assertNull(r.get("sh_badRowspan"), "rowspan=\"-1\" must not be copied");
		assertNull(r.get("sh_oobColspan"), "colspan=\"99999\" is out of range and must not be copied");
		assertNull(r.get("sh_badWidth"), "width=\"1e9\" must not be copied");
		assertNull(r.get("sh_badHeight"), "height=\"-5\" must not be copied");
	}

	/** Unknown tags unwrap (children kept) and empty/null values clear the slot - same contract as markdown. */
	@Test void b17d_sanitizedHtmlSlotFill_unwrapAndEmptyHandling() {
		var r = report();
		assertEquals(true, r.get("sh_unknownUnwrapped"));
		assertEquals(true, r.get("sh_unknownChildKept"));
		assertEquals(true, r.get("sh_emptyClears"));
		assertEquals(true, r.get("sh_nullClears"));
	}

	/** {@code isSafeImageSrc} directly: http(s) and same-origin only; no data:, no protocol-relative. */
	@Test void b17e_sanitizedHtml_imageSrcSchemeAllowlist() {
		var r = report();
		assertEquals(true, r.get("shSrc_https"));
		assertEquals(true, r.get("shSrc_http"));
		assertEquals(true, r.get("shSrc_absPath"));
		assertEquals(true, r.get("shSrc_relative"));
		assertEquals(false, r.get("shSrc_data"));
		assertEquals(false, r.get("shSrc_js"));
		assertEquals(false, r.get("shSrc_vbscript"));
		assertEquals(false, r.get("shSrc_protoRel"));
		assertEquals(false, r.get("shSrc_fragment"));
		assertEquals(false, r.get("shSrc_leadingSpaceJs"), "leading whitespace must not launder javascript:");
		assertEquals(false, r.get("shSrc_mixedCaseJs"), "case must not launder javascript:");
		assertEquals(false, r.get("shSrc_empty"));
		assertEquals(false, r.get("shSrc_null"));
	}

	/**
	 * {@code WORK-J0516}: {@code isSafeImageSrc} must reject protocol-relative {@code img[src]} values,
	 * including backslash/tab/CR/LF/triple-slash/leading-whitespace-obfuscated variants that the WHATWG URL
	 * parser normalizes to the same third-party origin as a literal {@code //host} - a naive
	 * {@code startsWith("//")} check alone is insufficient. A true single-leading-slash path and other
	 * legitimate srcs must not regress.
	 */
	@Test void b17e2_imageSrcProtocolRelativeVariantsRejected() {
		var r = report();
		assertEquals(false, r.get("shSrc_protoRel"), "literal //host must be rejected");
		assertEquals(false, r.get("shSrc_backslashProtoRel"),
			"backslash-obfuscated (two literal backslashes before the host) must resolve like //host and be rejected");
		assertEquals(false, r.get("shSrc_tabProtoRel"), "tab-obfuscated (/<TAB>/host) must be rejected");
		assertEquals(false, r.get("shSrc_lfProtoRel"), "LF-obfuscated (/<LF>/host) must be rejected");
		assertEquals(false, r.get("shSrc_crProtoRel"), "CR-obfuscated (/<CR>/host) must be rejected");
		assertEquals(false, r.get("shSrc_tripleSlashProtoRel"), "triple-slash (///host) must be rejected");
		assertEquals(false, r.get("shSrc_leadingSpaceProtoRel"), "leading-whitespace (' //host') must be rejected");
		// Legit cases must not regress.
		assertEquals(true, r.get("shSrc_absPath"), "a true single-leading-slash path must still be accepted");
		assertEquals(true, r.get("shSrc_relative"), "a scheme-less relative path must still be accepted");
		assertEquals(true, r.get("shSrc_https"));
		assertEquals(true, r.get("shSrc_http"));
	}

	/**
	 * End-to-end through {@code fillDetailSlots}: the {@code "sanitizedHtml"} wire token must really dispatch to
	 * the sanitized painter.  Without this, a broken dispatch would silently fall through to {@code textContent}
	 * - which looks safe and would leave every assertion above passing while the format did nothing.  The
	 * near-miss token asserts the fail-safe default is still textContent for anything unrecognized.
	 */
	@Test void b17f_sanitizedHtml_wireTokenDispatchesEndToEnd() {
		var r = report();
		assertEquals(true, r.get("sh_e2e_renderedAsHtml"), "\"sanitizedHtml\" must dispatch to the HTML painter");
		assertEquals(true, r.get("sh_e2e_scriptDropped"));
		assertEquals(true, r.get("sh_e2e_textNoAlert"));
		assertEquals(true, r.get("sh_unknownFormatIsText"),
			"an unrecognized format token must still fall back to textContent");
	}

	@Test void b18_hrefAndTitleFill_xssSafety() {
		var r = report();
		assertEquals(false, r.get("href_js"));
		assertEquals(true, r.get("href_https"));
		assertEquals(false, r.get("href_data"));
		assertEquals("Incident #42", r.get("title_filled"));
		assertEquals(true, r.get("title_xssNotInterpreted"));
		assertTrue(String.valueOf(r.get("title_xss")).contains("<img"));
	}

	/**
	 * {@code WORK-J0516}: {@code isSafeMarkdownHref} must reject protocol-relative {@code a[href]} values,
	 * including backslash/tab/CR/LF/triple-slash/leading-whitespace-obfuscated variants that the WHATWG URL
	 * parser normalizes to the same third-party origin as a literal {@code //host}. This is a phishing/
	 * open-redirect fix, not an XSS one - {@code javascript:} is already rejected via the colon-fallback rule
	 * exercised by {@link #b18_hrefAndTitleFill_xssSafety()}. A true single-leading-slash path, a scheme-less
	 * relative path, the allowlisted {@code mailto:} scheme, and a bare fragment must not regress - this
	 * changes existing MARKDOWN-format behavior deliberately (see the item).
	 */
	@Test void b18a_hrefProtocolRelativeVariantsRejected() {
		var r = report();
		assertEquals(false, r.get("href_protoRel"), "literal //host must be rejected");
		assertEquals(false, r.get("href_backslash"),
			"backslash-obfuscated (two literal backslashes before the host) must resolve like //host and be rejected");
		assertEquals(false, r.get("href_tabObfuscated"), "tab-obfuscated (/<TAB>/host) must be rejected");
		assertEquals(false, r.get("href_lfObfuscated"), "LF-obfuscated (/<LF>/host) must be rejected");
		assertEquals(false, r.get("href_crObfuscated"), "CR-obfuscated (/<CR>/host) must be rejected");
		assertEquals(false, r.get("href_tripleSlash"), "triple-slash (///host) must be rejected");
		assertEquals(false, r.get("href_leadingSpaceProtoRel"), "leading-whitespace (' //host') must be rejected");
		// Legit cases must not regress.
		assertEquals(true, r.get("href_singleSlashPath"), "a true single-leading-slash path must still be accepted");
		assertEquals(true, r.get("href_relativePath"), "a scheme-less relative path must still be accepted");
		assertEquals(true, r.get("href_mailto"), "the allowlisted mailto: scheme must still be accepted");
		assertEquals(true, r.get("href_fragment"), "a bare fragment must still be accepted");
		assertEquals(true, r.get("href_https"));
	}

	@Test void b19_paintActionMessageAndHeaderIcon() {
		var r = report();
		assertEquals(true, r.get("hasPaintActionMessageIntoDetail"));
		assertEquals("<b>disk full</b>", r.get("paint_text"));
		assertEquals(true, r.get("paint_xssNotInterpreted"));
		assertEquals(true, r.get("hasResolveDetailHeaderIcon"));
		assertEquals(true, r.get("icon_unknownHidden"));
	}

	@Test void b20_asyncStatusRendersAllStatesWithoutTouchingConsumerContent() {
		var r = report();
		assertEquals(true, r.get("hasRenderAsyncStatus"));
		assertEquals(true, r.get("async_defaultLoadingText"));
		assertEquals(true, r.get("async_statusRole"));
		assertEquals(true, r.get("async_marker"));
		assertEquals(true, r.get("async_spinnerDecorative"));
		assertEquals(true, r.get("async_loadingOverride"));
		assertEquals(true, r.get("async_noDuplicateLoading"));
		assertEquals(true, r.get("async_errorText"));
		assertEquals(true, r.get("async_errorHasNoSpinner"));
		assertEquals(true, r.get("async_emptyText"));
		assertEquals(true, r.get("async_emptyHasNoSpinner"));
		assertEquals(true, r.get("async_okRemovesStatus"));
		assertEquals(true, r.get("async_preservesConsumerContent"));
		assertEquals(true, r.get("async_noDuplicateAfterRefetch"));
	}

	@Test void b07_fillRenderSlot_tagProgressLinkedAndCanary() {
		var r = report();
		assertEquals(true, r.get("hasFillRender"));
		assertEquals(true, r.get("rr_tagHasClass"));
		assertEquals(true, r.get("rr_progressWidth"));
		assertEquals(true, r.get("rr_linkedHref"));
		assertEquals(false, r.get("rr_jsHref"));
		assertEquals(false, r.get("rr_hasScript"));
		assertEquals(false, r.get("rr_hostileStyle"));
		assertEquals(true, r.get("rr_truncateTitle"));
		assertEquals(true, r.get("rr_jsonCode"));
		assertEquals(true, r.get("rr_malformedMetaOk"));
		assertEquals("", r.get("rr_missing"));
		assertEquals(true, r.get("rr_dispatchRenderFirst"));
	}

	/**
	 * Pins the fix for {@code javascript:S5852} in {@code copyRenderStyle}.  Its width pattern ended in three
	 * independent {@code \s*} runs separated by optional {@code %} and {@code ;}, so a single whitespace tail
	 * could be split among them in cubically many ways; a style attribute of {@code width:1} + 4,000 spaces +
	 * a non-terminator backtracked for ~10.7s before the fix and ~0ms after.  The attribute must still be
	 * rejected - it is not a bare width declaration - so this pins throughput, not a change in what is allowed.
	 */
	@Test void b08_fillRenderSlot_widthStylePatternIsLinearOnAWhitespaceTail() {
		var r = report();
		assertEquals(true, r.get("rr_slowStyleSawSpan"));    // the span survived parsing; the guard really ran
		assertEquals(true, r.get("rr_slowStyleRejected"));   // still not copied through as a style attribute
		var ms = ((Number) r.get("rr_slowStyleMs")).longValue();
		assertTrue(ms < 2000, () -> "copyRenderStyle took " + ms + "ms on a 4,000-space tail; expected linear");
	}

	@Test void b04_404500_actionRefButtonless_collapseRemains() {
		var r = report();
		assertEquals(true, r.get("fail_ackDisabled"));
		assertEquals(true, r.get("fail_ackHidden"));
		assertEquals(true, r.get("fail_escHidden"));
		assertEquals(true, r.get("fail_collapseEnabled"));
	}

	@Test void b05_coalesceKeyAndDropIfOrphaned() {
		var r = report();
		assertEquals("a1:3", r.get("key_a1"));
		assertEquals(true, r.get("drop_gone"));
		assertEquals(true, r.get("drop_gen"));
		assertEquals(false, r.get("drop_keep"));
	}

	@Test void b06_loudContractMismatch() {
		var r = report();
		assertEquals("1", r.get("contractVersion"));
		assertEquals(true, r.get("contract_ok"));
		assertEquals(false, r.get("contract_bad"));
		assertEquals(false, r.get("contract_missing"));
	}

	@Test void b08_tabTargetIndex_rovingKeyboardMath() {
		var r = report();
		assertEquals(true, r.get("hasDetailTabTargetIndex"));
		assertNum(1, r.get("tti_right"));
		assertNum(0, r.get("tti_rightWrap"));     // ArrowRight wraps to the first tab
		assertNum(2, r.get("tti_left"));          // ArrowLeft wraps to the last tab
		assertNum(0, r.get("tti_home"));
		assertNum(2, r.get("tti_end"));
		assertNum(-1, r.get("tti_other"));        // unhandled key
	}

	@Test void b09_multiSection_buildsTabStrip_oneVisiblePane() {
		var r = report();
		assertEquals(true, r.get("hasBuildDetailStrip"));
		assertEquals(true, r.get("strip_built"));
		assertEquals(true, r.get("strip_isFirstChild"));
		assertEquals("tablist", r.get("strip_role"));
		assertEquals("tab", r.get("strip_mode"));
		assertEquals(true, r.get("strip_hasRibbonGroupClass"));   // shared strip widget - not a new grammar
		assertNum(2, r.get("strip_tabCount"));
		assertEquals("Overview,Context", r.get("strip_labels"));
		assertEquals("juneau-view-ribbon-btn", r.get("strip_btnClass"));
		assertEquals("true", r.get("strip_firstSelected"));
		assertEquals("false", r.get("strip_secondSelected"));
		assertNum(0, r.get("strip_firstTabindex"));
		assertNum(-1, r.get("strip_secondTabindex"));         // roving tabindex
		assertEquals(false, r.get("strip_pane0Hidden"));          // first pane initially visible
		assertEquals(true, r.get("strip_pane1Hidden"));
		assertEquals("tabpanel", r.get("strip_pane0Role"));
		assertEquals(true, r.get("strip_pane0Labelledby"));
		assertEquals(true, r.get("strip_tab0Controls"));
		assertEquals(true, r.get("strip_titleHidden"));           // stacked <h2> hidden - the tab replaces it
	}

	@Test void b10_activateDetailTab_visibilityOnly() {
		var r = report();
		assertEquals(true, r.get("hasActivateDetailTab"));
		assertEquals(true, r.get("act_tab1Selected"));
		assertEquals(true, r.get("act_tab0Deselected"));
		assertEquals(true, r.get("act_pane1Visible"));
		assertEquals(true, r.get("act_pane0Hidden"));
	}

	@Test void b11_keyboardNavigation_moveSelectionAndFocus() {
		var r = report();
		assertEquals(true, r.get("kbd_right_tab1Selected"));
		assertEquals(true, r.get("kbd_right_tab0Deselected"));
		assertEquals(true, r.get("kbd_right_pane1Visible"));
		assertEquals(true, r.get("kbd_right_focusMoved"));
		assertEquals(true, r.get("kbd_home_tab0Selected"));
		assertEquals(true, r.get("kbd_end_tab1Selected"));
		assertEquals(true, r.get("kbd_left_tab0Selected"));
		assertEquals(true, r.get("kbd_enter_noop"));
	}

	@Test void b12_singleSection_staysStripLess() {
		var r = report();
		assertEquals(true, r.get("single_noStrip"));
		assertEquals(true, r.get("single_firstStillSection"));
		assertEquals(true, r.get("single_paneNotHidden"));
		assertEquals(true, r.get("single_noTabpanelRole"));       // no lone tab, no tabpanel role
		assertEquals(true, r.get("single_titleNotHidden"));
	}

	@Test void b13_skillsFixture_becomesTabs() {
		var r = report();
		assertEquals("Skill,SKILL.md", r.get("skills_labels"));
		assertNum(2, r.get("skills_tabCount"));
	}

	@Test void b14_tabSwitch_parentVisibilityOnly_firesNestedActivationSeam() {
		var r = report();
		assertNum(0, r.get("noRefetch_fetchCalls"));          // parent detail envelope is DOM-visibility only
		assertEquals(true, r.get("noRefetch_clickSelectedTab0"));
		// The onActivate seam (the hook a newly-shown pane's nested table uses to run its own GET) fires on every
		// tab activation - keyboard then click - carrying the correct section id + the activated pane.
		assertNum(2, r.get("noRefetch_onActivateCount"));
		assertEquals("b", r.get("noRefetch_onActivateFirstSid"));
		assertEquals("a", r.get("noRefetch_onActivateLastSid"));
		assertEquals(true, r.get("noRefetch_onActivatePaneMatches"));
	}

	@Test void b15_headerKeepsStripAfterHeader() {
		var r = report();
		assertEquals(true, r.get("header_firstIsHeader"));
		assertEquals(true, r.get("header_stripAfterHeader"));
	}

	@Test void b16_findRowDetailTemplate_survivesDataTablesWrap() {
		var r = report();
		assertEquals(true, r.get("hasFindRowDetailTemplate"));
		assertEquals(true, r.get("find_sibling"));
		assertEquals(true, r.get("find_dt2Wrap"));
		assertEquals(true, r.get("find_missing"));
	}

	@Test void c01_actionRuleHelpersExported_andEvaluatedAfterTheLifecycleGate() throws Exception {
		var body = viewsJs();
		for (var name : new String[]{
			"actionRuleMatches: actionRuleMatches",
			"firstFailingActionRule: firstFailingActionRule",
			"applyActionRefRules: applyActionRefRules",
			"mintActionDescIdentity: mintActionDescIdentity"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
		// Ordering is the whole composition story: the rule pass must run AFTER setActionRefEnabled on the success
		// path, so it can only narrow the lifecycle gate rather than fight it.
		var enableAt = body.indexOf("setActionRefEnabled(panel, true);");
		var rulesAt = body.indexOf("applyActionRefRules(panel, body.fields);");
		assertTrue(enableAt >= 0, "expand success path no longer enables ActionRefs through setActionRefEnabled");
		assertTrue(rulesAt > enableAt, "the rule pass must run after the lifecycle enable, never before it");
		// Disable-only (D2): no hide/show branch anywhere in the rule pass.
		var pass = body.substring(body.indexOf("function applyActionRefRules("));
		pass = pass.substring(0, pass.indexOf("\n\t}"));
		assertFalse(pass.contains("hidden"), pass);
		assertFalse(pass.contains("disabled = false"), pass);
	}

	@Test void c02_ruleEvaluation_isThreeWay_matchNoMatchAndAbsentField() {
		var r = report();
		assertEquals(true, r.get("hasApplyActionRefRules"));
		assertEquals(true, r.get("hasMintActionDescIdentity"));
		// Matching: untouched, and no reason left attached.
		assertEquals(true, r.get("rule_matchStaysEnabled"));
		assertEquals(true, r.get("rule_matchNoTitle"));
		assertEquals(true, r.get("rule_matchNoDescribedby"));
		assertEquals(true, r.get("rule_matchNoReasonText"));
		// Not matching: PRESENT and DISABLED - asserting both is what makes this test fail on a rule that never
		// evaluated, which a presence-only assertion would pass.
		assertEquals(true, r.get("rule_noMatchDisabled"));
		assertEquals(true, r.get("rule_noMatchStillPresent"));
		assertEquals("This alert is not open.", r.get("rule_noMatchTitle"));
		assertEquals("This alert is not open.", r.get("rule_noMatchReasonText"));
		assertEquals(true, r.get("rule_noMatchDescribedbyPointsAtNode"));
		// Field missing from the payload: fails closed, still present.
		assertEquals(true, r.get("rule_absentFieldDisabled"));
		assertEquals(true, r.get("rule_absentFieldStillPresent"));
		assertEquals("This alert is not open.", r.get("rule_absentFieldTitle"));
	}

	@Test void c03_operatorSemantics() {
		var r = report();
		assertEquals(true, r.get("rule_eqYes"));
		assertEquals(false, r.get("rule_eqNo"));
		assertEquals(false, r.get("rule_eqAbsentKey"));       // fail closed, not fail open
		assertEquals(true, r.get("rule_neYes"));
		assertEquals(false, r.get("rule_neNo"));
		assertEquals(true, r.get("rule_absentOnEmpty"));      // present/absent read emptiness, not key existence
		assertEquals(false, r.get("rule_absentOnValue"));
		assertEquals(true, r.get("rule_presentOnValueEnabled"));
		assertEquals(true, r.get("rule_presentOnEmptyDisabled"));
		assertEquals(true, r.get("rule_eqCoercesNumber"));
	}

	@Test void c04_firstDeclaredFailingRuleWins() {
		var r = report();
		// Two rules, both failing, DIFFERENT reasons, declared in both orders: the answer flips with the
		// declaration, which is the only way declaration order is observable at all.
		assertEquals("REASON-STATE", r.get("rule_firstDeclaredWins"));
		assertEquals("REASON-TIER", r.get("rule_firstDeclaredWinsReversed"));
		assertEquals("REASON-STATE", r.get("rule_firstFailingHelper"));
		assertNull(r.get("rule_firstFailingHelperAllPass"));
	}

	@Test void c05_bothReasonChannelsClearTogether() {
		var r = report();
		assertEquals(true, r.get("rule_clearPreconditionTitleSet"));
		assertEquals(true, r.get("rule_clearedTitle"));
		assertEquals(true, r.get("rule_clearedDescribedby"));
		assertEquals(true, r.get("rule_clearedReasonText"));
	}

	@Test void c06_rulePassNeverReEnablesAndNeverHides() {
		var r = report();
		assertEquals(true, r.get("rule_lifecycleDisabledStaysDisabled"));
		assertEquals(true, r.get("rule_hiddenStaysHidden"));
		assertEquals(true, r.get("rule_hiddenStaysDisabled"));
		assertEquals(true, r.get("rule_passNeverHides"));
	}

	@Test void c07_reasonNodeIdentityIsPerRowAndPerAction() {
		var r = report();
		assertEquals(true, r.get("rule_descIdsUnique"));
		assertEquals(true, r.get("rule_row1PointsAtOwnNode"));
		assertEquals(true, r.get("rule_row2PointsAtOwnNode"));
		assertEquals(true, r.get("rule_perActionIdsDiffer"));
		assertEquals(true, r.get("rule_bothActionsGated"));
		var id1 = String.valueOf(r.get("rule_descId1"));
		assertTrue(id1.contains("alerts") && id1.contains("a1") && id1.contains("ack"), id1);
	}

	@Test void c08_malformedOrEmptyRulesGateNothing() {
		var r = report();
		assertEquals(true, r.get("rule_malformedGatesNothing"));
		assertEquals(true, r.get("rule_emptyRulesGatesNothing"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) DetailField.actions - the third ActionBar host, and the one the runtime was never taught about
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The fill path is attribute-scoped, so a bar emitted as a sibling of the value slot is not a paint target and
	 * cannot be disturbed by a paint.  This is the half of the claim that would fail if the bar had been emitted
	 * inside the value slot instead: {@code textContent} assignment would destroy it on the first paint.
	 */
	@Test void d01_aFieldHostedBarIsInvisibleToTheFillPath() {
		var r = report();
		assertEquals(true, r.get("dfa_onePaintTargetInTheBlock"));
		assertEquals(true, r.get("dfa_barIsNotAPaintTarget"));
		assertEquals(true, r.get("dfa_barIsASiblingOfTheValueSlot"));
	}

	/**
	 * The three-state case a blank/non-blank default could not express: after a non-blank paint the field shows its
	 * value <b>and</b> its bar, and the shared lifecycle gate still enables the button.  Pinned in markup by
	 * {@code DetailField_Actions_Emit_Test.d01} and behaviourally here.
	 */
	@Test void d02_valueAndBarCoexist_andTheBarIsStillEnabledAfterANonBlankPaint() {
		var r = report();
		assertEquals("alice", r.get("dfa_valuePainted"));
		assertEquals(true, r.get("dfa_barSurvivedThePaint"));
		// Asserting the pre-gate state is what makes the enable observable rather than assumed.
		assertEquals(true, r.get("dfa_buttonStillDisabledBeforeTheGate"));
		assertEquals(true, r.get("dfa_buttonEnabledByTheSharedGate"));
	}

	/** No blank/non-blank default: an unfilled field keeps its bar, and the bar still enables. */
	@Test void d03_aBlankValueDoesNotHideTheBar() {
		var r = report();
		assertEquals(true, r.get("dfa_blankValueIsEmptyString"));
		assertEquals(true, r.get("dfa_blankValueKeepsTheBar"));
		assertEquals(true, r.get("dfa_blankValueButtonEnabled"));
	}

	/** Contract-failure fail-closed reaches the third host through the same panel-scoped pass as the other two. */
	@Test void d04_hideActionRefsReachesAFieldHostedBar() {
		var r = report();
		assertEquals(true, r.get("dfa_failClosedDisabled"));
		assertEquals(true, r.get("dfa_failClosedHidden"));
	}

	/**
	 * Visibility for a field-hosted bar is the state-conditional predicates and nothing else, which is only true if
	 * those predicates actually reach a bar inside a field block.  Both directions, so a pass that gated everything
	 * would fail as loudly as one that gated nothing.
	 */
	@Test void d05_stateConditionalRulesReachAFieldHostedBar() {
		var r = report();
		assertEquals(true, r.get("dfa_gatedDisabled"));
		assertEquals(true, r.get("dfa_gatedStillPresent"));
		assertEquals("This record is not open.", r.get("dfa_gatedReason"));
		assertEquals(true, r.get("dfa_passingRuleStaysEnabled"));
	}

	/**
	 * LD-1 ({@code TODO-J0474}): a field-hosted bar's action message paints into that field's OWN slot, not the
	 * section's first field. A second field is present precisely so "first field happens to be the bar's own
	 * field" cannot make this pass by accident.
	 */
	@Test void d06_fieldHostedActionMessage_paintsIntoItsOwnFieldSlot_notTheSectionsFirst() {
		var r = report();
		assertEquals("ok", r.get("paintFieldHosted_ownSlotPainted"));
		assertEquals(true, r.get("paintFieldHosted_firstFieldUntouched"));
	}

	/**
	 * LD-1 regression guard: a section-hosted bar (no enclosing {@code .juneau-view-detail-field}) keeps painting
	 * into the section's first field slot, byte-identically to today's behaviour. A second field is present so
	 * this could not pass merely because there was only one field to choose from.
	 */
	@Test void d07_sectionHostedActionMessage_stillPaintsIntoTheSectionsFirstField() {
		var r = report();
		assertEquals("section message", r.get("paintSectionHosted_firstFieldPainted"));
		assertEquals(true, r.get("paintSectionHosted_secondFieldUntouched"));
	}
}
