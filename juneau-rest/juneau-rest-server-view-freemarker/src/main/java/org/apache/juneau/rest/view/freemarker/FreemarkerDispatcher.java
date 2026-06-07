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
package org.apache.juneau.rest.view.freemarker;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.view.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * Flavor-neutral worker bean carrying the FreeMarker raw-template-dispatch logic shared by the
 * {@link FreemarkerMixin} mixin, the {@link FreemarkerServlet} servlet, and the
 * {@link FreemarkerResource} child flavors.
 *
 * <p>
 * This is the §2.3.1 <i>flavor-neutral worker</i> for the FreeMarker capability: it is <b>not</b> one
 * of the three {@code @Rest} flavor classes &mdash; it is a plain bean that implements
 * {@link RawTemplateDispatcher} and holds the capability state ({@code basePath} /
 * {@code templateSuffix} / {@code cacheTemplates} / lazy default configuration) plus the raw-serve
 * and configuration-resolution logic. Each flavor independently holds an instance of this worker and
 * delegates to it, so no flavor is another flavor's worker.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FreemarkerMixin}
 * 	<li class='jc'>{@link FreemarkerServlet}
 * 	<li class='jc'>{@link FreemarkerResource}
 * 	<li class='jic'>{@link RawTemplateDispatcher}
 * </ul>
 *
 * @since 10.0.0
 */
public class FreemarkerDispatcher implements RawTemplateDispatcher {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = "/";

	/** Default template suffix &mdash; empty (literal template names, no implicit suffix). */
	public static final String DEFAULT_TEMPLATE_SUFFIX = "";

	/** Default template-cache flag &mdash; {@code true} (production-safe). */
	public static final boolean DEFAULT_CACHE_TEMPLATES = true;

	private final String basePath;
	private final String templateSuffix;
	private final boolean cacheTemplates;

	// Lazy bridge-default configuration. Built on first call to resolveConfiguration(...) when
	// no Configuration bean is registered in the request's BeanStore. Volatile so the
	// double-checked-locking idiom is safe under concurrent first-request load.
	@SuppressWarnings({
		"java:S3077" // volatile is required here for correct double-checked-locking safe-publication of the lazily-built default Configuration in resolveConfiguration(); the reference is publish-once (fully constructed before assignment) and never compound-mutated.
	})
	private volatile Configuration defaultConfiguration;

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
	protected FreemarkerDispatcher(Builder builder) {
		basePath = builder.basePath;
		templateSuffix = builder.templateSuffix;
		cacheTemplates = builder.cacheTemplates;
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
	 * Returns the template-cache flag.
	 *
	 * @return The cache flag.
	 */
	public boolean isCacheTemplates() {
		return cacheTemplates;
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
	 * Resolves the active {@link Configuration}.
	 *
	 * <p>
	 * Lookup order:
	 * <ol class='spaced-list'>
	 * 	<li>{@code req.getContext().getBeanStore().getBean(Configuration.class)} &mdash; any
	 * 		user-supplied bean (Spring {@code @Bean} including Spring Boot autoconfig, microservice
	 * 		{@code BasicBeanStore.put}, etc.).
	 * 	<li>Lazy bridge default &mdash; constructed on first call when no configuration bean is
	 * 		registered. Anchored on a classpath resource root derived from
	 * 		{@link #getBasePath() basePath}, with {@code IncompatibleImprovements} pinned to the
	 * 		bridge-tested minor version.
	 * </ol>
	 *
	 * @param req The current REST request.
	 * @return The active FreeMarker configuration. Never {@code null}.
	 */
	public Configuration resolveConfiguration(RestRequest req) {
		var bean = req.getContext().getBeanStore().getBean(Configuration.class);
		if (bean.isPresent())
			return bean.get();
		var local = defaultConfiguration;
		if (local == null) {
			synchronized (this) {
				local = defaultConfiguration;
				if (local == null) {
					local = buildDefaultConfiguration();
					defaultConfiguration = local;
				}
			}
		}
		return local;
	}

	/**
	 * Constructs the bridge-default {@link Configuration}.
	 *
	 * <p>
	 * Anchored on a classpath resource root derived from {@link #getBasePath() basePath} (leading +
	 * trailing slashes trimmed; a {@code "/"} base yields a root-of-classpath resolver). The default
	 * pins {@code IncompatibleImprovements} to {@link Configuration#VERSION_2_3_34} so behavior is
	 * stable across consumer upgrades of {@code org.freemarker:freemarker}; sets
	 * {@code DefaultEncoding} to {@code UTF-8} and {@code OutputFormat} to
	 * {@link HTMLOutputFormat#INSTANCE} so HTML escaping is the natural target; and applies
	 * {@code TemplateUpdateDelayMilliseconds} per the configured
	 * {@link #isCacheTemplates() cacheTemplates} flag.
	 *
	 * <p>
	 * Subclasses may override to plug in custom loaders / encodings / output formats without
	 * registering a separate {@code @Bean Configuration}.
	 *
	 * @return A new {@link Configuration} instance.
	 */
	protected Configuration buildDefaultConfiguration() {
		var cfg = new Configuration(Configuration.VERSION_2_3_34);
		cfg.setClassLoaderForTemplateLoading(FreemarkerDispatcher.class.getClassLoader(), toResourceRoot(basePath));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
		cfg.setTemplateUpdateDelayMilliseconds(cacheTemplates ? Long.MAX_VALUE : 0L);
		return cfg;
	}

	/**
	 * Translates a virtual base path (e.g. {@code "/templates/"}) into a FreeMarker
	 * {@code ClassLoaderTemplateLoader} resource root (e.g. {@code "/templates"}).
	 *
	 * <p>
	 * FreeMarker's
	 * {@link Configuration#setClassLoaderForTemplateLoading(ClassLoader, String)
	 * setClassLoaderForTemplateLoading(ClassLoader, String basePackagePath)} expects a
	 * classpath-relative root with a leading {@code "/"}. We trim any trailing slash and ensure a
	 * single leading slash; a {@code null} / blank / {@code "/"} base yields {@code "/"} (root of
	 * classpath).
	 *
	 * @param base The virtual base path.
	 * @return The FreeMarker classloader resource root (never {@code null}; always starts with
	 * 	{@code "/"}).
	 */
	static String toResourceRoot(String base) {
		if (isBlank(base))
			return "/";
		var s = base;
		while (s.endsWith("/") && s.length() > 1)
			s = s.substring(0, s.length() - 1);
		if (! s.startsWith("/"))
			s = "/" + s;
		return s;
	}

	/**
	 * Renders a raw template through the FreeMarker engine onto the response.
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
		// engine. We re-derive the configuration-relative template name from the safe result by
		// stripping the (already-normalized) basePath prefix.
		String safeTemplate;
		try {
			var resolved = FileUtils.resolveVirtualPathSafely(basePath, template);
			safeTemplate = stripBasePath(basePath, resolved);
		} catch (IllegalArgumentException ex) {
			throw new Forbidden("Path escapes configured base path.");
		}

		// Apply suffix idempotently so /freemarker/about resolves to about.ftlh (when
		// templateSuffix is configured), while /freemarker/about.ftlh stays as-is.
		safeTemplate = applyTemplateSuffix(safeTemplate);

		try {
			var cfg = resolveConfiguration(req);
			if (! res.containsHeader("Content-Type"))
				res.setHeader("Content-Type", FreemarkerViewRenderer.DEFAULT_CONTENT_TYPE);
			var tpl = cfg.getTemplate(safeTemplate);
			tpl.process(Map.of(), res.getWriter());
			res.getWriter().flush();
		} catch (LinkageError ex) {
			throw new InternalServerError(ex, FreemarkerViewRenderer.NO_ENGINE_DIAGNOSTIC);
		} catch (IOException | BasicHttpException ex) {
			throw ex;
		} catch (TemplateException | RuntimeException ex) {
			throw new InternalServerError(ex, "FreeMarker render failed for ''{0}''", safeTemplate);
		}
	}

	/**
	 * Strips the normalized {@code basePath} prefix from a {@code resolveVirtualPathSafely}
	 * result so the leftover string is configuration-relative (the bridge's default configuration
	 * re-adds the configured resource-root prefix via its template loader).
	 *
	 * <p>
	 * Normalization mirrors {@code FileUtils.resolveVirtualPathSafely}: a {@code null} / empty
	 * base normalizes to {@code "/"}; otherwise the base is guaranteed to start with {@code "/"}
	 * and end with {@code "/"} once the helper has normalized it.
	 *
	 * @param base The configured base path (typically {@code "/templates/"}).
	 * @param resolved The output of {@code resolveVirtualPathSafely} (always starts with the
	 * 	normalized base).
	 * @return The configuration-relative template name (e.g. {@code "hello.ftlh"} for base
	 * 	{@code "/templates/"} and resolved {@code "/templates/hello.ftlh"}).
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
	 * Builder for {@link FreemarkerDispatcher}.
	 */
	public static class Builder {

		String basePath = DEFAULT_BASE_PATH;
		String templateSuffix = DEFAULT_TEMPLATE_SUFFIX;
		boolean cacheTemplates = DEFAULT_CACHE_TEMPLATES;

		/** Constructor &mdash; package access for {@link FreemarkerDispatcher#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link FreemarkerDispatcher#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			basePath = isBlank(value) ? DEFAULT_BASE_PATH : value;
			return this;
		}

		/**
		 * Sets the suffix to append to template names that don't already end with it
		 * (idempotent).
		 *
		 * @param value The new suffix. {@code null} resets to
		 * 	{@link FreemarkerDispatcher#DEFAULT_TEMPLATE_SUFFIX}.
		 * @return This object.
		 */
		public Builder templateSuffix(String value) {
			templateSuffix = (value == null) ? DEFAULT_TEMPLATE_SUFFIX : value;
			return this;
		}

		/**
		 * Sets the template-cache flag for the bridge-default configuration.
		 *
		 * @param value The cache flag.
		 * @return This object.
		 */
		public Builder cacheTemplates(boolean value) {
			cacheTemplates = value;
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
		 * Reads the current cache-templates setting (test/inspection helper).
		 *
		 * @return The cache flag.
		 */
		public boolean isCacheTemplates() {
			return cacheTemplates;
		}

		/**
		 * Builds the {@link FreemarkerDispatcher}.
		 *
		 * @return A new {@link FreemarkerDispatcher} instance.
		 */
		public FreemarkerDispatcher build() {
			if (basePath == null)
				throw illegalArg("basePath must not be null");
			return new FreemarkerDispatcher(this);
		}
	}
}
