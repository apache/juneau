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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.dto.swagger.ui.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xmlschema.XmlSchemaDocSerializer;

/**
 * Basic configuration for a REST resource.
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
		XmlSchemaDocSerializer.class,
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
		JsonParser.Simple.class,
		XmlParser.class,
		HtmlParser.class,
		UonParser.class,
		UrlEncodingParser.class,
		OpenApiParser.class,
		MsgPackParser.class,
		PlainTextParser.class
	},

	// Optional external configuration file.
	config="$S{juneau.configFile,SYSTEM_DEFAULT}",

	// These are static files that are served up by the servlet under the specified sub-paths.
	// For example, "/servletPath/htdocs/javadoc.css" resolves to the file "[servlet-package]/htdocs/javadoc.css"
	// By default, we define static files through the external configuration file.
	staticFiles="$C{REST/staticFiles,htdocs:/htdocs,htdocs:htdocs}",

	logging=@Logging(
		level="INFO",
		useStackTraceHashing="true",
		rules={
			@LoggingRule(codes="500-", level="WARNING")
		}
	)
)
@SerializerConfig(
	// Enable automatic resolution of URI objects to root-relative values.
	uriResolution="ROOT_RELATIVE"
)
@HtmlDocConfig(

	// Default page header contents.
	header={
		"<h1>$R{resourceTitle}</h1>",  // Use @Rest(title)
		"<h2>$R{methodSummary,resourceDescription}</h2>", // Use either @RestMethod(summary) or @Rest(description)
		"$C{REST/header}"  // Extra header HTML defined in external config file.
	},

	// Basic page navigation links.
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS",
		"stats: servlet:/stats"
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
@JsonSchemaConfig(
	// Add descriptions to the following types when not specified:
	addDescriptionsTo="bean,collection,array,map,enum",
	// Add x-example to the following types:
	addExamplesTo="bean,collection,array,map",
	// Don't generate schema information on the Swagger bean itself or HTML beans.
	ignoreTypes="Swagger,org.apache.juneau.dto.html5.*",
	// Use $ref references for bean definitions to reduce duplication in Swagger.
	useBeanDefs="true"
)
@BeanConfig(
	// When parsing generated beans, ignore unknown properties that may only exist as getters and not setters.
	ignoreUnknownBeanProperties="true",
	// POJO swaps to apply to all serializers/parsers on this method.
	pojoSwaps={
		// Use the SwaggerUI swap when rendering Swagger beans.
		// This is a per-media-type swap that only applies to text/html requests.
		SwaggerUI.class
	}
)
@SuppressWarnings("deprecation")
public interface BasicRestConfig {

	/**
	 * [OPTIONS /*] - Show resource options.
	 *
	 * @param req The HTTP request.
	 * @return A bean containing the contents for the OPTIONS page.
	 */
	@RestMethod(name=OPTIONS, path="/*",
		summary="Swagger documentation",
		description="Swagger documentation for this resource."
	)
	@HtmlDocConfig(
		// Should override config annotations defined on class.
		rank=10,
		// Override the nav links for the swagger page.
		navlinks={
			"back: servlet:/",
			"json: servlet:/?method=OPTIONS&Accept=text/json&plainText=true"
		},
		// Never show aside contents of page inherited from class.
		aside="NONE"
	)
	public Swagger getOptions(RestRequest req);

	/**
	 * [* /error] - Error occurred.
	 *
	 * <p>
	 * Servlet chains will often automatically redirect to <js>"/error"</js> when any sort of error condition occurs
	 * (such as failed authentication) and will set appropriate response parameters (such as an <c>WWW-Authenticate</c>
	 * response header).
	 *
	 * <p>
	 * These responses should be left as-is without any additional processing.
	 */
	@RestMethod(name=ANY, path="/error",
		summary="Error occurred",
		description="An error occurred during handling of the request."
	)
	public void error();

	/**
	 * [GET /stats] - Timing statistics.
	 *
	 * <p>
	 * Timing statistics for method invocations on this resource.
	 *
	 * @param req The HTTP request.
	 * @return A collection of timing statistics for each annotated method on this resource.
	 */
	@RestMethod(name=GET, path="/stats",
		summary="Timing statistics",
		description="Timing statistics for method invocations on this resource."
	)
	@HtmlDocConfig(
		// Should override config annotations defined on class.
		rank=10,
		// Override the nav links for the swagger page.
		navlinks={
			"back: servlet:/",
			"json: servlet:/stats?Accept=text/json&plainText=true"
		},
		// Never show aside contents of page inherited from class.
		aside="NONE"
	)
	public RestContextStats getStats(RestRequest req);
}
