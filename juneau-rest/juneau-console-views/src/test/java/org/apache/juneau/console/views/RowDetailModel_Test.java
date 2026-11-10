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

import org.junit.jupiter.api.*;

/** Object-model handles of a tabbed row detail, run through the Node harness. */
class RowDetailModel_Test {

	private static Map<?,?> report() {
		var r = RegionsHarness.reportWith("row-detail-model.cjs", ViewsMixin.HELPERS_JS_RESOURCE, ViewsMixin.BUS_JS_RESOURCE,
			"/org/apache/juneau/views/juneau-model.js");
		assumeTrue(r != null, "node not available or row-detail-model.cjs not found - behavioral layer skipped");
		return r;
	}

	@Test void h10_tabHandles_existPerId() {
		var r = report();
		assertEquals(true, r.get("detail_exists"));
		assertEquals("a|b", r.get("tab_handles"));
		assertEquals(true, r.get("tab_lookup"));
	}

	@Test void h11_loaded_firesOncePerFill_withCount() {
		assertEquals("loaded:detailtab:a:{\"count\":4}", report().get("loaded_once_with_count"));
	}

	@Test void h12_rendered_reachesRegisteredHandler() {
		assertEquals(true, report().get("rendered_fired"));
	}

	@Test void h13_stripActivation_emitsActivated() {
		assertEquals(true, report().get("activated_emitted"));
	}

	@Test void h14_handleActivate_drivesStrip() {
		assertEquals(true, report().get("handle_activate_drives_strip"));
	}

	@Test void h15_handlerAction_paintedAndInvoked() {
		var r = report();
		assertEquals(true, r.get("action_painted"));
		assertEquals(true, r.get("no_duplicate_buttons"));
		assertEquals("go:rowdetail:click", r.get("action_invoked"));
	}

	@Test void h16_throwingHandler_contained() {
		assertEquals(true, report().get("throwing_handler_contained"));
	}
}
