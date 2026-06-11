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
 * Mustache view-rendering bridge &mdash; opt-in {@code juneau-rest-server-view-mustache} module.
 *
 * <p>
 * Provides three sibling pieces that together let a Juneau REST resource render Mustache
 * templates via <a href="https://github.com/spullara/mustache.java">mustache.java</a>:
 *
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.view.mustache.MustacheMixin} &mdash;
 * 		mixin that renders raw Mustache templates from the importer's classpath under
 * 		{@code /mustache/*} and auto-registers
 * 		{@link org.apache.juneau.rest.server.view.mustache.MustacheViewRenderer}.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.view.mustache.MustacheView} &mdash; immutable
 * 		value class (implements the core {@link org.apache.juneau.rest.server.view.View View} interface)
 * 		returned from {@code @RestOp} methods to ask the framework to render a Mustache template.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.view.mustache.MustacheViewRenderer} &mdash;
 * 		{@link org.apache.juneau.rest.server.processor.ResponseProcessor ResponseProcessor} that detects
 * 		{@link org.apache.juneau.rest.server.view.mustache.MustacheView MustacheView} returns and
 * 		dispatches them to the configured
 * 		{@link com.github.mustachejava.MustacheFactory MustacheFactory} via
 * 		{@code factory.compile(templateName).execute(writer, scope)}.
 * </ul>
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
 * <h5 class='section'>Engine-agnostic packaging:</h5>
 *
 * <p>
 * The bridge module's main POM declares <b>only</b>
 * {@code com.github.spullara.mustache.java:compiler} in {@code provided} scope &mdash; <b>no
 * concrete Mustache transitive dep</b> ships here. Consumers add the engine matching their
 * deployment:
 *
 * <ul class='spaced-list'>
 * 	<li><b>Juneau microservice / Jetty / Spring Boot:</b> add
 * 		{@code com.github.spullara.mustache.java:compiler}. With no user-supplied factory bean,
 * 		the bridge builds a default {@link com.github.mustachejava.MustacheFactory
 * 		MustacheFactory} on first request anchored on the importer's classpath under the
 * 		configured base path.
 * 	<li><b>Custom:</b> register your own {@code @Bean MustacheFactory} with whatever resolvers,
 * 		object handlers, and encoders you need.
 * </ul>
 *
 * <p>
 * When no Mustache engine is on the classpath, the renderer surfaces a clear diagnostic naming
 * the missing dependency and linking to the "Choosing a MustacheFactory" matrix in
 * {@code MustacheViewSupport.md}.
 *
 * <h5 class='section'>Note on Spring Boot's mustache starter:</h5>
 *
 * <p>
 * Spring Boot's {@code spring-boot-starter-mustache} pulls in {@code com.samskivert:jmustache}
 * (a separate Mustache implementation), <b>not</b> mustache.java. The bridge is mustache.java-
 * specific; consumers who want to use jmustache instead supply their own
 * {@link org.apache.juneau.rest.server.processor.ResponseProcessor ResponseProcessor} rather than
 * {@link org.apache.juneau.rest.server.view.mustache.MustacheViewRenderer MustacheViewRenderer}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.view.View}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MustacheViewSupport">Mustache View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerCompositionMixinsAndPaths">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.view.mustache;
