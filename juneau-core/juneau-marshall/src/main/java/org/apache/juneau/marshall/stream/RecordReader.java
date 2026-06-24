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
import java.util.stream.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Public low-level whole-value pull cursor over a Juneau-formatted document.
 *
 * <p>
 * Format-neutral cursor that yields one whole-value record per {@link #read(Class)} call.  Single-rooted
 * formats (json, xml, bson, yaml, ...) degenerate to a one-record stream; multi-rooted formats
 * (jsonl, csv, sse) expose each line / row / event as a separate record.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> (RecordReader <jv>r</jv> = Json.<jsf>DEFAULT</jsf>.fromRecords(<jv>input</jv>)) {
 * 		<jk>while</jk> (<jv>r</jv>.canRead()) {
 * 			Bean <jv>bean</jv> = <jv>r</jv>.read(Bean.<jk>class</jk>);
 * 			process(<jv>bean</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Streaming quality:</h5>
 * <ul>
 * 	<li>{@link #isStreaming()} == <jk>true</jk> &mdash; O(1)-memory streaming.  FULL-token formats
 * 		return a cursor that is also a {@link TokenReader} (the structural surface).
 * 	<li>{@link #isStreaming()} == <jk>false</jk> &mdash; buffered whole-value cursor (backed by
 * 		{@link RecordAdapter}); {@link #read(Class)} delegates to the format's existing parser and
 * 		the structural surface is not available.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link RecordWriter} &mdash; the push-side counterpart.
 * 	<li>{@link TokenReader} &mdash; structural extension for FULL-capability formats.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public interface RecordReader extends Closeable {

	/**
	 * Returns <jk>true</jk> if {@link #read(Class)} can be called to produce another record.
	 *
	 * <p>
	 * Single-rooted formats return <jk>true</jk> exactly once (for the root value), then
	 * <jk>false</jk> after the value has been consumed.  Multi-rooted formats return <jk>true</jk>
	 * once per remaining record on the wire.
	 *
	 * @return <jk>true</jk> if a record is available; <jk>false</jk> at end of stream.
	 * @throws IOException If a problem occurred peeking at the underlying stream.
	 * @throws ParseException If the input is malformed for the format.
	 */
	boolean canRead() throws IOException, ParseException;

	/**
	 * Reads and returns the next record bound to the requested type.
	 *
	 * @param <T> The expected return type.
	 * @param type The target Java type.
	 * @return The bound POJO.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed for the format or cannot be bound to the type.
	 * @throws IllegalStateException If {@link #canRead()} returned <jk>false</jk>.
	 */
	<T> T read(Class<T> type) throws IOException, ParseException;

	/**
	 * Same as {@link #read(Class)} but accepts a pre-resolved {@link ClassMeta}.
	 *
	 * @param <T> The expected return type.
	 * @param type The target type.
	 * @return The bound POJO.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed or cannot be bound to the type.
	 */
	<T> T read(ClassMeta<T> type) throws IOException, ParseException;

	/**
	 * Same as {@link #read(Class)} but accepts a parameterized {@link Type} and optional type
	 * arguments (the same shape as
	 * {@link Parser#parse(Object, Type, Type...)}).
	 *
	 * @param <T> The expected return type.
	 * @param type The target type.
	 * @param args Optional type arguments for parameterized types.
	 * @return The bound POJO.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed or cannot be bound to the type.
	 */
	<T> T read(Type type, Type... args) throws IOException, ParseException;

	/**
	 * Returns an {@link Iterator} view that yields each remaining record bound to the requested
	 * type.
	 *
	 * <p>
	 * The iterator drains from this cursor; consuming both surfaces concurrently is not supported.
	 *
	 * <p>
	 * Checked {@link IOException} / {@link ParseException} thrown by the underlying cursor are
	 * wrapped in an {@link RuntimeParseException} (carrying the typed cause) so they can surface
	 * through the {@link Iterator} API.
	 *
	 * @param <T> The element type.
	 * @param type The element type.
	 * @return An iterator over the remaining records.
	 */
	default <T> Iterator<T> iterator(Class<T> type) {
		return new Iterator<>() {
			@Override public boolean hasNext() {
				try {
					return canRead();
				} catch (IOException | ParseException e) {
					throw new RuntimeParseException(e);
				}
			}
			@Override public T next() {
				if (! hasNext())
					throw new java.util.NoSuchElementException();
				try {
					return read(type);
				} catch (IOException | ParseException e) {
					throw new RuntimeParseException(e);
				}
			}
		};
	}

	/**
	 * Returns a {@link Stream} view that yields each remaining record bound to the requested type.
	 *
	 * @param <T> The element type.
	 * @param type The element type.
	 * @return A stream over the remaining records.
	 */
	default <T> Stream<T> stream(Class<T> type) {
		var spliterator = Spliterators.spliteratorUnknownSize(iterator(type), Spliterator.ORDERED);
		return StreamSupport.stream(spliterator, false);
	}

	/**
	 * Returns <jk>true</jk> if this cursor is an O(1)-memory streaming cursor, or <jk>false</jk> if
	 * it is a buffered whole-value cursor (backed by {@link RecordAdapter}).
	 *
	 * @return <jk>true</jk> if this cursor streams; <jk>false</jk> if it buffers.
	 */
	boolean isStreaming();

	/**
	 * Closes the underlying stream.
	 *
	 * <p>
	 * Implementations should be idempotent &mdash; closing a closed reader is a no-op.
	 */
	@Override
	void close() throws IOException;
}
