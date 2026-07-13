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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.math.*;

import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenWriter} surface for the MessagePack format.
 *
 * <p>
 * MessagePack containers are length-prefixed (no indefinite-length encoding); the writer therefore
 * <b>buffers</b> each open container's body bytes until {@link #endObject()} / {@link #endArray()}
 * is called, then writes the length-prefix header followed by the buffered bytes.  Memory cost
 * is O(largest open container).
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>{@code byte[]} values dispatch to native MsgPack {@code bin}, not to a base64 string fallback.
 * </ul>
 */
@SuppressWarnings({
	"resource" // The writer's underlying OutputStream is owned by the caller via try-with-resources on the writer itself; Eclipse JDT flags the inner stream as unclosed but that's by design.
})
public class MsgPackTokenWriter implements TokenWriter {

	/**
	 * Cursor-level settings honored by {@link MsgPackTokenWriter}.
	 *
	 * @param walk The {@link PojoWalker.Options walk options} used by {@link #object(Object)}.
	 */
	@SuppressWarnings("javadoc")
	public record Settings(PojoWalker.Options walk) {

		/** Default canonical setting: default walk options. */
		public static final Settings DEFAULT = new Settings(PojoWalker.Options.DEFAULT);
	}

	private final OutputStream finalOut;
	private final Closeable owned;
	private final Settings settings;

	// Per-level buffers.  The active write target is the top of this stack; if depth==0 we write
	// directly to finalOut.
	private final java.util.ArrayDeque<ByteArrayOutputStream> buffers = new java.util.ArrayDeque<>();
	// Per-level container kind: true=map, false=array.
	private final java.util.ArrayDeque<Boolean> isMapStack = new java.util.ArrayDeque<>();
	// Per-level element-count (for arrays = number of values emitted; for maps = number of pairs).
	// For maps, incremented once per field() (= once per pair).
	private final java.util.ArrayDeque<Integer> elementCount = new java.util.ArrayDeque<>();
	// Per-level: for maps, true when the next emit should be a field (key), false after the
	// field when the next emit should be the value.
	private final java.util.ArrayDeque<Boolean> awaitingKey = new java.util.ArrayDeque<>();

	private boolean closed;

	/**
	 * Constructor with default settings.
	 *
	 * @param out The {@link OutputStream} to emit MsgPack to.  Must not be <jk>null</jk>.
	 */
	public MsgPackTokenWriter(OutputStream out) {
		this(out, null, Settings.DEFAULT);
	}

	/**
	 * Constructor with explicit settings.
	 *
	 * @param out The {@link OutputStream} to emit MsgPack to.  Must not be <jk>null</jk>.
	 * @param settings The settings.  Must not be <jk>null</jk>.
	 */
	public MsgPackTokenWriter(OutputStream out, Settings settings) {
		this(out, null, settings);
	}

	private MsgPackTokenWriter(OutputStream out, Closeable owned, Settings settings) {
		assertArgNotNull("out", out);
		assertArgNotNull("settings", settings);
		this.finalOut = out;
		this.owned = owned;
		this.settings = settings;
	}

	/**
	 * Internal factory used by {@link MsgPackSerializerSession#serializeTokens(Object)} to coerce
	 * supported output types ({@link OutputStream}, {@link File}) to a MsgPack writer.
	 *
	 * @param output The output object.
	 * @param settings The settings.
	 * @return A new {@link MsgPackTokenWriter}.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	public static MsgPackTokenWriter forOutput(Object output, Settings settings) throws IOException {
		if (output == null)
			throw new IOException("Output cannot be null.");
		if (output instanceof OutputStream os)
			return new MsgPackTokenWriter(os, null, settings);
		if (output instanceof File f) {
			var os = new BufferedOutputStream(new FileOutputStream(f));
			return new MsgPackTokenWriter(os, os, settings);
		}
		throw new IOException("Cannot convert object of type " + output.getClass().getName() + " to an OutputStream.");
	}

	/** The active write target for child bytes (top buffer or finalOut). */
	private OutputStream activeOut() {
		return buffers.isEmpty() ? finalOut : buffers.peek();
	}

	@Override /* TokenWriter */
	public TokenWriter startObject() throws IOException {
		assertOpen();
		// Push a new buffer; child writes go there until endObject.
		buffers.push(new ByteArrayOutputStream());
		isMapStack.push(true);
		elementCount.push(0);
		awaitingKey.push(true);
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter endObject() throws IOException {
		assertOpen();
		if (buffers.isEmpty() || !Boolean.TRUE.equals(isMapStack.peek()))
			throw new IllegalStateException("endObject called with no matching startObject");
		var body = buffers.pop();
		var count = elementCount.pop();
		isMapStack.pop();
		awaitingKey.pop();
		// Write the map header to whatever is now the active stream, then the buffered body.
		var header = new MsgPackOutputStream(activeOut());
		header.startMap(count);
		body.writeTo(activeOut());
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter startArray() throws IOException {
		assertOpen();
		buffers.push(new ByteArrayOutputStream());
		isMapStack.push(false);
		elementCount.push(0);
		awaitingKey.push(false);
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter endArray() throws IOException {
		assertOpen();
		if (buffers.isEmpty() || Boolean.TRUE.equals(isMapStack.peek()))
			throw new IllegalStateException("endArray called with no matching startArray");
		var body = buffers.pop();
		var count = elementCount.pop();
		isMapStack.pop();
		awaitingKey.pop();
		var header = new MsgPackOutputStream(activeOut());
		header.startArray(count);
		body.writeTo(activeOut());
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter fieldName(String name) throws IOException {
		assertOpen();
		assertArgNotNull("name", name);
		if (isMapStack.isEmpty() || !Boolean.TRUE.equals(isMapStack.peek()))
			throw new IllegalStateException("field called outside an object");
		if (!Boolean.TRUE.equals(awaitingKey.peek()))
			throw new IllegalStateException("field called twice without an intervening value");
		new MsgPackOutputStream(activeOut()).appendString(name);
		// Replace top of awaitingKey with false (we just wrote the key; expect a value next).
		awaitingKey.pop();
		awaitingKey.push(false);
		// Increment pair count once (per pair) — happens at the field, not the value.
		var c = elementCount.pop();
		elementCount.push(c + 1);
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter string(String value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueCheck();
		new MsgPackOutputStream(activeOut()).appendString(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(Number value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueCheck();
		new MsgPackOutputStream(activeOut()).appendNumber(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(long value) throws IOException {
		assertOpen();
		preValueCheck();
		new MsgPackOutputStream(activeOut()).appendLong(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(double value) throws IOException {
		assertOpen();
		preValueCheck();
		new MsgPackOutputStream(activeOut()).appendDouble(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(BigDecimal value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueCheck();
		new MsgPackOutputStream(activeOut()).appendDouble(value.doubleValue());
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(BigInteger value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueCheck();
		// Align with the databind serializer path (G4): long-range -> INT64, [2^63,2^64-1] -> UINT64,
		// otherwise throw — rather than the previous longValueExact() which threw on the representable
		// [2^63,2^64-1] range.
		new MsgPackOutputStream(activeOut()).appendBigInteger(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter bool(boolean value) throws IOException {
		assertOpen();
		preValueCheck();
		new MsgPackOutputStream(activeOut()).appendBoolean(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter nil() throws IOException {
		assertOpen();
		preValueCheck();
		new MsgPackOutputStream(activeOut()).appendNull();
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter binary(byte[] value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueCheck();
		new MsgPackOutputStream(activeOut()).appendBinary(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public MsgPackTokenWriter writeExt(int type, byte[] payload) throws IOException {
		assertOpen();
		if (payload == null)
			throw new IllegalArgumentException("ext payload must not be null");
		preValueCheck();
		new MsgPackOutputStream(activeOut()).writeExt(type, payload);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter object(Object value) throws IOException {
		assertOpen();
		PojoWalker.walk(this, value, settings.walk);
		return this;
	}

	@Override /* TokenWriter */
	public boolean isStreaming() { return true; }

	@Override /* TokenWriter */
	public void flush() throws IOException { finalOut.flush(); }

	@Override /* TokenWriter */
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		try {
			finalOut.flush();
		} finally {
			if (owned != null)
				owned.close();
		}
	}

	/**
	 * Throws an {@link IOException} if this writer has been {@link #close() closed}.
	 *
	 * @throws IOException If this writer is closed.
	 */
	private void assertOpen() throws IOException {
		if (closed)
			throw new IOException("Token writer is closed.");
	}

	private void preValueCheck() {
		// At map-key position, value emits are illegal — caller must call field() first.
		if (!isMapStack.isEmpty() && Boolean.TRUE.equals(isMapStack.peek()) && Boolean.TRUE.equals(awaitingKey.peek()))
			throw new IllegalStateException(
				"Value emitted at map-key position without preceding field(...).");
	}

	private void afterValue() {
		if (isMapStack.isEmpty())
			return;
		// After emitting a value:
		// - In a map, flip awaitingKey back to true (next emit is the next pair's key).
		// - In an array, increment element count.
		if (Boolean.TRUE.equals(isMapStack.peek())) {
			awaitingKey.pop();
			awaitingKey.push(true);
		} else {
			var c = elementCount.pop();
			elementCount.push(c + 1);
		}
	}
}
