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
package org.apache.juneau.microservice.jetty.template;

import org.apache.juneau.html.annotation.HtmlDocConfig;
import org.apache.juneau.microservice.jetty.resources.DebugResource;
import org.apache.juneau.microservice.resources.ConfigResource;
import org.apache.juneau.microservice.resources.LogsResource;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.ContentTypeMenuItem;
import org.apache.juneau.rest.widget.ThemeMenuItem;

/**
 * Root microservice page.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc my-jetty-microservice}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Rest(
	path="/*",
	title="My Microservice",
	description="Top-level resources page",
	children={
		HelloWorldResource.class,
		ConfigResource.class,
		LogsResource.class,
		DebugResource.class
	}
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class,
		ThemeMenuItem.class
	},
	navlinks={
		"api: servlet:/api",
		"stats: servlet:/stats"
	}
)
public class RootResources extends BasicRestServletGroup {
	private static final long serialVersionUID = 1L;
}
