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

import org.apache.juneau.examples.rest.addressbook.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Sample REST resource showing how to implement a "router" resource page.
 */
@RestResource(
	path="/",
	title="Root resources",
	description="This is an example of a router resource that is used to access other resources.",
	pageLinks="{options:'?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/RootResources.java'}",
	children={
		HelloWorldResource.class,
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
		ShutdownResource.class
	}
)
public class RootResources extends ResourceGroup {
	private static final long serialVersionUID = 1L;
}
