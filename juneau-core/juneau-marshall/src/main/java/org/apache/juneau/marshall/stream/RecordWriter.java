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

/**
 * Public low-level whole-value push generator over a Juneau-formatted document.
 *
 * <p>
 * Format-neutral cursor that emits one whole-value record per {@link #write(Object)} call.
 * Single-rooted formats accept exactly one call (subsequent calls throw); multi-rooted formats
 * (jsonl, csv, sse) emit one line / row / event per call.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder();
 * 	<jk>try</jk> (RecordWriter <jv>w</jv> = Json.<jsf>DEFAULT</jsf>.writeRecords(<jv>sb</jv>)) {
 * 		<jv>w</jv>.write(<jv>bean</jv>);
 * 	}
 * </p>
 *
 * <h5 class='section'>Streaming quality:</h5>
 * <ul>
 * 	<li>{@link #isStreaming()} == <jk>true</jk> &mdash; O(1)-memory streaming.  FULL-token formats
 * 		return a cursor that is also a {@link TokenWriter} (the structural surface).
 * 	<li>{@link #isStreaming()} == <jk>false</jk> &mdash; buffered whole-value cursor (backed by
 * 		{@link RecordAdapter}); {@link #write(Object)} delegates to the format's existing
 * 		serializer and the structural surface is not available.
 * </ul>
 *
 * <h5 class='section'>Resource ownership:</h5>
 * <p>
 * Every {@link RecordReader} / {@code RecordWriter} produced by a factory method
 * <b>closes what it was handed</b> &mdash; if the underlying input/output is {@link Closeable},
 * {@link #close()} closes it.  This contract is symmetric across all factory methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link RecordReader} &mdash; the pull-side counterpart.
 * 	<li>{@link TokenWriter} &mdash; structural extension for FULL-capability formats.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public interface RecordWriter extends Closeable, Flushable {

	/**
	 * Emits one whole-value record.
	 *
	 * @param value The value.  May be <jk>null</jk>.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 */
	RecordWriter write(Object value) throws IOException;

	/**
	 * Returns <jk>true</jk> if this cursor is an O(1)-memory streaming cursor, or <jk>false</jk> if
	 * it is a buffered whole-value cursor (backed by {@link RecordAdapter}).
	 *
	 * @return <jk>true</jk> if this cursor streams; <jk>false</jk> if it buffers.
	 */
	boolean isStreaming();

	/**
	 * Flushes any buffered output to the underlying stream.
	 *
	 * @throws IOException If a problem occurred flushing the underlying stream.
	 */
	@Override
	void flush() throws IOException;

	/**
	 * Closes the writer, flushing any buffered output first.
	 *
	 * <p>
	 * Implementations should be idempotent &mdash; closing a closed writer is a no-op.
	 */
	@Override
	void close() throws IOException;
}
