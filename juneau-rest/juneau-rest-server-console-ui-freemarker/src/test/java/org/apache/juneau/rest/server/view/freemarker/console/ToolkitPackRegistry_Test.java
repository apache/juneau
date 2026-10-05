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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ToolkitPackRegistry}: the built-in {@code "views"} pack emits CSS before JS with
 * the helpers runtime last, an empty name list resolves to nothing, an unknown name fails loud, and the
 * per-pack {@link ToolkitPackRegistry.AssetUrlResolver} seam (the same seam
 * {@code ConsoleFreemarkerMixin.Builder.registerToolkitPack} feeds) is honored.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent run() returns this.
})
class ToolkitPackRegistry_Test extends TestBase {

	// A live RestRequest so the built-in VIEWS_RESOLVER (ViewsMixin::viewAssetUrl) can resolve servlet URIs.
	private static RestRequest dummyRequest() throws Exception {
		try (var c = MockRestClient.buildLax(DummyHost.class);
			var rsp = c.get("/x").run()) {
			return DummyHost.CAPTURED.get();
		}
	}

	public static class DummyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}

	private static int indexOfContaining(List<String> urls, String needle) {
		for (var i = 0; i < urls.size(); i++)
			if (urls.get(i).contains(needle))
				return i;
		return -1;
	}

	@Test void views_order_cssThenJs_helpersLast_noPageCards() throws Exception {
		var reg = new ToolkitPackRegistry();
		var r = reg.resolve(List.of(ToolkitPackRegistry.PACK_VIEWS), dummyRequest(), true);

		// CSS: views.css then config.css.
		assertTrue(indexOfContaining(r.cssUrls(), "juneau-views.css") >= 0, () -> r.cssUrls().toString());
		assertTrue(indexOfContaining(r.cssUrls(), "juneau-config.css") >= 0, () -> r.cssUrls().toString());

		// JS load order is a contract: renders, icons, search, pagestate, ribbon, datatables glue, views, config,
		// regions, helpers LAST.
		var js = r.jsUrls();
		var order = List.of(
			"juneau-renders.js", "juneau-icons.js", "juneau-search.js", "juneau-pagestate.js", "juneau-ribbon.js",
			"juneau-datatables.js", "juneau-views.js", "juneau-config.js", "juneau-regions.js", "juneau-helpers.js");
		var prev = -1;
		for (var name : order) {
			var at = indexOfContaining(js, name);
			assertTrue(at >= 0, () -> name + " missing from " + js);
			var p = prev;
			assertTrue(at > p, () -> name + " out of order in " + js);
			prev = at;
		}
		assertTrue(js.get(js.size() - 1).contains("juneau-helpers.js"), js::toString);
		assertEquals(-1, indexOfContaining(js, "juneau-page-cards.js"), js::toString);
	}

	@Test void views_withoutServerModeTable_omitsDataTablesGlue() throws Exception {
		var reg = new ToolkitPackRegistry();
		var req = dummyRequest();
		var views = List.of(ToolkitPackRegistry.PACK_VIEWS);
		for (var r : List.of(reg.resolve(views, req), reg.resolve(views, req, false))) {
			assertEquals(-1, indexOfContaining(r.jsUrls(), "datatables"), () -> r.jsUrls().toString());
			assertTrue(indexOfContaining(r.jsUrls(), "juneau-views.js") >= 0, () -> r.jsUrls().toString());
			assertTrue(r.jsUrls().get(r.jsUrls().size() - 1).contains("juneau-helpers.js"), () -> r.jsUrls().toString());
		}
	}

	@Test void views_withServerModeTable_includesDataTablesGlue_neverTheDataTablesLibraryOrJquery() throws Exception {
		// A server-mode datatables card needs window.JuneauDataTables before juneau-views.js inits it, so the views
		// pack carries the first-party glue (cache-busted like every other asset) when the page has such a card.
		// jQuery and the DataTables library itself stay caller-provided.
		var js = new ToolkitPackRegistry().resolve(List.of(ToolkitPackRegistry.PACK_VIEWS), dummyRequest(), true).jsUrls();
		var glue = js.stream().filter(u -> u.toLowerCase().contains("datatables")).toList();
		assertSize(1, glue);
		assertContains("/juneau-datatables.js?v=", glue.get(0));
		assertTrue(indexOfContaining(js, "juneau-datatables.js") < indexOfContaining(js, "juneau-views.js"), js::toString);
		assertEquals(-1, indexOfContaining(js, "jquery"), js::toString);
	}

	@Test void calendar_isBuiltIn_resolvesThroughWidgetsMixin() throws Exception {
		// The built-in "calendar" pack (Task 14) ships the already-shipping juneau-calendar.* runtime from
		// juneau-rest-server-widgets and resolves through WidgetsMixin::widgetAssetUrl, NOT VIEWS_RESOLVER.
		var reg = new ToolkitPackRegistry();
		var r = reg.resolve(List.of(ToolkitPackRegistry.PACK_CALENDAR), dummyRequest());
		assertTrue(indexOfContaining(r.cssUrls(), "juneau-calendar.css") >= 0, () -> r.cssUrls().toString());
		assertTrue(indexOfContaining(r.jsUrls(), "juneau-calendar.js") >= 0, () -> r.jsUrls().toString());
		// The calendar pack carries ONLY the calendar assets - it does not drag in the views runtime.
		assertEquals(-1, indexOfContaining(r.jsUrls(), "juneau-views.js"), () -> r.jsUrls().toString());
	}

	@Test void viewsThenCalendar_authorOrder_viewsJsBeforeCalendarJs() throws Exception {
		// Author-listed order is a contract: toolkit="views,calendar" emits the views runtime before the
		// calendar runtime (shared layer stack), even though the two packs resolve through different mixins.
		var reg = new ToolkitPackRegistry();
		var r = reg.resolve(List.of(ToolkitPackRegistry.PACK_VIEWS, ToolkitPackRegistry.PACK_CALENDAR), dummyRequest());
		var views = indexOfContaining(r.jsUrls(), "juneau-views.js");
		var cal = indexOfContaining(r.jsUrls(), "juneau-calendar.js");
		assertTrue(views >= 0 && cal >= 0, () -> r.jsUrls().toString());
		assertTrue(views < cal, () -> r.jsUrls().toString());
	}

	@Test void omit_isEmpty() {
		var reg = new ToolkitPackRegistry();
		// An empty (or null) name list resolves to nothing and does NOT require a request.
		for (var names : Arrays.<List<String>>asList(List.of(), null)) {
			var r = reg.resolve(names, null);
			assertEmpty(r.cssUrls());
			assertEmpty(r.jsUrls());
		}
	}

	@Test void unknownPack_throws() throws Exception {
		var reg = new ToolkitPackRegistry();
		var req = dummyRequest();
		var nope = List.of("nope");
		var ex = assertThrows(IllegalArgumentException.class, () -> reg.resolve(nope, req));
		assertTrue(ex.getMessage().contains("Unknown toolkit pack"), ex::getMessage);
	}

	@Test void mixinBuilder_registerToolkitPack_isPublicSeam() throws Exception {
		// The per-pack resolver seam: an app-supplied pack resolves through ITS OWN resolver, not VIEWS_RESOLVER,
		// so a pack whose assets live in a different mixin cache-busts through that mixin.  This is the exact
		// mechanism ConsoleFreemarkerMixin.Builder.registerToolkitPack feeds.  (A non-empty name list still needs
		// a non-null request; the echo resolver here simply ignores it.)
		var reg = new ToolkitPackRegistry();
		reg.register("probe",
			List.of("/a.css"),
			List.of("/x.js", "/y.js"),
			(req, path) -> "ECHO" + path);   // an AssetUrlResolver that never touches the request
		var r = reg.resolve(List.of("probe"), dummyRequest());
		assertBean(r, "cssUrls,jsUrls", "[ECHO/a.css],[ECHO/x.js,ECHO/y.js]");
	}
}
