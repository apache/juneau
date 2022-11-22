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
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.annotation.*;

/**
 * Basic configuration for a REST resource that supports JSON transport.
 *
 * <p>
 * 	Default settings defined:
 * </p>
 * <ul class='spaced-list'>
 * 	<li class='ja'>{@link Rest}:
 * 		<ul>
 * 			<li class='jma'>{@link Rest#serializers() serializers}:
 * 				<ul class='javatree'>
 * 					<li class='jc'>{@link JsonSerializer}
 * 					<li class='jc'>{@link Json5Serializer}
 * 				</ul>
 * 			</li>
 * 			<li class='jma'>{@link Rest#parsers() parsers}:
 * 				<ul class='javatree'>
 * 					<li class='jc'>{@link JsonParser}
 * 					<li class='jc'>{@link Json5Parser}
 * 				</ul>
 * 			</li>
 * 			<li class='jma'>{@link Rest#defaultAccept() defaultAccept}:  <js>"text/json"</js>
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
 * </ul>
 *
 * <p>
 * 	This annotation can be applied to REST resource classes to define common JSON default configurations:
 * </p>
 * <p class='bjava'>
 * 	<jc>// Used on a top-level resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet <jk>implements</jk> BasicJsonConfig { ... }
 * </p>
 * <p class='bjava'>
 * 	<jc>// Used on a child resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestObject <jk>implements</jk> BasicJsonConfig { ... }
 * </p>
 *
 * <p>
 * 	Note that the framework will aggregate annotations defined on all classes in the class hierarchy with
 * 	values defined on child classes overriding values defined on parent classes.  That allows any values defined
 * 	on this interface to be overridden by annotations defined on the implemented class.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.AnnotatedClasses">@Rest-Annotated Classes</a>
 * </ul>
 */
@Rest(

	// Default serializers for all Java methods in the class.
	serializers={
		JsonSerializer.class,
		Json5Serializer.class
	},

	// Default parsers for all Java methods in the class.
	parsers={
		JsonParser.class,
		Json5Parser.class
	},

	defaultAccept="text/json"
)
public interface BasicJsonConfig extends DefaultConfig {}
