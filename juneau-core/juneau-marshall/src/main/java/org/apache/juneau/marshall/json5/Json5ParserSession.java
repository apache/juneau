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
package org.apache.juneau.marshall.json5;

import java.io.*;

import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link Json5Parser}.
 *
 * <p>
 * Extends {@link JsonParserSession} to support JSON5 syntax including unquoted attribute names,
 * single-quoted strings, string concatenation, JavaScript comments, and relaxed number formats.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonSupport">JSON Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S125",  // State-machine comments are documentation, not commented-out code
	"java:S3776", // Cognitive complexity acceptable for parser state machine
	"java:S135",  // Multiple break statements necessary for state machine error handling
	"java:S110",  // Inheritance depth is intentional to reuse parser-session behavior layers
	"resource" // ParserReader is managed by caller
})
public class Json5ParserSession extends JsonParserSession {

	private static final AsciiSet VALID_BARE_CHARS = AsciiSet.create().range('A', 'Z').range('a', 'z').range('0', '9').chars("$_-.").build();

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth is intentional across parser session builders
	})
	public static class Builder extends JsonParserSession.Builder<Builder> {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(Json5Parser ctx) {
			super(ctx);
		}

		@Override
		public Json5ParserSession build() {
			return new Json5ParserSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(Json5Parser ctx) {
		return new Builder(ctx);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected Json5ParserSession(Builder builder) {
		super(builder);
	}

	/**
	 * Opens a low-level pull-parser cursor over a JSON5 document, bound to this live session.
	 *
	 * <p>
	 * Same honored/ignored builder properties as {@link JsonParserSession#parseTokens(Object)};
	 * the JSON5 dialect relaxations (single-quoted strings, bare identifiers, trailing commas,
	 * missing values) are applied unconditionally as part of the format.
	 *
	 * @param input The input.  Accepts {@link Reader}, {@link CharSequence}, {@link InputStream},
	 * 	or <code><jk>byte</jk>[]</code>.
	 * @return A new {@link Json5TokenReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@SuppressWarnings({
		"java:S2095" // ParserPipe lifecycle is transferred to the returned Json5TokenReader, which closes it via its own close(); the caller owns the cursor via try-with-resources.
	})
	@Override /* JsonParserSession */
	public TokenReader parseTokens(Object input) throws IOException {
		var pipe = new ParserPipe(
			input,
			isDebug(),
			false /* strict */,
			isAutoCloseStreams(),
			isUnbuffered(),
			getStreamCharset());
		return new Json5TokenReader(pipe, new JsonTokenReader.Settings(isTrimStrings()), this);
	}

	@Override
	protected String parseFieldName(ParserReader r) throws IOException, ParseException {
		int c = r.peek();
		if (c == '\'' || c == '"')
			return parseString(r);
		if (! VALID_BARE_CHARS.contains(c))
			throw new ParseException(this, "Could not find the start of the field name.");
		r.mark();
		while (c != -1) {
			c = r.read();
			if (! VALID_BARE_CHARS.contains(c)) {
				r.unread();
				var s = r.getMarked().intern();
				return s.equals("null") ? null : s;
			}
		}
		throw new ParseException(this, "Could not find the end of the field name.");
	}

	@Override
	protected String parseString(ParserReader r) throws IOException, ParseException {
		r.mark();
		int qc = r.read();
		final boolean isQuoted = (qc == '\'' || qc == '"');
		String s = null;
		boolean isInEscape = false;
		int c = 0;
		while (c != -1) {
			c = r.read();
			if (isInEscape) {
				// @formatter:off
				switch (c) {
					case 'n': r.replace('\n'); break;
					case 'r': r.replace('\r'); break;
					case 't': r.replace('\t'); break;
					case 'f': r.replace('\f'); break;
					case 'b': r.replace('\b'); break;
					case '\\': r.replace('\\'); break;
					case '/': r.replace('/'); break;
					case '\'': r.replace('\''); break;
					case '"': r.replace('"'); break;
					case 'u': {
						String n = r.read(4);
						try {
							r.replace(Integer.parseInt(n, 16), 6);
						} catch (@SuppressWarnings("unused") NumberFormatException e) {
							throw new ParseException(this, "Invalid Unicode escape sequence in string.");
						}
						break;
					}
					default:
						throw new ParseException(this, "Invalid escape sequence in string.");
				}
				// @formatter:on
				isInEscape = false;
			} else {
				if (c == '\\') {
					isInEscape = true;
					r.delete();
				} else if (isQuoted) {
					if (c == qc) {
						s = r.getMarked(1, -1);
						break;
					}
				} else {
					if (c == ',' || c == '}' || c == ']' || isWhitespace(c) || c == -1) {
						s = r.getMarked(0, c == -1 ? 0 : -1);
						if (c != -1)
							r.unread();
						break;
					}
				}
			}
		}
		if (s == null)
			throw new ParseException(this, "Could not find expected end character '%s'.", (char)qc);

		skipCommentsAndSpace(r);
		if (r.peek() == '+') {
			@SuppressWarnings({
				"unused" // Intentionally unused; variable/parameter is required by the interface contract
			})
			int ignored = r.read();
			skipCommentsAndSpace(r);
			s += parseString(r);
		}
		return trim(s);
	}

	@Override
	protected Number parseNumber(ParserReader r, String s, Class<? extends Number> type) throws ParseException {
		return StringUtils.parseNumber(s, type);
	}

	@Override
	protected void skipCommentsAndSpace(ParserReader r) throws IOException, ParseException {
		int c = 0;
		while ((c = r.read()) != -1) {
			if (! isWhitespace(c)) {
				if (c == '/') {
					skipComments(r);
				} else {
					r.unread();
					return;
				}
			}
		}
	}

	@Override
	protected boolean isCommentOrWhitespace(int cp) {
		if (cp == '/')
			return true;
		return Character.isWhitespace(cp);
	}

	@Override
	protected boolean isWhitespace(int cp) {
		return Character.isWhitespace(cp);
	}

	@Override
	protected void onEmptyInput() {
		// JSON5 allows empty input, returning null.
	}

	@Override
	protected void onMissingValue() {
		// JSON5 allows missing values, returning null.
	}

	@Override
	protected boolean canCoerceNonStringToString() {
		return true;
	}

	@SuppressWarnings({
		"java:S6466" // False positive: new Json5Map(this) resolves to Json5Map(MarshallingSession), not Json5Map(Object...), since Json5ParserSession IS-A MarshallingSession.
	})
	@Override /* Overridden from JsonParserSession */
	protected MarshalledMap newGenericMap() {
		return new Json5Map(this);
	}

	@Override /* Overridden from JsonParserSession */
	protected MarshalledList newGenericList() {
		return new Json5List(this);
	}
}
