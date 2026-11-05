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
package org.apache.juneau.commons.svl;

import java.util.*;

/**
 * Parser for SVL <c>#{...}</c> expressions.
 *
 * <p>
 * Holds the lexer ({@link #lex(String)}) and the Pratt parser ({@link #parse(String, int)}). Keywords (<c>and or not matches true false null</c>) get their own
 * token types; the symbol forms <c>&amp;&amp; || !</c> produce the same types as their word forms (text keeps the original spelling).
 * Integer literals that do not fit a <c>long</c> (e.g. <c>9223372036854775808</c>) become Doubles.
 * Because keywords are reserved, they cannot be used as bare identifiers.
 */
class ExprParser {

	/** Token kinds. */
	enum TokenType {
		/** String literal; value is the decoded string. */ STRING,
		/** Numeric literal; value is a Long or Double. */ NUMBER,
		/** A <c>${...}</c> reference; text is the raw token. */ VARREF,
		/** A bare identifier. */ IDENT,
		/** <c>.</c> */ DOT,
		/** <c>?.</c> */ SAFE_DOT,
		/** <c>.?[</c> */ SELECT,
		/** <c>.![</c> */ PROJECT,
		/** <c>?:</c> */ ELVIS,
		/** <c>?</c> */ QUESTION,
		/** <c>:</c> */ COLON,
		/** <c>and</c> or <c>&amp;&amp;</c> */ AND,
		/** <c>or</c> or <c>||</c> */ OR,
		/** <c>not</c> or <c>!</c> */ NOT,
		/** <c>matches</c> */ MATCHES,
		/** <c>true</c> */ TRUE,
		/** <c>false</c> */ FALSE,
		/** <c>null</c> */ NULL,
		/** <c>==</c> */ EQ,
		/** <c>!=</c> */ NE,
		/** <c>&lt;</c> */ LT,
		/** <c>&lt;=</c> */ LE,
		/** <c>&gt;</c> */ GT,
		/** <c>&gt;=</c> */ GE,
		/** <c>+</c> */ PLUS,
		/** <c>-</c> */ MINUS,
		/** <c>*</c> */ STAR,
		/** <c>/</c> */ SLASH,
		/** <c>%</c> */ PERCENT,
		/** <c>(</c> */ LPAREN,
		/** <c>)</c> */ RPAREN,
		/** <c>[</c> */ LBRACKET,
		/** <c>]</c> */ RBRACKET,
		/** <c>,</c> */ COMMA,
		/** End of input. */ END
	}

	/**
	 * A lexed token.
	 *
	 * @param type Token kind.
	 * @param text Source text of the token.
	 * @param pos Zero-based offset of the token start.
	 * @param value Decoded value for STRING and NUMBER tokens; otherwise <jk>null</jk>.
	 */
	record Token(TokenType type, String text, int pos, Object value) {}

	/** Maximum infix/postfix operators in one expression (bounds left-associative chain depth deterministically). */
	static final int MAX_OPERATORS = 1000;

	private static final Map<String,TokenType> KEYWORDS = Map.of(
		"and", TokenType.AND, "or", TokenType.OR, "not", TokenType.NOT, "matches", TokenType.MATCHES,
		"true", TokenType.TRUE, "false", TokenType.FALSE, "null", TokenType.NULL);

	/**
	 * Lexes an expression body into tokens, ending with an END token.
	 *
	 * @param src The expression text (without the surrounding <c>#{}</c>).
	 * @return The tokens.
	 * @throws ExprException On an unterminated string or <c>${</c>, or an unexpected character.
	 */
	static List<Token> lex(String src) {
		var out = new ArrayList<Token>();
		int n = src.length(), i = 0;
		while (i < n) {
			char c = src.charAt(i);
			if (Character.isWhitespace(c)) { i++; continue; }
			int start = i;
			if (c == '\'' || c == '"') {
				var sb = new StringBuilder();
				i++;
				for (;;) {
					if (i >= n)
						throw new ExprException("Unterminated string", src, start);
					char d = src.charAt(i);
					if (d == c) {
						if (i + 1 < n && src.charAt(i + 1) == c) { sb.append(c); i += 2; continue; }
						i++;
						break;
					}
					sb.append(d);
					i++;
				}
				out.add(new Token(TokenType.STRING, src.substring(start, i), start, sb.toString()));
			} else if (c == '$') {
				if (i + 1 >= n || src.charAt(i + 1) != '{')
					throw new ExprException("Unexpected character '$'", src, i);
				i = scanVarRef(src, i);
				out.add(new Token(TokenType.VARREF, src.substring(start, i), start, null));
			} else if (isDigit(c) || (c == '.' && i + 1 < n && isDigit(src.charAt(i + 1)))) {
				i = scanNumber(src, i, out);
			} else if (Character.isJavaIdentifierStart(c)) {
				while (i < n && Character.isJavaIdentifierPart(src.charAt(i))) i++;
				var w = src.substring(start, i);
				out.add(new Token(KEYWORDS.getOrDefault(w, TokenType.IDENT), w, start, null));
			} else {
				var two = i + 1 < n ? src.substring(i, i + 2) : "";
				var three = i + 2 < n ? src.substring(i, i + 3) : "";
				TokenType t = null;
				int len = 0;
				if (three.equals(".?[")) { t = TokenType.SELECT; len = 3; }
				else if (three.equals(".![")) { t = TokenType.PROJECT; len = 3; }
				else {
					len = 2;
					switch (two) {
						case "&&": t = TokenType.AND; break;
						case "||": t = TokenType.OR; break;
						case "?:": t = TokenType.ELVIS; break;
						case "?.": t = TokenType.SAFE_DOT; break;
						case "==": t = TokenType.EQ; break;
						case "!=": t = TokenType.NE; break;
						case "<=": t = TokenType.LE; break;
						case ">=": t = TokenType.GE; break;
						default: len = 1;
					}
					if (t == null)
						t = switch (c) {
							case '.' -> TokenType.DOT;
							case '?' -> TokenType.QUESTION;
							case ':' -> TokenType.COLON;
							case '!' -> TokenType.NOT;
							case '<' -> TokenType.LT;
							case '>' -> TokenType.GT;
							case '+' -> TokenType.PLUS;
							case '-' -> TokenType.MINUS;
							case '*' -> TokenType.STAR;
							case '/' -> TokenType.SLASH;
							case '%' -> TokenType.PERCENT;
							case '(' -> TokenType.LPAREN;
							case ')' -> TokenType.RPAREN;
							case '[' -> TokenType.LBRACKET;
							case ']' -> TokenType.RBRACKET;
							case ',' -> TokenType.COMMA;
							default -> null;
						};
				}
				if (t == null)
					throw new ExprException("Unexpected character '" + c + "'", src, i);
				i += len;
				out.add(new Token(t, src.substring(start, i), start, null));
			}
		}
		out.add(new Token(TokenType.END, "", n, null));
		return out;
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * Returns the index just past the <c>${...}</c> starting at {@code start}.
	 *
	 * <p>
	 * Uses the shared {@link VarSplit#closeOfBody(String, int)} rules, so the lexer, the template compiler and the colon
	 * split agree on where a reference ends.
	 */
	private static int scanVarRef(String src, int start) {
		var close = VarSplit.closeOfBody(src, start + 2);
		if (close < 0)
			throw new ExprException("Unterminated ${", src, start);
		return close + 1;
	}

	/**
	 * Parses an expression body into an {@link Expr} tree.
	 *
	 * <h5 class='section'>Precedence</h5>
	 * <p>
	 * Binding powers, low to high: ternary <c>? :</c> 1 (right-assoc), Elvis <c>?:</c> 2 (right-assoc), <c>or ||</c> 3,
	 * <c>and &amp;&amp;</c> 4, <c>== != &lt; &lt;= &gt; &gt;= matches</c> 5, <c>+ -</c> 6, <c>* / %</c> 7, prefix
	 * <c>not ! - +</c> 8, postfix <c>. ?. [] ?.[] .?[] .![]</c> 9. Binary operators are left-associative, except that
	 * <c>matches</c> is non-associative: <c>a matches b matches c</c> is an error (parenthesise to chain).
	 * Symbol spellings are normalised: <c>&amp;&amp;</c> becomes <c>"and"</c>, <c>||</c> <c>"or"</c>, <c>!</c> <c>"not"</c>.
	 *
	 * <h5 class='section'>Keywords</h5>
	 * <ul>
	 * 	<li>A word-form <c>not</c>, <c>and</c> or <c>or</c> in primary position followed by <c>(</c> (with or without
	 * 		whitespace between) is a function {@link Expr.Call}, so <c>not(x)</c>, <c>not (x)</c> and <c>and(a, b)</c> call the
	 * 		functions of those names. In infix position (<c>a and (b)</c>) the word stays an operator. The symbol forms are
	 * 		never calls: <c>!(x)</c> is the unary operator.
	 * 	<li>Any keyword after <c>.</c> or <c>?.</c> is a property name (<c>obj.not</c>, <c>a?.matches</c>, <c>a.true</c>).
	 * 	<li><c>true false null</c> are literals in primary position; other keywords there are errors.
	 * 	<li>A bare identifier followed by <c>(</c> is a call; otherwise it is an {@link Expr.Ident} (scope lookup).
	 * </ul>
	 *
	 * <h5 class='section'>Positions</h5>
	 * <p>
	 * Every node's {@code pos} is the offset of the token that identifies it: the operator for {@link Expr.Binary},
	 * {@link Expr.Unary}, {@link Expr.Ternary} (the <c>?</c>) and {@link Expr.Elvis}; the <c>.</c> / <c>?.</c> for
	 * {@link Expr.Path}; the <c>[</c> (or the <c>?.</c> of <c>?.[</c>) for {@link Expr.Index}; the <c>.?[</c> / <c>.![</c>
	 * for {@link Expr.Select} / {@link Expr.Project}; the name for {@link Expr.Call} and {@link Expr.Ident}; and the token
	 * itself for literals and <c>${...}</c> references. Parentheses create no node.
	 *
	 * <h5 class='section'>Depth</h5>
	 * <p>
	 * Each nested sub-expression (parenthesised group, operand of a prefix operator, right operand, ternary branch,
	 * function argument, index or selection/projection body) is one level deeper than its parent; the whole expression is
	 * level 0. Left-associative chains and postfix chains do not nest. Exceeding {@code maxDepth} throws
	 * <c>expression nested too deeply</c> at the token where the too-deep sub-expression starts.
	 * {@code maxDepth} is not itself range-checked: callers must pass the small configured cap (default 32), since the
	 * parser recurses once per level.
	 *
	 * <h5 class='section'>Negative literals</h5>
	 * <p>
	 * There are no negative literals: <c>-1</c> parses as <c>Unary("-", Literal(1))</c> and is never folded. As a result
	 * <c>-9223372036854775808</c> is the negation of <c>9223372036854775808</c>, which does not fit a <c>long</c> and so
	 * lexes as a Double operand. That value (and any integer magnitude above <c>Long.MAX_VALUE</c>) loses integer
	 * precision.
	 *
	 * @param src The expression text (without the surrounding <c>#{}</c>).
	 * @param maxDepth The maximum nesting depth.
	 * @return The parsed tree.
	 * @throws ExprException On any lex or parse error, positioned at the offending token (the end-of-input offset for
	 * 	premature end).
	 */
	static Expr parse(String src, int maxDepth) {
		return parse(src, maxDepth, MAX_OPERATORS);
	}

	/**
	 * Parses with an explicit operator cap (tests use a huge cap to exercise the evaluator's stack-overflow guard).
	 *
	 * @param src The expression source.
	 * @param maxDepth The maximum nesting depth.
	 * @param maxOperators The maximum infix/postfix operators.
	 * @return The tree.
	 */
	static Expr parse(String src, int maxDepth, int maxOperators) {
		return new Parser(src, lex(src), maxDepth, maxOperators).parseAll();
	}

	/** Pratt parser state over one token list. */
	private static final class Parser {
		private static final int BP_TERNARY = 1, BP_ELVIS = 2, BP_OR = 3, BP_AND = 4, BP_CMP = 5, BP_ADD = 6, BP_MUL = 7, BP_PREFIX = 8;

		private final String src;
		private final List<Token> toks;
		private final int maxDepth;
		private final int maxOps;
		private int i, depth = -1, ops;

		Parser(String src, List<Token> toks, int maxDepth, int maxOps) {
			this.maxOps = maxOps;
			this.src = src;
			this.toks = toks;
			this.maxDepth = maxDepth;
		}

		Expr parseAll() {
			if (peek().type() == TokenType.END)
				throw new ExprException("Empty expression", src, peek().pos());
			var e = parseExpr(0);
			if (peek().type() != TokenType.END)
				throw unexpected(peek());
			return e;
		}

		private Token peek() {
			return toks.get(i);
		}

		private Token peek(int ahead) {
			return toks.get(Math.min(i + ahead, toks.size() - 1));
		}

		private Token next() {
			var t = toks.get(i);
			if (t.type() != TokenType.END)
				i++;
			return t;
		}

		private Token expect(TokenType type, String what) {
			var t = peek();
			if (t.type() != type)
				throw new ExprException("Expected '" + what + "' but found " + describe(t), src, t.pos());
			return next();
		}

		private ExprException unexpected(Token t) {
			return new ExprException("Unexpected " + describe(t), src, t.pos());
		}

		private static String describe(Token t) {
			return t.type() == TokenType.END ? "end of expression" : "'" + t.text() + "'";
		}

		private static boolean isWord(Token t) {
			return ! t.text().isEmpty() && Character.isJavaIdentifierStart(t.text().charAt(0));
		}

		private static int infixBp(TokenType t) {
			return switch (t) {
				case QUESTION -> BP_TERNARY;
				case ELVIS -> BP_ELVIS;
				case OR -> BP_OR;
				case AND -> BP_AND;
				case EQ, NE, LT, LE, GT, GE, MATCHES -> BP_CMP;
				case PLUS, MINUS -> BP_ADD;
				case STAR, SLASH, PERCENT -> BP_MUL;
				default -> -1;
			};
		}

		private static String binaryOp(Token t) {
			return switch (t.type()) {
				case AND -> "and";
				case OR -> "or";
				case MATCHES -> "matches";
				default -> t.text();
			};
		}

		/** Parses an expression whose infix operators all bind tighter than {@code minBp}; one nesting level deeper. */
		private Expr parseExpr(int minBp) {
			if (++depth > maxDepth)
				throw new ExprException("expression nested too deeply", src, peek().pos());
			try {
				var left = parsePrefix();
				var lastWasMatches = false;
				for (;;) {
					var t = peek();
					var pf = parsePostfix(left);
					if (pf != null) {
						countOp(t);
						left = pf;
						continue;
					}
					int bp = infixBp(t.type());
					if (bp <= minBp)
						return left;
					if (t.type() == TokenType.MATCHES && lastWasMatches)
						throw new ExprException("'matches' is non-associative; use parentheses", src, t.pos());
					countOp(t);
					next();
					lastWasMatches = t.type() == TokenType.MATCHES;
					if (t.type() == TokenType.QUESTION) {
						var a = parseExpr(0);
						expect(TokenType.COLON, ":");
						left = new Expr.Ternary(left, a, parseExpr(BP_TERNARY - 1), t.pos());
					} else if (t.type() == TokenType.ELVIS) {
						left = new Expr.Elvis(left, parseExpr(BP_ELVIS - 1), t.pos());
					} else {
						left = new Expr.Binary(binaryOp(t), left, parseExpr(bp), t.pos());
					}
				}
			} finally {
				depth--;
			}
		}

		/** Caps total operators so evaluation recursion depth never depends on the JVM stack size. */
		private void countOp(Token t) {
			if (++ops > maxOps)
				throw new ExprException("expression too complex", src, t.pos());
		}

		/** Applies one postfix operator to {@code target}, or returns <jk>null</jk> if the next token is not postfix. */
		private Expr parsePostfix(Expr target) {
			var t = peek();
			switch (t.type()) {
				case DOT -> {
					next();
					return new Expr.Path(target, propertyName(), false, t.pos());
				}
				case SAFE_DOT -> {
					next();
					if (peek().type() == TokenType.LBRACKET) {
						next();
						var idx = parseExpr(0);
						expect(TokenType.RBRACKET, "]");
						return new Expr.Index(target, idx, true, t.pos());
					}
					return new Expr.Path(target, propertyName(), true, t.pos());
				}
				case LBRACKET -> {
					next();
					var idx = parseExpr(0);
					expect(TokenType.RBRACKET, "]");
					return new Expr.Index(target, idx, false, t.pos());
				}
				case SELECT -> {
					next();
					var pred = parseExpr(0);
					expect(TokenType.RBRACKET, "]");
					return new Expr.Select(target, pred, t.pos());
				}
				case PROJECT -> {
					next();
					var map = parseExpr(0);
					expect(TokenType.RBRACKET, "]");
					return new Expr.Project(target, map, t.pos());
				}
				default -> {
					return null;
				}
			}
		}

		/** Reads a property name after <c>.</c> / <c>?.</c>: an identifier or any word keyword. */
		private String propertyName() {
			var t = peek();
			var ok = switch (t.type()) {
				case IDENT, TRUE, FALSE, NULL, MATCHES -> true;
				case AND, OR, NOT -> isWord(t);
				default -> false;
			};
			if (ok)
				return next().text();
			throw new ExprException("Expected property name but found " + describe(t), src, t.pos());
		}

		private Expr parsePrefix() {
			var t = peek();
			switch (t.type()) {
				case STRING, NUMBER -> {
					next();
					return new Expr.Literal(t.value(), t.pos());
				}
				case TRUE, FALSE -> {
					next();
					return new Expr.Literal(t.type() == TokenType.TRUE, t.pos());
				}
				case NULL -> {
					next();
					return new Expr.Literal(null, t.pos());
				}
				case VARREF -> {
					next();
					return new Expr.VarRef(t.text(), t.pos());
				}
				case IDENT -> {
					next();
					if (peek().type() == TokenType.LPAREN)
						return parseCall(t);
					return new Expr.Ident(t.text(), t.pos());
				}
				case AND, OR -> {
					if (isWord(t) && peek(1).type() == TokenType.LPAREN) {
						next();
						return parseCall(t);
					}
					throw unexpected(t);
				}
				case NOT -> {
					next();
					if (isWord(t) && peek().type() == TokenType.LPAREN)
						return parseCall(t);
					return new Expr.Unary("not", parseExpr(BP_PREFIX), t.pos());
				}
				case MINUS, PLUS -> {
					next();
					return new Expr.Unary(t.text(), parseExpr(BP_PREFIX), t.pos());
				}
				case LPAREN -> {
					next();
					var e = parseExpr(0);
					expect(TokenType.RPAREN, ")");
					return e;
				}
				default -> throw unexpected(t);
			}
		}

		/** Parses the argument list of a call whose name token was just consumed; the next token is <c>(</c>. */
		private Expr parseCall(Token name) {
			next();
			var args = new ArrayList<Expr>();
			if (peek().type() != TokenType.RPAREN) {
				for (;;) {
					args.add(parseExpr(0));
					if (peek().type() == TokenType.COMMA) {
						next();
						continue;
					}
					break;
				}
			}
			expect(TokenType.RPAREN, ")");
			return new Expr.Call(name.text(), List.copyOf(args), name.pos());
		}
	}

	private static int scanNumber(String src, int start, List<Token> out) {
		int n = src.length(), i = start;
		boolean dec = false;
		while (i < n && isDigit(src.charAt(i))) i++;
		if (i < n && src.charAt(i) == '.' && !(i + 1 < n && (Character.isJavaIdentifierStart(src.charAt(i + 1)) || src.charAt(i + 1) == '.' || src.charAt(i + 1) == '?' || src.charAt(i + 1) == '!'))) {
			dec = true;
			i++;
			while (i < n && isDigit(src.charAt(i))) i++;
		}
		if (i < n && (src.charAt(i) == 'e' || src.charAt(i) == 'E')) {
			int j = i + 1;
			if (j < n && (src.charAt(j) == '+' || src.charAt(j) == '-')) j++;
			if (j < n && isDigit(src.charAt(j))) {
				while (j < n && isDigit(src.charAt(j))) j++;
				dec = true;
				i = j;
			}
		}
		var text = src.substring(start, i);
		Object v;
		if (! dec) {
			try {
				v = Long.parseLong(text);
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				v = Double.parseDouble(text);
			}
		} else
			v = Double.parseDouble(text);
		out.add(new Token(TokenType.NUMBER, text, start, v));
		return i;
	}
}
