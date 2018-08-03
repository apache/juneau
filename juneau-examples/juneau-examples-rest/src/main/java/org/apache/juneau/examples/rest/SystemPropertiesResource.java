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
import static org.apache.juneau.http.HttpMethodName.*;

import java.util.*;
import java.util.Map;

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.widget.*;

@RestResource(
	path="/systemProperties",

	// Title and description that show up on HTML rendition page.
	// Also used in Swagger doc.
	title="System properties resource",
	description="REST interface for performing CRUD operations on system properties.",

	htmldoc=@HtmlDoc(

		// Widget used for content-type and styles pull-down menus.
		widgets={
			ContentTypeMenuItem.class,
			ThemeMenuItem.class
		},

		// Links on the HTML rendition page.
		// "request:/..." URIs are relative to the request URI.
		// "servlet:/..." URIs are relative to the servlet URI.
		// "$C{...}" variables are pulled from the config file.
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"form: servlet:/formPage",
			"$W{ContentTypeMenuItem}",
			"$W{ThemeMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		},

		// Custom page text in aside section.
		aside={
			"<div style='max-width:800px' class='text'>",
			"	<p>Shows standard GET/PUT/POST/DELETE operations and use of Swagger annotations.</p>",
			"</div>"
		},

		// Custom CSS styles applied to HTML view.
		style={
			"aside {display:table-caption} ",
			"aside p {margin: 0px 20px;}"
		},

		// Allow text wrapping in cells.
		nowrap="false"
	),

	// Properties that get applied to all serializers and parsers.
	properties={
		// Use single quotes.
	},

	// Support GZIP encoding on Accept-Encoding header.
	encoders=GzipEncoder.class,

	swagger=@ResourceSwagger(
		contact=@Contact(name="Juneau Developer",email="dev@juneau.apache.org"),
		license=@License(name="Apache 2.0",url="http://www.apache.org/licenses/LICENSE-2.0.html"),
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs=@ExternalDocs(description="Apache Juneau",url="http://juneau.apache.org")
	)
)
public class SystemPropertiesResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@RestMethod(
		summary="Show all system properties",
		description="Returns all system properties defined in the JVM.",
		swagger=@MethodSwagger(
			responses={
				"200: {description:'Returns a map of key/value pairs.', x-example:{key1:'val1',key2:'val2'}}"
			}
		)
	)
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map get(
			@Query(name="sort", description="Sort results alphabetically", _default="false", example="true") boolean sort
		) throws NotAcceptable {

		if (sort)
			return new TreeMap(System.getProperties());
		return System.getProperties();
	}

	@RestMethod(
		name=GET, path="/{propertyName}",
		summary="Get system property",
		description="Returns the value of the specified system property.",
		swagger=@MethodSwagger(
			responses={
				"200: {description:'The system property value, or null if not found'}"
			}
		)
	)
	public String getSystemProperty(
			@Path(name="propertyName", description="The system property name.", example="PATH") String propertyName
		) throws NotAcceptable {

		return System.getProperty(propertyName);
	}

	@RestMethod(
		name=PUT, path="/{propertyName}",
		summary="Replace system property",
		description="Sets a new value for the specified system property.",
		guards=AdminGuard.class
	)
	public SeeOtherRoot setSystemProperty(
			@Path(name="propertyName", description="The system property name") String propertyName,
			@Body(description="The new system property value") String value
		) throws UserNotAdminException, NotAcceptable, UnsupportedMediaType {

		System.setProperty(propertyName, value);
		return SeeOtherRoot.INSTANCE;
	}

	@RestMethod(
		summary="Add an entire set of system properties",
		description="Takes in a map of key/value pairs and creates a set of new system properties.",
		guards=AdminGuard.class
	)
	public SeeOtherRoot post(
			@Body(description="The new system property values", example="{key1:'val1',key2:123}") java.util.Properties newProperties
		) throws UserNotAdminException, NotAcceptable, UnsupportedMediaType {

		System.setProperties(newProperties);
		return SeeOtherRoot.INSTANCE;
	}

	@RestMethod(
		name=DELETE, path="/{propertyName}",
		summary="Delete system property",
		description="Deletes the specified system property.",
		guards=AdminGuard.class
	)
	public SeeOtherRoot deleteSystemProperty(
			@Path(name="propertyName", description="The system property name", example="PATH") String propertyName
		) throws UserNotAdminException, NotAcceptable {

		System.clearProperty(propertyName);
		return SeeOtherRoot.INSTANCE;
	}

	@RestMethod(
		summary="Form entry page",
		description="A form post page for setting a single system property value",
		guards=AdminGuard.class,
		htmldoc=@HtmlDoc(
			aside={
				"<div class='text'>",
				"	<p>Shows how HTML5 beans can be used to quickly create arbitrary HTML.</p>",
				"</div>"
			}
		)
	)
	public Form getFormPage() throws NotAcceptable {
		return form().method(POST).action("servlet:/formPagePost").children(
			table(
				tr(
					th("Set system property").colspan(2)
				),
				tr(
					td("Name: "), td(input("text").name("name"))
				),
				tr(
					td("Value: "), td(input("text").name("value"))
				)
			),
			button("submit","Click me!").style("float:right")
		);
	}

	@RestMethod(
		summary="Form page post",
		description="Accepts a simple form post of a system property name/value pair.",
		guards=AdminGuard.class
	)
	public SeeOtherRoot postFormPagePost(
			@FormData("name") String name,
			@FormData("value") String value
		) throws UserNotAdminException, NotAcceptable, UnsupportedMediaType {

		System.setProperty(name, value);
		return SeeOtherRoot.INSTANCE;
	}


	//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// Beans
	//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Response(description="User is not an administrator.")
	public static class UserNotAdminException extends Forbidden {
		private static final long serialVersionUID = 1L;

		public UserNotAdminException() {
			super("User is not an administrator");
		}
	}

}