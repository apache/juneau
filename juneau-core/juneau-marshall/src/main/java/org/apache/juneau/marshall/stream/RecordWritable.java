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
 * Capability role for objects that can open a whole-value {@link RecordWriter} cursor.
 *
 * <p>
 * The symmetric writer-side counterpart of {@link RecordReadable}.  Implemented at <b>both</b>
 * levels of the serializer surface:
 * <ul>
 * 	<li>the convenience {@link org.apache.juneau.marshall.serializer.Serializer Serializer}
 * 		subclass &mdash; a thin delegator that mirrors
 * 		{@link org.apache.juneau.marshall.serializer.Serializer#serialize(Object) serialize(...)} by
 * 		forwarding to {@code getSession().serializeRecords(output)};
 * 	<li>the {@link org.apache.juneau.marshall.serializer.SerializerSession SerializerSession}
 * 		subclass (where the live {@code BeanSession}/{@code MarshallingContext} is) &mdash; the real
 * 		cursor creation.
 * </ul>
 *
 * <p>
 * <b>Capability is detected via {@code instanceof}.</b>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link TokenWritable} &mdash; the structural-token extension (FULL formats).
 * 	<li>{@link RecordReadable} &mdash; the symmetric reader role.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public interface RecordWritable {

	/**
	 * Opens a low-level {@link RecordWriter} cursor over the specified output.
	 *
	 * <p>
	 * When invoked on a convenience {@link org.apache.juneau.marshall.serializer.Serializer
	 * Serializer} the cursor is opened with <b>default session args</b> (mirroring
	 * {@link org.apache.juneau.marshall.serializer.Serializer#serialize(Object) serialize(...)}).
	 * Callers that need request-derived configuration should instead call
	 * {@code createSession(args).build()} and invoke this method on the resulting
	 * {@link org.apache.juneau.marshall.serializer.SerializerSession SerializerSession}.
	 *
	 * @param output The output.  Subtypes define the supported types (typically
	 * 	{@link Writer} / {@link OutputStream} / {@link File} / {@link StringBuilder}).
	 * @return A new {@link RecordWriter} cursor.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	RecordWriter serializeRecords(Object output) throws IOException;

	/**
	 * Streaming-quality signal queryable <em>before</em> opening a cursor.
	 *
	 * @return <jk>true</jk> if {@link #serializeRecords(Object)} returns an O(1)-memory streaming
	 * 	cursor; <jk>false</jk> for buffered (whole-value) formats backed by
	 * 	{@link RecordAdapter}.
	 */
	default boolean isRecordStreaming() { return true; }
}
