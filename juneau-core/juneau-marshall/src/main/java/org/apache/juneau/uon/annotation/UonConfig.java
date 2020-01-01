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
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(UonConfigApply.class)
public @interface UonConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// UonCommon
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Indirectly applies {@link Uon @Uon} annotations to classes/methods.
	 *
	 * <p>
	 * Provides an alternate approach for applying annotations to classes/methods annotations using the {@link Uon#on() @Uon.on}
	 * annotation to specify the class/method names to apply the annotation to.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.ClassMethodAnnotations}
	 * </ul>
	 */
	Uon[] annotateUon() default {};

	//-------------------------------------------------------------------------------------------------------------------
	// UonParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property: Decode <js>"%xx"</js> sequences.
	 *
	 * <p>
	 * Specify <js>"true"</js> if URI encoded characters should be decoded, <js>"false"</js> if they've already been decoded
	 * before being passed to this parser.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js>
	 * 		</ul>
	 * 	<li>
	 * 		The default is <js>"false"</js> for {@link UonParser}, <js>"true"</js> for {@link UrlEncodingParser}.
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"UonParser.decoding.b"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonParser#UON_decoding}
	 * </ul>
	 */
	String decoding() default "";

	/**
	 * Configuration property:  Validate end.
	 *
	 * <p>
	 * If <js>"true"</js>, after parsing a POJO from the input, verifies that the remaining input in
	 * the stream consists of only comments or whitespace.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"UonParser.validateEnd.b"</js>.
	 * </ul>

	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonParser#UON_validateEnd}
	 * </ul>
	 */
	String validateEnd() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// UonSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <js>"true"</js>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link Serializer#SERIALIZER_addBeanTypes} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"UonSerializer.addBeanTypes.b"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonSerializer#UON_addBeanTypes}
	 * </ul>
	 */
	String addBeanTypes() default "";

	/**
	 * Configuration property:  Encode non-valid URI characters.
	 *
	 * <p>
	 * Encode non-valid URI characters with <js>"%xx"</js> constructs.
	 *
	 * <p>
	 * If <js>"true"</js>, non-valid URI characters will be converted to <js>"%xx"</js> sequences.
	 * <br>Set to <js>"false"</js> if parameter value is being passed to some other code that will already perform
	 * URL-encoding of non-valid URI characters.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js>
	 * 		</ul>
	 * 	<li>
	 * 		The default is <js>"false"</js> for {@link UonSerializer}, <js>"true"</js> for {@link UrlEncodingSerializer}.
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"UonSerializer.encoding.b"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonSerializer#UON_encoding}
	 * </ul>
	 */
	String encoding() default "";

	/**
	 * Configuration property:  Format to use for query/form-data/header values.
	 *
	 * <p>
	 * Specifies the format to use for URL GET parameter keys and values.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"UON"</js> (default) - Use UON notation for parameters.
	 * 			<li><js>"PLAINTEXT"</js> - Use plain text for parameters.
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"UonSerializer.paramFormat.s"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
	 * </ul>
	 */
	String paramFormat() default "";
}
