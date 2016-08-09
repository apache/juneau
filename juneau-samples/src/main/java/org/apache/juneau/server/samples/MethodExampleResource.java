/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server.samples;

import static org.apache.juneau.html.HtmlDocSerializerContext.*;

import java.util.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;

/**
 * Sample REST resource that shows how to define REST methods and OPTIONS pages
 */
@RestResource(
	path="/methodExample",
	messages="nls/MethodExampleResource",
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(org.apache.juneau.server.samples.MethodExampleResource)'}")
	}
)
public class MethodExampleResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** Example GET request that redirects to our example method */
	@RestMethod(name="GET", path="/")
	public Redirect doGetExample() throws Exception {
		return new Redirect("example1/xxx/123/{0}/xRemainder?p1=123&p2=yyy", UUID.randomUUID());
	}

	/** Example GET request using annotated attributes */
	@RestMethod(name="GET", path="/example1/{a1}/{a2}/{a3}/*", rc={200})
	public String doGetExample1(
			@Method String method,
			@Attr String a1,
			@Attr int a2,
			@Attr UUID a3,
			@Param("p1") int p1,
			@Param("p2") String p2,
			@Param("p3") UUID p3,
			@PathRemainder String remainder,
			@Header("Accept-Language") String lang,
			@Header("Accept") String accept,
			@Header("DNT") int doNotTrack
		) {
		String output = String.format(
				"method=%s, a1=%s, a2=%d, a3=%s, remainder=%s, p1=%d, p2=%s, p3=%s, lang=%s, accept=%s, dnt=%d",
				method, a1, a2, a3, remainder, p1, p2, p3, lang, accept, doNotTrack);
		return output;
	}

	/** Example GET request using methods on RestRequest and RestResponse */
	@RestMethod(name="GET", path="/example2/{a1}/{a2}/{a3}/*", rc={200})
	public void doGetExample2(RestRequest req, RestResponse res) throws Exception {
		String method = req.getMethod();

		// Attributes (from URL pattern variables)
		String a1 = req.getAttribute("a1", String.class);
		int a2 = req.getAttribute("a2", int.class);
		UUID a3 = req.getAttribute("a3", UUID.class);

		// Optional GET parameters
		int p1 = req.getParameter("p1", int.class, 0);
		String p2 = req.getParameter("p2", String.class);
		UUID p3 = req.getParameter("p3", UUID.class);

		// URL pattern post-match
		String remainder = req.getPathRemainder();

		// Headers
		String lang = req.getHeader("Accept-Language");
		int doNotTrack = req.getHeader("DNT", int.class);

		// Send back a simple String response
		String output = String.format(
				"method=%s, a1=%s, a2=%d, a3=%s, remainder=%s, p1=%d, p2=%s, p3=%s, lang=%s, dnt=%d",
				method, a1, a2, a3, remainder, p1, p2, p3, lang, doNotTrack);
		res.setOutput(output);
	}
}
