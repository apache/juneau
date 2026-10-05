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

import static org.apache.juneau.commons.lang.StateEnum.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * The single public parser for the {@code $}-search language (design §5.1).  Engines call it; a {@link BeanQuery}
 * itself carries only the raw strings.
 *
 * <h5 class='section'>Grammar</h5>
 * <p>
 * A column's expression is one of:
 * <ul>
 * 	<li>A <b>pattern</b> &mdash; a bare token (case-insensitive substring / prefix / exact per the column type),
 * 		optionally with {@code *} wildcards, or a value such as {@code $100} whose leading {@code $} is not a known
 * 		operator invocation.  Patterns are {@link SearchExpression#isLiteral() literal} nodes.  This also covers a
 * 		relative-duration token such as {@code -24h} or {@code +7d} ({@code [+-]<n>(ms|s|m|h|d)}) or a signed ISO-8601 duration such as {@code -PT24H}: the parser applies
 * 		no special grammar to it, since it is already an ordinary pattern; a TIMESTAMP or duration column's typed
 * 		conversion (not this parser) is what resolves it relative to request time.
 * 	<li>An <b>operator</b> &mdash; {@code $name(arg,...)} where {@code $name} is in the supplied
 * 		{@link SearchOperatorSet}.  Leaf-operator arguments are literals; combinator ({@code $and}/{@code $or}/
 * 		{@code $not}) arguments are themselves expressions.  Columns combine with {@code and}; {@code $or}/{@code $and}/
 * 		{@code $not} combine within one column.
 * </ul>
 *
 * <h5 class='section'>Literal dollar</h5>
 * <p>
 * {@code $$} is a literal {@code $} everywhere.  A token that starts with {@code $$} is always a pattern (never an
 * operator), and every {@code $$} inside a literal decodes to a single {@code $}.  So {@code $$eq(x)} is the literal
 * text {@code $eq(x)}, while {@code $eq(x)} is the operator.
 *
 * <h5 class='section'>Quoting</h5>
 * <p>
 * A quoted literal argument (design §5.1) may contain its own delimiter by doubling it &mdash; {@code ""} inside a
 * double-quoted value, or {@code ''} inside a single-quoted value, decodes to one literal quote character of that
 * same kind.  So {@code $eq("a""b")} is the literal text {@code a"b}.  This doubling decode is independent of, and
 * composes with, the {@code $$} &rarr; {@code $} decoding above.
 *
 * <h5 class='section'>Errors</h5>
 * <p>
 * An unknown {@code $name(...)}, a malformed expression (unclosed/unbalanced parentheses, junk after the close paren,
 * a stray comma, a wrong argument count), or an empty expression throws {@link BeanQuerySyntaxException}.  There is no
 * "incomplete draft" state on the server: a request-facing adapter catches the exception and surfaces the message.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> {
 * 		SearchExpression <jv>node</jv> = SearchExpressionParser.<jsm>parse</jsm>(<js>"$gt(10)"</js>, SearchOperatorSet.<jsm>standard</jsm>());
 * 		<jc>// node.isFunction() == true, node.name() == "$gt", node.literalArgs() == ["10"]</jc>
 * 	} <jk>catch</jk> (BeanQuerySyntaxException <jv>e</jv>) {
 * 		<jc>// e.code() names the specific grammar problem, e.g. Code.UNKNOWN_OPERATOR or Code.UNBALANCED</jc>
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
public final class SearchExpressionParser {

	/** No instances. */
	private SearchExpressionParser() {}

	/**
	 * Parses a raw column-search expression.
	 *
	 * @param raw The raw expression string (a pattern or {@code $name(...)}).  Must not be <jk>null</jk> or blank.
	 * @param operators The operator set that defines which {@code $}-names are recognized.  Must not be <jk>null</jk>.
	 * @return The parsed expression, never <jk>null</jk>.
	 * @throws BeanQuerySyntaxException If the expression is empty, names an unknown operator, or is malformed.
	 */
	public static SearchExpression parse(String raw, SearchOperatorSet operators) {
		reqnn("operators", operators);
		if (ib(raw))
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.EMPTY_EXPRESSION, "Empty search expression.");
		return parseExpr(raw.strip(), operators);
	}

	/** Parses one (already trimmed, non-empty) token into a node. */
	private static SearchExpression parseExpr(String s, SearchOperatorSet operators) {
		if (! s.startsWith("$$") && s.startsWith("$") && s.length() >= 2 && Character.isLetter(s.charAt(1)))
			return parseFunc(s, operators);
		if (isUnterminatedQuote(s))
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNTERMINATED_QUOTE, "Unterminated quoted value in search expression: '%s'", s);
		return SearchExpression.literal(decodeLiteral(s), isQuotedToken(s));
	}

	/** Whether {@code s} is delimited by a matching pair of double or single quotes. */
	private static boolean isQuotedToken(String s) {
		if (s.length() < 2)
			return false;
		var first = s.charAt(0);
		return (first == '"' || first == '\'') && s.charAt(s.length() - 1) == first;
	}

	/** Whether {@code s} opens a quote ({@code "} or {@code '} as its first character) that its own last character does not close. */
	private static boolean isUnterminatedQuote(String s) {
		var first = s.charAt(0);
		return (first == '"' || first == '\'') && ! isQuotedToken(s);
	}

	/**
	 * Parses a {@code $name(args...)} token that must span the entire string {@code s}.
	 *
	 * <p>
	 * After validating the operator name, a state machine walks the argument list once: top-level commas separate
	 * arguments, quotes protect commas/parens, and the matching {@code )} ends the call.  Nested expressions are then
	 * parsed recursively (same shape as the JSON parser's nested object/array dispatch).
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for parser state machine
		"java:S6541" // Brain method acceptable for parser state machine
	})
	private static SearchExpression parseFunc(String s, SearchOperatorSet operators) {
		var open = s.indexOf('(');
		if (open < 0)
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.MALFORMED_OPERATOR, "Malformed search expression (operator name without arguments): '%s'", s);
		var name = s.substring(0, open);
		if (! isValidName(name))
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.INVALID_OPERATOR_NAME, "Invalid operator name: '%s'", name);
		var op = operators.get(name);
		if (op == null)
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNKNOWN_OPERATOR, "Unknown search operator: '%s'", name);

		// S1: Outside quotes, tracking paren depth (looking for top-level ',' or matching ')').
		// S2: Inside a double-quoted argument fragment.
		// S3: Inside a single-quoted argument fragment.

		var state = S1;
		var depth = 0;
		var argStart = open + 1;
		var rawArgs = new ArrayList<String>();

		for (var i = open; i < s.length(); i++) {
			var c = s.charAt(i);
			if (state == S1) {
				if (c == '"') {
					state = S2;
				} else if (c == '\'') {
					state = S3;
				} else if (c == '(') {
					depth++;
				} else if (c == ')' && --depth == 0) {
					if (argStart != i || ! rawArgs.isEmpty())  // Otherwise $blank() — zero arguments.
						addPart(s, rawArgs, s.substring(argStart, i));
					if (inb(s.substring(i + 1)))
						throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.TRAILING_TEXT, "Unexpected text after ')' in search expression: '%s'", s);
					var args = rawArgs.stream().map(part -> parseExpr(part.strip(), operators)).toList();
					if (! op.acceptsArgCount(args.size()))
						throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.BAD_ARG_COUNT,
							regexHint("Operator '%s' does not accept %s argument(s).", op.name()), op.name(), args.size());
					return SearchExpression.func(op, args);
				} else if (c == ',' && depth == 1) {
					addPart(s, rawArgs, s.substring(argStart, i));
					argStart = i + 1;
				}
			} else if (state == S2 && c == '"') {
				state = S1;
			} else if (state == S3 && c == '\'') {
				state = S1;
			}
		}
		if (state != S1)
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNTERMINATED_QUOTE, "Unterminated quoted value in search expression: '%s'", s);
		throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNBALANCED, regexHint("Unclosed parenthesis in search expression: '%s'", op.name()), s);
	}

	/** A valid operator name is {@code $} + a letter + zero or more letters/digits. */
	static boolean isValidName(String name) {
		if (name.length() < 2 || name.charAt(0) != '$' || ! Character.isLetter(name.charAt(1)))
			return false;
		return name.substring(2).chars().allMatch(c -> Character.isLetterOrDigit((char)c));
	}

	/** Appends the D5 quoting hint to {@code message} when {@code opName} is {@code $regex}; otherwise unchanged. */
	private static String regexHint(String message, String opName) {
		return "$regex".equals(opName) ? message + " Quote regex patterns that contain commas or parentheses: $regex(\"…\")." : message;
	}

	/** Appends one split argument, rejecting a blank one (a stray/trailing comma). */
	private static void addPart(String whole, List<String> parts, String part) {
		if (ib(part))
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.EMPTY_ARGUMENT, "Empty argument in search expression: '%s'", whole);
		parts.add(part);
	}

	/**
	 * Strips a matching pair of surrounding double or single quotes from a literal token, collapses a doubled
	 * instance of that same quote character to one literal quote (design D5 — {@code ""} or {@code ''} inside a
	 * quoted value is one literal quote character), then decodes every {@code $$} escape to a single {@code $}.
	 */
	private static String decodeLiteral(String s) {
		var v = s;
		if (v.length() >= 2) {
			var first = v.charAt(0);
			if ((first == '"' || first == '\'') && v.charAt(v.length() - 1) == first) {
				v = v.substring(1, v.length() - 1);
				var q = String.valueOf(first);
				v = v.replace(q + q, q);
			}
		}
		return v.replace("$$", "$");
	}
}
