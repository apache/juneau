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
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Always-on round-trip for the page-cards sidecar scanner (F2 / F10).  The real {@code juneau-page-cards.js}
 * runtime is booted over the real {@code JuneauViews.regions.mount} pipeline (all under a DOM shim) and must:
 *
 * <ul>
 *   <li>paint a table from the SLOT_META datatables envelope {@code CardEnvelope.liftTable} emits &mdash; the
 *       exact JSON the Java golden in {@code CardDirective_Test} asserts;</li>
 *   <li>boot a {@code {contractVersion,id,populate}} sidecar into a populate hookup so the named populator
 *       actually runs and paints its slot;</li>
 *   <li>resolve a name-only {@code {contractVersion,id,template}} sidecar against {@code JuneauPage.templates}
 *       and set the slot's innerHTML from the looked-up template (no {@code regions.mount} for this one).</li>
 * </ul>
 *
 * <p>
 * The behavioral half runs the real runtime source under a DOM shim (see
 * {@code src/test/js/page-cards-mount.cjs}); it {@code assumeTrue}s (skips) when Node is unavailable,
 * like the other Node drivers.  {@code reportWithPageCards} loads {@code juneau-page-cards.js} last (the
 * order the "views" toolkit pack emits it) so the scanner can be booted directly.
 */
class PageCards_Mount_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.reportWithPageCards("page-cards-mount.cjs");
		assumeTrue(r != null, "node not available or page-cards-mount.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	@Test void liftedSlotMetaPaintsTable() {
		var r = report();
		assertAllTrue(r, "t_emptyBefore", "t_hasTable", "t_thead", "t_noError", "t_noConsoleErrors");
	}

	@Test void populateSidecarBootsIntoMount() {
		var r = report();
		assertAllTrue(r, "tp_bootIsFunction", "tp_populatorRanOnce", "tp_painted", "tp_noConsoleErrors");
	}

	@Test void nameOnlyTemplateSidecarSetsInnerHtml() {
		var r = report();
		assertAllTrue(r, "tt_innerHtmlFromTemplate", "tt_noConsoleErrors");
	}

	/**
	 * The {@code pill} catalog renderer (Task 15): its display facet emits {@code .jc-pill}/{@code .jc-pill-dot}
	 * chip markup with NO inline hex (the chip's colour comes from the {@code --jc-pill-*} palette classes, resolved
	 * by console-ui {@code chrome.css}, not by this renderer), the render id {@code pill} resolves through
	 * {@code parseRenderId}/{@code resolveRenderer} with no unknown-id fallback, and a datatables SLOT_META carrying
	 * a {@code render:'pill'} column mounts cleanly.
	 */
	@Test void pillRendererEmitsChipMarkupAndResolvesAndMounts() {
		var r = report();
		assertAllTrue(r,
			"pill_isRegistered", "pill_hasJcPillClass", "pill_hasDot", "pill_hasTone", "pill_hasValue",
			"pill_noInlineHex", "pill_noSlds",
			"pill_parseRenderId", "pill_resolves",
			"pill_mountHasTable", "pill_mountThead", "pill_mountNoError", "pill_mountNoConsoleErrors");
	}
}
