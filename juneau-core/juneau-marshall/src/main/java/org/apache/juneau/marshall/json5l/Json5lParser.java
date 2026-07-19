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
import org.apache.juneau.marshall.json5.*;

/**
 * Parses JSON5L (JSON5 Lines) input into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/json5l, text/json5l</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * JSON5L combines the relaxed JSON5 dialect with JSONL's newline-delimited framing.  Each non-empty,
 * non-comment line is parsed as a complete JSON5 value (single-quoted strings, unquoted field names,
 * trailing commas, comments, relaxed numbers).  Because JSON5 is a strict superset of JSON, this
 * parser also reads plain JSONL input unchanged.
 *
 * <p>
 * Comment handling follows JSON5 line-by-line: a line that is blank or contains only a comment is
 * skipped (like a blank line); inline comments within a record's JSON are tolerated.  A
 * <c>/* &#42;/</c> block comment must open and close on the same physical line — block comments
 * cannot span record boundaries.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse JSON5L into a list of POJOs</jc>
 * 	List&lt;MyBean&gt; <jv>list</jv> = Json5lParser.<jsf>DEFAULT</jsf>.read(<jv>json5lInput</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example input:</h5>
 * <p class='bjson'>
 * // header comment line
 * {name:'Alice',age:30}
 * {name:'Bob',age:25}  // trailing comment
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Json5l">JSON5L Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for Json5lParser hierarchy
	"java:S115"  // Constants use naming conventions that embed type info or config keys
})
public class Json5lParser extends Json5Parser {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends Json5Parser.Builder {

		private static final Cache<HashKey,Json5lParser> CACHE = Cache.of(HashKey.class, Json5lParser.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/json5l,text/json5l,application/jsonl,application/x-ndjson,text/jsonl")
				.type(Json5lParser.class);
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
		protected Builder(Json5lParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder<?> */
		public Json5lParser build() {
			return cache(CACHE).build(Json5lParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}
	}

	/** Default parser, Accept=application/json5l. */
	public static final Json5lParser DEFAULT = new Json5lParser(create());

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
	public Json5lParser(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public Json5lParserSession.Builder createSession() {
		return Json5lParserSession.create(this);
	}

	@Override /* Overridden from Context */
	public Json5lParserSession getSession() {
		return createSession().build();
	}
}
