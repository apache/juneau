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
package org.apache.juneau.hjson;

import java.io.*;
import java.util.regex.*;

/**
 * Tokenizer for Hjson format.
 *
 * <p>
 * Handles quoted strings, quoteless strings, multiline strings, comments, numbers, booleans, null,
 * and structural tokens.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S135",  // Multiple break/continue necessary for tokenizer state machine loops
	"java:S3776" // Cognitive complexity acceptable for Hjson grammar
})
public class HjsonTokenizer {

	/** Token types produced by the tokenizer. */
	public enum TokenType {
		/** Double or single-quoted string. */
		STRING,
		/** Quoteless string (to end of line). */
		QUOTELESS,
		/** Multiline string ('''...'''). */
		MULTILINE,
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
		/** Comma ,. */
		COMMA,
		/** Significant newline (member separator). */
		NEWLINE,
		/** End of input. */
		EOF
	}

	/**
	 * A token with optional string or number value.
	 *
	 * @param type Token type.
	 * @param stringValue String value for STRING, QUOTELESS, MULTILINE tokens; <jk>null</jk> otherwise.
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
		 * Creates a quoted-string token.
		 *
		 * @param value The string value.
		 * @return A new token.
		 */
		public static Token string(String value) {
			return new Token(TokenType.STRING, value, null);
		}
		/**
		 * Creates a quoteless-string token.
		 *
		 * @param value The string value.
		 * @return A new token.
		 */
		public static Token quoteless(String value) {
			return new Token(TokenType.QUOTELESS, value, null);
		}
		/**
		 * Creates a multiline-string token.
		 *
		 * @param value The string value.
		 * @return A new token.
		 */
		public static Token multiline(String value) {
			return new Token(TokenType.MULTILINE, value, null);
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
	@SuppressWarnings({
		"resource" // BufferedReader is owned by PushbackReader; PushbackReader constructor never throws
	})
	public HjsonTokenizer(Reader reader) {
		var in = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
		this.reader = new PushbackReader(in, 4);
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
			if (c == '/' && peekChar() == '*') {
				readChar();
				skipBlockComment();
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

	private void skipBlockComment() throws IOException {
		var c = readChar();
		while (c >= 0) {
			if (c == '*' && peekChar() == '/') {
				readChar();
				return;
			}
			c = readChar();
		}
	}

	private Token readToken() throws IOException {
		skipWhitespaceAndComments();
		var c = readChar();
		if (c < 0)
			return Token.of(TokenType.EOF);

		if (c == '{') return Token.of(TokenType.LBRACE);
		if (c == '}') return Token.of(TokenType.RBRACE);
		if (c == '[') return Token.of(TokenType.LBRACKET);
		if (c == ']') return Token.of(TokenType.RBRACKET);
		if (c == ':') return Token.of(TokenType.COLON);
		if (c == ',') return Token.of(TokenType.COMMA);
		if (c == '\n' || c == '\r') {
			if (c == '\r' && peekChar() == '\n')
				readChar();
			return Token.of(TokenType.NEWLINE);
		}

		if (c == '\'' && peekChar() == '\'' && peekChar(2) == '\'') {
			readChar();
			readChar();
			var indentBaseline = getColumn() - 3;
			return Token.multiline(readMultilineString(indentBaseline));
		}
		if (c == '"' || c == '\'') {
			unread(c);
			return Token.string(readQuotedString());
		}

		unread(c);

		var sb = new StringBuilder();
		var next = readChar();
		while (next >= 0 && next != '\n' && next != '\r' && next != ',' && next != ':' && next != '{' && next != '}' && next != '[' && next != ']') {
			sb.append((char) next);
			next = readChar();
		}
		if (next >= 0)
			unread(next);

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
				// Number too large for Long or other parse failure; fall through to quoteless
			}
		}

		return Token.quoteless(raw);
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
	 * Reads a quoteless string (to end of line, trimmed).
	 *
	 * @return The string value.
	 * @throws IOException If a read error occurs.
	 */
	public String readQuotelessString() throws IOException {
		var sb = new StringBuilder();
		var c = readChar();
		while (c >= 0 && c != '\n' && c != '\r') {
			sb.append((char) c);
			c = readChar();
		}
		if (c >= 0)
			unread(c);
		return sb.toString().trim();
	}

	/**
	 * Reads a multiline string ('''...''').
	 * Caller must have already consumed the opening '''.
	 *
	 * @param indentBaseline The 1-based column of the first quote in '''; used to strip
	 * 	leading indentation from subsequent lines per Hjson spec.
	 * @return The string value (with \n line separators, last newline ignored per spec).
	 * @throws IOException If a read error occurs.
	 */
	public String readMultilineString(int indentBaseline) throws IOException {
		var sb = new StringBuilder();
		var quoteCount = 0;
		var c = readChar();
		while (c >= 0) {
			if (c == '\'') {
				quoteCount++;
				if (quoteCount == 3) {
					var s = sb.toString();
					if (s.endsWith("\n"))
						s = s.substring(0, s.length() - 1);
					s = s.replace("\r\n", "\n").replace("\r", "\n");
					return stripMultilineIndent(s, indentBaseline);
				}
			} else {
				if (quoteCount == 1)
					sb.append("'");
				else if (quoteCount == 2)
					sb.append("''");
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
		return stripMultilineIndent(sb.toString(), indentBaseline);
	}

	private static String stripMultilineIndent(String s, int indentBaseline) {
		if (indentBaseline <= 0 || s.isEmpty())
			return s;
		var stripCount = indentBaseline - 1;
		if (stripCount <= 0)
			return s;
		var lines = s.split("\n", -1);
		var result = new StringBuilder();
		for (var i = 0; i < lines.length; i++) {
			if (i > 0)
				result.append('\n');
			var line = lines[i];
			var j = 0;
			while (j < stripCount && j < line.length() && (line.charAt(j) == ' ' || line.charAt(j) == '\t'))
				j++;
			result.append(line.substring(j));
		}
		return result.toString();
	}
}
