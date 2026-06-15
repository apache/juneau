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
package org.apache.juneau.marshall.cbor;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenReader} surface for the CBOR format
 * (RFC 8949).
 *
 * <p>
 * Backed by the existing internal {@link CborInputStream} plumbing.  CBOR's wire format is already
 * token-shaped (major-type encoded data items), so this cursor maps almost directly onto
 * {@link CborInputStream#readDataType()}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>The cursor is a true O(1)-memory streaming cursor ({@link #isStreaming()} == <jk>true</jk>).
 * 	<li>The cursor is purely structural; object swaps and {@code @Schema} annotations have no
 * 		effect.  Use the POJO databind path on {@link CborParser} for those.
 * </ul>
 */
@SuppressWarnings({
	"resource" // The cursor's underlying ParserPipe is owned by the caller via try-with-resources on the cursor itself; Eclipse JDT flags the inner pipe as unclosed but that's by design.
})
public class CborTokenReader implements TokenReader {

	private static final long INDEFINITE = -1L;

	/**
	 * Default maximum number of nested CBOR semantic tags (major type 6) the cursor will unwrap
	 * before throwing a {@link ParseException}.
	 *
	 * <p>
	 * CBOR allows a value to be wrapped in an arbitrary number of semantic tags
	 * (e.g. <c>0xC0 0xC0 ... value</c>).  Each tag is unwrapped recursively by {@link #next()}, so a
	 * pathologically deep chain of tags would otherwise blow the JVM call stack with a
	 * {@link StackOverflowError}.  This bound converts that failure mode into a clean, catchable
	 * {@link ParseException}.
	 */
	static final int DEFAULT_MAX_TAG_NESTING_DEPTH = 64;

	private final CborInputStream is;
	private final ParserPipe pipe;
	private final CborParserSession session;

	private int maxTagNestingDepth = DEFAULT_MAX_TAG_NESTING_DEPTH;
	// Current depth of the in-progress recursive tag unwrap (reset to 0 once a value is emitted).
	private int tagNestingDepth;

	private TokenType currentToken = TokenType.NOT_AVAILABLE;
	private boolean ended;

	// Container stack: parallel arrays — kind (0=array, 1=map) + remaining element count
	// (INDEFINITE for indefinite-length).  For maps the count covers key+value pairs at 2 each.
	private long[] stackRemaining = new long[16];
	private boolean[] stackIsMap = new boolean[16];
	// Per-level: at map-level, true when next emit should be a FIELD_NAME (key); false when next
	// emit should be a value.  Stored per-level so nested containers don't corrupt each other.
	private boolean[] stackAwaitingKey = new boolean[16];
	private int depth;

	// Scratch state for current token's value.
	private String currentString;
	private String currentNumberLexeme;
	private Number currentNumber;
	private boolean currentBoolean;
	private byte[] currentBinary;

	/**
	 * Constructor with default settings.
	 *
	 * @param pipe The parser input pipe.  Must not be <jk>null</jk>.
	 * @throws IOException If a problem occurred opening the underlying stream.
	 */
	public CborTokenReader(ParserPipe pipe) throws IOException {
		this(pipe, null);
	}

	/**
	 * Constructor used by {@link CborParserSession#parseTokens(Object)} to plumb the calling
	 * session through so that {@link #read(Class)} can delegate to the CBOR databind path.
	 *
	 * @param pipe The parser input pipe.  Must not be <jk>null</jk>.
	 * @param session The {@link CborParserSession} for {@link #read(Class)} delegation, or
	 * 	<jk>null</jk> to disable {@code read}.
	 * @throws IOException If a problem occurred opening the underlying stream.
	 */
	public CborTokenReader(ParserPipe pipe, CborParserSession session) throws IOException {
		this.pipe = pipe;
		this.is = new CborInputStream(pipe);
		this.session = session;
	}

	/**
	 * Sets the maximum number of nested CBOR semantic tags this cursor will unwrap before throwing
	 * a {@link ParseException}.
	 *
	 * <p>
	 * Defaults to {@link #DEFAULT_MAX_TAG_NESTING_DEPTH} (64).  A chain of more than this many tags
	 * wrapping a single value causes {@link #next()} to throw rather than risk a
	 * {@link StackOverflowError}.
	 *
	 * @param value The new maximum nesting depth.  Must be {@code >= 0}.
	 * @return This object.
	 */
	public CborTokenReader setMaxTagNestingDepth(int value) {
		maxTagNestingDepth = value;
		return this;
	}

	// ==============================================================================================
	// State-machine summary.  CBOR is already a token-shaped wire format (RFC 8949 major types), so
	// next() reads the next data-type tag via CborInputStream.readDataType() and dispatches based
	// on three things: (1) whether we're at end-of-input, (2) whether a definite-length container
	// has exhausted its element count (emit END_*), or whether the data type is BREAK (also emit
	// END_*), and (3) within a map, whether we're at a key position or value position.
	//
	// Per-level container state lives on parallel stacks (stackRemaining + stackIsMap +
	// stackAwaitingKey, indexed by depth).  A stackRemaining value of -1 means indefinite-length;
	// in that case the next() loop relies on CborInputStream returning DataType.BREAK instead of
	// counting elements.
	// ==============================================================================================
	@Override /* TokenReader */
	@SuppressWarnings({
		"java:S3776" // Token-reader state machine; complexity is inherent to the format decode loop.
	})
	public TokenType next() throws IOException, ParseException {
		if (ended) {
			currentToken = TokenType.END_OF_STREAM;
			return currentToken;
		}
		// Reset scratch.
		currentString = null;
		currentNumberLexeme = null;
		currentNumber = null;
		currentBinary = null;

		// Inside a definite-length container at element-count zero: emit END_*.
		if (depth > 0 && stackRemaining[depth - 1] == 0) {
			popContainer();
			return currentToken;
		}

		DataType dt;
		try {
			dt = is.readDataType();
		} catch (IOException e) {
			if (depth == 0) {
				ended = true;
				currentToken = TokenType.END_OF_STREAM;
				return currentToken;
			}
			throw e;
		}

		// Inside an indefinite-length container, BREAK closes it.
		if (dt == DataType.BREAK) {
			if (depth == 0 || stackRemaining[depth - 1] != INDEFINITE)
				throw new ParseException("Unexpected BREAK token outside indefinite-length container");
			popContainer();
			return currentToken;
		}

		// Map key position: CBOR keys can be any data type, but the token vocabulary expects
		// FIELD_NAME with a string view.  Convert string keys directly; coerce other key types
		// to their toString() form (matches the databind path's behavior).
		if (depth > 0 && stackIsMap[depth - 1] && stackAwaitingKey[depth - 1]) {
			currentToken = TokenType.FIELD_NAME;
			currentString = readScalarAsString(dt);
			consumedOneElement();  // flips awaiting-key: key->value, decrements remaining count
			return currentToken;
		}

		// Value position (in array, in map after key, or at root).
		switch (dt) {
			case ARRAY -> {
				pushContainer(false, is.readLength());
				currentToken = TokenType.START_ARRAY;
			}
			case MAP -> {
				var n = is.readLength();
				// MAP length is pair-count; track it as 2*n element-counts.
				pushContainer(true, n == INDEFINITE ? INDEFINITE : n * 2);
				currentToken = TokenType.START_OBJECT;
				// First emit inside the map will be a key; pushContainer already set this
				// for map-level entries.
			}
			case UINT, NINT -> {
				var v = is.readSignedLong();
				currentNumber = v;
				currentNumberLexeme = Long.toString(v);
				currentToken = TokenType.VALUE_NUMBER;
				consumedOneElement();
			}
			case FLOAT -> {
				var v = is.readDouble();
				currentNumber = v;
				currentNumberLexeme = Double.toString(v);
				currentToken = TokenType.VALUE_NUMBER;
				consumedOneElement();
			}
			case BOOLEAN -> {
				currentBoolean = is.readBoolean();
				currentToken = TokenType.VALUE_BOOLEAN;
				consumedOneElement();
			}
			case NULL, UNDEFINED -> {
				currentToken = TokenType.VALUE_NULL;
				consumedOneElement();
			}
			case STRING -> {
				currentString = is.readString();
				currentToken = TokenType.VALUE_STRING;
				consumedOneElement();
			}
			case BINARY -> {
				currentBinary = is.readBinary();
				currentToken = TokenType.VALUE_BINARY;
				consumedOneElement();
			}
			case TAG, SIMPLE -> {
				// Q3: by default normalize to common scalars.  TAG: skip the tag and read the
				// next item as the wrapped value.  SIMPLE: emit as VALUE_NULL (best-effort).
				if (dt == DataType.TAG) {
					// Guard against pathologically deep tag chains blowing the JVM stack.
					if (tagNestingDepth >= maxTagNestingDepth)
						throw new ParseException(
							"CBOR tag nesting depth exceeded the maximum of {0}.  Increase the limit via CborTokenReader.setMaxTagNestingDepth(int) if this is intentional.",
							maxTagNestingDepth);
					tagNestingDepth++;
					try {
						return next();  // consume the wrapped value
					} finally {
						tagNestingDepth--;
					}
				}
				currentToken = TokenType.VALUE_NULL;
				consumedOneElement();
			}
			default -> throw new ParseException("Unexpected CBOR data type: {0}", dt);
		}
		return currentToken;
	}

	private String readScalarAsString(DataType dt) throws IOException, ParseException {
		switch (dt) {
			case STRING:    return is.readString();
			case UINT, NINT: return Long.toString(is.readSignedLong());
			case BINARY: {
				var b = is.readBinary();
				return new String(b, java.nio.charset.StandardCharsets.UTF_8);
			}
			case BOOLEAN:   return Boolean.toString(is.readBoolean());
			case FLOAT:     return Double.toString(is.readDouble());
			default:
				throw new ParseException("Cannot use CBOR data type {0} as a map key", dt);
		}
	}

	private void pushContainer(boolean isMap, long remaining) {
		if (depth == stackRemaining.length) {
			var n = depth * 2;
			var nr = new long[n];
			var nm = new boolean[n];
			var nk = new boolean[n];
			System.arraycopy(stackRemaining, 0, nr, 0, depth);
			System.arraycopy(stackIsMap, 0, nm, 0, depth);
			System.arraycopy(stackAwaitingKey, 0, nk, 0, depth);
			stackRemaining = nr;
			stackIsMap = nm;
			stackAwaitingKey = nk;
		}
		stackRemaining[depth] = remaining;
		stackIsMap[depth] = isMap;
		stackAwaitingKey[depth] = isMap;  // map starts awaiting a key
		depth++;
	}

	private void popContainer() {
		depth--;
		currentToken = stackIsMap[depth] ? TokenType.END_OBJECT : TokenType.END_ARRAY;
		// The closed container counted as one value-emit in the parent's context.
		consumedOneElement();
	}

	private void consumedOneElement() {
		if (depth == 0)
			return;
		var top = depth - 1;
		if (stackRemaining[top] != INDEFINITE)
			stackRemaining[top]--;
		// In a map, alternate key/value positions per element.
		if (stackIsMap[top])
			stackAwaitingKey[top] = !stackAwaitingKey[top];
	}

	@Override /* TokenReader */
	public TokenType getCurrentToken() {
		return currentToken;
	}

	@Override /* TokenReader */
	public int getDepth() {
		return depth;
	}

	@Override /* TokenReader */
	public String getFieldName() {
		if (currentToken != TokenType.FIELD_NAME)
			throw new IllegalStateException("Current token is not FIELD_NAME (was " + currentToken + ")");
		return currentString;
	}

	@Override /* TokenReader */
	public String getString() {
		switch (currentToken) {
			case VALUE_STRING:  return currentString;
			case FIELD_NAME:    return currentString;
			case VALUE_NUMBER:  return currentNumberLexeme;
			case VALUE_BOOLEAN: return Boolean.toString(currentBoolean);
			case VALUE_NULL:    return null;
			default: throw new IllegalStateException(
				"Current token does not have a string view (was " + currentToken + ")");
		}
	}

	@Override /* TokenReader */
	public Number getNumber() throws ParseException {
		if (currentToken != TokenType.VALUE_NUMBER)
			throw new IllegalStateException("Current token is not VALUE_NUMBER (was " + currentToken + ")");
		return currentNumber;
	}

	@Override /* TokenReader */
	public String getNumberLexeme() {
		if (currentToken != TokenType.VALUE_NUMBER)
			throw new IllegalStateException("Current token is not VALUE_NUMBER (was " + currentToken + ")");
		return currentNumberLexeme;
	}

	@Override /* TokenReader */
	public boolean getBool() {
		if (currentToken != TokenType.VALUE_BOOLEAN)
			throw new IllegalStateException("Current token is not VALUE_BOOLEAN (was " + currentToken + ")");
		return currentBoolean;
	}

	@Override /* TokenReader */
	public byte[] getBinary() {
		if (currentToken != TokenType.VALUE_BINARY)
			throw new IllegalStateException("Current token is not VALUE_BINARY (was " + currentToken + ")");
		return currentBinary;
	}

	@Override /* TokenReader */
	public boolean canRead() throws IOException, ParseException {
		// CBOR has no separators to advance through; the cursor is "at a value boundary" iff
		// (a) we're not inside a definite-length container that's exhausted, and
		// (b) inside an indefinite-length container, the next byte isn't BREAK, and
		// (c) the stream isn't ended.
		if (ended)
			return false;
		if (depth > 0) {
			var remaining = stackRemaining[depth - 1];
			if (remaining == 0)
				return false;
			if (remaining == INDEFINITE && is.peekBreak())
				return false;
		}
		return true;
	}

	@Override /* TokenReader */
	public <T> T read(Class<T> type) throws IOException, ParseException {
		return read(session().getClassMeta(type));
	}

	@Override /* TokenReader */
	public <T> T read(ClassMeta<T> type) throws IOException, ParseException {
		var s = session();
		if (!canRead())
			throw new IllegalStateException(MSG_READ_NON_VALUE_STATE);
		try {
			T o = s.parseAnything(type, is, null, null);
			consumedOneElement();
			currentToken = TokenType.NOT_AVAILABLE;
			return o;
		} catch (Exception e) {
			if (e instanceof IOException ioe) throw ioe;
			if (e instanceof ParseException pe) throw pe;
			throw new ParseException(s, "read failed: {0}", e.getMessage());
		}
	}

	@Override /* TokenReader */
	public <T> T read(Type type, Type... args) throws IOException, ParseException {
		return read(session().<T>getClassMeta(type, args));
	}

	private CborParserSession session() {
		if (session == null)
			throw new UnsupportedOperationException(
				"read is not available on this CborTokenReader (no session attached). " +
				"Open the cursor via CborParser.parseTokens(...) to enable read.");
		return session;
	}

	@Override /* TokenReader */
	public void skipChildren() throws IOException, ParseException {
		if (currentToken != TokenType.START_OBJECT && currentToken != TokenType.START_ARRAY)
			return;
		var startDepth = depth - 1;
		while (depth > startDepth) {
			var t = next();
			if (t == TokenType.END_OF_STREAM)
				throw new ParseException("Unexpected end of input while skipping children");
		}
	}

	@Override /* TokenReader */
	public boolean isStreaming() {
		return true;
	}

	@Override /* TokenReader */
	public void close() throws IOException {
		pipe.close();
	}
}
