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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * Nested-path console chrome CSS. A relative or page-prefixed {@code juneau-console/chrome.css}
 * href on a page at {@code /rest/setup} 404s (the browser asks for
 * {@code /rest/setup/juneau-console/...}) and the page paints with no chrome. Root-absolute
 * {@code /juneau-console/chrome.css} is 200 and the chrome paints.
 *
 * <p>
 * This is an HTTP render of a real {@code <@console>} page, not a string-contains check on the
 * href (that substring also matches the buggy {@code /rest/setup/juneau-console/chrome.css}
 * form). Juneau has no Playwright e2e suite; this is the closest existing harness
 * ({@link MockRestClient} against the console-ui-freemarker module).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S8786", // Test-only patterns scan small rendered pages; the scan-to-attribute shape needs backtracking.
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class ConsoleDirective_ChromeAssetUrls_Test extends TestBase {

	private static final Pattern STYLESHEET_HREF = Pattern.compile(
		"<link\\s[^>]*rel=\"stylesheet\"[^>]*href=\"([^\"]+)\"",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern THEME_PATH = Pattern.compile(
		"^/juneau-console/themes/juneau-theme-[a-z0-9-]+\\.css$"
	);

	@Rest(mixins={ FreemarkerMixin.class, ConsoleChromeMixin.class }, renderResponseStackTraces="true")
	public static class NestedPageHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-pagetabs.ftlh")
				.build();
		}
		@RestGet(path="/rest/setup")
		public View setup() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Test void a01_nestedPage_chromeAndThemeHrefsAreSiteRootAndFetchable() throws Exception {
		try (var c = MockRestClient.buildLax(NestedPageHost.class);
			var page = c.get("/rest/setup").run()) {
			page.assertStatus(200);
			var html = page.getContent().asString();
			var pageUri = URI.create("http://localhost/rest/setup");

			var chromeHref = requiredHref(html, "chrome.css");
			var themeHref = requiredHref(html, "juneau-theme-");
			var chromePath = resolvedPath(pageUri, chromeHref);
			var themePath = resolvedPath(pageUri, themeHref);

			assertEquals("/juneau-console/chrome.css", chromePath,
				() -> "chrome.css must be site-root, not under the page path; href=" + chromeHref);
			assertTrue(THEME_PATH.matcher(themePath).matches(),
				() -> "theme CSS must be site-root, not under the page path; href=" + themeHref + " path=" + themePath);

			try (var chrome = c.get(chromePath).run()) {
				chrome.assertStatus(200).assertHeader("Content-Type").isContains("text/css");
				var css = chrome.getContent().asString();
				assertTrue(css.contains(".jc-header"), () -> css);
				assertTrue(css.contains(".juneau-page-nav"), () -> css);
			}
			try (var theme = c.get(themePath).run()) {
				theme.assertStatus(200).assertHeader("Content-Type").isContains("text/css");
			}

			// The shell script is site-root too, and served.
			var m = Pattern.compile("<script src=\"([^\"]*juneau-console\\.js[^\"]*)\"").matcher(html);
			assertTrue(m.find(), () -> html);
			var shellPath = resolvedPath(pageUri, m.group(1));
			assertEquals("/juneau-console/juneau-console.js", shellPath, () -> html);
			try (var shell = c.get(shellPath).run()) {
				shell.assertStatus(200).assertHeader("Content-Type").isContains("javascript");
			}

			// Home, Items and Admin are three top-level nav entries in the contract.
			assertPage(html).isValid().hasNavHref("home", "/home").hasNavHref("items", "/items").hasNavHref("admin", "/admin");
			assertEquals(3, assertPage(html).contract().getList("nav").size(), () -> html);
			assertFalse(html.contains("slds-"), () -> html);
		}
	}

	static String requiredHref(String html, String needle) {
		for (var href : stylesheetHrefs(html)) {
			if (href.contains(needle) && href.contains("juneau-console"))
				return href;
		}
		fail("no juneau-console stylesheet href containing '" + needle + "' in:\n" + html);
		return "";
	}

	static List<String> stylesheetHrefs(String html) {
		var out = new ArrayList<String>();
		var m = STYLESHEET_HREF.matcher(html);
		while (m.find())
			out.add(m.group(1));
		return out;
	}

	static String resolvedPath(URI pageUri, String href) {
		var resolved = pageUri.resolve(href);
		var path = resolved.getPath();
		return path == null ? "" : path;
	}
}
