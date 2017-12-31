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
import org.apache.juneau.rest.*;

/**
 * Identical to {@link FormData @FormData}, but only retrieves the parameter from the URL string, not URL-encoded form
 * posts.
 *
 * <p>
 * Unlike {@link FormData @FormData}, using this annotation does not result in the servlet reading the contents of
 * URL-encoded form posts.
 * Therefore, this annotation can be used in conjunction with the {@link Body @Body} annotation or
 * {@link RestRequest#getBody()} method for <code>application/x-www-form-urlencoded POST</code> calls.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 				<ja>@Query</ja>(<js>"p1"</js>) <jk>int</jk> p1, <ja>@Query</ja>(<js>"p2"</js>) String p2, <ja>@Query</ja>(<js>"p3"</js>) UUID p3) {
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		<jk>int</jk> p1 = req.getQueryParameter(<jk>int</jk>.<jk>class</jk>, <js>"p1"</js>, 0);
 * 		String p2 = req.getQueryParameter(String.<jk>class</jk>, <js>"p2"</js>);
 * 		UUID p3 = req.getQueryParameter(UUID.<jk>class</jk>, <js>"p3"</js>);
 * 		...
 * 	}
 * </p>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Query {

	/**
	 * URL query parameter name.
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
	 * (e.g. <js>"&amp;key=(1,2,3)"</js>.
	 * This annotation allows the use of multi-part parameters to represent collections
	 * (e.g. <js>"&amp;key=1&amp;key=2&amp;key=3"</js>.
	 *
	 * <p>
	 * This setting should only be applied to Java parameters of type array or Collection.
	 */
	boolean multipart() default false;

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing values from strings.
	 *
	 * <p>
	 * The default value for this parser is inherited from the servlet/method which defaults to {@link UonPartParser}.
	 * <br>You can use {@link SimplePartParser} to parse POJOs that are directly convertible from <code>Strings</code>.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Null.class;


	/**
	 * The default value for this query parameter if it's not present in the request.
	 */
	String def() default "";
}
