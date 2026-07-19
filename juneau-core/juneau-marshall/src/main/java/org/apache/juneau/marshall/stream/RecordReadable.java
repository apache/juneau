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
 * Capability role for objects that can open a whole-value {@link RecordReader} cursor.
 *
 * <p>
 * Implemented at <b>both</b> levels of the parser surface:
 * <ul>
 * 	<li>the convenience {@link org.apache.juneau.marshall.parser.Parser Parser} subclass (the
 * 		object REST negotiation matches on) &mdash; a thin delegator that mirrors
 * 		{@link org.apache.juneau.marshall.parser.Parser#read(Object, Class) read(...)} by
 * 		forwarding to {@code getSession().readRecords(input)};
 * 	<li>the {@link org.apache.juneau.marshall.parser.ParserSession ParserSession} subclass (where
 * 		the live {@code BeanSession}/{@code MarshallingContext} is) &mdash; the real cursor
 * 		creation.
 * </ul>
 *
 * <p>
 * <b>Capability is detected via {@code instanceof}:</b> a parser supports the record-streaming
 * surface exactly when it is an instance of this interface.  There is no separate capability enum
 * or marker interface.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link TokenReadable} &mdash; the structural-token extension (FULL formats).
 * 	<li>{@link RecordWritable} &mdash; the symmetric writer role.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public interface RecordReadable {

	/**
	 * Opens a low-level {@link RecordReader} cursor over the specified input.
	 *
	 * <p>
	 * When invoked on a convenience {@link org.apache.juneau.marshall.parser.Parser Parser} the
	 * cursor is opened with <b>default session args</b> (mirroring
	 * {@link org.apache.juneau.marshall.parser.Parser#read(Object, Class) read(...)}).  Callers
	 * that need request-derived configuration (locale, timezone, schema, swaps) should instead
	 * call {@code createSession(args).build()} and invoke this method on the resulting
	 * {@link org.apache.juneau.marshall.parser.ParserSession ParserSession} (which is also a
	 * {@code RecordReadable}).
	 *
	 * @param input The input.  Subtypes define the supported types (typically
	 * 	{@link Reader} / {@link CharSequence} / {@link InputStream} / {@code byte[]} / {@link File}).
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred reading the underlying input.
	 */
	RecordReader readRecords(Object input) throws IOException;

	/**
	 * Streaming-quality signal queryable <em>before</em> opening a cursor.
	 *
	 * @return <jk>true</jk> if {@link #readRecords(Object)} returns an O(1)-memory streaming
	 * 	cursor; <jk>false</jk> for buffered (whole-value) formats backed by
	 * 	{@link RecordAdapter}.
	 */
	default boolean isRecordStreaming() { return true; }
}
