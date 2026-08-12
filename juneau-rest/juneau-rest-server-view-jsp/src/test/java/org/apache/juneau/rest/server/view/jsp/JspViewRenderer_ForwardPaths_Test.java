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
 * (the pattern proven to work by {@code JspView_TypedHandler_Test} in {@code juneau-integration-tests}),
 * rather than relying on {@code @Rest(mixins=JspMixin.class)} alone — see {@code z01} below for why.
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

	// A BARE @Rest(mixins=JspMixin.class) does NOT route a HOST-defined @RestGet method's
	// JspView return value through JspViewRenderer -- this is the default, deliberately-isolated behavior.
	// RestContext#getRestAnnotationsForProperty resolves a HOST context's own responseProcessors chain from the
	// host's OWN @Rest annotation chain (ancestor classes); a mixin class's @Rest(responseProcessors=...) is a
	// property of the MIXIN's own sub-context (consulted only for ops declared directly on the mixin class
	// itself, e.g. JspMixin#render), and is NOT folded into the host's list unless the host opts in. See
	// Rest#mixins() javadoc ("host's chain runs first, then the mixin's appended. Host endpoints see only the
	// host's chain") and MixinInheritance_ResponseProcessors_Test#a02 in juneau-integration-tests, which pins
	// this default isolation. To have a host's own JspView returns reach JspViewRenderer, the host opts in via
	// @Mixin(mergeIntoHost=true) (see MergeIntoHost fixture + z02 below), which folds the mixin's list-shaped
	// @Rest attributes (including responseProcessors) into the host's own chain; the manual equivalent is
	// listing JspViewRenderer.class directly in the host's own @Rest(responseProcessors=...) -- exactly like
	// this class's own fixture A above.
	@Rest(mixins=JspMixin.class)
	public static class MixinOnly extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/view")
		public View view() {
			return JspView.of("hello.jsp");
		}
	}

	private static final MockRestClient cMixinOnly = MockRestClient.buildLax(MixinOnly.class);

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

	@Test void z01_mixinAlone_doesNotRouteHostViewReturnThroughRenderer() throws Exception {
		// Bean-serialized fallback (SerializedPojoProcessor won), NOT JSP-dispatched -- the body contains the
		// JspView bean's own "templateName" field, which JspViewRenderer's actual dispatch path never produces.
		var res = cMixinOnly.get("/view").accept("application/json").run();
		res.assertStatus(200);
		res.assertContent().asString().isContains("templateName");
	}

	@Test void z02_mergeIntoHost_routesHostViewReturnThroughRenderer() throws Exception {
		// Opt-in @Mixin(mergeIntoHost=true) folds JspMixin's @Rest(responseProcessors=JspViewRenderer.class)
		// into the HOST's own chain, so a host @RestGet returning a JspView is now CLAIMED by JspViewRenderer
		// (not bean-serialized). With a ServletContext that resolves no dispatcher, the renderer surfaces its
		// NO_ENGINE_DIAGNOSTIC 500 -- deterministic proof the JspView reached JspViewRenderer rather than the
		// default SerializedPojoProcessor (which would have produced a 200 "templateName" body as in z01).
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
		// call at all under MockRest's defaults). process(...) now has a dedicated
		// `catch (InternalServerError ex) { throw ex; }` clause ahead of the generic catch, so this
		// branch's specific "Could not resolve RequestDispatcher... NO_ENGINE_DIAGNOSTIC" message reaches
		// the response body intact instead of being re-wrapped into the generic "JSP render failed for"
		// message.
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
