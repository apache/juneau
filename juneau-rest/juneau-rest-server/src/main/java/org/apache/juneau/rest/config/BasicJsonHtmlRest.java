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
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.annotation.*;

/**
 * Basic configuration for a REST resource that supports JSON and HTML transport.
 *
 * <p>
 * 	Default settings defined:
 * </p>
 * <ul class='spaced-list'>
 * 	<li>{@link Rest @Rest}:
 * 		<ul>
 * 			<li><c>{@link Rest#serializers() serializers}=</c>
 * 				<ul class='javatreec'>
 * 					<li class='jc'>{@link JsonSerializer}
 * 					<li class='jc'>{@link HtmlDocSerializer}
 * 				</ul>
 * 			</li>
 * 			<li><c>{@link Rest#parsers() parsers}=</c>
 * 				<ul class='javatreec'>
 * 					<li class='jc'>{@link JsonParser}
 * 					<li class='jc'>{@link HtmlParser}
 * 				</ul>
 * 			</li>
 * 			<li><c>{@link Rest#defaultAccept() defaultAccept}=<js>"text/json"</c></li>
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
 * </ul>
 *
 * <p>
 * 	This annotation can be applied to REST resource classes to define common JSON default configurations:
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Used on a top-level resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet <jk>implements</jk> BasicJsonRest { ... }
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Used on a child resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestObject <jk>implements</jk> BasicJsonRest { ... }
 * </p>
 *
 * <p>
 * 	Note that the framework will aggregate annotations defined on all classes in the class hierarchy with
 * 	values defined on child classes overriding values defined on parent classes.  That allows any values defined
 * 	on this interface to be overridden by annotations defined on the implemented class.
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.AnnotatedClasses}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Rest(

	// Default serializers for all Java methods in the class.
	serializers={
		HtmlDocSerializer.class,
		JsonSerializer.class
	},

	// Default parsers for all Java methods in the class.
	parsers={
		HtmlParser.class,
		JsonParser.class
	},

	defaultAccept="text/json",

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
public interface BasicJsonHtmlRest {}
