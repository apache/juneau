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
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Covers {@link JspDispatcher#render render(...)}'s dispatch-succeeded and dispatch-failed branches
 * (the {@code rd != null} half of the {@code rd == null} check, plus every {@code catch} clause around
 * {@code rd.forward(...)}).
 *
 * <p>
 * {@link JspMixin_MockRest_Test} exercises a 500 with no custom wiring at all, but MockRest's default
 * {@code HttpServletRequest#getServletContext()} returns {@code null} outright (no container is present),
 * so that 500 actually comes from the generic {@code catch (Exception ex)} clause reacting to the
 * resulting {@code NullPointerException} on {@code ctx.getRequestDispatcher(...)} — not from the
 * {@code rd == null} branch's own dedicated diagnostic a few lines below. {@link #a00} here hits that
 * specific branch directly. This class swaps in a hand-rolled {@link ServletContext} (via
 * {@link MockRestRequest#servletContext} — copied onto the underlying {@code HttpServletRequest} for one
 * call) whose {@code getRequestDispatcher(...)} returns a {@link RequestDispatcher} test double that
 * forwards successfully or throws each of the exception types {@code render(...)} specifically catches,
 * without needing a real JSP engine or servlet container.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class JspDispatcher_ForwardPaths_Test extends TestBase {

	@Rest(mixins=JspMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

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
		@Override public void include(ServletRequest req, ServletResponse res) { /* not exercised by render(...) */ }
	}

	/**
	 * Builds a {@link ServletContext} whose only meaningful behavior is
	 * {@code getRequestDispatcher(String)} returning {@code dispatcher} (or {@code null}, to pin the
	 * already-covered {@code rd == null} branch via the same mechanism as a sanity check). Every other
	 * {@link ServletContext} method is never called by {@code render(...)}, so a {@link Proxy} that
	 * returns {@code null} for anything else is sufficient without hand-implementing the interface's
	 * several dozen other methods.
	 */
	private static ServletContext fakeServletContext(RequestDispatcher dispatcher) {
		return (ServletContext) Proxy.newProxyInstance(
			JspDispatcher_ForwardPaths_Test.class.getClassLoader(),
			new Class<?>[] { ServletContext.class },
			(proxy, method, args) -> "getRequestDispatcher".equals(method.getName()) ? dispatcher : null);
	}

	@Test void a00_contextPresentButDispatcherNull_reportsNoEngineDiagnostic() throws Exception {
		// A real (non-null) ServletContext that itself resolves the target to no dispatcher -- the
		// documented rd == null branch, distinct from JspMixin_MockRest_Test's NPE-driven 500 (see class
		// javadoc above). render(...) now has a dedicated `catch (InternalServerError ex) { throw ex; }`
		// clause ahead of the generic catch, so this branch's specific "Could not resolve
		// RequestDispatcher... NO_ENGINE_DIAGNOSTIC" message reaches the response body intact instead of
		// being re-wrapped into the generic "JSP render failed for" message.
		var ctx = fakeServletContext(null);
		var res = c.get("/jsp/hello.jsp").servletContext(ctx).run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("Could not resolve RequestDispatcher");
		res.assertContent().asString().isContains("No JSP engine is available on the classpath");
	}

	@Test void a01_dispatcherFound_forwardSucceeds_returns200() throws Exception {
		var ctx = fakeServletContext(new FakeDispatcher(() -> { /* no-op: a well-behaved engine committed the response itself */ }));
		c.get("/jsp/hello.jsp").servletContext(ctx).run().assertStatus(200);
	}

	@Test void a02_dispatcherFound_forwardThrowsIOException_propagatesAs500() throws Exception {
		// IOException is declared by render(...) and re-thrown as-is (not wrapped) — the framework's
		// default exception-to-status mapping for an undeclared-HTTP-status checked exception is 500.
		var ctx = fakeServletContext(new FakeDispatcher(() -> { throw new IOException("disk full"); }));
		c.get("/jsp/hello.jsp").servletContext(ctx).run().assertStatus(500);
	}

	@Test void a03_dispatcherFound_forwardThrowsNotFound_propagatesAs404() throws Exception {
		// NotFound is caught alongside IOException and re-thrown as-is (not wrapped) — its own status
		// code (404) drives the response, same as if the caller had thrown it directly.
		var ctx = fakeServletContext(new FakeDispatcher(() -> { throw new NotFound("no such template"); }));
		c.get("/jsp/hello.jsp").servletContext(ctx).run().assertStatus(404);
	}

	@Test void a04_dispatcherFound_forwardThrowsNoClassDefFoundError_wrappedWithEngineDiagnostic() throws Exception {
		// A JSP engine class missing at runtime (e.g. the api jar present but no impl on the classpath)
		// surfaces as NoClassDefFoundError from inside forward() -- wrapped with the same
		// NO_ENGINE_DIAGNOSTIC text as the rd == null case, rather than a bare 500.
		var ctx = fakeServletContext(new FakeDispatcher(() -> { throw new NoClassDefFoundError("org.apache.jasper.JspCompilationContext"); }));
		var res = c.get("/jsp/hello.jsp").servletContext(ctx).run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("jetty-ee11-apache-jsp");
	}

	@Test void a05_dispatcherFound_forwardThrowsOtherException_wrappedAs500() throws Exception {
		// Any other exception from forward() (e.g. a ServletException from a failing JSP compile) falls
		// through to the generic catch and is wrapped with the target path in the message, distinct from
		// both the IOException/NotFound passthrough and the NoClassDefFoundError engine diagnostic.
		var ctx = fakeServletContext(new FakeDispatcher(() -> { throw new ServletException("compile error"); }));
		var res = c.get("/jsp/hello.jsp").servletContext(ctx).run();
		res.assertStatus(500);
		res.assertContent().asString().isContains("JSP render failed for");
	}
}
