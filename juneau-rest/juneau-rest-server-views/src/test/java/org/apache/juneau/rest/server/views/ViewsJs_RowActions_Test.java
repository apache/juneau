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

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Always-on source-shape coverage for the {@code juneau-views.js} row-action + fail-closed CSRF plumbing
 * (TODO-415).  Mirrors {@code ViewsMixin_Serving_Test}'s served-script substring style: proves the load-bearing
 * pieces of the runtime's row-menu/submit contract are present in the shipped asset, without booting a browser
 * (the behavioral proof lives in the opt-in {@code RowActionCsrf_BrowserTest} canary).
 *
 * <p>
 * The client refusal these tests pin is <b>defense-against-consumer-omission</b>, not the security control: the
 * landed server-side {@link LoopbackBoundary} is.  The value of pinning the shapes here is that the two halves must
 * agree by construction &mdash; same default header, same JSON content type, same {@code isBlank}-not-{@code isEmpty}
 * fail-closed test, same non-safe-method rule.
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_RowActions_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	@Test void a01_contractVersionBumpedToThree() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("JUNEAU_VIEW_CONTRACT_VERSION = \"3\""), body);
		assertEquals("3", ViewDef.CONTRACT_VERSION);
	}

	@Test void a02_defaultCsrfHeaderMatchesTheServerBoundary() throws Exception {
		// The runtime's default header MUST equal LoopbackBoundary.DEFAULT_CSRF_HEADER or the two halves disagree.
		var body = viewsJs();
		assertTrue(body.contains("DEFAULT_CSRF_HEADER = \"" + LoopbackBoundary.DEFAULT_CSRF_HEADER + "\""), body);
		assertEquals("X-Csrf-Token", LoopbackBoundary.DEFAULT_CSRF_HEADER);
	}

	@Test void a03_failClosedTokenTestUsesBlankNotEmpty() throws Exception {
		// isBlankToken must match the boundary's check() (isBlank), so a WHITESPACE token also refuses - not the
		// SynchronizerToken.matches() isEmpty(), which would let whitespace through to a confusing 403.
		var body = viewsJs();
		var start = body.indexOf("function isBlankToken(");
		assertTrue(start >= 0, () -> "isBlankToken not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		var fn = body.substring(start, end < 0 ? body.length() : end);
		assertTrue(fn.contains(".trim()"), fn);   // whitespace-sensitive
	}

	@Test void a04_actionSubmitSendsJsonBodyAndCsrfHeader() throws Exception {
		var body = viewsJs();
		var start = body.indexOf("function buildActionRequest(");
		assertTrue(start >= 0, () -> "buildActionRequest not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		var fn = body.substring(start, end < 0 ? body.length() : end);
		// JSON content type (so the write passes LoopbackBoundary.isJson) + the token under the header name.
		assertTrue(fn.contains("\"Content-Type\": \"application/json\""), fn);
		assertTrue(fn.contains("JSON.stringify("), fn);
		assertTrue(fn.contains("headerName || DEFAULT_CSRF_HEADER"), fn);
		// Both fail-closed refusals: safe/absent method, and blank token.
		assertTrue(fn.contains("isSafeMethod(action.method)"), fn);
		assertTrue(fn.contains("isBlankToken(token)"), fn);
		assertTrue(fn.contains("refuse: true"), fn);
	}

	@Test void a05_safeMethodSetMirrorsMethodSafety() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("const SAFE_METHODS = { GET: 1, HEAD: 1, OPTIONS: 1, TRACE: 1 }"), body);
	}

	@Test void a06_rowMenuAndTriggerAreWired() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function initRowActions("), body);
		assertTrue(body.contains("function buildRowActionMenu("), body);
		assertTrue(body.contains("juneau-view-action-trigger"), body);
		assertTrue(body.contains("juneau-view-action-menu"), body);
	}

	@Test void a07_refusalIsVisibleNotSilent() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function renderRowActionRefusal("), body);
		assertTrue(body.contains("juneau-view-action-refusal"), body);
		assertTrue(body.contains("role\", \"alert\""), body);
	}

	@Test void a08_readsAutoEmbeddedCsrfAttribute() throws Exception {
		// The runtime reads the token ViewTable auto-embeds under data-juneau-csrf (ViewTable.CSRF_ATTR).
		var body = viewsJs();
		assertTrue(body.contains("data-juneau-csrf"), body);
		assertEquals("data-juneau-csrf", ViewTable.CSRF_ATTR);
	}
}
