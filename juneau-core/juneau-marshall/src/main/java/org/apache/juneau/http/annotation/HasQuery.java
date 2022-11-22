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
 * REST has-query-parameter annotation.
 *
 * <p>
 * Identifies whether or not an HTTP request has the specified query parameter.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments and argument-types of server-side <ja>@RestOp</ja>-annotated methods.
 * </ul>
 * <p>
 * 	Identical to {@link HasFormData @HasFormData}, but only checks the existing of the parameter in the URL string, not
 * 	URL-encoded form posts.
 * </p>
 * <p>
 * 	Unlike {@link HasFormData @HasFormData}, using this annotation does not result in the servlet reading the contents
 * 	of URL-encoded form posts.
 * 	Therefore, this annotation can be used in conjunction with the {@link Content @Cpmtemt} annotation or
 * 	{@code RestRequest.getContent()} method for <c>application/x-www-form-urlencoded POST</c> calls.
 *  </p>
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(<ja>@HasQuery</ja>(<js>"p1"</js>) <jk>boolean</jk> <jv>p1</jv>) {...}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code:
 * </p>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>) {
 * 		<jk>boolean</jk> <jv>p1</jv> = <jv>req</jv>.getQueryParam(<js>"p1"</js>).isPresent();
 * 		...
 * 	}
 * </p>
 * <p>
 * 	The parameter type must be either <jk>boolean</jk> or {@link java.lang.Boolean}.
 * </p>
 * <p>
 * 	The following table shows the behavioral differences between <ja>@HasQuery</ja> and <ja>@Query</ja>:
 * </p>
 * <table class='styled w400'>
 * 	<tr>
 * 		<th><c>Query content</c></th>
 * 		<th><c><ja>@HasQuery</ja>(<js>"a"</js>)</c></th>
 * 		<th><c><ja>@Query</ja>(<js>"a"</js>)</c></th>
 * 	</tr>
 * 	<tr>
 * 		<td><c>?a=foo</c></td>
 * 		<td><jk>true</jk></td>
 * 		<td><js>"foo"</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td><c>?a=</c></td>
 * 		<td><jk>true</jk></td>
 * 		<td><js>""</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td><c>?a</c></td>
 * 		<td><jk>true</jk></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td><c>?b=foo</c></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * </table>
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Documented
@Target({PARAMETER})
@Retention(RUNTIME)
@Inherited
public @interface HasQuery {

	/**
	 * URL query parameter name.
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
	 * The following are completely equivalent ways of defining the existence of a query entry:
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@HasQuery</ja>(name=<js>"petId"</js>) <jk>boolean</jk> <jv>hasPetId</jv>) {...}
	 * </p>
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@HasQuery</ja>(<js>"petId"</js>) <jk>boolean</jk> <jv>hasPetId</jv>) {...}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";
}
