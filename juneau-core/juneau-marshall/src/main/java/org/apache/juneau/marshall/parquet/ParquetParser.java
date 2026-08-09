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
package org.apache.juneau.marshall.parquet;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Parses Apache Parquet binary data into bean collections.
 *
 * <p>
 * Always returns a {@link List}&lt;T&gt;; each row in the file becomes one list element.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <c>Content-Type</c>: <bc>application/vnd.apache.parquet</bc>
 */
@SuppressWarnings({
	"java:S110",
	"java:S115",
	"resource" // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class ParquetParser extends InputStreamParser implements ParquetMetaProvider, RecordReadable, ArrayRecordReadable {

	private static final String ARG_copyFrom = "copyFrom";

	private static final int DEFAULT_MAX_LENGTH = ParquetParserSession.DEFAULT_MAX_LENGTH;
	private static final int DEFAULT_MAX_COUNT = ParquetParserSession.DEFAULT_MAX_COUNT;
	private static final int DEFAULT_MAX_INPUT_LENGTH = ParquetParserSession.DEFAULT_MAX_INPUT_LENGTH;

	private final Map<BeanPropertyMeta,ParquetBeanPropertyMeta> parquetBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,ParquetClassMeta> parquetClassMetas = new ConcurrentHashMap<>();

	/** Default parser. */
	public static final ParquetParser DEFAULT = new ParquetParser(create());

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParser.Builder<Builder> {

		private static final Cache<HashKey,ParquetParser> CACHE = Cache.of(HashKey.class, ParquetParser.class).build();

		private String nullKeyString;
		private int maxLength = DEFAULT_MAX_LENGTH;
		private int maxCount = DEFAULT_MAX_COUNT;
		private int maxInputLength = DEFAULT_MAX_INPUT_LENGTH;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/vnd.apache.parquet");
			nullKeyString = env("ParquetParser.nullKeyString", "<NULL>");
			maxLength = env("ParquetParser.maxLength", DEFAULT_MAX_LENGTH);
			maxCount = env("ParquetParser.maxCount", DEFAULT_MAX_COUNT);
			maxInputLength = env("ParquetParser.maxInputLength", DEFAULT_MAX_INPUT_LENGTH);
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullKeyString = copyFrom.nullKeyString;
			maxLength = copyFrom.maxLength;
			maxCount = copyFrom.maxCount;
			maxInputLength = copyFrom.maxInputLength;
		}

		protected Builder(ParquetParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullKeyString = copyFrom.nullKeyString;
			maxLength = copyFrom.maxLength;
			maxCount = copyFrom.maxCount;
			maxInputLength = copyFrom.maxInputLength;
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

		/**
		 * The maximum allowed wire-declared length (in bytes) for a single Parquet page (compressed or
		 * uncompressed).
		 *
		 * <p>
		 * Guards against malformed or adversarial input where a small file declares a huge per-page byte
		 * size, which would otherwise trigger {@link OutOfMemoryError}.  Malformed page headers are
		 * reported as a clean parse error instead.
		 *
		 * @param value The maximum length in bytes.  Default is 256 MiB.  Values &le; 0 disable the cap.
		 * @return This object.
		 */
		public Builder maxLength(int value) {
			maxLength = value;
			return this;
		}

		/**
		 * The maximum allowed wire-declared element count (file/row-group row counts, column-chunk value
		 * counts).
		 *
		 * <p>
		 * Guards against malformed or adversarial input where a small footer declares a huge row or value
		 * count, which would otherwise drive a huge {@code ArrayList} pre-allocation.  Malformed counts are
		 * reported as a clean parse error instead.
		 *
		 * @param value The maximum count.  Default is 10 million.  Values &le; 0 disable the cap.
		 * @return This object.
		 */
		public Builder maxCount(int value) {
			maxCount = value;
			return this;
		}

		/**
		 * The maximum allowed size (in bytes) of the whole Parquet input buffered into memory before parsing.
		 *
		 * <p>
		 * Parquet's end-of-file footer and column-major layout require the entire input to be buffered before
		 * any record can be reconstructed.  This cap rejects an oversized input with a clean parse error
		 * instead of buffering an unbounded body into memory.
		 *
		 * @param value The maximum input size in bytes.  Default is 256 MiB.  Values &le; 0 disable the cap.
		 * @return This object.
		 */
		public Builder maxInputLength(int value) {
			maxInputLength = value;
			return this;
		}

		@Override /* InputStreamParser.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public ParquetParser build() {
			return cache(CACHE).build(ParquetParser.class);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nullKeyString, maxLength, maxCount, maxInputLength);
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
	private final int maxLength;
	private final int maxCount;
	private final int maxInputLength;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this parser.
	 */
	public ParquetParser(Builder builder) {
		super(builder);
		nullKeyString = builder.nullKeyString;
		maxLength = builder.maxLength;
		maxCount = builder.maxCount;
		maxInputLength = builder.maxInputLength;
	}

	/**
	 * Returns the maximum allowed wire-declared length (in bytes) for a single Parquet page.
	 *
	 * @return The maximum length in bytes.
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * Returns the maximum allowed wire-declared element count (row/value counts).
	 *
	 * @return The maximum count.
	 */
	public int getMaxCount() {
		return maxCount;
	}

	/**
	 * Returns the maximum allowed size (in bytes) of the whole Parquet input buffered before parsing.
	 *
	 * @return The maximum input size in bytes.
	 */
	public int getMaxInputLength() {
		return maxInputLength;
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

	/**
	 * Convenience delegator for the whole-value {@link RecordReader} using <b>default session
	 * arguments</b>.  The real implementation lives on
	 * {@link ParquetParserSession#readRecords(Object)}.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* RecordReadable */
	public RecordReader readRecords(Object input) throws IOException {
		return ((RecordReadable) getSession()).readRecords(input);
	}

	/**
	 * Convenience delegator for the buffered array-element {@link RecordReader} using <b>default
	 * session arguments</b>.  The real implementation lives on
	 * {@link ParquetParserSession#readArrayRecords(Object)}.
	 *
	 * @param input The input.
	 * @return A buffered {@link RecordReader}.
	 * @throws IOException If a problem occurred reading the input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader readArrayRecords(Object input) throws IOException {
		return ((ArrayRecordReadable) getSession()).readArrayRecords(input);
	}

	/**
	 * The Parquet record cursor is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* RecordReadable */
	public boolean isRecordStreaming() { return false; }

	/**
	 * The Parquet array-record cursor is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* ArrayRecordReadable */
	public boolean isArrayRecordStreaming() { return false; }
}
