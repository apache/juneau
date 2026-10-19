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

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * Shared host for the C1 fixtures under {@code templates/c1/}.
 *
 * <p>
 * {@code GET /t/{name}} renders {@code c1/{name}.ftlh}. Pages that use {@code <@page>} render under
 * {@code c1/chrome.ftlh} ({@link Host}) or {@code c1/chrome-selected.ftlh} ({@link SelectedHost}). Fixtures whose
 * name starts with {@code csrf-} get the loopback CSRF attributes.
 */
@SuppressWarnings({
	"resource" // The static MockRestClient HOST/SELECTED fixtures live for the whole test run and are never closed
})
final class C1Fixtures {

	private C1Fixtures() {}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("c1/chrome.ftlh").build();
		}
		@RestGet(path="/t/{name}")
		public View t(@Path("name") String name, RestRequest req) {
			return C1Fixtures.view(name, req);
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class SelectedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("c1/chrome-selected.ftlh").build();
		}
		@RestGet(path="/t/{name}")
		public View t(@Path("name") String name, RestRequest req) {
			return C1Fixtures.view(name, req);
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class KpiHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("c1/chrome.ftlh").cardType(new KpiCardType()).build();
		}
		@RestGet(path="/t/{name}")
		public View t(@Path("name") String name, RestRequest req) {
			return C1Fixtures.view(name, req);
		}
	}

	static View view(String name, RestRequest req) {
		if (name.startsWith("csrf-")) {
			req.setAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, "tok-123");
			req.setAttribute(LoopbackBoundaryFilter.HEADER_ATTRIBUTE, "X-Csrf-Token");
		}
		return FreemarkerView.of("c1/" + name + ".ftlh");
	}

	private static final MockRestClient HOST = MockRestClient.buildLax(Host.class);
	private static final MockRestClient SELECTED = MockRestClient.buildLax(SelectedHost.class);
	private static final MockRestClient KPI = MockRestClient.buildLax(KpiHost.class);

	/** Renders {@code c1/{name}.ftlh} under {@code c1/chrome.ftlh} and expects HTTP 200. */
	static String render(String name) {
		return get(HOST, name, 200);
	}

	/** Renders {@code c1/{name}.ftlh} under {@code c1/chrome-selected.ftlh} and expects HTTP 200. */
	static String renderSelected(String name) {
		return get(SELECTED, name, 200);
	}

	/** Renders {@code c1/{name}.ftlh} on a host whose mixin registered {@link KpiCardType} and expects HTTP 200. */
	static String renderKpi(String name) {
		return get(KPI, name, 200);
	}

	/** Renders {@code c1/{name}.ftlh} and expects HTTP 500; returns the error body. */
	static String renderError(String name) {
		return get(HOST, name, 500);
	}

	/** Asserts the 500 body for {@code name} contains {@code message} verbatim. */
	static void assertError(String name, String message) {
		var body = renderError(name);
		assertTrue(body.contains(message), () -> "expected '" + message + "' in:\n" + body);
	}

	private static String get(MockRestClient c, String name, int status) {
		try (var rsp = c.get("/t/" + name).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		} catch (Exception e) {
			throw new AssertionError("GET /t/" + name + ": " + e.getMessage(), e);
		}
	}
}
