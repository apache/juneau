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
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link SseSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SseBasics">SSE Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for session hierarchy
})
public class SseSerializerSession extends WriterSerializerSession {

	@SuppressWarnings({
		"java:S115" // Match AssertionUtils arg-name style used throughout Juneau.
	})
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializerSession.Builder<Builder> {

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session. Cannot be <jk>null</jk>.
		 */
		protected Builder(SseSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public SseSerializerSession build() {
			return new SseSerializerSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session. Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(SseSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected SseSerializerSession(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from SerializerSession */
	@SuppressWarnings({ "java:S2095", "resource" }) // Writer is owned by SerializerPipe/caller and must not be closed by this session.
	protected void doSerialize(SerializerPipe pipe, Object o) throws IOException, SerializeException {
		var w = pipe.getWriter();
		if (o == null)
			return;
		if (o instanceof SseEvent e) {
			writeEvent(w, e);
		} else if (o instanceof Iterable<?> i) {
			for (var item : i)
				writeIfEvent(w, item);
		} else if (o instanceof Stream<?> s) {
			try (var s2 = s) {
				s2.forEach(item -> writeIfEventUnchecked(w, item));
			}
		} else if (o instanceof SseEvent[] arr) {
			for (var e : arr)
				writeEvent(w, e);
		} else if (o.getClass().isArray()) {
			var arr = (Object[]) o;
			for (var item : arr)
				writeIfEvent(w, item);
		} else {
			throw new SerializeException("SseSerializer cannot serialize objects of type ''{0}''.", o.getClass().getName());
		}
	}

	private static void writeIfEvent(Writer w, Object item) throws IOException, SerializeException {
		if (item == null)
			return;
		if (!(item instanceof SseEvent e))
			throw new SerializeException("SseSerializer encountered non-SseEvent element of type ''{0}''.", item.getClass().getName());
		writeEvent(w, e);
	}

	private static void writeIfEventUnchecked(Writer w, Object item) {
		try {
			writeIfEvent(w, item);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SerializeException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static void writeEvent(Writer w, SseEvent e) throws IOException {
		if (e == null)
			return;
		if (e.getEvent() != null && !e.getEvent().isEmpty())
			writeField(w, "event", e.getEvent());
		if (e.getData() != null) {
			for (var line : splitData(e.getData()))
				writeField(w, "data", line);
		}
		if (e.getId() != null)
			writeField(w, "id", e.getId());
		if (e.getRetry() != null)
			w.write("retry: " + e.getRetry() + "\n");
		w.write("\n");
		w.flush();
	}

	private static void writeField(Writer w, String name, String value) throws IOException {
		w.write(name);
		w.write(": ");
		w.write(value);
		w.write('\n');
	}

	static List<String> splitData(String s) {
		var parts = s.split("\n", -1);
		return Arrays.asList(parts);
	}
}
