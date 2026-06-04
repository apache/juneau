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
package org.apache.juneau.rest.view.thymeleaf;

import java.io.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.view.*;
import org.thymeleaf.*;
import org.thymeleaf.context.*;

/**
 * {@link ResponseProcessor} that detects {@link ThymeleafView}-typed return values and asks the
 * configured {@link TemplateEngine} to render them directly onto the response writer.
 *
 * <p>
 * Auto-registered by {@link ThymeleafMixin} via
 * {@link org.apache.juneau.rest.annotation.Rest#responseProcessors() @Rest(responseProcessors=...)}
 * &mdash; callers who add the mixin don't need to wire up this class explicitly. Callers who want
 * to handle {@code ThymeleafView} returns <i>without</i> adopting the mixin can add this class to
 * their own
 * {@link org.apache.juneau.rest.annotation.Rest#responseProcessors() responseProcessors} list.
 *
 * <h5 class='section'>Behavior:</h5>
 *
 * <ol class='spaced-list'>
 * 	<li>Inspect the response content. If the value is not a {@link ThymeleafView}, return
 * 		{@link ResponseProcessor#NEXT NEXT} so the rest of the chain runs.
 * 	<li>Read the active {@link ThymeleafMixin} from the {@code RestContext} bean store to
 * 		discover the {@link TemplateEngine} (lazy default if no engine bean is registered).
 * 	<li>Build a Thymeleaf {@link Context} populated with the view's attribute map.
 * 	<li>Apply every entry from {@link ThymeleafView#getResponseHeaders()} via
 * 		{@link jakarta.servlet.http.HttpServletResponse#setHeader(String, String)
 * 		res.setHeader(...)}.
 * 	<li>Default {@code Content-Type} to {@code text/html;charset=UTF-8} if the caller did not set
 * 		one explicitly (Thymeleaf's HTML template mode emits HTML; an explicit caller header wins).
 * 	<li>Call {@code templateEngine.process(view.getTemplateName(), context, res.getWriter())} to
 * 		stream the rendered output.
 * 	<li>When no Thymeleaf engine is on the classpath, the {@code TemplateEngine}-typed import here
 * 		fails to load at first use and surfaces {@link #NO_ENGINE_DIAGNOSTIC} naming the missing
 * 		dependency.
 * </ol>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThymeleafMixin}
 * 	<li class='jc'>{@link ThymeleafView}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ThymeleafViewSupport">Thymeleaf View Support</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class ThymeleafViewRenderer implements ViewRenderer {

	/** Default {@code Content-Type} applied when the view does not specify one explicitly. */
	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";

	/**
	 * Diagnostic message emitted when no Thymeleaf engine can be found on the classpath at
	 * render-time.
	 *
	 * <p>
	 * Public so tests and consumer apps can pattern-match against the message text without
	 * depending on internal string literals.
	 */
	public static final String NO_ENGINE_DIAGNOSTIC = """
		No Thymeleaf engine is available on the classpath. Add one of:
		  - org.springframework.boot:spring-boot-starter-thymeleaf  (Spring Boot autoconfig)
		  - org.thymeleaf:thymeleaf                                  (Juneau microservice / Jetty)
		Or register a custom @Bean TemplateEngine that picks up your preferred resolvers.
		See https://juneau.apache.org/docs/topics/ThymeleafViewSupport for the full matrix.""";

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException, BasicHttpException {
		var req = opSession.getRequest();
		var res = opSession.getResponse();

		var content = res.getContent(Object.class);
		if (! (content instanceof ThymeleafView view))
			return NEXT;

		// Resolve the bridge resource (carries TemplateEngine + cached default). Fall back to a
		// fresh ThymeleafMixin when the renderer is used standalone without the mixin.
		var bridge = req.getContext().getBeanStore()
			.getBean(ThymeleafMixin.class)
			.orElseGet(ThymeleafMixin::new);

		// Apply caller-supplied response headers first so a caller-provided Content-Type wins
		// over the bridge's default below.
		view.getResponseHeaders().forEach(res::setHeader);
		if (! res.containsHeader("Content-Type"))
			res.setHeader("Content-Type", DEFAULT_CONTENT_TYPE);

		try {
			var engine = bridge.resolveTemplateEngine(req);
			var ctx = newContext(req, view);
			engine.process(view.getTemplateName(), ctx, res.getWriter());
			return FINISHED;
		} catch (LinkageError ex) {
			throw new InternalServerError(ex, NO_ENGINE_DIAGNOSTIC);
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError(ex, "Thymeleaf render failed for ''{0}''",
				view.getTemplateName());
		}
	}

	/**
	 * Builds a fresh Thymeleaf {@link Context} for the current request, populated with every
	 * entry from {@link ThymeleafView#getAttributes()}.
	 *
	 * <p>
	 * The {@code Context} inherits the request's {@link java.util.Locale Locale} so Thymeleaf's
	 * i18n machinery (`#messages`, `MessageSource`) picks it up automatically.
	 *
	 * @param req The current REST request.
	 * @param view The view being rendered.
	 * @return A new {@link Context} carrying the view's attributes.
	 */
	static Context newContext(RestRequest req, ThymeleafView view) {
		var ctx = new Context(req.getLocale());
		view.getAttributes().forEach(ctx::setVariable);
		return ctx;
	}
}
