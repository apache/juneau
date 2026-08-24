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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Serve-time {@code $FV} resolution through the {@link ViewTable} emit path (W1): resolved chrome in painted
 * {@code <th>} and the VIEW_META sidecar, XSS/escaping, per-response isolation, and expand-JSON data staying
 * unresolved.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class ServerValuesResolution_Test extends TestBase {

	private static final String XSS = "<script>alert(1)</script> a&b \"q\"";

	/** A shared view instance so the per-response restore (no permanent overwrite of author templates) is exercised. */
	static final ViewDef VIEW = ViewDef.create("releases")
		.columns(
			Column.of("count").title("Failures ($FV{failedCount})"),
			Column.of("bad").title("$FV{xss}"),
			Column.of("miss").title("$FV{missing,Fallback}"),
			Column.of("plain").title("Plain"))
		.serverValues(ServerValues.create()
			.value("failedCount", s -> s.getBean(RestRequest.class).map(r -> r.getQueryParam("n").orElse("0")).orElse("0"))
			.value("xss", s -> XSS))
		.build();

	static ViewDef fixedView(String id, int count) {
		return ViewDef.create(id)
			.columns(Column.of("count").title("Failures ($FV{failedCount})"))
			.serverValues(ServerValues.create().value("failedCount", s -> count))
			.build();
	}

	@Rest(mixins=ViewsMixin.class)
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public VarResolver varResolver(VarResolver.Builder b) {
			return b.vars(ServerValuesVar.class).build();
		}

		@RestGet(path="/view") public HttpResource view(RestRequest req) {
			return html(Html.of(ViewTable.of(req, VIEW)));
		}

		@RestGet(path="/two") public HttpResource two(RestRequest req) {
			return html(Html.of(div(ViewTable.of(req, fixedView("a", 1)), ViewTable.of(req, fixedView("b", 2)))));
		}

		@RestGet(path="/rows") public HttpResource rows(RestRequest req) {
			var data = List.<Object>of(map("count", "$FV{failedCount}", "bad", "x", "miss", "y", "plain", "z"));
			return html(Html.of(ViewTable.of(req, VIEW, data)));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(Host.class);

	private static String body(String path) throws Exception {
		return c.get(path).run().assertStatus(200).getContent().asString();
	}

	/** Extracts the raw text between the named sidecar's opening and closing {@code <script>} tags. */
	private static String sidecarBody(String html, String viewId) {
		var open = html.indexOf("id=\"juneau-view:" + viewId + "\"");
		assertTrue(open >= 0, () -> "sidecar script tag not found:\n" + html);
		var contentStart = html.indexOf('>', open) + 1;
		var contentEnd = html.indexOf("</script>", contentStart);
		return html.substring(contentStart, contentEnd);
	}

	@Test void a01_resolvedScalar_inPaintedTh() throws Exception {
		var html = body("/view?n=7");
		assertTrue(html.contains("Failures (7)"), () -> html);
		assertFalse(html.contains("$FV{failedCount}"), () -> html);
	}

	@Test void a02_missingName_usesDefaultInChrome() throws Exception {
		var html = body("/view?n=7");
		assertTrue(html.contains("Fallback"), () -> html);
		assertFalse(html.contains("$FV{missing"), () -> html);
	}

	@Test void a03_xss_elementText_serializerEncoded() throws Exception {
		var html = body("/view?n=7");
		// No raw injection anywhere (painted th is entity-encoded; sidecar is escapeForScript-neutralized).
		assertFalse(html.contains("<script>alert(1)"), () -> html);
		// Entity-encoded element text, and NOT double-encoded.
		assertTrue(html.contains("&lt;script&gt;alert(1)"), () -> html);
		assertFalse(html.contains("&amp;lt;script"), () -> html);
	}

	@Test void a04_viewMeta_carriesResolvedChrome() throws Exception {
		var sidecar = sidecarBody(body("/view?n=7"), "releases");
		assertTrue(sidecar.contains("Failures (7)"), () -> sidecar);
		assertTrue(sidecar.contains("Fallback"), () -> sidecar);
		assertFalse(sidecar.contains("$FV{"), () -> sidecar);
	}

	@Test void a05_viewMeta_scriptBreakoutNeutralized() throws Exception {
		var sidecar = sidecarBody(body("/view?n=7"), "releases");
		// The resolved XSS title lands in the raw-text sidecar; escapeForScript must neutralize the </script> breakout.
		assertTrue(sidecar.contains("alert(1)"), () -> sidecar);
		assertFalse(sidecar.contains("</script>"), () -> sidecar);
	}

	@Test void a06_twoWidgets_oneResponse_noLeak() throws Exception {
		var html = body("/two");
		assertTrue(html.contains("Failures (1)"), () -> html);
		assertTrue(html.contains("Failures (2)"), () -> html);
	}

	@Test void a07_twoSequentialRequests_perResponseSnapshot() throws Exception {
		assertTrue(body("/view?n=3").contains("Failures (3)"));
		// The shared VIEW instance must have been restored after the first response; the second resolves freshly.
		var html = body("/view?n=9");
		assertTrue(html.contains("Failures (9)"), () -> html);
		assertFalse(html.contains("Failures (3)"), () -> html);
	}

	@Test void a08_expandData_notResolved() throws Exception {
		// A row cell that literally contains a $FV token is row data, never passed through VarResolver.
		var html = body("/rows?n=7");
		assertTrue(html.contains("$FV{failedCount}"), () -> html);
	}
}
