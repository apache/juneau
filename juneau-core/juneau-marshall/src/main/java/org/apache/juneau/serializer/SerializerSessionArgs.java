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
package org.apache.juneau.serializer;

import static org.apache.juneau.serializer.Serializer.*;

import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * Runtime arguments common to all serializer sessions.
 *
 * <p>
 * This object specifies information such as session locale or URI context.
 */
@FluentSetters
public final class SerializerSessionArgs extends BeanSessionArgs {

	/**
	 * Default serializer session args.
	 */
	public static final SerializerSessionArgs DEFAULT = new SerializerSessionArgs();

	Method javaMethod;
	VarResolverSession resolver;
	Boolean useWhitespace;
	Charset fileCharset, streamCharset;

	/**
	 * Static creator.
	 *
	 * @return A new parser session arguments object.
	 */
	public static final SerializerSessionArgs create() {
		return new SerializerSessionArgs();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * File charset.
	 *
	 * <p>
	 * The character set to use for writing Files to the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Serializer#serialize(Object, Object)}.
	 *
	 * <p>
	 * If not specified, defaults to the JVM system default charset.
	 *
	 * @param value
	 * 	The new property value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerSessionArgs fileCharset(Charset value) {
		fileCharset = value;
		return this;
	}

	/**
	 * The java method that called this serializer, usually the method in a REST servlet.
	 *
	 * @param value
	 * 	The new property value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerSessionArgs javaMethod(Method value) {
		this.javaMethod = value;
		return this;
	}

	/**
	 * String variable resolver.
	 *
	 * <p>
	 * If not specified, defaults to session created by {@link VarResolver#DEFAULT}.
	 *
	 * @param value
	 * 	The new property value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerSessionArgs resolver(VarResolverSession value) {
		this.resolver = value;
		return this;
	}

	/**
	 * Output stream charset.
	 *
	 * <p>
	 * The character set to use when writing to OutputStreams.
	 *
	 * <p>
	 * Used when passing in output streams and byte arrays to {@link WriterSerializer#serialize(Object, Object)}.
	 *
	 * <p>
	 * If not specified, defaults to UTF-8.
	 *
	 * @param value
	 * 	The new property value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerSessionArgs streamCharset(Charset value) {
		streamCharset = value;
		return this;
	}

	/**
	 * URI context bean.
	 *
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <p>
	 * If not specified, defaults to {@link Serializer#SERIALIZER_uriContext}.
	 *
	 * @param value
	 * 	The new property value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerSessionArgs uriContext(UriContext value) {
		property(SERIALIZER_uriContext, value);
		return this;
	}

	/**
	 * Use whitespace.
	 *
	 * <p>
	 * If true, whitespace is added to the output to improve readability.
	 *
	 * @param value
	 * 	The new property value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializerSessionArgs useWhitespace(Boolean value) {
		useWhitespace = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - SessionArgs */
	public SerializerSessionArgs debug(Boolean value) {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public SerializerSessionArgs locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public SerializerSessionArgs mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public SerializerSessionArgs properties(Map<String,Object> value) {
		super.properties(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public SerializerSessionArgs property(String key, Object value) {
		super.property(key, value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public SerializerSessionArgs timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public SerializerSessionArgs unmodifiable() {
		super.unmodifiable();
		return this;
	}

	@Override /* GENERATED - BeanSessionArgs */
	public SerializerSessionArgs schema(HttpPartSchema value) {
		super.schema(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* SessionArgs */
	public OMap toMap() {
		return super.toMap()
			.a(
				"SerializerSessionArgs",
				OMap
					.create()
					.filtered()
					.a("javaMethod", javaMethod)
					.a("resolver", resolver)
			);
	}
}
