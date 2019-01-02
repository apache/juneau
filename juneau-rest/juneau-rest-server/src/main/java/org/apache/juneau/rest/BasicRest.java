// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.jsonschema.JsonSchemaGenerator.*;

import javax.servlet.http.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.dto.swagger.ui.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Identical to {@link BasicRestServlet} but doesn't extend from {@link HttpServlet}
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.Instantiation.BasicRest}
 * </ul>
 */
@RestResource(

	// Allow OPTIONS requests to be simulated using ?method=OPTIONS query parameter.
	allowedMethodParams="OPTIONS",

	// HTML-page specific settings.
	htmldoc=@HtmlDoc(
		// Basic page navigation links.
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS"
		}
	)
)
public abstract class BasicRest implements BasicRestConfig {

	/**
	 * [OPTIONS /*] - Show resource options.
	 *
	 * @param req The HTTP request.
	 * @return A bean containing the contents for the OPTIONS page.
	 */
	@RestMethod(name=OPTIONS, path="/*",

		summary="Swagger documentation",
		description="Swagger documentation for this resource.",

		htmldoc=@HtmlDoc(
			// Override the nav links for the swagger page.
			navlinks={
				"back: servlet:/",
				"json: servlet:/?method=OPTIONS&Accept=text/json&plainText=true"
			},
			// Never show aside contents of page inherited from class.
			aside="NONE"
		),

		// POJO swaps to apply to all serializers/parsers on this method.
		pojoSwaps={
			// Use the SwaggerUI swap when rendering Swagger beans.
			// This is a per-media-type swap that only applies to text/html requests.
			SwaggerUI.class
		},

		// Properties to apply to all serializers/parsers and REST-specific API objects on this method.
		properties={
			// Add descriptions to the following types when not specified:
			@Property(name=JSONSCHEMA_addDescriptionsTo, value="bean,collection,array,map,enum"),

			// Add x-example to the following types:
			@Property(name=JSONSCHEMA_addExamplesTo, value="bean,collection,array,map"),

			// Don't generate schema information on the Swagger bean itself or HTML beans.
			@Property(name=JSONSCHEMA_ignoreTypes, value="Swagger,org.apache.juneau.dto.html5.*")
		},

		// Shortcut for boolean properties.
		flags={
			// Use $ref references for bean definitions to reduce duplication in Swagger.
			JSONSCHEMA_useBeanDefs,

			// When parsing generated beans, ignore unknown properties that may only exist as getters and not setters.
			BEAN_ignoreUnknownBeanProperties
		}
	)
	public Swagger getOptions(RestRequest req) {
		// Localized Swagger for this resource is available through the RestRequest object.
		return req.getSwagger();
	}
}
