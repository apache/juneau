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
package org.apache.juneau.examples.rest.dto;

import org.apache.juneau.jsonschema.annotation.ExternalDocs;
import org.apache.juneau.dto.*;
import org.apache.juneau.examples.rest.petstore.dto.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.widget.*;

/**
 * Sample REST resource for rendering predefined label beans.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@RestResource(
	path="/predefinedLabels",
	title="Predefined Label Beans",
	description="Shows examples of predefined label beans",
	swagger=@ResourceSwagger(
		contact=@Contact(name="Juneau Developer",email="dev@juneau.apache.org"),
		license=@License(name="Apache 2.0",url="http://www.apache.org/licenses/LICENSE-2.0.html"),
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs=@ExternalDocs(description="Apache Juneau",url="http://juneau.apache.org")
	)
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class,
		ThemeMenuItem.class
	},
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS",
		"$W{ContentTypeMenuItem}",
		"$W{ThemeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/dto/$R{servletClassSimple}.java"
	}
)
public class PredefinedLabelsResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@RestMethod
	public ResourceDescriptions get() throws Exception {
		return new ResourceDescriptions()
			.append("beanDescription", "BeanDescription")
			.append("htmlLinks", "HtmlLink")
		;
	}

	@RestMethod
	public BeanDescription getBeanDescription() throws Exception {
		return new BeanDescription(Pet.class);
	}

	@RestMethod
	public LinkString[] getHtmlLinks() throws Exception {
		return new LinkString[] {
			new LinkString("apache", "http://apache.org"),
			new LinkString("juneau", "http://juneau.apache.org")
		};
	}
}
