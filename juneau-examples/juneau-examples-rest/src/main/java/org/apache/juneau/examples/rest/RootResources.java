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

import static org.apache.juneau.serializer.Serializer.*;

import org.apache.juneau.examples.rest.addressbook.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.*;

/**
 * Sample REST resource showing how to implement a "router" resource page.
 */
@RestResource(
	path="/",
	title="Root resources",
	description="Example of a router resource page.",
	htmldoc=@HtmlDoc(
		widgets={
			PoweredByApache.class,
			ContentTypeMenuItem.class,
			StyleMenuItem.class
		},
		navlinks={
			"options: ?method=OPTIONS",
			"$W{ContentTypeMenuItem}",
			"$W{StyleMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		},
		aside={
			"<div style='max-width:400px' class='text'>",
			"	<p>This is an example of a 'router' page that serves as a jumping-off point to child resources.</p>",
			"	<p>Resources can be nested arbitrarily deep through router pages.</p>",
			"	<p>Note the <span class='link'>options</span> link provided that lets you see the generated swagger doc for this page.</p>",
			"	<p>Also note the <span class='link'>sources</span> link on these pages to view the source code for the page.</p>",
			"	<p>All content on pages in the UI are serialized POJOs.  In this case, it's a serialized array of beans with 2 properties, 'name' and 'description'.</p>",
			"	<p>Other features (such as this aside) are added through annotations.</p>",
			"</div>"
		},
		footer="$W{PoweredByApache}"
	),
	properties={
		// For testing purposes, we want to use single quotes in all the serializers so it's easier to do simple
		// String comparisons.
		// You can apply any of the Serializer/Parser/BeanContext settings this way.
		@Property(name=SERIALIZER_quoteChar, value="'")
	},
	children={
		HelloWorldResource.class,
		PetStoreResource.class,
		SystemPropertiesResource.class,
		MethodExampleResource.class,
		RequestEchoResource.class,
		TempDirResource.class,
		AddressBookResource.class,
		SampleRemoteableServlet.class,
		PhotosResource.class,
		AtomFeedResource.class,
		JsonSchemaResource.class,
		SqlQueryResource.class,
		TumblrParserResource.class,
		CodeFormatterResource.class,
		UrlEncodedFormResource.class,
		ConfigResource.class,
		LogsResource.class,
		DockerRegistryResource.class,
		PredefinedLabelsResource.class,
		StaticFilesResource.class,
		DebugResource.class,
		ShutdownResource.class
	}
)
public class RootResources extends BasicRestServletJenaGroup {
	// IMPORTANT!  If you don't need RDF support, change the parent class to ResourceGroup.
	// It allows you to remove the Jena prerequisite.

	private static final long serialVersionUID = 1L;
}
