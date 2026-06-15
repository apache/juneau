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
package org.apache.juneau.marshall.json5;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.json.*;

/**
 * Serializes POJO models to Simplified JSON.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/json, text/json</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/json5</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * 	This is a JSON serializer that uses simplified notation:
 * <ul class='spaced-list'>
 * 	<li>Lax quoting of JSON attribute names.
 * 	<li>Single quotes.
 * </ul>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{name:<js>"Alice"</js>,age:30}
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bjson'>
 * 	{name:<js>"Alice"</js>,age:30,address:{street:<js>"123 Main St"</js>,city:<js>"Boston"</js>,state:<js>"MA"</js>},tags:[<js>"a"</js>,<js>"b"</js>,<js>"c"</js>]}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonBasics">JSON Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable
	"java:S115"  // Constants use naming conventions that embed type info or config keys (e.g. PROP_escapeSolidus)
})
public class Json5Serializer extends JsonSerializer {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonSerializer.Builder<Builder> {

		private static final Cache<HashKey,Json5Serializer> CACHE = Cache.of(HashKey.class, Json5Serializer.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			quoteChar('\'')
				.produces("application/json5")
				.accept("application/json5,text/json5,application/json;q=0.9,text/json;q=0.9");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Json5Serializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder<?> */
		public Json5Serializer build() {
			return cache(CACHE).build(Json5Serializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}


	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class Readable extends Json5Serializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder builder) {
			super(builder.useWhitespace());
		}
	}

	/** Default serializer, single quotes. */
	public static final Json5Serializer DEFAULT = new Json5Serializer(create());

	/** Default serializer, single quotes, with whitespace. */
	public static final Json5Serializer DEFAULT_READABLE = new Readable(create());

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
	public Json5Serializer(Builder builder) {
		super(builder);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public Json5Serializer(JsonSerializer.Builder<?> builder) {
		super(builder.quoteChar('\''));
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public Json5SerializerSession.Builder createSession() {
		return Json5SerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public Json5SerializerSession getSession() { return createSession().build(); }

}
