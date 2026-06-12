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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end verification of the per-request Content-Security-Policy nonce handshake: the REST layer mints one nonce
 * per response, substitutes it into the {@code Content-Security-Policy} header's {@code {nonce}} placeholder, and the
 * HtmlDoc serializer stamps the SAME nonce onto the inline {@code <script>}/{@code <style>} tags it emits.
 */
@SuppressWarnings({
	"serial"  // serialVersionUID not required for test classes.
})
class RestContentSecurityPolicyNonce_Test extends TestBase {

	private static final Pattern NONCE_IN_HEADER = Pattern.compile("'nonce-([A-Za-z0-9_-]+)'");

	@Rest(defaultResponseHeaders = {
		"Content-Security-Policy: default-src 'self'; script-src 'self' 'nonce-{nonce}'; style-src 'self' 'nonce-{nonce}'"
	})
	public static class A_Nonce extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test void a01_headerAndTagsShareTheSameNonce() throws Exception {
		var res = MockRestClient.buildLax(A_Nonce.class).get("/page").accept("text/html").run();
		var csp = res.getHeader("Content-Security-Policy").asString().orElseThrow();
		var body = res.getContent().asString();

		// Placeholder must be gone, replaced by a real nonce.
		assertFalse(csp.contains("{nonce}"), () -> "Placeholder not resolved: " + csp);

		var m = NONCE_IN_HEADER.matcher(csp);
		assertTrue(m.find(), () -> "No nonce in header: " + csp);
		var nonce = m.group(1);

		// Both nonce sources in the header must use the same token.
		var m2 = NONCE_IN_HEADER.matcher(csp);
		while (m2.find())
			assertEquals(nonce, m2.group(1), () -> "Header uses mismatched nonces: " + csp);

		// The inline tags must be stamped with the SAME nonce as the header.
		assertTrue(body.contains("<style nonce=\"" + nonce + "\">"), () -> "Style tag missing matching nonce (" + nonce + "): " + body);
		assertTrue(body.contains("<script nonce=\"" + nonce + "\">"), () -> "Script tag missing matching nonce (" + nonce + "): " + body);
	}

	@Test void a02_freshNoncePerResponse() throws Exception {
		var c = MockRestClient.buildLax(A_Nonce.class);
		var csp1 = c.get("/page").accept("text/html").run().getHeader("Content-Security-Policy").asString().orElseThrow();
		var csp2 = c.get("/page").accept("text/html").run().getHeader("Content-Security-Policy").asString().orElseThrow();
		assertNotEquals(csp1, csp2, "Nonce must not be reused across responses");
	}

	//------------------------------------------------------------------------------------------------------------------
	// A static (no-nonce) policy must pass through untouched, and no nonce must leak onto the tags.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(defaultResponseHeaders = {
		"Content-Security-Policy: default-src 'self'; object-src 'none'"
	})
	public static class B_Static extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test void b01_staticPolicyUnchangedAndNoNonceStamped() throws Exception {
		var res = MockRestClient.buildLax(B_Static.class).get("/page").accept("text/html").run();
		var csp = res.getHeader("Content-Security-Policy").asString().orElseThrow();
		var body = res.getContent().asString();
		assertEquals("default-src 'self'; object-src 'none'", csp);
		assertFalse(body.contains("nonce="), () -> "No nonce should be stamped for a static policy: " + body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Report-only header name is honored end-to-end.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(defaultResponseHeaders = {
		"Content-Security-Policy-Report-Only: script-src 'self' 'nonce-{nonce}'"
	})
	public static class C_ReportOnly extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test void c01_reportOnlyHeaderResolvesNonce() throws Exception {
		var res = MockRestClient.buildLax(C_ReportOnly.class).get("/page").accept("text/html").run();
		var csp = res.getHeader("Content-Security-Policy-Report-Only").asString().orElseThrow();
		var body = res.getContent().asString();
		assertFalse(csp.contains("{nonce}"), () -> csp);
		var m = NONCE_IN_HEADER.matcher(csp);
		assertTrue(m.find(), () -> csp);
		assertTrue(body.contains("<script nonce=\"" + m.group(1) + "\">"), () -> body);
	}
}
