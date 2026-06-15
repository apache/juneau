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
package org.apache.juneau.marshall.sse;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link SseParser}.
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
	"java:S110",   // Inheritance depth acceptable for session hierarchy
	"java:S115",   // Match AssertionUtils arg-name style used throughout Juneau.
	"unchecked",   // Type erasure: SseEvent/List<SseEvent> returned via Object cast
	"resource"     // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class SseParserSession extends ReaderParserSession implements RecordReadable {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParserSession.Builder<Builder> {

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session. Cannot be <jk>null</jk>.
		 */
		protected Builder(SseParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public SseParserSession build() {
			return new SseParserSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session. Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(SseParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected SseParserSession(Builder builder) {
		super(builder);
	}

	@Override /* RecordReadable */
	public RecordReader parseRecords(Object input) throws IOException {
		return RecordAdapter.arrayReader(this, input);
	}

	@Override /* RecordReadable */
	public boolean isRecordStreaming() {
		return false;
	}

	@Override /* Overridden from ParserSession */
	@SuppressWarnings({
		"java:S2095" // Reader is owned by ParserPipe and closed via SseEventReader in try-with-resources.
	})
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		var r = pipe.getReader();
		if (r == null)
			return null;
		List<SseEvent> events;
		try (var r2 = new SseEventReader(r)) {
			events = r2.toList();
		}
		if (type == null || type.isObject() || type.isCollectionOrArray()) {
			if (type != null && type.isArray())
				return (T) events.toArray(new SseEvent[0]);
			return (T) events;
		}
		if (SseEvent.class.equals(type.inner())) {
			return events.isEmpty() ? null : (T) events.get(0);
		}
		if (List.class.isAssignableFrom(type.inner()))
			return (T) events;
		throw new ParseException(this, "SseParser cannot produce target type ''{0}''.", type);
	}
}
