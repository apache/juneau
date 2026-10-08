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

/** Source-shape and Node behavioral checks for {@code rowActions.run} on a confirm-only action with a confirm renderer. */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_RowActionsRun_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static Map<?,?> report() {
		var r = NodeHarness.report("row-actions-run.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(r != null, "node (or row-actions-run.cjs) not available - behavioral layer skipped");
		return r;
	}

	@Test void a01_confirmOnlyPathRoutesThroughThePipeline() throws Exception {
		var body = viewsJs();
		var fn = body.substring(body.indexOf("function openConfirmOnlyDialog("));
		fn = fn.substring(0, fn.indexOf("\n\t}\n"));
		assertTrue(fn.indexOf("confirmPipeline(") > 0, fn);
		assertTrue(fn.indexOf("confirmPipeline(") < fn.indexOf("submitActionDialog("), "confirm must precede the submit");
	}

	@Test void b01_confirmedActionSendsExactlyOneWrite() {
		var c2 = (Map<?,?>)report().get("confirmed");
		assertEquals(1, ((Number)c2.get("fetches")).intValue());
		assertEquals("/x/7", c2.get("url"));
		assertEquals("DELETE", c2.get("method"));
		assertEquals("delete", c2.get("action"));
		assertEquals("7", c2.get("targetId"));
	}

	@Test void b02_cancelledActionSendsNothing() {
		assertEquals(1, ((Number)((Map<?,?>)report().get("cancelled")).get("fetches")).intValue());
	}

	@Test void b03_rendererSeesFilledTitleLabelAndTone() {
		var asked = (List<?>)report().get("asked");
		var first = (Map<?,?>)asked.get(0);
		assertEquals("Delete Widget?", first.get("title"));
		assertEquals("Delete it", first.get("confirmLabel"));
		assertEquals("danger", first.get("tone"));
	}

	@Test void b04_emptyConfirmTokenRefusesBeforeAnythingShows() {
		var e = (Map<?,?>)report().get("emptyToken");
		assertEquals(0, ((Number)e.get("asked")).intValue());
		assertEquals(1, ((Number)e.get("fetches")).intValue());
	}

	@Test void b05_unknownActionAndOutsideRowReject() {
		assertEquals("E-JS-70", report().get("unknownAction"));
		assertEquals("E-JS-74", report().get("outsideRow"));
	}
}
