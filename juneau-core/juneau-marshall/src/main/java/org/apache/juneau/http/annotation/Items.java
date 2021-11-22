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

import org.apache.juneau.oapi.*;

/**
 * Swagger items annotation.
 *
 * <p>
 * A limited subset of JSON-Schema's items object.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 * <br>Also used to define OpenAPI schema information for POJOs serialized through {@link OpenApiSerializer} and parsed through {@link OpenApiParser}.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Items have a specific set of enumerated string values</jc>
 * 	<ja>@Query</ja>(
 * 		name=<js>"status"</js>,
 * 		type=<js>"array"</js>,
 * 		collectionFormat=<js>"csv"</js>,
 * 		items=<ja>@Items</ja>(
 * 			type=<js>"string"</js>,
 * 			_enum=<js>"AVAILABLE,PENDING,SOLD"</js>,
 * 			_default=<js>"AVAILABLE"</js>
 *		)
 * 	)
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// An array of arrays, the internal array being of type integer, numbers must be between 0 and 63 (inclusive)</jc>
 * 	<ja>@Query</ja>(
 * 		name=<js>"status"</js>,
 * 		type=<js>"array"</js>,
 * 		collectionFormat=<js>"csv"</js>,
 * 		items=<ja>@Items</ja>(
 * 			type=<js>"array"</js>,
 * 			items=<ja>@SubItems</ja>(
 * 				type=<js>"integer"</js>,
 * 				minimum=<js>"0"</js>,
 * 				maximum=<js>"63"</js>
 * 			)
 *		)
 * 	)
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerItemsObject}
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface Items {

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
	 * 		Each entry is a possible value.  Can also contain comma-delimited lists of values.
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
	 */
	SubItems items() default @SubItems;

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
}
