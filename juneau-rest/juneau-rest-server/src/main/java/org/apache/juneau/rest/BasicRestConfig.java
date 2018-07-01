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

import static org.apache.juneau.serializer.Serializer.*;

import org.apache.juneau.html.*;
import org.apache.juneau.htmlschema.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xmlschema.*;

/**
 * Basic configuration for a REST resource.
 *
 * <p>
 * Classes that don't extend from {@link BasicRestServlet} can implement this interface to
 * be configured with the same serializers/parsers/etc... as {@link BasicRestServlet}.
 */
@RestResource(
	serializers={
		HtmlDocSerializer.class, // HTML must be listed first because Internet Explore does not include text/html in their Accept header.
		HtmlStrippedDocSerializer.class,
		HtmlSchemaDocSerializer.class,
		JsonSerializer.class,
		JsonSerializer.Simple.class,
		JsonSchemaSerializer.class,
		XmlDocSerializer.class,
		XmlSchemaDocSerializer.class,
		UonSerializer.class,
		UrlEncodingSerializer.class,
		MsgPackSerializer.class,
		SoapXmlSerializer.class,
		PlainTextSerializer.class
	},
	parsers={
		JsonParser.class,
		JsonParser.Simple.class,
		XmlParser.class,
		HtmlParser.class,
		UonParser.class,
		UrlEncodingParser.class,
		MsgPackParser.class,
		PlainTextParser.class
	},
	properties={
		// URI-resolution is disabled by default.  Need to enable it.
		@Property(name=SERIALIZER_uriResolution, value="ROOT_RELATIVE")
	},
	htmldoc=@HtmlDoc(
		header={
			"<h1>$R{resourceTitle}</h1>",
			"<h2>$R{methodSummary,resourceDescription}</h2>",
			"$C{REST/header}"
		},
		navlinks={
			"up: request:/.."
		},
		stylesheet="$C{REST/theme,servlet:/htdocs/themes/devops.css}",
		head={
			"<link rel='icon' href='$U{$C{REST/favicon}}'/>"
		},
		footer="$C{REST/footer}",
		nowrap="true"
	),

	// Optional external configuration file.
	config="$S{juneau.configFile}",

	// These are static files that are served up by the servlet under the specified sub-paths.
	// For example, "/servletPath/htdocs/javadoc.css" resolves to the file "[servlet-package]/htdocs/javadoc.css"
	staticFiles={"$C{REST/staticFiles}"}
)
public interface BasicRestConfig {}
