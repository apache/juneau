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
package org.apache.juneau.rest.server.util;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/**
 * A {@link Writer} that encodes to an {@link OutputStream} using a small, growable character buffer.
 *
 * <p>
 * Replacement for <c>OutputStreamWriter</c> on the response path.  <c>OutputStreamWriter</c> allocates an 8 KB encoder
 * byte buffer per instance, which for small responses costs far more than the response body itself.  This writer buffers
 * characters (starting at {@value #INITIAL_CAPACITY}, growing to {@value #MAX_CAPACITY}) and encodes them in one step on
 * {@link #flush()}, {@link #close()} or when the buffer fills.
 *
 * <p>
 * Malformed and unmappable input is replaced, as <c>OutputStreamWriter</c> does.  A trailing high surrogate is held back
 * until its pair arrives (or until {@link #close()}).  The encoder is stateful across flushes, so charsets that emit a
 * byte-order mark or shift sequences behave as they do under <c>OutputStreamWriter</c>.
 */
class BufferedEncodingWriter extends Writer {

	private static final int INITIAL_CAPACITY = 256;
	private static final int MAX_CAPACITY = 8192;

	private final OutputStream out;
	private final Charset charset;
	private CharsetEncoder encoder;
	private char[] buf = new char[INITIAL_CAPACITY];
	private int count;
	private boolean closed;

	/**
	 * Constructor.
	 *
	 * @param out The output stream to write encoded bytes to.
	 * @param characterEncoding The charset name.
	 * @throws UnsupportedEncodingException If the charset name is illegal or not supported.
	 */
	BufferedEncodingWriter(OutputStream out, String characterEncoding) throws UnsupportedEncodingException {
		if (characterEncoding == null)
			throw new NullPointerException("charsetName");
		try {
			charset = Charset.forName(characterEncoding);
		} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
			throw new UnsupportedEncodingException(characterEncoding);
		}
		this.out = out;
	}

	@Override /* Overridden from Writer */
	public void write(int c) throws IOException {
		synchronized (lock) {
			ensureOpen();
			room(1);
			buf[count++] = (char)c;
		}
	}

	@Override /* Overridden from Writer */
	public void write(char[] cbuf, int off, int len) throws IOException {
		synchronized (lock) {
			ensureOpen();
			if (off < 0 || len < 0 || off > cbuf.length - len)
				throw new IndexOutOfBoundsException();
			while (len > 0) {
				var n = room(len);
				System.arraycopy(cbuf, off, buf, count, n);
				count += n;
				off += n;
				len -= n;
			}
		}
	}

	@Override /* Overridden from Writer */
	public void write(String str, int off, int len) throws IOException {
		synchronized (lock) {
			ensureOpen();
			if (off < 0 || len < 0 || off > str.length() - len)
				throw new IndexOutOfBoundsException();
			while (len > 0) {
				var n = room(len);
				str.getChars(off, off + n, buf, count);
				count += n;
				off += n;
				len -= n;
			}
		}
	}

	@Override /* Overridden from Writer */
	public void flush() throws IOException {
		synchronized (lock) {
			ensureOpen();
			drain(false);
			out.flush();
		}
	}

	@Override /* Overridden from Writer */
	public void close() throws IOException {
		synchronized (lock) {
			if (closed)
				return;
			try {
				drain(true);
				out.flush();
			} finally {
				closed = true;
				out.close();
			}
		}
	}

	private void ensureOpen() throws IOException {
		if (closed)
			throw new IOException("Stream closed");
	}

	/** Returns how many of the {@code want} chars can be appended now (always at least 1), growing or draining as needed. */
	private int room(int want) throws IOException {
		var free = buf.length - count;
		if (free >= want)
			return want;
		if (buf.length < MAX_CAPACITY) {
			var cap = buf.length;
			while (cap < count + want && cap < MAX_CAPACITY)
				cap <<= 1;
			buf = Arrays.copyOf(buf, cap);
			free = buf.length - count;
			if (free >= want)
				return want;
		}
		if (free == 0) {
			drain(false);
			free = buf.length - count;
		}
		return Math.min(want, free);
	}

	/** Encodes and writes buffered chars.  Unless {@code last}, an incomplete trailing surrogate pair stays buffered. */
	private void drain(boolean last) throws IOException {
		if (count == 0 && (! last || encoder == null))
			return;
		if (encoder == null)
			encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		var in = CharBuffer.wrap(buf, 0, count);
		var bb = ByteBuffer.allocate(Math.max(32, (int)Math.ceil(count * (double)encoder.maxBytesPerChar()) + 16));
		// The encoder keeps its own state across calls (BOMs, shift sequences), so encode incrementally rather than per chunk.
		while (encoder.encode(in, bb, last).isOverflow())
			bb = flushBytes(bb);
		if (last)
			while (encoder.flush(bb).isOverflow())
				bb = flushBytes(bb);
		flushBytes(bb);
		var leftover = in.remaining();
		System.arraycopy(buf, in.position(), buf, 0, leftover);
		count = leftover;
	}

	private ByteBuffer flushBytes(ByteBuffer bb) throws IOException {
		if (bb.position() > 0)
			out.write(bb.array(), bb.arrayOffset(), bb.position());
		bb.clear();
		return bb;
	}
}
