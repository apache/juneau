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
package org.apache.juneau.marshall.stream;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Static factories that produce buffered {@link RecordReader} / {@link RecordWriter} instances for
 * formats whose token-stream surface is whole-value-only (no native {@link TokenReader} /
 * {@link TokenWriter}).
 *
 * <p>
 * The cursors are created from the <b>live session</b> ({@link ParserSession} /
 * {@link SerializerSession}), so they honor the same per-invocation configuration (swaps, bean
 * filters, locale/timezone, schema) as {@code parse(...)}/{@code serialize(...)}.
 *
 * <p>
 * Reduces per-format override boilerplate to a one-liner on the session class:
 * <p class='bjava'>
 * 	<ja>@Override</ja>
 * 	<jk>public</jk> RecordReader readRecords(Object <jv>input</jv>) <jk>throws</jk> IOException {
 * 		<jk>return</jk> RecordAdapter.<jsm>reader</jsm>(<jk>this</jk>, <jv>input</jv>);
 * 	}
 *
 * 	<ja>@Override</ja>
 * 	<jk>public</jk> RecordWriter writeRecords(Object <jv>output</jv>) <jk>throws</jk> IOException {
 * 		<jk>return</jk> RecordAdapter.<jsm>writer</jsm>(<jk>this</jk>, <jv>output</jv>);
 * 	}
 * </p>
 *
 * <p>
 * All four cursors report {@link RecordReader#isStreaming() isStreaming()} == <jk>false</jk> (they
 * buffer) and obey one resource-ownership rule: {@link Closeable#close() close()} closes the
 * input/output it was handed when it is {@link Closeable}.  Multi-rooted formats that need
 * different semantics (jsonl, csv, sse) implement their own cursor.
 */
@SuppressWarnings({
	"resource" // RecordReader/RecordWriter returned here are Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public final class RecordAdapter {

	private RecordAdapter() {
		// Static-method facade.
	}

	/** Closes the handed input/output when it is {@link Closeable} (shared resource-ownership rule). */
	private static void closeIfCloseable(Object io) throws IOException {
		if (io instanceof Closeable io2)
			io2.close();
	}

	/**
	 * Creates a single-shot {@link RecordReader} that delegates {@code read(...)} calls to the
	 * session's {@link ParserSession#read(Object, Class) read(...)} entry point.
	 *
	 * @param session The format's live parser session.  Must not be <jk>null</jk>.
	 * @param input The input to bind on the first {@code read(...)} call.
	 * @return A new {@link RecordReader}.
	 */
	public static RecordReader reader(ParserSession session, Object input) {
		return new RecordReader() {
			private boolean exhausted;

			@Override public boolean canRead() {
				return !exhausted;
			}

			@Override public <T> T read(Class<T> type) throws IOException, ParseException {
				checkNotExhausted();
				exhausted = true;
				return session.read(input, type);
			}

			@Override public <T> T read(ClassMeta<T> type) throws IOException, ParseException {
				checkNotExhausted();
				exhausted = true;
				return session.read(input, type);
			}

			@Override public <T> T read(Type type, Type... args) throws IOException, ParseException {
				checkNotExhausted();
				exhausted = true;
				return session.read(input, type, args);
			}

			@Override public boolean isStreaming() {
				return false;
			}

			@Override public void close() throws IOException {
				exhausted = true;
				closeIfCloseable(input);
			}

			private void checkNotExhausted() {
				if (exhausted)
					throw new IllegalStateException("Cursor has already produced its single root value.");
			}
		};
	}

	/**
	 * Creates an array-element {@link RecordReader} backed by a buffered list parse.
	 *
	 * <p>
	 * Equivalent to calling {@code session.read(input, List.class, Object.class)} once and
	 * iterating the result.  This is correct but NOT streaming &mdash; the whole array is
	 * materialized.  Format-specific implementations may override
	 * {@link ArrayRecordReadable#readArrayRecords(Object)} directly for actual streaming.
	 *
	 * @param session The format's live parser session.  Must not be <jk>null</jk>.
	 * @param input The input to bind on the first read.
	 * @return A new {@link RecordReader} that yields one record per array element.
	 * @throws IOException If a problem occurred reading the input.
	 */
	@SuppressWarnings("unchecked")
	public static RecordReader arrayReader(ParserSession session, Object input) throws IOException {
		List<?> all;
		try {
			all = session.read(input, List.class, Object.class);
		} catch (ParseException e) {
			throw new IOException(e);
		}
		var iter = (all == null ? Collections.<Object>emptyList() : (List<Object>) all).iterator();
		return new RecordReader() {
			@Override public boolean canRead() {
				return iter.hasNext();
			}
			@Override public <T> T read(Class<T> type) {
				return session.convertToType(nextElement(), type);
			}
			@Override public <T> T read(ClassMeta<T> type) {
				return session.convertToType(nextElement(), type);
			}
			@Override public <T> T read(Type type, Type... args) {
				return session.<T>convertToType(nextElement(), type, args);
			}
			@Override public boolean isStreaming() {
				return false;
			}
			@Override public void close() throws IOException {
				closeIfCloseable(input);
			}
			private Object nextElement() {
				if (!iter.hasNext())
					throw new IllegalStateException("Array stream is exhausted.");
				return iter.next();
			}
		};
	}

	/**
	 * Creates an array-element {@link RecordWriter} that buffers each {@link RecordWriter#write(Object)
	 * write(...)} call and emits the whole list on {@link RecordWriter#close()}.
	 *
	 * <p>
	 * This is correct but NOT streaming &mdash; the writes are accumulated in memory.
	 * Format-specific implementations may override
	 * {@link ArrayRecordWritable#writeArrayRecords(Object)} directly for actual streaming.
	 *
	 * @param session The format's live serializer session.  Must not be <jk>null</jk>.
	 * @param output The output to write to.
	 * @return A new {@link RecordWriter} that accepts N elements and emits an N-element array.
	 */
	public static RecordWriter arrayWriter(SerializerSession session, Object output) {
		return new RecordWriter() {
			private final List<Object> buf = new ArrayList<>();
			private boolean closed;

			@Override public RecordWriter write(Object value) {
				if (closed)
					throw new IllegalStateException("Array stream is closed.");
				buf.add(value);
				return this;
			}
			@Override public boolean isStreaming() {
				return false;
			}
			@Override public void flush() throws IOException {
				if (output instanceof Flushable output2)
					output2.flush();
			}
			@Override public void close() throws IOException {
				if (closed)
					return;
				closed = true;
				try {
					session.write(buf, output);
				} catch (SerializeException e) {
					throw new IOException(e);
				}
				closeIfCloseable(output);
			}
		};
	}

	/**
	 * Creates a single-shot {@link RecordWriter} that delegates {@code write(Object)} calls to the
	 * session's {@link SerializerSession#write(Object, Object) write(...)} entry point.
	 *
	 * @param session The format's live serializer session.  Must not be <jk>null</jk>.
	 * @param output The output to write to.
	 * @return A new {@link RecordWriter}.
	 */
	public static RecordWriter writer(SerializerSession session, Object output) {
		return new RecordWriter() {
			private boolean written;

			@Override public RecordWriter write(Object value) throws IOException {
				if (written)
					throw new IllegalStateException("Cursor has already accepted its single root value.");
				written = true;
				try {
					session.write(value, output);
				} catch (SerializeException e) {
					throw new IOException(e);
				}
				return this;
			}

			@Override public boolean isStreaming() {
				return false;
			}

			@Override public void flush() throws IOException {
				if (output instanceof Flushable output2)
					output2.flush();
			}

			@Override public void close() throws IOException {
				closeIfCloseable(output);
			}
		};
	}
}
