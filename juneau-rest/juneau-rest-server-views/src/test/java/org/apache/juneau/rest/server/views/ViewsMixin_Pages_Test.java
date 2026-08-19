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
 * Serving + versioned-URL tests for the {@code juneau-pages.js} asset on {@link ViewsMixin}.
 *
 * <p>
 * Extends the existing {@code ViewsMixin_Serving_Test} Option-A serving-smoke pattern to the new opt-in page-runtime
 * asset: 200 + correct content-type + {@code Cache-Control}, a 404 when the mixin is absent, and the
 * {@code ?v=<buildVersion>} cache-buster via {@link ViewsMixin#viewAssetUrl(String)}.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class ViewsMixin_Pages_Test extends TestBase {

	public static class NoMixin extends org.apache.juneau.rest.server.servlet.BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends org.apache.juneau.rest.server.servlet.BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient cNoMixin = MockRestClient.buildLax(NoMixin.class);
	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);

	@Test void a01_hostWithoutMixin_pagesJsRouteIs404() throws Exception {
		cNoMixin.get(ViewsMixin.PAGES_JS_PATH).run().assertStatus(404);
	}

	@Test void a02_pagesJs_served() throws Exception {
		cWithMixin.get(ViewsMixin.PAGES_JS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/javascript")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-pages.js");
	}

	@Test void a03_viewAssetUrl_carriesVersionAndContentHashCacheBusterForPagesJs() {
		var v = ViewsMixin.class.getPackage().getImplementationVersion();
		var expectedPrefix = "servlet:" + ViewsMixin.PAGES_JS_PATH + "?v=" + (v == null ? "dev" : v) + "-";
		var url = ViewsMixin.viewAssetUrl(ViewsMixin.PAGES_JS_PATH);
		assertTrue(url.startsWith(expectedPrefix), url);
		assertTrue(url.substring(expectedPrefix.length()).matches("[0-9a-f]{8}"), url);
	}

	@Test void a04_pagesJs_bakesInPageContractVersionHandshake() throws Exception {
		var body = cWithMixin.get(ViewsMixin.PAGES_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("JUNEAU_PAGE_CONTRACT_VERSION"), body);
		assertTrue(body.contains("\"" + PageDef.CONTRACT_VERSION + "\""), body);
	}
}
