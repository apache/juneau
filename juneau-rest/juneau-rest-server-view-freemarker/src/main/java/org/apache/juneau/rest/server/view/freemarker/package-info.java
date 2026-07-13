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
 * Apache FreeMarker view-rendering bridge &mdash; opt-in {@code juneau-rest-server-view-freemarker}
 * module.
 *
 * <p>
 * Provides three sibling pieces that together let a Juneau REST resource render
 * <a href="https://freemarker.apache.org/">Apache FreeMarker</a> templates:
 *
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin} &mdash;
 * 		mixin that renders raw FreeMarker templates from the importer's classpath under
 * 		{@code /freemarker/*} and auto-registers
 * 		{@link org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer}.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.view.freemarker.FreemarkerView} &mdash; immutable
 * 		value class (implements the core {@link org.apache.juneau.rest.server.view.View View} interface)
 * 		returned from {@code @RestOp} methods to ask the framework to render a FreeMarker
 * 		template.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer} &mdash;
 * 		{@link org.apache.juneau.rest.server.processor.ResponseProcessor ResponseProcessor} that detects
 * 		{@link org.apache.juneau.rest.server.view.freemarker.FreemarkerView FreemarkerView} returns and
 * 		dispatches them to the configured
 * 		{@link freemarker.template.Configuration Configuration} via
 * 		{@code cfg.getTemplate(templateName).process(dataModel, writer)}.
 * </ul>
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=FreemarkerMixin.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> FreemarkerMixin freemarker() {
 * 			<jk>return</jk> FreemarkerMixin.<jsm>create</jsm>()
 * 				.basePath(<js>"/templates/"</js>)
 * 				.templateSuffix(<js>".ftlh"</js>)
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> FreemarkerView.<jsm>of</jsm>(<js>"hello"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Engine-agnostic packaging:</h5>
 *
 * <p>
 * The bridge module's main POM declares <b>only</b> {@code org.freemarker:freemarker} in
 * {@code provided} scope &mdash; <b>no concrete FreeMarker transitive dep</b> ships here.
 * Consumers add the engine matching their deployment:
 *
 * <ul class='spaced-list'>
 * 	<li><b>Juneau microservice / Jetty:</b> add {@code org.freemarker:freemarker}. With no
 * 		user-supplied configuration bean, the bridge builds a default
 * 		{@link freemarker.template.Configuration Configuration} on first request anchored on the
 * 		importer's classpath under the configured base path.
 * 	<li><b>Spring Boot:</b> add {@code spring-boot-starter-freemarker}. Spring Boot
 * 		autoconfigures a {@code freemarker.template.Configuration} bean which the bridge picks
 * 		up automatically via {@code BeanStore.getBean(Configuration.class)} &mdash; no further
 * 		wiring required.
 * 	<li><b>Custom:</b> register your own {@code @Bean freemarker.template.Configuration} with
 * 		whatever loaders / encodings / output formats / template-update policies you need.
 * </ul>
 *
 * <p>
 * When no FreeMarker engine is on the classpath, the renderer surfaces a clear diagnostic
 * naming the missing dependency and linking to the "Choosing a Configuration" matrix in
 * {@code FreemarkerViewSupport.md}.
 *
 * <h5 class='section'>{@code .ftl} vs {@code .ftlh}:</h5>
 *
 * <p>
 * FreeMarker auto-selects HTML escaping by file extension: {@code .ftlh} templates emit
 * HTML-escaped output, while {@code .ftl} templates emit raw output. For HTML responses, prefer
 * {@code .ftlh} so a future attribute-binding change can't introduce an XSS regression.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.view.View}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.view.freemarker;
