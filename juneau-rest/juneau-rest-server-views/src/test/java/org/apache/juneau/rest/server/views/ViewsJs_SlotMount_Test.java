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
		assertTrue(body.contains("data-juneau-region-declared"), body);
		assertTrue(body.contains("data-juneau-region-type"), body);
		var paintStart = body.indexOf("function paintSlotTable(");
		assertTrue(paintStart >= 0, body);
		var paintEnd = body.indexOf("function copyCsrfOntoTable(", paintStart);
		assertTrue(paintEnd > paintStart, body);
		var paintFn = body.substring(paintStart, paintEnd);
		assertFalse(paintFn.contains("initTable(table)"), "slot paint must not load a VIEW_META sidecar");
		assertFalse(paintFn.contains("data-juneau-region-meta"), paintFn);
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

	@Test void b06_blankTableUrlThrowsWithoutFetch() {
		var r = report();
		assertAllTrue(r, "t6_blankThrew", "t6_blankNamesUrl", "t6_blankNoFetch", "t6_blankLogged",
			"t6_blankNotStamped", "t7_wsThrew", "t7_wsNoFetch");
	}

	@Test void b07_missingIdOrBadShapeEnrolsNothing() {
		var r = report();
		assertAllTrue(r, "t8_missingIdThrew", "t8_probesNotStamped", "t9_badShapeThrew", "t9_probesNotStamped");
	}

	@Test void b08_csrfCopiedFromAncestorNotFromSsc() {
		var r = report();
		assertAllTrue(r, "t10_csrfCopied", "t10_csrfHeaderCopied", "t11_noJuneauToken",
			"t11_didNotCopySsc", "t11_missingToken");
	}

	@Test void b09_detailTemplateUsesExistingExpanderDomAndDeclaredDataUrl() {
		var r = report();
		assertAllTrue(r, "t12_hasTemplate", "t12_hasHeader", "t12_regionType",
			"t12_declaredDataUrlOnly", "t12_noRegionMeta", "t12_regionContract");
	}

	@Test void b10_nestedTableHasContractTwoAndNoHtmlId() {
		var r = report();
		assertAllTrue(r, "t13_nestedContract", "t13_nestedNoHtmlId", "t13_scopeParam");
	}

	@Test void b11_bulkMismatchWithholdsBulkOnly() {
		var r = report();
		assertAllTrue(r, "t14_hasTable", "t14_hasSelect", "t14_noBulk", "t14_logged");
	}

	@Test void b12_quickStatsPainterAndUnknownTypeFailsLoud() {
		var r = report();
		assertAllTrue(r, "t15_tile", "t15_bar", "t15_segments", "t15_contract",
			"t16_unknownBanner", "t16_unknownLogged", "t16_noTable");
	}
}
