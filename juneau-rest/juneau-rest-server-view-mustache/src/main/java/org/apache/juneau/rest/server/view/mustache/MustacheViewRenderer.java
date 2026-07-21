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
package org.apache.juneau.rest.server.view.mustache;

import java.io.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.view.*;

import com.github.mustachejava.*;

/**
 * {@link ResponseProcessor} that detects {@link MustacheView}-typed return values and asks the
 * configured {@link MustacheFactory} to render them directly onto the response writer.
 *
 * <p>
 * Auto-registered by {@link MustacheMixin} via
 * {@link Rest#responseProcessors() @Rest(responseProcessors=...)}
 * &mdash; callers who add the mixin don't need to wire up this class explicitly. Callers who want
 * to handle {@code MustacheView} returns <i>without</i> adopting the mixin can add this class to
 * their own
 * {@link Rest#responseProcessors() responseProcessors} list.
 *
 * <h5 class='section'>Behavior:</h5>
 *
 * <ol class='spaced-list'>
 * 	<li>Inspect the response content. If the value is not a {@link MustacheView}, return
 * 		{@link ResponseProcessor#NEXT NEXT} so the rest of the chain runs.
 * 	<li>Read the active {@link MustacheMixin} from the {@code RestContext} bean store to
 * 		discover the {@link MustacheFactory} (lazy default if no factory bean is registered) and
 * 		the optional template-suffix knob.
 * 	<li>Apply every entry from {@link MustacheView#getResponseHeaders()} via
 * 		{@link jakarta.servlet.http.HttpServletResponse#setHeader(String, String)
 * 		res.setHeader(...)}.
 * 	<li>Default {@code Content-Type} to {@code text/html;charset=UTF-8} if the caller did not set
 * 		one explicitly (Mustache is content-neutral, but HTML is overwhelmingly the most common
 * 		output target; an explicit caller header wins).
 * 	<li>Call {@code factory.compile(templateName).execute(res.getWriter(), view.getAttributes())}
 * 		to stream the rendered output.
 * 	<li>When no Mustache engine is on the classpath, the {@link MustacheFactory}-typed import
 * 		here fails to load at first use and surfaces {@link #NO_ENGINE_DIAGNOSTIC} naming the
 * 		missing dependency.
 * </ol>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MustacheMixin}
 * 	<li class='jc'>{@link MustacheView}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MustacheViewSupport">Mustache View Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeables here are framework-managed and not owned/closed by this class; not a real leak.
})
public class MustacheViewRenderer implements ViewRenderer {

	/** Default {@code Content-Type} applied when the view does not specify one explicitly. */
	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";

	/**
	 * Diagnostic message emitted when no Mustache engine can be found on the classpath at
	 * render-time.
	 *
	 * <p>
	 * Public so tests and consumer apps can pattern-match against the message text without
	 * depending on internal string literals.
	 */
	public static final String NO_ENGINE_DIAGNOSTIC =
		"""
		No Mustache engine is available on the classpath. Add:
		  - com.github.spullara.mustache.java:compiler   (mustache.java — the engine the bridge targets)
		Or register a custom @Bean MustacheFactory that picks up your preferred resolvers.
		Note: Spring Boot's spring-boot-starter-mustache ships com.samskivert:jmustache, NOT
		mustache.java; the bridge is mustache.java-specific. To use jmustache, supply your own
		ResponseProcessor instead of MustacheViewRenderer.
		See https://juneau.apache.org/docs/topics/MustacheViewSupport for the full matrix.""";

	// Lazily-created fallback used when no MustacheMixin bean is registered.  Cached (double-checked) so
	// the bridge-default MustacheFactory is built once instead of rebuilt per render.  Created lazily (not
	// eagerly) so a missing Mustache engine still surfaces at render time as NO_ENGINE_DIAGNOSTIC rather
	// than during RestContext construction.
	@SuppressWarnings({
		"java:S3077" // Publish-once cache: assigned once under double-checked locking in fallbackMixin(); MustacheMixin is fully built (effectively immutable — its only field is final) before assignment, so volatile safe-publication is sufficient.
	})
	private volatile MustacheMixin fallbackMixin;

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException, BasicHttpException {
		var req = opSession.getRequest();
		var res = opSession.getResponse();

		var content = res.getContent(Object.class);
		if (! (content instanceof MustacheView content2))
			return NEXT;

		// Resolve the bridge resource (carries MustacheFactory + cached default + templateSuffix).
		// Fall back to a cached MustacheMixin when the renderer is used standalone without
		// the mixin.
		var bridge = req.getContext().getBeanStore()
			.getBean(MustacheMixin.class)
			.orElseGet(this::fallbackMixin);

		// Apply caller-supplied response headers first so a caller-provided Content-Type wins
		// over the bridge's default below.
		content2.getResponseHeaders().forEach(res::setHeader);
		if (! res.containsHeader("Content-Type"))
			res.setHeader("Content-Type", DEFAULT_CONTENT_TYPE);

		// Apply the same traversal gate the raw /mustache/* mount uses so a typed-View template name
		// assembled from user input (e.g. MustacheView.of(userInput)) cannot escape the configured base
		// path via '../' segments.  Mirrors the JSP bridge's typed-View gate.
		String safeName;
		try {
			safeName = gateTemplateName(bridge.getBasePath(), content2.getTemplateName());
		} catch (IllegalArgumentException ex) {
			throw new InternalServerError(ex, "Mustache template name escapes configured base path: '%s'", content2.getTemplateName());
		}

		var templateName = bridge.applyTemplateSuffix(safeName);
		try {
			var factory = bridge.resolveMustacheFactory(req);
			var mustache = factory.compile(templateName);
			mustache.execute(res.getWriter(), content2.getAttributes()).flush();
			return FINISHED;
		} catch (LinkageError ex) {
			throw new InternalServerError(ex, NO_ENGINE_DIAGNOSTIC);
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError(ex, "Mustache render failed for '%s'", templateName);
		}
	}

	// Rejects '../'-style traversal in a typed-View template name using the same virtual-path gate the raw
	// mount applies, returning the configuration-relative template name.  Delegates to the shared
	// FileUtils.resolveVirtualPathSafely + MustacheDispatcher.stripBasePath used by the raw render path.
	static String gateTemplateName(String basePath, String templateName) {
		var resolved = FileUtils.resolveVirtualPathSafely(basePath, templateName);
		return MustacheDispatcher.stripBasePath(basePath, resolved);
	}

	// Returns the cached fallback mixin, creating it once (double-checked) on first standalone render.
	private MustacheMixin fallbackMixin() {
		var local = fallbackMixin;
		if (local == null) {
			synchronized (this) {
				local = fallbackMixin;
				if (local == null) {
					local = new MustacheMixin();
					fallbackMixin = local;
				}
			}
		}
		return local;
	}
}
