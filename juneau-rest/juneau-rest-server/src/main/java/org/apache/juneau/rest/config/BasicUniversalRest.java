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
 * Basic configuration for a REST resource that supports all languages and provides common default configuration values.
 *
 * <p>
 * 	Default settings defined:
 * </p>
 * <ul class='spaced-list'>
 * 	<li>{@link Rest @Rest}:
 * 		<ul>
 * 			<li><c>{@link Rest#serializers() serializers}=</c>
 * 				<ul class='javatreec'>
 * 					<li class='jc'>{@link HtmlDocSerializer}
 * 					<li class='jc'>{@link HtmlStrippedDocSerializer}
 * 					<li class='jc'>{@link HtmlSchemaDocSerializer}
 * 					<li class='jc'>{@link JsonSerializer}
 * 					<li class='jc'>{@link SimpleJsonSerializer}
 * 					<li class='jc'>{@link JsonSchemaSerializer}
 * 					<li class='jc'>{@link XmlDocSerializer}
 * 					<li class='jc'>{@link UonSerializer}
 * 					<li class='jc'>{@link UrlEncodingSerializer}
 * 					<li class='jc'>{@link OpenApiSerializer}
 * 					<li class='jc'>{@link MsgPackSerializer}
 * 					<li class='jc'>{@link SoapXmlSerializer}
 * 					<li class='jc'>{@link PlainTextSerializer}
 * 				</ul>
 * 			</li>
 * 			<li><c>{@link Rest#parsers() parsers}=</c>
 * 				<ul class='javatreec'>
 * 					<li class='jc'>{@link JsonParser}
 * 					<li class='jc'>{@link SimpleJsonParser}
 * 					<li class='jc'>{@link XmlParser}
 * 					<li class='jc'>{@link HtmlParser}
 * 					<li class='jc'>{@link UonParser}
 * 					<li class='jc'>{@link UrlEncodingParser}
 * 					<li class='jc'>{@link OpenApiParser}
 * 					<li class='jc'>{@link MsgPackParser}
 * 					<li class='jc'>{@link PlainTextParser}
 * 				</ul>
 * 			</li>
 * 			<li><c>{@link Rest#config() config}=<js>"$S{juneau.configFile,SYSTEM_DEFAULT}</js>"</c></li>
 *		</ul>
 *	</li>
 * 	<li>{@link BeanConfig @BeanConfig}:
 * 		<ul>
 * 			<li><c>{@link BeanConfig#ignoreUnknownBeanProperties() ignoreUnknownBeanProperties}=<js>"true"</js></c></li>
 * 		</ul>
 * 	</li>
 * 	<li>{@link SerializerConfig @SerializerConfig}:
 * 		<ul>
 * 			<li><c>{@link SerializerConfig#uriResolution() uriResolution}=<js>"ROOT_RELATIVE"</js></c></li>
 * 		</ul>
 * 	</li>
 * 	<li>{@link HtmlDocConfig @HtmlDocConfig}:
 * 		<ul>
 * 			<li><c>{@link HtmlDocConfig#header() header}=<js>"&lt;h1>$RS{title}&lt;/h1>&lt;h2>$RS{operationSummary,description}&lt;/h2>$C{REST/header}"</js></c></li>
 * 			<li><c>{@link HtmlDocConfig#navlinks() navlinks}=<js>"up: request:/.."</js></c></li>
 * 			<li><c>{@link HtmlDocConfig#stylesheet() stylesheet}=<js>"$C{REST/theme,servlet:/htdocs/themes/devops.css}"</js></c></li>
 * 			<li><c>{@link HtmlDocConfig#head() head}=<js>"$C{REST/head}"</js></c></li>
 * 			<li><c>{@link HtmlDocConfig#footer() footer}=<js>"$C{REST/footer}"</js></c></li>
 * 			<li><c>{@link HtmlDocConfig#nowrap() nowrap}=<js>"true"</js></c></li>
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <p>
 * 	This annotation can be applied to REST resource classes to define common default configurations:
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Used on a top-level resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet <jk>implements</jk> BasicUniversalRest { ... }
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Used on a child resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestObject <jk>implements</jk> BasicUniversalRest { ... }
 * </p>
 *
 * <p>
 * 	Note that the framework will aggregate annotations defined on all classes in the class hierarchy with
 * 	values defined on child classes overriding values defined on parent classes.  That allows any values defined
 * 	on this interface to be overridden by annotations defined on the implemented class.
 * </p>
 *
 * <ul class='seealso'>
 * 	<ul class='javatreec'>
 * 		<li class='jc'>{@link BasicRestServlet}
 * 		<li class='jc'>{@link BasicRestServletGroup}
 * 		<li class='jc'>{@link BasicRestObject}
 * 		<li class='jc'>{@link BasicRestObjectGroup}
 *	</ul>
 * </ul>
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
@BeanConfig(
	// When parsing generated beans, ignore unknown properties that may only exist as getters and not setters.
	ignoreUnknownBeanProperties="true"
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
public interface BasicUniversalRest {}
