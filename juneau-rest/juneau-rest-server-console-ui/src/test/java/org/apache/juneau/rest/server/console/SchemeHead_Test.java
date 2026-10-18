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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * The colour-scheme head pieces on {@link ConsoleChromeMixin}: the byte-constant pre-paint script and its CSP hash,
 * the dark override block, and the {@code schemeHead} helper for pages that don't render through the console
 * directive.
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse lifecycles are owned by the fluent chain; nothing long-lived leaks.
})
class SchemeHead_Test extends TestBase {

	/** The pre-paint script's inner text, pasted verbatim from the design. */
	private static final String EXPECTED_SCRIPT = """
		(function(d){var e=d.documentElement,k="juneau-console:scheme",s=null;
		if(e.hasAttribute("data-jc-scheme-toggle")){try{s=localStorage.getItem(k)}catch(x){}}
		if(s!=="light"&&s!=="dark"){s=e.getAttribute("data-jc-scheme-pin");}
		if(s!=="light"&&s!=="dark"){try{s=matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light"}catch(x){s="light"}}
		e.setAttribute("data-jc-scheme",s);})(document);""";

	private static final String SEP = "\n--8<--\n";

	@Rest
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/head", produces="text/plain")
		public String head(RestRequest req) {
			var nonce = req.getQueryParam("nonce").asString().orElse(null);
			if (nonce != null)
				req.setAttribute(RestRequest.CSP_NONCE_ATTR, nonce);
			var pin = req.getQueryParam("pin").asString().orElse(null);
			var toggle = "true".equals(req.getQueryParam("toggle").asString().orElse(null));
			var h = ConsoleChromeMixin.schemeHead(req, pin, toggle);
			return h.htmlAttributes() + SEP + h.head() + SEP + ConsoleChromeMixin.themeAssetUrl(req, "dark");
		}
	}

	private static String[] head(String query) throws Exception {
		var body = MockRestClient.buildLax(Host.class).get("/head?" + query).header("Accept", "text/plain").run().assertStatus(200).getContent().asString();
		var parts = body.split(SEP, -1);
		assertEquals(3, parts.length, () -> body);
		return parts;
	}

	@Test void a01_scriptBytes() {
		assertEquals(EXPECTED_SCRIPT, ConsoleChromeMixin.SCHEME_SCRIPT);
	}

	@Test void a02_hash() throws Exception {
		var digest = MessageDigest.getInstance("SHA-256").digest(EXPECTED_SCRIPT.getBytes(StandardCharsets.UTF_8));
		assertEquals(ConsoleChromeMixin.SCHEME_SCRIPT_CSP_HASH, "'sha256-" + Base64.getEncoder().encodeToString(digest) + "'");
	}

	@Test void a03_overrideBlock() {
		var tokens = new LinkedHashMap<String,String>();
		tokens.put("--jc-link", "#6cb4ff");
		assertEquals("@media not print{html:root[data-jc-scheme=\"dark\"]{--jc-link:#6cb4ff;--jc-focus:var(--jc-accent);}}",
			ConsoleChromeMixin.schemeOverrideBlock(tokens, Map.of("--jc-focus", "var(--jc-accent)")));
		assertEquals("", ConsoleChromeMixin.schemeOverrideBlock(Map.of(), Map.of()));
	}

	@Test void a03b_overrideBlock_leafIsEscaped() {
		var css = ConsoleChromeMixin.schemeOverrideBlock(Map.of("--jc-font", "'My;Font'"), Map.of());
		assertTrue(css.contains("\\3B "), css);
		assertFalse(css.contains("'My;Font'"), css);
	}

	@Test void a03c_overrideBlock_rejectsNonLiteralLeaf() {
		var arg = Map.of("--jc-link", "var(--jc-accent)");
		Map<String,String> none = Map.of();
		var ex = assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.schemeOverrideBlock(arg, none));
		assertTrue(ex.getMessage().contains("dark values must be literals"), ex::getMessage);
	}

	@Test void a03d_overrideBlock_rejectsBadAliasAndReservedName() {
		var arg2 = Map.of("--jc-focus", "#fff;}body{");
		Map<String,String> none = Map.of();
		assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.schemeOverrideBlock(none, arg2));
		var arg3 = Map.of("--jc-chrome-foo", "#fff");
		var ex = assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.schemeOverrideBlock(arg3, none));
		assertTrue(ex.getMessage().contains("Cannot declare reserved token '--jc-chrome-foo'"), ex::getMessage);
		var arg4 = Map.of("--jc-x", "#fff");
		var arg5 = Map.of("--jc-x", "var(--jc-accent)");
		ex = assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.schemeOverrideBlock(arg4, arg5));
		assertTrue(ex.getMessage().contains("both a leaf token and an alias"), ex::getMessage);
	}

	@Test void a04_schemeHead() throws Exception {
		var p = head("pin=dark&toggle=true");
		assertEquals("data-jc-scheme=\"dark\" data-jc-scheme-pin=\"dark\" data-jc-scheme-toggle", p[0]);
		assertEquals("<script>" + EXPECTED_SCRIPT + "</script>\n<link rel=\"stylesheet\" href=\"" + p[2] + "\">\n", p[1]);
		assertTrue(p[2].contains("juneau-theme-dark.css?v="), () -> p[2]);

		assertEquals("data-jc-scheme=\"light\" data-jc-scheme-pin=\"light\"", head("pin=light&toggle=false")[0]);
		assertEquals("data-jc-scheme-toggle", head("pin=auto&toggle=true")[0]);

		var auto = head("pin=auto&toggle=false");
		assertEquals("", auto[0]);
		assertTrue(auto[1].startsWith("<script>" + EXPECTED_SCRIPT + "</script>\n"), () -> auto[1]);

		assertThrows(IllegalArgumentException.class, () -> ConsoleChromeMixin.schemeHead(null, "dim", false));
		assertThrows(IllegalArgumentException.class, () -> ConsoleChromeMixin.schemeHead(null, null, false));
	}

	@Test void a05_schemeHead_stampsRequestNonce() throws Exception {
		var p = head("pin=auto&toggle=false&nonce=abc");
		assertTrue(p[1].startsWith("<script nonce=\"abc\">" + EXPECTED_SCRIPT + "</script>\n"), () -> p[1]);
	}
}
