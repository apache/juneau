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

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Method;
import org.apache.juneau.rest.labels.*;
import org.apache.juneau.utils.*;

/**
 * Sample REST resource that shows how to define REST methods.
 */
@RestResource(
	path="/methodExample",
	messages="nls/MethodExampleResource",
	htmldoc=@HtmlDoc(
		links="{up:'servlet:/..',options:'servlet:/?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/MethodExampleResource.java'}",
		aside=""
			+ "<div style='max-width:400px' class='text'>"
			+ "	<p>Shows the different methods for retrieving HTTP query/form-data parameters, headers, and path variables.</p>"
			+ "	<p>Each method is functionally equivalent but demonstrate different ways to accomplish the same tasks.</p>"
			+ "</div>"
	)
)
public class MethodExampleResource extends Resource {
	private static final long serialVersionUID = 1L;
	
	private static final UUID SAMPLE_UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
	private static final String SAMPLE_UUID_STRING = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

	/** Example GET request that redirects to our example method */
	@RestMethod(name="GET", path="/")
	public ResourceDescription[] doExample() throws Exception {
		return new ResourceDescription[] {
			new ResourceDescription("example1/foo/123/"+SAMPLE_UUID+"/path-remainder?q1=456&q2=bar", "Example 1 - Annotated method attributes."),
			new ResourceDescription("example2/foo/123/"+SAMPLE_UUID+"/path-remainder?q1=456&q2=bar", "Example 2 - Low-level RestRequest/RestResponse objects."),
			new ResourceDescription("example3/foo/123/"+SAMPLE_UUID+"/path-remainder?q1=456&q2=bar", "Example 3 - Intermediate-level APIs.")
		};
	}

	/** 
	 * Methodology #1 - GET request using annotated attributes.
	 * This approach uses annotated parameters for retrieving input.
	 */
	@RestMethod(name="GET", path="/example1/{p1}/{p2}/{p3}/*")
	public Map<String,Object> example1(
			@Method String method,                  // HTTP method.
			@Path String p1,                        // Path variables.
			@Path int p2,
			@Path UUID p3,
			@Query("q1") int q1,                    // Query parameters.
			@Query("q2") String q2,
			@Query(name="q3",def=SAMPLE_UUID_STRING) UUID q3,
			@PathRemainder String remainder,        // Path remainder after pattern match.
			@Header("Accept-Language") String lang, // Headers.
			@Header("Accept") String accept,
			@Header(name="DNT",def="1") Integer doNotTrack
		) {

		// Send back a simple Map response
		return new AMap<String,Object>()
			.append("method", method)
			.append("path-p1", p1)
			.append("path-p2", p2)
			.append("path-p3", p3)
			.append("remainder", remainder)
			.append("query-q1", q1)
			.append("query-q2", q2)
			.append("query-q3", q3)
			.append("header-lang", lang)
			.append("header-accept", accept)
			.append("header-doNotTrack", doNotTrack);
	}

	/** 
	 * Methodology #2 - GET request using methods on RestRequest and RestResponse.
	 * This approach uses low-level request/response objects to perform the same as above.
	 */
	@RestMethod(name="GET", path="/example2/{p1}/{p2}/{p3}/*")
	public void example2(
			RestRequest req,          // A direct subclass of HttpServletRequest.
			RestResponse res          // A direct subclass of HttpServletResponse.
		) throws Exception {
		
		// HTTP method.
		String method = req.getMethod();

		// Path variables.
		RequestPathMatch path = req.getPathMatch();
		String p1 = path.get("p1", String.class);
		int p2 = path.get("p2", int.class);
		UUID p3 = path.get("p3", UUID.class);

		// Query parameters.
		RequestQuery query = req.getQuery();
		int q1 = query.get("q1", 0, int.class);
		String q2 = query.get("q2", String.class);
		UUID q3 = query.get("q3", SAMPLE_UUID, UUID.class);

		// Path remainder after pattern match.
		String remainder = req.getPathMatch().getRemainder();

		// Headers.
		String lang = req.getHeader("Accept-Language");
		String accept = req.getHeader("Accept");
		int doNotTrack = req.getHeaders().get("DNT", 1, int.class);

		// Send back a simple Map response
		Map<String,Object> m = new AMap<String,Object>()
			.append("method", method)
			.append("path-p1", p1)
			.append("path-p2", p2)
			.append("path-p3", p3)
			.append("remainder", remainder)
			.append("query-q1", q1)
			.append("query-q2", q2)
			.append("query-q3", q3)
			.append("header-lang", lang)
			.append("header-accept", accept)
			.append("header-doNotTrack", doNotTrack);
		res.setOutput(m);
	}

	/** 
	 * Methodology #3 - GET request using special objects.
	 * This approach uses intermediate-level APIs.
	 * The framework recognizes the parameter types and knows how to resolve them.
	 */
	@RestMethod(name="GET", path="/example3/{p1}/{p2}/{p3}/*")
	public Map<String,Object> example3(
		HttpMethod method,           // HTTP method.
		RequestPathMatch path,       // Path variables.
		RequestQuery query,          // Query parameters.
		RequestHeaders headers,      // Headers.
		AcceptLanguage lang,         // Specific header classes.
		Accept accept
	) throws Exception {

		// Path variables.
		String p1 = path.get("p1", String.class);
		int p2 = path.get("p2", int.class);
		UUID p3 = path.get("p3", UUID.class);

		// Query parameters.
		int q1 = query.get("q1", 0, int.class);
		String q2 = query.get("q2", String.class);
		UUID q3 = query.get("q3", SAMPLE_UUID, UUID.class);

		// Path remainder after pattern match.
		String remainder = path.getRemainder();

		// Headers.
		int doNotTrack = headers.get("DNT", 1, int.class);

		// Send back a simple Map response
		return new AMap<String,Object>()
			.append("method", method)
			.append("path-p1", p1)
			.append("path-p2", p2)
			.append("path-p3", p3)
			.append("remainder", remainder)
			.append("query-q1", q1)
			.append("query-q2", q2)
			.append("query-q3", q3)
			.append("header-lang", lang)
			.append("header-accept", accept)
			.append("header-doNotTrack", doNotTrack);
	}	
}
