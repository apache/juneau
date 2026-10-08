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
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Always-on source-shape coverage for the sanctioned hook namespaces
 * ({@code JuneauViews.rowActions/dialogs/probeSelection/tables/status/csrf/details/html}), plus a Node behavioral
 * proof (via {@code hooks-api.cjs}) that every namespace is frozen and exposes the right member shapes.
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_Hooks_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Source-shape (always-on, no node needed)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_everyNamespaceIsFrozenAndPlacedAfterNsInit() throws Exception {
		var body = viewsJs();
		var anchor = body.indexOf("renderAsyncStatus: renderAsyncStatus");
		assertTrue(anchor >= 0, "NS.init closing anchor not found");
		for (var ns : List.of("NS.rowActions = Object.freeze(", "NS.dialogs = Object.freeze(",
				"NS.probeSelection = Object.freeze(", "NS.tables = Object.freeze(", "NS.status = Object.freeze(",
				"NS.csrf = Object.freeze(", "NS.details = Object.freeze(", "NS.html = Object.freeze(")) {
			assertTrue(body.indexOf(ns) > anchor, () -> ns + " not found after the NS.init anchor");
		}
	}

	@Test void a02_rowActionsRunResolvesContextBeforeDispatching() throws Exception {
		var body = viewsJs();
		var start = body.indexOf("NS.rowActions = Object.freeze(");
		var fragment = body.substring(start, body.indexOf("NS.dialogs = Object.freeze(", start));
		assertTrue(fragment.contains("hookContextOf"), fragment);
		assertTrue(fragment.contains("isDialogAction"), fragment);
		assertTrue(fragment.contains("E-JS-74"), fragment);
		assertTrue(fragment.contains("E-JS-70"), fragment);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Node behavioral harness (hooks-api.cjs) - runs when node is on PATH.
	// -----------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report() {
		var r = NodeHarness.report("hooks-api.cjs", ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE);
		assumeTrue(r != null, "node not available or hooks-api.cjs not found - behavioral layer skipped");
		return r;
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> ns(String name) {
		return (Map<String,Object>)report().get(name);
	}

	@Test void b01_everyNamespaceExistsAndIsFrozen() {
		for (var name : List.of("rowActions", "dialogs", "probeSelection", "tables", "status", "csrf", "details", "html")) {
			assertEquals(Boolean.TRUE, ns(name).get("exists"), name);
			assertEquals(Boolean.TRUE, ns(name).get("frozen"), name);
		}
	}

	@Test void b02_rowActionsHasTheFullMemberSet() {
		var members = (Map<?,?>)ns("rowActions").get("members");
		for (var m : List.of("contextOf", "find", "isDialog", "run", "open", "submit"))
			assertEquals("function", members.get(m), m);
	}

	@Test void b03_htmlEscDelegatesToRendersJs() {
		assertEquals("&lt;a&gt;&amp;&quot;&#39;", report().get("htmlEscOutput"));
	}

	@Test void b04_csrfHeaders() {
		var r = report();
		assertEquals(Map.of("X-Csrf-Token", "tok"), r.get("csrfHeadersWithToken"));
		assertEquals(Map.of("X-Mine", "tok"), r.get("csrfHeadersCustomName"));
		assertEquals(Map.of(), r.get("csrfHeadersBlank"));
		assertEquals(Map.of(), r.get("csrfHeadersNoEl"));
		assertEquals("tok", r.get("csrfToken"));
	}

	@Test void b05_tablesHandleAndReload() {
		var r = report();
		assertEquals("v", r.get("handleViewId"));
		assertEquals(Map.of("id", 7), r.get("handleRow"));
		assertEquals(Boolean.TRUE, r.get("handleNull"));
		assertEquals(Arrays.asList(null, false), r.get("reloadDefault"));
		assertEquals(Arrays.asList(null, true), r.get("reloadReset"));
		assertEquals(Boolean.TRUE, r.get("contextOfNull"));
	}
}
