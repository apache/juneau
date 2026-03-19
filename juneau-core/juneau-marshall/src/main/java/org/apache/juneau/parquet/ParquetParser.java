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
package org.apache.juneau.parquet;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.parser.InputStreamParser;

/**
 * Parses Apache Parquet binary data into bean collections.
 *
 * <p>
 * Always returns a {@link java.util.List}&lt;T&gt;; each row in the file becomes one list element.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <c>Content-Type</c>: <bc>application/vnd.apache.parquet</bc>
 */
@SuppressWarnings({
	"java:S110",
	"java:S115"
})
public class ParquetParser extends InputStreamParser implements ParquetMetaProvider {

	private static final String ARG_copyFrom = "copyFrom";

	private final Map<BeanPropertyMeta,ParquetBeanPropertyMeta> parquetBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,ParquetClassMeta> parquetClassMetas = new ConcurrentHashMap<>();

	/** Default parser. */
	public static final ParquetParser DEFAULT = new ParquetParser(create());

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParser.Builder {

		private static final Cache<HashKey,ParquetParser> CACHE = Cache.of(HashKey.class, ParquetParser.class).build();

		private String nullKeyString;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/vnd.apache.parquet");
			nullKeyString = env("ParquetParser.nullKeyString", "<NULL>");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullKeyString = copyFrom.nullKeyString;
		}

		protected Builder(ParquetParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullKeyString = copyFrom.nullKeyString;
		}

		/**
		 * The string used when parsing Parquet MAP keys that represent <jk>null</jk>.
		 *
		 * <p>
		 * Must match {@link ParquetSerializer.Builder#nullKeyString(String)} for round-trip.
		 *
		 * @param value The placeholder string. Default is <js>"&lt;NULL&gt;"</js>.
		 * @return This object.
		 */
		public Builder nullKeyString(String value) {
			nullKeyString = value == null ? "<NULL>" : value;
			return this;
		}

		@Override /* InputStreamParser.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public ParquetParser build() {
			return cache(CACHE).build(ParquetParser.class);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nullKeyString);
		}
	}

	/**
	 * Creates a new parser builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final String nullKeyString;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this parser.
	 */
	public ParquetParser(Builder builder) {
		super(builder);
		nullKeyString = builder.nullKeyString;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	public ParquetParserSession.Builder createSession() {
		return ParquetParserSession.create(this);
	}

	@Override
	public ParquetBeanPropertyMeta getParquetBeanPropertyMeta(BeanPropertyMeta bpm) {
		return bpm == null ? ParquetBeanPropertyMeta.DEFAULT
			: parquetBeanPropertyMetas.computeIfAbsent(bpm, k -> new ParquetBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override
	public ParquetClassMeta getParquetClassMeta(ClassMeta<?> cm) {
		return parquetClassMetas.computeIfAbsent(cm, k -> new ParquetClassMeta(k, this));
	}
}
