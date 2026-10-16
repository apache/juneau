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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * A console page rendered behind a {@link CsrfBoundary} carries the token, so no cache may keep it.
 *
 * <p>
 * The mock REST client never runs a servlet filter, so the page resource calls
 * {@link RequestBoundaryFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse,
 * jakarta.servlet.FilterChain) doFilter} on the real filter itself, with a no-op chain, before it renders.  That
 * publishes the same request attributes a deployed filter would.  What this proves is the page's own response
 * headers; whether the filter blocks anything is proven elsewhere, over a mocked chain and a real socket.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class CsrfPage_Test extends TestBase {

	static final CsrfBoundary BOUNDARY = CsrfBoundary.create().build();

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class PageHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome.ftlh")
				.build();
		}
		@RestGet(path="/page")
		public View page(RestRequest req, RestResponse res) throws Exception {
			// An application default that a token-bearing page must not inherit.
			res.setHeader("Cache-Control", "public, max-age=600");
			new RequestBoundaryFilter(BOUNDARY).doFilter(req.getHttpServletRequest(), res.getHttpServletResponse(), (q, s) -> {});
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Test void a04_tokenBearingPageNeverCacheable() throws Exception {
		try (var c = MockRestClient.buildLax(PageHost.class);
			var rsp = c.get("/page").header("Accept", "text/html").header("Host", "app.example").run()) {
			rsp.assertStatus(200);
			var body = rsp.getContent().asString();
			assertTrue(body.matches("(?s).*<meta name=\"csrf-token\" content=\"[0-9a-f]{128}\".*"), body);
			assertTrue(body.matches("(?s).* data-juneau-csrf=\"[0-9a-f]{128}\".*"), body);
			var cacheControl = rsp.getHeaders("Cache-Control");
			assertEquals(1, cacheControl.length, () -> java.util.Arrays.toString(cacheControl));
			assertEquals("private, no-store", cacheControl[0].getValue());
			assertFalse(cacheControl[0].getValue().contains("public"));
			assertEquals(0, rsp.getHeaders("Expires").length);
		}
	}
}
