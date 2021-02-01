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
package org.apache.juneau.jsonschema.annotation;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Swagger external documentation annotation.
 *
 * <p>
 * Allows referencing an external resource for extended documentation.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Normal</jc>
 * 	<ja>@Rest</ja>(
 * 		swagger=<ja>@Swagger</ja>(
 * 			externalDocs=<ja>@ExternalDocs</ja>(
 * 				description=<js>"Apache Juneau"</js>,
 * 				url=<js>"http://juneau.apache.org"</js>
 * 			)
 * 		)
 * 	)
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Free-form</jc>
 * 	<ja>@Rest</ja>(
 * 		swagger=<ja>@Swagger</ja>(
 * 			contact=<ja>@ExternalDocs</ja>({
 * 				<js>"description:'Apache Juneau',"</js>,
 * 				<js>"url:'http://juneau.apache.org'"</js>,
 * 			})
 * 		)
 * 	)
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerExternalDocumentationObject}
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface ExternalDocs {

	/**
	 * <mk>description</mk> field of the {@doc ExtSwaggerExternalDocumentationObject}.
	 *
	 * <p>
	 * A short description of the target documentation.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * <mk>url</mk> field of the {@doc ExtSwaggerExternalDocumentationObject}.
	 *
	 * <p>
	 * The URL for the target documentation. Value MUST be in the format of a URL.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The value is required.
	 * 	<li>
	 * 		The format is a URL string.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String url() default "";

	/**
	 * Free-form value for the {@doc ExtSwaggerExternalDocumentationObject}.
	 *
	 * <p>
	 * This is a {@doc SimplifiedJson} object that makes up the swagger information for this field.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of documentation:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			externalDocs=<ja>@ExternalDocs</ja>(
	 * 				description=<js>"Find out more about Juneau"</js>,
	 * 				url=<js>"http://juneau.apache.org"</js>
	 * 			)
	 * 		)
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			externalDocs=<ja>@ExternalDocs</ja>({
	 * 				<js>"description: 'Find out more about Juneau',"</js>,
	 * 				<js>"url: 'http://juneau.apache.org'"</js>
	 * 			})
	 * 		)
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			externalDocs=<ja>@ExternalDocs</ja>(<js>"$L{externalDocsSwagger}"</js>)
	 * 		)
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>externalDocsSwagger</mk> = <mv>{ description: "Find out more about Juneau", url: "http://juneau.apache.org" }</mv>
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
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@ExternalDocs</ja>(<js>"{url:'http://juneau.apache.org'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@ExternalDocs</ja>(<js>"url:'http://juneau.apache.org'"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] value() default {};
}
