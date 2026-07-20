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

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;

/**
 * O(1)-memory wrappers that adapt a fine-grained {@link TokenReader} / {@link TokenWriter} to
 * the array-element record-stream contract.  Memory does not scale with the number of array
 * elements &mdash; reading binds one element at a time; writing emits one element at a time.
 *
 * <p>
 * For the reader: the wrapper opens an outer array via the cursor's {@link TokenReader#next()},
 * yields elements via {@link TokenReader#read(Class)} until {@link TokenReader#canRead()} returns
 * false (end of array), then advances past the closing {@code ]} on close.
 *
 * <p>
 * For the writer: the wrapper opens an outer array via {@link TokenWriter#startArray()},
 * forwards each {@link RecordWriter#write(Object) write(...)} call to
 * {@link TokenWriter#object(Object)}, and emits the closing {@code ]} via
 * {@link TokenWriter#endArray()} on close.
 *
 * <p>
 * Used by FULL-capability formats whose token cursor genuinely streams (json, json5, cbor, etc.).
 * For PARTIAL formats whose token surface doesn't exist, see
 * {@link RecordAdapter#arrayReader(ParserSession, Object)} / {@link RecordAdapter#arrayWriter}, which
 * are buffered.
 */
@SuppressWarnings({
	"resource" // Wrapped TokenReader/TokenWriter are Closeables owned by the caller; Eclipse JDT @Owning/resource-leak warning is by design.
})
public final class StreamingArrayRecord {

	private StreamingArrayRecord() {}

	/**
	 * Wraps a {@link TokenReader} as a streaming array-element {@link RecordReader}.
	 *
	 * <p>
	 * The wrapper assumes the cursor is positioned at the start of an array on the wire.  It
	 * consumes the opening {@link TokenType#START_ARRAY} on construction and the closing
	 * {@link TokenType#END_ARRAY} on {@link RecordReader#close() close()}.
	 *
	 * @param cursor The token reader to wrap.  Must not be <jk>null</jk>.  Owned by the returned
	 * 	RecordReader; closing the RecordReader closes the cursor.
	 * @return A streaming array-element {@link RecordReader}.
	 * @throws IOException If a problem occurred reading the underlying input.
	 * @throws ParseException If the input is not a JSON-style array.
	 */
	public static RecordReader reader(TokenReader cursor) throws IOException, ParseException {
		var t = cursor.next();
		if (t != TokenType.START_ARRAY)
			throw new ParseException("Expected START_ARRAY at the root of arrayRecordReader input but got %s", t);
		return new RecordReader() {
			@Override public boolean canRead() throws IOException, ParseException {
				return cursor.canRead();
			}
			@Override public <T> T read(Class<T> type) throws IOException, ParseException {
				return cursor.read(type);
			}
			@Override public <T> T read(ClassMeta<T> type) throws IOException, ParseException {
				return cursor.read(type);
			}
			@Override public <T> T read(Type type, Type... args) throws IOException, ParseException {
				return cursor.read(type, args);
			}
			@Override public boolean isStreaming() {
				return true;
			}
			@Override public void close() throws IOException {
				cursor.close();
			}
		};
	}

	/**
	 * Wraps a {@link TokenWriter} as a streaming array-element {@link RecordWriter}.
	 *
	 * <p>
	 * The wrapper emits the opening array on construction.  Each
	 * {@link RecordWriter#write(Object)} call appends one array element via
	 * {@link TokenWriter#object(Object)}.  {@link RecordWriter#close() close()} emits the
	 * closing array bracket and closes the underlying cursor.
	 *
	 * @param cursor The token writer to wrap.  Must not be <jk>null</jk>.  Owned by the returned
	 * 	RecordWriter; closing the RecordWriter closes the cursor.
	 * @return A streaming array-element {@link RecordWriter}.
	 * @throws IOException If a problem occurred writing to the underlying output.
	 */
	public static RecordWriter writer(TokenWriter cursor) throws IOException {
		cursor.startArray();
		return new RecordWriter() {
			private boolean closed;

			@Override public RecordWriter write(Object value) throws IOException {
				if (closed)
					throw new IllegalStateException("Array stream is closed.");
				cursor.object(value);
				return this;
			}
			@Override public boolean isStreaming() {
				return true;
			}
			@Override public void flush() throws IOException {
				cursor.flush();
			}
			@Override public void close() throws IOException {
				if (closed)
					return;
				closed = true;
				cursor.endArray();
				cursor.close();
			}
		};
	}
}
