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
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/**
 * Node-sandbox coverage for the shell side of the client object model in {@code juneau-console.js}: the
 * {@code modelVersion} check, the {@code Card} base and class-form {@code registerCard}, and the page
 * {@code scripts} loader (harness {@code model-shell.cjs}).
 *
 * <p>
 * Gated on {@code node} being on {@code PATH} (skipped otherwise).
 */
class ConsoleModelShell_Test extends TestBase {

	private static Map<String,Object> report;

	@BeforeAll
	static void run() {
		report = BusHarness.run("model-shell.cjs", ViewsMixin.BUS_JS_RESOURCE, "/org/apache/juneau/console/juneau-console.js");
	}

	private static Object r(String key) {
		assumeTrue(report != null, "node (or the harness) is not available");
		assertTrue(report.containsKey(key), "missing report key: " + key);
		return report.get(key);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// modelVersion
	//-----------------------------------------------------------------------------------------------------------------

	@Test void modelVersion_missingWhenNeeded_isFatal() { assertEquals("E-JS-120", r("modelVersionMissing")); }
	@Test void modelVersion_missingReadsMissing() { assertTrue(String.valueOf(r("modelVersionMissingBanner")).contains("'(missing)'")); }
	@Test void modelVersion_missingInNestedCard_isFatal() { assertEquals("E-JS-120", r("modelVersionMissingNested")); }
	@Test void modelVersion_missingWithHandlerSubscription_isFatal() { assertEquals("E-JS-120", r("modelVersionMissingSubscribes")); }
	@Test void modelVersion_unsupported_isFatal() { assertEquals("E-JS-120", r("modelVersionBad")); }
	@Test void modelVersion_supported_mounts() { assertNull(r("modelVersionOk")); }
	@Test void noModel_noVersionNeeded() { assertNull(r("noModelNoVersion")); }
	@Test void noModel_neverReadsJuneauViews() { var m = (Map<?,?>)r("noModelNeverReadsViews"); assertEquals(0, ((Number)m.get("reads")).intValue()); assertNull(m.get("threw")); }
	@Test void modelVersion_tableRibbonModel_isFatal() { assertEquals("E-JS-120", r("modelVersionTableRibbon")); }

	//-----------------------------------------------------------------------------------------------------------------
	// Card base
	//-----------------------------------------------------------------------------------------------------------------

	@Test void card_rendersWithoutNewError() {
		var m = (Map<?,?>)r("class_card_renders_without_new_error");
		assertEquals(true, m.get("ok"));
		assertEquals(List.of(), m.get("errors"));
	}
	@Test void card_elSpecSetBeforeRender() {
		var m = (Map<?,?>)r("this_el_spec_set_before_render");
		assertEquals(true, m.get("el"));
		assertEquals("p1", m.get("specId"));
		assertEquals(true, m.get("frozen"));
		assertEquals(true, m.get("hasSignal"));
	}
	@Test void card_document() { assertEquals(true, r("member_document")); }
	@Test void card_template() { assertEquals(true, ((Map<?,?>)r("member_template")).get("got")); }
	@Test void card_fail() { assertEquals("x", r("member_fail")); }
	@Test void card_onReady() { assertEquals(Map.of("before", 0, "after", 1), r("member_onReady")); }
	@Test void card_fetchJsonAbortsOnDestroy() { assertEquals(Map.of("isAbort", true, "aborted", true), r("member_fetchJson_aborts_on_destroy")); }
	@Test void card_isAbort() { assertEquals(List.of(true, false), r("member_isAbort")); }
	@Test void card_paintLoadingAndError() { assertEquals(Map.of("loading", true, "e1", "boom", "e2", "two"), r("member_paintLoading_paintError")); }
	@Test void card_everyClearedOnDestroy() { assertEquals(Map.of("registered", 1, "afterDestroy", 0), r("member_every_cleared_on_destroy")); }
	@Test void card_prefs() { assertEquals(Map.of("a", 1), r("member_prefs")); }
	@Test void card_onDestroy() { assertEquals(1, r("member_onDestroy")); }
	@Test void card_announce() { assertEquals("hello", r("member_announce")); }
	@Test void card_signal() { assertEquals(Map.of("before", false, "after", true), r("member_signal")); }
	@Test void card_publishSubscribe() { assertEquals("function", ((Map<?,?>)r("member_publish_subscribe_returns_off")).get("offType")); }
	@Test void card_refreshDefault() { assertEquals(2, r("member_refresh")); }

	//-----------------------------------------------------------------------------------------------------------------
	// registerCard and class contract
	//-----------------------------------------------------------------------------------------------------------------

	@Test void register_objectForm_e21() { assertEquals("E-JS-21", r("register_objectForm_E21")); }
	@Test void register_objectForm_message() { assertTrue(String.valueOf(r("register_objectForm_message")).contains("object form was removed")); }
	@Test void register_functionForm_e21() { assertEquals("E-JS-21", r("register_functionForm_E21")); }
	@Test void register_string_e21() { assertEquals("E-JS-21", r("register_string_E21")); }
	@Test void register_missingRoleMethod_e21() { assertEquals("E-JS-21", ((Map<?,?>)r("register_missingRoleMethod_E21")).get("code")); }
	@Test void register_missingOpMethod_e21() { assertEquals("E-JS-21", ((Map<?,?>)r("register_missingOpMethod_E21")).get("code")); }
	@Test void register_classOk() { assertEquals(true, r("register_class_ok")); }
	@Test void register_duplicate_e12() { assertEquals("E-JS-12", r("register_duplicate_E12")); }
	@Test void queue_acceptsClass() { assertEquals(true, r("queue_accepts_class")); }
	@Test void queue_rejectsObject() { assertEquals("E-JS-21", r("queue_rejects_object")); }
	@Test void legacy_builtinObjectForm_nowE21() { assertEquals("[\"E-JS-21\",\"E-JS-21\",\"E-JS-21\",\"E-JS-21\"]", String.valueOf(r("legacy_builtin_object_form_now_E21"))); }
	@Test void noSetApiOnCtx() { assertEquals("undefined", r("no_setApi_on_ctx")); }
	@Test void cardApi_returnsInstanceForAllBuiltins() { assertEquals("[true,true,true,true]", String.valueOf(r("cardApi_returns_instance_for_all_builtins"))); }
	@Test void role_dispatchPayloadMeta() {
		var m = (Map<?,?>)r("role_dispatch_payload_meta");
		assertEquals(List.of(), m.get("banner"));
		assertEquals(List.of(Map.of("payload", Map.of("hot", 7), "hasMeta", true)), m.get("got"));
	}
	@Test void shellRoles_refreshParamsUnchanged() {
		var m = (Map<?,?>)r("shellRoles_refresh_params_unchanged");
		assertEquals(List.of("refresh", "render:/api/a"), m.get("log"));
		assertEquals(List.of(), m.get("banner"));
	}
	@Test void cmd_opReceivesFullCmd() { assertEquals(Map.of("op", "do-it", "ids", List.of(1), "resetPaging", true, "options", Map.of("a", 1), "target", "g"), r("cmd_op_receives_full_cmd")); }
	@Test void cmd_refreshForwards() { assertEquals(List.of("render", "refresh"), r("cmd_refresh_forwards")); }
	@Test void cmd_unknownOp_e48() { assertEquals(List.of("E-JS-48"), r("cmd_unknown_op_E48")); }
	@Test void cardApi_returnsInstance() { assertEquals(true, r("cardApi_returns_instance")); }
	@Test void cardApi_unknown_e22() { assertEquals("E-JS-22", r("cardApi_unknown_E22")); }
	@Test void cardApi_destroyed_e22() { assertEquals("E-JS-22", r("cardApi_destroyed_E22")); }
	@Test void cardTypeTable_readsStaticMaps() {
		var m = (Map<?,?>)r("cardTypeTable_reads_static_maps");
		assertEquals(List.of("alpha", "beta-x"), m.get("roles"));
		assertEquals(List.of("op-one"), m.get("ops"));
	}
	@Test void implicitTopics_functionForm() {
		var m = (Map<?,?>)r("implicitTopics_function_form");
		assertEquals(true, m.get("isFn"));
		assertEquals(List.of("selection:a", "plain:a"), m.get("a"));
		assertEquals(List.of("selection:b", "detail:b"), m.get("b"));
	}
	@Test void implicitTopics_claimed() { assertEquals("a", r("implicitTopics_claimed")); }
	@Test void builtin_htmlRenders() {
		var m = (Map<?,?>)r("builtin_html_renders");
		assertEquals(List.of(), m.get("errors"));
		assertEquals(true, m.get("instance"));
	}
	@Test void builtin_terminalViewRenders() {
		var m = (Map<?,?>)r("builtin_terminalView_renders");
		assertEquals(List.of("tv-body"), m.get("calls"));
		assertEquals(List.of(), m.get("errors"));
	}
	@Test void lifecycle_order() { assertEquals(List.of("render", "refresh", "destroy"), r("lifecycle_render_refresh_destroy_order")); }
	@Test void refresh_defaultRerenders() { assertEquals(List.of("render", "destroy", "render"), ((Map<?,?>)r("refresh_default_rerenders")).get("log")); }
	@Test void destroy_throwPaintedE8() { assertEquals(List.of("E-JS-8"), ((Map<?,?>)r("destroy_throw_painted_E8")).get("banner")); }

	//-----------------------------------------------------------------------------------------------------------------
	// scripts loader
	//-----------------------------------------------------------------------------------------------------------------

	@Test void scripts_loadInOrder() {
		var m = (Map<?,?>)r("scripts_load_in_order");
		assertEquals(List.of("/a.js"), m.get("afterReady"));
		assertEquals(List.of("/a.js", "/b.js"), m.get("afterFirst"));
		assertEquals(List.of(), m.get("banner"));
	}
	@Test void scripts_dedupeExistingTag() { assertEquals(List.of("/a.js", "/b.js"), r("scripts_dedupe_existing_tag")); }
	@Test void scripts_dedupeResolvedUrl() { assertEquals(List.of("http://juneau.test/a.js"), r("scripts_dedupe_resolved_url")); }
	@Test void scripts_crossOrigin_e124() { assertEquals(Map.of("codes", List.of("E-JS-124"), "appended", 0), r("scripts_crossOrigin_E124")); }
	@Test void scripts_backslash_e124() { assertEquals(Map.of("codes", List.of("E-JS-124"), "appended", 0), r("scripts_backslash_E124")); }
	@Test void scripts_baseHrefCrossOrigin_e124() { assertEquals(Map.of("codes", List.of("E-JS-124"), "appended", 0), r("scripts_baseHref_crossOrigin_E124")); }
	@Test void scripts_malformedExistingSrc_notAMatch() { assertEquals(Map.of("codes", List.of(), "srcs", List.of("http://[bad", "/a.js")), r("scripts_malformedExistingSrc_notAMatch")); }
	@Test void scripts_loadError_e124() { assertEquals(List.of("E-JS-124"), r("scripts_loadError_E124")); }
	@Test void scripts_nonceCopied() { assertEquals("abc123", r("scripts_nonce_copied")); }
	@Test void scripts_noneNoop() { assertEquals(0, ((Map<?,?>)r("scripts_none_noop")).get("scripts")); }
	@Test void scripts_noneSynchronousBusPhase() { assertEquals(true, r("scripts_none_synchronous_bus_phase")); }
	@Test void scripts_beforeWireCards() {
		var m = (Map<?,?>)r("scripts_before_wireCards");
		assertEquals("late ok", m.get("text"));
		assertEquals(List.of(), m.get("banner"));
		assertEquals(true, m.get("wired"));
	}
	@Test void sweep_unknownTypeWithoutScripts() { assertEquals(List.of("E-JS-4", "E-JS-10"), r("sweep_still_fails_unknown_type_without_scripts")); }
	@Test void sweep_unknownTypeAfterScripts() { assertEquals(List.of("E-JS-4"), r("sweep_fails_unknown_type_after_scripts")); }
	@Test void scripts_failureStillRunsSweepAndBus() { assertEquals(Map.of("banner", List.of("E-JS-124", "E-JS-4"), "wired", true), r("scripts_failure_still_runs_sweep_and_bus")); }
	@Test void modelNeverLoaded_isPainted() { assertEquals(List.of("E-JS-125"), r("model_never_loaded_is_painted")); }
	@Test void scriptsOnlyWithoutModel_isQuiet() { assertEquals(List.of(), r("scripts_only_without_model_is_quiet")); }
	@Test void navModelKey_isNotAModelBlock() { assertNull(r("nav_model_key_is_not_a_model_block")); }
	@Test void mount_withoutModelJs() {
		var m = (Map<?,?>)r("mount_without_model_js_loaded_still_works");
		assertEquals(true, m.get("sameType"));
		assertEquals("ok", m.get("text"));
		assertEquals(true, m.get("noModel"));
		assertEquals(List.of(), m.get("errors"));
	}
}
