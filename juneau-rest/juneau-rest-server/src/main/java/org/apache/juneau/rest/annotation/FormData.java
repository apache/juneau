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
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method to identify it as a form post
 * entry converted to a POJO.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPost(RestRequest req, RestResponse res,
 * 				<ja>@FormData</ja>(<js>"p1"</js>) <jk>int</jk> p1, <ja>@FormData</ja>(<js>"p2"</js>) String p2, <ja>@FormData</ja>(<js>"p3"</js>) UUID p3) {
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPost(RestRequest req, RestResponse res) {
 * 		<jk>int</jk> p1 = req.getFormData(<jk>int</jk>.<jk>class</jk>, <js>"p1"</js>, 0);
 * 		String p2 = req.getFormData(String.<jk>class</jk>, <js>"p2"</js>);
 * 		UUID p3 = req.getFormData(UUID.<jk>class</jk>, <js>"p3"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <h6 class='topic'>Important note concerning FORM posts</h6>
 *
 * This annotation should not be combined with the {@link Body @Body} annotation or {@link RestRequest#getBody()} method
 * for <code>application/x-www-form-urlencoded POST</code> posts, since it will trigger the underlying servlet
 * API to parse the body content as key-value pairs resulting in empty content.
 *
 * <p>
 * The {@link Query @Query} annotation can be used to retrieve a URL parameter in the URL string without triggering the
 * servlet to drain the body content.
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface FormData {

	/**
	 * FORM parameter name.
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 */
	String value() default "";

	/**
	 * Specify <jk>true</jk> if using multi-part parameters to represent collections and arrays.
	 *
	 * <p>
	 * Normally, we expect single parameters to be specified in UON notation for representing collections of values
	 * (e.g. <js>"key=(1,2,3)"</js>.
	 * This annotation allows the use of multi-part parameters to represent collections (e.g.
	 * <js>"key=1&amp;key=2&amp;key=3"</js>.
	 *
	 * <p>
	 * This setting should only be applied to Java parameters of type array or Collection.
	 */
	boolean multipart() default false;

	/**
	 * The expected format of the request parameter.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"UON"</js> - URL-Encoded Object Notation.
	 * 		<br>This notation allows for request parameters to contain arbitrarily complex POJOs.
	 * 	<li>
	 * 		<js>"PLAIN"</js> - Plain text.
	 * 		<br>This treats request parameters as plain text.
	 * 		<br>Only POJOs directly convertible from <l>Strings</l> can be represented in parameters when using this mode.
	 * 	<li>
	 * 		<js>"INHERIT"</js> (default) - Inherit from the {@link RestResource#paramFormat()} property on the
	 * 		servlet method or class.
	 * </ul>
	 *
	 * <p>
	 * Note that the parameter value <js>"(foo)"</js> is interpreted as <js>"(foo)"</js> when using plain mode, but
	 * <js>"foo"</js> when using UON mode.
	 */
	String format() default "INHERIT";

	/**
	 * The default value for this form-data parameter if it's not present in the request.
	 */
	String def() default "";
}
