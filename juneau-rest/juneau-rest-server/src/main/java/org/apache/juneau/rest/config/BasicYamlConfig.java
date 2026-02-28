/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.config;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.yaml.*;

/**
 * Basic configuration for a REST resource that supports YAML transport.
 *
 * <p>
 * 	Default settings defined:
 * </p>
 * <ul class='spaced-list'>
 * 	<li class='ja'>{@link Rest}:
 * 		<ul>
 * 			<li class='jma'>{@link Rest#serializers() serializers}:
 * 				<ul class='javatree'>
 * 					<li class='jc'>{@link YamlSerializer}
 * 				</ul>
 * 			</li>
 * 			<li class='jma'>{@link Rest#parsers() parsers}:
 * 				<ul class='javatree'>
 * 					<li class='jc'>{@link YamlParser}
 * 				</ul>
 * 			</li>
 * 			<li class='jma'>{@link Rest#defaultAccept() defaultAccept}:  <js>"application/yaml"</js>
 *		</ul>
 *	</li>
 * </ul>
 *
 * <p>
 * 	This annotation can be applied to REST resource classes to define common YAML default configurations:
 * </p>
 * <p class='bjava'>
 * 	<jc>// Used on a top-level resource.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet <jk>implements</jk> BasicYamlConfig { ... }
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
// @formatter:off
@Rest(

	// Default serializers for all Java methods in the class.
	serializers={
		YamlSerializer.class,
	},

	// Default parsers for all Java methods in the class.
	parsers={
		YamlParser.class,
	},

	defaultAccept="application/yaml"
)
public interface BasicYamlConfig extends DefaultConfig {}
