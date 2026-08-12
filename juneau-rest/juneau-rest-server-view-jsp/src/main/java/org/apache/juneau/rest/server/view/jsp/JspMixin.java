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
package org.apache.juneau.rest.server.view.jsp;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Mixin that wires JSP view-rendering onto any Juneau REST resource.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=JspMixin.class)}; the host then:
 *
 * <ol class='spaced-list'>
 * 	<li>Gains a default mount at {@code /jsp/*} that serves raw {@code .jsp} resources from the
 * 		importer's classpath via
 * 		{@link jakarta.servlet.RequestDispatcher#forward forward(...)}.
 * 	<li>Registers {@link JspViewRenderer} for the mixin's <b>own</b> endpoints (e.g. the
 * 		{@code /jsp/*} route above) via the mixin's own
 * 		{@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration.
 * </ol>
 *
 * <h5 class='section'>Auto-wiring the renderer into the host's own endpoints:</h5>
 *
 * <p>
 * By default a mixin's {@code responseProcessors} apply only to the mixin's own endpoints &mdash; a host's own
 * {@code @RestOp} methods see only the host's chain (the {@link Rest#mixins() @Rest(mixins=...)} rule "host's
 * chain runs first, then the mixin's appended; host endpoints see only the host's chain").  So with a bare
 * {@code @Rest(mixins=JspMixin.class)}, a host whose own {@code @RestOp} returns {@link JspView} would have the
 * framework's default {@code SerializedPojoProcessor} bean-serialize it instead of dispatching it to the JSP
 * engine.  Two ways to make the host's own {@code JspView} returns reach {@link JspViewRenderer}:
 *
 * <ol class='spaced-list'>
 * 	<li><b>Opt in via {@link Mixin#mergeIntoHost() @Mixin(mergeIntoHost=true)}</b> (recommended) &mdash; the
 * 		host declares the mixin through the rich {@link Rest#mixinDefs() mixinDefs} form with
 * 		{@code mergeIntoHost=true}, which folds {@code JspMixin}'s {@code @Rest(responseProcessors=...)} (and any
 * 		other list-shaped attributes) into the host's own chain.  No need to repeat
 * 		{@link JspViewRenderer JspViewRenderer.class} on the host.
 * 	<li><b>List {@link JspViewRenderer JspViewRenderer.class} explicitly</b> in the host's own
 * 		{@code @Rest(responseProcessors=...)} &mdash; the manual equivalent, useful when the host does not
 * 		declare the mixin via {@code mixinDefs}.
 * </ol>
 *
 * <p>
 * The {@link org.apache.juneau.rest.server.processor.ResponseProcessorList} partition pass repositions
 * {@link JspViewRenderer} (a
 * {@link org.apache.juneau.rest.server.view.ViewRenderer ViewRenderer}) ahead of
 * {@code SerializedPojoProcessor} in either case, so the {@link JspView} bean is dispatched to the JSP engine
 * rather than bean-serialized.
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixinDefs=<ja>@Mixin</ja>(type=JspMixin.<jk>class</jk>, mergeIntoHost=<jk>true</jk>))
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> JspMixin jsp() {
 * 			<jk>return</jk> JspMixin.<jsm>create</jsm>()
 * 				.basePath(<js>"/WEB-INF/views/"</js>)
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> JspView.<jsm>of</jsm>(<js>"hello.jsp"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /jsp/*} can be overridden via the SVL variable
 * {@code ${juneau.jsp.path:jsp}} &mdash; set via system property
 * ({@code -Djuneau.jsp.path=views}), environment variable
 * ({@code JUNEAU_JSP_PATH=views}), or {@code Config} key
 * ({@code juneau.jsp.path = views}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <p>
 * Override accepts bare token ({@code jsp}), leading slash ({@code /jsp}), trailing slash
 * ({@code jsp/}), or wildcard suffix ({@code /jsp/*}) &mdash; all resolve to the same mount.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/&#123;juneau.jsp.path:jsp&#125;/*")} on
 * {@link #render}; a class-level {@code @Rest(paths=...)} declaration would be silently
 * ignored under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='section'>Engine-agnostic packaging:</h5>
 *
 * <p>
 * The {@code juneau-rest-server-view-jsp} module ships <b>only</b> the JSP API + JSTL impl in
 * {@code provided} scope. <b>No JSP engine</b> is bundled with the bridge module. Consumers add
 * the engine matching their container:
 *
 * <ul class='spaced-list'>
 * 	<li><b>Jetty 12 EE11</b> (microservice-jetty, Spring Boot embedded Jetty):
 * 		{@code org.eclipse.jetty.ee11:jetty-ee11-apache-jsp}.
 * 	<li><b>Embedded Tomcat</b> (Spring Boot default):
 * 		{@code org.apache.tomcat.embed:tomcat-embed-jasper}.
 * 	<li><b>External-WAR</b> deployments: container-supplied; no additional dependency.
 * </ul>
 *
 * <p>
 * When no engine is on the classpath, the renderer surfaces
 * {@link JspViewRenderer#NO_ENGINE_DIAGNOSTIC} naming the missing dependency.
 *
 * <h5 class='section'>Spring Boot fat-jar caveat:</h5>
 *
 * <p>
 * Spring Boot's repackaged fat jar does not place {@code .jsp} files where the embedded JSP
 * engine looks by default. Place JSP resources under
 * {@code src/main/resources/META-INF/resources/WEB-INF/views/...} (the {@code META-INF/resources/}
 * prefix is the Servlet 3.0 convention Spring Boot honors for embedded servlets). Both
 * {@code mvn spring-boot:run} and the deployed jar pick the files up this way.
 *
 * <h5 class='section'>Multiple base paths:</h5>
 *
 * <p>
 * A host with {@code /views/} JSPs and {@code /admin/views/} JSPs registers two
 * {@code JspMixin} beans, each mounted via a subclass declaring its own op-level
 * {@code @RestGet(path=...)} override and its own {@code basePath}. See the
 * {@code JspMixin_MultiBasePath_Test} test in {@code juneau-integration-tests} for the canonical
 * pattern.
 *
 * <h5 class='section'>OpenAPI surface:</h5>
 *
 * <p>
 * The greedy {@code /*} handler is not API-meaningful and is excluded from generated
 * Swagger / OpenAPI specs via {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JspView}
 * 	<li class='jc'>{@link JspViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JspViewSupport">JSP View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest(
	responseProcessors={JspViewRenderer.class}
)
public class JspMixin {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = JspDispatcher.DEFAULT_BASE_PATH;

	/**
	 * Default cache-templates setting &mdash; {@code true}. Reported for
	 * {@link ViewMixinBuilder} conformance only; see {@link Builder#cacheTemplates(boolean)}.
	 */
	public static final boolean DEFAULT_CACHE_TEMPLATES = true;

	private final JspDispatcher worker;
	private final boolean cacheTemplates;

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * No-arg constructor &mdash; uses {@link #DEFAULT_BASE_PATH} as the base path.
	 *
	 * <p>
	 * The mixin walk falls back to this constructor when the {@link org.apache.juneau.commons.inject.BeanStore
	 * BeanStore} does not have a registered {@code JspMixin} bean.
	 */
	public JspMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected JspMixin(Builder builder) {
		worker = builder.worker.build();
		cacheTemplates = builder.cacheTemplates;
	}

	/**
	 * Returns the base path under which {@code .jsp} resources are resolved.
	 *
	 * <p>
	 * Read by {@link JspViewRenderer} when dispatching a {@link JspView}-typed return value, and
	 * by the {@link #render render(...)} handler when forwarding raw-{@code .jsp} requests under
	 * {@code /jsp/*}.
	 *
	 * @return The base path. Never {@code null}.
	 */
	public String getBasePath() {
		return worker.getBasePath();
	}

	/**
	 * Returns the configured cache-templates flag.
	 *
	 * <p>
	 * Reported for {@link ViewMixinBuilder} conformance only &mdash; JSP recompilation is
	 * delegated entirely to the servlet container (see {@link Builder#cacheTemplates(boolean)}
	 * for the full caveat).
	 *
	 * @return The cache flag. Defaults to {@link #DEFAULT_CACHE_TEMPLATES}.
	 */
	public boolean isCacheTemplates() {
		return cacheTemplates;
	}

	/**
	 * [GET /jsp/*] &mdash; forward the request to the JSP engine.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder (e.g. a
	 * request for {@code /jsp/admin/dashboard.jsp} matches the mount with
	 * {@code path = "admin/dashboard.jsp"}). Delegates to the shared {@link JspDispatcher} worker,
	 * which dispatches via {@code ServletContext.getRequestDispatcher(basePath + path).forward(...)}
	 * so the container's JSP engine renders the template.
	 *
	 * <p>
	 * Missing JSP resources surface as a 404 from the underlying container; missing JSP engine
	 * surfaces as {@link JspViewRenderer#NO_ENGINE_DIAGNOSTIC}.
	 *
	 * @param path The trailing path segment after {@code /jsp/} (the JSP file name relative to
	 * 	the configured {@link #getBasePath() base path}).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying servlet writer fails.
	 * @throws NotFound If the JSP resource cannot be resolved.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.jsp.path:jsp})}/*",
		summary="JSP view",
		description="Forward a request to the JSP engine for a raw .jsp resource under the configured base path.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res) throws IOException, NotFound {
		worker.render(path, req, res);
	}

	/**
	 * Builder for {@link JspMixin}.
	 *
	 * <p>
	 * Mirrors {@link JspDispatcher.Builder}'s configuration methods on its own surface and forwards
	 * each call into a held {@link JspDispatcher.Builder} (§2.3.1 worker-bean composition).
	 * Implements {@link ViewMixinBuilder} so {@link #basePath(String)} is guaranteed to transfer
	 * to the sibling Thymeleaf / Mustache / FreeMarker bridge builders; see
	 * {@link #cacheTemplates(boolean)} for this bridge's no-op conformance caveat.
	 */
	public static class Builder implements ViewMixinBuilder<Builder> {

		private final JspDispatcher.Builder worker = JspDispatcher.create();
		private boolean cacheTemplates = DEFAULT_CACHE_TEMPLATES;

		/** Constructor &mdash; package access for {@link JspMixin#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath / webapp base path under which {@code .jsp} resources are resolved.
		 *
		 * <p>
		 * Defaults to {@link JspMixin#DEFAULT_BASE_PATH "/"}. A typical configured value
		 * is {@code "/WEB-INF/views/"} &mdash; the Servlet-spec convention for hiding template
		 * files from direct HTTP access.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link JspMixin#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		@Override /* ViewMixinBuilder */
		public Builder basePath(String value) {
			worker.basePath(value);
			return this;
		}

		/**
		 * Accepted for {@link ViewMixinBuilder} conformance; has <b>no effect</b> on the JSP
		 * bridge.
		 *
		 * <p>
		 * JSP recompilation is delegated entirely to the servlet container via
		 * {@code RequestDispatcher.forward(...)} &mdash; there is no bridge-owned template
		 * cache to disable. Containers manage their own JSP recompilation policy (e.g. Tomcat's
		 * Jasper {@code development} / {@code modificationTestInterval} servlet-init-params,
		 * or Jetty's equivalent) outside this bridge's control. The value is stored and
		 * reported back by {@link #isCacheTemplates()} / {@link JspMixin#isCacheTemplates()}
		 * purely so callers coding against the shared {@link ViewMixinBuilder} contract get a
		 * consistent round-trip, not because it changes rendering behavior.
		 *
		 * @param value The cache flag (recorded only; see above).
		 * @return This object.
		 */
		@Override /* ViewMixinBuilder */
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
			return worker.getBasePath();
		}

		/**
		 * Reads the current cache-templates setting (test/inspection helper).
		 *
		 * <p>
		 * See {@link #cacheTemplates(boolean)} &mdash; this bridge has no template cache to
		 * report on; the value is whatever was last recorded.
		 *
		 * @return The cache flag.
		 */
		public boolean isCacheTemplates() {
			return cacheTemplates;
		}

		/**
		 * Builds the {@link JspMixin}.
		 *
		 * @return A new {@link JspMixin} instance.
		 */
		public JspMixin build() {
			return new JspMixin(this);
		}
	}
}
