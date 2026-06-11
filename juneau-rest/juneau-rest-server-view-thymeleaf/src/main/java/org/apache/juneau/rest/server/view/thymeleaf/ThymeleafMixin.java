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
package org.apache.juneau.rest.server.view.thymeleaf;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.thymeleaf.*;
import org.thymeleaf.templatemode.*;

/**
 * Mixin that wires Thymeleaf view-rendering onto any Juneau REST resource.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=ThymeleafMixin.class)}; the host then:
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
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=ThymeleafMixin.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> ThymeleafMixin thymeleaf() {
 * 			<jk>return</jk> ThymeleafMixin.<jsm>create</jsm>()
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
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by
 * {@link RestGet @RestGet(path="/&#123;juneau.thymeleaf.path:thymeleaf&#125;/*")} on {@link #render};
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
 * 		{@link org.thymeleaf.templateresolver.ClassLoaderTemplateResolver} configured with {@code prefix=basePath},
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
 * {@code ThymeleafMixin} beans, each mounted via a subclass declaring its own op-level
 * {@code @RestGet(path=...)} override and its own {@code basePath}. See the
 * {@code ThymeleafMixin_MockRest_Test} test in {@code juneau-integration-tests} for the canonical
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerCompositionMixinsAndPaths">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest(
	responseProcessors={ThymeleafViewRenderer.class}
)
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are Thymeleaf MIME type strings and template attribute keys; intentional
})
public class ThymeleafMixin {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = ThymeleafDispatcher.DEFAULT_BASE_PATH;

	/** Default template-cache flag &mdash; production-safe; opt out per-builder for dev. */
	public static final boolean DEFAULT_CACHE_TEMPLATES = ThymeleafDispatcher.DEFAULT_CACHE_TEMPLATES;

	/** Default template mode for the bridge's fallback engine. */
	public static final TemplateMode DEFAULT_TEMPLATE_MODE = ThymeleafDispatcher.DEFAULT_TEMPLATE_MODE;

	private final ThymeleafDispatcher worker;

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
	 * BeanStore} does not have a registered {@code ThymeleafMixin} bean.
	 */
	public ThymeleafMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected ThymeleafMixin(Builder builder) {
		worker = builder.worker.build();
	}

	/**
	 * Returns the base path under which template resources are resolved.
	 *
	 * @return The base path. Never {@code null}.
	 */
	public String getBasePath() {
		return worker.getBasePath();
	}

	/**
	 * Returns whether the bridge's default engine caches resolved templates.
	 *
	 * @return The cache flag. Defaults to {@link #DEFAULT_CACHE_TEMPLATES}.
	 */
	public boolean isCacheTemplates() {
		return worker.isCacheTemplates();
	}

	/**
	 * Returns the template mode used by the bridge's default engine.
	 *
	 * @return The template mode. Defaults to {@link #DEFAULT_TEMPLATE_MODE}.
	 */
	public TemplateMode getTemplateMode() {
		return worker.getTemplateMode();
	}

	/**
	 * Resolves the active {@link TemplateEngine} via the shared {@link ThymeleafDispatcher} worker.
	 *
	 * @param req The current REST request.
	 * @return The active template engine. Never {@code null}.
	 */
	public TemplateEngine resolveTemplateEngine(RestRequest req) {
		return worker.resolveTemplateEngine(req);
	}

	/**
	 * [GET /thymeleaf/*] &mdash; render a raw template through the Thymeleaf engine.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder (e.g. a
	 * request for {@code /thymeleaf/about} matches the mount with {@code path = "about"}).
	 * Delegates to the shared {@link ThymeleafDispatcher} worker ({@code .html}-suffix handling,
	 * path-traversal hardening, engine resolution, and render).
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
	@RestGet(
		path="/${juneau.thymeleaf.path:thymeleaf}/*",
		summary="Thymeleaf view",
		description="Render a raw .html Thymeleaf template under the configured base path.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res)
			throws IOException, BasicHttpException {
		worker.render(path, req, res);
	}

	/**
	 * Builder for {@link ThymeleafMixin}.
	 *
	 * <p>
	 * Mirrors {@link ThymeleafDispatcher.Builder}'s configuration methods on its own surface and
	 * forwards each call into a held {@link ThymeleafDispatcher.Builder} (§2.3.1 worker-bean
	 * composition).
	 */
	public static class Builder {

		private final ThymeleafDispatcher.Builder worker = ThymeleafDispatcher.create();

		/** Constructor &mdash; package access for {@link ThymeleafMixin#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * <p>
		 * Defaults to {@link ThymeleafMixin#DEFAULT_BASE_PATH "/"}. A typical configured
		 * value is {@code "/templates/"} &mdash; the Spring-Boot-compatible layout convention.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link ThymeleafMixin#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			worker.basePath(value);
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
			worker.cacheTemplates(value);
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
			worker.templateMode(value);
			return this;
		}

		/**
		 * Reads the current base path setting (test/inspection helper).
		 *
		 * @return The base path. Never {@code null}.
		 */
		public String getBasePath() {
			return worker.getBasePath();
		}

		/**
		 * Reads the current cache-templates setting (test/inspection helper).
		 *
		 * @return The cache flag.
		 */
		public boolean isCacheTemplates() {
			return worker.isCacheTemplates();
		}

		/**
		 * Reads the current template-mode setting (test/inspection helper).
		 *
		 * @return The template mode. Never {@code null}.
		 */
		public TemplateMode getTemplateMode() {
			return worker.getTemplateMode();
		}

		/**
		 * Builds the {@link ThymeleafMixin}.
		 *
		 * @return A new {@link ThymeleafMixin} instance.
		 */
		public ThymeleafMixin build() {
			return new ThymeleafMixin(this);
		}
	}
}
