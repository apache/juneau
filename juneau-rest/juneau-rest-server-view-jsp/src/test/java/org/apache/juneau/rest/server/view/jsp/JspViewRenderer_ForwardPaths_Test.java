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
package org.apache.juneau.rest.server.view.jsp;

import java.io.*;
import java.lang.reflect.*;

import jakarta.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.junit.jupiter.api.*;

/**
 * Covers {@link JspViewRenderer#process process(...)}'s dispatch-succeeded and dispatch-failed
 * branches (the {@code rd != null} half of the {@code rd == null} check, plus every {@code catch}
 * clause around {@code rd.forward(...)}).
 *
 * <p>
 * Mirrors {@link JspDispatcher_ForwardPaths_Test}'s technique (a hand-rolled {@link ServletContext}
 * swapped in via {@link MockRestRequest#servletContext}) but exercises the sibling code path reached
 * when an {@code @RestOp} method returns a {@link JspView} rather than a raw {@code /jsp/*} request.
 *
 * <p>
 * Fixture {@code A} declares {@code @Rest(responseProcessors=JspViewRenderer.class)} directly
 * (the pattern proven to work by {@code JspView_TypedHandler_Test} in {@code juneau-integration-tests}).
 * A bare {@code @Rest(mixins=JspMixin.class)} now reaches the same code path too, because {@code JspMixin}
 * declares {@link Rest#mergeResponseProcessorsIntoHost() @Rest(mergeResponseProcessorsIntoHost=true)} on its
 * own class, folding {@link JspViewRenderer} into the host's own chain — see {@code z01} below.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class JspViewRenderer_ForwardPaths_Test extends TestBase {

	@Rest(responseProcessors=JspViewRenderer.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/view")
		public View view() {
			return JspView.of("hello.jsp");
		}
		@RestGet(path="/escape")
		public View escape() {
			// Not user input (see the class-level javadoc on JspViewRenderer.joinPath): a caller-assembled
			// template name that itself escapes the configured base path via ".." segments.
			return JspView.of("../../etc/passwd");
		}
		@RestGet(path="/plain")
		public String plain() {
			return "not a JspView";
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	// A bare @Rest(mixins=JspMixin.class) DOES route a HOST-defined @RestGet method's JspView return value
	// through JspViewRenderer, because JspMixin declares @Rest(mergeResponseProcessorsIntoHost=true) on its own
	// class. That opt-in folds JspMixin's own @Rest(responseProcessors=JspViewRenderer.class) into the host's
	// own response-processor chain (response-processor-scoped only), so a host op returning a JspView reaches
	// the renderer with no extra wiring. See z01 below, JspMixin's class javadoc, and
	// MixinResponseProcessorFold_Test (rest-server) / MixinInheritance_ResponseProcessors_Test
	// (juneau-integration-tests), which pin the fold at the RestContext and mock-client levels respectively.
	@Rest(mixins=JspMixin.class)
	public static class MixinOnly extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/view")
		public View view() {
			return JspView.of("hello.jsp");
		}
	}

	private static final MockRestClient cMixinOnly = MockRestClient.buildLax(MixinOnly.class);

	// Non-silent regression guard: a mixin that registers JspViewRenderer but does NOT declare the
	// mergeResponseProcessorsIntoHost opt-in stays scoped to its own endpoints -- its responseProcessors are
	// NOT folded into the host's chain, so a host op returning a JspView falls back to bean-serialization.
	// This is the deliberately-isolated default that JspMixin overrides by opting in (see z03 vs z01).
	@Rest(responseProcessors=JspViewRenderer.class)
	public static class NoFoldMixin {
		@RestGet(path="/mixin-only-endpoint")
		public String noop() {
			return "noop";
		}
	}

	@Rest(mixins=NoFoldMixin.class)
	public static class HostWithNoFoldMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/view")
		public View view() {
			return JspView.of("hello.jsp");
		}
	}

	private static final MockRestClient cNoFoldMixin = MockRestClient.buildLax(HostWithNoFoldMixin.class);

	// Opt-in host: adopts JspMixin via the rich mixinDefs form with mergeIntoHost=true, so the mixin's
	// @Rest(responseProcessors=JspViewRenderer.class) folds into THIS host's own chain.
	@Rest(mixinDefs=@Mixin(type=JspMixin.class, mergeIntoHost=true))
	public static class MergeIntoHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/view")
		public View view() {
			return JspView.of("hello.jsp");
		}
	}

	private static final MockRestClient cMergeIntoHost = MockRestClient.buildLax(MergeIntoHost.class);

	@Test void z01_mixinAlone_routesHostViewReturnThroughRenderer() throws Exception {
		// JspMixin declares @Rest(mergeResponseProcessorsIntoHost=true), so a bare @Rest(mixins=JspMixin.class)
		// folds JspViewRenderer into the host's own chain: a host @RestGet returning a JspView is CLAIMED by the
		// renderer (not bean-serialized). With a well-behaved dispatcher that commits the response, the render
		// succeeds end-to-end (200) -- deterministic proof the fold routes AND renders, not merely that it was
		// intercepted. The companion null-dispatcher assertion below pins that it reached the renderer rather than
		// the default SerializedPojoProcessor (which would have produced a 200 "templateName" body, as in z03).
		var okCtx = fakeServletContext(new FakeDispatcher(() -> { /* well-behaved engine committed the response */ }));
		cMixinOnly.get("/view").servletContext(okCtx).run().assertStatus(200);

		var noEngineCtx = fakeServletContext(null);
		var res = cMixinOnly.get("/view").servletContext(noEngineCtx).run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("Could not resolve RequestDispatcher");
		res.assertContent().asString().isContains("No JSP engine is available on the classpath");
	}

	@Test void z03_nonOptedInMixin_doesNotRouteHostViewReturnThroughRenderer() throws Exception {
		// Regression guard for the non-silent contract: NoFoldMixin registers JspViewRenderer but does NOT opt in
		// via mergeResponseProcessorsIntoHost, so the renderer stays scoped to the mixin's own endpoints and is
		// NOT folded into the host's chain. The host's JspView return therefore falls back to bean-serialization
		// (SerializedPojoProcessor won) -- a 200 whose body contains the JspView bean's own "templateName" field,
		// which JspViewRenderer's dispatch path never produces. Contrast with z01 (opted-in JspMixin routes).
		var res = cNoFoldMixin.get("/view").accept("application/json").run();
		res.assertStatus(200);
		res.assertContent().asString().isContains("templateName");
	}

	@Test void z02_mergeIntoHost_routesHostViewReturnThroughRenderer() throws Exception {
		// Opt-in @Mixin(mergeIntoHost=true) folds JspMixin's @Rest(responseProcessors=JspViewRenderer.class)
		// into the HOST's own chain, so a host @RestGet returning a JspView is now CLAIMED by JspViewRenderer
		// (not bean-serialized). With a ServletContext that resolves no dispatcher, the renderer surfaces its
		// NO_ENGINE_DIAGNOSTIC 500 -- deterministic proof the JspView reached JspViewRenderer rather than the
		// default SerializedPojoProcessor (which would have produced a 200 "templateName" body as in z03).
		var ctx = fakeServletContext(null);
		var res = cMergeIntoHost.get("/view").servletContext(ctx).run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("Could not resolve RequestDispatcher");
		res.assertContent().asString().isContains("No JSP engine is available on the classpath");
	}

	/** Functional seam for {@link #forward(ServletRequest, ServletResponse)} on the fake dispatcher below. */
	@FunctionalInterface
	private interface ForwardAction {
		void run() throws ServletException, IOException;
	}

	/** Minimal {@link RequestDispatcher} whose {@code forward(...)} delegates to a configurable action. */
	private static final class FakeDispatcher implements RequestDispatcher {
		private final ForwardAction action;
		FakeDispatcher(ForwardAction action) { this.action = action; }
		@Override public void forward(ServletRequest req, ServletResponse res) throws ServletException, IOException { action.run(); }
		@Override public void include(ServletRequest req, ServletResponse res) { /* not exercised by process(...) */ }
	}

	/** See {@code JspDispatcher_ForwardPaths_Test.fakeServletContext} for why a bare {@link Proxy} suffices. */
	private static ServletContext fakeServletContext(RequestDispatcher dispatcher) {
		return (ServletContext) Proxy.newProxyInstance(
			JspViewRenderer_ForwardPaths_Test.class.getClassLoader(),
			new Class<?>[] { ServletContext.class },
			(proxy, method, args) -> "getRequestDispatcher".equals(method.getName()) ? dispatcher : null);
	}

	@Test void a0_nonJspViewContent_returnsNextAndFallsThroughToPojoSerializer() throws Exception {
		// The `content instanceof JspView` check's other branch: a non-JspView return value must NOT be
		// claimed by this processor -- it returns NEXT so the standard POJO serializer chain handles it.
		var res = c.get("/plain").run();
		res.assertStatus(200);
		res.assertContent().asString().isContains("not a JspView");
	}

	@Test void a0b_templateNameEscapesBasePath_wrappedAs500() throws Exception {
		// joinPath(...) throws IllegalArgumentException when the template name resolves outside
		// basePath; process(...) catches that (distinct from the rd == null / forward() catches below,
		// which all run later in the method) and wraps it with a dedicated escape diagnostic.
		var res = c.get("/escape").run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("escapes configured base path");
	}

	@Test void a00_contextPresentButDispatcherNull_reportsNoEngineDiagnostic() throws Exception {
		// The rd == null branch itself (a non-null ServletContext that resolves the target to no
		// dispatcher), as distinct from a null ServletContext (which never reaches this class's process()
		// call at all under MockRest's defaults). process(...) re-throws InternalServerError ahead of the
		// generic catch, so this branch's specific "Could not resolve RequestDispatcher...
		// NO_ENGINE_DIAGNOSTIC" message reaches the response body intact instead of being re-wrapped into
		// the generic "JSP render failed for" message.
		var ctx = fakeServletContext(null);
		var res = c.get("/view").servletContext(ctx).run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("Could not resolve RequestDispatcher");
		res.assertContent().asString().isContains("No JSP engine is available on the classpath");
	}

	@Test void a01_dispatcherFound_forwardSucceeds_returnsFinished() throws Exception {
		var ctx = fakeServletContext(new FakeDispatcher(() -> { /* no-op: a well-behaved engine committed the response itself */ }));
		c.get("/view").servletContext(ctx).run().assertStatus(200);
	}

	@Test void a02_dispatcherFound_forwardThrowsIOException_propagatesAs500() throws Exception {
		// IOException is declared by process(...) and re-thrown as-is (not wrapped) -- the framework's
		// default exception-to-status mapping for an undeclared-HTTP-status checked exception is 500.
		var ctx = fakeServletContext(new FakeDispatcher(() -> { throw new IOException("disk full"); }));
		c.get("/view").servletContext(ctx).run().assertStatus(500);
	}

	@Test void a03_dispatcherFound_forwardThrowsNoClassDefFoundError_wrappedWithEngineDiagnostic() throws Exception {
		// A JSP engine class missing at runtime surfaces as NoClassDefFoundError from inside forward()
		// -- wrapped with the same NO_ENGINE_DIAGNOSTIC text as the rd == null case, rather than a bare 500.
		var ctx = fakeServletContext(new FakeDispatcher(() -> { throw new NoClassDefFoundError("org.apache.jasper.JspCompilationContext"); }));
		var res = c.get("/view").servletContext(ctx).run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("jetty-ee11-apache-jsp");
	}

	@Test void a04_dispatcherFound_forwardThrowsOtherException_wrappedAs500() throws Exception {
		// Any other exception from forward() (e.g. a ServletException from a failing JSP compile) falls
		// through to the generic catch and is wrapped with the target path in the message.
		var ctx = fakeServletContext(new FakeDispatcher(() -> { throw new ServletException("compile error"); }));
		var res = c.get("/view").servletContext(ctx).run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("JSP render failed for");
	}
}
