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
package org.apache.juneau.marshall.prototext;

import java.io.*;
import java.util.*;

import org.apache.juneau.marshall.parser.*;

/**
 * Internal tokenizer for Protobuf Text Format parsing.
 *
 * <p>
 * Character-by-character reader that handles the full Protobuf Text Format grammar:
 * identifiers, strings (with C-style escaping), numbers (decimal, hex, octal, float),
 * booleans, comments, and structural tokens.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is package-private and not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115",  // CONST_ prefix follows framework convention
	"java:S135",  // Multiple break/continue necessary for tokenizer state machine loops
	"java:S3776", // Cognitive complexity acceptable for protobuf grammar
	"java:S6541", // Brain method acceptable for tokenizer
	"resource" // Reader field is owned by the caller; the tokenizer reads from it but does not own its lifecycle.
})
class PrototextTokenizer {

	private static final String CONST_integerOverflow = "Integer overflow: ";
	private static final String CONST_invalidFloat = "Invalid float: ";

	private final Reader reader;
	private final Deque<Integer> pushback = new ArrayDeque<>();
	private int line = 1;
	private int column;
	private PrototextToken nextToken;

	PrototextTokenizer(Reader reader) {
		this.reader = reader == null ? new StringReader("") : reader;
	}

	int peekChar() throws IOException {
		if (!pushback.isEmpty())
			return pushback.peekLast();
		int c = reader.read();
		if (c >= 0)
			pushback.addLast(c);
		return c;
	}

	int readChar() throws IOException {
		if (!pushback.isEmpty()) {
			int c = pushback.removeLast();
			advancePosition(c);
			return c;
		}
		int c = reader.read();
		advancePosition(c);
		return c;
	}

	private void advancePosition(int c) {
		if (c == '\n') {
			line++;
			column = 0;
		} else if (c >= 0) {
			column++;
		}
	}

	void unread(int c) {
		if (c >= 0)
			pushback.addLast(c);
	}

	void skipWhitespaceAndComments() throws IOException {
		while (true) {
			int c = peekChar();
			if (c < 0)
				break;
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == 0x0B || c == 0x0C) {
				readChar();
				continue;
			}
			if (c == '#') {
				readChar();
			while ((c = readChar()) >= 0 && c != '\n' && c != '\r')
				{ /* consume comment characters until end of line */ }
				if (c == '\r' && peekChar() == '\n')
					readChar();
				continue;
			}
			break;
		}
	}

	PrototextToken peek() throws IOException, ParseException {
		if (nextToken == null)
			nextToken = lex();
		return nextToken;
	}

	PrototextToken read() throws IOException, ParseException {
		var t = peek();
		nextToken = null;
		return t;
	}

	private PrototextToken lex() throws IOException, ParseException {
		skipWhitespaceAndComments();
		int c = peekChar();
		if (c < 0)
			return new PrototextToken(PrototextToken.TokenType.EOF, null);

		switch (c) {
			case '{' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.LBRACE, null); }
			case '}' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.RBRACE, null); }
			case '<' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.LANGLE, null); }
			case '>' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.RANGLE, null); }
			case '[' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.LBRACKET, null); }
			case ']' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.RBRACKET, null); }
			case ':' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.COLON, null); }
			case ',' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.COMMA, null); }
			case ';' -> { readChar(); return new PrototextToken(PrototextToken.TokenType.SEMICOLON, null); }
			case '"' -> { return new PrototextToken(PrototextToken.TokenType.STRING, readDoubleQuotedString()); }
			case '\'' -> { return new PrototextToken(PrototextToken.TokenType.STRING, readSingleQuotedString()); }
			default -> { /* Fall through to number/identifier handling or throw below */ }
		}

		if (mightStartNumber(c))
			return lexNumber();
		if (isLetterOrUnderscore(c))
			return new PrototextToken(PrototextToken.TokenType.IDENT, readIdentifier());
		throw parseException("Unexpected character: " + (char) c);
	}

	private boolean mightStartNumber(int c) throws IOException {
		if (Character.isDigit(c) || c == '.')
			return true;
		if (c == '+' || c == '-') {
			readChar(); // consume sign to look ahead
			int next = peekChar();
			unread(c); // restore sign for lexNumber
			return Character.isDigit(next) || next == '.' || next == 'i' || next == 'I' || next == 'n' || next == 'N';
		}
		// Bug #4c: 'n'/'i' might start nan / inf / infinity at value position, but it might also
		// start a regular identifier-shaped field name (nano_seconds, inf, index, ...).  Multi-char
		// lookahead disambiguates: peek past the leading letter to see if the assembled token is
		// nan / inf / infinity (case-insensitive) AND is followed by a value-terminator (whitespace,
		// comma, semicolon, ], }, or EOF).  ':' is deliberately NOT a terminator — a field whose
		// name happens to be {@code inf} (or {@code nan}) is legal proto and shows up as
		// {@code inf: value}, so the colon-immediately-after case must stay on the IDENT path.
		if (c == 'i' || c == 'I' || c == 'n' || c == 'N')
			return startsSpecialFloatLiteral();
		return false;
	}

	/**
	 * Multi-char lookahead helper for {@link #mightStartNumber}.  Reads identifier-shaped chars
	 * ({@link #isIdentChar}) up to 8 deep starting at the current position, peeks the next char as
	 * the candidate terminator, then unreads everything and restores the line/column counters so
	 * the lexer can reparse the same chars freshly.  Returns <jk>true</jk> only when the assembled
	 * token (case-folded) is one of {@code nan} / {@code inf} / {@code infinity} AND the next char
	 * is a value-terminator (whitespace / {@code ,} / {@code ;} / {@code ]} / {@code }} / EOF).
	 */
	private boolean startsSpecialFloatLiteral() throws IOException {
		var savedLine = line;
		var savedCol = column;
		var buf = new StringBuilder();
		while (buf.length() <= 8) {
			int next = peekChar();
			if (next < 0 || !isIdentChar(next))
				break;
			buf.append((char) readChar());
		}
		int term = peekChar();
		var isTerminator = term < 0
			|| term == ' ' || term == '\t' || term == '\n' || term == '\r' || term == 0x0B || term == 0x0C
			|| term == ',' || term == ';' || term == ']' || term == '}';
		var tok = buf.toString().toLowerCase();
		var isSpecial = isTerminator && (tok.equals("nan") || tok.equals("inf") || tok.equals("infinity"));
		// Restore consumed chars in reverse order (pushback is a LIFO stack — to make the next read
		// return buf[0], buf[0] must be on top, so we push buf[N-1] ... buf[0]).
		for (var i = buf.length() - 1; i >= 0; i--)
			unread(buf.charAt(i));
		// readChar() advances line/column unconditionally, while unread() does NOT decrement, so we
		// must explicitly restore the pre-lookahead position counters.  Without this every multi-char
		// peek would inflate column by buf.length() before the actual lex consumed the same chars.
		line = savedLine;
		column = savedCol;
		return isSpecial;
	}

	private PrototextToken lexNumber() throws IOException, ParseException {
		int c = peekChar();
		boolean neg = false;
		if (c == '-') {
			readChar();
			neg = true;
			c = peekChar();
		} else if (c == '+') {
			readChar();
			c = peekChar();
		}
		if (c == '0') {
			readChar();
			int next = peekChar();
			if (next == 'x' || next == 'X') {
				readChar();
				long val = readHexInteger();
				return new PrototextToken(PrototextToken.TokenType.HEX_INT, neg ? -val : val);
			}
			if (next >= '0' && next <= '7') {
				long val = readOctalInteger();
				return new PrototextToken(PrototextToken.TokenType.OCT_INT, neg ? -val : val);
			}
			if (next == '.' || next == 'e' || next == 'E' || next == 'f' || next == 'F') {
				double val = readFloatLiteral(neg, "0");
				return new PrototextToken(PrototextToken.TokenType.FLOAT, val);
			}
			return new PrototextToken(PrototextToken.TokenType.DEC_INT, 0L);
		}
		if (c == '.') {
			double val = readFloatLiteral(neg, "");
			return new PrototextToken(PrototextToken.TokenType.FLOAT, val);
		}
		if (c == 'i' || c == 'I' || c == 'n' || c == 'N')
			return readSpecialFloatOrIdent(neg);
		var result = readDecimalOrFloat(neg);
		if (result instanceof Double result2)
			return new PrototextToken(PrototextToken.TokenType.FLOAT, result2);
		return new PrototextToken(PrototextToken.TokenType.DEC_INT, result);
	}

	private long readHexInteger() throws IOException, ParseException {
		var sb = new StringBuilder();
		while (isHexDigit(peekChar()))
			sb.append((char) readChar());
		if (sb.isEmpty())
			throw parseException("Invalid hex integer");
		try {
			return Long.parseLong(sb.toString(), 16);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException(CONST_integerOverflow + sb);
		}
	}

	private long readOctalInteger() throws IOException, ParseException {
		var sb = new StringBuilder("0");
		while (peekChar() >= '0' && peekChar() <= '7')
			sb.append((char) readChar());
		try {
			return Long.parseLong(sb.toString(), 8);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException(CONST_integerOverflow + sb);
		}
	}

	private Object readDecimalOrFloat(boolean neg) throws IOException, ParseException {
		var sb = new StringBuilder();
		while (Character.isDigit(peekChar()) || peekChar() == '_') {
			int ch = readChar();
			if (ch != '_')
				sb.append((char) ch);
		}
		int c = peekChar();
		if (c == '.' || c == 'e' || c == 'E') {
			if (c == '.') {
				sb.append((char) readChar());
				while (Character.isDigit(peekChar()) || peekChar() == '_') {
					int ch = readChar();
					if (ch != '_')
						sb.append((char) ch);
				}
			}
			if (peekChar() == 'e' || peekChar() == 'E') {
				sb.append((char) readChar());
				if (peekChar() == '+' || peekChar() == '-')
					sb.append((char) readChar());
				while (Character.isDigit(peekChar()) || peekChar() == '_') {
					int ch = readChar();
					if (ch != '_')
						sb.append((char) ch);
				}
			}
			if (peekChar() == 'f' || peekChar() == 'F')
				sb.append((char) readChar());
			var s = sb.toString();
			try {
				double val = Double.parseDouble(s);
				return neg ? -val : val;
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				throw parseException(CONST_invalidFloat + s);
			}
		}
		if (peekChar() == 'f' || peekChar() == 'F') {
			readChar();
			var s = sb.toString();
			try {
				double val = Double.parseDouble(s);
				return neg ? -val : val;
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				throw parseException(CONST_invalidFloat + s);
			}
		}
		if (sb.isEmpty())
			throw parseException("Expected number");
		var s = sb.toString();
		try {
			return neg ? -Long.parseLong(s) : Long.parseLong(s);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException(CONST_integerOverflow + s);
		}
	}

	/**
	 * Reads an identifier-shaped token starting with 'n'/'N'/'i'/'I' and dispatches to a FLOAT
	 * token when the lowercased form is one of the proto special-float literals (nan / inf /
	 * infinity), or to an IDENT token otherwise.
	 */
	private PrototextToken readSpecialFloatOrIdent(boolean neg) throws IOException, ParseException {
		var sb = new StringBuilder();
		while (isLetterOrUnderscore(peekChar()) || Character.isDigit(peekChar()))
			sb.append((char) readChar());
		var s = sb.toString();
		var lower = s.toLowerCase();
		if (lower.equals("inf") || lower.equals("infinity"))
			return new PrototextToken(PrototextToken.TokenType.FLOAT, neg ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
		if (lower.equals("nan"))
			return new PrototextToken(PrototextToken.TokenType.FLOAT, Double.NaN);
		if (neg)
			throw parseException("Invalid float literal: -" + s);
		return new PrototextToken(PrototextToken.TokenType.IDENT, s);
	}

	/**
	 * Reads the tail of a float literal.  The integer part (if any) is pre-supplied as
	 * {@code prefix} (e.g. {@code "0"} for the leading-zero float branch, {@code ""} for a bare
	 * {@code .5}-style literal).  The reader optionally consumes {@code .digits}, an exponent
	 * ({@code eDigits} / {@code Edigits} with optional sign), and a trailing {@code f}/{@code F}
	 * suffix.
	 *
	 * @param neg <jk>true</jk> if a leading minus sign was consumed by the caller.
	 * @param prefix The integer part already consumed by the caller (without sign).
	 * @return The parsed double value.
	 * @throws IOException If the reader cannot be read.
	 * @throws ParseException If the assembled literal is not a valid float.
	 */
	private double readFloatLiteral(boolean neg, String prefix) throws IOException, ParseException {
		var sb = new StringBuilder(neg ? "-" : "");
		sb.append(prefix);
		if (peekChar() == '.') {
			sb.append((char) readChar());
			while (Character.isDigit(peekChar()) || peekChar() == '_') {
				int ch = readChar();
				if (ch != '_')
					sb.append((char) ch);
			}
		}
		if (peekChar() == 'e' || peekChar() == 'E') {
			sb.append((char) readChar());
			if (peekChar() == '+' || peekChar() == '-')
				sb.append((char) readChar());
			while (Character.isDigit(peekChar()) || peekChar() == '_') {
				int ch = readChar();
				if (ch != '_')
					sb.append((char) ch);
			}
		}
		if (peekChar() == 'f' || peekChar() == 'F')
			readChar();
		var s = sb.toString();
		try {
			return Double.parseDouble(s);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw parseException(CONST_invalidFloat + s);
		}
	}

	String readIdentifier() throws IOException, ParseException {
		var sb = new StringBuilder();
		int c = peekChar();
		if (!isLetterOrUnderscore(c))
			throw parseException("Expected identifier");
		while (isIdentChar(peekChar()))
			sb.append((char) readChar());
		return sb.toString();
	}

	String readString() throws IOException, ParseException {
		int c = peekChar();
		if (c == '"')
			return readDoubleQuotedString();
		if (c == '\'')
			return readSingleQuotedString();
		throw parseException("Expected quoted string");
	}

	private String readDoubleQuotedString() throws IOException, ParseException {
		if (readChar() != '"')
			throw parseException("Expected '\"'");
		return readStringContent('"');
	}

	private String readSingleQuotedString() throws IOException, ParseException {
		if (readChar() != '\'')
			throw parseException("Expected \"'\"");
		return readStringContent('\'');
	}

	private String readStringContent(char quote) throws IOException, ParseException {
		var sb = new StringBuilder();
		int c;
		while ((c = readChar()) >= 0 && c != quote) {
			if (c == '\\') {
				int esc = readChar();
				if (esc < 0)
					throw parseException("Unexpected end of string");
				sb.append(processEscape(esc));
			} else {
				sb.append((char) c);
			}
		}
		if (c < 0)
			throw parseException("Unterminated string");
		return sb.toString();
	}

	private char processEscape(int c) throws IOException, ParseException {
		return switch (c) {
			case 'a' -> '\u0007';
			case 'b' -> '\b';
			case 'f' -> '\f';
			case 'n' -> '\n';
			case 'r' -> '\r';
			case 't' -> '\t';
			case 'v' -> '\u000B';
			case '?' -> '?';
			case '\\' -> '\\';
			case '\'' -> '\'';
			case '"' -> '"';
			case 'x', 'X' -> (char) readHexEscape(2);
			case '0', '1', '2', '3', '4', '5', '6', '7' -> {
				unread(c);
				yield (char) readOctalEscape();
			}
			case 'u' -> readUnicodeEscape(4);
			case 'U' -> readUnicodeEscape(8);
			default -> throw parseException("Invalid escape sequence: \\" + (char) c);
		};
	}

	private int readHexEscape(int maxLen) throws IOException, ParseException {
		var hex = new StringBuilder();
		for (int i = 0; i < maxLen && isHexDigit(peekChar()); i++)
			hex.append((char) readChar());
		if (hex.isEmpty())
			throw parseException("Invalid hex escape");
		return Integer.parseInt(hex.toString(), 16);
	}

	private int readOctalEscape() throws IOException, ParseException {
		var oct = new StringBuilder();
		for (int i = 0; i < 3 && peekChar() >= '0' && peekChar() <= '7'; i++)
			oct.append((char) readChar());
		if (oct.isEmpty())
			throw parseException("Invalid octal escape");
		return Integer.parseInt(oct.toString(), 8);
	}

	private char readUnicodeEscape(int len) throws IOException, ParseException {
		var hex = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int c = readChar();
			if (c < 0 || !isHexDigit(c))
				throw parseException("Invalid unicode escape");
			hex.append((char) c);
		}
		int cp = Integer.parseInt(hex.toString(), 16);
		if (Character.isValidCodePoint(cp))
			return (char) cp;
		throw parseException("Invalid unicode code point: " + hex);
	}

	Number readInteger() throws IOException, ParseException {
		var t = read();
		return switch (t.type()) {
			case DEC_INT, OCT_INT, HEX_INT -> t.numberValue();
			default -> throw parseException("Expected integer, got " + t.type());
		};
	}

	double readFloat() throws IOException, ParseException {
		var t = read();
		if (t.type() == PrototextToken.TokenType.FLOAT)
			return t.numberValue().doubleValue();
		if (t.type() == PrototextToken.TokenType.DEC_INT || t.type() == PrototextToken.TokenType.HEX_INT || t.type() == PrototextToken.TokenType.OCT_INT)
			return t.numberValue().doubleValue();
		throw parseException("Expected float, got " + t.type());
	}

	boolean readBoolean() throws IOException, ParseException {
		var t = read();
		if (t.type() == PrototextToken.TokenType.IDENT) {
			var s = t.stringValue().toLowerCase();
			if (s.equals("true") || s.equals("t") || s.equals("1"))
				return true;
			if (s.equals("false") || s.equals("f") || s.equals("0"))
				return false;
		}
		if (t.type() == PrototextToken.TokenType.DEC_INT) {
			long v = t.numberValue().longValue();
			if (v == 0) return false;
			if (v == 1) return true;
		}
		throw parseException("Expected boolean, got " + t.type());
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

	private static boolean isLetterOrUnderscore(int c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
	}

	private static boolean isIdentChar(int c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
	}

	private static boolean isHexDigit(int c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}
}
