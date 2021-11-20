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
 * Swagger tag annotation.
 *
 * <p>
 * Allows adding meta data to a single tag that is used by the {@doc ExtSwaggerOperationObject}.
 * It is not mandatory to have a Tag Object per tag used there.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// A response object thats a hex-encoded string</jc>
 * 	<ja>@Rest</ja>(
 * 		swagger=<ja>@Swagger</ja>{
 * 			tags={
 * 				<ja>@Tag</ja>(
 * 					name=<js>"utility"</js>,
 * 					description=<js>"Utility methods"</js>
 * 				)
 * 			}
 * 		}
 * 	)
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Free-form</jc>
 * 	<ja>@Rest</ja>(
 * 		swagger=<ja>@Swagger</ja>{
 * 			tags={
 * 				<ja>@Tag</ja>({
 * 					<js>"name:'utility',"</js>,
 * 					<js>"description:'Utility methods'"</js>
 * 				})
 * 			}
 * 		}
 * 	)
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerTagObject}
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface Tag {

	/**
	 * <mk>description</mk> field of the {@doc ExtSwaggerTagObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * <mk>externalDocs</mk> field of the {@doc ExtSwaggerTagObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	ExternalDocs externalDocs() default @ExternalDocs;

	/**
	 * <mk>name</mk> field of the {@doc ExtSwaggerTagObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * </ul>
	 */
	String name() default "";

	/**
	 * Free-form value for the {@doc ExtSwaggerTagObject}.
	 *
	 * <p>
	 * This is a {@doc SimplifiedJson} object that makes up the swagger information for this Tag object.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the resource tags:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Swagger</ja>(
	 * 		tags={
	 * 			<ja>@Tag</ja>(
	 * 				name=<js>"pet"</js>,
	 * 				description=<js>"Everything about your Pets"</js>,
	 * 				externalDocs=<ja>@ExternalDocs</ja>(
	 * 					description="<js>Find out more"</js>,
	 * 					url=<js>"http://juneau.apache.org"</js>
	 * 				}
	 * 			)
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Swagger</ja>(
	 * 		tags={
	 * 			<ja>@Tag</ja>(
	 * 				name=<js>"pet"</js>,
	 * 				api={
	 * 					<js>"name: 'pet',"</js>,
	 * 					<js>"description: 'Everything about your Pets',"</js>,
	 * 					<js>"externalDocs: {"</js>,
	 * 						<js>"description: 'Find out more',"</js>,
	 * 						<js>"url: 'http://juneau.apache.org'"</js>
	 * 					<js>"}"</js>
	 * 				}
	 * 			)
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@Swagger</ja>(
	 * 		tags={
	 * 			<ja>@Tag</ja>(
	 * 				name=<js>"pet"</js>,
	 * 				api=<js>"$L{petTagSwagger}"</js>
	 * 			)
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>petTagSwagger</mk> = <mv>{ name: "pet", description: "Everything about your Pets", externalDocs: { description: "Find out more", url: "http://juneau.apache.org" } }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this body from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Tag</ja>(api=<js>"{description: 'Everything about your Pets'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Tag</ja>(api=<js>"description: 'Everything about your Pets'"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable:
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] value() default {};
}
