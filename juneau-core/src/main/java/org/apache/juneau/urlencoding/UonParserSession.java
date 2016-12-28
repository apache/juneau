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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.urlencoding.UonParserContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Session object that lives for the duration of a single use of {@link UonParser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class UonParserSession extends ParserSession {

	private final boolean decodeChars, whitespaceAware;
	private UonReader reader;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param input The input.  Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text.
	 * 		<li>{@link File} containing system encoded text.
	 * 	</ul>
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 * @param locale The session locale.
	 * 	If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * 	If <jk>null</jk>, then the timezone defined on the context is used.
	 */
	public UonParserSession(UonParserContext ctx, ObjectMap op, Object input, Method javaMethod, Object outer, Locale locale, TimeZone timeZone) {
		super(ctx, op, input, javaMethod, outer, locale, timeZone);
		if (op == null || op.isEmpty()) {
			decodeChars = ctx.decodeChars;
			whitespaceAware = ctx.whitespaceAware;
		} else {
			decodeChars = op.getBoolean(UON_decodeChars, ctx.decodeChars);
			whitespaceAware = op.getBoolean(UON_whitespaceAware, ctx.whitespaceAware);
		}
	}

	/**
	 * Create a specialized parser session for parsing URL parameters.
	 * <p>
	 * The main difference is that characters are never decoded, and the {@link UonParserContext#UON_decodeChars} property is always ignored.
	 *
	 * @param ctx The context to copy setting from.
	 * @param input The input.  Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence} (e.g. {@link String})
	 * 		<li>{@link InputStream} - Read as UTF-8 encoded character stream.
	 * 		<li>{@link File} - Read as system-default encoded stream.
	 * 	</ul>
	 */
	public UonParserSession(UonParserContext ctx, Object input) {
		super(ctx, null, input, null, null, null, null);
		decodeChars = false;
		whitespaceAware = ctx.whitespaceAware;
	}

	/**
	 * Returns the {@link UonParserContext#UON_decodeChars} setting value for this session.
	 *
	 * @return The {@link UonParserContext#UON_decodeChars} setting value for this session.
	 */
	public final boolean isDecodeChars() {
		return decodeChars;
	}

	/**
	 * Returns the {@link UonParserContext#UON_whitespaceAware} setting value for this session.
	 *
	 * @return The {@link UonParserContext#UON_whitespaceAware} setting value for this session.
	 */
	public final boolean isWhitespaceAware() {
		return whitespaceAware;
	}

	@Override /* ParserSession */
	public UonReader getReader() throws Exception {
		if (reader == null) {
			Object input = getInput();
			if (input instanceof UonReader)
				reader = (UonReader)input;
			else if (input instanceof CharSequence)
				reader = new UonReader((CharSequence)input, decodeChars);
			else
				reader = new UonReader(super.getReader(), decodeChars);
		}
		return reader;
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
