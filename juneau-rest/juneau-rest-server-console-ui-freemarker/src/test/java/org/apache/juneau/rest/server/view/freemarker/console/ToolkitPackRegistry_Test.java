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
import static org.apache.juneau.rest.server.view.freemarker.console.ToolkitPack.Kind.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ToolkitPackRegistry}: the built-in packs, the dependency graph, provided packs, duplicate
 * detection against the page's own assets, and the vendor/runtime split.  The per-pack
 * {@link ToolkitPackRegistry.AssetUrlResolver} seam (the same seam
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

	private static final ToolkitPackRegistry.AssetUrlResolver ECHO = (req, path) -> "ECHO" + path;

	private static ToolkitPack pack(String name, ToolkitPack.Kind kind, String js, String...deps) {
		return ToolkitPack.create(name).kind(kind).resolver(ECHO).js(js).dependsOn(deps).build();
	}

	private static List<String> files(List<String> urls) {
		return urls.stream().map(ToolkitPackRegistry::fileName).toList();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - built-in packs
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_views_runtimeOrder_helpersLast_noGlue() throws Exception {
		var r = new ToolkitPackRegistry().resolve(List.of(ToolkitPackRegistry.PACK_VIEWS), dummyRequest());
		assertList(files(r.runtimeCss()), "juneau-views.css", "juneau-config.css");
		// JS load order is a contract: renders, icons, search, pagestate, urlstate (Copy link), ribbon, views, config,
		// regions, console-output, run-view, helpers LAST.  The DataTables glue is its own pack now.
		assertList(files(r.runtimeJs()),
			"juneau-renders.js", "juneau-icons.js", "juneau-search.js", "juneau-pagestate.js", "juneau-urlstate.js",
			"juneau-ribbon.js", "juneau-views.js", "juneau-config.js", "juneau-regions.js", "juneau-console-output.js",
			"juneau-run-view.js", "juneau-helpers.js");
		assertEmpty(r.vendorCss());
		assertEmpty(r.vendorJs());
	}

	@Test void a02_calendar_isBuiltIn_resolvesThroughWidgetsMixin() throws Exception {
		var r = new ToolkitPackRegistry().resolve(List.of(ToolkitPackRegistry.PACK_CALENDAR), dummyRequest());
		assertList(files(r.runtimeCss()), "juneau-calendar.css");
		assertList(files(r.runtimeJs()), "juneau-calendar.js");
	}

	@Test void a03_viewsThenCalendar_authorOrder() throws Exception {
		var r = new ToolkitPackRegistry().resolve(List.of(ToolkitPackRegistry.PACK_VIEWS, ToolkitPackRegistry.PACK_CALENDAR), dummyRequest());
		assertTrue(indexOfContaining(r.runtimeJs(), "juneau-views.js") < indexOfContaining(r.runtimeJs(), "juneau-calendar.js"), r.runtimeJs()::toString);
	}

	@Test void a04_omit_isEmpty_needsNoRequest() {
		var reg = new ToolkitPackRegistry();
		for (var names : Arrays.<List<String>>asList(List.of(), null))
			assertBean(reg.resolve(names, null), "vendorCss,vendorJs,runtimeCss,runtimeJs", "[],[],[],[]");
	}

	@Test void a05_unknownRequested_namesSource() throws Exception {
		var reg = new ToolkitPackRegistry();
		var req = dummyRequest();
		var e = assertThrows(IllegalArgumentException.class, () -> reg.resolve(List.of("nope"), req));
		assertString("Unknown toolkit pack 'nope' (requested).", e.getMessage());
	}

	@Test void a06_threeArgRegister_isRuntime_ownResolver() throws Exception {
		var reg = new ToolkitPackRegistry();
		reg.register("probe", List.of("/a.css"), List.of("/x.js", "/y.js"), ECHO);
		assertBean(reg.resolve(List.of("probe"), dummyRequest()), "vendorCss,vendorJs,runtimeCss,runtimeJs", "[],[],[ECHO/a.css],[ECHO/x.js,ECHO/y.js]");
	}

	@Test void a07_glue_pullsDataTablesAndJquery_fromWebJars() throws Exception {
		var r = new ToolkitPackRegistry().resolve(List.of(ToolkitPackRegistry.PACK_DATATABLES_GLUE), dummyRequest());
		assertList(files(r.vendorCss()), "datatables.datatables.min.css");
		assertList(files(r.vendorJs()), "jquery.min.js", "datatables.min.js", "juneau-datatables.js");
		var jq = WebJarResolver.version("org.webjars", "jquery");
		var dt = WebJarResolver.version("org.webjars.npm", "datatables.net");
		assertContains("/webjars/jquery/" + jq + "/jquery.min.js?v=" + jq, r.vendorJs().get(0));
		assertContains("/webjars/datatables.net/" + dt + "/js/dataTables.min.js?v=" + dt, r.vendorJs().get(1));
		assertContains("/juneau-datatables.js?v=", r.vendorJs().get(2));
		assertEmpty(r.runtimeJs());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - graph, provided, kinds
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_buttons_expandsDependencies() throws Exception {
		var r = new ToolkitPackRegistry().resolve(List.of(ToolkitPackRegistry.PACK_DATATABLES_BUTTONS), dummyRequest());
		assertList(files(r.vendorJs()), "jquery.min.js", "datatables.min.js", "datatables.buttons.min.js", "buttons.html5.min.js");
		assertList(files(r.vendorCss()), "datatables.datatables.min.css", "buttons.datatables.min.css");
	}

	@Test void b02_siblingOrder_followsRoots() throws Exception {
		var reg = new ToolkitPackRegistry();
		reg.register(pack("a", VENDOR, "/a.js"));
		reg.register(pack("b", VENDOR, "/b.js", "c"));
		reg.register(pack("c", VENDOR, "/c.js"));
		var req = dummyRequest();
		assertList(reg.resolve(List.of("b", "a"), req).vendorJs(), "ECHO/c.js", "ECHO/b.js", "ECHO/a.js");
		assertList(reg.resolve(List.of("a", "b"), req).vendorJs(), "ECHO/a.js", "ECHO/c.js", "ECHO/b.js");
	}

	@Test void b03_packNamedTwice_emittedOnce() throws Exception {
		var r = new ToolkitPackRegistry().resolve(List.of(ToolkitPackRegistry.PACK_DATATABLES_GLUE, ToolkitPackRegistry.PACK_DATATABLES, ToolkitPackRegistry.PACK_DATATABLES_GLUE), dummyRequest());
		assertList(files(r.vendorJs()), "jquery.min.js", "datatables.min.js", "juneau-datatables.js");
	}

	@Test void b04_unknownDependsOn_failsAtValidate() {
		var reg = new ToolkitPackRegistry();
		reg.register(pack("x", VENDOR, "/x.js", "ghost"));
		var e = assertThrows(IllegalArgumentException.class, reg::validate);
		assertString("Unknown toolkit pack 'ghost' (dependsOn of pack 'x').", e.getMessage());
	}

	@Test void b05_unknownDependsOn_failsAtRender() throws Exception {
		var reg = new ToolkitPackRegistry();
		reg.register(pack("x", VENDOR, "/x.js", "ghost"));
		var req = dummyRequest();
		var e = assertThrows(IllegalArgumentException.class, () -> reg.resolve(List.of("x"), req));
		assertString("Unknown toolkit pack 'ghost' (dependsOn of pack 'x').", e.getMessage());
	}

	@Test void b06_cycle_failsAtValidate() {
		var reg = new ToolkitPackRegistry();
		reg.register(pack("a", VENDOR, "/a.js", "b"));
		reg.register(pack("b", VENDOR, "/b.js", "a"));
		var e = assertThrows(IllegalArgumentException.class, reg::validate);
		assertString("Toolkit pack cycle: a → b → a.", e.getMessage());
	}

	@Test void b07_cycle_failsAtRender() throws Exception {
		var reg = new ToolkitPackRegistry();
		reg.register(pack("a", VENDOR, "/a.js", "b"));
		reg.register(pack("b", VENDOR, "/b.js", "a"));
		var req = dummyRequest();
		var e = assertThrows(IllegalArgumentException.class, () -> reg.resolve(List.of("b"), req));
		assertString("Toolkit pack cycle: b → a → b.", e.getMessage());
	}

	@Test void b08_provided_contributesNothing_dependenciesStillLoad() throws Exception {
		var reg = new ToolkitPackRegistry().provide(List.of(ToolkitPackRegistry.PACK_DATATABLES)).validate();
		var r = reg.resolve(List.of(ToolkitPackRegistry.PACK_DATATABLES_GLUE), dummyRequest());
		assertList(files(r.vendorJs()), "jquery.min.js", "juneau-datatables.js");
		assertEmpty(r.vendorCss());
	}

	@Test void b09_providedJquery_unprovidedDataTables_emitsDataTablesOnly() throws Exception {
		var reg = new ToolkitPackRegistry().provide(List.of(ToolkitPackRegistry.PACK_JQUERY)).validate();
		var r = reg.resolve(List.of(ToolkitPackRegistry.PACK_DATATABLES), dummyRequest());
		assertList(files(r.vendorJs()), "datatables.min.js");
	}

	@Test void b10_unknownProvided_failsAtValidate() {
		var reg = new ToolkitPackRegistry().provide(List.of("ghost"));
		var e = assertThrows(IllegalArgumentException.class, reg::validate);
		assertString("Unknown toolkit pack 'ghost' (providedPacks).", e.getMessage());
	}

	@Test void b11_split_runtimeMayDependOnVendor() throws Exception {
		var reg = new ToolkitPackRegistry();
		reg.register(pack("v", VENDOR, "/v.js"));
		reg.register(pack("r", RUNTIME, "/r.js", "v"));
		var r = reg.validate().resolve(List.of("r"), dummyRequest());
		assertBean(r, "vendorJs,runtimeJs", "[ECHO/v.js],[ECHO/r.js]");
	}

	@Test void b12_vendorDependingOnRuntime_failsAtValidate() {
		var reg = new ToolkitPackRegistry();
		reg.register(pack("v2", VENDOR, "/v2.js", ToolkitPackRegistry.PACK_VIEWS));
		var e = assertThrows(IllegalArgumentException.class, reg::validate);
		assertString("VENDOR toolkit pack 'v2' cannot depend on RUNTIME pack 'views'.", e.getMessage());
	}

	@Test void b13_replacement_wins_andDropsOldDependsOn() throws Exception {
		var reg = new ToolkitPackRegistry();
		reg.register(pack(ToolkitPackRegistry.PACK_DATATABLES, VENDOR, "/app/dt.js"));
		var r = reg.validate().resolve(List.of(ToolkitPackRegistry.PACK_DATATABLES_GLUE), dummyRequest());
		assertList(files(r.vendorJs()), "dt.js", "juneau-datatables.js");
		assertEmpty(r.vendorCss());
	}

	@Test void b14_requireKnown_namesSource() {
		var reg = new ToolkitPackRegistry();
		reg.requireKnown(List.of(ToolkitPackRegistry.PACK_VIEWS), "<@page toolkit=>");
		var e = assertThrows(IllegalArgumentException.class, () -> reg.requireKnown(List.of("views", "nope"), "<@page toolkit=>"));
		assertString("Unknown toolkit pack 'nope' (<@page toolkit=>).", e.getMessage());
	}

	@Test void b15_resolverFailure_namesPack() throws Exception {
		var reg = new ToolkitPackRegistry();
		reg.register(ToolkitPack.create("p").kind(VENDOR).js("x").resolver((req, path) -> { throw new IllegalStateException("needs org.example:p on the classpath."); }).build());
		var req = dummyRequest();
		var e = assertThrows(IllegalStateException.class, () -> reg.resolve(List.of("p"), req));
		assertString("Pack 'p' needs org.example:p on the classpath.", e.getMessage());
	}

	@Test void b16_builtIns_validate() {
		assertNotNull(new ToolkitPackRegistry().validate());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - duplicates against the page's own init=/css=
	//-----------------------------------------------------------------------------------------------------------------

	private static final class Capture extends Handler {
		final List<String> messages = new ArrayList<>();
		@Override public void publish(LogRecord r) { messages.add(r.getLevel() + ": " + r.getMessage()); }
		@Override public void flush() { /* no-op */ }
		@Override public void close() { /* no-op */ }
	}

	private static List<String> warnings(Runnable r) {
		var log = Logger.getLogger(ToolkitPackRegistry.class.getName());
		var cap = new Capture();
		log.addHandler(cap);
		try {
			r.run();
		} finally {
			log.removeHandler(cap);
		}
		return cap.messages;
	}

	@Test void c01_initDuplicate_skipped_andWarned() throws Exception {
		var reg = new ToolkitPackRegistry();
		var req = dummyRequest();
		var out = new ToolkitPackRegistry.Resolved[1];
		var msgs = warnings(() -> out[0] = reg.resolve(List.of(ToolkitPackRegistry.PACK_DATATABLES_GLUE), req, List.of("/app/static/JQuery.Min.JS?x=1"), "admin/releases.ftlh"));
		assertList(files(out[0].vendorJs()), "datatables.min.js", "juneau-datatables.js");
		assertSize(1, msgs);
		assertMatchesGlob("WARNING: Template 'admin/releases.ftlh': skipped pack 'jquery' asset */webjars/jquery/*/jquery.min.js?v=*; the page already loads /app/static/JQuery.Min.JS?x=1.", msgs.get(0));
	}

	@Test void c02_cssDuplicate_skipped() throws Exception {
		var r = new ToolkitPackRegistry().resolve(List.of(ToolkitPackRegistry.PACK_DATATABLES), dummyRequest(), List.of("https://cdn.example/dataTables.dataTables.min.css"), "t.ftlh");
		assertEmpty(r.vendorCss());
	}

	@Test void c03_fileName_lastSegment_noQueryOrFragment_lowerCase() {
		assertString("jquery.min.js", ToolkitPackRegistry.fileName("http://h/a/JQuery.min.js?v=1#f"));
		assertString("a.css", ToolkitPackRegistry.fileName("a.css"));
	}
}
