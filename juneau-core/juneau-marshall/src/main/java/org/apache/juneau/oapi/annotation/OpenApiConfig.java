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
package org.apache.juneau.oapi.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.msgpack.*;

/**
 * Annotation for specifying config properties defined in {@link MsgPackSerializer} and {@link MsgPackParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.OpenApiDetails">OpenAPI Details</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({OpenApiConfigAnnotation.SerializerApply.class,OpenApiConfigAnnotation.ParserApply.class})
public @interface OpenApiConfig {

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
	// OpenApiCommon
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Default format for HTTP parts.
	 *
	 * <p>
	 * Specifies the format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.annotation.Schema#format()}.
	 *
	 * <ul class='values javatree'>
	 * 	<li class='jc'>{@link HttpPartFormat}
	 * 	<ul>
	 * 		<li class='jf'>{@link HttpPartFormat#UON UON} - UON notation (e.g. <js>"'foo bar'"</js>).
	 * 		<li class='jf'>{@link HttpPartFormat#INT32 INT32} - Signed 32 bits.
	 * 		<li class='jf'>{@link HttpPartFormat#INT64 INT64} - Signed 64 bits.
	 * 		<li class='jf'>{@link HttpPartFormat#FLOAT FLOAT} - 32-bit floating point number.
	 * 		<li class='jf'>{@link HttpPartFormat#DOUBLE DOUBLE} - 64-bit floating point number.
	 * 		<li class='jf'>{@link HttpPartFormat#BYTE BYTE} - BASE-64 encoded characters.
	 * 		<li class='jf'>{@link HttpPartFormat#BINARY BINARY} - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
	 * 		<li class='jf'>{@link HttpPartFormat#BINARY_SPACED BINARY_SPACED} - Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
	 * 		<li class='jf'>{@link HttpPartFormat#DATE DATE} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
	 * 		<li class='jf'>{@link HttpPartFormat#DATE_TIME DATE_TIME} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
	 * 		<li class='jf'>{@link HttpPartFormat#PASSWORD PASSWORD} - Used to hint UIs the input needs to be obscured.
	 * 		<li class='jf'>{@link HttpPartFormat#NO_FORMAT NO_FORMAT} - (default) Not specified.
	 * 	</ul>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.oapi.OpenApiSerializer.Builder#format(HttpPartFormat)}
	 * 	<li class='jm'>{@link org.apache.juneau.oapi.OpenApiParser.Builder#format(HttpPartFormat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String format() default "";

	/**
	 * Default collection format for HTTP parts.
	 *
	 * <p>
	 * Specifies the collection format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.annotation.Schema#collectionFormat()}.
	 *
	 * <ul class='values javatree'>
	 * 	<li class='jc'>{@link HttpPartCollectionFormat}
	 * 	<ul>
	 * 		<li class='jf'>{@link HttpPartCollectionFormat#CSV CSV} - (default) Comma-separated values (e.g. <js>"foo,bar"</js>).
	 * 		<li class='jf'>{@link HttpPartCollectionFormat#SSV SSV} - Space-separated values (e.g. <js>"foo bar"</js>).
	 * 		<li class='jf'>{@link HttpPartCollectionFormat#TSV TSV} - Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 * 		<li class='jf'>{@link HttpPartCollectionFormat#PIPES PIPES} - Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 * 		<li class='jf'>{@link HttpPartCollectionFormat#MULTI MULTI} - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 * 		<li class='jf'>{@link HttpPartCollectionFormat#UONC UONC} - UON collection notation (e.g. <js>"@(foo,bar)"</js>).
	 * 	</ul>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.oapi.OpenApiSerializer.Builder#collectionFormat(HttpPartCollectionFormat)}
	 * 	<li class='jm'>{@link org.apache.juneau.oapi.OpenApiParser.Builder#collectionFormat(HttpPartCollectionFormat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String collectionFormat() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// OpenApiSerializer
	//-------------------------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------------------------
	// OpenApiParser
	//-------------------------------------------------------------------------------------------------------------------
}
