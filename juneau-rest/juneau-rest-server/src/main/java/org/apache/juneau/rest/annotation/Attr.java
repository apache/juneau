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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;

/**
 * REST request attribute annotation.
 *
 * <p>
 * Identifies a POJO retrieved from the request attributes map.
 *
 * Annotation that can be applied to a parameter of a <ja>@RestOp</ja>-annotated method to identify it as a value
 * retrieved from the request attributes.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public</jk> Person getPerson(<ja>@Attr</ja>(<js>"ETag"</js>) UUID <jv>etag</jv>) {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>
 * 	<jk>public void</jk> postPerson(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) {
 * 		UUID <jv>etag</jv> = <jv>req</jv>.getAttributes().get(UUID.<jk>class</jk>, <js>"ETag"</js>);
 * 		...
 * 	}
 * </p>
  *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Attr {

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing strings to values.
	 *
	 * <p>
	 * Overrides for this part the part parser defined on the REST resource which by default is {@link OpenApiParser}.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Void.class;

	//=================================================================================================================
	// Attributes common to all Swagger Parameter objects
	//=================================================================================================================

	/**
	 * Request attribute name.
	 *
	 * <p>
	 * The value should be either a valid attribute name, or <js>"*"</js> to represent multiple name/value pairs
	 *
	 * <p>
	 * A blank value (the default) has the following behavior:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If the data type is <c>NameValuePairs</c>, <c>Map</c>, or a bean,
	 * 		then it's the equivalent to <js>"*"</js> which will cause the value to be serialized as name/value pairs.
	 *
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bjava'>
	 * 	<ja>@RestPost</ja>(<js>"/addPet"</js>)
	 * 	<jk>public void</jk> addPet(<ja>@Attr</ja> JsonMap <jv>allAttributes</jv>) {...}
	 * 		</p>
	 * 	</li>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining a header entry:
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Attr</ja>(name=<js>"api_key"</js>) String <jv>apiKey</jv>) {...}
	 * </p>
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Attr</ja>(<js>"api_key"</js>) String <jv>apiKey</jv>) {...}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";
}
