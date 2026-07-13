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

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Parses <c>text/event-stream</c> input (WHATWG Server-Sent Events) into Java objects.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types: <bc>text/event-stream</bc>.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Eager parse path — drains the stream into a {@link java.util.List List&lt;SseEvent&gt;}.
 * For true line-driven streaming over long-lived connections, use {@link SseEventReader} directly.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Sse">SSE Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for parser hierarchy
	"resource" // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class SseParser extends ReaderParser implements RecordReadable {

	/** Default parser, all default settings. */
	public static final SseParser DEFAULT = new SseParser(create());

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
	public static class Builder extends ReaderParser.Builder<Builder> {

		private static final Cache<HashKey,SseParser> CACHE = Cache.of(HashKey.class, SseParser.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes(SseSerializer.MEDIA_TYPE);
			type(SseParser.class);
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
		 * @param copyFrom The parser to copy from. Must not be <jk>null</jk>.
		 */
		protected Builder(SseParser copyFrom) {
			super(assertArgNotNull("copyFrom", copyFrom));
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey());
		}

		@Override /* Overridden from Context.Builder<?> */
		public SseParser build() {
			return cache(CACHE).build(SseParser.class);
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
	public SseParser(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public SseParserSession.Builder createSession() {
		return SseParserSession.create(this);
	}

	@Override /* Overridden from Context */
	public SseParserSession getSession() {
		return createSession().build();
	}

	/**
	 * Convenience delegator that opens a {@link RecordReader} over the input using
	 * <b>default session arguments</b> (mirrors {@link #parse(Object, Class)}).
	 *
	 * <p>
	 * The real implementation lives on {@link SseParserSession#parseRecords(Object)}.  Callers
	 * that need request-derived configuration (locale, timezone, schema, swaps) should call
	 * {@link #createSession()} and invoke {@link SseParserSession#parseRecords(Object)} on the
	 * built session instead.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* RecordReadable */
	public RecordReader parseRecords(Object input) throws IOException {
		return getSession().parseRecords(input);
	}

	@Override /* RecordReadable */
	public boolean isRecordStreaming() {
		return false;
	}
}
