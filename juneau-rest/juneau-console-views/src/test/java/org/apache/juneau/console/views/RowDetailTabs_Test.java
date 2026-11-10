// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** Tabbed row details in {@code juneau-views.js}: template branch, inline and bean expand, header actions, sidecar. */
class RowDetailTabs_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.reportWithHelpers("row-detail-tabs.cjs");
		assumeTrue(r != null, "node not available or row-detail-tabs.cjs not found - behavioral layer skipped");
		return r;
	}

	@Test void a01_untabbed_templateUnchanged() {
		assertEquals(true, report().get("untabbed_noShell"));
	}

	@Test void a02_inline_noFetch_tabsPaintedFromRowData() {
		var r = report();
		assertEquals(true, r.get("inline_ok"));
		assertEquals(true, r.get("inline_noFetch"));
		assertEquals("Alpha|Beta", r.get("inline_tabLabels"));
		assertEquals("name=Rex|hidden=secret", r.get("inline_values"));
		assertEquals("G", r.get("inline_groupTitle"));
		assertEquals("ok", r.get("inline_state"));
	}

	@Test void a03_fieldsTab_labelsFromColumnThenHumanised() {
		assertEquals("Name|Hidden", report().get("inline_labelFromColumn"));
	}

	@Test void a04_allEmptyFieldsTab_saysSo() {
		assertEquals("No details available.", report().get("empty_message"));
	}

	@Test void a05_bean_oneFetch_countsInLabels() {
		var r = report();
		assertEquals("/rd/pet/s-7", r.get("bean_fetches"));
		assertEquals("Alpha|Beta (3)", r.get("bean_labels"));
	}

	@Test void a06_bean_missingEndpoint_failsClosed() {
		assertEquals("error", report().get("bean_noUrl_state"));
	}

	@Test void a07_unknownKind_errorPane_stripSurvives() {
		var r = report();
		assertEquals(true, r.get("unknownKind_strip"));
		assertEquals(true, r.get("unknownKind_message"));
	}

	@Test void a08_tabsNotAnArray_headerOnly() {
		var r = report();
		assertEquals(true, r.get("badTabs_noShell"));
		assertEquals(true, r.get("badTabs_hasHeader"));
		assertEquals(1.0, ((Number)r.get("badTabs_errors")).doubleValue());
	}

	@Test void a09_headerActions_linkSubstitutedAndUnsafeRefused() {
		var r = report();
		assertEquals(1L, ((Number)r.get("actions_linkCount")).longValue());
		assertEquals("/pets/s-7/a%20b%3Cc", r.get("actions_linkHref"));
		assertEquals(1L, ((Number)r.get("actions_buttonCount")).longValue());
	}

	@Test void a10_scriptPayloads_areInertText() {
		var r = report();
		assertEquals(true, r.get("xss_noScriptEl"));
		assertEquals(true, r.get("xss_valueIsText"));
	}

	@Test void a11_nestedSidecar_taggedForFindNestedSidecar() {
		var r = report();
		assertEquals(1L, ((Number)r.get("sidecar_count")).longValue());
		assertEquals("orders", r.get("sidecar_id"));
		assertEquals("/o", r.get("sidecar_json"));
		assertEquals("application/json", r.get("sidecar_type"));
	}

	@Test void b01_lazy_firstActivationFetchesOnlyThatTab() {
		var r = report();
		assertEquals(true, r.get("lazy_noTabFetchBeforeActivation"));
		assertEquals(true, r.get("lazy_firstActivationFetches"));
	}

	@Test void b02_perRowCache_noRefetchOnReactivation() {
		assertEquals(true, report().get("cache_perRowNoRefetch"));
	}

	@Test void b03_cacheNone_refetchesEachActivation() {
		assertEquals(true, report().get("cache_noneRefetches"));
	}

	@Test void b04_rememberedTab_recordedOnContext() {
		assertEquals(true, report().get("remembered_tab"));
	}

	@Test void b05_skippedTick_idlesNonActiveRefreshOnPollTabs() {
		var r = report();
		assertEquals(true, r.get("poll_idleClearsNonActive"));
		assertEquals(true, r.get("poll_idleRefetchesOnActivation"));
	}

	@Test void b06_invalidate_api() {
		var r = report();
		assertEquals(true, r.get("invalidate_present"));
		assertEquals(true, r.get("invalidate_activeRepopulated"));
	}

	@Test void b07_collapse_abortsInflightTabFetches() {
		var r = report();
		assertEquals(true, r.get("abort_inflightBefore"));
		assertEquals(true, r.get("abort_onCollapse"));
	}

	@Test void b08_redraw_reopensByRowIdWithActiveTab() {
		var r = report();
		assertEquals(true, r.get("reopen_expanded"));
		assertEquals(true, r.get("reopen_activeTab"));
		assertEquals(true, r.get("reopen_gone"));
		assertEquals("reattach:s-7:true", r.get("reopen_model"));
	}

	@Test void b09_tabError_retryRebuildsOnlyThatTabAndRestoresFocus() {
		var r = report();
		assertEquals(true, r.get("error_retryShown"));
		assertEquals(true, r.get("error_retryRepaints"));
		assertEquals(true, r.get("error_retryOnlyThatTab"));
		assertEquals(true, r.get("error_retryFocus"));
	}
}
