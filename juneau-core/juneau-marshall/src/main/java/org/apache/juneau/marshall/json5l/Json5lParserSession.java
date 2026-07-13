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
package org.apache.juneau.marshall.json5l;

import java.io.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link Json5lParser}.
 *
 * <p>
 * Extends {@link Json5ParserSession} to inherit the full JSON5 dialect (single-quoted strings,
 * bare/unquoted field names, trailing commas, missing values, comments, relaxed numbers) and
 * re-adds JSONL's newline-delimited framing: input is read one line at a time, blank and
 * comment-only lines are skipped, and each remaining line is parsed as one JSON5 value.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Json5l">JSON5L Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"unchecked", // Type erasure: elementType is ClassMeta<?>; toArray/convertToType return Object
	"java:S110", // Inheritance depth acceptable
	"resource"   // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class Json5lParserSession extends Json5ParserSession {

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth is intentional across parser session builders
	})
	public static class Builder extends Json5ParserSession.Builder {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(Json5lParser ctx) {
			super(ctx);
		}

		@Override
		public Json5lParserSession build() {
			return new Json5lParserSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(Json5lParser ctx) {
		return new Builder(ctx);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected Json5lParserSession(Builder builder) {
		super(builder);
	}

	/**
	 * Opens a low-level pull-parser cursor over a JSON5L document, bound to this live session.
	 *
	 * <p>
	 * Each top-level JSON5 value (one per line) is emitted as a flat sequence at depth 0; there is
	 * no virtual root container.  Same honored/ignored builder properties as
	 * {@link Json5ParserSession#parseTokens(Object)}.
	 *
	 * @param input The input.  Accepts {@link Reader}, {@link CharSequence}, {@link InputStream},
	 * 	<code><jk>byte</jk>[]</code>, or {@link File}.
	 * @return A new {@link Json5lTokenReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@SuppressWarnings({
		"java:S2095" // ParserPipe lifecycle is transferred to the returned Json5lTokenReader, which closes it via its own close(); the caller owns the cursor via try-with-resources.
	})
	@Override /* Json5ParserSession */
	public TokenReader parseTokens(Object input) throws IOException {
		var pipe = new ParserPipe(
			input,
			isDebug(),
			false /* strict */,
			isAutoCloseStreams(),
			isUnbuffered(),
			getStreamCharset(),
			getFileCharset());
		return new Json5lTokenReader(pipe, new JsonTokenReader.Settings(isTrimStrings()), this);
	}

	/**
	 * Streaming array-element {@link RecordReader} for JSON5L.
	 *
	 * <p>
	 * Aliases the JSON5L line record stream: the {@link Json5lTokenReader} returned by
	 * {@link #parseTokens(Object)} is itself a {@link RecordReader} that yields one record per
	 * top-level line at depth 0.  Memory is O(1) in the number of lines.
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
					if (isParseable(trimmed)) {
						try (var linePipe = createPipe(trimmed)) {
							var item = super.doParse(linePipe, elementType);
							results.add(item);
						}
					}
				}
				return type.isArray() ? (T) toArray(type, results) : (T) convertToType(results, type);
			}

			// Single object: parse just the first non-empty, non-comment line
			var br = new BufferedReader(r);
			String line;
			while ((line = br.readLine()) != null) {
				var trimmed = line.trim();
				if (isParseable(trimmed)) {
					try (var linePipe = createPipe(trimmed)) {
						return super.doParse(linePipe, type);
					}
				}
			}
			return null;
		}
	}

	/**
	 * Returns <jk>true</jk> if the trimmed line contains a JSON5 value to parse — i.e. it is neither
	 * blank nor a comment-only line (a line consisting solely of a <c>//</c> line comment or a
	 * single-line <c>/* &#42;/</c> block comment).
	 *
	 * @param trimmed The whitespace-trimmed line.
	 * @return <jk>true</jk> if the line should be parsed as a JSON5 value.
	 */
	private static boolean isParseable(String trimmed) {
		if (trimmed.isEmpty())
			return false;
		if (trimmed.startsWith("//"))
			return false;
		// A line that is exactly a single block comment (possibly with surrounding whitespace,
		// already trimmed) is comment-only.  Anything after the closing */ makes it parseable.
		return !(trimmed.startsWith("/*") && trimmed.endsWith("*/") && trimmed.indexOf("*/") == trimmed.length() - 2);
	}
}
