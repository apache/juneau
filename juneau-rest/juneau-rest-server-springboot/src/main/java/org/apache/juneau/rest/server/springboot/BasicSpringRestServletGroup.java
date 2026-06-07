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
package org.apache.juneau.rest.server.springboot;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.ops.*;

/**
 * Specialized subclass of {@link BasicSpringRestServlet} for showing "group" pages.
 *
 * <p>
 * Meant as a base class for top-level REST resources in Spring Boot environments.
 *
 * <p>
 * Provides support for JSON, XML, HTML, URL-Encoding, UON, XML, OpenAPI, and MessagePack.  See {@link BasicUniversalConfig}
 * for details.
 *
 * <p>
 * Adds the group-navigation endpoint ({@code GET /}) by composing {@link NavigationMixin} via
 * {@link Rest#mixins() @Rest(mixins=...)} on top of the residual op-mixins inherited from
 * {@link BasicSpringRestServlet}. The navigation op lives on the {@code NavigationMixin} sub-context (backed
 * by {@link org.apache.juneau.rest.server.servlet.RestMixin#getHostContext()}) but renders with the host's
 * {@link org.apache.juneau.html.HtmlDocConfig @HtmlDocConfig} page decoration &mdash; mixin
 * sub-contexts inherit the host's class-level {@code @HtmlDocConfig} (and other class-level config), so the
 * navigation page matches the host's other endpoints.
 *
 * <p>
 * Children are attached to this resource using the {@link Rest#children() @Rest(children)} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringbootBasics">juneau-rest-server-springboot Basics</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(mixins=NavigationMixin.class)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for BasicSpringRestServletGroup hierarchy
})
public abstract class BasicSpringRestServletGroup extends BasicSpringRestServlet {
	private static final long serialVersionUID = 1L;
}