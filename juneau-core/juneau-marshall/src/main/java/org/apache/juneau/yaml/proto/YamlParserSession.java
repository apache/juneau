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
package org.apache.juneau.yaml.proto;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * @deprecated Never implemented.
 */
@Deprecated
public final class YamlParserSession extends ReaderParserSession {

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected YamlParserSession(YamlParser ctx, ParserSessionArgs args) {
		super(ctx, args);
	}

	/**
	 * Returns <jk>true</jk> if the specified character is whitespace.
	 *
	 * <p>
	 * The definition of whitespace is different for strict vs lax mode.
	 * Strict mode only interprets 0x20 (space), 0x09 (tab), 0x0A (line feed) and 0x0D (carriage return) as whitespace.
	 * Lax mode uses {@link Character#isWhitespace(int)} to make the determination.
	 *
	 * @param cp The codepoint.
	 * @return <jk>true</jk> if the specified character is whitespace.
	 */
	protected final boolean isWhitespace(int cp) {
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
	protected final boolean isCommentOrWhitespace(int cp) {
		if (cp == '/')
			return true;
		if (isStrict())
			return cp <= 0x20 && (cp == 0x09 || cp == 0x0A || cp == 0x0D || cp == 0x20);
		return Character.isWhitespace(cp);
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
		return null;
	}

	@Override /* ReaderParserSession */
	protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		return null;
	}

	@Override /* ReaderParserSession */
	protected <E> Collection<E> doParseIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType) throws Exception {
		return null;
	}
}
