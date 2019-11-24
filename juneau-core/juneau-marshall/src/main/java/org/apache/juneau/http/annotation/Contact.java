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
 * Swagger contact annotation.
 *
 * <p>
 * The contact information for the exposed API.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Normal</jc>
 * 	<ja>@Rest</ja>(
 * 		swagger=<ja>@ResourceSwagger</ja>(
 * 			contact=<ja>@Contact</ja>(
 * 				name=<js>"Juneau Development Team"</js>,
 * 				email=<js>"dev@juneau.apache.org"</js>,
 * 				url=<js>"http://juneau.apache.org"</js>
 * 			)
 * 		)
 * 	)
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Free-form</jc>
 * 	<ja>@Rest</ja>(
 * 		swagger=<ja>@ResourceSwagger</ja>(
 * 			contact=<ja>@Contact</ja>({
 * 				<js>"name:'Juneau Development Team',"</js>,
 * 				<js>"email:'dev@juneau.apache.org',"</js>,
 * 				<js>"url:'http://juneau.apache.org',"</js>,
 * 				<js>"x-extra:'extra field'"</js>
 * 			})
 * 		)
 * 	)
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.Swagger}
 * 	<li class='extlink'>{@doc SwaggerContactObject}
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface Contact {

	/**
	 * <mk>name</mk> field of the {@doc SwaggerContactObject}.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String name() default "";

	/**
	 * <mk>url</mk> field of the {@doc SwaggerContactObject}.
	 *
	 * <p>
	 * The URL pointing to the contact information. MUST be in the format of a URL.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a URL string.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String url() default "";

	/**
	 * <mk>email</mk> field of the {@doc SwaggerContactObject}.
	 *
	 * <p>
	 * The email address of the contact person/organization. MUST be in the format of an email address.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is an email string.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String email() default "";

	/**
	 * Free-form value for the {@doc SwaggerContactObject}.
	 *
	 * <p>
	 * This is a JSON object that makes up the swagger information for this field.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the contact information:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(
	 * 			contact=<ja>@Contact</ja>(
	 * 				name=<js>"Juneau Development Team"</js>,
	 * 				email=<js>"dev@juneau.apache.org"</js>,
	 * 				url=<js>"http://juneau.apache.org"</js>
	 * 			)
	 * 		)
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(
	 * 			contact=<ja>@Contact</ja>({
	 * 				<js>"name: 'Juneau Development Team',"</js>,
	 * 				<js>"email: 'dev@juneau.apache.org',"</js>,
	 * 				<js>"url: 'http://juneau.apache.org'"</js>,
	 * 			})
	 * 		)
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(
	 * 			contact=<ja>@Contact</ja>(<js>"$L{contactSwagger}"</js>)
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>contactSwagger</mk> = <mv>{ name: "Juneau Development Team", email: "dev@juneau.apache.org", url: "http://juneau.apache.org" }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this field from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimpleJson} object.
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Contact</ja>(<js>"{name: 'Juneau Development Team'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Contact</ja>(<js>"name: 'Juneau Development Team'"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] value() default {};
}
