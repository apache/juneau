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

/** Client-rendered tabs: the renderer registry and its context, run through the Node harness. */
class DetailRegister_Test {

	private static Map<?,?> report() {
		var r = RegionsHarness.reportWith("detail-register.cjs", ViewsMixin.HELPERS_JS_RESOURCE, ViewsMixin.BUS_JS_RESOURCE,
			"/org/apache/juneau/views/juneau-model.js");
		assumeTrue(r != null, "node not available or detail-register.cjs not found - behavioral layer skipped");
		return r;
	}

	@Test void a01_registered_calledOnce_withHandleAndFrozenData() {
		var r = report();
		assertEquals(true, r.get("reg_calledOnce"));
		assertEquals("c:detailtab", r.get("reg_tab"));
		assertEquals("fail,fetch,invalidate,pane,refresh,row,rowId,signal", r.get("reg_dataKeys"));
		assertEquals(true, r.get("reg_dataFrozen"));
		assertEquals("s-7:Rex:secret", r.get("reg_row"));
		assertEquals("initial", r.get("reg_refresh"));
		assertEquals(true, r.get("reg_paneSignal"));
		assertEquals(true, r.get("reg_painted"));
	}

	@Test void a02_helpers_frozenWhitelist() {
		assertEquals("fieldGrid,renderAsyncStatus:true:function:function:fieldGrid,renderAsyncStatus", report().get("reg_helpers"));
	}

	@Test void a03_blankNameOrNonFunction_refused() {
		assertEquals(3, ((Number)report().get("refuse_errors")).intValue());
	}

	@Test void a04_lastWriteWins() {
		assertEquals(true, report().get("lastWins"));
	}

	@Test void a05_unregistered_showsError() {
		assertEquals("Renderer 'nope' is not registered", report().get("unregistered"));
	}

	@Test void a06_fetch_sameOrigin_substitutes_csrf_accept() {
		var r = report();
		assertEquals("/api/pet/s-7/labels", r.get("fetch_url"));
		assertEquals("application/json", r.get("fetch_accept"));
		assertEquals(true, r.get("fetch_csrf"));
		assertEquals(true, r.get("fetch_cross"));
		assertEquals(true, r.get("fetch_proto"));
		assertEquals(true, r.get("fetch_noEvilRequest"));
		assertEquals("ok:{\"contractVersion\":\"1\",\"labels\":[\"a\"]}", r.get("fetch_good"));
	}

	@Test void a07_fetch_rejectsNon2xx_andWrongContract() {
		var r = report();
		assertEquals(true, r.get("fetch_bad"));
		assertEquals(true, r.get("fetch_contract"));
	}

	@Test void a08_throw_showsError_withRetry_thatReruns() {
		var r = report();
		assertEquals("kaput", r.get("throw_status"));
		assertEquals(true, r.get("throw_hasRetry"));
		assertEquals(true, r.get("retry_reran"));
	}

	@Test void a09_failAndRejection_showError() {
		var r = report();
		assertEquals("nope", r.get("fail_status"));
		assertEquals(true, r.get("fail_retry"));
		assertEquals("async boom", r.get("reject_status"));
	}

	@Test void a10_promise_loadingThenEmptyText() {
		var r = report();
		assertEquals("Loading...", String.valueOf(r.get("promise_loading")).replace("\u2026", "..."));
		assertEquals("Nothing here", r.get("promise_empty"));
	}
}
