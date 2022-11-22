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
package org.apache.juneau.uon.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;

/**
 * Annotation for specifying config properties defined in {@link UonSerializer} and {@link UonParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.UonDetails">UON Details</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({UonConfigAnnotation.SerializerApply.class,UonConfigAnnotation.ParserApply.class})
public @interface UonConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// UonCommon
	//-------------------------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------------------------
	// UonParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Decode <js>"%xx"</js> sequences.
	 *
	 * <p>
	 * Specify <js>"true"</js> if URI encoded characters should be decoded, <js>"false"</js> if they've already been decoded
	 * before being passed to this parser.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default for {@link UrlEncodingParser})
	 * 	<li><js>"false"</js> (default for {@link UonParser})
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.uon.UonParser.Builder#decoding()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String decoding() default "";

	/**
	 * Validate end.
	 *
	 * <p>
	 * If <js>"true"</js>, after parsing a POJO from the input, verifies that the remaining input in
	 * the stream consists of only comments or whitespace.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>

	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.uon.UonParser.Builder#validateEnd()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String validateEnd() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// UonSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <js>"true"</js>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.uon.UonSerializer.Builder#addBeanTypesUon()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addBeanTypes() default "";

	/**
	 * Encode non-valid URI characters.
	 *
	 * <p>
	 * Encode non-valid URI characters with <js>"%xx"</js> constructs.
	 *
	 * <p>
	 * If <js>"true"</js>, non-valid URI characters will be converted to <js>"%xx"</js> sequences.
	 * <br>Set to <js>"false"</js> if parameter value is being passed to some other code that will already perform
	 * URL-encoding of non-valid URI characters.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default for {@link UrlEncodingSerializer})
	 * 	<li><js>"false"</js> (default for {@link UonSerializer})
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.uon.UonSerializer.Builder#encoding()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String encoding() default "";

	/**
	 * Format to use for query/form-data/header values.
	 *
	 * <p>
	 * Specifies the format to use for URL GET parameter keys and values.
	 *
	 * <ul class='values'>
	 * 	<li><js>"UON"</js> (default) - Use UON notation for parameters.
	 * 	<li><js>"PLAINTEXT"</js> - Use plain text for parameters.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.uon.UonSerializer.Builder#paramFormat(ParamFormat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String paramFormat() default "";
}
