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
package org.apache.juneau.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.parser.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonParser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public final class JsonParserSession extends ParserSession {

	private ParserReader reader;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * The context contains all the configuration settings for this object.
	 * @param input The input.  Can be any of the following types:
	 * <ul>
	 * 	<li><jk>null</jk>
	 * 	<li>{@link Reader}
	 * 	<li>{@link CharSequence}
	 * 	<li>{@link InputStream} containing UTF-8 encoded text.
	 * 	<li>{@link File} containing system encoded text.
	 * </ul>
	 * @param op The override properties.
	 * These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 * @param locale The session locale.
	 * If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 */
	public JsonParserSession(JsonParserContext ctx, ObjectMap op, Object input, Method javaMethod, Object outer,
			Locale locale, TimeZone timeZone, MediaType mediaType) {
		super(ctx, op, input, javaMethod, outer, locale, timeZone, mediaType);
	}

	@Override /* ParserSession */
	public ParserReader getReader() throws Exception {
		if (reader == null) {
			Object input = getInput();
			if (input == null)
				return null;
			if (input instanceof CharSequence)
				reader = new ParserReader((CharSequence)input);
			else
				reader = new ParserReader(super.getReader());
		}
		return reader;
	}

	/**
	 * Returns <jk>true</jk> if the specified character is whitespace.
	 * <p>
	 * The definition of whitespace is different for strict vs lax mode.
	 * Strict mode only interprets 0x20 (space), 0x09 (tab), 0x0A (line feed) and 0x0D (carriage return) as whitespace.
	 * Lax mode uses {@link Character#isWhitespace(int)} to make the determination.
	 *
	 * @param cp The codepoint.
	 * @return <jk>true</jk> if the specified character is whitespace.
	 */
	public final boolean isWhitespace(int cp) {
		if (isStrict())
				return cp <= 0x20 && (cp == 0x09 || cp == 0x0A || cp == 0x0D || cp == 0x20);
		return Character.isWhitespace(cp);
	}

	/**
	 * Returns <jk>true</jk> if the specified character is whitespace or '/'.
	 *
	 * @param cp The codepoint.
	 * @return <jk>true</jk> if the specified character is whitespace or '/'.
	 */
	public final boolean isCommentOrWhitespace(int cp) {
		if (cp == '/')
			return true;
		if (isStrict())
			return cp <= 0x20 && (cp == 0x09 || cp == 0x0A || cp == 0x0D || cp == 0x20);
	return Character.isWhitespace(cp);
	}

	@Override /* ParserSession */
	public Map<String,Object> getLastLocation() {
		Map<String,Object> m = super.getLastLocation();
		if (reader != null) {
			m.put("line", reader.getLine());
			m.put("column", reader.getColumn());
		}
		return m;
	}
}
