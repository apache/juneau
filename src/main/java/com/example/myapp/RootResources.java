/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.myapp;

import org.apache.juneau.marshall.html.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widget.*;

/**
 * Root "router" page that links to child resources. Registered as the WAR's servlet via {@code web.xml}.
 *
 * @serial exclude
 */
@Rest(
	path="/*",
	title="My Juneau Microservice",
	description="Root router page.",
	children={
		HelloWorldResource.class
	}
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class,
		ThemeMenuItem.class
	},
	navlinks={
		"api: servlet:/api",
		"stats: servlet:/stats",
		"$W{ContentTypeMenuItem}",
		"$W{ThemeMenuItem}"
	},
	aside={
		"<div class='text'>",
		"\t<p>This is a router page that jumps off to child resources. The <span class='link'>options</span> link shows the generated Swagger doc.</p>",
		"</div>"
	},
	asideFloat="RIGHT"
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for microservice hierarchy
})
public class RootResources extends BasicRestServletGroup {
	private static final long serialVersionUID = 1L;
}
