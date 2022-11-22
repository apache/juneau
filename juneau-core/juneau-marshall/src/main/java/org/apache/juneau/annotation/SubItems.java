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
package org.apache.juneau.annotation;

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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a>
 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface SubItems {

	/**
	 * <mk>default</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] _default() default {};

	/**
	 * <mk>enum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] _enum() default {};

	/**
	 * <mk>$ref</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String $ref() default "";

	/**
	 * Synonym for {@link #collectionFormat()}.
	 *
	 * @return The annotation value.
	 */
	String cf() default "";

	/**
	 * <mk>collectionFormat</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String collectionFormat() default "";

	/**
	 * Synonym for {@link #_default()}.
	 *
	 * @return The annotation value.
	 */
	String[] df() default {};

	/**
	 * Synonym for {@link #_enum()}.
	 *
	 * @return The annotation value.
	 */
	String[] e() default {};

	/**
	 * Synonym for {@link #exclusiveMaximum()}.
	 *
	 * @return The annotation value.
	 */
	boolean emax() default false;

	/**
	 * Synonym for {@link #exclusiveMinimum()}.
	 *
	 * @return The annotation value.
	 */
	boolean emin() default false;

	/**
	 * <mk>exclusiveMaximum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean exclusiveMaximum() default false;

	/**
	 * <mk>exclusiveMinimum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean exclusiveMinimum() default false;

	/**
	 * Synonym for {@link #format()}.
	 *
	 * @return The annotation value.
	 */
	String f() default "";

	/**
	 * <mk>format</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String format() default "";

	/**
	 * <mk>items</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * <p>
	 * This is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * <br>It must be declared free-form because it's not possible to nest annotations in Java.
	 *
	 * @return The annotation value.
	 */
	String[] items() default {};

	/**
	 * Synonym for {@link #maximum()}.
	 *
	 * @return The annotation value.
	 */
	String max() default "";

	/**
	 * Synonym for {@link #maxItems()}.
	 *
	 * @return The annotation value.
	 */
	long maxi() default -1;

	/**
	 * <mk>maximum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String maximum() default "";

	/**
	 * <mk>maxItems</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	long maxItems() default -1;

	/**
	 * Synonym for {@link #maxLength()}.
	 *
	 * @return The annotation value.
	 */
	long maxl() default -1;

	/**
	 * <mk>maxLength</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	long maxLength() default -1;

	/**
	 * Synonym for {@link #minimum()}.
	 *
	 * @return The annotation value.
	 */
	String min() default "";

	/**
	 * Synonym for {@link #minItems()}.
	 *
	 * @return The annotation value.
	 */
	long mini() default -1;

	/**
	 * <mk>minimum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String minimum() default "";

	/**
	 * <mk>minItems</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	long minItems() default -1;

	/**
	 * Synonym for {@link #minLength()}.
	 *
	 * @return The annotation value.
	 */
	long minl() default -1;

	/**
	 * <mk>minLength</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	long minLength() default -1;

	/**
	 * Synonym for {@link #multipleOf()}.
	 *
	 * @return The annotation value.
	 */
	String mo() default "";

	/**
	 * <mk>multipleOf</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String multipleOf() default "";

	/**
	 * Synonym for {@link #pattern()}.
	 *
	 * @return The annotation value.
	 */
	String p() default "";

	/**
	 * <mk>pattern</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String pattern() default "";

	/**
	 * Synonym for {@link #type()}.
	 *
	 * @return The annotation value.
	 */
	String t() default "";

	/**
	 * <mk>type</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String type() default "";

	/**
	 * Synonym for {@link #uniqueItems()}.
	 *
	 * @return The annotation value.
	 */
	boolean ui() default false;

	/**
	 * <mk>uniqueItems</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Swagger Items Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean uniqueItems() default false;
}
