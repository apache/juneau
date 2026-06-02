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
package org.apache.juneau.yaml;

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.json.JsonParser;
import org.apache.juneau.parser.*;

/**
 * Parses YAML text into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/yaml, text/yaml</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This parser uses an indentation-aware state machine to parse YAML directly into POJOs without intermediate
 * DOM objects.  It supports block-style and flow-style mappings and sequences, plain and quoted scalars,
 * comments, document markers, and standard YAML scalar types.
 *
 * <h5 class='section'>Limitations compared to JSON</h5>
 * <p>
 * The YAML parser has some limitations when compared to {@link JsonParser}:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps with non-String keys ({@link Boolean}, {@link java.util.Date}, {@link java.time.temporal.Temporal},
 * 		{@link Enum}) may not round-trip correctly when the target type is a generic {@link java.util.Map} with
 * 		those key types.  {@link java.util.LinkedHashMap LinkedHashMap} and {@link java.util.TreeMap TreeMap}
 * 		with {@link String} keys work reliably.
 * 	<li>
 * 		No strict vs non-strict mode; unlike JSON, there is no equivalent to JSON's lax parsing of comments,
 * 		unquoted attributes, or concatenated strings.
 * 	<li>
 * 		YAML's indentation-based structure requires consistent formatting; malformed indentation can cause
 * 		parsing errors where equivalent JSON would parse successfully.
 * </ul>
 *
 * <h5 class='figure'>Example input (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name: Alice
 * 	age: 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	name: Alice
 * 	age: 30
 * 	address:
 * 	  street: 123 Main St
 * 	  city: Boston
 * 	  state: MA
 * 	tags:
 * 	- a
 * 	- b
 * 	- c
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/YamlBasics">YAML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class YamlParser extends ReaderParser {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParser.Builder<Builder> {

		private static final Cache<HashKey,YamlParser> CACHE = Cache.of(HashKey.class, YamlParser.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/yaml,text/yaml");
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
		protected Builder(YamlParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder */
		public YamlParser build() {
			return cache(CACHE).build(YamlParser.class);
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey());
		}


	}

	/** Default parser, all default settings.*/
	public static final YamlParser DEFAULT = new YamlParser(create());

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
	public YamlParser(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public YamlParserSession.Builder createSession() {
		return YamlParserSession.create(this);
	}

	@Override /* Overridden from Context */
	public YamlParserSession getSession() { return createSession().build(); }
}
