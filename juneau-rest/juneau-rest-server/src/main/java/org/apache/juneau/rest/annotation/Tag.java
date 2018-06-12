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

/**
 * Swagger schema annotation.
 * 
 * <p>
 * The Schema Object allows the definition of input and output data types. 
 * These types can be objects, but also primitives and arrays. 
 * This object is based on the JSON Schema Specification Draft 4 and uses a predefined subset of it. 
 * On top of this subset, there are extensions provided by this specification to allow for more complete documentation.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#tagObject">Swagger Specification &gt; Tag Object</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Tag {
	
	/**
	 * <mk>name</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#tagObject">Tag</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * </ul>
	 */
	String name() default "";
	
	/**
	 * A synonym for {@link #name()}.
	 * 
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 * 
	 * <p>
	 * The following are completely equivalent ways of defining a simple tag:
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(
	 * 			tags={
	 * 				<ja>@Tag</ja>(name=<js>"store"</js>)
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(
	 * 			tags={
	 * 				<ja>@Tag</ja>(<js>"store"</js>)
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 */
	String value() default "";
	
	/**
	 * <mk>description</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#tagObject">Tag</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};
	
	/**
	 * <mk>externalDocs</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#tagObject">Tag</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	ExternalDocs externalDocs() default @ExternalDocs;
	
	/**
	 * Free-form value for the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#tagObject">Tag</a> object.
	 * 
	 * <p>
	 * This is a JSON object that makes up the swagger information for this Tag object.
	 * 
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the resource tags:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@RestResource</ja>(
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
	 * 	<ja>@RestResource</ja>(
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
	 * 	<ja>@RestResource</ja>(
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a Simplified JSON object.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
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
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] api() default {};
}
