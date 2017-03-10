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
import static org.apache.juneau.html.HtmlDocSerializerContext.*;

import java.util.*;
import java.util.Map;

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Body;

@RestResource(
	path="/systemProperties",
	title="System properties resource",
	description="REST interface for performing CRUD operations on system properties.",
	properties={
		@Property(name=SERIALIZER_quoteChar, value="'"),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',form:'formPage',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/SystemPropertiesResource.java'}"),
	},
	stylesheet="styles/devops.css",
	encoders=GzipEncoder.class,
	contact="{name:'John Smith',email:'john@smith.com'}",
	license="{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}",
	version="2.0",
	termsOfService="You're on your own.",
	tags="[{name:'Java',description:'Java utility',externalDocs:{description:'Home page',url:'http://juneau.apache.org'}}]",
	externalDocs="{description:'Home page',url:'http://juneau.apache.org'}"
)
public class SystemPropertiesResource extends Resource {
	private static final long serialVersionUID = 1L;

	@RestMethod(
		name="GET", path="/",
		summary="Show all system properties",
		description="Returns all system properties defined in the JVM.",
		parameters={
			@Parameter(in="query", name="sort", description="Sort results alphabetically.", _default="false")
		},
		responses={
			@Response(value=200, description="Returns a map of key/value pairs.")
		}
	)
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map getSystemProperties(@Query("sort") boolean sort) throws Throwable {
		if (sort)
			return new TreeMap(System.getProperties());
		return System.getProperties();
	}

	@RestMethod(
		name="GET", path="/{propertyName}",
		summary="Get system property",
		description="Returns the value of the specified system property.",
		parameters={
			@Parameter(in="path", name="propertyName", description="The system property name.")
		},
		responses={
			@Response(value=200, description="The system property value, or null if not found.")
		}
	)
	public String getSystemProperty(@Path String propertyName) throws Throwable {
		return System.getProperty(propertyName);
	}
	
	@RestMethod(
		name="PUT", path="/{propertyName}", 
		summary="Replace system property",
		description="Sets a new value for the specified system property.",
		guards=AdminGuard.class,
		parameters={
			@Parameter(in="path", name="propertyName", description="The system property name."),
			@Parameter(in="body", description="The new system property value."),
		},
		responses={
			@Response(value=302, 
				headers={
					@Parameter(name="Location", description="The root URL of this resource.")
				}
			),	
			@Response(value=403, description="User is not an admin.") 
		}
	)
	public Redirect setSystemProperty(@Path String propertyName, @Body String value) {
		System.setProperty(propertyName, value);
		return new Redirect();
	}

	@RestMethod(
		name="POST", path="/", 
		summary="Add an entire set of system properties",
		description="Takes in a map of key/value pairs and creates a set of new system properties.",
		guards=AdminGuard.class,
		parameters={
			@Parameter(in="path", name="propertyName", description="The system property key."),
			@Parameter(in="body", description="The new system property values.", schema="{example:{key1:'val1',key2:123}}"),
		},
		responses={
			@Response(value=302, 
				headers={
					@Parameter(name="Location", description="The root URL of this resource.")
				}
			),	
			@Response(value=403, description="Unauthorized:  User is not an admin.") 
		}
	)
	public Redirect setSystemProperties(@Body java.util.Properties newProperties) {
		System.setProperties(newProperties);
		return new Redirect();
	}

	@RestMethod(
		name="DELETE", path="/{propertyName}", 
		summary="Delete system property",
		description="Deletes the specified system property.",
		guards=AdminGuard.class,
		parameters={
			@Parameter(in="path", name="propertyName", description="The system property name."),
		},
		responses={
			@Response(value=302, 
				headers={
					@Parameter(name="Location", description="The root URL of this resource.")
				}
			),	
			@Response(value=403, description="Unauthorized:  User is not an admin") 
		}
	)
	public Redirect deleteSystemProperty(@Path String propertyName) {
		System.clearProperty(propertyName);
		return new Redirect();
	}

	@RestMethod(
		name="OPTIONS", path="/*",
		summary="Show resource options",
		description="Show resource options as a Swagger doc"
	)
	public Swagger getOptions(RestRequest req) {
		return req.getSwagger();
	}
	
	@RestMethod(
		name="GET", path="/formPage",
		summary="Form entry page",
		description="A form post page for setting a single system property value",
		guards=AdminGuard.class
	)
	public Form getFormPage() {
		return form().method("POST").action("formPagePost").children(
			h4("Set system property"),
			"Name: ", input("text").name("name"), br(),
			"Value: ", input("text").name("value"), br(), br(),
			button("submit","Click me!").style("float:right")
		);
	}

	@RestMethod(
		name="POST", path="/formPagePost", 
		description="Accepts a simple form post of a system property name/value pair.",
		guards=AdminGuard.class
	)
	public Redirect formPagePost(@FormData("name") String name, @FormData("value") String value) {
		System.setProperty(name, value);
		return new Redirect();
	}
}