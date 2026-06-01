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
 * Thymeleaf view-rendering bridge &mdash; opt-in {@code juneau-rest-server-view-thymeleaf} module.
 *
 * <p>
 * Provides three sibling pieces that together let a Juneau REST resource render Thymeleaf
 * templates:
 *
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.thymeleaf.ThymeleafMixin} &mdash;
 * 		mixin that renders raw {@code .html} templates from the importer's classpath under
 * 		{@code /thymeleaf/*} and auto-registers
 * 		{@link org.apache.juneau.rest.view.thymeleaf.ThymeleafViewRenderer}.
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.thymeleaf.ThymeleafView} &mdash; immutable
 * 		value class (implements the core {@link org.apache.juneau.rest.view.View View} interface)
 * 		returned from {@code @RestOp} methods to ask the framework to render a Thymeleaf template.
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.thymeleaf.ThymeleafViewRenderer} &mdash;
 * 		{@link org.apache.juneau.rest.processor.ResponseProcessor ResponseProcessor} that detects
 * 		{@link org.apache.juneau.rest.view.thymeleaf.ThymeleafView ThymeleafView} returns and
 * 		dispatches them to the configured
 * 		{@link org.thymeleaf.TemplateEngine TemplateEngine} via
 * 		{@code engine.process(templateName, context, writer)}.
 * </ul>
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=ThymeleafMixin.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> ThymeleafMixin thymeleaf() {
 * 			<jk>return</jk> ThymeleafMixin.<jsm>create</jsm>()
 * 				.basePath(<js>"/templates/"</js>)
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> ThymeleafView.<jsm>of</jsm>(<js>"hello"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Engine-agnostic packaging:</h5>
 *
 * <p>
 * The bridge module's main POM declares <b>only</b> {@code org.thymeleaf:thymeleaf} in
 * {@code provided} scope &mdash; <b>no concrete Thymeleaf transitive dep</b> ships here. Consumers
 * add the engine matching their deployment:
 *
 * <ul class='spaced-list'>
 * 	<li><b>Spring Boot:</b> add {@code org.springframework.boot:spring-boot-starter-thymeleaf}
 * 		&mdash; brings in {@code thymeleaf} + {@code thymeleaf-spring6} + autoconfigured
 * 		{@code SpringTemplateEngine}. The bridge picks the engine up automatically via
 * 		{@code BeanStore.getBean(TemplateEngine.class)}.
 * 	<li><b>Juneau microservice / Jetty:</b> add {@code org.thymeleaf:thymeleaf} directly. With no
 * 		user-supplied engine bean, the bridge builds a default {@link org.thymeleaf.TemplateEngine
 * 		TemplateEngine} on first request anchored on the importer's classloader.
 * 	<li><b>Custom:</b> register your own {@code @Bean TemplateEngine} with whatever resolvers,
 * 		dialects, and template modes you need.
 * </ul>
 *
 * <p>
 * When no Thymeleaf engine is on the classpath, the renderer surfaces a clear diagnostic naming
 * the missing dependency and linking to the "Choosing a TemplateEngine" matrix in
 * {@code ThymeleafViewSupport.md}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.View}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ThymeleafViewSupport">Thymeleaf View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.view.thymeleaf;
