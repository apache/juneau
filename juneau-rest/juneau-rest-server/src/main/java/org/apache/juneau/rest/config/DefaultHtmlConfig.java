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
package org.apache.juneau.rest.config;

import org.apache.juneau.html.annotation.*;

/**
 * Predefined REST configuration that defines common default values for HTML Doc serializers.
 */
@HtmlDocConfig(

	// Default page header contents.
	header={
		"<h1>$RS{title}</h1>",  // Use @Rest(title)
		"<h2>$RS{operationSummary,description}</h2>", // Use either @RestOp(summary) or @Rest(description)
		"$C{REST/header}"  // Extra header HTML defined in external config file.
	},

	// Basic page navigation links.
	navlinks={
		"up: request:/.."
	},

	// Default stylesheet to use for the page.
	// Can be overridden from external config file.
	// Default is DevOps look-and-feel (aka Depression look-and-feel).
	stylesheet="$C{REST/theme,servlet:/htdocs/themes/devops.css}",

	// Default contents to add to the <head> section of the HTML page.
	// Use it to add a favicon link to the page.
	head="$C{REST/head}",

	// No default page footer contents.
	// Can be overridden from external config file.
	footer="$C{REST/footer}",

	// By default, table cell contents should not wrap.
	nowrap="true"
)
public interface DefaultHtmlConfig {}
