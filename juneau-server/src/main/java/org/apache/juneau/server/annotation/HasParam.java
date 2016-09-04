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
package org.apache.juneau.server.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.server.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method
 * 	to identify whether or not the request has the specified GET or POST parameter.
 * <p>
 * Note that this can be used to detect the existence of a parameter when it's not set to a particular value.
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(<ja>@HasParam</ja>(<js>"p1"</js>) <jk>boolean</jk> p1) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req) {
 * 		<jk>boolean</jk> p1 = req.hasParameter(<js>"p1"</js>);
 * 		...
 * 	}
 * </p>
 * <p>
 * The following table shows the behavioral differences between <code>@HasParam</code> and <code>@Param</code>...
 * <table class='styled'>
 * 	<tr>
 * 		<th><code>URL</code></th>
 * 		<th><code><ja>@HasParam</ja>(<js>"a"</js>)</code></th>
 * 		<th><code><ja>@Param</ja>(<js>"a"</js>)</code></th>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?a=foo</code></td>
 * 		<td><code><jk>true</jk></td>
 * 		<td><code><js>"foo"</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?a=</code></td>
 * 		<td><code><jk>true</jk></td>
 * 		<td><code><js>""</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?a</code></td>
 * 		<td><code><jk>true</jk></td>
 * 		<td><code><jk>null</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?b=foo</code></td>
 * 		<td><code><jk>false</jk></td>
 * 		<td><code><jk>null</jk></td>
 * 	</tr>
 * </table>
 *
 * <h6 class='topic'>Important note concerning FORM posts</h6>
 * <p>
 * This annotation should not be combined with the {@link Content @Content} annotation or {@link RestRequest#getInput(Class)} method
 * 	for <code>application/x-www-form-urlencoded POST</code> posts, since it will trigger the underlying servlet API to parse the body
 * 	content as key-value pairs, resulting in empty content.
 * <p>
 * The {@link HasQParam @HasQParam} annotation can be used to check for the existing of a URL parameter
 * 	in the URL string without triggering the servlet to drain the body content.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface HasParam {

	/**
	 * URL parameter name.
	 */
	String value();
}
