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
package org.apache.juneau.rest.servlet;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.ops.*;

import jakarta.servlet.*;

/**
 * Specialized subclass of {@link BasicRestServlet} for showing "group" pages.
 *
 * <p>
 * Meant as a base class for top-level REST resources in servlet containers.
 *
 * <p>
 * Provides support for JSON, XML, HTML, URL-Encoding, UON, XML, OpenAPI, and MessagePack.  See {@link BasicUniversalConfig}
 * for details.
 *
 * <p>
 * Adds the group-navigation endpoint ({@code GET /}) by composing {@link NavigationMixin} via
 * {@link Rest#mixins() @Rest(mixins=...)} on top of the residual op-mixins inherited from
 * {@link BasicRestServlet}. The navigation op lives on the {@code NavigationMixin} sub-context (backed by
 * {@link RestMixin#getHostContext()}) but renders with the host's
 * {@link org.apache.juneau.html.annotation.HtmlDocConfig @HtmlDocConfig} page decoration &mdash; mixin
 * sub-contexts inherit the host's class-level {@code @HtmlDocConfig} (and other class-level config), so the
 * navigation page matches the host's other endpoints.
 *
 * <p>
 * Children are attached to this resource using the {@link Rest#children() @Rest(children)} annotation.
 *
 * <h5 class='section'>Dynamic children:</h5>
 *
 * <p>
 * Additional child resources can be registered or unregistered at runtime via {@link #addChild(Class)} /
 * {@link #removeChild(String)} (and overloads). This is particularly useful in test fixtures where a single
 * group servlet is mounted once and individual tests plug in their own {@code @Rest} resources without restarting
 * the servlet container.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(mixins=NavigationMixin.class)
public abstract class BasicRestServletGroup extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Returns the {@link RestChildren} registry backing this group resource.
	 *
	 * <p>
	 * Use this for full access to the dynamic add/remove API, including the {@code replace} overloads and the
	 * unmodifiable {@link RestChildren#asMap() asMap()} snapshot.
	 *
	 * @return The {@link RestChildren} registry for this resource's {@link RestContext}.
	 */
	public RestChildren getChildResources() {
		return getContext().getRestChildren();
	}

	/**
	 * Dynamically registers a child REST resource by class.
	 *
	 * <p>
	 * Convenience pass-through to {@link RestChildren#addChild(Class)} on {@link #getChildResources()}.
	 *
	 * @param resourceClass The {@code @Rest}-annotated resource class.
	 * @return The newly-built child {@link RestContext}.
	 * @throws ServletException If construction or lifecycle initialization of the child fails.
	 */
	public RestContext addChild(Class<?> resourceClass) throws ServletException {
		return getChildResources().addChild(resourceClass);
	}

	/**
	 * Dynamically registers a pre-instantiated child REST resource.
	 *
	 * <p>
	 * Convenience pass-through to {@link RestChildren#addChild(Object)} on {@link #getChildResources()}.
	 *
	 * @param resource The {@code @Rest}-annotated resource instance.
	 * @return The newly-built child {@link RestContext}.
	 * @throws ServletException If construction or lifecycle initialization of the child fails.
	 */
	public RestContext addChild(Object resource) throws ServletException {
		return getChildResources().addChild(resource);
	}

	/**
	 * Dynamically registers a pre-instantiated child REST resource at an explicit path.
	 *
	 * <p>
	 * Convenience pass-through to {@link RestChildren#addChild(String, Object)} on {@link #getChildResources()}.
	 *
	 * @param path The path segment under this group at which to mount the child.
	 * @param resource The {@code @Rest}-annotated resource instance.
	 * @return The newly-built child {@link RestContext}.
	 * @throws ServletException If construction or lifecycle initialization of the child fails.
	 */
	public RestContext addChild(String path, Object resource) throws ServletException {
		return getChildResources().addChild(path, resource);
	}

	/**
	 * Removes the child registered at the given composed path.
	 *
	 * <p>
	 * Convenience pass-through to {@link RestChildren#removeChild(String)} on {@link #getChildResources()}.
	 *
	 * @param path The composed path key (as returned by {@link RestContext#getPath()}).
	 * @return The removed and destroyed {@link RestContext}, or {@code null} if no child was registered there.
	 */
	public RestContext removeChild(String path) {
		return getChildResources().removeChild(path);
	}

	/**
	 * Removes the first child whose resource class matches the given type.
	 *
	 * <p>
	 * Convenience pass-through to {@link RestChildren#removeChild(Class)} on {@link #getChildResources()}.
	 *
	 * @param resourceClass The resource class to match.
	 * @return The removed and destroyed {@link RestContext}, or {@code null} if no matching child was found.
	 */
	public RestContext removeChild(Class<?> resourceClass) {
		return getChildResources().removeChild(resourceClass);
	}
}
