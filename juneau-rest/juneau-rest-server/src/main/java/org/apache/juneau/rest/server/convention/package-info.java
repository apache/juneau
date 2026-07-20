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
 * Convention-endpoints mixin pack — composable {@code @Rest(mixins=...)} resources that ship the
 * "internet conventions" surface (favicon, robots, sitemap, version, well-known) every public-facing
 * service eventually grows.
 *
 * <p>
 * Four sibling mixins compose into the host {@code @Rest}-annotated resource. Each mixin owns its
 * default mount paths and is independently mountable; the four together drop in as a pack via
 * {@code @Rest(mixins={FaviconMixin.class, SeoMixin.class, VersionMixin.class, WellKnownMixin.class})}.
 * </p>
 *
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.convention.FaviconMixin} —
 * 		{@code /favicon.ico} with a 30-day {@code Cache-Control} and a default Juneau-branded icon
 * 		on the framework classpath.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.convention.SeoMixin} —
 * 		{@code /robots.txt} (deny-all by default) and {@code /sitemap.xml} (empty
 * 		{@code <urlset>} by default); both builder-driven.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.convention.VersionMixin} —
 * 		{@code /version} (SVL-configurable) returning a JSON metadata
 * 		map; defaults read {@code MANIFEST.MF} + {@code git.properties} from the classpath.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.convention.WellKnownMixin} —
 * 		{@code /.well-known/security.txt} per RFC 9116; 404 unless explicitly configured.
 * </ul>
 *
 * <h5 class='section'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(
 * 		path=<js>"/api"</js>,
 * 		mixins={
 * 			FaviconMixin.<jk>class</jk>,
 * 			SeoMixin.<jk>class</jk>,
 * 			VersionMixin.<jk>class</jk>,
 * 			WellKnownMixin.<jk>class</jk>
 * 		}
 * 	)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 * 		<ja>@Bean</ja> SeoMixin seo() {
 * 			<jk>return</jk> SeoMixin.<jsm>create</jsm>().robotsAllow(<js>"*"</js>, <js>"/"</js>).build();
 * 		}
 * 		<ja>@Bean</ja> WellKnownMixin wellKnown() {
 * 			<jk>return</jk> WellKnownMixin.<jsm>create</jsm>()
 * 				.securityTxt(<js>"Contact: security@example.com\nExpires: 2027-01-01T00:00:00Z\n"</js>)
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * All four endpoints are excluded from generated Swagger / OpenAPI specs via
 * {@link org.apache.juneau.rest.server.OpSwagger#ignore() @OpSwagger(ignore=true)} —
 * convention endpoints are not API-meaningful.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server — Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.convention;
