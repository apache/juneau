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
package org.apache.juneau.urlencoding.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.urlencoding.*;

/**
 * Annotation for specifying config properties defined in {@link UrlEncodingSerializer} and {@link UrlEncodingParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.UrlEncodingDetails">URL-Encoding Details</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({UrlEncodingConfigAnnotation.SerializerApply.class,UrlEncodingConfigAnnotation.ParserApply.class})
public @interface UrlEncodingConfig {

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
	// UrlEncodingCommon
	//-------------------------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------------------------
	// UrlEncodingSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Parser bean property collections/arrays as separate key/value pairs.
	 *
	 * <p>
	 * This is the parser-side equivalent of the {@link org.apache.juneau.urlencoding.UrlEncodingSerializer.Builder#expandedParams()} setting.
	 *
	 * <p>
	 * If <js>"false"</js>, serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * <br>If <js>"true"</js>, serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='warn'>
	 * 		If parsing multi-part parameters, it's highly recommended to use Collections or Lists
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * 	<li class='note'>
	 * 		This option only applies to beans.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.urlencoding.UrlEncodingSerializer.Builder#expandedParams()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String expandedParams() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// UrlEncodingParser
	//-------------------------------------------------------------------------------------------------------------------
}
