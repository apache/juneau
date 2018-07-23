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
 * 	<li>Java method arguments and argument-types of server-side <ja>@RestMethod</ja>-annotated REST Java methods.
 * </ul>
 *
 * <p>
 * Note that this can be used to detect the existence of a parameter when it's not set to a particular value.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
 * 	<jk>public void</jk> doPost(<ja>@HasFormData</ja>(<js>"p1"</js>) <jk>boolean</jk> p1) {
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
 * 	<jk>public void</jk> doPost(RestRequest req) {
 * 		<jk>boolean</jk> p1 = req.hasFormData(<js>"p1"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * The following table shows the behavioral differences between <code>@HasFormData</code> and <code>@FormData</code>...
 * <table class='styled'>
 * 	<tr>
 * 		<th><code>Body content</code></th>
 * 		<th><code><ja>@HasFormData</ja>(<js>"a"</js>)</code></th>
 * 		<th><code><ja>@FormData</ja>(<js>"a"</js>)</code></th>
 * 	</tr>
 * 	<tr>
 * 		<td><code>a=foo</code></td>
 * 		<td><jk>true</jk></td>
 * 		<td><js>"foo"</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>a=</code></td>
 * 		<td><jk>true</jk></td>
 * 		<td><js>""</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>a</code></td>
 * 		<td><jk>true</jk></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>b=foo</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * </table>
 *
 * <h5 class='topic'>Important note concerning FORM posts</h5>
 *
 * This annotation should not be combined with the {@link Body @Body} annotation or <code>RestRequest.getBody()</code> method
 * for <code>application/x-www-form-urlencoded POST</code> posts, since it will trigger the underlying servlet API to
 * parse the body content as key-value pairs, resulting in empty content.
 *
 * <p>
 * The {@link HasQuery @HasQuery} annotation can be used to check for the existing of a URL parameter in the URL string
 * without triggering the servlet to drain the body content.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.HttpPartAnnotations.HasFormData">Overview &gt; juneau-rest-server &gt; @HasFormData</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface HasFormData {

	/**
	 * FORM parameter name.
	 *
	 * Required. The name of the parameter. Parameter names are case sensitive.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain-text.
	 * </ul>
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
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<jk>@HasFormData</jk>(name=<js>"petId"</js>) <jk>boolean</jk> hasPetId) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<jk>@HasFormData</jk>(<js>"petId"</js>) <jk>boolean</jk> hasPetId) {...}
	 * </p>
	 */
	String value() default "";
}
