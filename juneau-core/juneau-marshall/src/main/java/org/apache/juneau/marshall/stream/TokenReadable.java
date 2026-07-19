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
 * Capability role for parsers that additionally expose the structural {@link TokenReader} cursor
 * (FULL formats: json, json5, jsonl, cbor, msgpack).
 *
 * <p>
 * Because {@link TokenReader} IS-A {@link RecordReader}, FULL formats get the record surface for
 * free &mdash; the {@link #readRecords(Object)} default returns {@link #readTokens(Object)}.
 * Capability is detected via {@code instanceof}; a format that supports tokens is a
 * {@code TokenReadable} (and transitively a {@link RecordReadable}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link TokenWritable} &mdash; the symmetric writer role.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public interface TokenReadable extends RecordReadable {

	/**
	 * Opens a low-level {@link TokenReader} cursor over the specified input.
	 *
	 * @param input The input.  Subtypes define the supported types (typically
	 * 	{@link Reader} / {@link CharSequence} / {@link InputStream} / <code><jk>byte</jk>[]</code>).
	 * @return A new {@link TokenReader} cursor positioned at {@link TokenType#NOT_AVAILABLE}.
	 * @throws IOException If a problem occurred reading the underlying input.
	 */
	TokenReader readTokens(Object input) throws IOException;

	/**
	 * FULL formats get record semantics for free &mdash; a {@link TokenReader} IS-A
	 * {@link RecordReader}.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor (the same object as {@link #readTokens(Object)}).
	 * @throws IOException If a problem occurred reading the underlying input.
	 */
	@Override
	default RecordReader readRecords(Object input) throws IOException {
		return readTokens(input);
	}
}
