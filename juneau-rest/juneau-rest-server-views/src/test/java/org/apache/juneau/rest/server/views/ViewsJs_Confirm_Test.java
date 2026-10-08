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

/** Source-shape and Node behavioral checks for {@code JuneauViews.dialogs.confirm} and its renderer registry. */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_Confirm_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static Map<?,?> report() {
		var r = NodeHarness.report("confirm.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(r != null, "node (or confirm.cjs) not available - behavioral layer skipped");
		return r;
	}

	@Test void a01_sourceExposesConfirmSurface() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("confirm: confirmPipeline"), "dialogs.confirm");
		assertTrue(body.contains("registerConfirmRenderer: registerConfirmRenderer"), "rowActions.registerConfirmRenderer");
		assertTrue(body.contains("registerChrome: registerDialogChrome"), "dialogs.registerChrome");
		assertTrue(body.contains("E-JS-71"), "unregistered renderer code");
	}

	@Test void b01_registeredRendererDecidesTheAnswer() {
		var r = report();
		assertEquals(true, r.get("surface"));
		assertEquals(true, r.get("stubOk"));
		assertEquals(false, r.get("stubCancel"));
		assertEquals(true, r.get("duplicateKeptFirst"));
		assertEquals(true, r.get("actionObjectRenderer"));
	}

	@Test void b02_rendererReceivesTheDescribedRequest() {
		var saw = (List<?>)report().get("rendererSaw");
		var first = (Map<?,?>)saw.get(0);
		assertEquals("danger", first.get("tone"));
		assertEquals("Go", first.get("confirmLabel"));
		assertEquals(2, ((Number)first.get("items")).intValue());
		assertEquals(true, first.get("sameRequest"));
		var second = (Map<?,?>)saw.get(1);
		assertEquals("default", second.get("tone"));
		assertEquals("Confirm", second.get("confirmLabel"));
	}

	@Test void b03_unregisteredRendererRejectsWithE_JS_71() {
		assertEquals("E-JS-71", report().get("unregistered"));
	}

	@Test void b04_builtInModalCapsListAndStylesDanger() {
		var b = (Map<?,?>)report().get("builtIn");
		assertEquals(true, b.get("shown"));
		assertEquals(21, ((Number)b.get("listItems")).intValue());
		assertEquals("\u2026and 5 more", b.get("lastItem"));
		assertEquals("Delete", b.get("confirmText"));
		assertEquals(true, b.get("dangerClass"));
		assertEquals(true, b.get("cancelFocused"));
		assertEquals(1, ((Number)b.get("depthWhileOpen")).intValue());
	}

	@Test void b05_builtInConfirmResolvesAndRunsActionOnlyOnConfirm() {
		var b = (Map<?,?>)report().get("builtIn");
		assertEquals(true, b.get("resolved"));
		assertEquals(1, ((Number)b.get("actionRuns")).intValue());
		assertEquals(true, b.get("goneAfter"));
		assertEquals(false, b.get("cancelResolved"));
		assertEquals(1, ((Number)b.get("actionRunsAfterCancel")).intValue());
		assertEquals(true, report().get("defaultTonePlain"));
		assertEquals(0, ((Number)report().get("depthAfter")).intValue());
	}
}
