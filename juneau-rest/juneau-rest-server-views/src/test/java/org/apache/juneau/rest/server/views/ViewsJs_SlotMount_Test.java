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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for table-in-slot mount: {@code { table: url | envelope }} values, CSRF ancestor copy,
 * DETAIL_SLOT template chrome, nested tables, bulk handshake withhold, and QuickStats paint.
 *
 * <p>
 * The behavioral half runs the real runtime source under a DOM shim (see {@code src/test/js/slot-mount.cjs}).
 */
class ViewsJs_SlotMount_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.report("slot-mount.cjs");
		assumeTrue(r != null, "node not available or slot-mount.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_sourceShape_slotInitIsNotSidecarPath() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("JUNEAU_SLOT_CONTRACT_VERSION = \"1\""), body);
		assertTrue(body.contains("NS.SLOT_CONTRACT_VERSION = JUNEAU_SLOT_CONTRACT_VERSION"), body);
		assertTrue(body.contains("initTableFromDef: initTableFromDef"), body);
		assertTrue(body.contains("mountTableSlot: mountTableSlot"), body);
		assertTrue(body.contains("return Promise.resolve(initTableFromDef(table, viewDef, extras))"), body);
		assertTrue(body.contains("slot.closest(\"[data-juneau-csrf]\")"), body);
		assertFalse(body.contains("data-ssc-csrf"), "slot CSRF copy must not read data-ssc-csrf-*");
		assertTrue(body.contains("dataset.juneauRegionDeclared"), body);
		assertTrue(body.contains("dataset.juneauRegionType"), body);
		var paintStart = body.indexOf("function paintSlotTable(");
		assertTrue(paintStart >= 0, body);
		var paintEnd = body.indexOf("function copyCsrfOntoTable(", paintStart);
		assertTrue(paintEnd > paintStart, body);
		var paintFn = body.substring(paintStart, paintEnd);
		assertFalse(paintFn.contains("initTable(table)"), "slot paint must not load a VIEW_META sidecar");
		assertFalse(paintFn.contains("data-juneau-region-meta"), paintFn);
		var buildStart = body.indexOf("function buildDetailTemplate(");
		assertTrue(buildStart >= 0, body);
		var buildEnd = body.indexOf("\n\tfunction buildDetailHeader(", buildStart);
		assertTrue(buildEnd > buildStart, body);
		var buildFn = body.substring(buildStart, buildEnd);
		assertTrue(buildFn.contains("const dest = tpl.content"), buildFn);
		assertTrue(buildFn.contains("dest.appendChild"), buildFn);
		assertFalse(buildFn.contains("tpl.appendChild"),
			() -> "detail chrome must go into tpl.content, the fragment expand clones: " + buildFn);
	}

	@Test void b01_inlineEnvelopePaintsTableWithoutRegionStamp() {
		var r = report();
		assertAllTrue(r, "t1_emptyBefore", "t1_hasTable", "t1_thead", "t1_noRegionOnSlot",
			"t1_wrapperMarker", "t1_layoutWide", "t1_noErrors", "t1_initFromDefExported");
	}

	@Test void b02_fetchFailureBannersThatSlotAndKeepsSiblingRegion() {
		var r = report();
		assertAllTrue(r, "t2_fetchedUrl", "t2_bannerInSlot", "t2_noTable", "t2_logged",
			"t2_regionStayed", "t2_handleCount");
	}

	@Test void b03_malformedJsonBanners() {
		var r = report();
		assertAllTrue(r, "t3_malformedBanner", "t3_malformedLogged");
	}

	@Test void b04_slotContractMismatchBanners() {
		var r = report();
		assertAllTrue(r, "t4_versionBanner", "t4_versionLogged", "t4_noTable");
	}

	@Test void b05_urlFetchPaintsTable() {
		var r = report();
		assertAllTrue(r, "t5_urlUsed", "t5_tableFromUrl");
	}

	@Test void b08_csrfCopiedFromAncestorNotFromSsc() {
		var r = report();
		assertAllTrue(r, "t10_csrfCopied", "t10_csrfHeaderCopied", "t11_noJuneauToken",
			"t11_didNotCopySsc", "t11_missingToken");
	}

	@Test void b09_detailTemplateUsesExistingExpanderDomAndDeclaredDataUrl() {
		var r = report();
		assertAllTrue(r, "t12_hasTemplate", "t12_lightDomEmpty", "t12_contentHasChildren",
			"t12_hasHeader", "t12_regionType", "t12_declaredDataUrlOnly", "t12_noRegionMeta",
			"t12_regionContract");
	}

	@Test void b10_leftoverSectionsDoNotReconstructNestedTable() {
		var r = report();
		assertAllTrue(r, "t13_noNested", "t13_hasRegion", "t13_noSectionFrame");
	}

	@Test void b11_bulkMismatchWithholdsBulkOnly() {
		var r = report();
		assertAllTrue(r, "t14_hasTable", "t14_hasSelect", "t14_noBulk", "t14_logged");
	}

	@Test void b12_quickStatsPainterAndUnknownTypeFailsLoud() {
		var r = report();
		assertAllTrue(r, "t15_tile", "t15_bar", "t15_segments", "t15_contract",
			"t16_unknownBanner", "t16_unknownLogged", "t16_noTable");
		// status-tone token paints is-<tone>; an off-palette tone paints no modifier
		assertBean(r, "t15_toneWarningClass,t15_toneAccentClass", "jc-stat-value is-warning,jc-stat-value");
	}

	@Test void b13_rowsOnlyTableUsesInlineDataWithEscapingRenderAndNoFetch() {
		var r = report();
		assertAllTrue(r, "t17_hasTable", "t17_noFetch", "t17_rowsStashed", "t17_rowIdField", "t17_noDomRows",
			"t17_noAjax", "t17_clientSide", "t17_dataIsRows", "t17_hasRender", "t17_escapes", "t17_rawForSort",
			"t17_nullBlank", "t17_noErrors", "t17_dataUrlKeepsAjax");
	}

	@Test void b14_crossOriginDetailEndpointIsRefusedWithWarning() {
		var r = report();
		assertAllTrue(r, "t18_relativeKept", "t18_sameOriginAbsKept", "t18_noWarnYet", "t18_crossOriginNotStamped",
			"t18_warned");
	}

	@Test void b14b_headerOnlyDetailTitleAllowlistsNamedKeysOnly() {
		var r = report();
		assertAllTrue(r, "tf_headerOnlyRendersId", "tf_headerOnlyAllowlist", "tf_headerOnlyNamedKeysOnly",
			"tf_unnamedFieldNotLeaked", "tf_unlistedPlaceholderInDataNotAllowlisted", "tf_regionTitleFieldsStillWork",
			"tf_regionTitleRenders", "tf_noPlaceholders");
	}

	@Test void b15_documentScanLeavesASlotBuiltTableAlone() {
		var r = report();
		assertAllTrue(r, "t19_tableMounted", "t19_scanLogsNoMissingSidecar");
	}

	@Test void b16_cardApiResolvesSlotBuiltTable() {
		var r = report();
		assertAllTrue(r, "t20_noThrow", "t20_selection", "t20_reloadRedraws");
	}

	@Test void b17_quickStatPainters_useCssomNotStyleAttribute() throws Exception {
		var body = viewsJs();
		var start = body.indexOf("function paintStatBar(");
		var end = body.indexOf("function buildDetailTemplate(", start);
		assertTrue(start >= 0 && end > start, body);
		var fn = body.substring(start, end);
		assertTrue(fn.contains("fill.style.width = pct + \"%\""), fn);
		assertTrue(fn.contains("fill.style.flexGrow = String(count)"), fn);
		assertFalse(fn.contains("setAttribute(\"style\""), "painters must size through CSSOM, not a style attribute");
		assertFalse(fn.contains("cssText"), fn);
		assertFalse(fn.contains("inner" + "HTML"), "painters must not use a raw HTML-sink assignment");
	}

	@Test void b18_quickStatKindsPaintDistinctMarkup() {
		var r = report();
		assertAllTrue(r, "t21_tile", "t21_nullTile", "t21_meterRole", "t21_segRole", "t21_impliedMeter",
			"t21_impliedSeg", "t21_noErrors");
	}

	@Test void b19_malformedQuickStatItemsFailClosed() {
		var r = report();
		assertAllTrue(r, "t22_tileWithMax", "t22_meterNoMax", "t22_segNoSegments", "t22_meterWithSegments",
			"t22_segmentedWithMax", "t22_tileWithSegments", "t22_both", "t22_unknownType",
			"t22_zeroMax", "t22_negMax", "t22_stringValue", "t22_negValue", "t22_emptySegments",
			"t22_meterNoLabel", "t22_meterBlankLabel", "t22_meterNullLabel", "t22_segNoLabel", "t22_segBlankLabel", "t22_segNullLabel");
	}

	@Test void b20_meterGeometryAndAria() {
		var r = report();
		assertAllTrue(r, "t23_width", "t23_ariaNow", "t23_valuetextWarning", "t23_fillTone", "t23_zeroNoMin",
			"t23_tinyMin", "t23_over", "t23_notOver", "t23_errTrack", "t23_text", "t23_blankText",
			"t23_offPaletteTone", "t23_hiddenParts", "t23_valueHidden", "t23_overErrorOrder");
		assertAllTrue(r, "t23b_noThrow", "t23b_painted", "t23b_textOnly", "t23b_segmentTextOnly");
	}

	@Test void b21_segmentedBarLegendAndPalette() {
		var r = report();
		assertAllTrue(r, "t24_fillsSkipZero", "t24_legendHasZero", "t24_flexGrow", "t24_colours", "t24_fillColours",
			"t24_swatchHidden", "t24_barHidden", "t24_labelHidden", "t24_group", "t24_paletteCapsAtEight");
	}

	@Test void b22_srcEnvelopeQuickStatsAreCheckedOnTheClient() {
		var r = report();
		assertAllTrue(r, "t25_segmentedMounts", "t25_malformedFailsClosed");
	}
}
