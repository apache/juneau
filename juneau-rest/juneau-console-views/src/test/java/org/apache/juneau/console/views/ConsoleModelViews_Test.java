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
 * Node-sandbox coverage for the hooks the client object model has in {@code juneau-views.js} and
 * {@code juneau-ribbon.js} (table handle, draw cycle, detail rows, ribbon), through {@code model-views.cjs}.
 */
class ConsoleModelViews_Test extends TestBase {

	private static final String MODEL_JS = "/org/apache/juneau/views/juneau-model.js";

	private static Map<String,Object> report;

	@BeforeAll
	static void run() {
		report = BusHarness.run("model-views.cjs", ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE,
			ViewsMixin.RIBBON_JS_RESOURCE, ViewsMixin.SEARCH_JS_RESOURCE, MODEL_JS);
	}

	private static final String[] KEYS = {
		"tables_handle_is_model_Table",
		"tables_handle_four_members",
		"tables_ready_resolves_with_table",
		"draw_hooks_bound_to_own_table_not_nested",
		"draw_keyed_expanded_detaches_not_destroys",
		"draw_keyed_default_destroys_detail",
		"draw_positional_destroyed",
		"draw_missing_row_evicted",
		"registry_stable_after_draws",
		"expand_creates_detail_instance_and_tabs",
		"collapse_destroys_detail_keeps_row",
		"row_expanded_collapsed_events",
		"detail_actions_painted_and_click_runs_handler",
		"ribbon_item_handles_by_id",
		"ribbon_missing_id_custom_item_E121",
		"ribbon_stock_default_ids_client_mirror",
		"ribbon_duplicate_stock_kind_E121",
		"ribbon_new_item_types_have_handles",
		"ribbon_hidden_items_leave_no_handle",
		"detail_tab_setLabel_on_strip_button",
		"ribbon_action_item_click_handler",
		"ribbon_stock_refresh_unchanged",
		"table_loaded_event_count",
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
