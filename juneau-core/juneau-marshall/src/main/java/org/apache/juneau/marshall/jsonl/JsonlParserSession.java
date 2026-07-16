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
package org.apache.juneau.marshall.jsonl;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonlParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonlSupport">JSONL Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"unchecked", // Type erasure: elementType is ClassMeta<?>; toArray/convertToType return Object
	"java:S110", // Inheritance depth acceptable
	"java:S115", // Constants use UPPER_snakeCase convention
	"resource"   // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class JsonlParserSession extends JsonParserSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"unused" // ctx passed to super(); parent uses it; Builder stores for constructor chain
	})
	public static class Builder extends JsonParserSession.Builder<Builder> {

		private JsonlParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(JsonlParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public JsonlParserSession build() {
			return new JsonlParserSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(JsonlParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonlParserSession(Builder builder) {
		super(builder);
	}

	/**
	 * Opens a low-level pull-parser cursor over a JSONL document, bound to this live session.
	 *
	 * <p>
	 * Each top-level JSON value (one per line) is emitted as a flat sequence at depth 0; there
	 * is no virtual root container.  See {@link JsonlTokenReader} for the contract.  Same
	 * honored/ignored builder properties as {@link JsonParserSession#parseTokens(Object)}.
	 *
	 * @param input The input.  Accepts {@link Reader}, {@link CharSequence},
	 * 	{@link InputStream}, or <code><jk>byte</jk>[]</code>.
	 * @return A new {@link JsonlTokenReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@SuppressWarnings({
		"java:S2095" // ParserPipe lifecycle is transferred to the returned JsonlTokenReader, which closes it via its own close(); the caller owns the cursor via try-with-resources.
	})
	@Override /* JsonParserSession */
	public TokenReader parseTokens(Object input) throws IOException {
		var pipe = new ParserPipe(
			input,
			isDebug(),
			false /* strict */,
			isAutoCloseStreams(),
			isUnbuffered(),
			getStreamCharset());
		return new JsonlTokenReader(pipe, new JsonTokenReader.Settings(isTrimStrings()), this);
	}

	/**
	 * Streaming array-element {@link RecordReader} for JSONL.
	 *
	 * <p>
	 * Unlike the inherited {@link JsonParserSession#parseArrayRecords(Object)} &mdash; which expects a
	 * bracketed top-level <c>[...]</c> array and is invalid for line-delimited JSONL &mdash; this aliases
	 * the JSONL line record stream: the {@link JsonlTokenReader} returned by {@link #parseTokens(Object)}
	 * is itself a {@link RecordReader} that yields one record per top-level line at depth 0.  Memory is
	 * O(1) in the number of lines.
	 *
	 * @param input The input.
	 * @return A new line-delimited {@link RecordReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader parseArrayRecords(Object input) throws IOException {
		return parseTokens(input);
	}

	@Override /* Overridden from JsonParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (var r = pipe.getParserReader()) {
			if (r == null)
				return null;

			if (type.isCollectionOrArray()) {
				var elementType = type.getElementType();
				var results = newGenericList();
				var br = new BufferedReader(r);
				String line;
				while ((line = br.readLine()) != null) {
					var trimmed = line.trim();
					if (!trimmed.isEmpty()) {
						try (var linePipe = createPipe(trimmed)) {
							var item = super.doParse(linePipe, elementType);
							results.add(item);
						}
					}
				}
				return type.isArray() ? (T) toArray(type, results) : (T) convertToType(results, type);
			}

			// Single object: parse just the first non-empty line
			var br = new BufferedReader(r);
			String line;
			while ((line = br.readLine()) != null) {
				var trimmed = line.trim();
				if (!trimmed.isEmpty()) {
					try (var linePipe = createPipe(trimmed)) {
						return super.doParse(linePipe, type);
					}
				}
			}
			return null;
		}
	}
}
