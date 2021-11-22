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
 * Swagger items annotation.
 *
 * <p>
 * This class is essentially identical to {@link Items} except it's used for defining items of items.
 *
 * <p>
 * Since annotations cannot be nested, we're forced to create a separate annotation for it.
 * <br>If you want to nest items further, you have to define them free-form using {@link #items()} as free-form JSON.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerItemsObject}
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface SubItems {

	/**
	 * <mk>default</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String[] _default() default {};

	/**
	 * <mk>enum</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String[] _enum() default {};

	/**
	 * <mk>$ref</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String $ref() default "";

	/**
	 * Synonym for {@link #collectionFormat()}.
	 */
	String cf() default "";

	/**
	 * <mk>collectionFormat</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String collectionFormat() default "";

	/**
	 * Synonym for {@link #_default()}.
	 */
	String[] df() default {};

	/**
	 * Synonym for {@link #_enum()}.
	 */
	String[] e() default {};

	/**
	 * Synonym for {@link #exclusiveMaximum()}.
	 */
	boolean emax() default false;

	/**
	 * Synonym for {@link #exclusiveMinimum()}.
	 */
	boolean emin() default false;

	/**
	 * <mk>exclusiveMaximum</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	boolean exclusiveMaximum() default false;

	/**
	 * <mk>exclusiveMinimum</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	boolean exclusiveMinimum() default false;

	/**
	 * Synonym for {@link #format()}.
	 */
	String f() default "";

	/**
	 * <mk>format</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String format() default "";

	/**
	 * <mk>items</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * <p>
	 * This is a {@doc SimplifiedJson} object.
	 * <br>It must be declared free-form because it's not possible to nest annotations in Java.
	 */
	String[] items() default {};

	/**
	 * Synonym for {@link #maximum()}.
	 */
	String max() default "";

	/**
	 * Synonym for {@link #maxItems()}.
	 */
	long maxi() default -1;

	/**
	 * <mk>maximum</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String maximum() default "";

	/**
	 * <mk>maxItems</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	long maxItems() default -1;

	/**
	 * Synonym for {@link #maxLength()}.
	 */
	long maxl() default -1;

	/**
	 * <mk>maxLength</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	long maxLength() default -1;

	/**
	 * Synonym for {@link #minimum()}.
	 */
	String min() default "";

	/**
	 * Synonym for {@link #minItems()}.
	 */
	long mini() default -1;

	/**
	 * <mk>minimum</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String minimum() default "";

	/**
	 * <mk>minItems</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	long minItems() default -1;

	/**
	 * Synonym for {@link #minLength()}.
	 */
	long minl() default -1;

	/**
	 * <mk>minLength</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	long minLength() default -1;

	/**
	 * Synonym for {@link #multipleOf()}.
	 */
	String mo() default "";

	/**
	 * <mk>multipleOf</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String multipleOf() default "";

	/**
	 * Synonym for {@link #pattern()}.
	 */
	String p() default "";

	/**
	 * <mk>pattern</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String pattern() default "";

	/**
	 * Synonym for {@link #type()}.
	 */
	String t() default "";

	/**
	 * <mk>type</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	String type() default "";

	/**
	 * Synonym for {@link #uniqueItems()}.
	 */
	boolean ui() default false;

	/**
	 * <mk>uniqueItems</mk> field of the {@doc ExtSwaggerItemsObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a plain-text string.
	 * </ul>
	 */
	boolean uniqueItems() default false;

	/**
	 * Free-form value for the {@doc ExtSwaggerItemsObject}.
	 *
	 * <p>
	 * This is a {@doc SimplifiedJson} object that makes up the swagger information for this field.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Note that the only swagger field you can't specify using this value is <js>"name"</js> whose value needs to be known during servlet initialization.
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
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
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] value() default {};
}
