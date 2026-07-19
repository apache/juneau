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
package org.apache.juneau.rest.client;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * A streaming HTTP request body that emits its content through a token/record-streaming cursor.
 *
 * <p>
 * Lets a caller pass a {@code Consumer<RecordWriter>} or {@code Consumer<TokenWriter>} as the body of a request.
 * The consumer is invoked lazily during {@link #writeTo(OutputStream)} &mdash; the cursor writes directly to the
 * live transport output stream, so arbitrarily large payloads are streamed to the wire without being buffered in
 * memory first.
 *
 * <p>
 * While the next-generation client negotiates a parser for inbound/response bodies (from the response
 * {@code Content-Type}), the request-body OUTPUT format here is an explicit caller choice: the {@link Serializer}
 * that backs the cursor is supplied directly (defaulting to {@link JsonSerializer#DEFAULT}).  The body's
 * {@link #getContentType()} reflects that serializer's media type.
 *
 * <h5 class='section'>Repeatability:</h5>
 * <p>
 * Streaming bodies are <b>non-repeatable</b> by default ({@link #isRepeatable()} returns {@code false}) because the
 * caller's consumer may be backed by a one-shot source.  A non-repeatable body fails fast (throws {@link IOException})
 * if a resend is required (i.e. {@link #writeTo(OutputStream)} is invoked more than once).  Callers whose consumer can
 * be replayed safely may opt in via {@link #repeatable()}.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<ja>@Remote</ja>
 * 	<jk>public interface</jk> MyApi {
 *
 * 		<ja>@RemotePost</ja>(<js>"/bulk-upload"</js>)
 * 		<jk>void</jk> upload(<ja>@Content</ja> RecordStreamBody <jv>body</jv>);
 * 	}
 *
 * 	<jc>// Caller</jc>
 * 	<jv>api</jv>.upload(RecordStreamBody.<jsm>records</jsm>(<jv>w</jv> -&gt; {
 * 		<jk>for</jk> (Bean <jv>b</jv> : <jv>source</jv>())
 * 			<jv>w</jv>.write(<jv>b</jv>);
 * 	}));
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class RecordStreamBody implements HttpBody {

	/**
	 * Creates a body that streams via a {@link RecordWriter} (whole-value record cursor) using the default JSON serializer.
	 *
	 * @param consumer The callback that emits records. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static RecordStreamBody records(Consumer<RecordWriter> consumer) {
		return records(JsonSerializer.DEFAULT, consumer);
	}

	/**
	 * Creates a body that streams via a {@link RecordWriter} (whole-value record cursor) using the given serializer.
	 *
	 * @param serializer The serializer that opens the record cursor. Must not be <jk>null</jk> and must implement
	 * 	{@link RecordWritable}.
	 * @param consumer The callback that emits records. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the serializer does not support the record-writer surface.
	 */
	public static RecordStreamBody records(Serializer serializer, Consumer<RecordWriter> consumer) {
		assertArgNotNull("serializer", serializer);
		assertArgNotNull("consumer", consumer);
		if (! (serializer instanceof RecordWritable))
			throw iaex("Serializer '%s' does not support the record-writer surface.", serializer.getClass().getName());
		return new RecordStreamBody(serializer, consumer, RecordWriter.class, false);
	}

	/**
	 * Creates a body that streams via a {@link TokenWriter} (fine-grained structural cursor) using the default JSON serializer.
	 *
	 * @param consumer The callback that emits structural events. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static RecordStreamBody token(Consumer<TokenWriter> consumer) {
		return token(JsonSerializer.DEFAULT, consumer);
	}

	/**
	 * Creates a body that streams via a {@link TokenWriter} (fine-grained structural cursor) using the given serializer.
	 *
	 * @param serializer The serializer that opens the token cursor. Must not be <jk>null</jk> and must implement
	 * 	{@link TokenWritable}.
	 * @param consumer The callback that emits structural events. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the serializer does not support the token-writer surface.
	 */
	public static RecordStreamBody token(Serializer serializer, Consumer<TokenWriter> consumer) {
		assertArgNotNull("serializer", serializer);
		assertArgNotNull("consumer", consumer);
		if (! (serializer instanceof TokenWritable))
			throw iaex("Serializer '%s' does not support the token-writer surface.", serializer.getClass().getName());
		return new RecordStreamBody(serializer, consumer, TokenWriter.class, false);
	}

	private final Serializer serializer;
	private final Consumer<?> consumer;
	private final Class<?> writerKind;
	private final boolean repeatable;
	private boolean written;

	private RecordStreamBody(Serializer serializer, Consumer<?> consumer, Class<?> writerKind, boolean repeatable) {
		this.serializer = serializer;
		this.consumer = consumer;
		this.writerKind = writerKind;
		this.repeatable = repeatable;
	}

	/**
	 * Returns a copy of this body flagged as repeatable.
	 *
	 * <p>
	 * Only opt in when the caller's consumer can be safely replayed (e.g. it iterates a re-readable source).
	 *
	 * @return A new repeatable body. Never <jk>null</jk>.
	 */
	public RecordStreamBody repeatable() {
		return new RecordStreamBody(serializer, consumer, writerKind, true);
	}

	@Override /* HttpBody */
	public String getContentType() {
		var mt = serializer.getResponseContentType();
		return mt == null ? null : mt.toString();
	}

	@Override /* HttpBody */
	public long getContentLength() {
		return -1;
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return repeatable;
	}

	@Override /* HttpBody */
	@SuppressWarnings({
		"unchecked", // Consumer is paired with writerKind at construction; the cast matches the opened cursor type.
		"resource"   // The cursor wraps the caller-owned 'out'; its close() flushes but does not close 'out'.
	})
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);
		if (written && ! repeatable)
			throw new IOException("Non-repeatable streaming body cannot be resent.  Mark the body repeatable() if its producer can be replayed.");
		written = true;
		try (var w = (writerKind == TokenWriter.class)
				? ((TokenWritable) serializer).writeTokens(out)
				: ((RecordWritable) serializer).writeRecords(out)) {
			((Consumer<RecordWriter>) consumer).accept(w);
			w.flush();
		}
	}
}
