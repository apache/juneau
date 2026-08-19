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
package org.apache.juneau.rest.server.view.freemarker.console.datatables;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.junit.jupiter.api.*;

/**
 * Ticket 361 Phase 7 gate: the {@code <@datatable>} macro (this module's only deliverable) &mdash; a golden-HTML
 * integration proof that a row bean's {@code @Html(render=TagHtmlRender.class)} enum property (Phase 4) renders as
 * pill markup (Phase 6's now-render-aware {@code DataTablesTable}) nested inside a {@code jc-table} through the
 * same trusted-HTML adapter Phase 5 built for {@code <@tag>}.
 */
@SuppressWarnings({
	"resource" // Test-fixture AutoCloseables are managed by the test lifecycle, not real leaks (mixed-module resource analysis on test code).
})
class ConsoleDataTablesFreemarkerMixin_Test extends TestBase {

	@TagDomain(domain="status")
	public enum Release { RELEASED, DRAFT }

	/** Row bean: one plain property, one {@code @Html(render=TagHtmlRender.class)}-annotated enum property. */
	public static class ReleaseRow {
		public String name = "widget";
		@Html(render=TagHtmlRender.class) public Release status = Release.RELEASED;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RED control: a host wired with plain ConsoleFreemarkerMixin (the datatable macro's backing shared variable is
	// never registered) -- the reserved datatable.ftlh template itself DOES resolve (same classpath-root loader
	// ConsoleFreemarkerMixin already splices in sees every module's resources), but calling <@datatable> inside it
	// fails because jcDataTableHtml is undefined. Proves the gap is real: neither the macro's OWN registration nor
	// the Phase-6-updated DataTablesTable overload it depends on are wired without ConsoleDataTablesFreemarkerMixin.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class)
	public static class PlainControlHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").build();
		}
		@RestGet(path="/releases")
		public View releases() {
			return FreemarkerView.of("admin/releases.ftlh")
				.attr("releases", List.of(new ReleaseRow()))
				.attr("rowTypeName", ReleaseRow.class.getName());
		}
	}

	@Test void a01_plainConsoleFreemarkerMixin_datatableMacroUnresolvable() throws Exception {
		var c = MockRestClient.buildLax(PlainControlHost.class);
		c.get("/releases").run().assertStatus(500);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// GREEN: ConsoleDataTablesFreemarkerMixin registers the <@datatable> macro's backing shared variable (on top of
	// everything ConsoleFreemarkerMixin already wires) -- the rendered output must contain the pill markup nested
	// inside a <td> inside a <table class="jc-table" data-juneau-datatable ...>.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class)
	public static class DataTablesHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleDataTablesFreemarkerMixin.create().basePath("/templates/").build();
		}
		@RestGet(path="/releases")
		public View releases() {
			return FreemarkerView.of("admin/releases.ftlh")
				.attr("releases", List.of(new ReleaseRow()))
				.attr("rowTypeName", ReleaseRow.class.getName());
		}
	}

	@Test void a02_consoleDataTablesFreemarkerMixin_rendersPillMarkupInsideJcTable() throws Exception {
		var c = MockRestClient.buildLax(DataTablesHost.class);
		var body = c.get("/releases").run().assertStatus(200).getContent().asString();
		// find() on an anchor-free pattern (no wrapping .*) avoids the super-linear backtracking risk of
		// String.matches() with unbounded quantifiers at both ends.
		assertTrue(Pattern.compile("<table(?=[^>]*class=['\"]jc-table['\"])(?=[^>]*data-juneau-datatable)[^>]*>").matcher(body).find(),
			() -> "expected <table class='jc-table' ...data-juneau-datatable...> (attribute order not asserted), body:\n" + body);
		assertTrue(Pattern.compile("<td[^>]*>\\s*<span(?=[^>]*class=['\"]tag status released['\"])[^>]*>.*?</td>", Pattern.DOTALL).matcher(body).find(),
			() -> "expected <span class='tag status released'> nested inside a <td>, body:\n" + body);
		assertTrue(body.contains("widget"), () -> "expected the plain property's raw value too, body:\n" + body);
		assertFalse(body.contains("&lt;span"), () -> "macro output was HTML-escaped (double-escaped), body:\n" + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Module-graph re-check (S6): this module's main source is the ONLY one of the three new modules whose imports
	// include org.apache.juneau.rest.server.datatables.* -- console-ui and console-ui-freemarker main sources
	// still don't (re-verified here alongside the real code, not just at the POM level).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a03_onlyThisModuleImportsDatatablesPackage() throws Exception {
		var restDir = new java.io.File(System.getProperty("user.dir")).getParentFile();

		var thisModuleImportsDatatables = importsDatatablesPackage(new java.io.File(restDir, "juneau-rest-server-console-ui-freemarker-datatables/src/main/java"));
		assertTrue(thisModuleImportsDatatables, "console-ui-freemarker-datatables MUST import org.apache.juneau.rest.server.datatables.* (it's the only module allowed to)");

		assertFalse(importsDatatablesPackage(new java.io.File(restDir, "juneau-rest-server-console-ui/src/main/java")),
			"console-ui main source must NOT import org.apache.juneau.rest.server.datatables.*");
		assertFalse(importsDatatablesPackage(new java.io.File(restDir, "juneau-rest-server-console-ui-freemarker/src/main/java")),
			"console-ui-freemarker main source must NOT import org.apache.juneau.rest.server.datatables.*");
	}

	private static boolean importsDatatablesPackage(java.io.File srcMain) throws Exception {
		assertTrue(srcMain.isDirectory(), () -> "Expected sibling src/main/java not found: " + srcMain.getAbsolutePath());
		try (var files = java.nio.file.Files.walk(srcMain.toPath())) {
			return files
				.filter(p -> p.toString().endsWith(".java"))
				.anyMatch(p -> {
					try {
						return java.nio.file.Files.readString(p).contains("org.apache.juneau.rest.server.datatables");
					} catch (java.io.IOException e) {
						throw new java.io.UncheckedIOException(e);
					}
				});
		}
	}
}
