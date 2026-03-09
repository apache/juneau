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
package org.apache.juneau.hocon;

import java.io.*;

import java.util.regex.*;

/**
 * Tokenizer for HOCON (Human-Optimized Config Object Notation) format.
 *
 * <p>
 * Handles unquoted strings, quoted strings, triple-quoted strings, numbers, booleans, null,
 * structural tokens, substitutions ({@code ${var}} and {@code ${?var}}), and comments.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://github.com/lightbend/config/blob/main/HOCON.md">HOCON Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S135",  // Multiple break/continue necessary for tokenizer state machine loops
	"java:S3776" // Cognitive complexity acceptable for HOCON grammar
})
public class HoconTokenizer {

	/** Characters that cannot appear in unquoted strings per HOCON spec (includes . for path parsing). Note: / is allowed for paths like /usr/local. */
	private static final String UNQUOTED_FORBIDDEN = "$\"{}[]:=,+#^\\?!@*&\t\n\r ";

	/** Token types produced by the tokenizer. */
	public enum TokenType {
		/** Unquoted string (key or value). */
		UNQUOTED_STRING,
		/** Double or single-quoted string. */
		QUOTED_STRING,
		/** Triple-quoted string ("""..."""). */
		TRIPLE_QUOTED,
		/** Number. */
		NUMBER,
		/** Boolean true. */
		TRUE,
		/** Boolean false. */
		FALSE,
		/** Null value. */
		NULL,
		/** Left brace {. */
		LBRACE,
		/** Right brace }. */
		RBRACE,
		/** Left bracket [. */
		LBRACKET,
		/** Right bracket ]. */
		RBRACKET,
		/** Colon :. */
		COLON,
		/** Equals =. */
		EQUALS,
		/** Plus-equals +=. */
		PLUS_EQUALS,
		/** Comma ,. */
		COMMA,
		/** Significant newline (member separator). */
		NEWLINE,
		/** Substitution ${var}. */
		SUBSTITUTION,
		/** Optional substitution ${?var}. */
		OPT_SUBSTITUTION,
		/** End of input. */
		EOF
	}

	/**
	 * A token with optional string or number value.
	 *
	 * @param type Token type.
	 * @param stringValue String value for string tokens; <jk>null</jk> otherwise.
	 * @param numberValue Number value for NUMBER tokens; <jk>null</jk> otherwise.
	 */
	public record Token(TokenType type, String stringValue, Number numberValue) {
		/**
		 * Creates a structural or literal token with no value.
		 *
		 * @param type Token type.
		 * @return A new token.
		 */
		public static Token of(TokenType type) {
			return new Token(type, null, null);
		}
		/**
		 * Creates a string token.
		 *
		 * @param value The string value.
		 * @return A new token.
		 */
		public static Token string(String value) {
			return new Token(TokenType.UNQUOTED_STRING, value, null);
		}
		/**
		 * Creates a quoted-string token.
		 *
		 * @param value The string value.
		 * @return A new token.
		 */
		public static Token quoted(String value) {
			return new Token(TokenType.QUOTED_STRING, value, null);
		}
		/**
		 * Creates a triple-quoted string token.
		 *
		 * @param value The string value.
		 * @return A new token.
		 */
		public static Token tripleQuoted(String value) {
			return new Token(TokenType.TRIPLE_QUOTED, value, null);
		}
		/**
		 * Creates a number token.
		 *
		 * @param value The number value.
		 * @return A new token.
		 */
		public static Token number(Number value) {
			return new Token(TokenType.NUMBER, null, value);
		}
		/**
		 * Creates a substitution token.
		 *
		 * @param path The reference path.
		 * @param optional Whether it is optional (${?var}).
		 * @return A new token.
		 */
		public static Token substitution(String path, boolean optional) {
			return new Token(optional ? TokenType.OPT_SUBSTITUTION : TokenType.SUBSTITUTION, path, null);
		}
	}

	private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

	private final PushbackReader reader;
	private Token peeked;
	private int line = 1;
	private int column = 1;

	/**
	 * Constructor.
	 *
	 * @param reader The reader to tokenize.
	 */
	public HoconTokenizer(Reader reader) {
		var in = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
		this.reader = new PushbackReader(in, 8);
	}

	/**
	 * Returns the current line number (1-based).
	 *
	 * @return The line number.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Returns the current column number (1-based).
	 *
	 * @return The column number.
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Peeks at the next token without consuming it.
	 *
	 * @return The next token.
	 * @throws IOException If a read error occurs.
	 */
	public Token peek() throws IOException {
		if (peeked == null)
			peeked = readToken();
		return peeked;
	}

	/**
	 * Peeks at the next token without skipping whitespace first.
	 * Caller must call {@link #skipWhitespaceAndCommentsExceptNewlines()} first when checking for value concatenation.
	 *
	 * @return The next token.
	 * @throws IOException If a read error occurs.
	 */
	public Token peekNoSkip() throws IOException {
		if (peeked == null)
			peeked = readTokenImmediate();
		return peeked;
	}

	/**
	 * Reads and consumes the next token.
	 *
	 * @return The next token.
	 * @throws IOException If a read error occurs.
	 */
	public Token read() throws IOException {
		if (peeked != null) {
			var t = peeked;
			peeked = null;
			return t;
		}
		return readToken();
	}

	private int readChar() throws IOException {
		var c = reader.read();
		if (c == '\n' || (c == '\r' && peekChar() != '\n')) {
			line++;
			column = 1;
		} else if (c >= 0) {
			column++;
		}
		return c;
	}

	private int peekChar() throws IOException {
		var c = reader.read();
		if (c >= 0)
			reader.unread(c);
		return c;
	}

	private void unread(int c) throws IOException {
		if (c >= 0) {
			reader.unread(c);
			column--;
			if (c == '\n')
				line--;
		}
	}

	/**
	 * Skips whitespace and comments.
	 *
	 * @throws IOException If a read error occurs.
	 */
	public void skipWhitespaceAndComments() throws IOException {
		while (true) {
			var c = readChar();
			if (c < 0)
				return;
			if (c == ' ' || c == '\t' || c == '\r' || c == '\n')
				continue;
			if (c == '#') {
				skipLineComment();
				continue;
			}
			if (c == '/' && peekChar() == '/') {
				readChar();
				skipLineComment();
				continue;
			}
			unread(c);
			return;
		}
	}

	private void skipLineComment() throws IOException {
		var c = readChar();
		while (c >= 0 && c != '\n' && c != '\r')
			c = readChar();
		if (c == '\r' && peekChar() == '\n')
			readChar();
	}

	/** Skips a line comment but stops before the newline (does not consume it). */
	private void skipLineCommentExceptNewline() throws IOException {
		var c = readChar();
		while (c >= 0 && c != '\n' && c != '\r') {
			c = readChar();
		}
		if (c >= 0)
			unread(c);
	}

	/**
	 * Skips whitespace and comments but stops at newlines.
	 * Used when parsing value concatenation so newlines end the value.
	 *
	 * @throws IOException If a read error occurs.
	 */
	public void skipWhitespaceAndCommentsExceptNewlines() throws IOException {
		while (true) {
			var c = readChar();
			if (c < 0)
				return;
			if (c == '\n' || c == '\r') {
				unread(c);
				return;
			}
			if (c == ' ' || c == '\t')
				continue;
			if (c == '#') {
				skipLineCommentExceptNewline();
				continue;
			}
			if (c == '/' && peekChar() == '/') {
				readChar();
				skipLineCommentExceptNewline();
				continue;
			}
			unread(c);
			return;
		}
	}

	private Token readToken() throws IOException {
		skipWhitespaceAndComments();
		return readTokenImmediate();
	}

	private Token readTokenImmediate() throws IOException {
		var c = readChar();
		if (c < 0)
			return Token.of(TokenType.EOF);

		if (c == '{') return Token.of(TokenType.LBRACE);
		if (c == '}') return Token.of(TokenType.RBRACE);
		if (c == '[') return Token.of(TokenType.LBRACKET);
		if (c == ']') return Token.of(TokenType.RBRACKET);
		if (c == ',') return Token.of(TokenType.COMMA);
		if (c == '\n' || c == '\r') {
			if (c == '\r' && peekChar() == '\n')
				readChar();
			return Token.of(TokenType.NEWLINE);
		}

		if (c == ':') {
			return Token.of(TokenType.COLON);
		}
		if (c == '=') {
			if (peekChar() != '+')
				return Token.of(TokenType.EQUALS);
		}
		if (c == '+' && peekChar() == '=') {
			readChar();
			return Token.of(TokenType.PLUS_EQUALS);
		}

		if (c == '"') {
			if (peekChar() == '"' && peekChar(2) == '"') {
				readChar();
				readChar();
				return Token.tripleQuoted(readTripleQuotedString());
			}
			unread(c);
			return Token.quoted(readQuotedString());
		}
		if (c == '\'') {
			unread(c);
			return Token.quoted(readQuotedString());
		}

		if (c == '$' && peekChar() == '{') {
			readChar();
			return readSubstitutionToken();
		}

		unread(c);
		return readUnquotedOrNumber();
	}

	private Token readSubstitutionToken() throws IOException {
		var sb = new StringBuilder();
		var c = readChar();
		var optional = c == '?';
		if (optional)
			c = readChar();
		while (c >= 0 && c != '}') {
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
				break;
			sb.append((char) c);
			c = readChar();
		}
		if (c == '}')
			return Token.substitution(sb.toString().trim(), optional);
		// Malformed ${...} - treat as string
		return Token.string("${" + (optional ? "?" : "") + sb);
	}

	private Token readUnquotedOrNumber() throws IOException {
		var sb = new StringBuilder();
		var c = readChar();
		while (c >= 0 && UNQUOTED_FORBIDDEN.indexOf(c) < 0) {
			sb.append((char) c);
			c = readChar();
		}
		if (c >= 0)
			unread(c);
		var raw = sb.toString().trim();
		if (raw.isEmpty())
			return readToken();

		if (raw.equals("true")) return Token.of(TokenType.TRUE);
		if (raw.equals("false")) return Token.of(TokenType.FALSE);
		if (raw.equals("null")) return Token.of(TokenType.NULL);

		var numMatcher = NUMBER_PATTERN.matcher(raw);
		if (numMatcher.matches()) {
			var numStr = numMatcher.group();
			try {
				if (numStr.contains(".") || numStr.toLowerCase().contains("e"))
					return Token.number(Double.parseDouble(numStr));
				return parseIntegerOrLongToken(numStr);
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				// Number too large for Long or other parse failure; fall through to unquoted
			}
		}

		return Token.string(raw);
	}

	private static Token parseIntegerOrLongToken(String numStr) {
		try {
			return Token.number(Integer.parseInt(numStr));
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return Token.number(Long.parseLong(numStr));
		}
	}

	private int peekChar(int offset) throws IOException {
		var buf = new int[offset];
		for (var i = 0; i < offset; i++)
			buf[i] = readChar();
		for (var i = offset - 1; i >= 0; i--)
			unread(buf[i]);
		return offset > 0 ? buf[offset - 1] : -1;
	}

	/**
	 * Reads a quoted string ("..." or '...') with JSON-style escaping.
	 *
	 * @return The string value.
	 * @throws IOException If a read error occurs.
	 */
	public String readQuotedString() throws IOException {
		var quote = readChar();
		if (quote != '"' && quote != '\'')
			return "";
		var sb = new StringBuilder();
		var c = readChar();
		while (c >= 0 && c != quote) {
			if (c == '\\') {
				c = readChar();
				if (c >= 0) {
					if (c == 'u')
						sb.append(readUnicodeEscape());
					else
						sb.append(unescape(c));
				}
			} else {
				sb.append((char) c);
			}
			c = readChar();
		}
		return sb.toString();
	}

	private static char unescape(int c) {
		return switch (c) {
			case '"' -> '"';
			case '\'' -> '\'';
			case '\\' -> '\\';
			case '/' -> '/';
			case 'b' -> '\b';
			case 'f' -> '\f';
			case 'n' -> '\n';
			case 'r' -> '\r';
			case 't' -> '\t';
			default -> (char) c;
		};
	}

	private char readUnicodeEscape() throws IOException {
		var hex = new StringBuilder(4);
		for (var i = 0; i < 4; i++) {
			var c = readChar();
			if (c < 0 || !isHexDigit(c))
				throw new IOException("Invalid \\u escape sequence at line " + line + ", column " + column);
			hex.append((char) c);
		}
		return (char) Integer.parseInt(hex.toString(), 16);
	}

	private static boolean isHexDigit(int c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	/**
	 * Reads an unquoted string until a forbidden character.
	 *
	 * @return The string value.
	 * @throws IOException If a read error occurs.
	 */
	public String readUnquotedString() throws IOException {
		var sb = new StringBuilder();
		var c = readChar();
		while (c >= 0 && UNQUOTED_FORBIDDEN.indexOf(c) < 0) {
			sb.append((char) c);
			c = readChar();
		}
		if (c >= 0)
			unread(c);
		return sb.toString().trim();
	}

	/**
	 * Reads a triple-quoted string ("""..."""). No escape processing inside per HOCON spec.
	 *
	 * @return The string value.
	 * @throws IOException If a read error occurs.
	 */
	public String readTripleQuotedString() throws IOException {
		var sb = new StringBuilder();
		var quoteCount = 0;
		var c = readChar();
		while (c >= 0) {
			if (c == '"') {
				quoteCount++;
				if (quoteCount == 3) {
					var s = sb.toString();
					if (s.endsWith("\n"))
						s = s.substring(0, s.length() - 1);
					return s.replace("\r\n", "\n").replace("\r", "\n");
				}
			} else {
				if (quoteCount == 1)
					sb.append('"');
				else if (quoteCount == 2)
					sb.append("\"\"");
				quoteCount = 0;
				if (c == '\r' && peekChar() == '\n') {
					readChar();
					sb.append('\n');
				} else if (c != '\r') {
					sb.append((char) c);
				}
			}
			c = readChar();
		}
		return sb.toString();
	}
}
