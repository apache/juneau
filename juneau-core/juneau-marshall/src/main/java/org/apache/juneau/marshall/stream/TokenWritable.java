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
 * Capability role for serializers that additionally expose the structural {@link TokenWriter}
 * cursor (FULL formats: json, json5, jsonl, cbor, msgpack, jsonschema, jcs).
 *
 * <p>
 * Because {@link TokenWriter} IS-A {@link RecordWriter}, FULL formats get the record surface for
 * free &mdash; the {@link #serializeRecords(Object)} default returns
 * {@link #serializeTokens(Object)}.  Capability is detected via {@code instanceof}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link TokenReadable} &mdash; the symmetric reader role.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public interface TokenWritable extends RecordWritable {

	/**
	 * Opens a low-level {@link TokenWriter} push generator targeting the specified output.
	 *
	 * @param output The output.  Subtypes define the supported types (typically
	 * 	{@link Writer} / {@link OutputStream} / {@link File} / {@link StringBuilder}).
	 * @return A new {@link TokenWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	TokenWriter serializeTokens(Object output) throws IOException;

	/**
	 * FULL formats get record semantics for free &mdash; a {@link TokenWriter} IS-A
	 * {@link RecordWriter}.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter} cursor (the same object as
	 * 	{@link #serializeTokens(Object)}).
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override
	default RecordWriter serializeRecords(Object output) throws IOException {
		return serializeTokens(output);
	}
}
