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
package org.apache.juneau.jsonl;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.reflect.ExecutableException;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonlParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonlBasics">JSONL Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"unchecked", // Type erasure: elementType is ClassMeta<?>; toArray/convertToType return Object
	"java:S110", // Inheritance depth acceptable
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class JsonlParserSession extends JsonParserSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"unused" // ctx passed to super(); parent uses it; Builder stores for constructor chain
	})
	public static class Builder extends JsonParserSession.Builder {

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

	@Override /* Overridden from JsonParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (var r = pipe.getParserReader()) {
			if (r == null)
				return null;

			if (type.isCollectionOrArray()) {
				var elementType = type.getElementType();
				var results = new JsonList(this);
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
