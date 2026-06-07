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
package org.apache.juneau.rest.server.view.freemarker;

import java.io.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.view.*;

import freemarker.template.*;

/**
 * {@link ResponseProcessor} that detects {@link FreemarkerView}-typed return values and asks the
 * configured {@link Configuration} to render them directly onto the response writer.
 *
 * <p>
 * Auto-registered by {@link FreemarkerMixin} via
 * {@link org.apache.juneau.rest.server.Rest#responseProcessors() @Rest(responseProcessors=...)}
 * &mdash; callers who add the mixin don't need to wire up this class explicitly. Callers who want
 * to handle {@code FreemarkerView} returns <i>without</i> adopting the mixin can add this class
 * to their own
 * {@link org.apache.juneau.rest.server.Rest#responseProcessors() responseProcessors} list.
 *
 * <h5 class='section'>Behavior:</h5>
 *
 * <ol class='spaced-list'>
 * 	<li>Inspect the response content. If the value is not a {@link FreemarkerView}, return
 * 		{@link ResponseProcessor#NEXT NEXT} so the rest of the chain runs.
 * 	<li>Read the active {@link FreemarkerMixin} from the {@code RestContext} bean store
 * 		to discover the {@link Configuration} (lazy default if no configuration bean is
 * 		registered) and the optional template-suffix knob.
 * 	<li>Apply every entry from {@link FreemarkerView#getResponseHeaders()} via
 * 		{@link jakarta.servlet.http.HttpServletResponse#setHeader(String, String)
 * 		res.setHeader(...)}.
 * 	<li>Default {@code Content-Type} to {@code text/html;charset=UTF-8} if the caller did not set
 * 		one explicitly. FreeMarker's bridge-default {@code Configuration} uses
 * 		{@code HTMLOutputFormat} so HTML is the natural target; an explicit caller header wins.
 * 	<li>Call {@code configuration.getTemplate(templateName).process(view.getAttributes(),
 * 		res.getWriter())} to stream the rendered output.
 * 	<li>When no FreeMarker engine is on the classpath, the {@link Configuration}-typed import
 * 		here fails to load at first use and surfaces {@link #NO_ENGINE_DIAGNOSTIC} naming the
 * 		missing dependency.
 * </ol>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FreemarkerMixin}
 * 	<li class='jc'>{@link FreemarkerView}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class FreemarkerViewRenderer implements ViewRenderer {

	/** Default {@code Content-Type} applied when the view does not specify one explicitly. */
	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";

	/**
	 * Diagnostic message emitted when no FreeMarker engine can be found on the classpath at
	 * render-time.
	 *
	 * <p>
	 * Public so tests and consumer apps can pattern-match against the message text without
	 * depending on internal string literals.
	 */
	public static final String NO_ENGINE_DIAGNOSTIC = """
		No Apache FreeMarker engine is available on the classpath. Add:
		  - org.freemarker:freemarker                       (FreeMarker engine core)
		Or, for Spring Boot:
		  - org.springframework.boot:spring-boot-starter-freemarker
		    (autoconfigures a freemarker.template.Configuration bean the bridge picks up).
		Or register a custom @Bean freemarker.template.Configuration with whatever loaders /
		encodings / output formats you need.
		See https://juneau.apache.org/docs/topics/FreemarkerViewSupport for the full matrix.""";

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException, BasicHttpException {
		var req = opSession.getRequest();
		var res = opSession.getResponse();

		var content = res.getContent(Object.class);
		if (! (content instanceof FreemarkerView view))
			return NEXT;

		// Resolve the bridge resource (carries Configuration + cached default + templateSuffix).
		// Fall back to a fresh FreemarkerMixin when the renderer is used standalone
		// without the mixin.
		var bridge = req.getContext().getBeanStore()
			.getBean(FreemarkerMixin.class)
			.orElseGet(FreemarkerMixin::new);

		// Apply caller-supplied response headers first so a caller-provided Content-Type wins
		// over the bridge's default below.
		view.getResponseHeaders().forEach(res::setHeader);
		if (! res.containsHeader("Content-Type"))
			res.setHeader("Content-Type", DEFAULT_CONTENT_TYPE);

		var templateName = bridge.applyTemplateSuffix(view.getTemplateName());
		try {
			var cfg = bridge.resolveConfiguration(req);
			var template = cfg.getTemplate(templateName);
			template.process(view.getAttributes(), res.getWriter());
			res.getWriter().flush();
			return FINISHED;
		} catch (LinkageError ex) {
			throw new InternalServerError(ex, NO_ENGINE_DIAGNOSTIC);
		} catch (IOException ex) {
			throw ex;
		} catch (TemplateException | RuntimeException ex) {
			throw new InternalServerError(ex, "FreeMarker render failed for ''{0}''", templateName);
		}
	}
}
