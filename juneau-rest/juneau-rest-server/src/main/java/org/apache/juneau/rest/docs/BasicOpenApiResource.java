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

import java.io.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.openapi3.OpenApi;
import org.apache.juneau.bean.openapi3.ui.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.yaml.*;

/**
 * Mixin that publishes the OpenAPI 3.1 spec endpoints for a resource.
 *
 * <p>
 * Three URL paths are mounted:
 * </p>
 * <ul class='spaced-list'>
 * 	<li>{@code /openapi} &mdash; content-negotiated (HTML view via {@link RedocUI} swap, plus all serializers
 * 		registered on the host).
 * 	<li>{@code /openapi.json} &mdash; <em>format-pinned</em>: always emits JSON regardless of the request
 * 		{@code Accept} header. Implemented via {@link RestResponse#getDirectWriter(String)} so the pin is
 * 		robust against {@code Accept: text/html} or any other media type.
 * 	<li>{@code /openapi.yaml} &mdash; <em>format-pinned</em>: always emits YAML regardless of the request
 * 		{@code Accept} header.
 * </ul>
 *
 * <p>
 * Sibling of {@link BasicSwaggerResource} (Swagger v2 spec at {@code /api}),
 * {@link BasicSwaggerUiResource} (HTML-first mount at {@code /swagger}), and {@link BasicRedocResource}
 * (HTML-first mount at {@code /redoc}). All four classes live in the
 * {@code org.apache.juneau.rest.docs} api-docs mixin pack.
 *
 * <p>
 * The mixin declares {@link Rest#serializers() @Rest(serializers={YamlSerializer.class})} so the
 * {@code /openapi} content-negotiated endpoint can serve {@code application/yaml} when explicitly
 * requested via {@code Accept}. Sub-context inheritance (per the mixin sub-context model) confines this
 * contribution to this mixin's endpoints &mdash; the host's other endpoints retain their own serializer set.
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount prefix {@code /openapi} can be overridden via the SVL variable
 * {@code ${juneau.openapi.path:openapi}} &mdash; the same variable is reused for all three mounts
 * ({@code /openapi/*}, {@code /openapi.json}, {@code /openapi.yaml}) so a single override
 * relocates the whole surface. Set via system property
 * ({@code -Djuneau.openapi.path=v1/openapi}), environment variable
 * ({@code JUNEAU_OPENAPI_PATH=v1/openapi}), or {@code Config} key
 * ({@code juneau.openapi.path = v1/openapi}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time; see the FINISHED-99 archive
 * (SVL resolution in {@code @RestOp(path)}) for the full resolution chain.
 *
 * <p>
 * Override accepts bare token ({@code openapi}), leading slash ({@code /openapi}), trailing slash
 * ({@code openapi/}), or wildcard suffix ({@code /openapi/*}) &mdash; all resolve to the same mount.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The three mount paths
 * ({@code /openapi/*}, {@code /openapi.json}, {@code /openapi.yaml}) are pinned at the op level
 * by {@link RestGet @RestGet(path=...)} on the handler methods; a class-level
 * {@code @Rest(paths=...)} declaration would be silently ignored under the mixin pattern (see
 * {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/myresource"</js>, mixins=BasicOpenApiResource.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
 * 		<jc>// Now also serves /myresource/openapi, /myresource/openapi.json, /myresource/openapi.yaml.</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicSwaggerResource}
 * 	<li class='jc'>{@link BasicSwaggerUiResource}
 * 	<li class='jc'>{@link BasicRedocResource}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest(
	serializers={YamlSerializer.class}
)
@JsonSchemaConfig(
	addDescriptionsTo="bean,collection,array,map,enum",
	addExamplesTo="bean,collection,array,map",
	ignoreTypes="OpenApi,org.apache.juneau.bean.html5.*",
	useBeanDefs="true"
)
public class BasicOpenApiResource {

	/**
	 * [GET /openapi] &mdash; emit the OpenAPI 3.1 document for this resource.
	 *
	 * <p>
	 * Standard Juneau content negotiation. {@code Accept: text/html} renders the Redoc HTML view via the
	 * {@link RedocUI} per-media-type swap; other accept types serialize the {@link OpenApi} bean using the
	 * host's serializer set (plus the {@link YamlSerializer} contributed by this mixin).
	 *
	 * @param req The current REST request.
	 * @return The OpenAPI document.
	 * @throws NotFound If no OpenAPI document is available for this resource.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.openapi.path:openapi})}/*",
		summary="OpenAPI 3.1 documentation",
		description="OpenAPI 3.1 documentation for this resource."
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
	public OpenApi getOpenApi(RestRequest req) {
		return req.getOpenApi().orElseThrow(NotFound::new);
	}

	/**
	 * [GET /openapi.json] &mdash; emit the OpenAPI 3.1 document as JSON, ignoring the {@code Accept} header.
	 *
	 * <p>
	 * Format-pinned: always returns {@code application/json}. Implemented via
	 * {@link RestResponse#getDirectWriter(String)} so the pin is not subject to content negotiation.
	 *
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws NotFound If no OpenAPI document is available for this resource.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.openapi.path:openapi})}.json",
		summary="OpenAPI 3.1 documentation (JSON)",
		description="OpenAPI 3.1 documentation for this resource as JSON."
	)
	public void getOpenApiJson(RestRequest req, RestResponse res) throws IOException {
		var doc = req.getOpenApi().orElseThrow(NotFound::new);
		try (var w = res.getDirectWriter("application/json")) {
			JsonSerializer.DEFAULT_READABLE.serialize(doc, w);
		}
	}

	/**
	 * [GET /openapi.yaml] &mdash; emit the OpenAPI 3.1 document as YAML, ignoring the {@code Accept} header.
	 *
	 * <p>
	 * Format-pinned: always returns {@code application/yaml}.
	 *
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws NotFound If no OpenAPI document is available for this resource.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.openapi.path:openapi})}.yaml",
		summary="OpenAPI 3.1 documentation (YAML)",
		description="OpenAPI 3.1 documentation for this resource as YAML."
	)
	public void getOpenApiYaml(RestRequest req, RestResponse res) throws IOException {
		var doc = req.getOpenApi().orElseThrow(NotFound::new);
		try (var w = res.getDirectWriter("application/yaml")) {
			YamlSerializer.DEFAULT_READABLE.serialize(doc, w);
		}
	}
}
