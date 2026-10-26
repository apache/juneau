/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.oapi;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.msgpack.*;

/**
 * Annotation for specifying config properties defined in {@link MsgPackSerializer} and {@link MsgPackParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/OpenApiSupport">OpenApi</a>
 * </ul>
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@ContextApply({ OpenApiConfigAnnotation.SerializerApply.class, OpenApiConfigAnnotation.ParserApply.class })
public @interface OpenApiConfig {

	/**
	 * Default collection format for HTTP parts.
	 *
	 * <p>
	 * Specifies the collection format to use for HTTP parts when not otherwise specified via {@code Schema.collectionFormat()}.
	 *
	 * <ul class='values javatree'>
	 * 	<li class='jc'>{@code HttpPartCollectionFormat}
	 * 	<ul>
	 * 		<li class='jf'>{@code CSV} - (default) Comma-separated values (e.g. <js>"foo,bar"</js>).
	 * 		<li class='jf'>{@code SSV} - Space-separated values (e.g. <js>"foo bar"</js>).
	 * 		<li class='jf'>{@code TSV} - Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 * 		<li class='jf'>{@code PIPES} - Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 * 		<li class='jf'>{@code MULTI} - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 * 		<li class='jf'>{@code UONC} - UON collection notation (e.g. <js>"@(foo,bar)"</js>).
	 * 	</ul>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link OpenApiSerializer.Builder#collectionFormat(HttpPartCollectionFormat)}
	 * 	<li class='jm'>{@link OpenApiParser.Builder#collectionFormat(HttpPartCollectionFormat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String collectionFormat() default "";

	/**
	 * Default format for HTTP parts.
	 *
	 * <p>
	 * Specifies the format to use for HTTP parts when not otherwise specified via {@code Schema.format()}.
	 *
	 * <ul class='values javatree'>
	 * 	<li class='jc'>{@code HttpPartFormat}
	 * 	<ul>
	 * 		<li class='jf'>{@code UON} - UON notation (e.g. <js>"'foo bar'"</js>).
	 * 		<li class='jf'>{@code INT32} - Signed 32 bits.
	 * 		<li class='jf'>{@code INT64} - Signed 64 bits.
	 * 		<li class='jf'>{@code FLOAT} - 32-bit floating point number.
	 * 		<li class='jf'>{@code DOUBLE} - 64-bit floating point number.
	 * 		<li class='jf'>{@code BYTE} - BASE-64 encoded characters.
	 * 		<li class='jf'>{@code BINARY} - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
	 * 		<li class='jf'>{@code BINARY_SPACED} - Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
	 * 		<li class='jf'>{@code DATE} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
	 * 		<li class='jf'>{@code DATE_TIME} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
	 * 		<li class='jf'>{@code PASSWORD} - Used to hint UIs the input needs to be obscured.
	 * 		<li class='jf'>{@code NO_FORMAT} - (default) Not specified.
	 * 	</ul>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link OpenApiSerializer.Builder#format(HttpPartFormat)}
	 * 	<li class='jm'>{@link OpenApiParser.Builder#format(HttpPartFormat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String format() default "";

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;
}