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
package org.apache.juneau.junit.bct;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.juneau.junit.bct.Utils.*;
import static org.apache.juneau.junit.bct.NestedTokenizer.ParseState.*;

import java.util.*;

/**
 * Splits a nested comma-delimited string into a list of Token objects using a state machine parser.
 *
 * <p>This class parses complex nested structures with support for escaping and arbitrary nesting depth.
 * The parser uses a finite state machine to handle different contexts during parsing.</p>
 *
 * <h5 class='section'>Supported Syntax:</h5>
 * <ul>
 *    <li><js>"foo"</js> - Single value token</li>
 *    <li><js>"foo,bar"</js> - Multiple value tokens</li>
 *    <li><js>"foo{a,b},bar"</js> - Token with nested values</li>
 *    <li><js>"foo{a{a1,a2}},bar"</js> - Recursively nested values</li>
 *    <li><js>"foo\\,bar"</js> - Escaped comma in value</li>
 *    <li><js>"foo\\{bar\\}"</js> - Escaped braces in value</li>
 * </ul>
 *
 * <h5 class='section'>State Machine:</h5>
 * <p>The parser operates in several states:</p>
 * <ul>
 *    <li><b>PARSING_VALUE:</b> Reading a token value</li>
 *    <li><b>PARSING_NESTED:</b> Reading nested content within braces</li>
 *    <li><b>IN_ESCAPE:</b> Processing escaped character</li>
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 * <p class='bjava'>
 *    <jc>// Simple tokens</jc>
 *    <jk>var</jk> <jv>tokens</jv> = NestedTokenizer.<jsm>tokenize</jsm>(<js>"foo,bar,baz"</js>);
 *    <jc>// tokens = [Token{value="foo"}, Token{value="bar"}, Token{value="baz"}]</jc>
 *
 *    <jc>// Nested tokens</jc>
 *    <jk>var</jk> <jv>nested</jv> = NestedTokenizer.<jsm>tokenize</jsm>(<js>"user{name,email},config{timeout,retries}"</js>);
 *    <jc>// nested[0] = Token{value="user", nested=[Token{value="name"}, Token{value="email"}]}</jc>
 *    <jc>// nested[1] = Token{value="config", nested=[Token{value="timeout"}, Token{value="retries"}]}</jc>
 * </p>
 */
class NestedTokenizer {

	/**
	 * Represents a parsed token with optional nested sub-tokens.
	 *
	 * <p>A Token contains a string value and may have nested tokens representing
	 * the content inside braces. Tokens support deep nesting for complex hierarchical structures.</p>
	 *
	 * <h5 class='section'>Structure:</h5>
	 * <ul>
	 *    <li><b>value:</b> The main token value (part before any braces)</li>
	 *    <li><b>nested:</b> Optional list of nested tokens (content within braces)</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 *    <li><js>"foo"</js> → <js>Token{value="foo", nested=null}</js></li>
	 *    <li><js>"foo{a,b}"</js> → <js>Token{value="foo", nested=[Token{value="a"}, Token{value="b"}]}</js></li>
	 * </ul>
	 */
	public static class Token {

		/** The main value of this token */
		private final String value;

		/** Nested tokens if this token has braced content, null otherwise */
		private List<Token> nested;

		/**
		 * Creates a new token with the specified value.
		 *
		 * @param value The token value
		 */
		public Token(String value) {
			this.value = value != null ? value : "";
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof Token o2) && eq(this, o2, (x, y) -> eq(x.value, y.value) && eq(x.nested, y.nested));
		}

		/**
		 * Returns an unmodifiable view of the nested tokens.
		 *
		 * @return unmodifiable list of nested tokens, or empty list if none
		 */
		public List<Token> getNested() { return nested != null ? unmodifiableList(nested) : emptyList(); }

		/**
		 * Returns the main value of this token.
		 *
		 * @return The token value
		 */
		public String getValue() { return value; }

		@Override
		public int hashCode() {
			return Objects.hash(value, nested);
		}

		/**
		 * Returns true if this token has nested content.
		 *
		 * @return true if nested tokens exist
		 */
		public boolean hasNested() {
			return nested != null && !nested.isEmpty();
		}

		@Override
		public String toString() {
			return hasNested() ? nested.stream().map(Object::toString).collect(joining(",", value + "{", "}")) : value;
		}

		/**
		 * Sets the nested tokens for this token (package-private for tokenizer use).
		 *
		 * @param nested The list of nested tokens
		 */
		void setNested(List<Token> nested) { this.nested = nested; }
	}

	/**
	 * Parser states for the finite state machine.
	 */
	enum ParseState {
		/** Parsing a token value outside of nested braces */
		PARSING_VALUE,
		/** Parsing nested content within braces */
		PARSING_NESTED,
		/** Processing an escaped character */
		IN_ESCAPE
	}

	public static List<Token> tokenize(String in) {
		if (in == null)
			throw new IllegalArgumentException("Input was null.");
		if (in.isBlank())
			throw new IllegalArgumentException("Input was empty.");

		var length = in.length();
		var pos = 0;
		var state = PARSING_VALUE;
		var currentValue = new StringBuilder();
		var nestedDepth = 0;
		var nestedStart = -1;
		var tokens = new ArrayList<Token>();
		var lastWasComma = false;
		var justCompletedNested = false;

		while (pos < length) {
			var c = in.charAt(pos);

			if (state == PARSING_VALUE) {
				if (c == '\\') {
					state = IN_ESCAPE;
				} else if (c == ',') {
					var value = currentValue.toString().trim();
					// Add token unless it's empty and we just completed a nested token
					if (!value.isEmpty() || tokens.isEmpty() || !justCompletedNested) {
						tokens.add(new Token(value));
					}
					currentValue.setLength(0);
					nestedStart = -1;
					lastWasComma = true;
					justCompletedNested = false;
					pos = skipWhitespace(in, pos);
				} else if (c == '{') {
					nestedStart = pos + 1;
					nestedDepth = 1;
					state = PARSING_NESTED;
				} else {
					currentValue.append(c);
					lastWasComma = false;
					justCompletedNested = false;
				}
			} else if (state == PARSING_NESTED) {
				if (c == '\\') {
					state = IN_ESCAPE;
				} else if (c == '{') {
					nestedDepth++;
				} else if (c == '}') {
					nestedDepth--;
					if (nestedDepth == 0) {
						var value = currentValue.toString().trim();
						var nestedContent = in.substring(nestedStart, pos);
						var token = new Token(value);
						if (!nestedContent.trim().isEmpty()) {
							token.setNested(tokenize(nestedContent));
						}
						tokens.add(token);
						currentValue.setLength(0);
						nestedStart = -1;
						lastWasComma = false; // Reset since we've completed a token
						justCompletedNested = true; // Flag that we just completed a nested token
						pos = skipWhitespace(in, pos);
						state = PARSING_VALUE;
					}
				}
			} else /* (state == IN_ESCAPE) */ {
				// Add the escaped character to current value only if we're parsing the main token value
				if (nestedDepth == 0) {
					currentValue.append(c);
				}
				state = (nestedDepth > 0) ? PARSING_NESTED : PARSING_VALUE;
			}

			pos++;
		}

		// Add final token if we have content, or if input ended with comma, or if no tokens yet
		var finalValue = currentValue.toString().trim();
		if (!finalValue.isEmpty() || lastWasComma || tokens.isEmpty()) {
			tokens.add(new Token(finalValue));
		}

		return tokens;
	}

	private static int skipWhitespace(String input, int position) {
		var length = input.length();
		while (position + 1 < length && Character.isWhitespace(input.charAt(position + 1))) {
			position++;
		}
		return position;
	}
}