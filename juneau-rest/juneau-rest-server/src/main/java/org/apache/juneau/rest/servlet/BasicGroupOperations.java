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

import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.openapi3.OpenApi;
import org.apache.juneau.bean.openapi3.ui.*;
import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.bean.swagger.ui.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.matcher.*;

import jakarta.servlet.http.*;

/**
 * Basic REST group operation methods.
 *
 * <p>
 * 	Defines 3 special use REST operation endpoints rooted at <c>/</c>:
 * </p>
 *
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>)
 * 	<jk>public</jk> {@link ChildResourceDescriptions} {@link #getChildren(RestRequest) getChildren}({@link RestRequest} <jv>req</jv>);
 *
 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, matchers=HasSwaggerQueryParam.<jk>class</jk>)
 * 	<jk>public</jk> {@link Swagger} {@link #getChildrenSwagger(RestRequest) getChildrenSwagger}({@link RestRequest} <jv>req</jv>);
 *
 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, matchers=HasOpenApiQueryParam.<jk>class</jk>)
 * 	<jk>public</jk> {@link OpenApi} {@link #getChildrenOpenApi(RestRequest) getChildrenOpenApi}({@link RestRequest} <jv>req</jv>);
 * </p>
 *
 * <p>
 * 	The two extra endpoints overload <c>GET /</c> when the request carries a <c>?Swagger</c> or <c>?OpenApi</c>
 * 	query parameter, returning the corresponding spec document for the group resource inline. Without those query
 * 	parameters, {@link #getChildren(RestRequest)} produces the standard navigation page. Each handler respects
 * 	{@link Rest#apiFormat()}: the Swagger handler returns <c>404</c> when <c>apiFormat="openapi"</c>, and the
 * 	OpenAPI handler returns <c>404</c> when <c>apiFormat="swagger"</c> (the default).
 * </p>
 *
 * <p>
 * 	Implementations provided by the following classes:
 * </p>
 * <ul class='javatreec'>
 * 	<li class='jac'>{@link BasicRestServletGroup}
 * 	<li class='jac'>{@link BasicRestObjectGroup}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
public interface BasicGroupOperations {

	/**
	 * Matcher that fires when the request carries a <c>?Swagger</c> query parameter (with any value).
	 *
	 * <p>
	 * Used by {@link #getChildrenSwagger(RestRequest)} to overload <c>GET /</c> on a group resource so that
	 * callers can fetch the Swagger v2 document for the group inline, alongside the standard navigation page.
	 */
	class HasSwaggerQueryParam extends RestMatcher {
		@Override /* Overridden from RestMatcher */
		public boolean matches(HttpServletRequest req) {
			return req.getParameter("Swagger") != null;
		}
	}

	/**
	 * Matcher that fires when the request carries a <c>?OpenApi</c> query parameter (with any value).
	 *
	 * <p>
	 * Used by {@link #getChildrenOpenApi(RestRequest)} to overload <c>GET /</c> on a group resource so that
	 * callers can fetch the OpenAPI 3.1 document for the group inline.
	 */
	class HasOpenApiQueryParam extends RestMatcher {
		@Override /* Overridden from RestMatcher */
		public boolean matches(HttpServletRequest req) {
			return req.getParameter("OpenApi") != null;
		}
	}

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
	@RestGet(path = "/", summary = "Navigation page")
	default ChildResourceDescriptions getChildren(RestRequest req) {
		return ChildResourceDescriptions.of(req);
	}

	/**
	 * [GET /?Swagger] - Returns the Swagger v2 document for this group resource.
	 *
	 * <p>
	 * Overloads {@link #getChildren(RestRequest)} when the request carries a <c>?Swagger</c> query parameter.
	 * Returns <c>404</c> when {@link Rest#apiFormat()} resolves to {@code "openapi"} (canonical endpoint shifts
	 * to <c>/openapi/*</c> in that mode).
	 *
	 * @param req The HTTP request.
	 * @return The Swagger v2 document for this resource.
	 */
	@RestGet(
		path = "/",
		summary = "Swagger documentation (inline)",
		matchers = HasSwaggerQueryParam.class
	)
	@HtmlDocConfig(
		rank = 10,
		navlinks = { "back: servlet:/", "json: servlet:/?Swagger=true&Accept=text/json&plainText=true" },
		aside = "NONE"
	)
	@MarshalledConfig(swaps = { SwaggerUI.class })
	default Swagger getChildrenSwagger(RestRequest req) {
		if (RestServerConstants.API_FORMAT_OPENAPI.equals(req.getContext().getApiFormat()))
			throw new NotFound();
		return req.getSwagger().orElseThrow(NotFound::new);
	}

	/**
	 * [GET /?OpenApi] - Returns the OpenAPI 3.1 document for this group resource.
	 *
	 * <p>
	 * Overloads {@link #getChildren(RestRequest)} when the request carries a <c>?OpenApi</c> query parameter.
	 * Returns <c>404</c> when {@link Rest#apiFormat()} resolves to {@code "swagger"} (the default — the
	 * <c>/openapi/*</c> sibling is not mounted in that mode and neither is this query mirror).
	 *
	 * @param req The HTTP request.
	 * @return The OpenAPI 3.1 document for this resource.
	 */
	@RestGet(
		path = "/",
		summary = "OpenAPI 3.1 documentation (inline)",
		matchers = HasOpenApiQueryParam.class
	)
	@HtmlDocConfig(
		rank = 10,
		navlinks = { "back: servlet:/", "json: servlet:/?OpenApi=true&Accept=text/json&plainText=true" },
		aside = "NONE"
	)
	@MarshalledConfig(swaps = { RedocUI.class })
	default OpenApi getChildrenOpenApi(RestRequest req) {
		if (RestServerConstants.API_FORMAT_SWAGGER.equals(req.getContext().getApiFormat()))
			throw new NotFound();
		return req.getOpenApi().orElseThrow(NotFound::new);
	}
}