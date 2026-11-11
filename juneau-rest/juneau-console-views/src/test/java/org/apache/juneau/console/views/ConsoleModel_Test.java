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
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Node-sandbox coverage for {@code juneau-model.js}, the client object model (handles, path ids, registry,
 * lifecycle, kind classes, triggers and the handler registry), through the harness {@code model.cjs}.  The harness
 * reports each check as {@code true} or a failure string.
 *
 * <p>
 * Gated on {@code node} being on {@code PATH} (skipped otherwise).
 */
class ConsoleModel_Test extends TestBase {

	private static final String MODEL_JS = "/org/apache/juneau/views/juneau-model.js";
	private static final String INLINE_JS = "/org/apache/juneau/views/juneau-model-inline.js";
	private static final String CONSOLE_JS = "/org/apache/juneau/console/juneau-console.js";

	private static Map<String,Object> report;

	@BeforeAll
	static void run() {
		report = BusHarness.run("model.cjs", ViewsMixin.BUS_JS_RESOURCE, CONSOLE_JS, ViewsMixin.RIBBON_JS_RESOURCE, MODEL_JS, INLINE_JS);
	}

	private static final String[] KEYS = {
		// Core
		"path_segment_encoding", "path_page_card_table_row_detail_tab", "path_positional_row",
		"get_returns_same_object", "get_unknown_null", "get_never_creates",
		"detach_keyed_row_reopens_same_object", "detach_methods_noop_warn_once", "reattach_clears_warned_flag",
		"positional_rows_destroyed_on_draw", "collapse_destroys_detail_and_tabs_not_row", "missing_keyed_row_evicted_after_draw",
		"destroy_disposes_bus_owner", "registry_size_stable_after_polls", "page_unmount_destroys_subtree",
		"parent_children", "children_is_copy", "closest_self_and_ancestor", "find_relative_path", "find_kind_id_depth_first", "page_accessor",
		"spec_deep_frozen", "missing_member_is_typeerror", "helpers_is_frozen_empty_object",
		// Handler registry and built-ins
		"register_ok_get_names_sorted", "register_badName_E114_fatal", "register_badName_with_shell_is_JuneauConsoleError",
		"register_duplicate_E115", "register_nonFunction_E122", "register_juneauNs_E123", "get_unknown_undefined",
		"handlers_namespace_frozen", "builtins_listed", "builtin_refresh_calls_target_refresh", "builtin_refreshClosest_kind_payload",
		"builtin_expand_collapse_row", "builtin_activate", "builtin_publish_topic_payload",
		"countLabel_uses_payload_count_over_rowCount", "countLabel_undefined_noop", "countLabel_writes_serverLabel_plus_count",
		"countBadge_info_tone",
		"inline_compiles_body_to_fn_target_event", "inline_compile_error_E119", "inline_csp_evalerror_E119_mentions_unsafe_eval", "inline_opt_in_required",
		// Kind classes
		"Table_has_four_legacy_members", "Table_methods_delegate_to_impl", "Table_rows_rowCount_row_lookup", "Table_refresh_is_reload",
		"Row_positional_flag", "Row_expand_collapse_events", "RowDetail_tab_lookup", "Ribbon_item_lookup_by_id",
		"RibbonItem_requires_id_E121", "Switcher_Chart_skeletons", "absent_member_typeerror", "Card_collisions_resolved",
		"Card_shell_assigns_el_and_spec", "Card_registers_and_renders_via_shell", "HtmlCard_instanceof_model_Card",
		"DetailTab_setLabel_updates_strip_button", "DetailTab_setBadge_chip_and_tones", "transient_setLabel_restored_on_rerender",
		"PageTab_label_badge_activate", "RibbonItem_label_enable_disable_activate", "Card_title_label_and_badge",
		"kind_Page_exists_and_instanceof_Handle", "kind_PageTab_exists_and_instanceof_Handle", "kind_Card_exists_and_instanceof_Handle",
		"kind_Table_exists_and_instanceof_Handle", "kind_Row_exists_and_instanceof_Handle", "kind_RowDetail_exists_and_instanceof_Handle",
		"kind_DetailTab_exists_and_instanceof_Handle", "kind_Ribbon_exists_and_instanceof_Handle", "kind_RibbonItem_exists_and_instanceof_Handle",
		"kind_Switcher_exists_and_instanceof_Handle", "kind_SwitcherView_exists_and_instanceof_Handle", "kind_Chart_exists_and_instanceof_Handle",
		// Triggers and handler resolution
		"on_returns_off",
		"on_string_typeerror",
		"bind_function_typeerror",
		"subscribe_returns_off",
		"subscribe_string_typeerror",
		"bind_returns_this",
		"listen_returns_this",
		"action_returns_this",
		"action_duplicate_id_E121",
		"action_click_payload",
		"event_frozen_except_stopPropagation",
		"event_target_is_declaring_handle",
		"event_source_is_emitter",
		"event_target_for_row_action_is_row_not_card",
		"action_handler_target_is_declaring_handle_event_source_is_clicked_row",
		"emit_bubbles_to_page",
		"emit_app_listeners_before_model_on",
		"emit_stopPropagation",
		"emit_custom_name",
		"bind_undocumented_event_E121",
		"listen_template_expansion",
		"listen_subscription_disposed_on_detach",
		"listen_resubscribed_on_reattach_once",
		"app_subscribe_suspended_on_detach_not_doubled",
		"no_leaked_subscribers_after_destroy",
		"destroy_drops_app_listeners",
		"publish_through_handle_owner",
		"handler_throw_painted_E118_target_only",
		"handler_rejects_painted_E118",
		"other_handlers_still_run_after_throw",
		"detached_target_logs_only",
		"lifecycle_events_per_kind",
		"resolve_unknown_E116_painted_inline_others_still_work",
		"imperative_unknown_handler_throws_E116_at_call",
		"resolve_after_scripts",
		"late_registration_still_E116",
		"replay_first_paint_events_once_in_order",
		"handler_error_reuses_one_slot_per_code",
		"replay_skips_destroyed_handles",
		"inline_without_opt_in_E119",
		"inline_without_file_E119",
		"inline_with_compiler_ok",
		"fromJSON_builds_page_cards_nav", "fromJSON_card_ids_required_E121", "switcherStyleSlider", "switcherStyleBad", "switcherViewCardsAccepted", "switcherViewCardsAndCardRejected", "fromJSON_ribbon_item_ids_required_E121", "fromJSON_divider_no_handle", "fromJSON_model_blocks_become_declared", "fromJSON_bad_handler_is_finding_not_throw", "fromJSON_detail_template_and_tabs", "mount_creates_page_handle_with_paths", "mount_card_handles_are_instances_returned_by_cardApi", "mount_html_builtin_card_is_handle", "mount_replays_rendered_after_resolution", "unmount_destroys_tree", "second_mount_replaces_page", "premade_handles_win_over_json_instances", "late_registered_shell_card_subclass_is_not_adopted", "lateBase_builtins_registered_before_model_are_model_cards", "lateBase_builtins_registered_after_model_are_model_cards", "page_loaded_event_after_all_cards_settle", "mount_unregistered_name_painted_E116_page_renders", "mount_json_findings_painted_inline",
		// Client validation
		"corpus_every_case_has_mirror", "validate_never_throws", "validate_valid_page_has_no_findings", "validate_findings_shape", "schema_port_keywords_equal_java_set", "schema_port_full_schema_accepts_valid_page", "schema_pass_opt_in_on_json_mount", "pack_missing_reported", "handlerNameGrammar_json", "handlerNameGrammar_imperative", "reservedNamespace_json", "reservedNamespace_imperative", "actionIdUnique_json", "actionIdUnique_imperative", "actionIdGrammar_json", "actionIdGrammar_imperative", "onEventDocumented_page_json", "onEventDocumented_page_imperative", "onEventDocumented_card_json", "onEventDocumented_card_imperative", "onEventDocumented_table_json", "onEventDocumented_table_imperative", "onEventDocumented_row_imperative", "onEventDocumented_rowdetail_json", "onEventDocumented_rowdetail_imperative", "onEventDocumented_detailtab_json", "onEventDocumented_detailtab_imperative", "onEventDocumented_ribbonitem_json", "onEventDocumented_ribbonitem_imperative", "onEventDocumented_pagetab_json", "onEventDocumented_pagetab_imperative", "onEventDocumented_view_imperative", "onEventDocumented_chart_imperative", "subscribeTopic_json", "subscribeTopic_imperative", "subscribeTemplateScope_json", "subscribeTemplateScope_imperative", "inlineWithoutOptIn_json", "inlineWithoutOptIn_imperative", "modelVersion_json", "modelVersion_imperative", "contractVersion_json", "contractVersion_imperative", "siblingIdUnique_json", "siblingIdUnique_imperative", "subscriptionRole_json", "cardReservedKeys_json", "pageHeader_json", "schema_unknown_root_key_json", "schema_subscription_missing_as_json", "schema_subscription_both_as_and_handler_json", "schema_closed_card_extra_key_json",
		// Imperative construction
		"ctor_Page_requires_title_nav_activeNav_E121", "ctor_Page_contractVersion_default", "ctor_Page_modelVersion_bad_E120_thrown", "page_add_card_wraps_bare_Table", "page_addCard_unknown_type_E4", "add_already_parented_E121", "add_sibling_collision_E121", "path_deferred_until_root", "path_assigned_at_add", "table_setDetail_template_path", "detail_addTab_template", "template_change_applies_to_future_instances", "instance_addTab_transient", "ribbon_addItem_requires_id", "removeTab_removeItem_remove_destroy", "toJSON_page_full_contract_validates_against_schema", "toJSON_includes_optional_root_keys", "toJSON_includes_model_blocks", "h_action_bind_listen_write_under_model", "fromJSON_toJSON_roundtrip_equal", "build_equals_fromJSON_mount", "identity_kept_through_mount", "page_mount_calls_shell_mount_with_toJSON_and_instances", "imperative_bad_handler_throws_E114_E123_E119_at_call", "prebuilt_handles_win_over_shell_instances"
	};

	@TestFactory
	Stream<DynamicTest> checks() {
		return Arrays.stream(KEYS).map(k -> DynamicTest.dynamicTest(k, () -> {
			assumeTrue(report != null, "node (or the harness) is not available");
			assertTrue(report.containsKey(k), "missing report key: " + k);
			assertEquals(true, report.get(k));
		}));
	}
}
