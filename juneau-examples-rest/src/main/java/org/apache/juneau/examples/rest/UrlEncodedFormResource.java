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

import static org.apache.juneau.dto.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Body;
import org.apache.juneau.transforms.*;

/**
 * Sample REST resource for loading URL-Encoded form posts into POJOs.
 */
@RestResource(
	path="/urlEncodedForm",
	messages="nls/UrlEncodedFormResource",
	title="Tumblr parser service",
	description="Specify a URL to a Tumblr blog and parse the results.",
	pageLinks="{up:'request:/..',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/UrlEncodedFormResource.java'}"
)
public class UrlEncodedFormResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** GET request handler */
	@RestMethod(name="GET", path="/")
	public Div doGet(RestRequest req) {
		return div(
			script("text/javascript",
				"\n	// Load results from IFrame into this document."
				+"\n	function loadResults(buff) {"
				+"\n		var doc = buff.contentDocument || buff.contentWindow.document;"
				+"\n		var buffBody = doc.getElementById('data');"
				+"\n		document.getElementById('results').innerHTML = buffBody.innerHTML;"
				+"\n	}"
			),
			form().id("form").action(req.getServletURI()).method("POST").target("buff").children(
				table(
					tr(
						th(req.getMessage("aString")),
						td(input().name("aString").type("text"))
					),
					tr(
						th(req.getMessage("aNumber")),
						td(input().name("aNumber").type("number"))
					),
					tr(
						th(req.getMessage("aDate")),
						td(input().name("aDate").type("datetime"), " (ISO8601, e.g. ", code("2001-07-04T15:30:45Z"), " )")
					),
					tr(
						td().colspan(2).style("text-align:right").children(
							button("submit", req.getMessage("submit"))
						)
					)
				)
			),
			br(),
			div().id("results"),
			iframe().name("buff").style("display:none").onload("parent.loadResults(this)")
		);
	}

	/** POST request handler */
	@RestMethod(name="POST", path="/")
	public Object doPost(@Body FormInputBean input) throws Exception {
		// Just mirror back the request
		return input;
	}

	public static class FormInputBean {
		public String aString;
		public int aNumber;
		@BeanProperty(swap=CalendarSwap.ISO8601DT.class)
		public Calendar aDate;
	}
}
