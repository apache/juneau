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

import org.apache.juneau.annotation.*;
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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.UtilityBeans">Utility Beans</a>
 * </ul>
 */
@Rest(
	path="/utilitybeans",
	title="Utility beans examples",
	description="Examples of utility bean usage."
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
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/UtilityBeansResource.java"
	},
	aside={
		"<div class='text'>",
		"	<p>Examples of serialized beans in the org.apache.juneau.rest.utilitybeans package.</p>",
		"</div>"
	},
	asideFloat="RIGHT"
)
@SuppressWarnings("javadoc")
public class UtilityBeansResource extends BasicRestObject {

	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	/**
	 * [HTTP GET /utilitybeans]
	 * @return Descriptive links to the child endpoints.
	 */
	@RestGet("/")
	public ResourceDescriptions getChildDescriptions() {
		return ResourceDescriptions
			.create()
			.append("BeanDescription", "Example of BeanDescription bean")
			.append("Hyperlink", "Example of Hyperlink bean")
			.append("SeeOtherRoot", "Example of SeeOtherRoot bean");
	}

	/**
	 * [HTTP GET /utilitybeans/BeanDescription]
	 * @return Example of serialized org.apache.juneau.rest.utilitybeans.ResourceDescriptions bean.
	 */
	@RestGet("/BeanDescription")
	@HtmlDocConfig(
		aside={
			"<div class='text'>",
			"	<p>Example of serialized ResourceDescriptions bean.</p>",
			"</div>"
		}
	)
	public BeanDescription aBeanDescription() {
		return BeanDescription.of(Address.class);
	}

	@Bean(p="street,city,state,zip,isCurrent")
	public static class Address {
		public String street;
		public String city;
		public String state;
		public int zip;
		public boolean isCurrent;

		public Address() {}
	}

	/**
	 * [HTTP GET /utilitybeans/Hyperlink]
	 * @return Example of serialized org.apache.juneau.rest.utilitybeans.Hyperlink bean.
	 */
	@RestGet("/Hyperlink")
	@HtmlDocConfig(
		aside={
			"<div class='text'>",
			"	<p>Example of serialized Hyperlink bean.</p>",
			"</div>"
		}
	)
	public Hyperlink aHyperlink() {
		return Hyperlink.create("/utilitybeans", "Back to /utilitybeans");
	}

	/**
	 * [HTTP GET /utilitybeans/SeeOtherRoot]
	 * @return Example of serialized SeeOtherRoot bean.
	 * This just redirects back to the servlet root.
	 */
	@RestGet("/SeeOtherRoot")
	@HtmlDocConfig(
		aside={
			"<div class='text'>",
			"	<p>Example of serialized org.apache.juneau.rest.utilitybeans.SeeOtherRoot bean.</p>",
			"</div>"
		}
	)
	public SeeOtherRoot aSeeOtherRoot() {
		return SeeOtherRoot.INSTANCE;
	}
}