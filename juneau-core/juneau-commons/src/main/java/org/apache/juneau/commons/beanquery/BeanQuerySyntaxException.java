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
package org.apache.juneau.commons.beanquery;

import org.apache.juneau.commons.*;

/**
 * Thrown when a {@code $}-search expression names an unknown operator or is otherwise malformed (an unclosed
 * parenthesis, a wrong argument count, junk after the close paren, an empty expression, and so on), or when a
 * higher-level check (column allow-list, sort keys, paging, regex policy, operator/type mismatch, an unparseable
 * typed value) rejects a caller-supplied query.
 *
 * <p>
 * The single public parser ({@link SearchExpressionParser}) throws this for every syntactic problem; the #1/#2
 * validation pipeline throws it for every semantic one. Every instance carries a stable {@link Code}: messages are
 * for people, {@link #code()} is for callers (an HTTP error body, the JS engine, a test corpus) that need to branch
 * on *which* problem occurred without parsing the message.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> {
 * 		SearchExpressionParser.<jsm>parse</jsm>(<js>"$foo(1)"</js>, SearchOperatorSet.<jsm>standard</jsm>());
 * 	} <jk>catch</jk> (BeanQuerySyntaxException e) {
 * 		<jk>if</jk> (e.code() == BeanQuerySyntaxException.Code.<jsf>UNKNOWN_OPERATOR</jsf>)
 * 			log.warn(<js>"Unknown operator: {}"</js>, e.getMessage());
 * 	}
 * </p>
 *
 * @serial exclude
 * @since 10.0.0
 */
public class BeanQuerySyntaxException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Stable identity for a {@link BeanQuerySyntaxException}, independent of the (free-text) message.
	 *
	 * <p>
	 * Codes are part of the public API: renaming one is a breaking change. New codes may be added.
	 *
	 * @since 10.0.0
	 */
	public enum Code {

		/** The search string is empty or blank. */
		EMPTY_EXPRESSION,
		/** {@code $} + letter-led text before {@code (} that is not a valid operator name (e.g. {@code $eq!(x)}). */
		INVALID_OPERATOR_NAME,
		/** An operator name with no {@code (...)} call following it. */
		MALFORMED_OPERATOR,
		/** The operator name is well-formed but not registered in the operator set in use. */
		UNKNOWN_OPERATOR,
		/** The operator's argument count is outside its declared {@code minArgs}/{@code maxArgs}. */
		BAD_ARG_COUNT,
		/** A comma-separated argument slot is empty (a stray or trailing comma). */
		EMPTY_ARGUMENT,
		/** An operator call, or a #2 group, never closes. */
		UNBALANCED,
		/** Text follows the closing {@code )} of a top-level operator call. */
		TRAILING_TEXT,
		/** A quoted literal never finds its closing quote. */
		UNTERMINATED_QUOTE,
		/** A {@code $or}/{@code $and}/{@code $not} group has too few or too many items. */
		GROUP_ARITY,
		/** The same column appears more than once at the top level of a search string. */
		DUPLICATE_COLUMN,
		/** The raw search string exceeds the configured maximum length. */
		SEARCH_TOO_LONG,
		/** The raw options string exceeds the configured maximum length. */
		OPTS_TOO_LONG,
		/** The search string has more top-level clauses than the configured maximum. */
		TOO_MANY_CLAUSES,
		/** A search, sort, or view column is not in the allow-list. */
		UNKNOWN_COLUMN,
		/** An expression nests deeper than the configured maximum. */
		TOO_DEEP,
		/** {@code $regex} was used but regex search is not enabled for this list. */
		REGEX_DISABLED,
		/** A {@code $regex} pattern exceeds the configured maximum length. */
		REGEX_TOO_LONG,
		/** A {@code $regex} match exceeded its evaluation deadline. */
		REGEX_TIMEOUT,
		/** An operator does not apply to the column's declared {@code SearchType}. */
		OPERATOR_TYPE,
		/** The sort clause names more keys than the configured maximum. */
		TOO_MANY_SORT_KEYS,
		/** A requested paging position is negative. */
		NEGATIVE_POSITION,
		/** A literal argument or bare literal is not a parseable {@code numeric}, {@code boolean} or {@code timestamp} value for its column's type (see {@link SearchType#parse(String)}). */
		BAD_VALUE;
	}

	private final Code code;

	/**
	 * Constructor.
	 *
	 * @param code The stable {@link Code} identifying which problem occurred. Must not be <jk>null</jk>.
	 * @param message The {@link String#format(String, Object...) String.format}-style message (<c>%s</c> placeholders).
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments.
	 */
	public BeanQuerySyntaxException(Code code, String message, Object...args) {
		super(message, args);
		this.code = code;
	}

	/**
	 * Returns the stable code identifying which problem occurred.
	 *
	 * @return The code, never <jk>null</jk>.
	 */
	public Code code() {
		return code;
	}
}
