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
import org.apache.juneau.console.*;
import org.apache.juneau.console.views.*;
import org.junit.jupiter.api.*;

/**
 * Directive emission of the client object model scripts and the inline-handler CSP check: the model script follows
 * the bus in the runtime pack, the inline compiler is a separate tag only for a page that opts in, and an opted-in
 * page fails fast under a response CSP that lacks {@code 'unsafe-eval'}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class InlineHandlerCsp_Test extends TestBase {

	@Rest(mixins={FreemarkerMixin.class, ViewsMixin.class}, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return ConsoleFreemarkerMixin.create().basePath("/templates/").build(); }

		private static PageSpec spec(boolean inline) {
			var s = PageSpec.create().template("admin/theme-nameless.ftlh").toolkit("views");
			return inline ? s.allowInlineHandlers(true).on("loaded", HandlerRef.inline("return 1")) : s;
		}
		@RestGet(path="/plain") public View plain(RestRequest req) { return spec(false).view(req); }
		@RestGet(path="/inline") public View inline(RestRequest req) { return spec(true).view(req); }
		@RestGet(path="/csp-strict") public View cspStrict(RestRequest req) {
			req.getResponse().setHeader("Content-Security-Policy", "script-src 'self'");
			return spec(true).view(req);
		}
		@RestGet(path="/csp-eval") public View cspEval(RestRequest req) {
			req.getResponse().setHeader("Content-Security-Policy", "script-src 'self' 'unsafe-eval'");
			return spec(true).view(req);
		}
		@RestGet(path="/csp-default") public View cspDefault(RestRequest req) {
			req.getResponse().setHeader("Content-Security-Policy", "default-src 'self'");
			return spec(true).view(req);
		}
		@RestGet(path="/csp-default-eval") public View cspDefaultEval(RestRequest req) {
			req.getResponse().setHeader("Content-Security-Policy", "default-src 'self' 'unsafe-eval'");
			return spec(true).view(req);
		}
		@RestGet(path="/csp-multi") public View cspMulti(RestRequest req) {
			req.getResponse().addHeader("Content-Security-Policy", "script-src 'self' 'unsafe-eval'");
			req.getResponse().addHeader("Content-Security-Policy", "script-src 'self'");
			return spec(true).view(req);
		}
		@RestGet(path="/csp-eval-mixed-case") public View cspEvalMixedCase(RestRequest req) {
			req.getResponse().setHeader("Content-Security-Policy", "script-src 'self' 'Unsafe-Eval'");
			return spec(true).view(req);
		}
		@RestGet(path="/late-table-inline") public View lateTableInline(RestRequest req) {
			var t = TableSpec.create("t1").dataUrl("/api/t").columns(Column.create("id"));
			var page = spec(false).table(t, (java.util.function.Consumer<CardSpec>)null);
			t.on("loaded", HandlerRef.inline("return 1"));   // added after table(...) captured the table
			return page.view(req);
		}
		@RestGet(path="/csp-elem-only") public View cspElemOnly(RestRequest req) {
			req.getResponse().setHeader("Content-Security-Policy", "script-src 'self' 'unsafe-eval'; script-src-elem 'self'");
			return spec(true).view(req);
		}
	}

	private static String get(String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	@Test void a01_modelJs_followsBus() throws Exception {
		var b = get("/plain", 200);
		var bus = b.indexOf("juneau-bus.js");
		var model = b.indexOf("juneau-model.js");
		assertTrue(bus >= 0 && model > bus, () -> b);
		assertTrue(model < b.indexOf("juneau-renders.js"), () -> b);
	}

	@Test void a02_notOptedIn_noInlineFile() throws Exception {
		assertFalse(get("/plain", 200).contains("juneau-model-inline.js"));
	}

	@Test void a03_optIn_inlineAfterModel() throws Exception {
		var b = get("/inline", 200);
		assertTrue(b.indexOf("juneau-model-inline.js") > b.indexOf("juneau-model.js"), () -> b);
		assertTrue(b.contains("\"allowInlineHandlers\":true"), () -> b);
	}

	@Test void b01_strictCsp_throws() throws Exception {
		var b = get("/csp-strict", 500);
		assertTrue(b.contains("allowInlineHandlers(true) needs 'unsafe-eval' in script-src; the response CSP is: "), () -> b);
	}

	@Test void b02_unsafeEval_renders() throws Exception {
		assertTrue(get("/csp-eval", 200).contains("juneau-model-inline.js"));
	}

	@Test void b03_defaultSrcFallback_throws() throws Exception {
		assertTrue(get("/csp-default", 500).contains("needs 'unsafe-eval'"));
	}

	@Test void b04_defaultSrcWithEval_renders() throws Exception {
		assertTrue(get("/csp-default-eval", 200).contains("juneau-model-inline.js"));
	}

	@Test void b05_scriptSrcElemIgnored() throws Exception {
		assertTrue(get("/csp-elem-only", 200).contains("juneau-model-inline.js"));
	}

	@Test void b06_everyPolicyChecked() throws Exception {
		assertTrue(get("/csp-multi", 500).contains("needs 'unsafe-eval'"));
	}

	@Test void b07_policyKeywordsAreCaseInsensitive() throws Exception {
		assertTrue(get("/csp-eval-mixed-case", 200).contains("juneau-model-inline.js"));
	}

	@Test void c01_inlineHandlerAddedToTableAfterTheCall_stillNeedsOptIn() throws Exception {
		assertTrue(get("/late-table-inline", 500).contains("allowInlineHandlers(true)"));
	}
}
