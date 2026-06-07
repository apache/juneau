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

import org.apache.juneau.*;
import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.bean.openapi3.ui.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.rest.*;

/**
 * Mixin that publishes the Redoc HTML view at {@code /redoc} for the OpenAPI 3.1 spec.
 *
 * <p>
 * Sibling of {@link SwaggerMixin} (Swagger v2 spec at {@code /api}),
 * {@link SwaggerUiMixin} (HTML-first mount at {@code /swagger}), and {@link OpenApiMixin}
 * (OpenAPI 3.1 spec at {@code /openapi*}). All four classes live in the
 * {@code org.apache.juneau.rest.docs} api-docs mixin pack.
 *
 * <p>
 * Declares {@link Rest#mixins() @Rest(mixins=&#123;OpenApiMixin.class&#125;)} so transitive resolution
 * pulls in {@code OpenApiMixin} (and its {@code /openapi*} mounts) automatically when this
 * mixin is composed into a host.
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /redoc/*} can be overridden via the SVL variable
 * {@code ${juneau.redoc.path:redoc}} &mdash; set via system property
 * ({@code -Djuneau.redoc.path=docs}), environment variable
 * ({@code JUNEAU_REDOC_PATH=docs}), or {@code Config} key
 * ({@code juneau.redoc.path = docs}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <p>
 * Override accepts bare token ({@code redoc}), leading slash ({@code /redoc}), trailing slash
 * ({@code redoc/}), or wildcard suffix ({@code /redoc/*}) &mdash; all resolve to the same mount.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/&#123;juneau.redoc.path:redoc&#125;/*")} on
 * {@link #getRedoc}; a class-level {@code @Rest(paths=...)} declaration would be silently
 * ignored under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/myresource"</js>, mixins=RedocMixin.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
 * 		<jc>// Serves /myresource/openapi, /openapi.json, /openapi.yaml (transitively) and /myresource/redoc (this mixin).</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>Content negotiation:</h5>
 *
 * <p>
 * {@link Rest#defaultAccept() @Rest(defaultAccept="text/html")} causes bare browser requests
 * (no {@code Accept} header) to render HTML via the {@link RedocUI} swap. Explicit accept headers
 * (e.g. {@code Accept: application/json}) are honored normally.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link SwaggerMixin}
 * 	<li class='jc'>{@link SwaggerUiMixin}
 * 	<li class='jc'>{@link OpenApiMixin}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest(
	mixins={OpenApiMixin.class},
	defaultAccept="text/html"
)
@JsonSchemaConfig(
	addDescriptionsTo="bean,collection,array,map,enum",
	addExamplesTo="bean,collection,array,map",
	ignoreTypes="OpenApi,org.apache.juneau.bean.html5.*",
	useBeanDefs="true"
)
public class RedocMixin {

	/**
	 * [GET /redoc] &mdash; render the Redoc HTML view (or the spec, depending on {@code Accept}).
	 *
	 * @param req The current REST request.
	 * @return The OpenAPI document.
	 * @throws NotFound If no OpenAPI document is available for this resource.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.redoc.path:redoc})}/*",
		summary="Redoc UI",
		description="Redoc HTML view of the OpenAPI 3.1 documentation for this resource."
	)
	@HtmlDocConfig(
		rank=10,
		navlinks={
			"back: servlet:/",
			"json: servlet:/openapi.json",
			"yaml: servlet:/openapi.yaml"
		},
		aside="NONE"
	)
	@MarshalledConfig(
		swaps={
			RedocUI.class
		}
	)
	public OpenApi getRedoc(RestRequest req) {
		return req.getOpenApi().orElseThrow(NotFound::new);
	}
}
