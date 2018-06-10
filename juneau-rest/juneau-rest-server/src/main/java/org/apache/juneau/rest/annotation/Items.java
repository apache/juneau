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
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Swagger Specification &gt; Items Object</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Items {
	
	String type() default "";
	String format() default "";
	String collectionFormat() default "";
	String pattern() default "";
	String $ref() default "";
	String maximum() default "";
	String minimum() default "";
	String multipleOf() default "";
	String maxLength() default "";
	String minLength() default "";
	String maxItems() default "";
	String minItems() default "";
	String exclusiveMaximum() default "";
	String exclusiveMinimum() default "";
	String uniqueItems() default "";
	String[] _default() default {};
	String[] _enum() default {};
	
	/**
	 * Free-form value for Items objects in Swagger
	 * 
	 * <p>
	 * This is a JSON object that makes up the swagger information for this field.
	 * 
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of an Items object:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Query</ja>(
	 * 		name=<js>"status"</js>, 
	 * 		type=<js>"array"</js>,
	 * 		items=<ja>@Items</ja>(
	 * 			type=<js>"string"</js>,
	 * 			_enum=<js>"AVAILABLE,PENDING,SOLD"</js>,
	 * 			_default=<js>"AVAILABLE"</js>
	 * 		)
	 * 	) 
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Query</ja>(
	 * 		name=<js>"status"</js>, 
	 * 		type=<js>"array"</js>,
	 * 		items=<ja>@Items</ja>({
	 * 			<js>"type: 'string'"</js>,
	 * 			<js>"enum: ['AVAILABLE','PENDING','SOLD'],"</js>,
	 * 			<js>"default: 'AVAILABLE'"</js>
	 * 		})
	 * 	) 
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form as part of parent</jc>
	 * 	<ja>@Query</ja>(
	 * 		name=<js>"status"</js>, 
	 * 		api={
	 * 			<js>"type:'array',"</js>,
	 * 			<js>"items: {"</js>,
	 * 				<js>"type: 'string',"</js>,
	 * 				<js>"enum: ['AVAILABLE','PENDING','SOLD'],"</js>,
	 * 				<js>"default: 'AVAILABLE'"</js>,
	 * 			<js>"}"</js>)
	 * 		}
	 * 	) 
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@Query</ja>(
	 * 		name=<js>"status"</js>, 
	 * 		type=<js>"array"</js>,
	 * 		items=<ja>@Items</ja>(<js>"$L{statusItemsSwagger}"</js>)
	 * 	) 
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>statusItemsSwagger</mk> = <mv>{ type: "string", enum: ["AVAILABLE","PENDING","SOLD"], default: "AVAILABLE" }</mv>
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
	 * 		Note that the only swagger field you can't specify using this value is <js>"name"</js> whose value needs to be known during servlet initialization.
	 * 	<li>
	 * 		The format is a Simplified JSON object.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Items</ja>(api=<js>"{type: 'string'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Items</ja>(api=<js>"type: 'string'"</js>)
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
