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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

import com.github.mustachejava.*;

/**
 * Flavor-neutral worker bean carrying the Mustache raw-template-dispatch logic shared by the
 * {@link MustacheMixin} mixin, the {@link MustacheServlet} servlet, and the {@link MustacheResource}
 * child flavors.
 *
 * <p>
 * This is the §2.3.1 <i>flavor-neutral worker</i> for the Mustache capability: it is <b>not</b> one
 * of the three {@code @Rest} flavor classes &mdash; it is a plain bean that implements
 * {@link RawTemplateDispatcher} and holds the capability state ({@code basePath} /
 * {@code templateSuffix} / lazy default factory) plus the raw-serve and factory-resolution logic.
 * Each flavor independently holds an instance of this worker and delegates to it, so no flavor is
 * another flavor's worker.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MustacheMixin}
 * 	<li class='jc'>{@link MustacheServlet}
 * 	<li class='jc'>{@link MustacheResource}
 * 	<li class='jic'>{@link RawTemplateDispatcher}
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeables here are framework-managed and not owned/closed by this class; not a real leak.
})
public class MustacheDispatcher implements RawTemplateDispatcher {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = "/";

	/** Default template suffix &mdash; empty (literal template names, no implicit suffix). */
	public static final String DEFAULT_TEMPLATE_SUFFIX = "";

	private final String basePath;
	private final String templateSuffix;

	// Lazy bridge-default factory. Built on first call to resolveMustacheFactory(...) when no
	// MustacheFactory bean is registered in the request's BeanStore. Volatile so the
	// double-checked-locking idiom is safe under concurrent first-request load.
	@SuppressWarnings({
		"java:S3077" // Publish-once cache: assigned once under double-checked locking in resolveMustacheFactory(); the MustacheFactory is fully built before assignment, so volatile safe-publication is sufficient.
	})
	private volatile MustacheFactory defaultFactory;

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected MustacheDispatcher(Builder builder) {
		basePath = builder.basePath;
		templateSuffix = builder.templateSuffix;
	}

	/**
	 * Returns the base path under which template resources are resolved.
	 *
	 * @return The base path. Never {@code null}.
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Returns the configured template-name suffix.
	 *
	 * @return The template suffix. Never {@code null}.
	 */
	public String getTemplateSuffix() {
		return templateSuffix;
	}

	/**
	 * Appends {@link #getTemplateSuffix()} to {@code name} if not already present (idempotent).
	 *
	 * @param name The template name. Must not be {@code null}.
	 * @return The template name with the configured suffix appended (if applicable).
	 */
	public String applyTemplateSuffix(String name) {
		if (isEmpty(templateSuffix))
			return name;
		if (name.endsWith(templateSuffix))
			return name;
		return name + templateSuffix;
	}

	/**
	 * Resolves the active {@link MustacheFactory}.
	 *
	 * <p>
	 * Lookup order:
	 * <ol class='spaced-list'>
	 * 	<li>{@code req.getContext().getBeanStore().getBean(MustacheFactory.class)} &mdash; any
	 * 		user-supplied bean (Spring {@code @Bean}, microservice {@code BasicBeanStore.put},
	 * 		etc.).
	 * 	<li>Lazy bridge default &mdash; constructed on first call when no factory bean is
	 * 		registered. Carries a {@link DefaultMustacheFactory} anchored on a resource root
	 * 		derived from {@link #getBasePath() basePath} (leading + trailing slashes trimmed).
	 * </ol>
	 *
	 * @param req The current REST request.
	 * @return The active Mustache factory. Never {@code null}.
	 */
	public MustacheFactory resolveMustacheFactory(RestRequest req) {
		var bean = req.getContext().getBeanStore().getBean(MustacheFactory.class);
		if (bean.isPresent())
			return bean.get();
		var local = defaultFactory;
		if (local == null) {
			synchronized (this) {
				local = defaultFactory;
				if (local == null) {
					local = buildDefaultFactory();
					defaultFactory = local;
				}
			}
		}
		return local;
	}

	/**
	 * Constructs the bridge-default {@link MustacheFactory}.
	 *
	 * <p>
	 * {@link DefaultMustacheFactory} anchored on a classpath resource root derived from
	 * {@link #getBasePath() basePath} (leading + trailing slashes trimmed). A {@code "/"} base
	 * yields a no-prefix factory. Subclasses may override to plug in custom resolvers / object
	 * handlers without registering a separate {@code @Bean MustacheFactory}.
	 *
	 * @return A new {@link MustacheFactory} instance.
	 */
	protected MustacheFactory buildDefaultFactory() {
		var root = toResourceRoot(basePath);
		return root.isEmpty() ? new DefaultMustacheFactory() : new DefaultMustacheFactory(root);
	}

	/**
	 * Translates a virtual base path (e.g. {@code "/templates/"}) into a mustache.java resource
	 * root (e.g. {@code "templates"}).
	 *
	 * <p>
	 * mustache.java's {@link DefaultMustacheFactory#DefaultMustacheFactory(String)} expects a
	 * classpath-relative root without a leading slash and treats a trailing slash as part of the
	 * resolved path, so we trim both ends. A {@code null} / blank / {@code "/"} base yields an
	 * empty string &mdash; meaning "no prefix" (factory resolves names directly).
	 *
	 * @param base The virtual base path.
	 * @return The mustache.java resource root (never {@code null}).
	 */
	static String toResourceRoot(String base) {
		if (isBlank(base))
			return "";
		var s = base;
		while (s.startsWith("/"))
			s = s.substring(1);
		while (s.endsWith("/"))
			s = s.substring(0, s.length() - 1);
		return s;
	}

	/**
	 * Renders a raw template through the Mustache engine onto the response.
	 *
	 * @param path The trailing path segment after the mount prefix (the template name relative to
	 * 	the configured {@link #getBasePath() base path}; configured suffix appended if missing).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 * @throws BasicHttpException On boundary violation (403), missing engine (500), or render
	 * 	failure (500).
	 */
	@Override /* RawTemplateDispatcher */
	public void render(String path, RestRequest req, RestResponse res)
			throws IOException, BasicHttpException {

		var template = (path == null) ? "" : path;

		// resolveVirtualPathSafely validates that the resolved virtual path stays inside the
		// configured basePath. The traversal check operates on basePath + template so a template
		// name like "../../etc/passwd" or "a/b/../../../secret" is rejected before reaching the
		// engine. We re-derive the factory-relative template name from the safe result by
		// stripping the (already-normalized) basePath prefix.
		String safeTemplate;
		try {
			var resolved = FileUtils.resolveVirtualPathSafely(basePath, template);
			safeTemplate = stripBasePath(basePath, resolved);
		} catch (IllegalArgumentException ex) {
			throw new Forbidden("Path escapes configured base path.");
		}

		// Apply suffix idempotently so /mustache/about resolves to about.mustache (when
		// templateSuffix is configured), while /mustache/about.mustache stays as-is.
		safeTemplate = applyTemplateSuffix(safeTemplate);

		try {
			var factory = resolveMustacheFactory(req);
			if (! res.containsHeader("Content-Type"))
				res.setHeader("Content-Type", MustacheViewRenderer.DEFAULT_CONTENT_TYPE);
			var mustache = factory.compile(safeTemplate);
			mustache.execute(res.getWriter(), Map.of()).flush();
		} catch (LinkageError ex) {
			throw new InternalServerError(ex, MustacheViewRenderer.NO_ENGINE_DIAGNOSTIC);
		} catch (IOException | BasicHttpException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError(ex, "Mustache render failed for ''{0}''", safeTemplate);
		}
	}

	/**
	 * Strips the normalized {@code basePath} prefix from a {@code resolveVirtualPathSafely}
	 * result so the leftover string is factory-relative (the bridge's default factory re-adds
	 * the configured resource-root prefix).
	 *
	 * <p>
	 * Normalization mirrors {@code FileUtils.resolveVirtualPathSafely}: a {@code null} / empty
	 * base normalizes to {@code "/"}; otherwise the base is guaranteed to start with {@code "/"}
	 * and end with {@code "/"} once the helper has normalized it.
	 *
	 * @param base The configured base path (typically {@code "/templates/"}).
	 * @param resolved The output of {@code resolveVirtualPathSafely} (always starts with the
	 * 	normalized base).
	 * @return The factory-relative template name (e.g. {@code "hello.mustache"} for base
	 * 	{@code "/templates/"} and resolved {@code "/templates/hello.mustache"}).
	 */
	static String stripBasePath(String base, String resolved) {
		var bp = isEmpty(base) ? "/" : base;
		if (! bp.endsWith("/"))
			bp = bp + "/";
		if (! bp.startsWith("/"))
			bp = "/" + bp;
		if (resolved.startsWith(bp))
			return resolved.substring(bp.length());
		throw iaex("Resolved path ''{0}'' does not start with base ''{1}''", resolved, bp);
	}

	/**
	 * Builder for {@link MustacheDispatcher}.
	 */
	public static class Builder {

		String basePath = DEFAULT_BASE_PATH;
		String templateSuffix = DEFAULT_TEMPLATE_SUFFIX;

		/** Constructor &mdash; package access for {@link MustacheDispatcher#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link MustacheDispatcher#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			basePath = isBlank(value) ? DEFAULT_BASE_PATH : value;
			return this;
		}

		/**
		 * Sets the suffix to append to template names that don't already end with it (idempotent).
		 *
		 * @param value The new suffix. {@code null} resets to
		 * 	{@link MustacheDispatcher#DEFAULT_TEMPLATE_SUFFIX}.
		 * @return This object.
		 */
		public Builder templateSuffix(String value) {
			templateSuffix = (value == null) ? DEFAULT_TEMPLATE_SUFFIX : value;
			return this;
		}

		/**
		 * Reads the current base path setting (test/inspection helper).
		 *
		 * @return The base path. Never {@code null}.
		 */
		public String getBasePath() {
			return basePath;
		}

		/**
		 * Reads the current template-suffix setting (test/inspection helper).
		 *
		 * @return The template suffix. Never {@code null}.
		 */
		public String getTemplateSuffix() {
			return templateSuffix;
		}

		/**
		 * Builds the {@link MustacheDispatcher}.
		 *
		 * @return A new {@link MustacheDispatcher} instance.
		 */
		public MustacheDispatcher build() {
			if (basePath == null)
				throw iaex("basePath must not be null");
			return new MustacheDispatcher(this);
		}
	}
}
