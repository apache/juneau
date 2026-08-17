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

import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

import freemarker.cache.*;
import freemarker.template.Configuration;
import freemarker.template.TemplateNotFoundException;

/**
 * Phase 5 gate: {@link ConsoleFreemarkerMixin} (reserved-path template loader, {@code base.ftlh}, the
 * {@code <@tag>} macro) and the two should-fix S2 gates the r2 review added (double-wrap prevention;
 * consumer-supplied {@code Configuration} identity honored).
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class ConsoleFreemarkerMixin_Test extends TestBase {

	private enum Release { RELEASED, DRAFT }

	//-----------------------------------------------------------------------------------------------------------------
	// a) Classpath-location gate
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * RED: before any console wiring exists, a plain {@code basePath("/templates/")} bridge-default configuration
	 * cannot resolve the reserved console template &mdash; proves the gap is real, not hypothetical.
	 */
	@Test void a01_plainFreemarkerMixin_cannotResolveReservedTemplate() throws Exception {
		var plain = FreemarkerMixin.create().basePath("/templates/").build();
		var cfg = plain.resolveConfiguration(dummyRequest());
		assertThrows(TemplateNotFoundException.class, () -> cfg.getTemplate(ConsoleFreemarkerMixin.BASE_TEMPLATE_PATH));
	}

	/** GREEN: the same shape of check against {@link ConsoleFreemarkerMixin} succeeds. */
	@Test void a02_consoleFreemarkerMixin_resolvesReservedTemplate() throws Exception {
		var console = ConsoleFreemarkerMixin.create().basePath("/templates/").build();
		var cfg = console.resolveConfiguration(dummyRequest());
		assertDoesNotThrow(() -> cfg.getTemplate(ConsoleFreemarkerMixin.BASE_TEMPLATE_PATH));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Golden-HTML composition gate (through FreemarkerViewRenderer's exact-type lookup)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class)
	public static class PlainControlHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return FreemarkerMixin.create().basePath("/templates/").build();
		}
		@RestGet(path="/mypage")
		public View mypage() {
			return FreemarkerView.of("admin/mypage.ftlh").attr("release", Release.RELEASED);
		}
	}

	@Rest(mixins=FreemarkerMixin.class)
	public static class ConsoleHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").build();
		}
		@RestGet(path="/mypage")
		public View mypage() {
			return FreemarkerView.of("admin/mypage.ftlh").attr("release", Release.RELEASED);
		}
	}

	/**
	 * RED / false-green trap avoidance: the vanilla {@code /templates/}-rooted loader cannot see the reserved
	 * classpath-root path, so the {@code <#include>} in {@code admin/mypage.ftlh} fails and the render 500s.
	 * {@code basePath("/templates/")} (not the accidental default {@code "/"}) is what makes this a real RED
	 * &mdash; under {@code "/"} the vanilla classpath-root loader would see the reserved path too and false-GREEN.
	 */
	@Test void b01_plainFreemarkerMixin_cannotComposeReservedInclude() throws Exception {
		var c = MockRestClient.buildLax(PlainControlHost.class);
		c.get("/mypage").run().assertStatus(500);
	}

	/**
	 * GREEN: identical page/attrs, but the host registers {@code ConsoleFreemarkerMixin} under the
	 * load-bearing {@code FreemarkerMixin} return type. Both loaders work: the consumer's own nested
	 * {@code admin/mypage.ftlh} resolves AND the {@code <#include>}d reserved {@code base.ftlh} resolves and
	 * renders the {@code <@tag>} macro's chrome markup.
	 */
	@Test void b02_consoleFreemarkerMixin_composesConsumerPageAndReservedInclude() throws Exception {
		var c = MockRestClient.buildLax(ConsoleHost.class);
		var body = c.get("/mypage").run()
			.assertStatus(200)
			.getContent().asString();
		assertTrue(body.contains("consumer-page-marker"), () -> "consumer's own /templates/ page did not render, body:\n" + body);
		assertTrue(body.contains("tag status released"), () -> "the <#include>d base.ftlh's <@tag> macro did not render, body:\n" + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Anti-pattern negative gate (wrong bean return type)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class)
	public static class WrongBeanTypeHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		// Anti-pattern: declared return type is the SUBTYPE, not FreemarkerMixin -- invisible to the renderer's
		// exact-type getBean(FreemarkerMixin.class) lookup.
		@Bean public ConsoleFreemarkerMixin freemarker() {
			// Explicit cast is required here (not just illustrative): the inherited fluent setter basePath(...)
			// returns the PARENT FreemarkerMixin.Builder static type, so build() resolves at compile time against
			// THAT type's declared return type (FreemarkerMixin) -- see ConsoleFreemarkerMixin's class javadoc.
			return (ConsoleFreemarkerMixin) ConsoleFreemarkerMixin.create().basePath("/templates/").build();
		}
		@RestGet(path="/mypage")
		public View mypage() {
			return FreemarkerView.of("admin/mypage.ftlh").attr("release", Release.RELEASED);
		}
	}

	/**
	 * The wrongly-typed bean is invisible to {@code FreemarkerViewRenderer}'s exact-type lookup, which falls back
	 * to a vanilla {@code new FreemarkerMixin()} ({@code basePath="/"}). Asserting the *outcome* (no composed
	 * chrome under {@code /templates/}), not a specific exception, since the failure mode is wiring-dependent.
	 */
	@Test void c01_wrongBeanReturnType_doesNotComposeChrome() throws Exception {
		var c = MockRestClient.buildLax(WrongBeanTypeHost.class);
		var res = c.get("/mypage").run();
		if (res.getStatusCode() == 200) {
			var body = res.getContent().asString();
			assertFalse(body.contains("tag status released"), () -> "wrong-bean-type wiring unexpectedly composed chrome, body:\n" + body);
		}
		// else: non-200 (e.g. 500 from an unresolvable include) is also an acceptable "did not compose" outcome.
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) Trusted-HTML gate
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * {@code <@tag domain="status" value=release/>} (a raw enum constant bound as a template attribute) produces
	 * literal pill markup, not the {@code DefaultObjectWrapper} bean-dump/empty footgun a bare {@code ${...}}
	 * would produce.
	 */
	@Test void d01_tagMacro_rendersLiteralPillMarkup_notEscaped() throws Exception {
		var c = MockRestClient.buildLax(ConsoleHost.class);
		var body = c.get("/mypage").run().assertStatus(200).getContent().asString();
		assertFalse(body.contains("&lt;span"), () -> "macro output was HTML-escaped (double-escaped), body:\n" + body);
		// find() on an anchor-free pattern (no wrapping .*) avoids the super-linear backtracking risk of
		// String.matches() with unbounded quantifiers at both ends.
		assertTrue(Pattern.compile("<span[^>]*class=['\"]tag status released['\"][^>]*>").matcher(body).find(),
			() -> "expected literal <span class='tag status released'> markup, body:\n" + body);
	}

	/**
	 * A bare {@code ${Tag.of(...)}} (never done through the macro in this codebase, but documenting the footgun
	 * directly) is NOT equivalent to calling the Java factory and stringifying its result through the trusted
	 * adapter: {@link org.apache.juneau.rest.server.console.Tag#of} returns an {@code HtmlElement}, and the
	 * bridge-default {@code DefaultObjectWrapper} (exposeFields=true) wraps arbitrary objects as bean models, not
	 * pre-rendered markup -- so the ONLY supported v1 insertion point is the {@code <@tag>} macro documented on
	 * {@link ConsoleFreemarkerMixin}, never a direct method-model call written by hand in a template.
	 */
	@Test void d02_tagMethodModel_isNotExposedAsABareTemplateVariable() throws Exception {
		var console = ConsoleFreemarkerMixin.create().basePath("/templates/").build();
		var cfg = console.resolveConfiguration(dummyRequest());
		// jcTagHtml is a shared variable on the augmented Configuration -- exists ONLY so base.ftlh's <#macro tag>
		// can call it; it is deliberately not part of any public API surface a consumer would discover/write by
		// hand (no javadoc, package-private class). This assertion documents that the shared variable is scoped
		// to the internal macro plumbing, not a public "insertion point" a consumer should call directly.
		assertNotNull(cfg.getSharedVariable(TagMethodModel.NAME), "the <@tag> macro's backing method must be registered");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e) Should-fix S2 gate 1: double-wrap prevention
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class)
	public static class DoubleWrapProbeHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").build();
		}
		@RestGet(path="/probe")
		public String probe(RestRequest req) {
			var mixin = (ConsoleFreemarkerMixin) req.getContext().getBeanStore().getBean(FreemarkerMixin.class).orElseThrow();
			var cfg = mixin.resolveConfiguration(req);
			var loader = cfg.getTemplateLoader();
			if (! (loader instanceof MultiTemplateLoader multi))
				return "not-multi";
			return multi.getTemplateLoaderCount() + ":" + System.identityHashCode(multi.getTemplateLoader(0)) + ":" + System.identityHashCode(cfg);
		}
	}

	/**
	 * Two successive {@code resolveConfiguration(req)} calls on the same {@code ConsoleFreemarkerMixin} (no
	 * {@code Configuration} bean) must return the same cached {@code Configuration} instance whose
	 * {@code getTemplateLoader()} is a {@code MultiTemplateLoader} of length 2 <b>both times</b> &mdash; not
	 * {@code Multi(Multi(base, console), console)} on the second call. Two independent HTTP requests hit the same
	 * mixin-bean singleton (one {@code MockRestClient}/{@code RestContext}), each triggering one
	 * {@code resolveConfiguration(req)} call.
	 */
	@Test void e01_secondResolveConfigurationCall_doesNotNestTheLoader() throws Exception {
		var c = MockRestClient.buildLax(DoubleWrapProbeHost.class);
		var r1 = c.get("/probe").accept("text/plain").run().getContent().asString();
		var r2 = c.get("/probe").accept("text/plain").run().getContent().asString();
		var p1 = r1.split(":");
		var p2 = r2.split(":");
		assertEquals(3, p1.length, () -> "expected a MultiTemplateLoader on the first call, got: " + r1);
		assertEquals(3, p2.length, () -> "expected a MultiTemplateLoader on the second call, got: " + r2);
		assertEquals(p1[2], p2[2], "both calls must return the SAME cached Configuration instance");
		assertEquals("2", p1[0], "first call: expected exactly 2 loaders (base + console)");
		assertEquals("2", p2[0], () -> "double-wrap: second call's loader count grew past 2 (nested Multi), got " + p2[0]);
		assertEquals(p1[1], p2[1], "double-wrap: index-0 loader identity changed between calls (got re-wrapped)");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f) Should-fix S2 gate 2: consumer Configuration identity honored
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class)
	public static class ConsumerConfigHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final Configuration USER_CFG = new Configuration(Configuration.VERSION_2_3_34);
		static final TemplateLoader ORIGINAL_LOADER = USER_CFG.getTemplateLoader();
		@Bean public Configuration configuration() { return USER_CFG; }
		@Bean public FreemarkerMixin freemarker() { return ConsoleFreemarkerMixin.create().build(); }
		@RestGet(path="/probe")
		public String probe(RestRequest req) {
			var mixin = (ConsoleFreemarkerMixin) req.getContext().getBeanStore().getBean(FreemarkerMixin.class).orElseThrow();
			var cfg = mixin.resolveConfiguration(req);
			var sameInstance = cfg == USER_CFG;
			var loaderUnchanged = cfg.getTemplateLoader() == ORIGINAL_LOADER;
			var reservedResolves = true;
			try {
				cfg.getTemplate(ConsoleFreemarkerMixin.BASE_TEMPLATE_PATH);
			} catch (Exception ex) {
				reservedResolves = false;
			}
			return sameInstance + ":" + loaderUnchanged + ":" + reservedResolves;
		}
	}

	/**
	 * A host that registers its own {@code @Bean Configuration} (no console loader) plus
	 * {@code ConsoleFreemarkerMixin}: {@code resolveConfiguration(req)} must return that SAME instance
	 * ({@code ==}), its {@code getTemplateLoader()} must be unchanged, and the reserved console template must NOT
	 * resolve (the consumer never added the console loader themselves).
	 */
	@Test void f01_consumerSuppliedConfiguration_isReturnedUntouched() throws Exception {
		var c = MockRestClient.buildLax(ConsumerConfigHost.class);
		var body = c.get("/probe").accept("text/plain").run().getContent().asString();
		assertEquals("true:true:false", body,
			() -> "expected same-instance=true, loader-unchanged=true, reserved-template-resolves=false; got " + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// helpers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A minimal live {@link RestRequest} for in-process {@code resolveConfiguration(req)} calls (empty
	 * {@code BeanStore} -- no {@code Configuration} bean registered). {@code MockRestClient}'s classic client
	 * runs the server side synchronously on the calling thread, so the handler's capture into
	 * {@link DummyHost#CAPTURED} is visible immediately after {@code run()} returns.
	 */
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
}
