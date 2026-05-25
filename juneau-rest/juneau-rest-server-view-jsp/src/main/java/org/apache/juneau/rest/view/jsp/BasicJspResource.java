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
package org.apache.juneau.rest.view.jsp;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin that wires JSP view-rendering onto any Juneau REST resource.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=BasicJspResource.class)}; the host then:
 *
 * <ol class='spaced-list'>
 * 	<li>Gains a default mount at {@code /jsp/*} that serves raw {@code .jsp} resources from the
 * 		importer's classpath via
 * 		{@link jakarta.servlet.RequestDispatcher#forward forward(...)}.
 * 	<li>Picks up {@link JspViewRenderer} automatically via the mixin's
 * 		{@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration, so
 * 		{@code @RestOp}-method return values of type {@link JspView} render through the JSP
 * 		engine without any additional wiring.
 * </ol>
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=BasicJspResource.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> BasicJspResource jsp() {
 * 			<jk>return</jk> BasicJspResource.<jsm>create</jsm>()
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
 * Resolution happens once at {@link RestContext} construction time; see the FINISHED-99 archive
 * (SVL resolution in {@code @RestOp(path)}) for the full resolution chain.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/${juneau.jsp.path:jsp}/*")} on
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
 * {@code BasicJspResource} beans, each mounted via a subclass declaring its own op-level
 * {@code @RestGet(path=...)} override and its own {@code basePath}. See the
 * {@code BasicJspResource_MultiBasePath_Test} test in {@code juneau-utest} for the canonical
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest(
	responseProcessors={JspViewRenderer.class}
)
public class BasicJspResource {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = "/";

	private final String basePath;

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
	 * BeanStore} does not have a registered {@code BasicJspResource} bean.
	 */
	public BasicJspResource() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected BasicJspResource(Builder builder) {
		basePath = builder.basePath;
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
		return basePath;
	}

	/**
	 * [GET /jsp/*] &mdash; forward the request to the JSP engine.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder (e.g. a
	 * request for {@code /jsp/admin/dashboard.jsp} matches the mount with
	 * {@code path = "admin/dashboard.jsp"}). The handler then dispatches via
	 * {@code ServletContext.getRequestDispatcher(basePath + path).forward(...)} so the
	 * container's JSP engine renders the template.
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
		path="/${juneau.jsp.path:jsp}/*",
		summary="JSP view",
		description="Forward a request to the JSP engine for a raw .jsp resource under the configured base path.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res) throws IOException, NotFound {
		// joinPath delegates to FileUtils.resolveVirtualPathSafely, which throws IAE on any
		// resolved target that escapes basePath (../, %2e%2e treated as literal, absolute-path
		// injection, etc.). Map IAE → 403 (Forbidden); attackers can't distinguish from the
		// container-layer rejection of malformed paths.
		String target;
		try {
			target = JspViewRenderer.joinPath(basePath, path);
		} catch (@SuppressWarnings("unused") IllegalArgumentException ex) {
			throw new Forbidden("Path escapes configured base path.");
		}
		try {
			var ctx = req.getServletContext();
			var rd = ctx.getRequestDispatcher(target);
			if (rd == null)
				throw new InternalServerError("Could not resolve RequestDispatcher for ''{0}''. {1}",
					target, JspViewRenderer.NO_ENGINE_DIAGNOSTIC);
			rd.forward(req.getHttpServletRequest(), res.getHttpServletResponse());
		} catch (NoClassDefFoundError ex) {
			throw new InternalServerError(ex, JspViewRenderer.NO_ENGINE_DIAGNOSTIC);
		} catch (IOException | NotFound ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError(ex, "JSP render failed for ''{0}''", target);
		}
	}

	/**
	 * Builder for {@link BasicJspResource}.
	 */
	public static class Builder {

		private String basePath = DEFAULT_BASE_PATH;

		/** Constructor &mdash; package access for {@link BasicJspResource#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath / webapp base path under which {@code .jsp} resources are resolved.
		 *
		 * <p>
		 * Defaults to {@link BasicJspResource#DEFAULT_BASE_PATH "/"}. A typical configured value
		 * is {@code "/WEB-INF/views/"} &mdash; the Servlet-spec convention for hiding template
		 * files from direct HTTP access.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link BasicJspResource#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			basePath = (value == null || value.isBlank()) ? DEFAULT_BASE_PATH : value;
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
		 * Builds the {@link BasicJspResource}.
		 *
		 * @return A new {@link BasicJspResource} instance.
		 */
		public BasicJspResource build() {
			if (basePath == null)
				throw illegalArg("basePath must not be null");
			return new BasicJspResource(this);
		}
	}
}
