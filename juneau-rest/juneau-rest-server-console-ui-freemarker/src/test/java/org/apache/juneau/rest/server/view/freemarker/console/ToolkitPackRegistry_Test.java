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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ToolkitPackRegistry}: the built-in {@code "views"} pack emits CSS before JS with
 * the page-cards runtime last, an empty name list resolves to nothing, an unknown name fails loud, and the
 * per-pack {@link ToolkitPackRegistry.AssetUrlResolver} seam (the same seam
 * {@code ConsoleFreemarkerMixin.Builder.registerToolkitPack} feeds) is honored.
 *
 * @since 10.0.0
 */
class ToolkitPackRegistry_Test extends TestBase {

	// A live RestRequest so the built-in VIEWS_RESOLVER (ViewsMixin::viewAssetUrl) can resolve servlet URIs.
	private static RestRequest dummyRequest() throws Exception {
		var c = MockRestClient.buildLax(DummyHost.class);
		c.get("/x").run();
		return DummyHost.CAPTURED.get();
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

	@Test void views_order_cssThenJs_pageCardsLast() throws Exception {
		var reg = new ToolkitPackRegistry();
		var r = reg.resolve(List.of(ToolkitPackRegistry.PACK_VIEWS), dummyRequest());

		// CSS: views.css then config.css.
		assertTrue(indexOfContaining(r.cssUrls(), "juneau-views.css") >= 0, () -> r.cssUrls().toString());
		assertTrue(indexOfContaining(r.cssUrls(), "juneau-config.css") >= 0, () -> r.cssUrls().toString());

		// JS load order is a contract: renders, icons, ribbon, views, config, regions, helpers, page-cards LAST.
		var js = r.jsUrls();
		var order = List.of(
			"juneau-renders.js", "juneau-icons.js", "juneau-ribbon.js", "juneau-views.js",
			"juneau-config.js", "juneau-regions.js", "juneau-helpers.js", "juneau-page-cards.js");
		var prev = -1;
		for (var name : order) {
			var at = indexOfContaining(js, name);
			assertTrue(at >= 0, () -> name + " missing from " + js);
			var p = prev;
			assertTrue(at > p, () -> name + " out of order in " + js);
			prev = at;
		}
		// page-cards is the very last JS entry.
		assertTrue(js.get(js.size() - 1).contains("juneau-page-cards.js"), () -> js.toString());
		// No datatables / jQuery in the first-party pack.
		assertEquals(-1, indexOfContaining(js, "datatables"), () -> js.toString());
		assertEquals(-1, indexOfContaining(js, "jquery"), () -> js.toString());
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
			assertTrue(r.cssUrls().isEmpty(), () -> "" + r.cssUrls());
			assertTrue(r.jsUrls().isEmpty(), () -> "" + r.jsUrls());
		}
	}

	@Test void unknownPack_throws() throws Exception {
		var reg = new ToolkitPackRegistry();
		var req = dummyRequest();
		var ex = assertThrows(IllegalArgumentException.class, () -> reg.resolve(List.of("nope"), req));
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
		assertEquals(List.of("ECHO/a.css"), r.cssUrls());
		assertEquals(List.of("ECHO/x.js", "ECHO/y.js"), r.jsUrls());
	}
}
