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

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonl.*;

/**
 * Serializes POJO models to JSON5L (JSON5 Lines).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/json5l, text/json5l</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/json5l</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * JSON5L combines the relaxed JSON5 dialect with JSONL's newline-delimited framing: each top-level
 * value is written as a compact document on its own line, exactly as {@link JsonlSerializer} does.
 * <p>
 * By <b>default</b> the per-line output is strict RFC-8259 JSON (byte-identical to
 * {@link JsonlSerializer}), keeping output maximally portable.  The {@link Builder#json5Sugar()
 * json5Sugar()} opt-in switches the per-line output to JSON5 sugar (single-quoted strings, unquoted
 * field names where safe).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Strict-per-line output (default)</jc>
 * 	String <jv>json5l</jv> = Json5lSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myList</jv>);
 *
 * 	<jc>// JSON5-sugar-per-line output</jc>
 * 	Json5lSerializer <jv>serializer</jv> = Json5lSerializer.<jsm>create</jsm>().json5Sugar().build();
 * 	String <jv>json5l</jv> = <jv>serializer</jv>.serialize(<jv>myList</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output, sugar off (List of beans):</h5>
 * <p class='bjson'>
 * {"name":"Alice","age":30}
 * {"name":"Bob","age":25}
 * </p>
 *
 * <h5 class='figure'>Example output, sugar on:</h5>
 * <p class='bjson'>
 * {name:'Alice',age:30}
 * {name:'Bob',age:25}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Json5lBasics">JSON5L Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable
})
public class Json5lSerializer extends JsonlSerializer {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonSerializer.Builder<Builder> {

		private static final Cache<HashKey,Json5lSerializer> CACHE = Cache.of(HashKey.class, Json5lSerializer.class).build();

		boolean json5Sugar;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/json5l")
				.accept("application/json5l,text/json5l,application/jsonl;q=0.9,application/x-ndjson;q=0.9,text/jsonl;q=0.9")
				.type(Json5lSerializer.class)
				.useWhitespace(false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			json5Sugar = copyFrom.json5Sugar;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Json5lSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			json5Sugar = copyFrom.json5Sugar;
		}

		/**
		 * Emit JSON5 sugar (single-quoted strings, unquoted field names where safe) on each line
		 * instead of strict RFC-8259 JSON.
		 *
		 * <p>
		 * Off by default, in which case the per-line output is byte-identical to
		 * {@link JsonlSerializer}.
		 *
		 * @return This object.
		 */
		public Builder json5Sugar() {
			json5Sugar = true;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), json5Sugar);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Json5lSerializer build() {
			return cache(CACHE).build(Json5lSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}
	}

	/** Default serializer, strict-JSON-per-line. */
	public static final Json5lSerializer DEFAULT = new Json5lSerializer(create());

	final boolean json5Sugar;

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public Json5lSerializer(Builder builder) {
		super(builder.useWhitespace(false));
		json5Sugar = builder.json5Sugar;
	}

	/**
	 * Returns <jk>true</jk> if this serializer emits JSON5 sugar on each line.
	 *
	 * @return <jk>true</jk> if JSON5 sugar is enabled.
	 */
	public boolean isJson5Sugar() {
		return json5Sugar;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public Json5lSerializerSession.Builder createSession() {
		return Json5lSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public Json5lSerializerSession getSession() {
		return createSession().build();
	}
}
