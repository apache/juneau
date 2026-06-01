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
package org.apache.juneau.rest.docs;

import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.bean.swagger.ui.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin that publishes the Swagger-UI HTML view at {@code /swagger}.
 *
 * <p>
 * Sibling of {@link SwaggerMixin} (Swagger v2 spec at {@code /api}),
 * {@link OpenApiMixin} (OpenAPI 3.1 spec at {@code /openapi*}), and
 * {@link RedocMixin} (HTML-first mount at {@code /redoc}). All four classes live in the
 * {@code org.apache.juneau.rest.docs} api-docs mixin pack.
 *
 * <p>
 * Declares {@link Rest#mixins() @Rest(mixins={SwaggerMixin.class})} so transitive resolution
 * pulls in {@code SwaggerMixin} (and its {@code /api} mount) automatically when this mixin
 * is composed into a host. The two endpoints share the same Swagger document; they differ only in
 * URL surface and content-negotiation defaults.
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /swagger/*} can be overridden via the SVL variable
 * {@code ${juneau.swaggerui.path:swagger}} &mdash; set via system property
 * ({@code -Djuneau.swaggerui.path=docs}), environment variable
 * ({@code JUNEAU_SWAGGERUI_PATH=docs}), or {@code Config} key
 * ({@code juneau.swaggerui.path = docs}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time; see the FINISHED-99 archive
 * (SVL resolution in {@code @RestOp(path)}) for the full resolution chain.
 *
 * <p>
 * Override accepts bare token ({@code swagger}), leading slash ({@code /swagger}), trailing slash
 * ({@code swagger/}), or wildcard suffix ({@code /swagger/*}) &mdash; all resolve to the same mount.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/${juneau.swaggerui.path:swagger}/*")}
 * on {@link #getSwaggerUi}; a class-level {@code @Rest(paths=...)} declaration would be silently
 * ignored under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/myresource"</js>, mixins=SwaggerUiMixin.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
 * 		<jc>// Serves /myresource/api  (transitively) and /myresource/swagger (this mixin).</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>Content negotiation:</h5>
 *
 * <p>
 * The {@code @Rest(defaultAccept="text/html")} declaration causes bare browser requests
 * (no {@code Accept} header) to render HTML via the {@link SwaggerUI} swap.
 * Explicit accept headers (e.g. {@code Accept: application/json}) are honored normally.
 * </p>
 *
 * <p>
 * Sub-context inheritance from FINISHED-81 confines the {@code defaultAccept} contribution to this
 * mixin's endpoints — the host's other endpoints retain their own content-negotiation defaults.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link SwaggerMixin}
 * 	<li class='jc'>{@link OpenApiMixin}
 * 	<li class='jc'>{@link RedocMixin}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server — Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest(
	mixins={SwaggerMixin.class},
	defaultAccept="text/html"
)
@JsonSchemaConfig(
	addDescriptionsTo="bean,collection,array,map,enum",
	addExamplesTo="bean,collection,array,map",
	ignoreTypes="Swagger,org.apache.juneau.bean.html5.*",
	useBeanDefs="true"
)
public class SwaggerUiMixin {

	/**
	 * [GET /swagger] — render the Swagger-UI HTML view (or the spec, depending on {@code Accept}).
	 *
	 * @param req The current REST request.
	 * @return The Swagger document.
	 * @throws NotFound If no Swagger document is available for this resource.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.swaggerui.path:swagger})}/*",
		summary="Swagger UI",
		description="Swagger-UI HTML view of the Swagger v2 documentation for this resource."
	)
	@HtmlDocConfig(
		rank=10,
		navlinks={
			"back: servlet:/",
			"json: servlet:/swagger?Accept=text/json&plainText=true"
		},
		aside="NONE"
	)
	@MarshalledConfig(
		swaps={
			SwaggerUI.class
		}
	)
	public Swagger getSwaggerUi(RestRequest req) {
		return req.getSwagger().orElseThrow(NotFound::new);
	}
}
