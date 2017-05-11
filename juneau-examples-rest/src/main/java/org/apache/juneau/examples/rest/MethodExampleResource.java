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

/**
 * Sample REST resource that shows how to define REST methods.
 */
@RestResource(
	path="/methodExample",
	messages="nls/MethodExampleResource",
	pageLinks="{up:'$R{servletParentURI}',options:'?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/MethodExampleResource.java'}"
)
public class MethodExampleResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** Example GET request that redirects to our example method */
	@RestMethod(name="GET", path="/")
	public Redirect doExample() throws Exception {
		return new Redirect("example1/xxx/123/{0}/xRemainder?q1=123&q2=yyy", UUID.randomUUID());
	}

	/** 
	 * Methodology #1 - GET request using annotated attributes.
	 * This approach uses annotated parameters for retrieving input.
	 */
	@RestMethod(name="GET", path="/example1/{p1}/{p2}/{p3}/*")
	public String example1(
			@Method String method,                  // HTTP method.
			@Path String p1,                        // Path variables.
			@Path int p2,
			@Path UUID p3,
			@Query("q1") int q1,                    // Query parameters.
			@Query("q2") String q2,
			@Query("q3") UUID q3,
			@PathRemainder String remainder,        // Path remainder after pattern match.
			@Header("Accept-Language") String lang, // Headers.
			@Header("Accept") String accept,
			@Header("DNT") int doNotTrack
		) {

		// Send back a simple String response
		String output = String.format(
				"method=%s, p1=%s, p2=%d, p3=%s, remainder=%s, q1=%d, q2=%s, q3=%s, lang=%s, accept=%s, dnt=%d",
				method, p1, p2, p3, remainder, q1, q2, q3, lang, accept, doNotTrack);
		return output;
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
		UUID q3 = query.get("q3", UUID.class);

		// Path remainder after pattern match.
		String remainder = req.getPathMatch().getRemainder();

		// Headers.
		String lang = req.getHeader("Accept-Language");
		String accept = req.getHeader("Accept");
		int doNotTrack = req.getHeaders().get("DNT", int.class);

		// Send back a simple String response
		String output = String.format(
				"method=%s, p1=%s, p2=%d, p3=%s, remainder=%s, q1=%d, q2=%s, q3=%s, lang=%s, accept=%s, dnt=%d",
				method, p1, p2, p3, remainder, q1, q2, q3, lang, accept, doNotTrack);
		res.setOutput(output);
	}

	/** 
	 * Methodology #3 - GET request using special objects.
	 * This approach uses intermediate-level APIs.
	 * The framework recognizes the parameter types and knows how to resolve them.
	 */
	@RestMethod(name="GET", path="/example3/{p1}/{p2}/{p3}/*")
	public String example3(
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
		UUID q3 = query.get("q3", UUID.class);

		// Path remainder after pattern match.
		String remainder = path.getRemainder();

		// Headers.
		int doNotTrack = headers.get("DNT", int.class);

		// Send back a simple String response
		String output = String.format(
				"method=%s, p1=%s, p2=%d, p3=%s, remainder=%s, q1=%d, q2=%s, q3=%s, lang=%s, accept=%s, dnt=%d",
				method, p1, p2, p3, remainder, q1, q2, q3, lang, accept, doNotTrack);
		return output;
	}	
}
