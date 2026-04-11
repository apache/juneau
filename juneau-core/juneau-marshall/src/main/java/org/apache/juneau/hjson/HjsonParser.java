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
package org.apache.juneau.hjson;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.JsonList;
import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.parser.*;

/**
 * Parses Hjson (Human JSON) text into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/hjson, application/hjson+json</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This parser handles all standard Hjson syntax as defined by the
 * <a class="doclink" href="https://hjson.github.io/syntax.html">Hjson specification</a>.
 *
 * <p>
 * In addition to standard JSON, this parser handles the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@code #} single-line comments (to end of line).
 * 	<li>
 * 		{@code //} single-line comments.
 * 	<li>
 * 		{@code /* ... *}{@code /} block comments.
 * 	<li>
 * 		Quoteless string values (text read to end of line, leading/trailing whitespace trimmed).
 * 	<li>
 * 		Multiline strings delimited by {@code '''} (triple single-quote) with indentation stripping.
 * 	<li>
 * 		Optional commas — newlines may serve as member/element separators.
 * 	<li>
 * 		Trailing commas (ignored).
 * 	<li>
 * 		Unquoted keys.
 * 	<li>
 * 		Root-braceless objects (top-level content without wrapping {@code {}}).
 * </ul>
 *
 * <p>
 * Value type disambiguation: the parser identifies value types in this order:
 * number → boolean → null → quoteless string.
 * A value such as {@code 5 times} that starts like a number but contains non-numeric
 * characters is treated as a quoteless string.
 *
 * <p>
 * This parser handles the following input and returns the corresponding Java type:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Hjson/JSON objects ({@code {...}}) are converted to {@link JsonMap JsonMaps}.
 * 		<b>Note:</b> If a <code><xa>_type</xa>=<xs>'xxx'</xs></code> attribute is specified on the object,
 * 		an attempt is made to convert the object to an instance of the specified Java bean class.
 * 	<li>
 * 		Hjson/JSON arrays ({@code [...]}) are converted to {@link JsonList JsonLists}.
 * 	<li>
 * 		Quoted strings ({@code "..."} or {@code '...'}) are converted to {@link String Strings}.
 * 	<li>
 * 		Multiline strings ({@code '''...'''}) are converted to {@link String Strings}.
 * 	<li>
 * 		Quoteless strings are converted to {@link String Strings}.
 * 	<li>
 * 		Numbers are converted to {@link Integer Integers}, {@link Long Longs},
 * 		{@link Float Floats}, or {@link Double Doubles} depending on size and format.
 * 	<li>
 * 		{@code true}/{@code false} are converted to {@link Boolean Booleans}.
 * 	<li>
 * 		{@code null} returns <jk>null</jk>.
 * 	<li>
 * 		Input consisting of only whitespace or comments returns <jk>null</jk>.
 * </ul>
 *
 * <h5 class='figure'>Example input - readable mode:</h5>
 * <p class='bjson'>
 * 	{
 * 	  name: Alice
 * 	  age: 30
 * 	}
 * </p>
 *
 * <h5 class='figure'>Example input - compact mode:</h5>
 * <p class='bjson'>
 * 	{name:Alice,age:30}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Root-braceless Hjson files (content without a wrapping {@code {}}) are supported on input,
 * 		but {@link HjsonSerializer} always emits root braces by default.
 * 		A parse-then-serialize round-trip of a root-braceless file will produce braced output.
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HjsonBasics">Hjson Basics</a>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", "java:S115"
})
public class HjsonParser extends ReaderParser implements HjsonMetaProvider {

	private static final String ARG_ctx = "ctx";

	private final java.util.concurrent.ConcurrentHashMap<ClassMeta<?>, HjsonClassMeta> hjsonClassMetas = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.concurrent.ConcurrentHashMap<BeanPropertyMeta, HjsonBeanPropertyMeta> hjsonBeanPropertyMetas = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParser.Builder {

		protected Builder() {
			consumes("application/hjson,application/hjson+json");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_ctx, copyFrom));
		}

		protected Builder(HjsonParser copyFrom) {
			super(assertArgNotNull(ARG_ctx, copyFrom));
		}

		@Override
		public HjsonParser build() {
			return new HjsonParser(this);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}
	}

	/** Default parser. */
	public static final HjsonParser DEFAULT = new HjsonParser(create());

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public HjsonParser(Builder builder) {
		super(builder);
	}

	@Override
	public HjsonParserSession.Builder createSession() {
		return HjsonParserSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	public HjsonBeanPropertyMeta getHjsonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return HjsonBeanPropertyMeta.DEFAULT;
		return hjsonBeanPropertyMetas.computeIfAbsent(bpm, k -> new HjsonBeanPropertyMeta(k, this));
	}

	@Override
	public HjsonClassMeta getHjsonClassMeta(ClassMeta<?> cm) {
		return hjsonClassMetas.computeIfAbsent(cm, k -> new HjsonClassMeta(k, this));
	}
}
