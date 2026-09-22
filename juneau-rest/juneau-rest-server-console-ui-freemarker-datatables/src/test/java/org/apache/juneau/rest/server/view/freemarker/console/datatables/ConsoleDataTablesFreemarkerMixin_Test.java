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
import org.apache.juneau.bean.html5.Span;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.junit.jupiter.api.*;

import freemarker.cache.*;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;

/**
 * Ticket 361 Phase 7 gate: the {@code <@datatable>} macro (this module's only deliverable) &mdash; a golden-HTML
 * integration proof that a row bean's {@code @Html(render=...)} enum property renders as
 * pill markup (Phase 6's now-render-aware {@code DataTablesTable}) nested inside a {@code jc-table} through the
 * same trusted-HTML adapter Phase 5 built for {@code <@tag>}.
 */
@SuppressWarnings({
	"resource" // Test-fixture AutoCloseables are managed by the test lifecycle, not real leaks (mixed-module resource analysis on test code).
})
class ConsoleDataTablesFreemarkerMixin_Test extends TestBase {

	public enum Release { RELEASED, DRAFT }

	/** Local stand-in for the retired {@code TagHtmlRender}: a status pill span. */
	public static class StatusPillRender extends HtmlRender<Enum<?>> {
		@Override
		public Object getContent(SerializerSession session, Enum<?> value) {
			if (value == null)
				return null;
			return new Span().class_("tag status " + value.name().toLowerCase(Locale.ROOT));
		}
	}

	/** Row bean: one plain property, one {@code @Html(render=...)}-annotated enum property. */
	public static class ReleaseRow {
		public String name = "widget";
		@Html(render=StatusPillRender.class) public Release status = Release.RELEASED;
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

	//-----------------------------------------------------------------------------------------------------------------
	// f) Consumer Configuration is the same instance, then fill-missing stamped
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class)
	public static class ConsumerConfigHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final Configuration USER_CFG = new Configuration(Configuration.VERSION_2_3_34);
		static {
			USER_CFG.setTemplateLoader(new StringTemplateLoader());
		}
		static final Object ORIGINAL_WRAPPER = USER_CFG.getObjectWrapper();
		static final String ORIGINAL_ENCODING = USER_CFG.getDefaultEncoding();
		static final Object ORIGINAL_OUTPUT_FORMAT = USER_CFG.getOutputFormat();
		static final long ORIGINAL_UPDATE_DELAY = USER_CFG.getTemplateUpdateDelayMilliseconds();
		@Bean public Configuration configuration() { return USER_CFG; }
		@Bean public FreemarkerMixin freemarker() { return ConsoleDataTablesFreemarkerMixin.create().build(); }
		@RestGet(path="/probe")
		public String probe(RestRequest req) {
			var mixin = (ConsoleDataTablesFreemarkerMixin) req.getContext().getBeanStore().getBean(FreemarkerMixin.class).orElseThrow();
			var cfg1 = mixin.resolveConfiguration(req);
			var cfg2 = mixin.resolveConfiguration(req);
			var sameInstance = cfg1 == USER_CFG && cfg2 == USER_CFG;
			var settingsUnchanged = cfg1.getObjectWrapper() == ORIGINAL_WRAPPER
				&& Objects.equals(cfg1.getDefaultEncoding(), ORIGINAL_ENCODING)
				&& cfg1.getOutputFormat() == ORIGINAL_OUTPUT_FORMAT
				&& cfg1.getTemplateUpdateDelayMilliseconds() == ORIGINAL_UPDATE_DELAY;
			var varsFilled = cfg1.getSharedVariable(PageDirectiveModel.NAME) != null
				&& cfg1.getSharedVariable(CardDirectiveModel.NAME) != null
				&& cfg1.getSharedVariable(NavigationDirectiveModel.NAME) != null
				&& cfg1.getSharedVariable(NodeDirectiveModel.NAME) != null
				&& cfg1.getSharedVariable(ThemeDirectiveModel.NAME) != null
				&& cfg1.getSharedVariable("jcTagHtml") != null
				&& cfg1.getSharedVariable(DataTableMethodModel.NAME) != null;
			var reservedResolves = true;
			try {
				cfg1.getTemplate(ConsoleFreemarkerMixin.BASE_TEMPLATE_PATH);
			} catch (@SuppressWarnings("unused") Exception ex) {
				reservedResolves = false;
			}
			return sameInstance + ":" + settingsUnchanged + ":" + varsFilled + ":" + reservedResolves
				+ ":" + loaderCount(cfg1) + ":" + loaderCount(cfg2);
		}
	}

	/**
	 * Same assertions as {@code ConsoleFreemarkerMixin_Test.f01}, plus {@code jcDataTableHtml} is set.
	 */
	@Test void f01_consumerSuppliedConfiguration_isAugmentedInPlace() throws Exception {
		var c = MockRestClient.buildLax(ConsumerConfigHost.class);
		var body = c.get("/probe").accept("text/plain").run().getContent().asString();
		assertEquals("true:true:true:true:2:2", body,
			() -> "expected same-instance, settings-unchanged, vars-filled (incl jcDataTableHtml), reserved-resolves, loader-count 2 then 2; got " + body);
	}

	@Rest(mixins=FreemarkerMixin.class)
	public static class CollisionConfigHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final Configuration COLLISION_CFG = new Configuration(Configuration.VERSION_2_3_34);
		static final TemplateModel PAGE_SENTINEL = new SimpleScalar("consumer-page");
		static {
			COLLISION_CFG.setSharedVariable(PageDirectiveModel.NAME, PAGE_SENTINEL);
		}
		@Bean public Configuration configuration() { return COLLISION_CFG; }
		@Bean public FreemarkerMixin freemarker() { return ConsoleDataTablesFreemarkerMixin.create().build(); }
		@RestGet(path="/probe")
		public String probe(RestRequest req) {
			var mixin = (ConsoleDataTablesFreemarkerMixin) req.getContext().getBeanStore().getBean(FreemarkerMixin.class).orElseThrow();
			var cfg = mixin.resolveConfiguration(req);
			var pageKept = cfg.getSharedVariable(PageDirectiveModel.NAME) == PAGE_SENTINEL;
			var othersFilled = cfg.getSharedVariable(CardDirectiveModel.NAME) != null
				&& cfg.getSharedVariable(NavigationDirectiveModel.NAME) != null
				&& cfg.getSharedVariable(NodeDirectiveModel.NAME) != null
				&& cfg.getSharedVariable(ThemeDirectiveModel.NAME) != null
				&& cfg.getSharedVariable("jcTagHtml") != null
				&& cfg.getSharedVariable(DataTableMethodModel.NAME) != null;
			return pageKept + ":" + othersFilled;
		}
	}

	/**
	 * Consumer bean already has {@code page} set: that sentinel is kept; the other Juneau reserved names
	 * (including {@code jcDataTableHtml}) are still fill-missing stamped.
	 */
	@Test void f02_consumerPageSentinel_isKeptAndOtherNamesFilled() throws Exception {
		var c = MockRestClient.buildLax(CollisionConfigHost.class);
		var body = c.get("/probe").accept("text/plain").run().getContent().asString();
		assertEquals("true:true", body,
			() -> "expected page-sentinel-kept=true, other-names-filled=true; got " + body);
	}

	@Rest
	public static class SharedConfigDummyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final Configuration SHARED_CFG = new Configuration(Configuration.VERSION_2_3_34);
		static {
			SHARED_CFG.setTemplateLoader(new StringTemplateLoader());
		}
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		@Bean public Configuration configuration() { return SHARED_CFG; }
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}

	/**
	 * Two {@link ConsoleDataTablesFreemarkerMixin} instances sharing one consumer {@code Configuration} bean
	 * splice the console loader once: {@code MultiTemplateLoader} count stays 2.
	 */
	@Test void f03_twoMixinInstances_sharedConsumerBean_loaderCountStays2() throws Exception {
		MockRestClient.buildLax(SharedConfigDummyHost.class).get("/x").run();
		var req = SharedConfigDummyHost.CAPTURED.get();
		var m1 = ConsoleDataTablesFreemarkerMixin.create().build();
		var m2 = ConsoleDataTablesFreemarkerMixin.create().build();
		var c1 = m1.resolveConfiguration(req);
		var c2 = m2.resolveConfiguration(req);
		assertSame(SharedConfigDummyHost.SHARED_CFG, c1);
		assertSame(SharedConfigDummyHost.SHARED_CFG, c2);
		assertEquals(2, loaderCount(c1));
		assertEquals(2, loaderCount(c2));
		assertNotNull(c1.getSharedVariable(DataTableMethodModel.NAME));
		assertNotNull(c2.getSharedVariable(DataTableMethodModel.NAME));
	}

	private static int loaderCount(Configuration cfg) {
		var loader = cfg.getTemplateLoader();
		return loader instanceof MultiTemplateLoader multi ? multi.getTemplateLoaderCount() : -1;
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
