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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.util.*;

import freemarker.core.*;
import freemarker.template.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.view.*;

/**
 * Mixin that wires Apache FreeMarker view-rendering onto any Juneau REST resource.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=BasicFreemarkerResource.class)}; the host then:
 *
 * <ol class='spaced-list'>
 * 	<li>Gains a default mount at {@code /freemarker/*} that serves raw FreeMarker templates from
 * 		the importer's classpath by asking the configured {@link Configuration} for the named
 * 		template and rendering it with an empty data model (raw render path; callers who want
 * 		attributes use {@link FreemarkerView} from a typed handler instead).
 * 	<li>Picks up {@link FreemarkerViewRenderer} automatically via the mixin's
 * 		{@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration, so
 * 		{@code @RestOp}-method return values of type {@link FreemarkerView} render through the
 * 		FreeMarker engine without any additional wiring.
 * </ol>
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=BasicFreemarkerResource.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> BasicFreemarkerResource freemarker() {
 * 			<jk>return</jk> BasicFreemarkerResource.<jsm>create</jsm>()
 * 				.basePath(<js>"/templates/"</js>)
 * 				.templateSuffix(<js>".ftlh"</js>)
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> FreemarkerView.<jsm>of</jsm>(<js>"hello"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /freemarker/*} can be overridden via the SVL variable
 * {@code ${juneau.freemarker.path:freemarker}} &mdash; set via system property
 * ({@code -Djuneau.freemarker.path=views}), environment variable
 * ({@code JUNEAU_FREEMARKER_PATH=views}), or {@code Config} key
 * ({@code juneau.freemarker.path = views}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time; see
 * {@code FINISHED-99-svl-in-op-paths.md} for the full resolution chain.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by
 * {@link RestGet @RestGet(path="/${juneau.freemarker.path:freemarker}/*")} on {@link #render};
 * a class-level {@code @Rest(paths=...)} declaration would be silently ignored under the mixin
 * pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='section'>Choosing a Configuration:</h5>
 *
 * <p>
 * The {@code juneau-rest-server-view-freemarker} module ships <b>only</b>
 * {@code org.freemarker:freemarker} in {@code provided} scope. <b>No configuration bean is
 * bundled.</b> The bridge resolves a {@link Configuration} at first use from three sources, in
 * order:
 *
 * <ul class='spaced-list'>
 * 	<li><b>User-supplied bean:</b> register a {@code @Bean freemarker.template.Configuration}
 * 		(Spring) or
 * 		{@code BasicBeanStore.put(freemarker.template.Configuration.class, cfg)} (microservice).
 * 		Spring Boot's {@code spring-boot-starter-freemarker} autoconfigures one out of the box;
 * 		the bridge picks it up via {@code BeanStore.getBean(Configuration.class)}.
 * 	<li><b>Bridge default:</b> when nothing is registered, the bridge constructs a default
 * 		{@link Configuration} anchored on a classpath resource root derived from
 * 		{@link #getBasePath() basePath} ({@code "/templates/"} becomes
 * 		{@code ClassLoaderTemplateResolver}-equivalent prefix {@code "/templates"}). The default
 * 		pins {@code IncompatibleImprovements} to the bridge-tested minor version
 * 		({@code Configuration.VERSION_2_3_34}), sets {@code DefaultEncoding} to {@code UTF-8},
 * 		uses {@code HTMLOutputFormat} so HTML escaping is the natural target, and applies
 * 		{@code TemplateUpdateDelayMilliseconds} per the {@link #isCacheTemplates() cacheTemplates}
 * 		flag (production-safe by default; users opt into hot-reload via
 * 		{@link Builder#cacheTemplates(boolean) cacheTemplates(false)}).
 * </ul>
 *
 * <p>
 * When no FreeMarker engine is on the classpath, the renderer surfaces
 * {@link FreemarkerViewRenderer#NO_ENGINE_DIAGNOSTIC} naming the missing dependency.
 *
 * <h5 class='section'>Template suffix:</h5>
 *
 * <p>
 * FreeMarker's API does not expose a configuration-level resolver suffix &mdash; the literal
 * template name is what {@link Configuration#getTemplate(String) cfg.getTemplate(...)} sees. The
 * {@link Builder#templateSuffix(String) templateSuffix(...)} builder knob fills the gap: when
 * non-blank, the bridge appends the configured suffix to template names that don't already end
 * with it (idempotent), so callers can write {@code FreemarkerView.of("hello")} or request
 * {@code /freemarker/hello} and have it resolve to {@code hello.ftlh} on the classpath.
 *
 * <h5 class='section'>{@code .ftl} vs {@code .ftlh}:</h5>
 *
 * <p>
 * FreeMarker auto-selects HTML escaping by file extension: {@code .ftlh} templates emit
 * HTML-escaped output; {@code .ftl} templates emit raw output. For HTML responses, prefer
 * {@code .ftlh} so a future attribute-binding change can't introduce an XSS regression.
 *
 * <h5 class='section'>Multiple base paths:</h5>
 *
 * <p>
 * A host with {@code /templates/} and {@code /admin/templates/} template trees registers two
 * {@code BasicFreemarkerResource} beans, each mounted via a subclass declaring its own op-level
 * {@code @RestGet(path=...)} override and its own {@code basePath}. See the
 * {@code BasicFreemarkerResource_MockRest_Test} test in {@code juneau-utest} for the canonical
 * pattern.
 *
 * <h5 class='section'>OpenAPI surface:</h5>
 *
 * <p>
 * The greedy {@code /*} handler is not API-meaningful and is excluded from generated
 * Swagger / OpenAPI specs via {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FreemarkerView}
 * 	<li class='jc'>{@link FreemarkerViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest(
	responseProcessors={FreemarkerViewRenderer.class}
)
public class BasicFreemarkerResource implements RawTemplateDispatcher {

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
	 * No-arg constructor &mdash; uses {@link #DEFAULT_BASE_PATH} as the base path,
	 * {@link #DEFAULT_TEMPLATE_SUFFIX} as the template suffix, and
	 * {@link #DEFAULT_CACHE_TEMPLATES} as the cache flag.
	 *
	 * <p>
	 * The mixin walk falls back to this constructor when the
	 * {@link org.apache.juneau.commons.inject.BeanStore BeanStore} does not have a registered
	 * {@code BasicFreemarkerResource} bean.
	 */
	public BasicFreemarkerResource() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected BasicFreemarkerResource(Builder builder) {
		basePath = builder.basePath;
		templateSuffix = builder.templateSuffix;
		cacheTemplates = builder.cacheTemplates;
	}

	/**
	 * Returns the base path under which template resources are resolved.
	 *
	 * <p>
	 * Used to derive the bridge-default configuration's classpath resource root, and as the
	 * boundary for the path-traversal check in {@link #render render(...)}.
	 *
	 * @return The base path. Never {@code null}.
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Returns the configured template-name suffix.
	 *
	 * <p>
	 * When non-blank, the bridge appends this suffix to any template name that does not already
	 * end with it (idempotent). Defaults to {@link #DEFAULT_TEMPLATE_SUFFIX} (empty &mdash;
	 * literal names).
	 *
	 * @return The template suffix. Never {@code null}.
	 */
	public String getTemplateSuffix() {
		return templateSuffix;
	}

	/**
	 * Returns the template-cache flag.
	 *
	 * <p>
	 * {@code true} means the bridge-default {@link Configuration} pins
	 * {@code TemplateUpdateDelayMilliseconds} to {@code Long.MAX_VALUE} (cache forever &mdash;
	 * production-safe); {@code false} means {@code 0} (re-check on every request &mdash;
	 * suitable for dev hot-reload).
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
		if (templateSuffix == null || templateSuffix.isEmpty())
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
	 * 		registered. Anchored on a classpath resource root derived from {@link #basePath}, with
	 * 		{@code IncompatibleImprovements} pinned to the bridge-tested minor version.
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
	 * Anchored on a classpath resource root derived from {@link #basePath} (leading + trailing
	 * slashes trimmed; a {@code "/"} base yields a root-of-classpath resolver). The default pins
	 * {@code IncompatibleImprovements} to {@link Configuration#VERSION_2_3_34} so behavior is
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
		cfg.setClassLoaderForTemplateLoading(BasicFreemarkerResource.class.getClassLoader(), toResourceRoot(basePath));
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
		if (base == null || base.isBlank())
			return "/";
		var s = base;
		while (s.endsWith("/") && s.length() > 1)
			s = s.substring(0, s.length() - 1);
		if (! s.startsWith("/"))
			s = "/" + s;
		return s;
	}

	/**
	 * [GET /freemarker/*] &mdash; render a raw template through the FreeMarker engine.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder (e.g. a
	 * request for {@code /freemarker/about.ftlh} matches the mount with
	 * {@code path = "about.ftlh"}; a request for {@code /freemarker/admin/dashboard.ftlh} matches
	 * with {@code path = "admin/dashboard.ftlh"}). Behavior:
	 *
	 * <ol class='spaced-list'>
	 * 	<li>Validate the resolved {@code <basePath><path>} via
	 * 		{@link FileUtils#resolveVirtualPathSafely(String, String)} &mdash; reject any
	 * 		{@code ..} traversal with HTTP 403.
	 * 	<li>Apply the configured template suffix idempotently (e.g. with
	 * 		{@code templateSuffix(".ftlh")}, a request for {@code /freemarker/about} resolves
	 * 		template {@code "about.ftlh"} via {@link #applyTemplateSuffix}).
	 * 	<li>Ask the active {@link Configuration} for the template and process it with an empty
	 * 		data model directly onto the response writer; request attributes and parameters are
	 * 		not auto-bound (callers who want attributes use {@link FreemarkerView} from a typed
	 * 		handler instead).
	 * </ol>
	 *
	 * <p>
	 * Missing template surfaces as the engine's own {@link TemplateNotFoundException}
	 * (HTTP 500 from the renderer's catch-all); missing engine surfaces as
	 * {@link FreemarkerViewRenderer#NO_ENGINE_DIAGNOSTIC}.
	 *
	 * @param path The trailing path segment after {@code /freemarker/} (the template name
	 * 	relative to the configured {@link #getBasePath() base path}; configured suffix appended
	 * 	if missing).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 * @throws BasicHttpException On boundary violation (403), missing engine (500), or render
	 * 	failure (500).
	 */
	@Override /* RawTemplateDispatcher */
	@RestGet(
		path="/${juneau.freemarker.path:freemarker}/*",
		summary="FreeMarker view",
		description="Render a raw FreeMarker template under the configured base path.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res)
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
		} catch (@SuppressWarnings("unused") IllegalArgumentException ex) {
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
		} catch (IOException ex) {
			throw ex;
		} catch (BasicHttpException ex) {
			throw ex;
		} catch (TemplateException ex) {
			throw new InternalServerError(ex, "FreeMarker render failed for ''{0}''", safeTemplate);
		} catch (RuntimeException ex) {
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
		var bp = (base == null || base.isEmpty()) ? "/" : base;
		if (! bp.endsWith("/"))
			bp = bp + "/";
		if (! bp.startsWith("/"))
			bp = "/" + bp;
		if (resolved.startsWith(bp))
			return resolved.substring(bp.length());
		throw illegalArg("Resolved path ''{0}'' does not start with base ''{1}''", resolved, bp);
	}

	/**
	 * Builder for {@link BasicFreemarkerResource}.
	 */
	public static class Builder {

		private String basePath = DEFAULT_BASE_PATH;
		private String templateSuffix = DEFAULT_TEMPLATE_SUFFIX;
		private boolean cacheTemplates = DEFAULT_CACHE_TEMPLATES;

		/** Constructor &mdash; package access for {@link BasicFreemarkerResource#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * <p>
		 * Defaults to {@link BasicFreemarkerResource#DEFAULT_BASE_PATH "/"}. A typical configured
		 * value is {@code "/templates/"} &mdash; the same Spring-Boot-compatible layout
		 * convention the sibling JSP / Thymeleaf / Mustache bridges use.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link BasicFreemarkerResource#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			basePath = (value == null || value.isBlank()) ? DEFAULT_BASE_PATH : value;
			return this;
		}

		/**
		 * Sets the suffix to append to template names that don't already end with it
		 * (idempotent).
		 *
		 * <p>
		 * Defaults to {@link BasicFreemarkerResource#DEFAULT_TEMPLATE_SUFFIX ""} (no implicit
		 * suffix &mdash; literal template names). Typical configured values are {@code ".ftlh"}
		 * (HTML auto-escape) or {@code ".ftl"} (no auto-escape).
		 *
		 * @param value The new suffix. {@code null} resets to
		 * 	{@link BasicFreemarkerResource#DEFAULT_TEMPLATE_SUFFIX}.
		 * @return This object.
		 */
		public Builder templateSuffix(String value) {
			templateSuffix = (value == null) ? DEFAULT_TEMPLATE_SUFFIX : value;
			return this;
		}

		/**
		 * Sets the template-cache flag for the bridge-default configuration.
		 *
		 * <p>
		 * Defaults to {@link BasicFreemarkerResource#DEFAULT_CACHE_TEMPLATES true} (cache
		 * forever &mdash; production-safe). Set {@code false} to enable dev hot-reload (FreeMarker
		 * re-checks the underlying resource on every render).
		 *
		 * <p>
		 * This flag only affects the bridge-default {@link Configuration}; user-supplied
		 * {@code @Bean Configuration}s are not modified.
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
		 * Builds the {@link BasicFreemarkerResource}.
		 *
		 * @return A new {@link BasicFreemarkerResource} instance.
		 */
		public BasicFreemarkerResource build() {
			if (basePath == null)
				throw illegalArg("basePath must not be null");
			return new BasicFreemarkerResource(this);
		}
	}
}
