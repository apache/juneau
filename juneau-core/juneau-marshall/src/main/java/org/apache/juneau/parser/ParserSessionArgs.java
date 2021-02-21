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
package org.apache.juneau.parser;

import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;

/**
 * Runtime arguments common to all parser sessions.
 */
public final class ParserSessionArgs extends BeanSessionArgs {

	Method javaMethod;
	Object outer;

	/**
	 * Default parser session args.
	 */
	public static final ParserSessionArgs DEFAULT = new ParserSessionArgs();

	/**
	 * Creator.
	 *
	 * @return A new parser session arguments object.
	 */
	public static final ParserSessionArgs create() {
		return new ParserSessionArgs();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * File charset.
	 *
	 * <p>
	 * The character set to use for reading Files from the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Parser#parse(Object, Class)}.
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
	public ParserSessionArgs fileCharset(Charset value) {
		property(ReaderParser.RPARSER_fileCharset, value);
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
	public ParserSessionArgs javaMethod(Method value) {
		this.javaMethod = value;
		return this;
	}

	/**
	 * The outer object for instantiating top-level non-static inner classes.
	 *
	 * @param value
	 * 	The new property value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ParserSessionArgs outer(Object value) {
		this.outer = value;
		return this;
	}

	/**
	 * Input stream charset.
	 *
	 * <p>
	 * The character set to use for converting InputStreams and byte arrays to readers.
	 *
	 * <p>
	 * Used when passing in input streams and byte arrays to {@link Parser#parse(Object, Class)}.
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
	public ParserSessionArgs streamCharset(Charset value) {
		property(ReaderParser.RPARSER_streamCharset, value);
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - SessionArgs */
	public ParserSessionArgs debug(Boolean value) {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public ParserSessionArgs locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public ParserSessionArgs mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public ParserSessionArgs properties(Map<String,Object> value) {
		super.properties(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public ParserSessionArgs property(String key, Object value) {
		super.property(key, value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public ParserSessionArgs timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanSessionArgs */
	public ParserSessionArgs schema(HttpPartSchema value) {
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
				"ParserSessionArgs",
				OMap
					.create()
					.filtered()
					.a("javaMethod", javaMethod)
					.a("outer", outer)
			);
	}
}