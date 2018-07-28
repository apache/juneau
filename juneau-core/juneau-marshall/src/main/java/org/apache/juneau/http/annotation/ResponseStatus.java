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

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * REST response status annotation.
 *
 * <p>
 * Annotation that can be applied to parameters and types to denote them as an HTTP response status on server-side REST method parameters.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Java method arguments and argument-types of server-side <ja>@RestMethod</ja>-annotated REST Java methods.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.HttpPartAnnotations.ResponseStatus">Overview &gt; juneau-rest-server &gt; @ResponseStatus</a>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.Swagger">Overview &gt; juneau-rest-server &gt; OPTIONS pages and Swagger</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#responseObject">Swagger Specification &gt; Response Object</a>
 * </ul>
 */
@Documented
@Target({})
@Retention(RUNTIME)
@Inherited
public @interface ResponseStatus {

	/**
	 * The HTTP status of the response.
	 */
	int code() default 0;

	/**
	 * A synonym to {@link #code()}.
	 *
	 * <p>
	 * Useful if you only want to specify a code only.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the response code:
	 * <p class='bcode w800'>
	 * 	<ja>@ResponseStatus</ja>(code=200)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@ResponseStatus</ja>(200)
	 * </p>
	 */
	int value() default 0;

	/**
	 * <mk>description</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#responseObject">Response</a> object.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * Free-form value for the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#responseObject">Response</a> object.
	 *
	 * <p>
	 * This is a JSON object that makes up the swagger information for this Response object.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the Response object:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@ResponseStatus</ja>(
	 * 		code=401,
	 * 		description=<js>"Invalid user/pw"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@ResponseStatus</ja>(
	 * 		code=401,
	 * 		api={
	 * 			<js>"description: 'Invalid user/pw'"</js>
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form using variables</jc>
	 * 	<ja>@ResponseStatus</ja>(
	 * 		code=401,
	 * 		api=<js>"$L{unauthorizedSwagger}"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>unauthorizedSwagger</mk> = <mv>{ description: "Invalid user/pw" }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this body from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Note that the only swagger field you can't specify using this value is <js>"code"</js> whose value needs to be known during servlet initialization.
	 * 	<li>
	 * 		The format is a <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.JsonDetails.SimplifiedJson'>Simplified JSON</a> object.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@ResponseStatus</ja>(<js>"{description:'Invalid user/pw'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@ResponseStatus</ja>(<js>"description:'Invalid user/pw'"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] api() default {};
}
