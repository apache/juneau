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

import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.annotation.*;

/**
 * Sample REST resource showing how to implement a nested "router" resource page.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	path="/dto",
	title="DTO examples",
	description="Example serialization of predefined Data Transfer Objects.",
	children={
		AtomFeedResource.class,
		JsonSchemaResource.class
	}
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class
	},
	navlinks={
		"up: request:/..",
		"api: servlet:/api",
		"$W{ContentTypeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/dto/DtoExamples.java"
	},
	aside={
		"<div style='max-width:400px' class='text'>",
		"	<p>This is an example of a nested 'router' page that serves as a jumping-off point to other child resources.</p>",
		"</div>"
	}
)
@SerializerConfig(
	// For testing purposes, we want to use single quotes in all the serializers so it's easier to do simple
	// String comparisons.
	// You can apply any of the Serializer/Parser/BeanContext settings this way.
	quoteChar="'"
)
public class DtoExamples extends BasicRestServletGroup {
	private static final long serialVersionUID = 1L;
}
