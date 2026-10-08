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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.petstore.console.browser.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.console.test.*;
import org.junit.jupiter.api.*;

/**
 * Every stylesheet and script a console page links resolves on the host the runners and the browser tests mount.
 *
 * <p>
 * The console chrome links its assets at context-root URLs ({@code /juneau-console/chrome.css}, the theme pack, the
 * shell script), so they only load if the host mounts something at those paths.  MockRest page tests assert the
 * hrefs; this test fetches them with {@link AssetResolutionAssert}.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreAssets_Test extends TestBase {

	private static final MockRestClient C = MockRestClient.create(PetstoreTestServer.BrowserHost.class).noTrace().ignoreErrors().build();

	private static String html(String path) throws Exception {
		return C.get(path).accept("text/html").run().assertStatus(200).getContent().asString();
	}

	private static int status(String url) {
		try {
			return C.get(url).run().getStatusCode();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - every linked asset answers 200
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_everyPageAssetResolves() throws Exception {
		for (var path : PetstorePages_ContractTest.PAGE_PATHS)
			AssetResolutionAssert.assertAssetsResolve(path, html(path), PetstoreAssets_Test::status);
	}

	@Test void a02_theChromeLinksItsMountedAssets() throws Exception {
		assertContainsAll(String.join("\n", AssetResolutionAssert.assetUrls(html("/console/store"))), "/juneau-console/chrome.css", "/juneau-console/juneau-console.js");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - the pages' own links and the DataTables dependencies
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_theReactFlavorEmbedsAnAppThatAnswers() throws Exception {
		var html = html("/console/dev/flavors/react");
		assertContains("src=\"/petstore-ui\"", html);
		org.junit.jupiter.api.Assertions.assertEquals(200, status("/petstore-ui"));
	}

	private static final String JQUERY = "/webjars/jquery/3.7.1/jquery.min.js";
	private static final String DATATABLES = "/webjars/datatables.net/2.3.8/js/dataTables.min.js";

	@Test void b02_everyDatatablesPageLoadsJqueryAndDataTablesFromTheWebJarsBeforeTheToolkit() throws Exception {
		for (var path : java.util.List.of("/console/ops/jobs", "/console/ops/audit")) {
			var html = html(path);
			var jquery = html.indexOf(JQUERY);
			var dataTables = html.indexOf(DATATABLES);
			var glue = html.indexOf("juneau-datatables.js");
			var toolkit = html.indexOf("juneau-views.js");
			org.junit.jupiter.api.Assertions.assertTrue(jquery > 0 && dataTables > jquery && glue > dataTables && toolkit > glue,
				() -> path + ": jquery=" + jquery + ", dataTables=" + dataTables + ", glue=" + glue + ", views=" + toolkit);
			assertContains("dataTables.dataTables.min.css", html);
			org.junit.jupiter.api.Assertions.assertEquals(html.indexOf(JQUERY), html.lastIndexOf(JQUERY), () -> path + ": jQuery loads once");
			org.junit.jupiter.api.Assertions.assertFalse(html.contains("cdn."), () -> path + " must not load a library from a CDN");
			org.junit.jupiter.api.Assertions.assertFalse(html.contains("/console/vendor/"), () -> path + " still loads the old vendor copies");
		}
		org.junit.jupiter.api.Assertions.assertFalse(html("/console/about").contains("jquery"), "a page without a table loads no jQuery");
	}

	@Test void b03_theWebJarsAreServed() throws Exception {
		for (var url : java.util.List.of("/console/ops/audit" + JQUERY, "/console/ops/audit" + DATATABLES))
			C.get(url).run().assertStatus(200).assertHeader("Content-Type").asString().isContains("javascript");
	}

	@Test void b04_aPathOutsideTheWebJarsDoesNotResolve() throws Exception {
		org.junit.jupiter.api.Assertions.assertEquals(404, status("/console/ops/audit/webjars/../x"));
		org.junit.jupiter.api.Assertions.assertEquals(404, status("/console/ops/audit/webjars/no-such/file.js"));
		org.junit.jupiter.api.Assertions.assertEquals(404, status("/console/vendor/jquery/3.7.1/jquery.min.js"));
	}
}
