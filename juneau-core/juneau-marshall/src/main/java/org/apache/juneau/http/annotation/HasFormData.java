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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * REST has-form-data annotation.
 *
 * Identifies whether or not an HTTP request has the specified multipart form POST parameter.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments and argument-types of server-side <ja>@RestOp</ja>-annotated methods.
 * </ul>
 * <p>
 * 	This annotation can be used to detect the existence of a parameter when it's not set to a particular value.
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>
 * 	<jk>public void</jk> doPost(<ja>@HasFormData</ja>(<js>"p1"</js>) <jk>boolean</jk> <jv>p1</jv>) {...}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code:
 * </p>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>
 * 	<jk>public void</jk> doPost(RestRequest <jv>req</jv>) {
 * 		<jk>boolean</jk> <jv>p1</jv> = <jv>req</jv>.getFormParam(<js>"p1"</js>).isPresent();
 * 		...
 * 	}
 * </p>
 * <p>
 * 	The parameter type must be either <jk>boolean</jk> or {@link java.lang.Boolean}.
 * </p>
 * <p>
 * 	The following table shows the behavioral differences between <ja>@HasFormData</ja> and <ja>@FormData</ja>:
 * </p>
 * <table class='styled w400'>
 * 	<tr>
 * 		<th><c>Body content</c></th>
 * 		<th><c><ja>@HasFormData</ja>(<js>"a"</js>)</c></th>
 * 		<th><c><ja>@FormData</ja>(<js>"a"</js>)</c></th>
 * 	</tr>
 * 	<tr>
 * 		<td><c>a=foo</c></td>
 * 		<td><jk>true</jk></td>
 * 		<td><js>"foo"</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td><c>a=</c></td>
 * 		<td><jk>true</jk></td>
 * 		<td><js>""</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td><c>a</c></td>
 * 		<td><jk>true</jk></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td><c>b=foo</c></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * </table>
 *
 * <h5 class='topic'>Important note concerning FORM posts</h5>
 * <p>
 * 	This annotation should not be combined with the {@link Content @Content} annotation or {@code RestRequest.getContent()} method
 * 	for <c>application/x-www-form-urlencoded POST</c> posts, since it will trigger the underlying servlet API to
 * 	parse the body content as key-value pairs, resulting in empty content.
 * </p>
 * <p>
 * 	The {@link HasQuery @HasQuery} annotation can be used to check for the existing of a URL parameter in the URL string
 * 	without triggering the servlet to drain the body content.
 * </p>
 *
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Documented
@Target({PARAMETER})
@Retention(RUNTIME)
@Inherited
public @interface HasFormData {

	/**
	 * FORM parameter name.
	 *
	 * Required. The name of the parameter. Parameter names are case sensitive.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain-text.
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
	 * The following are completely equivalent ways of defining the existence of a form post entry:
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@HasFormData</ja>(name=<js>"petId"</js>) <jk>boolean</jk> <jv>hasPetId</jv>) {...}
	 * </p>
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@HasFormData</ja>(<js>"petId"</js>) <jk>boolean</jk> <jv>hasPetId</jv>) {...}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";
}
