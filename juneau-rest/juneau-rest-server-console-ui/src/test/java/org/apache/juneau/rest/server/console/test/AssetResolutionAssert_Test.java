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
package org.apache.juneau.rest.server.console.test;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // The static in-process MockRestClient C (and its responses) live for the whole test class and hold no external resources
})
class AssetResolutionAssert_Test extends TestBase {

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		/** A representative console page: the chrome's own stylesheet, theme and shell script, plus an image. */
		@RestGet(path="/page")
		public void page(RestRequest req, RestResponse res) throws IOException {
			res.setContentType("text/html");
			res.getWriter().write("<html><head>"
				+ "<link rel=\"stylesheet\" href=\"" + ConsoleChromeMixin.chromeCssUrl(req) + "\">"
				+ "<link rel=\"stylesheet\" href=\"" + ConsoleChromeMixin.themeAssetUrl(req, "gray") + "\">"
				+ "<script src=\"" + ConsoleChromeMixin.consoleJsUrl(req) + "\"></script>"
				+ "</head><body><img src=\"/logo.svg\"></body></html>");
		}

		@RestGet(path="/logo.svg")
		public void logo(RestResponse res) throws IOException {
			res.setContentType("image/svg+xml");
			res.getWriter().write("<svg xmlns=\"http://www.w3.org/2000/svg\"/>");
		}
	}

	private static final MockRestClient C = MockRestClient.buildLax(Host.class);

	private static int status(String url) {
		try {
			return C.get(url).run().getStatusCode();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String html(String path) throws Exception {
		return C.get(path).accept("text/html").run().assertStatus(200).getContent().asString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - the representative page
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_chromePageAssetsAllResolve() throws Exception {
		var page = html("/page");
		assertEquals(4, AssetResolutionAssert.assetUrls(page).size());
		AssetResolutionAssert.assertAssetsResolve("/page", page, AssetResolutionAssert_Test::status);
	}

	@Test void a02_theChromeLinksItsMountedAssets() throws Exception {
		var urls = String.join("\n", AssetResolutionAssert.assetUrls(html("/page")));
		assertContainsAll(urls, "/juneau-console/chrome.css", "/juneau-console/juneau-console.js", "/juneau-console/themes/juneau-theme-gray.css", "/logo.svg");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - the helper itself
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_missingAssetIsReported() {
		var html = "<link rel=\"stylesheet\" href=\"/juneau-console/chrome.css\"><script src=\"/juneau-console/gone.js\"></script>";
		assertList(AssetResolutionAssert.unresolved(html, AssetResolutionAssert_Test::status), "404 /juneau-console/gone.js");
		var e = assertThrows(AssertionError.class, () -> AssetResolutionAssert.assertAssetsResolve("/p", html, AssetResolutionAssert_Test::status));
		assertContainsAll(e.getMessage(), "Unresolved assets linked from /p", "404 /juneau-console/gone.js");
	}

	@Test void b02_onlyAssetLinksAndSameOriginUrlsAreChecked() {
		var html = "<link rel=\"canonical\" href=\"/c\">"
			+ "<link rel=\"stylesheet\" href=\"https://cdn.example.com/x.css\">"
			+ "<script src=\"//cdn.example.com/x.js\"></script>"
			+ "<img src=\"data:image/png;base64,AAAA\">"
			+ "<script data-src=\"/not-an-asset.js\"></script>"
			+ "<script>var s = 1;</script>"
			+ "<link rel=\"icon\" href=\"/i.ico#frag\">"
			+ "<script src=\"/a.js?x=1&amp;y=2\"></script>"
			+ "<script src=\"/a.js?x=1&amp;y=2\"></script>";
		assertList(AssetResolutionAssert.assetUrls(html), "/i.ico", "/a.js?x=1&y=2");
	}

	@Test void b03_noAssetsPasses() {
		AssetResolutionAssert.assertAssetsResolve("<p>none</p>", u -> 500);
		assertEquals(List.of(), AssetResolutionAssert.unresolved("<p>none</p>", u -> 500));
	}
}
