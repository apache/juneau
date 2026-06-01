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
 * JSP view-rendering bridge &mdash; opt-in {@code juneau-rest-server-view-jsp} module.
 *
 * <p>
 * Provides three sibling pieces that together let a Juneau REST resource render JSP templates:
 *
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.jsp.JspMixin} &mdash; mixin that
 * 		serves raw {@code .jsp} resources from the importer's classpath under
 * 		{@code /jsp/*} and auto-registers {@link org.apache.juneau.rest.view.jsp.JspViewRenderer}.
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.jsp.JspView} &mdash; immutable value class
 * 		(implements the core {@link org.apache.juneau.rest.view.View View} interface) returned
 * 		from {@code @RestOp} methods to ask the framework to render a JSP.
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.jsp.JspViewRenderer} &mdash;
 * 		{@link org.apache.juneau.rest.processor.ResponseProcessor ResponseProcessor} that detects
 * 		{@link org.apache.juneau.rest.view.jsp.JspView JspView} returns and dispatches via
 * 		{@link jakarta.servlet.RequestDispatcher#forward
 * 		ServletContext.getRequestDispatcher(...).forward(...)}.
 * </ul>
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=JspMixin.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> JspMixin jsp() {
 * 			<jk>return</jk> JspMixin.<jsm>create</jsm>()
 * 				.basePath(<js>"/WEB-INF/views/"</js>)
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> JspView.<jsm>of</jsm>(<js>"hello.jsp"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Engine-agnostic packaging:</h5>
 *
 * <p>
 * The bridge module's main POM declares <b>only</b> the JSP API + JSTL impl in {@code provided}
 * scope &mdash; <b>no JSP engine</b> ships here. Consumers add the engine matching their
 * container:
 *
 * <ul class='spaced-list'>
 * 	<li><b>Jetty 12 EE11</b> (microservice-jetty, Spring Boot embedded Jetty):
 * 		{@code org.eclipse.jetty.ee11:jetty-ee11-apache-jsp}.
 * 	<li><b>Embedded Tomcat</b> (Spring Boot default):
 * 		{@code org.apache.tomcat.embed:tomcat-embed-jasper}.
 * 	<li><b>External-WAR</b> deployments (Tomcat / JBoss / WildFly): engine is already on the
 * 		container's classpath; no additional dependency required.
 * </ul>
 *
 * <p>
 * When no engine is on the classpath, the renderer surfaces a clear diagnostic naming the
 * missing dependency and linking to the "Choosing a JSP engine" matrix in
 * {@code JspViewSupport.md}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.View}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JspViewSupport">JSP View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.view.jsp;
