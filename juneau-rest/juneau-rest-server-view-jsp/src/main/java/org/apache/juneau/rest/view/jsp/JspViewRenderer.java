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
package org.apache.juneau.rest.view.jsp;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.view.*;

/**
 * {@link ResponseProcessor} that detects {@link JspView}-typed return values and dispatches them
 * to the underlying JSP engine via
 * {@link jakarta.servlet.RequestDispatcher#forward(jakarta.servlet.ServletRequest,
 * jakarta.servlet.ServletResponse) ServletContext.getRequestDispatcher(...).forward(...)}.
 *
 * <p>
 * Auto-registered by {@link JspMixin} via
 * {@link org.apache.juneau.rest.annotation.Rest#responseProcessors() @Rest(responseProcessors=...)}
 * &mdash; callers who add the mixin don't need to wire up this class explicitly. Callers who want
 * to handle {@code JspView} returns <i>without</i> adopting the mixin (e.g. mounting their own
 * raw-JSP path) can add this class to their own
 * {@link org.apache.juneau.rest.annotation.Rest#responseProcessors() responseProcessors} list.
 *
 * <h5 class='section'>Behavior:</h5>
 *
 * <ol class='spaced-list'>
 * 	<li>Inspect the response content. If the value is not a {@link JspView}, return
 * 		{@link ResponseProcessor#NEXT NEXT} so the rest of the chain runs (the standard POJO
 * 		serializer / plain-text fallback handle non-{@code JspView} returns).
 * 	<li>Read the active {@link JspMixin} from the {@code RestContext} bean store to
 * 		discover the configured base path; fall back to {@code "/"} when none is registered (a
 * 		caller using the renderer standalone without the mixin).
 * 	<li>Copy every entry from {@link JspView#getAttributes()} onto the request as a request
 * 		attribute via {@link jakarta.servlet.ServletRequest#setAttribute(String, Object)
 * 		req.setAttribute(...)} so the JSP / JSTL EL pipeline can resolve them.
 * 	<li>Apply every entry from {@link JspView#getResponseHeaders()} via
 * 		{@link jakarta.servlet.http.HttpServletResponse#setHeader(String, String)
 * 		res.setHeader(...)}.
 * 	<li>Obtain the underlying {@link jakarta.servlet.ServletContext ServletContext},
 * 		{@code getRequestDispatcher(basePath + templateName)}, and call {@code forward(...)}.
 * 	<li>When no JSP engine is on the classpath, the dispatcher returns {@code null} (or forward
 * 		fails with {@code ClassNotFoundException}). Surface a human-readable diagnostic naming the
 * 		missing dependency and linking to the "Choosing a JSP engine" matrix.
 * </ol>
 *
 * <h5 class='section'>Note on {@code forward()}:</h5>
 *
 * <p>
 * {@link jakarta.servlet.RequestDispatcher#forward forward()} resets the response output stream.
 * If a {@link org.apache.juneau.rest.annotation.RestPreCall @RestPreCall} hook has already
 * written response headers/body before the renderer runs, the forward may fail or produce
 * malformed output. The renderer runs at response-resolution time so this is rarely hit by
 * accident; document the constraint for any user relying on {@code RestPreCall}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JspMixin}
 * 	<li class='jc'>{@link JspView}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JspViewSupport">JSP View Support</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class JspViewRenderer implements ViewRenderer {

	/**
	 * Diagnostic message emitted when no JSP engine can be found on the classpath at forward-time.
	 *
	 * <p>
	 * Public so tests and consumer apps can pattern-match against the message text without
	 * depending on internal string literals.
	 */
	public static final String NO_ENGINE_DIAGNOSTIC =
		"No JSP engine is available on the classpath. Add one of:\n"
		+ "  - org.eclipse.jetty.ee11:jetty-ee11-apache-jsp  (Jetty 12 EE11)\n"
		+ "  - org.apache.tomcat.embed:tomcat-embed-jasper  (embedded Tomcat / Spring Boot default)\n"
		+ "  - Or rely on the deployment container's bundled engine (Tomcat / JBoss / WildFly / ...)\n"
		+ "See https://juneau.apache.org/docs/topics/JspViewSupport for the full matrix.";

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException, BasicHttpException {
		var req = opSession.getRequest();
		var res = opSession.getResponse();

		var content = res.getContent(Object.class);
		if (! (content instanceof JspView view))
			return NEXT;

		// Resolve the base path. Prefer the registered JspMixin bean (which the mixin
		// instantiates), fall back to "/" when the renderer is used standalone.
		var basePath = req.getContext().getBeanStore()
			.getBean(JspMixin.class)
			.map(JspMixin::getBasePath)
			.orElse("/");

		// Copy attributes onto the request so the JSP / JSTL EL can resolve them.
		view.getAttributes().forEach(req::setAttribute);

		// Apply caller-supplied response headers (e.g. Content-Type, Cache-Control).
		view.getResponseHeaders().forEach(res::setHeader);

		// Resolve the dispatch path: basePath + templateName, normalized for a single separator.
		// A path that escapes basePath (e.g. template names assembled from user input that
		// included ../ segments) is rejected with InternalServerError — template names are
		// caller-controlled, so an escape attempt indicates a server-side bug, not a request-
		// side attack. (User-input flowing into JSP rendering is handled by JspMixin.render
		// which catches IAE and surfaces it as Forbidden.)
		String target;
		try {
			target = joinPath(basePath, view.getTemplateName());
		} catch (IllegalArgumentException ex) {
			throw new InternalServerError(ex, "JSP template name escapes configured base path: ''{0}''",
				view.getTemplateName());
		}

		// Find the JSP engine via the servlet context. Both forward() failure modes (missing
		// engine, dispatcher==null, ClassNotFoundException during forward) surface as a clear
		// diagnostic naming the missing dependency.
		try {
			var ctx = req.getServletContext();
			var rd = ctx.getRequestDispatcher(target);
			if (rd == null)
				throw new InternalServerError("Could not resolve RequestDispatcher for ''{0}''. {1}",
					target, NO_ENGINE_DIAGNOSTIC);
			rd.forward(req.getHttpServletRequest(), res.getHttpServletResponse());
			return FINISHED;
		} catch (NoClassDefFoundError ex) {
			throw new InternalServerError(ex, NO_ENGINE_DIAGNOSTIC);
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError(ex, "JSP render failed for ''{0}''", target);
		}
	}

	/**
	 * Joins a base path and a template name with a single {@code "/"} separator, rejecting any
	 * resolved target that escapes {@code basePath} via {@code ..} segments.
	 *
	 * <p>
	 * Delegates to {@link FileUtils#resolveVirtualPathSafely(String, String)} — the canonical
	 * shared implementation reused by {@code JspMixin.render(...)} for the
	 * raw-{@code .jsp} dispatch path. Handles every {@code (basePath, template)} combination of
	 * trailing/leading slashes uniformly so callers can pass {@code basePath("/WEB-INF/views")} or
	 * {@code basePath("/WEB-INF/views/")} and {@code "hello.jsp"} or {@code "/hello.jsp"} without
	 * worrying about double-slash artifacts.
	 *
	 * <p>
	 * A {@code null} or empty {@code basePath} is normalized to {@code "/"} (the
	 * {@link JspMixin#DEFAULT_BASE_PATH default}) before the boundary check runs.
	 *
	 * @param basePath The base path (e.g. {@code "/WEB-INF/views/"}).
	 * @param template The template name (e.g. {@code "hello.jsp"}).
	 * @return The joined path (e.g. {@code "/WEB-INF/views/hello.jsp"}).
	 * @throws IllegalArgumentException If {@code template} escapes {@code basePath} via
	 * 	{@code ..} segments.
	 */
	static String joinPath(String basePath, String template) {
		var bp = isEmpty(basePath) ? "/" : basePath;
		return FileUtils.resolveVirtualPathSafely(bp, template);
	}
}
