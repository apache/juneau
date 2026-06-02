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
package org.apache.juneau.json5;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.json.JsonParser;

/**
 * Parses any valid JSON text into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/json5, text/json5</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Identical to {@link JsonParser} but accepts JSON5 syntax (single quotes, comments, trailing commas,
 * unquoted keys) and uses media type <bc>application/json5</bc>. Use this parser when the input may
 * contain JSON5-style syntax.
 *
 * <h5 class='figure'>Example input (Map of name/age):</h5>
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
 * 	<li class='link'>{@link JsonParser} - For strict RFC 8259 JSON only
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for Json5Parser hierarchy
	"java:S115"  // Constants use naming conventions that embed type info or config keys (e.g. PROP_escapeSolidus)
})
public class Json5Parser extends JsonParser {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonParser.Builder<Builder> {

		private static final Cache<HashKey,Json5Parser> CACHE = Cache.of(HashKey.class, Json5Parser.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/json5,text/json5,application/json,text/json");
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
		protected Builder(Json5Parser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder<?> */
		public Json5Parser build() {
			return cache(CACHE).build(Json5Parser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}


	}

	/** Default parser, Accept=application/json5. */
	public static final Json5Parser DEFAULT = new Json5Parser(create());

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
	public Json5Parser(Builder builder) {
		super(builder);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public Json5Parser(JsonParser.Builder<?> builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public Json5ParserSession.Builder createSession() {
		return Json5ParserSession.create(this);
	}

	@Override /* Overridden from Context */
	public Json5ParserSession getSession() { return createSession().build(); }
}
