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
package org.apache.juneau.petstore.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class PetstoreUiResource_Test extends TestBase {

	private static final MockRestClient CLIENT = MockRestClient.buildLax(PetstoreUiResource.class);

	@Test void a01_uiHtml_servedFromClasspath() throws Exception {
		CLIENT.get("/static/petstore-ui.html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("<title>Juneau Petstore — React UI</title>");
	}

	@Test void a02_uiHtml_referencesPetstoreEndpoint() throws Exception {
		// Sanity check that the SPA actually points at the right backend route — guards against
		// accidental rename of /petstore/pets without updating the SPA.
		CLIENT.get("/static/petstore-ui.html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("/petstore/pets");
	}

	@Test void a03_unknownStaticFile_404() throws Exception {
		CLIENT.get("/static/no-such-file.html")
			.run()
			.assertStatus(404);
	}

	@Test void a04_resourceRoot_servesTheApp() throws Exception {
		// The console iframes and links the bare /petstore-ui URL; it must answer, not 404.
		CLIENT.get("/")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("<title>Juneau Petstore — React UI</title>");
	}

	@Test void a05_page_hasNoExternalScriptUrls() throws Exception {
		// The page must load fully offline: every script src is same-origin, none is an http(s) URL.
		var html = CLIENT.get("/static/petstore-ui.html").run().assertStatus(200).getContent().asString();
		var srcs = java.util.regex.Pattern.compile("<script[^>]*\\ssrc\\s*=\\s*[\"']([^\"']*)[\"']").matcher(html);
		var count = 0;
		while (srcs.find()) {
			count++;
			assertFalse(srcs.group(1).matches("(?i)^(https?:)?//.*"), "External script src: " + srcs.group(1));
		}
		assertEquals(2, count, "Expected exactly the two vendored React scripts.");
		assertFalse(html.contains("text/babel"), "Page must not require in-browser JSX compilation.");
	}

	@Test void a06_vendoredReact_served() throws Exception {
		CLIENT.get("/static/vendor/react.production.min.js")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").asString().isContains("javascript")
			.assertContent().asString().isContains("react.production.min.js");
	}

	@Test void a07_vendoredReactDom_served() throws Exception {
		CLIENT.get("/static/vendor/react-dom.production.min.js")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").asString().isContains("javascript")
			.assertContent().asString().isContains("react-dom.production.min.js");
	}

	@Test void a08_page_scriptSrcsResolveToServedFiles() throws Exception {
		// Each script src in the page, resolved against the resource mount, must be served 200.
		var html = CLIENT.get("/static/petstore-ui.html").run().getContent().asString();
		var srcs = java.util.regex.Pattern.compile("<script[^>]*\\ssrc\\s*=\\s*[\"']([^\"']*)[\"']").matcher(html);
		while (srcs.find()) {
			var src = srcs.group(1);
			assertTrue(src.startsWith("/petstore-ui/"), "Unexpected script path: " + src);
			CLIENT.get(src.substring("/petstore-ui".length())).run().assertStatus(200);
		}
	}
}
