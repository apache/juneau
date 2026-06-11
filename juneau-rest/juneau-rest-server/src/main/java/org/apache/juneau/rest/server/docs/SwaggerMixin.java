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
package org.apache.juneau.rest.server.docs;

import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.bean.swagger.ui.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin that publishes the Swagger v2 spec endpoint at {@code /api/*}.
 *
 * <p>
 * Sibling of {@link SwaggerUiMixin} (HTML-first mount at {@code /swagger}),
 * {@link OpenApiMixin} (OpenAPI 3.1 spec at {@code /openapi*}), and
 * {@link RedocMixin} (HTML-first mount at {@code /redoc}). All four classes live in the
 * {@code org.apache.juneau.rest.server.docs} api-docs mixin pack.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=SwaggerMixin.class)};
 * the {@code /api} URL becomes available alongside the host's own endpoints with no further wiring.
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /api/*} can be overridden via the SVL variable
 * {@code ${juneau.swagger.path:api}} &mdash; set via system property
 * ({@code -Djuneau.swagger.path=swagger}), environment variable
 * ({@code JUNEAU_SWAGGER_PATH=swagger}), or {@code Config} key
 * ({@code juneau.swagger.path = swagger}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <p>
 * Override accepts bare token ({@code api}), leading slash ({@code /api}), trailing slash
 * ({@code api/}), or wildcard suffix ({@code /api/*}) &mdash; all resolve to the same mount.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/&#123;juneau.swagger.path:api&#125;/*")} on
 * {@link #getSwagger}; a class-level {@code @Rest(paths=...)} declaration would be silently
 * ignored under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/myresource"</js>, mixins=SwaggerMixin.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
 * 		<ja>@RestGet</ja>(<js>"/items"</js>) <jk>public</jk> List&lt;Item&gt; items() { ... }
 * 		<jc>// Now also serves /myresource/api with the Swagger v2 spec for this resource.</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>Content negotiation:</h5>
 *
 * <p>
 * Standard Juneau content negotiation. {@code Accept: text/html} renders the Swagger-UI HTML view
 * via the {@link SwaggerUI} per-media-type swap; other accept types serialize the
 * {@link Swagger} bean using the host's serializer set.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link SwaggerUiMixin}
 * 	<li class='jc'>{@link OpenApiMixin}
 * 	<li class='jc'>{@link RedocMixin}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerCompositionMixinsAndPaths">REST Server — Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
@JsonSchemaConfig(
	addDescriptionsTo="bean,collection,array,map,enum",
	addExamplesTo="bean,collection,array,map",
	ignoreTypes="Swagger,org.apache.juneau.bean.html5.*",
	useBeanDefs="true"
)
public class SwaggerMixin {

	/**
	 * [GET /api] — emit the Swagger v2 spec for this resource.
	 *
	 * @param req The current REST request.
	 * @return The Swagger document.
	 * @throws NotFound If no Swagger document is available for this resource.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.swagger.path:api})}/*",
		summary="Swagger documentation",
		description="Swagger v2 documentation for this resource."
	)
	@HtmlDocConfig(
		rank=10,
		navlinks={
			"back: servlet:/",
			"json: servlet:/api?Accept=text/json&plainText=true"
		},
		aside="NONE"
	)
	@MarshalledConfig(
		swaps={
			SwaggerUI.class
		}
	)
	public Swagger getSwagger(RestRequest req) {
		return req.getSwagger().orElseThrow(NotFound::new);
	}
}
