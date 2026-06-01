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
package org.apache.juneau.rest.springboot;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.config.*;

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
 * Adds the group-navigation endpoint ({@code GET /}) as a concrete method on top of the residual op-mixins
 * inherited from {@link BasicSpringRestServlet}. The navigation page is rendered as a method of the host
 * resource (rather than a sub-context mixin) so it inherits the host's
 * {@link org.apache.juneau.html.annotation.HtmlDocConfig @HtmlDocConfig} page decoration.
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
@Rest
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for BasicSpringRestServletGroup hierarchy
})
public abstract class BasicSpringRestServletGroup extends BasicSpringRestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * [GET /] - Get child resources.
	 *
	 * <p>
	 * Returns a bean that lists and allows navigation to child resources. Default implementation
	 * delegates to {@link ChildResourceDescriptions#of(RestRequest)}; subclasses may override.
	 *
	 * @param req The HTTP request.
	 * @return The bean containing links to the child resources.
	 */
	@RestGet(path="/", summary="Navigation page")
	public ChildResourceDescriptions getChildren(RestRequest req) {
		return ChildResourceDescriptions.of(req);
	}
}