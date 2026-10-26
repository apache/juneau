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
package org.apache.juneau.console.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * {@code <@token scheme="dark">}: a dark-scheme token declared inside {@code <@theme>} lands in its own
 * {@code html:root[data-jc-scheme="dark"]} rule after the light override block, holding only the declared names.
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class TokenDirective_Dark_Test extends TestBase {

	private static final String DARK_OPEN = "@media not print{html:root[data-jc-scheme=\"dark\"]{";

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/t")
		public View page(RestRequest req) {
			return FreemarkerView.of("admin/scheme-token-" + req.getQueryParam("n").asString().orElseThrow() + ".ftlh");
		}
	}

	private static String render(String name, int status) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get("/t?n=" + name).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	/** The inline {@code <style>} body of a rendered page. */
	private static String style(String body) {
		var i = body.indexOf("<style>");
		assertTrue(i >= 0, () -> body);
		return body.substring(i + "<style>".length(), body.indexOf("</style>", i));
	}

	@Test void a01_darkOnlyEmitted() throws Exception {
		var body = render("dark-only", 200);
		assertEquals(DARK_OPEN + "--jc-link:#6cb4ff;}}", style(body));
		assertFalse(body.contains("html:root{"), () -> body);
	}

	@Test void a02_mixed() throws Exception {
		var css = style(render("mixed", 200));
		assertTrue(css.startsWith("html:root{"), css);
		var light = css.substring(0, css.indexOf(DARK_OPEN));
		assertTrue(light.contains("--jc-accent:#123456;"), css);
		assertTrue(light.contains("--jc-text:"), () -> "light block is seeded from the stock palette: " + css);
		assertEquals(DARK_OPEN + "--jc-link:#6cb4ff;}}", css.substring(css.indexOf(DARK_OPEN)));
	}

	@Test void a03_varValueRejected() throws Exception {
		var body = render("var", 500);
		assertTrue(body.contains("dark values must be literals"), () -> body);
	}

	@Test void a04_aliasLive() throws Exception {
		assertEquals(DARK_OPEN + "--jc-focus:var(--jc-accent);}}", style(render("alias", 200)));
	}

	@Test void a05_unknownAliasTarget() throws Exception {
		var body = render("alias-unknown", 500);
		assertTrue(body.contains("references unknown token '--jc-nope'"), () -> body);
	}

	@Test void a05b_roleAliasTarget() throws Exception {
		assertEquals(DARK_OPEN + "--jc-focus:var(--jc-surface);}}", style(render("alias-role", 200)));
	}

	@Test void a06_reservedRejected() throws Exception {
		var body = render("reserved", 500);
		assertTrue(body.contains("Cannot declare reserved token '--jc-chrome-foo'"), () -> body);
	}

	@Test void a07_badScheme() throws Exception {
		for (var n : new String[] {"scheme-light", "scheme-x"}) {
			var body = render(n, 500);
			assertTrue(body.contains("<@token name=\"--jc-link\"> scheme= must be 'dark'"), () -> body);
		}
	}
}
