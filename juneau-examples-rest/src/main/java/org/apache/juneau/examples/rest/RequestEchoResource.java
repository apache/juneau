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
package org.apache.juneau.examples.rest;

import static org.apache.juneau.html.HtmlDocSerializerContext.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.transforms.*;

/**
 * Sample REST resource for echoing HttpServletRequests back to the browser.
 */
@RestResource(
	path="/echo",
	title="Request echo service",
	description="Echos the current HttpServletRequest object back to the browser.",
	pageLinks="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/RequestEchoResource.java'}",
	properties={
		@Property(name=SERIALIZER_maxDepth, value="5"),
		@Property(name=SERIALIZER_detectRecursions, value="true")
	},
	beanFilters={
		// Interpret these as their parent classes, not subclasses
		HttpServletRequest.class, HttpSession.class, ServletContext.class,
	},
	pojoSwaps={
		// Add a special filter for Enumerations
		EnumerationSwap.class
	}
)
public class RequestEchoResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** GET request handler */
	@RestMethod(name="GET", path="/*", converters={Traversable.class,Queryable.class}, summary="Serializes the incoming HttpServletRequest object.")
	public HttpServletRequest doGet(RestRequest req, RestResponse res, @Properties ObjectMap properties) {
		// Set the HtmlDocSerializer title programmatically.
		res.setPageTitle("Contents of HttpServletRequest object");

		// Just echo the request back as the response.
		return req;
	}
}
