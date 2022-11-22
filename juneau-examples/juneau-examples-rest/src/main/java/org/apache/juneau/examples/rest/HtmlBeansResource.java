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

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.widget.*;

/**
 * Sample resource that allows images to be uploaded and retrieved.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Marshalling">REST Marshalling</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlBeans">HtmlBeans</a>
 * </ul>
 */
@Rest(
	path="/htmlbeans",
	title="HTML bean examples",
	description="Examples of serialized HTML beans."
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class
	},
	navlinks={
		"up: request:/..",
		"api: servlet:/api",
		"stats: servlet:/stats",
		"$W{ContentTypeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/HtmlBeansResource.java"
	},
	aside={
		"<div class='text'>",
		"	<p>Examples of serialized HTML beans.</p>",
		"</div>"
	},
	asideFloat="RIGHT"
)
public class HtmlBeansResource extends BasicRestObject {

	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	/**
	 * [HTTP GET /htmlbeans]
	 * @return Descriptive links to the child endpoints.
	 */
	@RestGet("/")
	public ResourceDescriptions getChildDescriptions() {
		return ResourceDescriptions
			.create()
			.append("table", "Example of a serialized table")
			.append("div", "Example of a serialized div tag")
			.append("form", "Example of a serialized form");
	}

	/**
	 * [HTTP GET /htmlbeans/table]
	 * @return An example table.
	 */
	@RestGet("/table")
	@HtmlDocConfig(
		aside={
			"<div class='text'>",
			"	<p>Example of serialized table.</p>",
			"</div>"
		}
	)
	public Table aTable() {
		return table(
			tr(
				th("c1"),
				th("c2")
			),
			tr(
				td("v1"),
				td("v2")
			)
		);
	}

	/**
	 * [HTTP GET /htmlbeans/div]
	 * @return An example div tag.
	 */
	@RestGet("/div")
	@HtmlDocConfig(
		aside={
			"<div class='text'>",
			"	<p>Example of serialized div tag.</p>",
			"</div>"
		}
	)
	public HtmlElement aDiv() {
		return div()
			.children(
				p("Juneau supports ", b(i("mixed")), " content!")
			)
			.onmouseover("alert(\"boo!\");");
	}

	/**
	 * [HTTP GET /htmlbeans/form]
	 * @return An example form tag.
	 */
	@RestGet("/form")
	@HtmlDocConfig(
		aside={
			"<div class='text'>",
			"	<p>Example of serialized HTML form.</p>",
			"</div>"
		}
	)
	public Form aForm() {
		return form().action("/submit").method("POST")
			.children(
				"Position (1-10000): ", input("number").name("pos").value(1), br(),
				"Limit (1-10000): ", input("number").name("limit").value(100), br(),
				button("submit", "Submit"),
				button("reset", "Reset")
			);
	}
}