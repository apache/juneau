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
package org.apache.juneau.marshall.json;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenReader} surface for the JSON format.
 *
 * <p>
 * Backed by the existing internal {@link ParserReader} plumbing.  Operates as a hand-rolled state
 * machine over the JSON grammar &mdash; structural tokens (<c>{</c>, <c>}</c>, <c>[</c>, <c>]</c>,
 * <c>,</c>, <c>:</c>) and the four scalar shapes (string, number, boolean, null).
 *
 * <p>
 * Pipe-level settings (charset, auto-close, unbuffered) are configured on the
 * {@link ParserPipe} the caller hands in.  Cursor-level behavior is driven by a small
 * {@link Settings} bundle &mdash; currently just <c>trimStrings</c>.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>Whitespace and JSON comments are tolerated &mdash; comments are silently skipped to mirror
 * 		Juneau's existing JSON parser behavior.
 * 	<li>The cursor is purely structural; object swaps and {@code @Schema} annotations have no
 * 		effect.  Use the POJO databind path on {@link JsonParser} for those.
 * </ul>
 */
@SuppressWarnings({
	"java:S3776", // Cognitive complexity acceptable for JSON tokenizer state machine
	"java:S6541", // Brain method acceptable for tokenizer dispatch
	"java:S115",  // Message-key constant uses UPPER_camelCase convention.
	"resource"    // The cursor's underlying ParserPipe is owned by the caller via try-with-resources on the cursor itself; Eclipse JDT flags the inner pipe as unclosed but that's by design.
})
public class JsonTokenReader implements TokenReader {

	/**
	 * Cursor-level settings honored by {@link JsonTokenReader}.
	 *
	 * <p>
	 * Pipe-level concerns (auto-close, unbuffered, charsets) are configured on the
	 * {@link ParserPipe} itself before construction; only behaviors that the cursor applies to
	 * already-decoded tokens live here.
	 *
	 * @param trimStrings If <jk>true</jk>, leading/trailing whitespace is trimmed from
	 * 	{@link TokenType#VALUE_STRING} and {@link TokenType#FIELD_NAME} values returned by the
	 * 	cursor.
	 */
	public record Settings(boolean trimStrings) {

		/** Default canonical setting: no string trimming. */
		public static final Settings DEFAULT = new Settings(false);
	}

	// ==============================================================================================
	// Subclass extension surface.
	//
	// The fields and methods below are part of JsonTokenReader's documented subclass-extension
	// contract.  Cross-package subclasses (Json5TokenReader in marshall.json5, JsonlTokenReader in
	// marshall.jsonl) need access to the cursor's internal state machine and per-token scratch
	// buffers in order to implement format dialects (single-quoted strings, bare identifiers,
	// trailing commas, missing values, etc.).
	//
	// Fields are protected (rather than private + accessor) because the dispatch methods
	// (readValueStart, readFieldNameOrEndObject, readCommaOrEndArray, ...) directly mutate the
	// per-token scratch and the state machine variable.  Wrapping them in get/set pairs would add
	// indirection without improving safety.  In return, subclasses MUST treat them as
	// implementation-coupled: Juneau reserves the right to evolve them across major versions.
	// ==============================================================================================

	/**
	 * Error-message format string for invalid JSON numbers.  Used with
	 * {@link #parseException(String, Object...)}.
	 */
	protected static final String MSG_invalidNumber = "Invalid JSON number: ''{0}''";

	/**
	 * Error-message format string for unexpected characters at the cursor's current position.
	 * Used with {@link #parseException(String, Object...)}.
	 */
	protected static final String MSG_unexpectedChar = "Unexpected character: ''{0}''";

	/**
	 * State-machine state: at root, after <c>[</c>, after <c>:</c>, or after <c>,</c> inside an
	 * array.  The next read should be a JSON value (object, array, or scalar).
	 */
	protected static final int S00_expectValue = 0;

	/**
	 * State-machine state: after <c>{</c> or after <c>,</c> inside an object.  The next read
	 * should be a field name (string).
	 */
	protected static final int S01_expectFieldName = 1;

	/**
	 * State-machine state: just emitted a {@link TokenType#FIELD_NAME}.  The next char should be
	 * <c>:</c>.
	 */
	protected static final int S02_expectColon = 2;

	/**
	 * State-machine state: just emitted a value inside an object.  The next char should be
	 * <c>,</c> (followed by another field name) or <c>}</c>.
	 */
	protected static final int S03_expectCommaOrEndObject = 3;

	/**
	 * State-machine state: just emitted a value inside an array.  The next char should be
	 * <c>,</c> (followed by another value) or <c>]</c>.
	 */
	protected static final int S04_expectCommaOrEndArray = 4;

	/**
	 * State-machine state: the root document has been fully consumed.  The next call to
	 * {@link #next()} returns {@link TokenType#END_OF_STREAM}.
	 */
	protected static final int S05_end = 5;

	/** Underlying character-level reader; subclasses use this for direct {@code read}/{@code unread}/{@code mark}/{@code getMarked} calls during dialect-specific tokenization. */
	protected final ParserReader r;

	private final ParserPipe pipe;

	/** Cursor-level settings (currently just <c>trimStrings</c>). */
	protected final Settings settings;

	private final JsonParserSession session;

	/** The token most recently emitted by {@link #next()}.  Subclasses set this when overriding state-machine dispatch methods. */
	protected TokenType currentToken = TokenType.NOT_AVAILABLE;

	/** Current state-machine state; one of the {@code STATE_*} constants. */
	protected int state = S00_expectValue;

	// Container stack: 0 bits = object, 1 bits = array.  Limited to 64 levels of nesting
	// (tracked via {@link #depth}); deeper documents fall back to a heap-allocated overflow array.
	private long containerBits;
	private boolean[] containerOverflow;

	/** Current container nesting depth (0 = root). */
	protected int depth;

	/** Field name buffer for the current {@link TokenType#FIELD_NAME} token. */
	protected String currentFieldName;

	/** String value buffer for the current {@link TokenType#VALUE_STRING} token. */
	protected String currentString;

	/** Numeric lexeme buffer for the current {@link TokenType#VALUE_NUMBER} token (preserved verbatim from the input). */
	protected String currentNumberLexeme;

	private Number currentNumber;

	/** Boolean value buffer for the current {@link TokenType#VALUE_BOOLEAN} token. */
	protected boolean currentBoolean;

	/**
	 * Constructor with default {@link Settings}.
	 *
	 * @param pipe The parser input pipe to read from.  Must not be <jk>null</jk>.
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public JsonTokenReader(ParserPipe pipe) throws IOException {
		this(pipe, Settings.DEFAULT, null);
	}

	/**
	 * Constructor with explicit {@link Settings}.
	 *
	 * @param pipe The parser input pipe to read from.  Must not be <jk>null</jk>.
	 * @param settings The cursor-level settings.  Must not be <jk>null</jk>.
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public JsonTokenReader(ParserPipe pipe, Settings settings) throws IOException {
		this(pipe, settings, null);
	}

	/**
	 * Constructor used by {@link JsonParser#parseTokens(Object)} to plumb the calling parser's
	 * session in so that {@link #read(Class)} can delegate to the existing databind path.
	 *
	 * @param pipe The parser input pipe to read from.  Must not be <jk>null</jk>.
	 * @param settings The cursor-level settings.  Must not be <jk>null</jk>.
	 * @param session The {@link JsonParserSession} to use for {@link #read(Class)} delegation,
	 * 	or <jk>null</jk> to disable {@code read} on this reader (it will throw
	 * 	{@link UnsupportedOperationException}).
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public JsonTokenReader(ParserPipe pipe, Settings settings, JsonParserSession session) throws IOException {
		this.pipe = pipe;
		this.r = pipe.getParserReader();
		this.settings = settings;
		this.session = session;
	}

	// ==============================================================================================
	// State-machine summary.  next() reads the next non-whitespace, non-comment character from the
	// underlying ParserReader and dispatches to one of the readXxx() methods below based on the
	// cursor's current state.  Each readXxx() method is overridable by subclasses to add
	// dialect-specific behavior.
	//
	//   S00_expectValue                 - At root, after [, after :, after , inside [.
	//                                        Expecting a JSON value: { [ " digit t f n.
	//                                        Dispatches to readValueStart(c).
	//   S01_expectFieldName             - After { or after , inside {.
	//                                        Expecting a quoted string field name (or }).
	//                                        Dispatches to readFieldNameOrEndObject(c).
	//   S02_expectColon                 - Just emitted a FIELD_NAME.  Expecting :.
	//                                        Handled inline by readColonThenValue(c).
	//   S03_expectCommaOrEndObject      - Just emitted a value inside {.  Expecting , or }.
	//                                        Dispatches to readCommaOrEndObject(c).
	//   S04_expectCommaOrEndArray       - Just emitted a value inside [.  Expecting , or ].
	//                                        Dispatches to readCommaOrEndArray(c).
	//   S05_end                         - Root document fully consumed.  next() returns
	//                                        END_OF_STREAM.
	//
	// State transitions are performed by afterValue() (sets the post-value state based on the
	// enclosing container) and by the dispatch methods themselves.
	// ==============================================================================================
	@Override /* Overridden from TokenReader */
	public TokenType next() throws IOException, ParseException {
		if (state == S05_end) {
			currentToken = TokenType.END_OF_STREAM;
			return currentToken;
		}

		// Reset per-token scratch.
		currentFieldName = null;
		currentString = null;
		currentNumberLexeme = null;
		currentNumber = null;

		int c = readSkipWsAndComments();
		if (c == -1) {
			if (state != S00_expectValue || depth != 0)
				throw parseException("Unexpected end of input");
			state = S05_end;
			currentToken = TokenType.END_OF_STREAM;
			return currentToken;
		}

		switch (state) {
			case S00_expectValue                 -> readValueStart(c);
			case S01_expectFieldName             -> readFieldNameOrEndObject(c);
			case S02_expectColon                 -> readColonThenValue(c);
			case S03_expectCommaOrEndObject      -> readCommaOrEndObject(c);
			case S04_expectCommaOrEndArray       -> readCommaOrEndArray(c);
			default                              -> throw parseException("Internal tokenizer state error");
		}
		return currentToken;
	}

	@Override /* Overridden from TokenReader */
	public TokenType getCurrentToken() {
		return currentToken;
	}

	@Override /* Overridden from TokenReader */
	public int getDepth() {
		return depth;
	}

	@Override /* Overridden from TokenReader */
	public String getFieldName() {
		if (currentToken != TokenType.FIELD_NAME)
			throw new IllegalStateException("Current token is not FIELD_NAME (was " + currentToken + ")");
		return currentFieldName;
	}

	@Override /* Overridden from TokenReader */
	public String getString() {
		switch (currentToken) {
			case VALUE_STRING:    return currentString;
			case FIELD_NAME:      return currentFieldName;
			case VALUE_NUMBER:    return currentNumberLexeme;
			case VALUE_BOOLEAN:   return Boolean.toString(currentBoolean);
			case VALUE_NULL:      return null;
			default: throw new IllegalStateException(
				"Current token does not have a string view (was " + currentToken + ")");
		}
	}

	@Override /* Overridden from TokenReader */
	public Number getNumber() throws ParseException {
		if (currentToken != TokenType.VALUE_NUMBER)
			throw new IllegalStateException(
				"Current token is not VALUE_NUMBER (was " + currentToken + ")");
		if (currentNumber == null)
			currentNumber = StringUtils.parseNumber(currentNumberLexeme, null);
		return currentNumber;
	}

	@Override /* Overridden from TokenReader */
	public String getNumberLexeme() {
		if (currentToken != TokenType.VALUE_NUMBER)
			throw new IllegalStateException(
				"Current token is not VALUE_NUMBER (was " + currentToken + ")");
		return currentNumberLexeme;
	}

	@Override /* Overridden from TokenReader */
	public boolean getBool() {
		if (currentToken != TokenType.VALUE_BOOLEAN)
			throw new IllegalStateException(
				"Current token is not VALUE_BOOLEAN (was " + currentToken + ")");
		return currentBoolean;
	}

	@Override /* Overridden from TokenReader */
	public byte[] getBinary() {
		throw new IllegalStateException("JSON does not produce VALUE_BINARY tokens");
	}

	@Override /* Overridden from TokenReader */
	public boolean canRead() throws IOException, ParseException {
		// Auto-advance through structural separators that don't themselves emit value tokens.
		// This lets users write `while (r.canRead()) bean = r.read(...)` for array
		// streaming without manually consuming commas.
		advanceToValueState();
		if (state != S00_expectValue)
			return false;
		// At S00_expectValue the next non-whitespace char might still be a closer (empty
		// container).  Peek to disambiguate.
		var c = readSkipWsAndComments();
		if (c == -1)
			return false;
		r.unread();
		return c != ']' && c != '}';
	}

	/**
	 * Helper for {@link #canRead()} and {@link #read(ClassMeta)}: if the cursor is
	 * sitting on a structural separator (comma between array elements, colon after a field name),
	 * consume it so the cursor lands at S00_expectValue.  No-op if already at a value
	 * boundary or at end-of-container.
	 */
	private void advanceToValueState() throws IOException, ParseException {
		while (true) {
			switch (state) {
				case S04_expectCommaOrEndArray: {
					var c = readSkipWsAndComments();
					if (c == -1 || c == ']') {
						if (c != -1) r.unread();
						return;
					}
					if (c != ',')
						throw parseException("Expected ',' or ']' but got ''{0}''", (char) c);
					// If a trailing-comma case follows (next non-ws is ']'), keep the comma-or-end
					// state so a subsequent next() closes the container correctly.  Format-specific
					// hook: plain JSON rejects trailing commas, JSON5 accepts.
					var c2 = readSkipWsAndComments();
					if (c2 == ']') {
						r.unread();
						if (!allowsTrailingComma())
							throw parseException("Trailing comma not allowed in this format");
						return;  // state stays S04_expectCommaOrEndArray
					}
					if (c2 != -1) r.unread();
					state = S00_expectValue;
					break;
				}
				case S03_expectCommaOrEndObject: {
					var c = readSkipWsAndComments();
					if (c == -1 || c == '}') {
						if (c != -1) r.unread();
						return;
					}
					if (c != ',')
						throw parseException("Expected ',' or '}' but got ''{0}''", (char) c);
					var c2 = readSkipWsAndComments();
					if (c2 == '}') {
						r.unread();
						if (!allowsTrailingComma())
							throw parseException("Trailing comma not allowed in this format");
						return;  // state stays S03_expectCommaOrEndObject
					}
					if (c2 != -1) r.unread();
					// After a comma inside an object, the next thing is a field name, not a value.
					state = S01_expectFieldName;
					return;
				}
				case S02_expectColon: {
					var c = readSkipWsAndComments();
					if (c == -1)
						return;
					if (c != ':')
						throw parseException("Expected ':' after field name but got ''{0}''", (char) c);
					state = S00_expectValue;
					break;
				}
				default:
					return;
			}
		}
	}

	/**
	 * Subclass extension point: returns whether trailing commas are accepted before <c>]</c> /
	 * <c>}</c>.
	 *
	 * <p>
	 * Default returns <jk>false</jk> (strict RFC-8259 JSON).  Json5TokenReader overrides this to
	 * return <jk>true</jk> for the JSON5 dialect.
	 *
	 * @return <jk>true</jk> if a trailing comma should be silently accepted; <jk>false</jk> if it
	 * 	should produce a parse error.
	 */
	protected boolean allowsTrailingComma() {
		return false;
	}

	@Override /* Overridden from TokenReader */
	public <T> T read(Class<T> type) throws IOException, ParseException {
		return read(session().getClassMeta(type));
	}

	@Override /* Overridden from TokenReader */
	public <T> T read(ClassMeta<T> type) throws IOException, ParseException {
		var s = session();
		advanceToValueState();
		assertReadValueState();
		try {
			T o = s.parseAnything(type, r, null, null);
			markValueConsumed();
			return o;
		} catch (Exception e) {
			if (e instanceof IOException ioe) throw ioe;
			if (e instanceof ParseException pe) throw pe;
			throw new ParseException(s, "read failed: {0}", e.getMessage());
		}
	}

	@Override /* Overridden from TokenReader */
	public <T> T read(Type type, Type... args) throws IOException, ParseException {
		return read(session().<T>getClassMeta(type, args));
	}

	private JsonParserSession session() {
		if (session == null)
			throw new UnsupportedOperationException(
				"read is not available on this JsonTokenReader (no session attached). " +
				"Open the cursor via JsonParser.parseTokens(...) to enable read.");
		return session;
	}

	/**
	 * Validates that the cursor is positioned at a value boundary (i.e. the next emitted token
	 * would be value-shaped).
	 */
	private void assertReadValueState() {
		if (state != S00_expectValue)
			throw new IllegalStateException(MSG_READ_NON_VALUE_STATE);
	}

	/**
	 * Resyncs the cursor's state machine after {@code parseAnything} consumed a value.  The cursor's
	 * depth does not change (the value was contained at the current depth); only the post-value
	 * separator-expectation state needs to update.
	 */
	private void markValueConsumed() {
		// We did not surface an explicit token to the caller, but functionally this is "a value
		// just got emitted at the current depth," so the same post-value transition that next()
		// would have performed applies.
		afterValue();
		// The current token slot is meaningless after read (the caller has the bound POJO).
		// Set a sentinel so accidental getStringValue() / getCurrentToken() usage doesn't return
		// a stale prior token's value.
		currentToken = TokenType.NOT_AVAILABLE;
	}

	@Override /* Overridden from TokenReader */
	public void skipChildren() throws IOException, ParseException {
		if (currentToken != TokenType.START_OBJECT && currentToken != TokenType.START_ARRAY)
			return;
		var startDepth = depth - 1;
		while (depth > startDepth) {
			var t = next();
			if (t == TokenType.END_OF_STREAM)
				throw parseException("Unexpected end of input while skipping children");
		}
	}

	@Override /* Overridden from TokenReader */
	public void close() throws IOException {
		pipe.close();
	}

	// =================================================================================
	// State-machine dispatch
	// =================================================================================

	/**
	 * State-machine dispatch: handles the next character at {@link #S00_expectValue}.
	 *
	 * <p>
	 * Subclasses (e.g. Json5TokenReader) override this to add dialect-specific value shapes
	 * (single-quoted strings, bare identifiers, missing-value handling).
	 *
	 * @param c The next non-whitespace, non-comment character read from the input.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed at this position.
	 */
	protected void readValueStart(int c) throws IOException, ParseException {
		switch (c) {
			case '{':
				pushContainer(false);
				currentToken = TokenType.START_OBJECT;
				state = S01_expectFieldName;
				return;
			case '[':
				pushContainer(true);
				currentToken = TokenType.START_ARRAY;
				state = S00_expectValue;
				return;
			case '"':
				r.unread();
				currentString = maybeTrim(readString());
				currentToken = TokenType.VALUE_STRING;
				afterValue();
				return;
			case 't', 'f', 'n':
				r.unread();
				readKeywordValue(c);
				afterValue();
				return;
			case ']':
				if (depth > 0 && isInArray()) {
					popContainer();
					currentToken = TokenType.END_ARRAY;
					afterValue();
					return;
				}
				throw parseException(MSG_unexpectedChar, (char) c);
			default:
				if (isNumberStart(c)) {
					r.unread();
					readNumberValue();
					afterValue();
					return;
				}
				throw parseException(MSG_unexpectedChar, (char) c);
		}
	}

	/**
	 * State-machine dispatch: handles the next character at {@link #S01_expectFieldName}.
	 *
	 * <p>
	 * Subclasses (e.g. Json5TokenReader) override this to accept bare-identifier field names and
	 * single-quoted field names.
	 *
	 * @param c The next non-whitespace character read from the input.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed at this position.
	 */
	protected void readFieldNameOrEndObject(int c) throws IOException, ParseException {
		if (c == '}') {
			popContainer();
			currentToken = TokenType.END_OBJECT;
			afterValue();
			return;
		}
		if (c != '"')
			throw parseException("Expected '\"' for field name but got ''{0}''", (char) c);
		r.unread();
		currentFieldName = maybeTrim(readString());
		currentToken = TokenType.FIELD_NAME;
		state = S02_expectColon;
	}

	private String maybeTrim(String s) {
		if (!settings.trimStrings || s == null)
			return s;
		return s.trim();
	}

	private void readColonThenValue(int c) throws IOException, ParseException {
		if (c != ':')
			throw parseException("Expected ':' after field name but got ''{0}''", (char) c);
		var c2 = readSkipWsAndComments();
		if (c2 == -1)
			throw parseException("Unexpected end of input after ':'");
		state = S00_expectValue;
		readValueStart(c2);
	}

	/**
	 * State-machine dispatch: handles the next character at
	 * {@link #S03_expectCommaOrEndObject}.
	 *
	 * <p>
	 * Subclasses (e.g. Json5TokenReader) override this to accept trailing commas before <c>}</c>.
	 *
	 * @param c The next non-whitespace character read from the input.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed at this position.
	 */
	protected void readCommaOrEndObject(int c) throws IOException, ParseException {
		if (c == '}') {
			popContainer();
			currentToken = TokenType.END_OBJECT;
			afterValue();
			return;
		}
		if (c != ',')
			throw parseException("Expected ',' or '}' but got ''{0}''", (char) c);
		var c2 = readSkipWsAndComments();
		if (c2 == -1)
			throw parseException("Unexpected end of input after ','");
		// After a comma inside an object the next thing must be a field name (or '}' is illegal here).
		state = S01_expectFieldName;
		readFieldNameOrEndObject(c2);
	}

	/**
	 * State-machine dispatch: handles the next character at
	 * {@link #S04_expectCommaOrEndArray}.
	 *
	 * <p>
	 * Subclasses (e.g. Json5TokenReader) override this to accept trailing commas before <c>]</c>.
	 *
	 * @param c The next non-whitespace character read from the input.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed at this position.
	 */
	protected void readCommaOrEndArray(int c) throws IOException, ParseException {
		if (c == ']') {
			popContainer();
			currentToken = TokenType.END_ARRAY;
			afterValue();
			return;
		}
		if (c != ',')
			throw parseException("Expected ',' or ']' but got ''{0}''", (char) c);
		var c2 = readSkipWsAndComments();
		if (c2 == -1)
			throw parseException("Unexpected end of input after ','");
		state = S00_expectValue;
		readValueStart(c2);
	}

	/**
	 * Sets the post-value state based on the enclosing container.
	 *
	 * <p>
	 * Subclasses (e.g. JsonlTokenReader) override this when the format treats consecutive
	 * top-level values differently &mdash; JSONL transitions back to {@link #S00_expectValue}
	 * at depth 0 instead of {@link #S05_end} so the next line is read.
	 */
	protected void afterValue() {
		if (depth == 0)
			state = S05_end;
		else if (isInArray())
			state = S04_expectCommaOrEndArray;
		else
			state = S03_expectCommaOrEndObject;
	}

	// =================================================================================
	// Scalar readers
	// =================================================================================

	/**
	 * Reads a double-quoted JSON string from the current input position.
	 *
	 * <p>
	 * Handles escape sequences (<c>\\n</c>, <c>\\t</c>, <c>\\u<i>NNNN</i></c>, etc.) inline via
	 * {@link ParserReader#replace(char)}.  Subclasses (e.g. Json5TokenReader) override this to
	 * also accept single-quoted strings.
	 *
	 * @return The parsed string (without surrounding quotes).
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed at this position.
	 */
	protected String readString() throws IOException, ParseException {
		r.mark();
		var qc = r.read();
		if (qc != '"')
			throw parseException("Did not find quote character marking beginning of string");
		String s = null;
		var inEscape = false;
		var c = 0;
		while (c != -1) {
			c = r.read();
			if (c <= 0x1F && c != -1)
				throw parseException("Unescaped control character encountered: ''0x{0}''", String.format("%04X", c));
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

	/**
	 * Reads one of the JSON keyword literals (<c>true</c>, <c>false</c>, <c>null</c>) starting
	 * with the given lookahead character.  Sets {@link #currentToken} and {@link #currentBoolean}
	 * accordingly.
	 *
	 * @param c The first character of the keyword (one of <c>'t'</c>, <c>'f'</c>, <c>'n'</c>).
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the keyword is malformed.
	 */
	protected void readKeywordValue(int c) throws IOException, ParseException {
		switch (c) {
			case 't':
				expectLiteral("true");
				currentBoolean = true;
				currentToken = TokenType.VALUE_BOOLEAN;
				return;
			case 'f':
				expectLiteral("false");
				currentBoolean = false;
				currentToken = TokenType.VALUE_BOOLEAN;
				return;
			case 'n':
				expectLiteral("null");
				currentToken = TokenType.VALUE_NULL;
				return;
			default:
				throw parseException(MSG_unexpectedChar, (char) c);
		}
	}

	private void expectLiteral(String literal) throws IOException, ParseException {
		var s = r.read(literal.length());
		if (!literal.equals(s))
			throw parseException("Unrecognized syntax.  Expected=''{0}'', Actual=''{1}''", literal, s);
	}

	/**
	 * Reads a JSON number from the current input position.  Captures the lexeme via
	 * {@link ParserReader#parseNumberString()} and validates it; defers actual numeric parsing
	 * to {@link #getNumber()} (lazy).  Sets {@link #currentNumberLexeme} and {@link #currentToken}.
	 *
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is not a valid JSON number.
	 */
	protected void readNumberValue() throws IOException, ParseException {
		// Mirror JsonParserSession#parseNumber's lexeme-collection: collect "number chars" then
		// hand off to StringUtils.parseNumber.  The lexeme is preserved as currentNumberLexeme so
		// precision-sensitive consumers can recover the original textual form (resolved Q6).
		var s = r.parseNumberString();
		validateNumberLexeme(s);
		currentNumberLexeme = s;
		currentToken = TokenType.VALUE_NUMBER;
		// Number itself is parsed lazily on first getNumberValue() call.
	}

	private void validateNumberLexeme(String s) throws ParseException {
		if (s.isEmpty())
			throw parseException(MSG_invalidNumber, s);
		var isNegative = false;
		var c = s.charAt(0);
		if (c == '-') {
			isNegative = true;
			c = (s.length() == 1 ? 'x' : s.charAt(1));
		}
		if (c == '.')
			throw parseException(MSG_invalidNumber, s);
		if (c == '0' && s.length() > (isNegative ? 2 : 1)) {
			var c2 = s.charAt(isNegative ? 2 : 1);
			if (c2 != '.' && c2 != 'e' && c2 != 'E')
				throw parseException(MSG_invalidNumber, s);
		}
		var i = s.indexOf('.');
		if (i != -1 && (s.length() == (i + 1) || !isDigit(s.charAt(i + 1))))
			throw parseException(MSG_invalidNumber, s);
	}

	// =================================================================================
	// Reader helpers
	// =================================================================================

	/**
	 * Reads the next non-whitespace, non-comment character or -1 at end of stream.
	 *
	 * @return The next significant character, or <c>-1</c> at end of input.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If a JSON comment is malformed.
	 */
	protected int readSkipWsAndComments() throws IOException, ParseException {
		while (true) {
			var c = r.read();
			if (c == -1)
				return -1;
			if (c == '/')
				skipComment();
			else if (!Character.isWhitespace(c))
				return c;
		}
	}

	@SuppressWarnings({
		"java:S2677" // read() return intentionally discarded: consuming the already-peeked '/' that closes a block comment.
	})
	private void skipComment() throws IOException, ParseException {
		var c = r.read();
		if (c == '*') {
			while (true) {
				c = r.read();
				if (c == -1)
					throw parseException("Unterminated block comment");
				if (c == '*' && r.peek() == '/') {
					r.read();
					return;
				}
			}
		}
		if (c == '/') {
			while (true) {
				c = r.read();
				if (c == -1 || c == '\n')
					return;
			}
		}
		throw parseException("Open ended comment");
	}

	/**
	 * Returns whether the given character could start a JSON number lexeme (digit or minus sign).
	 *
	 * @param c The character.
	 * @return <jk>true</jk> if the character could start a number.
	 */
	protected static boolean isNumberStart(int c) {
		return c == '-' || (c >= '0' && c <= '9');
	}

	/**
	 * Returns whether the given character is an ASCII decimal digit.
	 *
	 * @param c The character.
	 * @return <jk>true</jk> if the character is between <c>'0'</c> and <c>'9'</c> inclusive.
	 */
	protected static boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}

	// =================================================================================
	// Container stack
	// =================================================================================

	/**
	 * Pushes a new container onto the cursor's depth stack.
	 *
	 * @param isArray <jk>true</jk> if the new container is an array; <jk>false</jk> for an object.
	 */
	protected void pushContainer(boolean isArray) {
		if (depth < 64) {
			if (isArray)
				containerBits |= (1L << depth);
			else
				containerBits &= ~(1L << depth);
		} else {
			if (containerOverflow == null || depth - 64 >= containerOverflow.length)
				growOverflow();
			containerOverflow[depth - 64] = isArray;
		}
		depth++;
	}

	/**
	 * Pops the top container off the cursor's depth stack.
	 *
	 * @throws ParseException If the stack is already empty (defensive only; well-formed JSON
	 * 	cannot trigger this).
	 */
	protected void popContainer() throws ParseException {
		if (depth == 0)
			throw parseException("Container stack underflow");
		depth--;
	}

	/**
	 * Returns whether the cursor's current container (the one most recently pushed) is an array.
	 *
	 * @return <jk>true</jk> if currently inside an array; <jk>false</jk> if inside an object.
	 */
	protected boolean isInArray() {
		var top = depth - 1;
		if (top < 64)
			return (containerBits & (1L << top)) != 0;
		return containerOverflow[top - 64];
	}

	private void growOverflow() {
		var n = containerOverflow == null ? 16 : containerOverflow.length << 1;
		var bigger = new boolean[n];
		if (containerOverflow != null)
			System.arraycopy(containerOverflow, 0, bigger, 0, containerOverflow.length);
		containerOverflow = bigger;
	}

	/**
	 * Builds a {@link ParseException} with the given message + {@link java.text.MessageFormat}-style
	 * arguments.  Used by all dispatch methods to surface input-malformed errors with consistent
	 * formatting.
	 *
	 * @param message The exception message format string ({0}, {1}, ...).
	 * @param args The format arguments.
	 * @return A new {@link ParseException} ready to throw.
	 */
	protected ParseException parseException(String message, Object... args) {
		return new ParseException(message, args);
	}
}
