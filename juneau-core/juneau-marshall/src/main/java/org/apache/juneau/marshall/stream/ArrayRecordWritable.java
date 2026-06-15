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
 * Capability role for serializers that can emit each element of a top-level wire array as a
 * separate record.
 *
 * <p>
 * The symmetric writer-side counterpart of {@link ArrayRecordReadable}.  Capability is detected
 * via {@code instanceof}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link ArrayRecordReadable} &mdash; the symmetric reader role.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public interface ArrayRecordWritable {

	/**
	 * Opens a {@link RecordWriter} that wraps each {@link RecordWriter#write(Object) write(...)}
	 * call as one element of a top-level wire array.
	 *
	 * @param output The output.
	 * @return A new element-streamed {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	RecordWriter serializeArrayRecords(Object output) throws IOException;

	/**
	 * Variant of {@link #serializeArrayRecords(Object)} for length-prefixed binary formats that
	 * require the element count up front (msgpack, bson).  Other formats may ignore the count and
	 * behave the same as {@link #serializeArrayRecords(Object)}.
	 *
	 * @param output The output.
	 * @param expectedCount The number of elements that will be written.  Must match the number of
	 * 	{@link RecordWriter#write(Object)} calls before {@link RecordWriter#close()}.
	 * @return A new element-streamed {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	default RecordWriter serializeArrayRecords(Object output, int expectedCount) throws IOException {
		return serializeArrayRecords(output);
	}

	/**
	 * Streaming-quality signal queryable <em>before</em> opening a cursor.
	 *
	 * @return <jk>true</jk> for true element streaming (json/cbor/msgpack); <jk>false</jk> for the
	 * 	buffered {@link RecordAdapter#arrayWriter}-backed formats (yaml, parquet, bson).
	 */
	default boolean isArrayRecordStreaming() { return true; }
}
