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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.math.*;

import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenWriter} surface for the CBOR format
 * (RFC 8949).
 *
 * <p>
 * Wraps a {@link CborOutputStream} and emits **indefinite-length** containers
 * (<c>0xBF</c> for maps, <c>0x9F</c> for arrays, terminated by <c>0xFF</c> BREAK) so that the
 * caller doesn't have to know element counts up front &mdash; a natural fit for streaming
 * generation.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>{@code byte[]} values dispatch to native CBOR major-type-2 byte strings via
 * 		{@link CborOutputStream#appendBinary(byte[])}, not to a base64 string fallback.
 * 	<li>The writer is purely structural; object swaps and {@code @Schema} annotations are NOT
 * 		applied at the token layer (see {@link TokenWriter#object(Object)} for the bridge that
 * 		does apply them).
 * </ul>
 */
@SuppressWarnings({
	"resource" // The writer's underlying OutputStream is owned by the caller via try-with-resources on the writer itself; Eclipse JDT flags the inner stream as unclosed but that's by design.
})
public class CborTokenWriter implements TokenWriter {

	/**
	 * Cursor-level settings honored by {@link CborTokenWriter}.
	 *
	 * @param walk The {@link org.apache.juneau.marshall.stream.PojoWalker.Options walk options} used by
	 * 	{@link #object(Object)} when walking a value graph.
	 */
	public record Settings(PojoWalker.Options walk) {

		/** Default canonical setting: default walk options. */
		public static final Settings DEFAULT = new Settings(PojoWalker.Options.DEFAULT);
	}

	private final CborOutputStream out;
	private final OutputStream raw;
	private final Closeable owned;
	private final Settings settings;

	// Container stack: 0 = array, 1 = map; tracked depth lets us validate end-kind matches.
	private boolean[] stackIsMap = new boolean[16];
	private int depth;
	// Map state: track whether we're emitting a key (true) or a value (false) at the current
	// map level.
	private boolean[] stackAwaitingKey = new boolean[16];

	private boolean closed;

	/**
	 * Constructor with default settings.
	 *
	 * @param out The {@link OutputStream} to emit CBOR to.  Must not be <jk>null</jk>.
	 */
	public CborTokenWriter(OutputStream out) {
		this(out, null, Settings.DEFAULT);
	}

	/**
	 * Constructor with explicit settings.
	 *
	 * @param out The {@link OutputStream} to emit CBOR to.  Must not be <jk>null</jk>.
	 * @param settings The settings.  Must not be <jk>null</jk>.
	 */
	public CborTokenWriter(OutputStream out, Settings settings) {
		this(out, null, settings);
	}

	private CborTokenWriter(OutputStream out, Closeable owned, Settings settings) {
		assertArgNotNull("out", out);
		assertArgNotNull("settings", settings);
		this.raw = out;
		this.out = new CborOutputStream(out);
		this.owned = owned;
		this.settings = settings;
	}

	/**
	 * Internal factory used by {@link CborSerializerSession#serializeTokens(Object)} to coerce
	 * supported output types ({@link OutputStream}, {@link File}) to a CBOR writer.
	 *
	 * @param output The output object.
	 * @param settings The settings.
	 * @return A new {@link CborTokenWriter}.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	public static CborTokenWriter forOutput(Object output, Settings settings) throws IOException {
		if (output == null)
			throw new IOException("Output cannot be null.");
		if (output instanceof OutputStream os)
			return new CborTokenWriter(os, null, settings);
		if (output instanceof File f) {
			var os = new BufferedOutputStream(new FileOutputStream(f));
			return new CborTokenWriter(os, os, settings);
		}
		throw new IOException("Cannot convert object of type " + output.getClass().getName() + " to an OutputStream.");
	}

	@Override /* TokenWriter */
	public TokenWriter startObject() throws IOException {
		assertOpen();
		preValueWrite();
		// 0xBF = indefinite-length map start.
		raw.write(0xBF);
		pushContainer(true);
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter endObject() throws IOException {
		assertOpen();
		popContainer(true);
		// 0xFF = BREAK (closes both indefinite arrays and maps).
		raw.write(0xFF);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter startArray() throws IOException {
		assertOpen();
		preValueWrite();
		// 0x9F = indefinite-length array start.
		raw.write(0x9F);
		pushContainer(false);
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter endArray() throws IOException {
		assertOpen();
		popContainer(false);
		raw.write(0xFF);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter fieldName(String name) throws IOException {
		assertOpen();
		assertArgNotNull("name", name);
		if (depth == 0 || !stackIsMap[depth - 1])
			throw new IllegalStateException("field called outside an object");
		if (!stackAwaitingKey[depth - 1])
			throw new IllegalStateException("field called twice without an intervening value");
		out.appendString(name);
		stackAwaitingKey[depth - 1] = false;
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter string(String value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueWrite();
		out.appendString(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(Number value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueWrite();
		out.appendNumber(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(long value) throws IOException {
		assertOpen();
		preValueWrite();
		out.appendLong(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(double value) throws IOException {
		assertOpen();
		preValueWrite();
		out.appendDouble(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(BigDecimal value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueWrite();
		out.appendDouble(value.doubleValue());
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter number(BigInteger value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueWrite();
		// Delegates to appendNumber so values outside the signed-long range are emitted losslessly
		// (native CBOR integer up to ±2^64, decimal string beyond) rather than throwing on overflow.
		out.appendNumber(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter bool(boolean value) throws IOException {
		assertOpen();
		preValueWrite();
		out.appendBoolean(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter nil() throws IOException {
		assertOpen();
		preValueWrite();
		out.appendNull();
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public TokenWriter binary(byte[] value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValueWrite();
		out.appendBinary(value);
		afterValue();
		return this;
	}

	@Override /* TokenWriter */
	public CborTokenWriter writeTag(long tagNumber) throws IOException {
		assertOpen();
		// A tag is a prefix on the next value emit; it does NOT consume a map-key/value or
		// array-element slot.  Skip preValueWrite()/afterValue() — the wrapped value emit that
		// follows owns the state transition.
		out.writeTag(tagNumber);
		return this;
	}

	@Override /* TokenWriter */
	public CborTokenWriter writeSimple(int value) throws IOException {
		assertOpen();
		preValueWrite();
		out.writeSimple(value);
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
	public boolean isStreaming() {
		return true;
	}

	@Override /* TokenWriter */
	public void flush() throws IOException {
		raw.flush();
	}

	@Override /* TokenWriter */
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		try {
			raw.flush();
		} finally {
			if (owned != null)
				owned.close();
		}
	}

	// =================================================================================
	// Helpers
	// =================================================================================

	/**
	 * Throws an {@link IOException} if this writer has been {@link #close() closed}.
	 *
	 * @throws IOException If this writer is closed.
	 */
	private void assertOpen() throws IOException {
		if (closed)
			throw new IOException("Token writer is closed.");
	}

	private void preValueWrite() {
		if (depth == 0)
			return;
		if (stackIsMap[depth - 1] && stackAwaitingKey[depth - 1])
			throw new IllegalStateException(
				"Value emitted at map-key position without preceding field(...).");
	}

	private void afterValue() {
		if (depth == 0)
			return;
		if (stackIsMap[depth - 1])
			stackAwaitingKey[depth - 1] = true;  // next emit inside this map should be a key
	}

	private void pushContainer(boolean isMap) {
		if (depth == stackIsMap.length) {
			var n = depth * 2;
			var nm = new boolean[n];
			var nk = new boolean[n];
			System.arraycopy(stackIsMap, 0, nm, 0, depth);
			System.arraycopy(stackAwaitingKey, 0, nk, 0, depth);
			stackIsMap = nm;
			stackAwaitingKey = nk;
		}
		stackIsMap[depth] = isMap;
		stackAwaitingKey[depth] = isMap;  // map starts awaiting a key
		depth++;
	}

	private void popContainer(boolean expectMap) {
		if (depth == 0)
			throw new IllegalStateException("end-container called with no matching start-container");
		var actualIsMap = stackIsMap[depth - 1];
		if (actualIsMap != expectMap)
			throw new IllegalStateException(
				"end-container kind mismatch: expected " + (expectMap ? "object" : "array") +
				", got " + (actualIsMap ? "object" : "array"));
		depth--;
	}
}
