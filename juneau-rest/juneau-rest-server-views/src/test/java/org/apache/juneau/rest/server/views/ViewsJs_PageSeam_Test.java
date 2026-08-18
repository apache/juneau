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
 * Lazy-init seam test for {@code juneau-views.js} (TODO-399 Phase C, Task 6, design doc §"Client page runtime").
 *
 * <p>
 * There is no JS-execution (jsdom/Option-B) harness in this module yet (confirmed by inspecting
 * {@code ViewsMixin_Serving_Test}, which is Option-A-only: {@code e03}/{@code g04} assert on served-script content
 * substrings, not on executed behavior) &mdash; so, per that established convention, this test asserts on the
 * served script text: {@code initTable} must be reachable off the public {@code NS.init} namespace (previously
 * private), and {@code initAll} must skip tables scoped under a {@code [data-juneau-page]} shell.
 */
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
