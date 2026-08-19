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
import org.junit.jupiter.api.*;

/**
 * Lazy-init seam test for {@code juneau-views.js} (design doc §"Client page runtime").
 *
 * <p>
 * Asserts on the served script text: {@code initTable} must be reachable off the public {@code NS.init} namespace
 * (previously private), and {@code initAll} must skip tables scoped under a {@code [data-juneau-page]} shell.
 * <p>
 * The <i>consequences</i> of this seam &mdash; that a panel lazy-inits exactly the view tables it owns, and that a
 * sub-tabbed tab's outer panel does not claim its sub-panels' tables &mdash; are verified behaviourally in
 * {@link PagePanelVisibility_BrowserTest}, which stubs {@code NS.init.initTable} in a real browser and records which
 * views it is called for.  These text assertions remain as always-on tripwires for the seam's <i>shape</i>, since
 * that harness is opt-in.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_PageSeam_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends org.apache.juneau.rest.server.servlet.BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	@Test void a01_initTableIsPubliclyExposedOnNsInit() throws Exception {
		var body = c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("initTable: initTable"), body);
	}

	@Test void a02_initAllSkipsTablesScopedUnderAJuneauPageShell() throws Exception {
		var body = c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var initAllStart = body.indexOf("function initAll(");
		assertTrue(initAllStart >= 0, () -> "initAll not found:\n" + body);
		var initAllEnd = body.indexOf("\n\t}", initAllStart);
		var initAllBody = body.substring(initAllStart, initAllEnd < 0 ? body.length() : initAllEnd);
		assertTrue(initAllBody.contains("data-juneau-page"), initAllBody);
	}

	@Test void a03_initTableRemainsIdempotentGuardIntact() throws Exception {
		// Regression: the seam must not remove the existing isDataTable idempotent re-entry guard.
		var body = c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("isDataTable(table)"), body);
	}
}
