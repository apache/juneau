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

import org.apache.juneau.rest.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method
 * 	to identify whether or not the request has the specified multipart form POST parameter.
 * <p>
 * Note that this can be used to detect the existence of a parameter when it's not set to a particular value.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPost(<ja>@HasFormData</ja>(<js>"p1"</js>) <jk>boolean</jk> p1) {
 * 		...
 * 	}
 * </p>
 * <p>
 * This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPost(RestRequest req) {
 * 		<jk>boolean</jk> p1 = req.hasFormData(<js>"p1"</js>);
 * 		...
 * 	}
 * </p>
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
 * <h6 class='topic'>Important note concerning FORM posts</h6>
 * <p>
 * This annotation should not be combined with the {@link Body @Body} annotation or {@link RestRequest#getBody()} method
 * 	for <code>application/x-www-form-urlencoded POST</code> posts, since it will trigger the underlying servlet API to parse the body
 * 	content as key-value pairs, resulting in empty content.
 * <p>
 * The {@link HasQuery @HasQuery} annotation can be used to check for the existing of a URL parameter
 * 	in the URL string without triggering the servlet to drain the body content.
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface HasFormData {

	/**
	 * URL parameter name.
	 */
	String value();
}
