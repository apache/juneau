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
 * Tests that {@code <@page init=>} / {@code css=>} reject relative entries, in both the directive and the validator.
 */
class PageAssetsAbsolute_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome.ftlh")
				.build();
		}
		@RestGet(path="/rel-init")
		public View relInit() {
			return FreemarkerView.of("admin/page-rel-init.ftlh");
		}
		@RestGet(path="/rel-css")
		public View relCss() {
			return FreemarkerView.of("admin/page-rel-css.ftlh");
		}
		@RestGet(path="/uris")
		public View uris() {
			return FreemarkerView.of("admin/page-uri-assets.ftlh");
		}
	}

	private static String get(String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get(path).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	@Test void a01_directive_relativeInit_isRejected() throws Exception {
		var body = get("/rel-init", 500);
		assertTrue(body.contains("<@page> init= entry 'one.js' must be absolute (start with '/') or a context: / servlet: URI."), () -> body);
	}

	@Test void a02_directive_relativeCss_isRejected() throws Exception {
		var body = get("/rel-css", 500);
		assertTrue(body.contains("<@page> css= entry 'rel.css' must be absolute (start with '/') or a context: / servlet: URI."), () -> body);
	}

	@Test void a03_directive_absoluteAndUriForms_areAccepted() throws Exception {
		var body = get("/uris", 200);
		assertTrue(body.contains("href=\"/abs.css\"") && body.contains("src=\"/abs.js\""), () -> body);
		assertTrue(body.contains("ctx.css") && body.contains("srv.css") && body.contains("ctx.js") && body.contains("srv.js"), () -> body);
		assertFalse(body.contains("context:/") || body.contains("servlet:/"), () -> body);
	}

	private static String rules(String source) {
		return ConsoleTemplateValidator.create().validateSource("t.ftlh", source).toString();
	}

	@Test void b01_validator_relativeInit_isFlagged() {
		var f = ConsoleTemplateValidator.create().validateSource("t.ftlh", "<@page init=\"one.js\"></@page>");
		assertTrue(f.stream().anyMatch(x -> x.rule().equals("relative-page-asset") && x.message().equals(
			"<@page> init= entry 'one.js' must be absolute (start with '/') or a context: / servlet: URI.")), () -> f.toString());
	}

	@Test void b02_validator_relativeCssInList_isFlagged() {
		var f = ConsoleTemplateValidator.create().validateSource("t.ftlh", "<@page css=[\"/ok.css\", \"rel.css\", \"//cdn/x.css\"]></@page>");
		assertEquals(2, f.stream().filter(x -> x.rule().equals("relative-page-asset")).count(), () -> f.toString());
	}

	@Test void b03_validator_absoluteAndUriForms_areClean() {
		var s = rules("<@page css=[\"/a.css\", \"context:/b.css\", \"servlet:/c.css\"] init=\"/a.js, servlet:/b.js\"></@page>");
		assertFalse(s.contains("relative-page-asset"), () -> s);
	}

	@Test void b04_validator_expressionEntries_areSkipped() {
		var s = rules("<@page css=assetUrl(\"/c.css\") init=[assetUrl(\"/a.js\")]></@page>");
		assertFalse(s.contains("relative-page-asset"), () -> s);
	}
}
