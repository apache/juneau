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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * End-to-end server wiring smoke test for {@link PageTable} + {@link ViewsMixin} (TODO-399 Phase C, Task 9).
 *
 * <p>
 * Proves a REST resource composing {@link ViewsMixin} and returning a {@code PageTable.of(...)} response emits a
 * well-formed page: the {@code [data-juneau-page]} shell, every child view's marker table + VIEW_META sidecar, the
 * PAGE_META sidecar, and (via {@code ViewsMixin}) the asset links a real page would load &mdash; including the new
 * opt-in {@code juneau-pages.js} alongside the existing {@code juneau-views.js}/{@code juneau-ribbon.js}/
 * {@code juneau-renders.js}/{@code juneau-views.css} (design doc §"Client page runtime"; mirrors
 * {@code ViewServerWiring_Test}/{@code ViewsMixin_Serving_Test}'s end-to-end coverage style).
 */
class PageServerWiring_Test extends TestBase {

	public static class Release {
		public String name;
		public String status;
	}

	private static ViewDef view(String id) {
		return ViewDef.create(id)
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/" + id + "/data")
			.columns(Column.of("name").title("Name"), Column.of("status").title("Status"))
			.build();
	}

	private static PageDef buildAdminPage() {
		return PageDef.create("admin")
			.title("Admin")
			.tabs(
				Tab.create("releases", "Releases").view(view("releases")),
				Tab.create("catalog", "Catalog").subtabs(
					Subtab.create("packages", "Packages").view(view("packages")),
					Subtab.create("bundles", "Bundles").view(view("bundles"))))
			.build();
	}

	@Rest(mixins=ViewsMixin.class)
	public static class PageHost extends org.apache.juneau.rest.server.servlet.BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/admin") public HttpResource adminPage() {
			var markup = Html.of(PageTable.of(buildAdminPage()));
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(StandardCharsets.UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(PageHost.class);

	private static String sidecarBody(String html, String sidecarId) {
		var open = html.indexOf("id=\"" + sidecarId + "\"");
		assertTrue(open >= 0, () -> "sidecar '" + sidecarId + "' not found:\n" + html);
		var contentStart = html.indexOf('>', open) + 1;
		var contentEnd = html.indexOf("</script>", contentStart);
		return html.substring(contentStart, contentEnd);
	}

	@Test void a01_pageEndpointEmitsShellAndAllChildViews() throws Exception {
		var html = c.get("/admin").accept("text/html").run().assertStatus(200).getContent().asString();
		assertTrue(html.contains("data-juneau-page=\"admin\""), html);
		for (var id : List.of("releases", "packages", "bundles")) {
			assertTrue(html.contains("data-juneau-view=\"" + id + "\""), html);
			assertTrue(html.contains("id=\"juneau-view:" + id + "\""), html);
		}
		assertTrue(html.contains("id=\"juneau-page:admin\""), html);
	}

	@Test void a02_pageMetaRoundTripsTabTree() throws Exception {
		var html = c.get("/admin").accept("text/html").run().assertStatus(200).getContent().asString();
		var body = sidecarBody(html, "juneau-page:admin");
		var meta = Json.to(body, Map.class);
		assertEquals("admin", meta.get("id"));
		assertEquals(PageDef.CONTRACT_VERSION, meta.get("contractVersion"));
		var tabs = (List<?>) meta.get("tabs");
		assertEquals(2, tabs.size());
	}

	@Test void a03_childViewSidecarsRoundTripToTheirOwnViewDef() throws Exception {
		var html = c.get("/admin").accept("text/html").run().assertStatus(200).getContent().asString();
		var body = sidecarBody(html, "juneau-view:releases");
		var expected = Json.to(Json.of(view("releases")), Map.class);
		var actual = Json.to(body, Map.class);
		assertEquals(expected, actual, body);
	}

	@Test void a04_repeatedRequests_areByteIdentical() throws Exception {
		var html1 = c.get("/admin").accept("text/html").run().assertStatus(200).getContent().asString();
		var html2 = c.get("/admin").accept("text/html").run().assertStatus(200).getContent().asString();
		assertEquals(html1, html2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// The host also exposes the full asset set a real page would load (ViewsMixin, including the new pages.js).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_hostServesAllFiveAssetsIncludingPagesJs() throws Exception {
		for (var path : List.of(
				ViewsMixin.VIEWS_JS_PATH, ViewsMixin.RIBBON_JS_PATH, ViewsMixin.RENDERS_JS_PATH,
				ViewsMixin.VIEWS_CSS_PATH, ViewsMixin.PAGES_JS_PATH))
			c.get(path).run().assertStatus(200);
	}
}
