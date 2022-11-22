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
import org.apache.juneau.jena.*;
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
 * Identical to {@link BasicUniversalConfig} but includes RDF marshalling support.
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
 * 					<li class='jc'>{@link RdfXmlSerializer}
 * 					<li class='jc'>{@link RdfXmlAbbrevSerializer}
 * 					<li class='jc'>{@link TurtleSerializer}
 * 					<li class='jc'>{@link NTripleSerializer}
 * 					<li class='jc'>{@link N3Serializer}
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
 * 					<li class='jc'>{@link RdfXmlParser}
 * 					<li class='jc'>{@link TurtleParser}
 * 					<li class='jc'>{@link NTripleParser}
 * 					<li class='jc'>{@link N3Parser}
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
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet <jk>implements</jk> BasicUniversalJenaConfig { ... }
 * </p>
 * <p class='bjava'>
 * 	<jc>// Used on a child resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestObject <jk>implements</jk> BasicUniversalJenaConfig { ... }
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
 * 	<li class='link'>{doc jrs.AnnotatedClasses}
 * </ul>
 */
@Rest(
	serializers={
		RdfXmlSerializer.class,
		RdfXmlAbbrevSerializer.class,
		TurtleSerializer.class,
		NTripleSerializer.class,
		N3Serializer.class
	},
	parsers={
		RdfXmlParser.class,
		TurtleParser.class,
		NTripleParser.class,
		N3Parser.class
	}
)
public interface BasicUniversalJenaConfig extends BasicUniversalConfig {}
