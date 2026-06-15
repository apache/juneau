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
 * Capability role for parsers that can stream the elements of a top-level wire array, yielding
 * each element as a separate record.
 *
 * <p>
 * Implemented by formats with a meaningful "stream the elements of a top-level array" semantic
 * (json, json5, cbor, msgpack &mdash; true element streaming; yaml, parquet, bson, xml, html
 * &mdash; buffered via {@link RecordAdapter#arrayReader}).  Capability is detected via
 * {@code instanceof}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link ArrayRecordWritable} &mdash; the symmetric writer role.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public interface ArrayRecordReadable {

	/**
	 * Opens a {@link RecordReader} that yields each element of a top-level wire array as a
	 * separate record.
	 *
	 * @param input The input.
	 * @return A new element-streamed {@link RecordReader}.
	 * @throws IOException If a problem occurred reading the underlying input.
	 */
	RecordReader parseArrayRecords(Object input) throws IOException;

	/**
	 * Variant of {@link #parseArrayRecords(Object)} that accepts a caller-specified root element
	 * name.  Used by HTML/XML when the wire format is e.g. {@code <items><item>...} and the caller
	 * needs to declare which child element is the array element.
	 *
	 * <p>
	 * The default ignores the root name and delegates to {@link #parseArrayRecords(Object)}.
	 *
	 * @param input The input.
	 * @param rootElementName The element name whose children are array elements.  May be
	 * 	<jk>null</jk> for auto-detection.
	 * @return A new element-streamed {@link RecordReader}.
	 * @throws IOException If a problem occurred reading the underlying input.
	 */
	default RecordReader parseArrayRecords(Object input, String rootElementName) throws IOException {
		return parseArrayRecords(input);
	}

	/**
	 * Streaming-quality signal queryable <em>before</em> opening a cursor.
	 *
	 * @return <jk>true</jk> for true element streaming (json/cbor/msgpack); <jk>false</jk> for the
	 * 	buffered {@link RecordAdapter#arrayReader}-backed formats (xml, html, yaml, parquet, bson).
	 */
	default boolean isArrayRecordStreaming() { return true; }
}
