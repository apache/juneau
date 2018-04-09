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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.examples.addressbook.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.widget.*;

/**
 * Sample REST resource for rendering predefined label beans.
 */
@RestResource(
	path="/predefinedLabels",
	title="Predefined Label Beans",
	description="Shows examples of predefined label beans",
	htmldoc=@HtmlDoc(
		widgets={
			ContentTypeMenuItem.class,
			StyleMenuItem.class
		},
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"$W{ContentTypeMenuItem}",
			"$W{StyleMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		}
	)
)
public class PredefinedLabelsResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@RestMethod(name=GET, path="/")
	public ResourceDescription[] getChildMethods() throws Exception {
		return new ResourceDescription[] {
			new ResourceDescription("beanDescription", "BeanDescription"),
			new ResourceDescription("htmlLinks", "HtmlLink")
		};
	}
	
	@RestMethod(name=GET, path="/beanDescription")
	public BeanDescription getBeanDescription() throws Exception {
		return new BeanDescription(Person.class);
	}	
	
	@RestMethod(name=GET, path="/htmlLinks")
	public ALink[] htmlLinks() throws Exception {
		return new ALink[] {
			new ALink("apache", "http://apache.org"),
			new ALink("juneau", "http://juneau.apache.org")
		};
	}
	
	@HtmlLink(nameProperty="n", hrefProperty="l")
	public static class ALink {
		public String n, l;
		public ALink(String n, String l) {
			this.n = n;
			this.l = l;
		}
	}
}
