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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.view.*;
import org.thymeleaf.*;
import org.thymeleaf.templatemode.*;
import org.thymeleaf.templateresolver.*;

/**
 * Flavor-neutral worker bean carrying the Thymeleaf raw-template-dispatch logic shared by the
 * {@link ThymeleafMixin} mixin, the {@link ThymeleafServlet} servlet, and the
 * {@link ThymeleafResource} child flavors.
 *
 * <p>
 * This is the §2.3.1 <i>flavor-neutral worker</i> for the Thymeleaf capability: it is <b>not</b> one
 * of the three {@code @Rest} flavor classes &mdash; it is a plain bean that implements
 * {@link RawTemplateDispatcher} and holds the capability state ({@code basePath} /
 * {@code cacheTemplates} / {@code templateMode} / lazy default engine) plus the raw-serve and
 * engine-resolution logic. Each flavor independently holds an instance of this worker and delegates
 * to it, so no flavor is another flavor's worker.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThymeleafMixin}
 * 	<li class='jc'>{@link ThymeleafServlet}
 * 	<li class='jc'>{@link ThymeleafResource}
 * 	<li class='jic'>{@link RawTemplateDispatcher}
 * </ul>
 *
 * @since 9.5.0
 */
public class ThymeleafDispatcher implements RawTemplateDispatcher {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = "/";

	/** Default template-cache flag &mdash; production-safe; opt out per-builder for dev. */
	public static final boolean DEFAULT_CACHE_TEMPLATES = true;

	/** Default template mode for the bridge's fallback engine. */
	public static final TemplateMode DEFAULT_TEMPLATE_MODE = TemplateMode.HTML;

	private final String basePath;
	private final boolean cacheTemplates;
	private final TemplateMode templateMode;

	// Lazy bridge-default engine. Built on first call to resolveTemplateEngine(...) when no
	// TemplateEngine bean is registered in the request's BeanStore. Volatile so the
	// double-checked-locking idiom is safe under concurrent first-request load.
	private volatile TemplateEngine defaultEngine;

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
	protected ThymeleafDispatcher(Builder builder) {
		basePath = builder.basePath;
		cacheTemplates = builder.cacheTemplates;
		templateMode = builder.templateMode;
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
	 * Returns whether the bridge's default engine caches resolved templates.
	 *
	 * @return The cache flag. Defaults to {@link #DEFAULT_CACHE_TEMPLATES}.
	 */
	public boolean isCacheTemplates() {
		return cacheTemplates;
	}

	/**
	 * Returns the template mode used by the bridge's default engine.
	 *
	 * @return The template mode. Defaults to {@link #DEFAULT_TEMPLATE_MODE}.
	 */
	public TemplateMode getTemplateMode() {
		return templateMode;
	}

	/**
	 * Resolves the active {@link TemplateEngine}.
	 *
	 * <p>
	 * Lookup order:
	 * <ol class='spaced-list'>
	 * 	<li>{@code req.getContext().getBeanStore().getBean(TemplateEngine.class)} &mdash; covers
	 * 		Spring Boot autoconfig (`SpringTemplateEngine` is-a `TemplateEngine`) and any
	 * 		user-supplied bean.
	 * 	<li>Lazy bridge default &mdash; constructed on first call when no engine bean is
	 * 		registered. Carries a single {@link ClassLoaderTemplateResolver} keyed off
	 * 		({@link #getBasePath() basePath}, {@code .html}, {@link #getTemplateMode() templateMode},
	 * 		{@link #isCacheTemplates() cacheTemplates}).
	 * </ol>
	 *
	 * @param req The current REST request.
	 * @return The active template engine. Never {@code null}.
	 */
	public TemplateEngine resolveTemplateEngine(RestRequest req) {
		var bean = req.getContext().getBeanStore().getBean(TemplateEngine.class);
		if (bean.isPresent())
			return bean.get();
		var local = defaultEngine;
		if (local == null) {
			synchronized (this) {
				local = defaultEngine;
				if (local == null) {
					local = buildDefaultEngine();
					defaultEngine = local;
				}
			}
		}
		return local;
	}

	/**
	 * Constructs the bridge-default {@link TemplateEngine}.
	 *
	 * <p>
	 * Single {@link ClassLoaderTemplateResolver} with {@code prefix=basePath},
	 * {@code suffix=".html"}, {@code templateMode=templateMode}, and
	 * {@code cacheable=cacheTemplates}. Subclasses may override to plug in custom resolvers /
	 * dialects without registering a separate {@code @Bean TemplateEngine}.
	 *
	 * @return A new {@link TemplateEngine} instance.
	 */
	protected TemplateEngine buildDefaultEngine() {
		var resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix(basePath);
		resolver.setSuffix(".html");
		resolver.setTemplateMode(templateMode);
		resolver.setCacheable(cacheTemplates);
		resolver.setCharacterEncoding("UTF-8");
		var engine = new TemplateEngine();
		engine.setTemplateResolver(resolver);
		return engine;
	}

	/**
	 * Renders a raw template through the Thymeleaf engine onto the response.
	 *
	 * @param path The trailing path segment after the mount prefix (the template name relative to
	 * 	the configured {@link #getBasePath() base path}; trailing {@code .html} stripped).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 * @throws BasicHttpException On boundary violation (403), missing engine (500), or render
	 * 	failure (500).
	 */
	@Override /* RawTemplateDispatcher */
	public void render(String path, RestRequest req, RestResponse res)
			throws IOException, BasicHttpException {

		// The engine resolver re-adds the .html suffix; strip if present so callers can request
		// /thymeleaf/hello, /thymeleaf/hello.html, or /thymeleaf/admin/dashboard.html uniformly.
		var template = (path == null) ? "" : path;
		if (template.endsWith(".html"))
			template = template.substring(0, template.length() - ".html".length());

		// resolveVirtualPathSafely validates that the resolved virtual path stays inside the
		// configured basePath. The traversal check operates on basePath + template so a template
		// name like "../../etc/passwd" or "a/b/../../../secret" is rejected before reaching the
		// engine. We re-derive the engine-relative template name from the safe result by
		// stripping the (already-normalized) basePath prefix.
		String safeTemplate;
		try {
			var resolved = FileUtils.resolveVirtualPathSafely(basePath, template);
			safeTemplate = stripBasePath(basePath, resolved);
		} catch (@SuppressWarnings("unused") IllegalArgumentException ex) {
			throw new Forbidden("Path escapes configured base path.");
		}

		try {
			var engine = resolveTemplateEngine(req);
			var ctx = new org.thymeleaf.context.Context(req.getLocale());
			if (! res.containsHeader("Content-Type"))
				res.setHeader("Content-Type", ThymeleafViewRenderer.DEFAULT_CONTENT_TYPE);
			engine.process(safeTemplate, ctx, res.getWriter());
		} catch (LinkageError ex) {
			throw new InternalServerError(ex, ThymeleafViewRenderer.NO_ENGINE_DIAGNOSTIC);
		} catch (IOException ex) {
			throw ex;
		} catch (BasicHttpException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError(ex, "Thymeleaf render failed for ''{0}''", safeTemplate);
		}
	}

	/**
	 * Strips the normalized {@code basePath} prefix from a {@code resolveVirtualPathSafely}
	 * result so the leftover string is engine-relative (the resolver re-adds the configured
	 * prefix + suffix).
	 *
	 * <p>
	 * Normalization mirrors {@code FileUtils.resolveVirtualPathSafely}: a {@code null} / empty
	 * base normalizes to {@code "/"}; otherwise the base is guaranteed to start with {@code "/"}
	 * and end with {@code "/"} once the helper has normalized it.
	 *
	 * @param base The configured base path (typically {@code "/templates/"}).
	 * @param resolved The output of {@code resolveVirtualPathSafely} (always starts with the
	 * 	normalized base).
	 * @return The engine-relative template name (e.g. {@code "hello"} for base
	 * 	{@code "/templates/"} and resolved {@code "/templates/hello"}).
	 */
	static String stripBasePath(String base, String resolved) {
		var bp = isEmpty(base) ? "/" : base;
		if (! bp.endsWith("/"))
			bp = bp + "/";
		if (! bp.startsWith("/"))
			bp = "/" + bp;
		if (resolved.startsWith(bp))
			return resolved.substring(bp.length());
		throw illegalArg("Resolved path ''{0}'' does not start with base ''{1}''", resolved, bp);
	}

	/**
	 * Builder for {@link ThymeleafDispatcher}.
	 */
	public static class Builder {

		String basePath = DEFAULT_BASE_PATH;
		boolean cacheTemplates = DEFAULT_CACHE_TEMPLATES;
		TemplateMode templateMode = DEFAULT_TEMPLATE_MODE;

		/** Constructor &mdash; package access for {@link ThymeleafDispatcher#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link ThymeleafDispatcher#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			basePath = isBlank(value) ? DEFAULT_BASE_PATH : value;
			return this;
		}

		/**
		 * Sets whether the bridge's default engine caches resolved templates.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder cacheTemplates(boolean value) {
			cacheTemplates = value;
			return this;
		}

		/**
		 * Sets the template mode used by the bridge's default engine.
		 *
		 * @param value The new value. {@code null} resets to {@link #DEFAULT_TEMPLATE_MODE}.
		 * @return This object.
		 */
		public Builder templateMode(TemplateMode value) {
			templateMode = (value == null) ? DEFAULT_TEMPLATE_MODE : value;
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
		 * Reads the current cache-templates setting (test/inspection helper).
		 *
		 * @return The cache flag.
		 */
		public boolean isCacheTemplates() {
			return cacheTemplates;
		}

		/**
		 * Reads the current template-mode setting (test/inspection helper).
		 *
		 * @return The template mode. Never {@code null}.
		 */
		public TemplateMode getTemplateMode() {
			return templateMode;
		}

		/**
		 * Builds the {@link ThymeleafDispatcher}.
		 *
		 * @return A new {@link ThymeleafDispatcher} instance.
		 */
		public ThymeleafDispatcher build() {
			if (basePath == null)
				throw illegalArg("basePath must not be null");
			return new ThymeleafDispatcher(this);
		}
	}
}
