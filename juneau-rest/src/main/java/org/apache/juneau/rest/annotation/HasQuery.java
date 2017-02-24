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
 * Identical to {@link HasFormData @HasFormData}, but only checks the existing of the parameter in the
 * 	URL string, not URL-encoded form posts.
 * <p>
 * Unlike {@link HasFormData @HasFormData}, using this annotation does not result in the servlet reading the contents
 * 	of URL-encoded form posts.
 * Therefore, this annotation can be used in conjunction with the {@link Body @Body} annotation
 * 	or {@link RestRequest#getBody(Class)} method for <code>application/x-www-form-urlencoded POST</code> calls.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doPost(<ja>@HasQuery</ja>(<js>"p1"</js>) <jk>boolean</jk> p1, <ja>@Body</ja> Bean myBean) {
 * 		...
 * 	}
 * </p>
 * <p>
 * This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req) {
 * 		<jk>boolean</jk> p1 = req.hasQueryParameter(<js>"p1"</js>);
 * 		...
 * 	}
 * </p>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface HasQuery {

	/**
	 * URL parameter name.
	 */
	String value();
}
