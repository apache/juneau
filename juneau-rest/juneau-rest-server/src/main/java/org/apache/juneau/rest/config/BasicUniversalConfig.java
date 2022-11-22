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
import org.apache.juneau.csv.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Predefined configuration for a REST resource that supports all languages
 * and provides common default configuration values.
 *
 * <p>
 * 	Default settings defined:
 * </p>
 * <ul class='spaced-list'>
 * 	<li class='ja'>{@link Rest}:
 * 		<ul>
 * 			<li class='jma'>{@link Rest#serializers() serializers}:
 * 				<ul class='javatree'>
 * 					<li class='jc'>{@link HtmlDocSerializer}
 * 					<li class='jc'>{@link HtmlStrippedDocSerializer}
 * 					<li class='jc'>{@link HtmlSchemaDocSerializer}
 * 					<li class='jc'>{@link JsonSerializer}
 * 					<li class='jc'>{@link Json5Serializer}
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
 * 			<li class='jma'>{@link Rest#parsers() parsers}:
 * 				<ul class='javatree'>
 * 					<li class='jc'>{@link JsonParser}
 * 					<li class='jc'>{@link Json5Parser}
 * 					<li class='jc'>{@link XmlParser}
 * 					<li class='jc'>{@link HtmlParser}
 * 					<li class='jc'>{@link UonParser}
 * 					<li class='jc'>{@link UrlEncodingParser}
 * 					<li class='jc'>{@link OpenApiParser}
 * 					<li class='jc'>{@link MsgPackParser}
 * 					<li class='jc'>{@link PlainTextParser}
 * 				</ul>
 * 			</li>
 * 			<li class='jma'>{@link Rest#config() config}:  <js>"$S{juneau.configFile,SYSTEM_DEFAULT}"</js>
 *		</ul>
 *	</li>
 * 	<li class='ja'>{@link BeanConfig}:
 * 		<ul>
 * 			<li class='jma'>{@link BeanConfig#ignoreUnknownBeanProperties() ignoreUnknownBeanProperties}:  <js>"true"</js>
 * 		</ul>
 * 	</li>
 * 	<li class='ja'>{@link SerializerConfig}:
 * 		<ul>
 * 			<li class='jma'>{@link SerializerConfig#uriResolution() uriResolution}:  <js>"ROOT_RELATIVE"</js>
 * 		</ul>
 * 	</li>
 * 	<li class='ja'>{@link HtmlDocConfig}:
 * 		<ul>
 * 			<li class='jma'>{@link HtmlDocConfig#header() header}:  <js>"&lt;h1&gt;$RS{title}&lt;/h1&gt;&lt;h2&gt;$RS{operationSummary,description}&lt;/h2&gt;$C{REST/header}"</js>
 * 			<li class='jma'>{@link HtmlDocConfig#navlinks() navlinks}:  <js>"up: request:/.."</js>
 * 			<li class='jma'>{@link HtmlDocConfig#stylesheet() stylesheet}:  <js>"$C{REST/theme,servlet:/htdocs/themes/devops.css}"</js>
 * 			<li class='jma'>{@link HtmlDocConfig#head() head}:  <js>"$C{REST/head}"</js>
 * 			<li class='jma'>{@link HtmlDocConfig#footer() footer}:  <js>"$C{REST/footer}"</js>
 * 			<li class='jma'>{@link HtmlDocConfig#nowrap() nowrap}:  <js>"true"</js>
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <p>
 * 	This annotation can be applied to REST resource classes to define common default configurations:
 * </p>
 * <p class='bjava'>
 * 	<jc>// Used on a top-level resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet <jk>implements</jk> BasicUniversalConfig { ... }
 * </p>
 * <p class='bjava'>
 * 	<jc>// Used on a child resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestObject <jk>implements</jk> BasicUniversalConfig { ... }
 * </p>
 *
 * <p>
 * 	Note that the framework will aggregate annotations defined on all classes in the class hierarchy with
 * 	values defined on child classes overriding values defined on parent classes.  That allows any values defined
 * 	on this interface to be overridden by annotations defined on the implemented class.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<ul class='javatreec'>
 * 		<li class='jc'>{@link BasicRestServlet}
 * 		<li class='jc'>{@link BasicRestServletGroup}
 * 		<li class='jc'>{@link BasicRestObject}
 * 		<li class='jc'>{@link BasicRestObjectGroup}
 *	</ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.AnnotatedClasses">@Rest-Annotated Classes</a>
 * </ul>
 */
@Rest(

	// Default serializers for all Java methods in the class.
	serializers={
		HtmlDocSerializer.class, // HTML must be listed first because Internet Explore does not include text/html in their Accept header.
		HtmlStrippedDocSerializer.class,
		HtmlSchemaDocSerializer.class,
		JsonSerializer.class,
		Json5Serializer.class,
		JsonSchemaSerializer.class,
		XmlDocSerializer.class,
		UonSerializer.class,
		UrlEncodingSerializer.class,
		OpenApiSerializer.class,
		MsgPackSerializer.class,
		SoapXmlSerializer.class,
		PlainTextSerializer.class,
		CsvSerializer.class
	},

	// Default parsers for all Java methods in the class.
	parsers={
		JsonParser.class,
		Json5Parser.class,
		XmlParser.class,
		HtmlParser.class,
		UonParser.class,
		UrlEncodingParser.class,
		OpenApiParser.class,
		MsgPackParser.class,
		PlainTextParser.class,
		CsvParser.class
	}
)
public interface BasicUniversalConfig extends DefaultConfig, DefaultHtmlConfig {}
