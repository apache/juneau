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

import java.io.*;

import com.github.mustachejava.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin that wires Mustache view-rendering onto any Juneau REST resource.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=MustacheMixin.class)}; the host then:
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
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=MustacheMixin.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> MustacheMixin mustache() {
 * 			<jk>return</jk> MustacheMixin.<jsm>create</jsm>()
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
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by
 * {@link RestGet @RestGet(path="/&#123;juneau.mustache.path:mustache&#125;/*")} on {@link #render};
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
 * {@code MustacheMixin} beans, each mounted via a subclass declaring its own op-level
 * {@code @RestGet(path=...)} override and its own {@code basePath}. See the
 * {@code MustacheMixin_MockRest_Test} test in {@code juneau-utest} for the canonical
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
 * @since 10.0.0
 */
// @formatter:off
@Rest(
	responseProcessors={MustacheViewRenderer.class}
)
public class MustacheMixin {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = MustacheDispatcher.DEFAULT_BASE_PATH;

	/** Default template suffix &mdash; empty (literal template names, no implicit suffix). */
	public static final String DEFAULT_TEMPLATE_SUFFIX = MustacheDispatcher.DEFAULT_TEMPLATE_SUFFIX;

	private final MustacheDispatcher worker;

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
	 * {@code MustacheMixin} bean.
	 */
	public MustacheMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected MustacheMixin(Builder builder) {
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
	 * Returns the configured template-name suffix.
	 *
	 * @return The template suffix. Never {@code null}.
	 */
	public String getTemplateSuffix() {
		return worker.getTemplateSuffix();
	}

	/**
	 * Appends {@link #getTemplateSuffix()} to {@code name} if not already present (idempotent).
	 *
	 * @param name The template name. Must not be {@code null}.
	 * @return The template name with the configured suffix appended (if applicable).
	 */
	public String applyTemplateSuffix(String name) {
		return worker.applyTemplateSuffix(name);
	}

	/**
	 * Resolves the active {@link MustacheFactory} via the shared {@link MustacheDispatcher} worker.
	 *
	 * @param req The current REST request.
	 * @return The active Mustache factory. Never {@code null}.
	 */
	public MustacheFactory resolveMustacheFactory(RestRequest req) {
		return worker.resolveMustacheFactory(req);
	}

	/**
	 * [GET /mustache/*] &mdash; render a raw template through the Mustache engine.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder (e.g. a
	 * request for {@code /mustache/about.mustache} matches the mount with
	 * {@code path = "about.mustache"}). Delegates to the shared {@link MustacheDispatcher} worker
	 * (path-traversal hardening, suffix application, factory resolution, and render).
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
	@RestGet(
		path="/${juneau.mustache.path:mustache}/*",
		summary="Mustache view",
		description="Render a raw Mustache template under the configured base path.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res)
			throws IOException, BasicHttpException {
		worker.render(path, req, res);
	}

	/**
	 * Builder for {@link MustacheMixin}.
	 *
	 * <p>
	 * Mirrors {@link MustacheDispatcher.Builder}'s configuration methods on its own surface and
	 * forwards each call into a held {@link MustacheDispatcher.Builder} (§2.3.1 worker-bean
	 * composition).
	 */
	public static class Builder {

		private final MustacheDispatcher.Builder worker = MustacheDispatcher.create();

		/** Constructor &mdash; package access for {@link MustacheMixin#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * <p>
		 * Defaults to {@link MustacheMixin#DEFAULT_BASE_PATH "/"}. A typical configured
		 * value is {@code "/templates/"} &mdash; the same Spring-Boot-compatible layout
		 * convention the sibling JSP / Thymeleaf bridges use.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link MustacheMixin#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			worker.basePath(value);
			return this;
		}

		/**
		 * Sets the suffix to append to template names that don't already end with it (idempotent).
		 *
		 * <p>
		 * Defaults to {@link MustacheMixin#DEFAULT_TEMPLATE_SUFFIX ""} (no implicit
		 * suffix &mdash; literal template names). Typical configured value is
		 * {@code ".mustache"}, the mustache.java convention.
		 *
		 * @param value The new suffix. {@code null} resets to
		 * 	{@link MustacheMixin#DEFAULT_TEMPLATE_SUFFIX}.
		 * @return This object.
		 */
		public Builder templateSuffix(String value) {
			worker.templateSuffix(value);
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
		 * Reads the current template-suffix setting (test/inspection helper).
		 *
		 * @return The template suffix. Never {@code null}.
		 */
		public String getTemplateSuffix() {
			return worker.getTemplateSuffix();
		}

		/**
		 * Builds the {@link MustacheMixin}.
		 *
		 * @return A new {@link MustacheMixin} instance.
		 */
		public MustacheMixin build() {
			return new MustacheMixin(this);
		}
	}
}
