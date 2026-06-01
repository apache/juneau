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
/**
 * API-docs mixin pack — composable {@code @Rest(mixins=...)} resources that publish Swagger v2
 * and OpenAPI 3.1 spec endpoints with Swagger-UI and Redoc HTML mounts.
 *
 * <p>
 * Four sibling mixins compose into the host {@code @Rest}-annotated resource. Each mixin owns its
 * own URL prefix (or single endpoint) and is independently mountable; transitive resolution lets a
 * UI mixin pull in its corresponding spec mixin automatically.
 * </p>
 *
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link org.apache.juneau.rest.docs.SwaggerMixin} —
 * 		spec endpoint at {@code /api}, Swagger v2 + Swagger-UI swap.
 * 	<li class='jc'>{@link org.apache.juneau.rest.docs.SwaggerUiMixin} —
 * 		HTML-first mount at {@code /swagger}, transitively pulls in {@code SwaggerMixin}.
 * 	<li class='jc'>{@link org.apache.juneau.rest.docs.OpenApiMixin} —
 * 		spec endpoints at {@code /openapi}, {@code /openapi.json}, and {@code /openapi.yaml};
 * 		OpenAPI 3.1 + Redoc swap on the content-negotiated {@code /openapi} mount.
 * 	<li class='jc'>{@link org.apache.juneau.rest.docs.RedocMixin} —
 * 		HTML-first mount at {@code /redoc}, transitively pulls in {@code OpenApiMixin}.
 * </ul>
 *
 * <h5 class='section'>Composition examples:</h5>
 *
 * <p class='bjava'>
 * 	<jc>// OpenAPI 3.1 spec only — /openapi, /openapi.json, /openapi.yaml.</jc>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=OpenApiMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet { ... }
 *
 * 	<jc>// OpenAPI 3.1 + Redoc — /openapi, /openapi.json, /openapi.yaml, /redoc.</jc>
 * 	<jc>// Transitive mixin resolution pulls OpenApiMixin in for free.</jc>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=RedocMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet { ... }
 *
 * 	<jc>// Everything — /api, /swagger, /openapi, /openapi.json, /openapi.yaml, /redoc.</jc>
 * 	<jc>// This is the post-9.5.0 default for BasicRestServlet / BasicRestResource subclasses.</jc>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins={SwaggerUiMixin.<jk>class</jk>, RedocMixin.<jk>class</jk>})
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet { ... }
 * </p>
 *
 * <p>
 * The two UI mixins ({@link org.apache.juneau.rest.docs.SwaggerUiMixin} and
 * {@link org.apache.juneau.rest.docs.RedocMixin}) declare {@code @Rest(defaultAccept="text/html")}
 * so that bare browser requests (no {@code Accept} header, or {@code Accept: *}{@code /*}) render the
 * HTML view rather than the serializer-default media type. The two spec mixins use Juneau's standard
 * content negotiation unchanged.
 * </p>
 *
 * <p>
 * The OpenAPI {@code .json} and {@code .yaml} variants are <em>format-pinned</em> — they ignore the
 * request {@code Accept} header and always return the file extension's wire format. This is
 * implemented by explicit serializer dispatch inside the mixin, not by content negotiation, so the
 * pin is robust against {@code Accept: text/html} or any other request media type.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server — Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.docs;
