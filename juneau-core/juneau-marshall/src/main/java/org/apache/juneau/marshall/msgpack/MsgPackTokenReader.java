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
package org.apache.juneau.marshall.msgpack;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenReader} surface for the MessagePack format.
 *
 * <p>
 * Backed by the existing internal {@link MsgPackInputStream} plumbing.  MessagePack containers
 * are length-prefixed; this cursor tracks the remaining-element-count per level on a stack and
 * emits END_* when the count is exhausted.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>The cursor is a true O(1)-memory streaming cursor ({@link #isStreaming()} == <jk>true</jk>).
 * </ul>
 */
@SuppressWarnings({
	"resource" // The cursor's underlying ParserPipe is owned by the caller via try-with-resources on the cursor itself; Eclipse JDT flags the inner pipe as unclosed but that's by design.
})
public class MsgPackTokenReader implements TokenReader {

	private final MsgPackInputStream is;
	private final ParserPipe pipe;
	private final MsgPackParserSession session;

	private TokenType currentToken = TokenType.NOT_AVAILABLE;
	private boolean ended;

	// Container stack (parallel arrays).
	private long[] stackRemaining = new long[16];
	private boolean[] stackIsMap = new boolean[16];
	private boolean[] stackAwaitingKey = new boolean[16];
	private int depth;

	// Scratch for current value.
	private String currentString;
	private String currentNumberLexeme;
	private Number currentNumber;
	private boolean currentBoolean;
	private byte[] currentBinary;

	// Binary-native opt-in state (175ad).  Defaults preserve the normalize-by-default contract:
	// EXT payloads surface as VALUE_BINARY with no metadata.  When nativeMode is true, the EXT
	// branch additionally captures the signed type byte into 'extType' and sets
	// 'nativeKind' = MSGPACK_EXT.
	private boolean nativeMode;
	private BinaryNativeKind nativeKind = BinaryNativeKind.NONE;
	private int extType;

	/**
	 * Constructor with default settings.
	 *
	 * @param pipe The parser input pipe.  Must not be <jk>null</jk>.
	 * @throws IOException If a problem occurred opening the underlying stream.
	 */
	public MsgPackTokenReader(ParserPipe pipe) throws IOException {
		this(pipe, null);
	}

	/**
	 * Constructor used by {@link MsgPackParserSession#parseTokens(Object)} to plumb the calling
	 * session through.
	 *
	 * @param pipe The parser input pipe.  Must not be <jk>null</jk>.
	 * @param session The {@link MsgPackParserSession} for {@link #read(Class)} delegation,
	 * 	or <jk>null</jk> to disable {@code read}.
	 * @throws IOException If a problem occurred opening the underlying stream.
	 */
	public MsgPackTokenReader(ParserPipe pipe, MsgPackParserSession session) throws IOException {
		this.pipe = pipe;
		this.is = new MsgPackInputStream(pipe);
		this.session = session;
	}

	/**
	 * Enables or disables binary-native opt-in mode.
	 *
	 * <p>
	 * When enabled, MsgPack {@code ext} tokens carry the signed type byte via
	 * {@link #getExtType()}.  When disabled (default), the payload still surfaces as
	 * {@link TokenType#VALUE_BINARY} but the type byte is dropped &mdash; identical to the
	 * pre-native-mode behavior.
	 *
	 * @param value <jk>true</jk> to enable native mode.
	 * @return This object.
	 */
	public MsgPackTokenReader setNativeMode(boolean value) {
		nativeMode = value;
		return this;
	}

	// ==============================================================================================
	// State-machine summary.  MsgPack containers are length-prefixed (no indefinite-length
	// encoding), so next() reads the data-type tag via MsgPackInputStream.readDataType() and
	// dispatches based on: (1) whether we're at end-of-input, (2) whether the current container's
	// remaining element count has reached zero (emit END_*), and (3) within a map, whether we're
	// at a key position or value position.
	//
	// Per-level container state lives on parallel stacks (stackRemaining + stackIsMap +
	// stackAwaitingKey, indexed by depth).  Each emit decrements stackRemaining[top]; map-level
	// emits also flip stackAwaitingKey[top] between key and value positions.
	// ==============================================================================================
	@Override /* TokenReader */
	public TokenType next() throws IOException, ParseException {
		if (ended) {
			currentToken = TokenType.END_OF_STREAM;
			return currentToken;
		}
		currentString = null;
		currentNumberLexeme = null;
		currentNumber = null;
		currentBinary = null;
		nativeKind = BinaryNativeKind.NONE;

		// Inside a container at element-count zero: emit END_*.
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

		// Map key position: emit FIELD_NAME.
		if (depth > 0 && stackIsMap[depth - 1] && stackAwaitingKey[depth - 1]) {
			currentToken = TokenType.FIELD_NAME;
			currentString = readScalarAsString(dt);
			consumedOneElement();
			return currentToken;
		}

		switch (dt) {
			case ARRAY -> {
				pushContainer(false, is.readLength());
				currentToken = TokenType.START_ARRAY;
			}
			case MAP -> {
				var n = is.readLength();
				pushContainer(true, n * 2);  // pair-count -> element-count
				currentToken = TokenType.START_OBJECT;
			}
			case INT -> {
				var v = (long) is.readInt();
				currentNumber = v;
				currentNumberLexeme = Long.toString(v);
				currentToken = TokenType.VALUE_NUMBER;
				consumedOneElement();
			}
			case LONG -> {
				var v = is.readLong();
				currentNumber = v;
				currentNumberLexeme = Long.toString(v);
				currentToken = TokenType.VALUE_NUMBER;
				consumedOneElement();
			}
			case FLOAT -> {
				var v = (double) is.readFloat();
				currentNumber = v;
				currentNumberLexeme = Double.toString(v);
				currentToken = TokenType.VALUE_NUMBER;
				consumedOneElement();
			}
			case DOUBLE -> {
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
			case NULL -> {
				currentToken = TokenType.VALUE_NULL;
				consumedOneElement();
			}
			case STRING -> {
				currentString = is.readString();
				currentToken = TokenType.VALUE_STRING;
				consumedOneElement();
			}
			case BIN -> {
				currentBinary = is.readBinary();
				currentToken = TokenType.VALUE_BINARY;
				consumedOneElement();
			}
			case EXT -> {
				// Q3 default: normalize to common scalars; the extension payload surfaces as
				// VALUE_BINARY.  In native mode (175ad), the signed ext type byte is also
				// captured for retrieval via getExtType().
				if (nativeMode) {
					extType = (byte) is.getExtType();
					nativeKind = BinaryNativeKind.MSGPACK_EXT;
				}
				currentBinary = is.readBinary();
				currentToken = TokenType.VALUE_BINARY;
				consumedOneElement();
			}
			default -> throw new ParseException("Unexpected MsgPack data type: %s", dt);
		}
		return currentToken;
	}

	private String readScalarAsString(DataType dt) throws IOException, ParseException {
		switch (dt) {
			case STRING:    return is.readString();
			case INT:       return Long.toString(is.readInt());
			case LONG:      return Long.toString(is.readLong());
			case BIN: {
				var b = is.readBinary();
				return new String(b, java.nio.charset.StandardCharsets.UTF_8);
			}
			case BOOLEAN:   return Boolean.toString(is.readBoolean());
			case FLOAT:     return Float.toString(is.readFloat());
			case DOUBLE:    return Double.toString(is.readDouble());
			default:
				throw new ParseException("Cannot use MsgPack data type %s as a map key", dt);
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
		stackAwaitingKey[depth] = isMap;
		depth++;
	}

	private void popContainer() {
		depth--;
		currentToken = stackIsMap[depth] ? TokenType.END_OBJECT : TokenType.END_ARRAY;
		consumedOneElement();
	}

	private void consumedOneElement() {
		if (depth == 0)
			return;
		var top = depth - 1;
		stackRemaining[top]--;
		if (stackIsMap[top])
			stackAwaitingKey[top] = !stackAwaitingKey[top];
	}

	@Override /* TokenReader */
	public TokenType getCurrentToken() { return currentToken; }

	@Override /* TokenReader */
	public int getDepth() { return depth; }

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
		return currentBinary == null ? null : currentBinary.clone();
	}

	@Override /* TokenReader */
	public boolean canRead() throws IOException, ParseException {
		return !ended && !(depth > 0 && stackRemaining[depth - 1] == 0);
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
			throw new ParseException(s, "read failed: %s", e.getMessage());
		}
	}

	@Override /* TokenReader */
	public <T> T read(Type type, Type... args) throws IOException, ParseException {
		return read(session().<T>getClassMeta(type, args));
	}

	private MsgPackParserSession session() {
		if (session == null)
			throw new UnsupportedOperationException(
				"read is not available on this MsgPackTokenReader (no session attached). " +
				"Open the cursor via MsgPackParser.parseTokens(...) to enable read.");
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
	public boolean isStreaming() { return true; }

	@Override /* TokenReader */
	public void close() throws IOException { pipe.close(); }

	@Override /* TokenReader */
	public BinaryNativeKind getNativeKind() {
		return nativeKind;
	}

	@Override /* TokenReader */
	public int getTagCount() {
		return 0;
	}

	@Override /* TokenReader */
	public long getTag(int index) {
		throw new IndexOutOfBoundsException("Tag index " + index + " out of bounds for tagCount 0");
	}

	@Override /* TokenReader */
	public int getExtType() {
		if (nativeKind != BinaryNativeKind.MSGPACK_EXT)
			throw new IllegalStateException("Current token is not a MsgPack ext (nativeKind=" + nativeKind + ")");
		return extType;
	}

	@Override /* TokenReader */
	public int getSimpleValue() {
		throw new IllegalStateException("MsgPack cursor has no CBOR simple value.");
	}
}
