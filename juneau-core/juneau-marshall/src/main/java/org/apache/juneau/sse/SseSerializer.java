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
package org.apache.juneau.sse;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes {@link SseEvent} objects to the <c>text/event-stream</c> wire format defined by the
 * <a class="doclink" href="https://html.spec.whatwg.org/multipage/server-sent-events.html">WHATWG Server-Sent Events spec</a>.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Produces <c>Content-Type</c> types: <bc>text/event-stream</bc>.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Accepts any of the following as input and emits one wire event per element followed by a blank
 * line (the spec's "dispatch" boundary):
 * <ul class='spaced-list'>
 * 	<li>A single {@link SseEvent}.
 * 	<li>{@link Iterable Iterable&lt;SseEvent&gt;}.
 * 	<li>An array {@code SseEvent[]}.
 * 	<li>{@link java.util.stream.Stream Stream&lt;SseEvent&gt;}.
 * </ul>
 *
 * <p>
 * After each event is written, the session calls {@link Writer#flush() Writer.flush()} so that downstream
 * REST infrastructure flushes the underlying TCP socket and the event is delivered immediately to the
 * client.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SseBasics">SSE Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for serializer hierarchy
})
public class SseSerializer extends WriterSerializer {

	@SuppressWarnings({
		"java:S115" // Match AssertionUtils arg-name style used throughout Juneau.
	})
	private static final String ARG_w = "w";

	/**
	 * Wire-format media type produced by this serializer: <c>text/event-stream</c>.
	 */
	public static final String MEDIA_TYPE = "text/event-stream";

	/** Default serializer, all default settings. */
	public static final SseSerializer DEFAULT = new SseSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder<Builder> {

		private static final Cache<HashKey,SseSerializer> CACHE = Cache.of(HashKey.class, SseSerializer.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces(MEDIA_TYPE);
			type(SseSerializer.class);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from. Must not be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull("copyFrom", copyFrom));
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The serializer to copy from. Must not be <jk>null</jk>.
		 */
		protected Builder(SseSerializer copyFrom) {
			super(assertArgNotNull("copyFrom", copyFrom));
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey());
		}

		@Override /* Overridden from Context.Builder<?> */
		public SseSerializer build() {
			return cache(CACHE).build(SseSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public SseSerializer(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public SseSerializerSession.Builder createSession() {
		return SseSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public SseSerializerSession getSession() {
		return createSession().build();
	}

	/**
	 * Writes a comment line (heartbeat) to the supplied writer in the SSE wire format and flushes it.
	 *
	 * @param w The writer. Must not be <jk>null</jk>.
	 * @param comment The comment text. {@code null} is treated as an empty string.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 */
	@SuppressWarnings({ "java:S2095", "resource" }) // Writer ownership is the caller's; we only write/flush.
	public static void writeComment(Writer w, String comment) throws IOException {
		assertArgNotNull(ARG_w, w);
		var c = comment == null ? "" : comment;
		for (var line : c.split("\n", -1))
			w.write(": " + line + "\n");
		w.write("\n");
		w.flush();
	}
}
