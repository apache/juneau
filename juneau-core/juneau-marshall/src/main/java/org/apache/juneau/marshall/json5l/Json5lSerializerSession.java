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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link Json5lSerializer}.
 *
 * <p>
 * Extends {@link JsonlSerializerSession} to inherit JSONL's line-delimited framing.  When the
 * owning {@link Json5lSerializer} has {@link Json5lSerializer.Builder#json5Sugar() json5Sugar}
 * enabled, the per-line output switches to JSON5 sugar (single-quoted strings, unquoted field names
 * where safe) on both the databind and token-streaming paths; otherwise the inherited strict
 * RFC-8259 output is used unchanged.
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
	"resource", // Resource management handled externally
	"java:S110", // Inheritance depth acceptable
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class Json5lSerializerSession extends JsonlSerializerSession {

	private static final String ARG_ctx = "ctx";

	private static final char SUGAR_QUOTE = '\'';

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonlSerializerSession.Builder {

		final Json5lSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Json5lSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public Json5lSerializerSession build() {
			return new Json5lSerializerSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(Json5lSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final boolean json5Sugar;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected Json5lSerializerSession(Builder builder) {
		super(builder);
		json5Sugar = builder.ctx.isJson5Sugar();
	}

	@Override /* Overridden from JsonSerializerSession */
	protected JsonWriter getJsonWriter(SerializerPipe out) {
		if (! json5Sugar)
			return super.getJsonWriter(out);
		var output = out.getRawOutput();
		if (output instanceof JsonWriter output2)
			return output2;
		var w = JsonWriter.create(out.getWriter(), isUseWhitespace(), getMaxIndent(), isEscapeSolidus(), SUGAR_QUOTE, true, isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	@Override /* Overridden from JsonlSerializerSession */
	public TokenWriter writeTokens(Object output) throws IOException {
		if (! json5Sugar)
			return super.writeTokens(output);
		var walk = new PojoWalker.Options(
			isKeepNullProperties(),
			isTrimEmptyMaps(),
			isTrimEmptyCollections(),
			isSortMaps(),
			isSortCollections(),
			isTrimStrings(),
			getMarshallingContext());
		var settings = new JsonTokenWriter.Settings(
			false /* useWhitespace — JSON5L is one-record-per-line, no pretty-print */,
			getMaxIndent(),
			SUGAR_QUOTE,
			isEscapeSolidus(),
			isTrimStrings(),
			true /* simpleAttrs — JSON5 sugar: unquoted field names where safe */,
			walk,
			false /* disableObject */);
		return JsonlTokenWriter.forOutput(output, settings);
	}
}
