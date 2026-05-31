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
package org.apache.juneau.rest.view.mustache;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.util.*;

import com.github.mustachejava.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.view.*;

/**
 * Mixin that wires Mustache view-rendering onto any Juneau REST resource.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=BasicMustacheResource.class)}; the host then:
 *
 * <ol class='spaced-list'>
 * 	<li>Gains a default mount at {@code /mustache/*} that serves raw Mustache templates from the
 * 		importer's classpath by asking the configured {@link MustacheFactory} to compile and
 * 		render them with no scope (raw render path; callers who want attributes use
 * 		{@link MustacheView} from a typed handler instead).
 * 	<li>Picks up {@link MustacheViewRenderer} automatically via the mixin's
 * 		{@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration, so
 * 		{@code @RestOp}-method return values of type {@link MustacheView} render through the
 * 		Mustache engine without any additional wiring.
 * </ol>
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=BasicMustacheResource.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> BasicMustacheResource mustache() {
 * 			<jk>return</jk> BasicMustacheResource.<jsm>create</jsm>()
 * 				.basePath(<js>"/templates/"</js>)
 * 				.templateSuffix(<js>".mustache"</js>)
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> MustacheView.<jsm>of</jsm>(<js>"hello"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /mustache/*} can be overridden via the SVL variable
 * {@code ${juneau.mustache.path:mustache}} &mdash; set via system property
 * ({@code -Djuneau.mustache.path=views}), environment variable
 * ({@code JUNEAU_MUSTACHE_PATH=views}), or {@code Config} key
 * ({@code juneau.mustache.path = views}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time; see
 * {@code FINISHED-99-svl-in-op-paths.md} for the full resolution chain.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by
 * {@link RestGet @RestGet(path="/${juneau.mustache.path:mustache}/*")} on {@link #render};
 * a class-level {@code @Rest(paths=...)} declaration would be silently ignored under the mixin
 * pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='section'>Choosing a MustacheFactory:</h5>
 *
 * <p>
 * The {@code juneau-rest-server-view-mustache} module ships <b>only</b> the mustache.java
 * compiler API in {@code provided} scope. <b>No factory bean is bundled.</b> The bridge resolves
 * a {@link MustacheFactory} at first use from three sources, in order:
 *
 * <ul class='spaced-list'>
 * 	<li><b>User-supplied bean:</b> register a {@code @Bean MustacheFactory} (Spring) or
 * 		{@code BasicBeanStore.put(MustacheFactory.class, factory)} (microservice). Allows full
 * 		control of resolvers, object handlers, and encoders.
 * 	<li><b>Bridge default:</b> when nothing is registered, the bridge constructs a default
 * 		{@link DefaultMustacheFactory} anchored on a resource root derived from
 * 		{@link #getBasePath() basePath} (leading + trailing slashes trimmed; a {@code "/"} base
 * 		yields a no-prefix factory). With {@code basePath("/templates/")}, the factory resolves
 * 		template names like {@code "hello.mustache"} as classpath resource
 * 		{@code templates/hello.mustache}.
 * </ul>
 *
 * <p>
 * mustache.java's {@code DefaultMustacheFactory} caches compiled templates internally; the
 * bridge does not currently expose a cache-disable knob (consumers who want hot-reload in
 * development supply their own {@code @Bean MustacheFactory} configured as needed).
 *
 * <p>
 * When no Mustache engine is on the classpath, the renderer surfaces
 * {@link MustacheViewRenderer#NO_ENGINE_DIAGNOSTIC} naming the missing dependency.
 *
 * <h5 class='section'>Template suffix:</h5>
 *
 * <p>
 * Unlike the Thymeleaf bridge (which always appends {@code .html} via the engine resolver's
 * suffix setting), mustache.java has no resolver-suffix concept &mdash; the literal template name
 * is what {@link MustacheFactory#compile(String) factory.compile(...)} sees. The
 * {@link Builder#templateSuffix(String) templateSuffix(...)} builder knob fills the gap: when
 * non-blank, the bridge appends the configured suffix to template names that don't already end
 * with it (idempotent), so callers can write {@code MustacheView.of("hello")} or request
 * {@code /mustache/hello} and have it resolve to {@code hello.mustache} on the classpath.
 *
 * <h5 class='section'>Multiple base paths:</h5>
 *
 * <p>
 * A host with {@code /templates/} and {@code /admin/templates/} template trees registers two
 * {@code BasicMustacheResource} beans, each mounted via a subclass declaring its own op-level
 * {@code @RestGet(path=...)} override and its own {@code basePath}. See the
 * {@code BasicMustacheResource_MockRest_Test} test in {@code juneau-utest} for the canonical
 * pattern.
 *
 * <h5 class='section'>OpenAPI surface:</h5>
 *
 * <p>
 * The greedy {@code /*} handler is not API-meaningful and is excluded from generated
 * Swagger / OpenAPI specs via {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MustacheView}
 * 	<li class='jc'>{@link MustacheViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MustacheViewSupport">Mustache View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest(
	responseProcessors={MustacheViewRenderer.class}
)
public class BasicMustacheResource implements RawTemplateDispatcher {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = "/";

	/** Default template suffix &mdash; empty (literal template names, no implicit suffix). */
	public static final String DEFAULT_TEMPLATE_SUFFIX = "";

	private final String basePath;
	private final String templateSuffix;

	// Lazy bridge-default factory. Built on first call to resolveMustacheFactory(...) when no
	// MustacheFactory bean is registered in the request's BeanStore. Volatile so the
	// double-checked-locking idiom is safe under concurrent first-request load.
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
	 * No-arg constructor &mdash; uses {@link #DEFAULT_BASE_PATH} as the base path and
	 * {@link #DEFAULT_TEMPLATE_SUFFIX} for the template suffix.
	 *
	 * <p>
	 * The mixin walk falls back to this constructor when the
	 * {@link org.apache.juneau.commons.inject.BeanStore BeanStore} does not have a registered
	 * {@code BasicMustacheResource} bean.
	 */
	public BasicMustacheResource() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected BasicMustacheResource(Builder builder) {
		basePath = builder.basePath;
		templateSuffix = builder.templateSuffix;
	}

	/**
	 * Returns the base path under which template resources are resolved.
	 *
	 * <p>
	 * Used to derive the bridge-default factory's resource root, and as the boundary for the
	 * path-traversal check in {@link #render render(...)}.
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
	 * 		derived from {@link #basePath} (leading + trailing slashes trimmed).
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
	 * {@link #basePath} (leading + trailing slashes trimmed). A {@code "/"} base yields a
	 * no-prefix factory. Subclasses may override to plug in custom resolvers / object handlers
	 * without registering a separate {@code @Bean MustacheFactory}.
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
		if (base == null || base.isBlank())
			return "";
		var s = base;
		while (s.startsWith("/"))
			s = s.substring(1);
		while (s.endsWith("/"))
			s = s.substring(0, s.length() - 1);
		return s;
	}

	/**
	 * [GET /mustache/*] &mdash; render a raw template through the Mustache engine.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder (e.g. a
	 * request for {@code /mustache/about.mustache} matches the mount with
	 * {@code path = "about.mustache"}; a request for {@code /mustache/admin/dashboard.mustache}
	 * matches with {@code path = "admin/dashboard.mustache"}). Behavior:
	 *
	 * <ol class='spaced-list'>
	 * 	<li>Validate the resolved {@code <basePath><path>} via
	 * 		{@link FileUtils#resolveVirtualPathSafely(String, String)} &mdash; reject any
	 * 		{@code ..} traversal with HTTP 403.
	 * 	<li>Apply the configured template suffix idempotently (e.g. with
	 * 		{@code templateSuffix(".mustache")}, a request for {@code /mustache/about} resolves
	 * 		template {@code "about.mustache"} via {@link #applyTemplateSuffix}).
	 * 	<li>Ask the active {@link MustacheFactory} to compile the template and execute it with
	 * 		an empty scope directly onto the response writer; request attributes and parameters
	 * 		are not auto-bound (callers who want attributes use {@link MustacheView} from a typed
	 * 		handler instead).
	 * </ol>
	 *
	 * <p>
	 * Missing template surfaces as the engine's own {@link MustacheNotFoundException}
	 * (HTTP 500 from the renderer's catch-all); missing engine surfaces as
	 * {@link MustacheViewRenderer#NO_ENGINE_DIAGNOSTIC}.
	 *
	 * @param path The trailing path segment after {@code /mustache/} (the template name relative
	 * 	to the configured {@link #getBasePath() base path}; configured suffix appended if
	 * 	missing).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 * @throws BasicHttpException On boundary violation (403), missing engine (500), or render
	 * 	failure (500).
	 */
	@Override /* RawTemplateDispatcher */
	@RestGet(
		path="/${juneau.mustache.path:mustache}/*",
		summary="Mustache view",
		description="Render a raw Mustache template under the configured base path.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res)
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
		} catch (@SuppressWarnings("unused") IllegalArgumentException ex) {
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
		} catch (IOException ex) {
			throw ex;
		} catch (BasicHttpException ex) {
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
	 * Builder for {@link BasicMustacheResource}.
	 */
	public static class Builder {

		private String basePath = DEFAULT_BASE_PATH;
		private String templateSuffix = DEFAULT_TEMPLATE_SUFFIX;

		/** Constructor &mdash; package access for {@link BasicMustacheResource#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * <p>
		 * Defaults to {@link BasicMustacheResource#DEFAULT_BASE_PATH "/"}. A typical configured
		 * value is {@code "/templates/"} &mdash; the same Spring-Boot-compatible layout
		 * convention the sibling JSP / Thymeleaf bridges use.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link BasicMustacheResource#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			basePath = (value == null || value.isBlank()) ? DEFAULT_BASE_PATH : value;
			return this;
		}

		/**
		 * Sets the suffix to append to template names that don't already end with it (idempotent).
		 *
		 * <p>
		 * Defaults to {@link BasicMustacheResource#DEFAULT_TEMPLATE_SUFFIX ""} (no implicit
		 * suffix &mdash; literal template names). Typical configured value is
		 * {@code ".mustache"}, the mustache.java convention.
		 *
		 * @param value The new suffix. {@code null} resets to
		 * 	{@link BasicMustacheResource#DEFAULT_TEMPLATE_SUFFIX}.
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
		 * Builds the {@link BasicMustacheResource}.
		 *
		 * @return A new {@link BasicMustacheResource} instance.
		 */
		public BasicMustacheResource build() {
			if (basePath == null)
				throw illegalArg("basePath must not be null");
			return new BasicMustacheResource(this);
		}
	}
}
