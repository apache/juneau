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

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Basic configuration for a REST resource that supports all languages.
 *
 * <p>
 * Classes that don't extend from {@link BasicRestServlet} can implement this interface to
 * be configured with the same serializers/parsers/etc... as {@link BasicRestServlet}.
 */
@Rest(

	// Default serializers for all Java methods in the class.
	serializers={
		HtmlDocSerializer.class, // HTML must be listed first because Internet Explore does not include text/html in their Accept header.
		HtmlStrippedDocSerializer.class,
		HtmlSchemaDocSerializer.class,
		JsonSerializer.class,
		SimpleJsonSerializer.class,
		JsonSchemaSerializer.class,
		XmlDocSerializer.class,
		UonSerializer.class,
		UrlEncodingSerializer.class,
		OpenApiSerializer.class,
		MsgPackSerializer.class,
		SoapXmlSerializer.class,
		PlainTextSerializer.class
	},

	// Default parsers for all Java methods in the class.
	parsers={
		JsonParser.class,
		SimpleJsonParser.class,
		XmlParser.class,
		HtmlParser.class,
		UonParser.class,
		UrlEncodingParser.class,
		OpenApiParser.class,
		MsgPackParser.class,
		PlainTextParser.class
	},

	// Optional external configuration file.
	config="$S{juneau.configFile,SYSTEM_DEFAULT}"
)
@SerializerConfig(
	// Enable automatic resolution of URI objects to root-relative values.
	uriResolution="ROOT_RELATIVE"
)
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
@BeanConfig(
	// When parsing generated beans, ignore unknown properties that may only exist as getters and not setters.
	ignoreUnknownBeanProperties="true"
)
public interface BasicUniversalRest {}
