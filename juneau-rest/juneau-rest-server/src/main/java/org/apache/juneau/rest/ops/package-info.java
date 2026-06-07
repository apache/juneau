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
 * Ops / introspection mixin pack — composable {@code @Rest(mixins=...)} resources that ship the
 * operational surface (request echo, JVM admin, route index) every long-running Juneau service
 * eventually grows.
 *
 * <p>
 * Three sibling mixins compose into the host {@code @Rest}-annotated resource. Each mixin owns
 * its default mount paths, ships secure-by-default (debug-gated for echo, deny-all-guard for
 * admin), and is independently mountable; the three together drop in as a pack via
 * {@code @Rest(mixins={EchoMixin.class, AdminMixin.class, RouteIndexMixin.class})}.
 * </p>
 *
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link org.apache.juneau.rest.ops.EchoMixin} —
 * 		{@code /echo/*} and {@code /debug/echo/*} request echo, gated behind the host's
 * 		{@link org.apache.juneau.rest.debug.DebugEnablement DebugEnablement}; sensitive headers
 * 		({@code Authorization}, {@code Cookie}, etc.) are redacted by default.
 * 	<li class='jc'>{@link org.apache.juneau.rest.ops.AdminMixin} —
 * 		{@code /admin/threads}, {@code /admin/heap}, {@code /admin/cache/flush} (POST), and
 * 		{@code /admin/ratelimit}; default-deny via
 * 		{@link org.apache.juneau.rest.guard.DenyAllGuard} until the importer registers an
 * 		{@code @Bean RestGuardList} factory.
 * 	<li class='jc'>{@link org.apache.juneau.rest.ops.RouteIndexMixin} —
 * 		{@code /options} and {@code /routes} returning a JSON list of every
 * 		{@code @RestOp}-annotated method on the host (plus mixins), excluding
 * 		{@code @OpSwagger(ignore=true)} ops.
 * </ul>
 *
 * <h5 class='section'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(
 * 		path=<js>"/api"</js>,
 * 		mixins={
 * 			EchoMixin.<jk>class</jk>,
 * 			AdminMixin.<jk>class</jk>,
 * 			RouteIndexMixin.<jk>class</jk>
 * 		},
 * 		debug=<js>"conditional"</js>            <jc>// gates EchoMixin per-request</jc>
 * 	)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 *
 * 		<jc>// Required: register an auth guard chain; replaces the deny-all default.</jc>
 * 		<ja>@Bean</ja>(name=<js>"guards"</js>)
 * 		<jk>public</jk> RestGuardList guards(BeanStore <jv>bs</jv>) {
 * 			<jk>return</jk> RestGuardList.<jsm>create</jsm>(<jv>bs</jv>)
 * 				.append(<jk>new</jk> MyAuthGuard())
 * 				.build();
 * 		}
 *
 * 		<ja>@Bean</ja> AdminMixin admin() {
 * 			<jk>return</jk> AdminMixin.<jsm>create</jsm>()
 * 				.cacheFlush(<js>"primary"</js>, () -&gt; primaryCache.invalidateAll())
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * All endpoints carry
 * {@link org.apache.juneau.rest.OpSwagger#ignore() @OpSwagger(ignore=true)} —
 * ops endpoints are not API-meaningful and are excluded from any generated Swagger / OpenAPI
 * spec.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server — Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.ops;
