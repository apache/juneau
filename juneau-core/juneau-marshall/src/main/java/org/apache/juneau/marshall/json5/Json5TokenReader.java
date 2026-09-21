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

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenReader} surface for the JSON5 format.
 *
 * <p>
 * Subclasses {@link JsonTokenReader} to add the JSON5 dialect's relaxations:
 * <ul>
 * 	<li>Strings can be single-quoted, double-quoted, or unquoted bare identifiers.
 * 	<li>Field names can be single-quoted, double-quoted, or unquoted bare identifiers.
 * 	<li>Trailing commas before <c>]</c> / <c>}</c> are allowed.
 * 	<li>Missing values in arrays / objects emit {@link TokenType#VALUE_NULL}.
 * 			<li>JavaScript comments (<c>//</c> line, <c>/* &#42;/</c> block) are tolerated &mdash; already
 * 		accepted by the parent class.
 * 	<li>JSON5 numbers: leading <c>+</c>, leading/trailing decimal points, hexadecimal, {@code Infinity}/{@code NaN}.
 * 	<li>String line-continuation (backslash + line terminator is omitted from the value).
 * </ul>
 *
 * <p>
 * The cursor is a true O(1)-memory streaming cursor ({@link #isStreaming()} == <jk>true</jk>).
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>The cursor is purely structural; object swaps and {@code @Schema} annotations have no
 * 		effect.  Use the POJO databind path on {@link Json5Parser} for those.
 * </ul>
 */
@SuppressWarnings({
	"resource" // The cursor's underlying ParserPipe is owned by the caller via try-with-resources on the cursor itself; Eclipse JDT flags the inner pipe as unclosed but that's by design.
})
public class Json5TokenReader extends JsonTokenReader {

	/**
	 * Constructor with default settings.
	 *
	 * @param pipe The parser input pipe to read from.  Must not be <jk>null</jk>.
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public Json5TokenReader(ParserPipe pipe) throws IOException {
		super(pipe);
	}

	/**
	 * Constructor used by {@link Json5ParserSession#readTokens(Object)} to plumb the calling
	 * session through so that {@link #read(Class)} can delegate to the JSON5 databind path.
	 *
	 * @param pipe The parser input pipe to read from.  Must not be <jk>null</jk>.
	 * @param settings The cursor-level settings.  Must not be <jk>null</jk>.
	 * @param session The {@link Json5ParserSession} for {@link #read(Class)} delegation, or
	 * 	<jk>null</jk> to disable {@code read}.
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public Json5TokenReader(ParserPipe pipe, Settings settings, Json5ParserSession session) throws IOException {
		super(pipe, settings, session);
	}

	// =================================================================================
	// JSON5 dialect relaxations
	// =================================================================================

	@Override /* JsonTokenReader */
	protected boolean allowsTrailingComma() {
		return true;
	}

	@Override /* JsonTokenReader */
	protected void readNumberValue() throws IOException, ParseException {
		var s = Json5ParserSession.readJson5NumberLexeme(r);
		if (s.isEmpty())
			throw parseException("Invalid JSON5 number: '%s'", s);
		currentNumberLexeme = s;
		currentToken = TokenType.VALUE_NUMBER;
	}

	@Override /* TokenReader */
	public Number getNumber() throws ParseException {
		if (currentToken != TokenType.VALUE_NUMBER)
			throw new IllegalStateException("Current token is not VALUE_NUMBER (was " + currentToken + ")");
		return Json5ParserSession.parseJson5Number(currentNumberLexeme, null);
	}

	@Override /* JsonTokenReader */
	protected void readValueStart(int c) throws IOException, ParseException {
		// Missing-value: a leading comma or container-end at value position emits VALUE_NULL and
		// re-injects the structural char so the next state transition picks it up.
		if (c == ',' || c == ']' || c == '}') {
			r.unread();
			currentToken = TokenType.VALUE_NULL;
			afterValue();
			return;
		}
		// Single-quoted strings (JSON5).
		if (c == '\'') {
			r.unread();
			currentString = maybeTrimString(readString());
			currentToken = TokenType.VALUE_STRING;
			afterValue();
			return;
		}
		// JSON5 numbers that JSON itself would not start: leading '+', leading '.', Infinity, NaN.
		if (c == '+' || c == '.') {
			r.unread();
			readNumberValue();
			afterValue();
			return;
		}
		// Bare identifier as a string value (e.g. `[foo, bar]` -> ["foo", "bar"]).
		if (isBareStart(c)) {
			r.unread();
			// Bare identifiers might also be the keyword literals true/false/null — distinguish.
			var word = readBareWord();
			switch (word) {
				case "true":  currentBoolean = true;  currentToken = TokenType.VALUE_BOOLEAN; afterValue(); return;
				case "false": currentBoolean = false; currentToken = TokenType.VALUE_BOOLEAN; afterValue(); return;
				case "null":  currentToken = TokenType.VALUE_NULL; afterValue(); return;
				case "Infinity":
				case "NaN":
					currentNumberLexeme = word;
					currentToken = TokenType.VALUE_NUMBER;
					afterValue();
					return;
				default:
					currentString = maybeTrimString(word);
					currentToken = TokenType.VALUE_STRING;
					afterValue();
					return;
			}
		}
		super.readValueStart(c);
	}

	@Override /* JsonTokenReader */
	protected void readFieldNameOrEndObject(int c) throws IOException, ParseException {
		if (c == '}') {
			popContainer();
			currentToken = TokenType.END_OBJECT;
			afterValue();
			return;
		}
		// Single-quoted field name.
		if (c == '\'') {
			r.unread();
			currentFieldName = maybeTrimString(readString());
			currentToken = TokenType.FIELD_NAME;
			state = S02_expectColon;
			return;
		}
		// Bare identifier field name.
		if (isBareStart(c)) {
			r.unread();
			currentFieldName = maybeTrimString(readBareWord());
			currentToken = TokenType.FIELD_NAME;
			state = S02_expectColon;
			return;
		}
		super.readFieldNameOrEndObject(c);
	}

	@Override /* JsonTokenReader */
	protected void readCommaOrEndArray(int c) throws IOException, ParseException {
		if (c == ']') {
			popContainer();
			currentToken = TokenType.END_ARRAY;
			afterValue();
			return;
		}
		if (c != ',')
			throw parseException("Expected , or ] but got '%s'", (char) c);
		var c2 = readSkipWsAndComments();
		if (c2 == -1)
			throw parseException("Unexpected end of input after ','");
		// JSON5 trailing comma: `[1,]` is legal — `]` here closes the array.
		if (c2 == ']') {
			popContainer();
			currentToken = TokenType.END_ARRAY;
			afterValue();
			return;
		}
		state = S00_expectValue;
		readValueStart(c2);
	}

	@Override /* JsonTokenReader */
	protected void readCommaOrEndObject(int c) throws IOException, ParseException {
		if (c == '}') {
			popContainer();
			currentToken = TokenType.END_OBJECT;
			afterValue();
			return;
		}
		if (c != ',')
			throw parseException("Expected , or } but got '%s'", (char) c);
		var c2 = readSkipWsAndComments();
		if (c2 == -1)
			throw parseException("Unexpected end of input after ','");
		// JSON5 trailing comma: `{a:1,}` is legal — `}` here closes the object.
		if (c2 == '}') {
			popContainer();
			currentToken = TokenType.END_OBJECT;
			afterValue();
			return;
		}
		state = S01_expectFieldName;
		readFieldNameOrEndObject(c2);
	}

	@Override /* JsonTokenReader */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for JSON5 quoted-string escape-handling state machine.
	})
	protected String readString() throws IOException, ParseException {
		// JSON5 strings may be single- or double-quoted, and a backslash before a line
		// terminator is a line-continuation (the terminator is omitted from the value).
		r.mark();
		var qc = r.read();
		if (qc != '"' && qc != '\'')
			throw parseException("Did not find quote character marking beginning of string");
		String s = null;
		var inEscape = false;
		var c = 0;
		while (c != -1) {
			c = r.read();
			if (inEscape) {
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
					case '\n':
						r.delete();
						break;
					case '\r':
						r.delete();
						if (r.peek() == '\n') {
							r.read();
							r.delete();
						}
						break;
					case 'u': {
						var hex = r.read(4);
						try {
							r.replace(Integer.parseInt(hex, 16), 6);
						} catch (@SuppressWarnings("unused") NumberFormatException e) {
							throw parseException("Invalid Unicode escape sequence in string");
						}
						break;
					}
					default:
						throw parseException("Invalid escape sequence in string");
				}
				inEscape = false;
			} else {
				if (c <= 0x1F && c != -1)
					throw parseException("Unescaped control character encountered: '0x%s'", String.format("%04X", c));
				if (c == '\\') {
					inEscape = true;
					r.delete();
				} else if (c == qc) {
					s = r.getMarked(1, -1);
					break;
				}
			}
		}
		if (s == null)
			throw parseException("Unterminated string");
		return s;
	}

	private String readBareWord() throws IOException, ParseException {
		// The leading char was already validated via isBareStart(); since every bare-start char is
		// also a valid bare-continuation char, the loop consumes it on the first iteration.
		r.mark();
		while (true) {
			var c = r.read();
			if (c == -1 || !isBareCont(c)) {
				if (c != -1) r.unread();
				return r.getMarked();
			}
		}
	}

	private static boolean isBareStart(int c) {
		return Character.isLetter(c) || c == '_' || c == '$';
	}

	private static boolean isBareCont(int c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '$';
	}

	// Local trim helper — the parent's maybeTrim is private and not visible to this subclass.
	private String maybeTrimString(String s) {
		if (s == null || !settings.trimStrings())
			return s;
		return s.trim();
	}
}
