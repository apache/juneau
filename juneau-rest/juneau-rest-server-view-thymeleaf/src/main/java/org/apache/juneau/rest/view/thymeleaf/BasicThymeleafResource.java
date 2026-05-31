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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.view.*;
import org.thymeleaf.*;
import org.thymeleaf.templatemode.*;
import org.thymeleaf.templateresolver.*;

/**
 * Mixin that wires Thymeleaf view-rendering onto any Juneau REST resource.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=BasicThymeleafResource.class)}; the host then:
 *
 * <ol class='spaced-list'>
 * 	<li>Gains a default mount at {@code /thymeleaf/*} that serves raw {@code .html} templates from
 * 		the importer's classpath by asking the configured
 * 		{@link org.thymeleaf.TemplateEngine TemplateEngine} to render them with the current
 * 		request's locale and attributes available to the template.
 * 	<li>Picks up {@link ThymeleafViewRenderer} automatically via the mixin's
 * 		{@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration, so
 * 		{@code @RestOp}-method return values of type {@link ThymeleafView} render through the
 * 		Thymeleaf engine without any additional wiring.
 * </ol>
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=BasicThymeleafResource.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> BasicThymeleafResource thymeleaf() {
 * 			<jk>return</jk> BasicThymeleafResource.<jsm>create</jsm>()
 * 				.basePath(<js>"/templates/"</js>)
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> ThymeleafView.<jsm>of</jsm>(<js>"hello"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /thymeleaf/*} can be overridden via the SVL variable
 * {@code ${juneau.thymeleaf.path:thymeleaf}} &mdash; set via system property
 * ({@code -Djuneau.thymeleaf.path=views}), environment variable
 * ({@code JUNEAU_THYMELEAF_PATH=views}), or {@code Config} key
 * ({@code juneau.thymeleaf.path = views}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time; see
 * {@code FINISHED-99-svl-in-op-paths.md} for the full resolution chain.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by
 * {@link RestGet @RestGet(path="/${juneau.thymeleaf.path:thymeleaf}/*")} on {@link #render};
 * a class-level {@code @Rest(paths=...)} declaration would be silently ignored under the mixin
 * pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='section'>Choosing a TemplateEngine:</h5>
 *
 * <p>
 * The {@code juneau-rest-server-view-thymeleaf} module ships <b>only</b> the Thymeleaf core API in
 * {@code provided} scope. <b>No engine bean is bundled.</b> The bridge resolves a
 * {@link org.thymeleaf.TemplateEngine TemplateEngine} at first use from three sources, in order:
 *
 * <ul class='spaced-list'>
 * 	<li><b>Spring Boot autoconfig:</b> {@code spring-boot-starter-thymeleaf} contributes an
 * 		autoconfigured {@code SpringTemplateEngine} (which extends {@code TemplateEngine}); the
 * 		bridge picks it up via {@code BeanStore.getBean(TemplateEngine.class)}. No further wiring
 * 		needed.
 * 	<li><b>User-supplied bean:</b> register a {@code @Bean TemplateEngine} (Spring) or
 * 		{@code BasicBeanStore.put(TemplateEngine.class, engine)} (microservice). Allows full
 * 		control of resolvers, dialects, and template modes.
 * 	<li><b>Bridge default:</b> when nothing is registered, the bridge constructs a default
 * 		{@link org.thymeleaf.TemplateEngine TemplateEngine} with a single
 * 		{@link ClassLoaderTemplateResolver} configured with {@code prefix=basePath},
 * 		{@code suffix=".html"}, {@code templateMode=HTML}, and {@code cacheable=cacheTemplates}
 * 		(default {@code true} &mdash; production-safe; set {@code cacheTemplates(false)} on the
 * 		builder for hot-reload during development).
 * </ul>
 *
 * <p>
 * When no Thymeleaf engine is on the classpath, the renderer surfaces
 * {@link ThymeleafViewRenderer#NO_ENGINE_DIAGNOSTIC} naming the missing dependency.
 *
 * <h5 class='section'>Multiple base paths:</h5>
 *
 * <p>
 * A host with {@code /templates/} and {@code /admin/templates/} template trees registers two
 * {@code BasicThymeleafResource} beans, each mounted via a subclass declaring its own op-level
 * {@code @RestGet(path=...)} override and its own {@code basePath}. See the
 * {@code BasicThymeleafResource_MockRest_Test} test in {@code juneau-utest} for the canonical
 * pattern.
 *
 * <h5 class='section'>OpenAPI surface:</h5>
 *
 * <p>
 * The greedy {@code /*} handler is not API-meaningful and is excluded from generated
 * Swagger / OpenAPI specs via {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThymeleafView}
 * 	<li class='jc'>{@link ThymeleafViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ThymeleafViewSupport">Thymeleaf View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest(
	responseProcessors={ThymeleafViewRenderer.class}
)
public class BasicThymeleafResource implements RawTemplateDispatcher {

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
	 * No-arg constructor &mdash; uses {@link #DEFAULT_BASE_PATH} as the base path,
	 * {@link #DEFAULT_CACHE_TEMPLATES} for the cache flag, and {@link #DEFAULT_TEMPLATE_MODE} for
	 * the template mode.
	 *
	 * <p>
	 * The mixin walk falls back to this constructor when the {@link org.apache.juneau.commons.inject.BeanStore
	 * BeanStore} does not have a registered {@code BasicThymeleafResource} bean.
	 */
	public BasicThymeleafResource() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected BasicThymeleafResource(Builder builder) {
		basePath = builder.basePath;
		cacheTemplates = builder.cacheTemplates;
		templateMode = builder.templateMode;
	}

	/**
	 * Returns the base path under which template resources are resolved.
	 *
	 * <p>
	 * Used as the {@link ClassLoaderTemplateResolver#setPrefix(String) prefix} of the bridge's
	 * default engine, and as the boundary for the path-traversal check in
	 * {@link #render render(...)}.
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
	 * 		({@link #basePath}, {@code .html}, {@link #templateMode}, {@link #cacheTemplates}).
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
	 * [GET /thymeleaf/*] &mdash; render a raw template through the Thymeleaf engine.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder (e.g. a
	 * request for {@code /thymeleaf/about} matches the mount with {@code path = "about"}; a
	 * request for {@code /thymeleaf/admin/dashboard.html} matches with
	 * {@code path = "admin/dashboard.html"}). Behavior:
	 *
	 * <ol class='spaced-list'>
	 * 	<li>Strip any trailing {@code .html} extension (the engine resolver re-adds it).
	 * 	<li>Validate the resolved {@code <basePath><path>} via
	 * 		{@link FileUtils#resolveVirtualPathSafely(String, String)} &mdash; reject any
	 * 		{@code ..} traversal with HTTP 403.
	 * 	<li>Ask the active {@link TemplateEngine} to render the template directly onto the
	 * 		response writer with the request's locale as the {@link org.thymeleaf.context.Context
	 * 		Context}'s locale; request attributes and parameters are not auto-bound (callers who
	 * 		want attributes use {@link ThymeleafView} from a typed handler instead).
	 * </ol>
	 *
	 * <p>
	 * Missing template surfaces as the engine's own {@code TemplateInputException} (HTTP 500 from
	 * the renderer's catch-all); missing engine surfaces as
	 * {@link ThymeleafViewRenderer#NO_ENGINE_DIAGNOSTIC}.
	 *
	 * @param path The trailing path segment after {@code /thymeleaf/} (the template name relative
	 * 	to the configured {@link #getBasePath() base path}; trailing {@code .html} stripped).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 * @throws BasicHttpException On boundary violation (403), missing engine (500), or render
	 * 	failure (500).
	 */
	@Override /* RawTemplateDispatcher */
	@RestGet(
		path="/${juneau.thymeleaf.path:thymeleaf}/*",
		summary="Thymeleaf view",
		description="Render a raw .html Thymeleaf template under the configured base path.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res)
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
	 * Builder for {@link BasicThymeleafResource}.
	 */
	public static class Builder {

		private String basePath = DEFAULT_BASE_PATH;
		private boolean cacheTemplates = DEFAULT_CACHE_TEMPLATES;
		private TemplateMode templateMode = DEFAULT_TEMPLATE_MODE;

		/** Constructor &mdash; package access for {@link BasicThymeleafResource#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * <p>
		 * Defaults to {@link BasicThymeleafResource#DEFAULT_BASE_PATH "/"}. A typical configured
		 * value is {@code "/templates/"} &mdash; the Spring-Boot-compatible layout convention.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link BasicThymeleafResource#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			basePath = (value == null || value.isBlank()) ? DEFAULT_BASE_PATH : value;
			return this;
		}

		/**
		 * Sets whether the bridge's default engine caches resolved templates.
		 *
		 * <p>
		 * Defaults to {@code true} (production-safe). Set to {@code false} during development to
		 * pick up edits without restarting the server. This setting only applies to the bridge's
		 * fallback engine; if a {@link TemplateEngine} bean is registered, its own caching
		 * configuration is honored.
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
		 * <p>
		 * Defaults to {@link TemplateMode#HTML HTML}. Other modes (`XML`, `TEXT`, `JAVASCRIPT`,
		 * `CSS`, `RAW`) work if the user supplies templates in that mode; engines that want
		 * mixed-mode support should register a custom {@code @Bean TemplateEngine} with multiple
		 * resolvers instead.
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
		 * Builds the {@link BasicThymeleafResource}.
		 *
		 * @return A new {@link BasicThymeleafResource} instance.
		 */
		public BasicThymeleafResource build() {
			if (basePath == null)
				throw illegalArg("basePath must not be null");
			return new BasicThymeleafResource(this);
		}
	}
}
