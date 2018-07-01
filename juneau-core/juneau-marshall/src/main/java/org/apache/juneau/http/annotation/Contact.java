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

import org.apache.juneau.json.*;

/**
 * Swagger contact annotation.
 * 
 * <p>
 * The contact information for the exposed API.
 * 
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Normal</jc>
 * 	<ja>@RestResource</ja>(
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
 * 	<ja>@RestResource</ja>(
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
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#contactObject">Swagger Specification &gt; Contact Object</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Contact {
	
	/**
	 * <mk>name</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#contactObject">Contact</a> object.
	 * 
	 * <p>
	 * The identifying name of the contact person/organization.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String name() default "";

	/**
	 * <mk>url</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#contactObject">Contact</a> object.
	 * 
	 * <p>
	 * The URL pointing to the contact information. MUST be in the format of a URL.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a URL string.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String url() default "";

	/**
	 * <mk>email</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#contactObject">Contact</a> object.
	 * 
	 * <p>
	 * The email address of the contact person/organization. MUST be in the format of an email address.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is an email string.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String email() default "";
	
	/**
	 * Free-form value for the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#contactObject">Contact</a> object.
	 * 
	 * <p>
	 * This is a JSON object that makes up the swagger information for this field.
	 * 
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the contact information:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@RestResource</ja>(
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
	 * 	<ja>@RestResource</ja>(
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
	 * 	<ja>@RestResource</ja>(
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a {@link JsonSerializer#DEFAULT_LAX Simple-JSON} object.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
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
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] value() default {};
}
