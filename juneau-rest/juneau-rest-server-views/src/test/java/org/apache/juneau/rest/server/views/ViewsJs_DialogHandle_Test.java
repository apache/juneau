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

/** Source-shape and Node behavioral checks for the {@code DialogHandle} returned by {@code dialogs.open}. */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_DialogHandle_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static Map<?,?> report() {
		var r = NodeHarness.report("dialog-handle.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(r != null, "node (or dialog-handle.cjs) not available - behavioral layer skipped");
		return r;
	}

	@Test void a01_sourceExposesHandleAndChromeRegistry() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("ui.setConfirmLabel"), "setConfirmLabel");
		assertTrue(body.contains("ui.onConfirm"), "onConfirm");
		assertTrue(body.contains("E-JS-72"), "unregistered chrome code");
		assertTrue(body.contains("depth: dialogLayerCount"), "dialogs.depth");
	}

	@Test void b01_handleSurface() {
		assertEquals(true, report().get("handleSurface"));
	}

	@Test void b02_stepsAdvanceRetreatAndSubmitOnTheLast() {
		var w = (Map<?,?>)report().get("wizard");
		assertEquals("edit", w.get("step0"));
		assertEquals("Next", w.get("label0"));
		assertEquals(true, w.get("backHidden0"));
		assertEquals("review", w.get("step1"));
		assertEquals("Apply", w.get("label1"));
		assertEquals("Review", w.get("title1"));
		assertEquals(true, w.get("backShown1"));
		assertEquals(1, ((Number)w.get("openAfterNext")).intValue());
		assertEquals("edit", w.get("stepBack"));
		assertEquals("edit", w.get("stepAfterUnknown"));
		assertEquals("review", w.get("stepAfterSet"));
		assertEquals("submitted", w.get("closed"));
		assertEquals(0, ((Number)w.get("openAfterSubmit")).intValue());
	}

	@Test void b03_relabelingAndOnConfirmGating() {
		var g = (Map<?,?>)report().get("gate");
		assertEquals("Review changes", g.get("confirmLabel"));
		assertEquals("Back out", g.get("cancelLabel"));
		assertEquals(true, g.get("fieldsIsObject"));
		assertEquals(1, ((Number)g.get("openAfterFalse")).intValue());
		assertEquals(1, ((Number)g.get("openAfterPromiseFalse")).intValue());
		assertEquals(1, ((Number)g.get("openAfterTruthyNotTrue")).intValue());
		assertEquals("submitted", g.get("closed"));
		assertEquals(4, ((Number)g.get("calls")).intValue());
		assertEquals(0, ((Number)g.get("openAfterTrue")).intValue());
	}

	@Test void b04_closePopsOnlyItsOwnLayerAndDepthCapHolds() {
		var c2 = (Map<?,?>)report().get("layers");
		assertEquals(2, ((Number)c2.get("depthTwo")).intValue());
		assertEquals(true, c2.get("thirdRefused"));
		assertEquals(1, ((Number)c2.get("depthAfterInnerClose")).intValue());
		assertEquals(1, ((Number)c2.get("outerStillOpen")).intValue());
		assertEquals("cancelled", c2.get("innerClosed"));
		assertEquals("cancelled", c2.get("outerClosed"));
		assertEquals(1, ((Number)c2.get("onCancelRan")).intValue());
		assertEquals(0, ((Number)c2.get("depthEnd")).intValue());
	}

	@Test void b05_chromeRegistry() {
		var ch = (Map<?,?>)report().get("chrome");
		assertEquals(true, ch.get("unknownReturnsNull"));
		assertEquals(true, ch.get("depthUnchangedByUnknown"));
		assertEquals(true, ch.get("opened"));
		var seen = (List<?>)ch.get("seen");
		assertEquals(1, seen.size());
		assertEquals(List.of("actions", "cancelBtn", "confirmBtn", "dialog", "dismissBtn", "header"), ((Map<?,?>)seen.get(0)).get("keys"));
	}

	@Test void b06_openWithActionSubmitsOnlyOnTheLastStep() {
		var sub = (Map<?,?>)report().get("submit");
		assertEquals(0, ((Number)sub.get("afterNext")).intValue());
		assertEquals(1, ((Number)sub.get("total")).intValue());
		assertEquals("/x/apply", sub.get("url"));
		assertEquals("apply", sub.get("action"));
	}
}
