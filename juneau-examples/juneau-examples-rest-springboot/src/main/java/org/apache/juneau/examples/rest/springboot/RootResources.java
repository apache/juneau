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
package org.apache.juneau.examples.rest.springboot;

import org.apache.juneau.examples.rest.*;
import org.apache.juneau.examples.rest.dto.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.springboot.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.annotation.*;

/**
 * Sample REST resource showing how to implement a "router" resource page.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#jrss.Overview">juneau-rest-server-springboot &gt; Overview</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	title="Root resources",
	description="Example of a router resource page.",
	children={
		HelloWorldResource.class,
		DtoExamples.class,
		UtilityBeansResource.class,
		HtmlBeansResource.class,
		ConfigResource.class,
		ShutdownResource.class
	}
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class
	},
	navlinks={
		"api: servlet:/api",
		"stats: servlet:/stats",
		"$W{ContentTypeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
	},
	aside={
		"<div class='text'>",
		"	<p>This is an example of a 'router' page that serves as a jumping-off point to child resources.</p>",
		"	<p>Resources can be nested arbitrarily deep through router pages.</p>",
		"	<p>Note the <span class='link'>API</span> link provided that lets you see the generated swagger doc for this page.</p>",
		"	<p>Also note the <span class='link'>STATS</span> link that provides basic usage statistics.</p>",
		"	<p>Also note the <span class='link'>SOURCE</span> link on these pages to view the source code for the page.</p>",
		"	<p>All content on pages in the UI are serialized POJOs.  In this case, it's a serialized array of beans with 2 properties, 'name' and 'description'.</p>",
		"	<p>Other features (such as this aside) are added through annotations.</p>",
		"</div>"
	},
	asideFloat="RIGHT"
)
@SerializerConfig(
	// For testing purposes, we want to use single quotes in all the serializers so it's easier to do simple
	// String comparisons.
	// You can apply any of the Serializer/Parser/BeanContext settings this way.
	quoteChar="'"
)
public class RootResources extends BasicSpringRestServletGroup {
	private static final long serialVersionUID = 1L;
}
