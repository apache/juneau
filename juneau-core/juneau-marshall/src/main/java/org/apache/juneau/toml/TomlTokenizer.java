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
package org.apache.juneau.toml;

import java.io.*;
import java.util.*;

import org.apache.juneau.parser.ParseException;

/**
 * Internal tokenizer for TOML v1.0.0 parsing.
 *
 * <p>
 * Character-by-character reader that handles the full TOML grammar: strings, numbers,
 * booleans, date/times, keys, table headers, and structural elements.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is package-private and not intended for external use.
 * </ul>
 */
@SuppressWarnings({
	"java:S3776", // Cognitive complexity acceptable for TOML grammar
	"java:S6541"  // Brain method acceptable for tokenizer
})
class TomlTokenizer {

	private static final String CONST_integerOverflow = "Integer overflow: ";

	private final Reader reader;
	private final Deque<Integer> pushback = new ArrayDeque<>();
	private int line = 1;
	private int column;

	TomlTokenizer(Reader reader) {
		this.reader = reader == null ? new StringReader("") : reader;
	}

	int peek() throws IOException {
		if (!pushback.isEmpty())
			return pushback.peekLast();
		int c = reader.read();
		if (c >= 0)
			pushback.addLast(c);
		return c;
	}

	int read() throws IOException {
		if (!pushback.isEmpty()) {
			int c = pushback.removeLast();
			if (c == '\n') {
				line++;
				column = 0;
			} else {
				column++;
			}
			return c;
		}
		int c = reader.read();
		if (c == '\n') {
			line++;
			column = 0;
		} else if (c >= 0) {
			column++;
		}
		return c;
	}

	void unread(int c) {
		if (c >= 0)
			pushback.addLast(c);
	}

	String readUntil(char... stops) throws IOException {
		var sb = new StringBuilder();
		int c;
		while ((c = read()) >= 0) {
			for (char stop : stops) {
				if (c == stop)
					return sb.toString();
			}
			sb.append((char) c);
		}
		return sb.toString();
	}

	void skipWhitespace() throws IOException {
		int c;
		while ((c = peek()) >= 0 && (c == ' ' || c == '\t'))
			read();
	}

	void skipWhitespaceAndNewlines() throws IOException {
		int c;
		while ((c = peek()) >= 0 && (c == ' ' || c == '\t' || c == '\n' || c == '\r'))
			read();
	}

	void skipComment() throws IOException {
		if (peek() == '#') {
		while (read() >= 0 && peek() != '\n' && peek() != '\r')
			{ /* consume comment characters until end of line */ }
		}
	}

	void skipWhitespaceAndComments() throws IOException {
		while (true) {
			skipWhitespaceAndNewlines();
			if (peek() == '#')
				skipToNextLine();
			else
				break;
		}
	}

	void skipToNextLine() throws IOException {
		int c;
		while ((c = read()) >= 0 && c != '\n' && c != '\r')
			{ /* consume characters until end of line */ }
		if (c == '\r' && peek() == '\n')
			read();
	}

	boolean isEof() throws IOException {
		return peek() < 0;
	}

	int getLine() {
		return line;
	}

	int getColumn() {
		return column;
	}

	ParseException parseException(String message) {
		return new ParseException(message + " at line " + line + ", column " + column);
	}

	String readBareKey() throws IOException, ParseException {
		int c = peek();
		if (c < 0)
			return null;
		if (c == '"')
			return readBasicString();
		if (c == '\'') {
			if (peekTriple('\''))
				return readMultiLineLiteralString();
			return readLiteralString();
		}
		var sb = new StringBuilder();
		while (isBareKeyChar(peek())) {
			sb.append((char) read());
		}
		if (sb.isEmpty())
			return null;
		return sb.toString();
	}

	private static boolean isBareKeyChar(int c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
	}

	String readBasicString() throws IOException, ParseException {
		if (read() != '"')
			throw parseException("Expected '\"'");
		return readBasicStringContent();
	}

	private String readBasicStringContent() throws IOException, ParseException {
		var sb = new StringBuilder();
		int c;
		while ((c = read()) >= 0 && c != '"') {
			if (c == '\\') {
				int escaped = read();
				if (escaped < 0)
					throw parseException("Unexpected end of string");
				switch (escaped) {
					case 'b': sb.append('\b'); break;
					case 't': sb.append('\t'); break;
					case 'n': sb.append('\n'); break;
					case 'f': sb.append('\f'); break;
					case 'r': sb.append('\r'); break;
					case '"': sb.append('"'); break;
					case '\\': sb.append('\\'); break;
					case 'u':
						sb.append(readUnicodeEscape(4));
						break;
					case 'U':
						sb.append(readUnicodeEscape(8));
						break;
					default:
						throw parseException("Invalid escape sequence: \\" + (char) escaped);
				}
			} else {
				sb.append((char) c);
			}
		}
		if (c < 0)
			throw parseException("Unterminated string");
		return sb.toString();
	}

	private char readUnicodeEscape(int len) throws IOException, ParseException {
		var hex = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int c = read();
			if (c < 0 || !isHexDigit(c))
				throw parseException("Invalid unicode escape");
			hex.append((char) c);
		}
		int cp = Integer.parseInt(hex.toString(), 16);
		if (Character.isValidCodePoint(cp))
			return (char) cp;
		throw parseException("Invalid unicode code point: " + hex);
	}

	private static boolean isHexDigit(int c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	String readLiteralString() throws IOException, ParseException {
		if (read() != '\'')
			throw parseException("Expected \"'\"");
		var sb = new StringBuilder();
		int c;
		while ((c = read()) >= 0 && c != '\'')
			sb.append((char) c);
		if (c < 0)
			throw parseException("Unterminated literal string");
		return sb.toString();
	}

	String readMultiLineBasicString() throws IOException, ParseException {
		if (read() != '"' || read() != '"' || read() != '"')
			throw parseException("Expected '\"'\"'\"'\"");
		var sb = new StringBuilder();
		int c;
		boolean trimmedFirstNewline = false;
		while ((c = peek()) >= 0) {
			if (c == '"') {
				read();
				if (peek() == '"') {
					read();
					if (peek() == '"') {
						read();
						break;
					}
					sb.append('"').append('"');
				} else {
					sb.append('"');
				}
			} else if (c == '\\') {
				read();
				int next = peek();
				if (next == '\n' || next == '\r') {
					read();
					if (next == '\r' && peek() == '\n')
						read();
					while ((next = peek()) >= 0 && (next == ' ' || next == '\t' || next == '\n' || next == '\r'))
						read();
				} else if (next >= 0) {
					read();
					sb.append(processBasicEscape(next));
				}
			} else {
				read();
				if (!trimmedFirstNewline && sb.isEmpty() && (c == '\n' || c == '\r'))
					trimmedFirstNewline = true;
				else
					sb.append((char) c);
			}
		}
		if (c < 0)
			throw parseException("Unterminated multi-line string");
		return sb.toString();
	}

	private char processBasicEscape(int c) throws IOException, ParseException {
		return switch (c) {
			case 'b' -> '\b';
			case 't' -> '\t';
			case 'n' -> '\n';
			case 'f' -> '\f';
			case 'r' -> '\r';
			case '"' -> '"';
			case '\\' -> '\\';
			case 'u' -> readUnicodeEscape(4);
			case 'U' -> readUnicodeEscape(8);
			default -> throw parseException("Invalid escape: \\" + (char) c);
		};
	}

	String readMultiLineLiteralString() throws IOException, ParseException {
		if (read() != '\'' || read() != '\'' || read() != '\'')
			throw parseException("Expected \"'''\"");
		var sb = new StringBuilder();
		int c;
		boolean trimmedFirst = false;
		while ((c = peek()) >= 0) {
			if (c == '\'') {
				read();
				int p1 = peek();
				if (p1 == '\'') {
					read();
					if (peek() == '\'') {
						read();
						break;
					}
					sb.append('\'').append('\'');
				} else {
					sb.append('\'');
				}
			} else {
				read();
				if (!trimmedFirst && sb.isEmpty() && (c == '\n' || c == '\r'))
					trimmedFirst = true;
				else
					sb.append((char) c);
			}
		}
		if (c < 0)
			throw parseException("Unterminated multi-line literal string");
		return sb.toString();
	}


	private boolean peekTriple(char quote) throws IOException {
		if (peek() != quote)
			return false;
		read();
		if (peek() != quote) {
			unread(quote);
			return false;
		}
		read();
		if (peek() != quote) {
			unread(quote);
			unread(quote);
			return false;
		}
		return true;
	}

	Number readInteger() throws IOException, ParseException {
		skipWhitespace();
		int c = peek();
		boolean neg = false;
		if (c == '-') {
			read();
			neg = true;
			c = peek();
		} else if (c == '+') {
			read();
			c = peek();
		}
		if (c == '0') {
			read();
			int next = peek();
			if (next == 'x' || next == 'X') {
				read();
				long val = readHexInteger();
				return neg ? -val : val;
			}
			if (next == 'o' || next == 'O') {
				read();
				long val = readOctalInteger();
				return neg ? -val : val;
			}
			if (next == 'b' || next == 'B') {
				read();
				long val = readBinaryInteger();
				return neg ? -val : val;
			}
			unread('0');
		}
		return readDecimalInteger(neg);
	}

	private long readHexInteger() throws IOException, ParseException {
		var sb = new StringBuilder();
		while (isHexDigit(peek()) || peek() == '_') {
			int c = read();
			if (c != '_')
				sb.append((char) c);
		}
		if (sb.isEmpty())
			throw parseException("Invalid hex integer");
		try {
			return Long.parseLong(sb.toString(), 16);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException(CONST_integerOverflow + sb);
		}
	}

	private long readOctalInteger() throws IOException, ParseException {
		var sb = new StringBuilder();
		while ((peek() >= '0' && peek() <= '7') || peek() == '_') {
			int c = read();
			if (c != '_')
				sb.append((char) c);
		}
		if (sb.isEmpty())
			throw parseException("Invalid octal integer");
		try {
			return Long.parseLong(sb.toString(), 8);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException(CONST_integerOverflow + sb);
		}
	}

	private long readBinaryInteger() throws IOException, ParseException {
		var sb = new StringBuilder();
		while (peek() == '0' || peek() == '1' || peek() == '_') {
			int c = read();
			if (c != '_')
				sb.append((char) c);
		}
		if (sb.isEmpty())
			throw parseException("Invalid binary integer");
		try {
			return Long.parseLong(sb.toString(), 2);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException(CONST_integerOverflow + sb);
		}
	}

	private Number readDecimalInteger(boolean neg) throws IOException, ParseException {
		var sb = new StringBuilder();
		while (Character.isDigit(peek()) || peek() == '_') {
			int c = read();
			if (c != '_')
				sb.append((char) c);
		}
		if (sb.isEmpty())
			throw parseException("Expected integer");
		String s = sb.toString();
		if (s.length() > 1 && s.startsWith("0") && Character.isDigit(s.charAt(1)))
			throw parseException("Leading zeros not allowed in decimal integer");
		try {
			long val = Long.parseLong(s);
			return neg ? -val : val;
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException(CONST_integerOverflow + s);
		}
	}

	Number readFloat() throws IOException, ParseException {
		skipWhitespace();
		var sb = new StringBuilder();
		int c = peek();
		if (c == '+' || c == '-') {
			sb.append((char) read());
			c = peek();
		}
		while (Character.isDigit(c) || c == '_') {
			sb.append((char) read());
			c = peek();
		}
		if (c == '.') {
			sb.append((char) read());
			c = peek();
			while (Character.isDigit(c) || c == '_') {
				sb.append((char) read());
				c = peek();
			}
		}
		if (c == 'e' || c == 'E') {
			sb.append((char) read());
			c = peek();
			if (c == '+' || c == '-') {
				sb.append((char) read());
			}
			c = peek();
			while (Character.isDigit(c) || c == '_') {
				sb.append((char) read());
				c = peek();
			}
		}
		if (sb.isEmpty())
			throw parseException("Expected float");
		String s = sb.toString().replace("_", "");
		try {
			return Double.parseDouble(s);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException("Invalid float: " + s);
		}
	}

	Number readSpecialFloat() throws IOException, ParseException {
		skipWhitespace();
		var sb = new StringBuilder();
		if (peek() == '+' || peek() == '-') {
			sb.append((char) read());
		}
		while (peek() >= 0 && Character.isLetter(peek())) {
			sb.append((char) read());
		}
		String s = sb.toString().toLowerCase();
		if (s.equals("inf"))
			return Double.POSITIVE_INFINITY;
		if (s.equals("-inf"))
			return Double.NEGATIVE_INFINITY;
		if (s.equals("nan") || s.equals("-nan") || s.equals("+nan"))
			return Double.NaN;
		throw parseException("Expected inf or nan, got: " + s);
	}

	Boolean readBoolean() throws IOException, ParseException {
		skipWhitespace();
		if (peek() == 't') {
			expect("true");
			return Boolean.TRUE;
		}
		if (peek() == 'f') {
			expect("false");
			return Boolean.FALSE;
		}
		throw parseException("Expected true or false");
	}

	void expect(String expected) throws IOException, ParseException {
		for (char c : expected.toCharArray()) {
			int r = read();
			if (r < 0 || r != c)
				throw parseException("Expected '" + expected + "'");
		}
	}

	Object readDateTime() throws IOException, ParseException {
		skipWhitespace();
		var sb = new StringBuilder();
		int c;
		while ((c = peek()) >= 0 && c != '\n' && c != '\r' && c != '#' && c != ',' && c != ']' && c != '}') {
			sb.append((char) read());
		}
		String s = sb.toString().trim();
		if (s.isEmpty())
			throw parseException("Expected date-time");
		return parseDateTimeString(s);
	}

	static Object parseDateTimeString(String s) throws ParseException {
		if (s.contains("Z") || s.contains("+") || (s.contains("-") && s.lastIndexOf('-') > 10)) {
			try {
				return java.time.OffsetDateTime.parse(s.replace(" ", "T"));
			} catch (Exception e) {
				throw new ParseException("Invalid offset date-time: " + s, e);
			}
		}
		if (s.contains("T") || s.contains(" ")) {
			try {
				return java.time.LocalDateTime.parse(s.replace(" ", "T"));
			} catch (Exception e) {
				throw new ParseException("Invalid local date-time: " + s, e);
			}
		}
		if (s.contains("-") && s.length() == 10) {
			try {
				return java.time.LocalDate.parse(s);
			} catch (Exception e) {
				throw new ParseException("Invalid local date: " + s, e);
			}
		}
		if (s.contains(":")) {
			try {
				return java.time.LocalTime.parse(s);
			} catch (Exception e) {
				throw new ParseException("Invalid local time: " + s, e);
			}
		}
		throw new ParseException("Invalid date-time: " + s);
	}
}
