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
package org.apache.juneau.hocon;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.parser.*;

/**
 * Parses HOCON (Human-Optimized Config Object Notation) text into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/hocon</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Multi-pass parsing strategy: tokenize → build intermediate tree → resolve substitutions → convert to bean.
 *
 * <p>
 * Accepts all three key-value separators: <c>=</c> (HOCON convention), <c>:</c> (JSON style), and whitespace before <c>{</c> (for object values).
 *
 * <p>
 * Supports unquoted string keys and values, path expressions (<c>a.b.c = value</c>), object merging, substitution resolution,
 * triple-quoted strings, both <c>//</c> and <c>#</c> comments, and optional root braces.
 *
 * <h5 class='figure'>Example input (bean):</h5>
 * <p class='bjson'>
 * name = myapp
 * port = 8080
 * debug = true
 * database {
 *   host = localhost
 *   port = 5432
 * }
 * tags = [web, api, rest]
 * </p>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'><c>include</c> directives are not supported — they are ignored with a warning during parsing.
 * 	<li class='note'>Duration/size unit suffixes (<c>10s</c>, <c>5m</c>, <c>512K</c>, <c>2G</c>) are treated as unquoted strings.
 * 	<li class='note'>Substitutions are resolved before bean conversion; the resolved (concrete) values are what Juneau sees.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HoconBasics">hocon-basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", "java:S115"
})
public class HoconParser extends ReaderParser implements HoconMetaProvider {

	private static final String ARG_ctx = "ctx";

	private final java.util.concurrent.ConcurrentHashMap<ClassMeta<?>, HoconClassMeta> hoconClassMetas = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.concurrent.ConcurrentHashMap<BeanPropertyMeta, HoconBeanPropertyMeta> hoconBeanPropertyMetas = new java.util.concurrent.ConcurrentHashMap<>();

	/** Whether to resolve ${var} substitutions. */
	protected final boolean resolveSubstitutions;

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParser.Builder {

		private static final Cache<HashKey, HoconParser> CACHE = Cache.of(HashKey.class, HoconParser.class).build();

		private boolean resolveSubstitutions = true;

		protected Builder() {
			consumes("application/hocon");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_ctx, copyFrom));
			resolveSubstitutions = copyFrom.resolveSubstitutions;
		}

		protected Builder(HoconParser copyFrom) {
			super(assertArgNotNull(ARG_ctx, copyFrom));
			resolveSubstitutions = copyFrom.resolveSubstitutions;
		}

		/**
		 * Enables substitution resolution.
		 *
		 * @param value <jk>true</jk> to resolve ${var} and ${?var}.
		 * @return This object.
		 */
		public Builder resolveSubstitutions(boolean value) {
			resolveSubstitutions = value;
			return this;
		}

		@Override
		public HoconParser build() {
			return cache(CACHE).build(HoconParser.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), resolveSubstitutions);
		}
	}

	/** Default parser. */
	public static final HoconParser DEFAULT = new HoconParser(create());

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
	public HoconParser(Builder builder) {
		super(builder);
		resolveSubstitutions = builder.resolveSubstitutions;
	}

	@Override
	public HoconParserSession.Builder createSession() {
		return HoconParserSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	public HoconBeanPropertyMeta getHoconBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return HoconBeanPropertyMeta.DEFAULT;
		return hoconBeanPropertyMetas.computeIfAbsent(bpm, k -> new HoconBeanPropertyMeta(k, this));
	}

	@Override
	public HoconClassMeta getHoconClassMeta(ClassMeta<?> cm) {
		return hoconClassMetas.computeIfAbsent(cm, k -> new HoconClassMeta(k, this));
	}
}
