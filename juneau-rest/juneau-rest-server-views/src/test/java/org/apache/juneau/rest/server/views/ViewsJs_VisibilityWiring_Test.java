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
 * Where {@code visibleWhen} takes effect: a Node behavioral proof (via {@code visibility-wiring.cjs}) for row actions,
 * plus always-on source-shape checks for the ribbon, field grid and region paths.
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_VisibilityWiring_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String asset(String path) throws Exception {
		return c.get(path).run().assertStatus(200).getContent().asString();
	}

	private static Map<?,?> report() {
		var r = NodeHarness.report("visibility-wiring.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(r != null, "node (or visibility-wiring.cjs) not available - behavioral layer skipped");
		return r;
	}

	@Test void a01_menuOmitsActionsHiddenForTheRow() {
		var r = report();
		assertEquals(List.of("ack", "page", "view"), r.get("menuOpenRow"));
		assertEquals(List.of("page", "view"), r.get("menuClosedRow"));
	}

	@Test void a02_pillAndTriggerHiddenAtDrawTime() {
		var r = report();
		assertEquals(true, r.get("pillHidden"));
		assertEquals(true, r.get("pillShown"));
		assertEquals(true, r.get("triggerHiddenWhenNothingLeft"));
		assertEquals(true, r.get("triggerShownWhenSomethingLeft"));
	}

	@Test void a03_hiddenActionNeverFires() {
		var r = report();
		assertEquals(false, r.get("hiddenActionFired"));
		assertEquals(true, r.get("visibleActionFired"));
	}

	@Test void b01_ribbonFiltersItemsByFacts() throws Exception {
		var body = asset(ViewsMixin.RIBBON_JS_PATH);
		assertTrue(body.contains("ribbonItemVisible"), body.length() + " bytes");
		assertTrue(body.contains(".filter(ribbonItemVisible)"));
	}

	@Test void b02_fieldGridSkipsHiddenFields() throws Exception {
		var body = asset(ViewsMixin.HELPERS_JS_PATH);
		assertTrue(body.contains("NS.rules.testRow(field.visibleWhen, values)"));
	}

	@Test void b03_hiddenRegionTakesTheExistingDeferredPath() throws Exception {
		var body = asset(ViewsMixin.REGIONS_JS_PATH);
		assertTrue(body.contains("hiddenByVisibleWhen(region)"));
		assertTrue(body.contains("data-juneau-region-hidden"));
	}
}
