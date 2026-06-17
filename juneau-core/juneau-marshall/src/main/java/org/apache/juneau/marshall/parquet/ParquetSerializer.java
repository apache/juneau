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
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Serializes bean collections to Apache Parquet binary format.
 *
 * <p>
 * Parquet is a columnar format; each bean is one row and each bean property is one column.
 * The serializer accepts a {@link Collection}, array, or single bean at the root.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Produces <c>Content-Type</c>: <bc>application/vnd.apache.parquet</bc>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115",  // Constants use UPPER_snakeCase naming convention
	"resource" // Closeable resources are owned by the caller's serializer session; Eclipse JDT @Owning warning is by design.
})
public class ParquetSerializer extends OutputStreamSerializer implements ParquetMetaProvider, RecordWritable, ArrayRecordWritable {

	private static final String ARG_copyFrom = "copyFrom";

	/** Default serializer. */
	public static final ParquetSerializer DEFAULT = new ParquetSerializer(create());

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializer.Builder<Builder> {

		private static final Cache<HashKey,ParquetSerializer> CACHE = Cache.of(HashKey.class, ParquetSerializer.class).build();

		private CompressionCodec compressionCodec = CompressionCodec.UNCOMPRESSED;
		private int rowGroupSize = 128 * 1024 * 1024;
		private int pageSize = 1024 * 1024;
		private boolean addBeanTypesParquet = false;
		private boolean writeDatesAsTimestamp = true;
		private boolean emitLogicalTypes = false;
		private boolean nativeLogicalTypes = false;
		private ParquetCycleHandling cycleHandling = ParquetCycleHandling.NULL;
		private int maxRecursionDepth = 5;
		private String nullKeyString;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/vnd.apache.parquet");
			accept("application/vnd.apache.parquet");
			emitLogicalTypes = env("ParquetSerializer.emitLogicalTypes", false);
			maxRecursionDepth = env("ParquetSerializer.maxRecursionDepth", 5);
			nullKeyString = env("ParquetSerializer.nullKeyString", "<NULL>");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			compressionCodec = copyFrom.compressionCodec;
			rowGroupSize = copyFrom.rowGroupSize;
			pageSize = copyFrom.pageSize;
			addBeanTypesParquet = copyFrom.addBeanTypesParquet;
			writeDatesAsTimestamp = copyFrom.writeDatesAsTimestamp;
			emitLogicalTypes = copyFrom.emitLogicalTypes;
			nativeLogicalTypes = copyFrom.nativeLogicalTypes;
			cycleHandling = copyFrom.cycleHandling;
			maxRecursionDepth = copyFrom.maxRecursionDepth;
			nullKeyString = copyFrom.nullKeyString;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The serializer to copy from.
		 */
		protected Builder(ParquetSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			compressionCodec = copyFrom.compressionCodec;
			rowGroupSize = copyFrom.rowGroupSize;
			pageSize = copyFrom.pageSize;
			addBeanTypesParquet = copyFrom.addBeanTypesParquet;
			writeDatesAsTimestamp = copyFrom.writeDatesAsTimestamp;
			emitLogicalTypes = copyFrom.emitLogicalTypes;
			nativeLogicalTypes = copyFrom.nativeLogicalTypes;
			cycleHandling = copyFrom.cycleHandling;
			maxRecursionDepth = copyFrom.maxRecursionDepth;
			nullKeyString = copyFrom.nullKeyString;
		}

		/**
		 * Sets the compression codec.
		 *
		 * @param value UNCOMPRESSED or GZIP.
		 * @return This object.
		 */
		public Builder compressionCodec(CompressionCodec value) {
			compressionCodec = value != null ? value : CompressionCodec.UNCOMPRESSED;
			return this;
		}

		/**
		 * Sets the target row group size in bytes.
		 *
		 * @param value Size in bytes.
		 * @return This object.
		 */
		public Builder rowGroupSize(int value) {
			rowGroupSize = Math.max(1024, value);
			return this;
		}

		/**
		 * Sets the target page size in bytes.
		 *
		 * @param value Size in bytes.
		 * @return This object.
		 */
		public Builder pageSize(int value) {
			pageSize = Math.max(1024, value);
			return this;
		}

		/**
		 * Adds <js>_type</js> column for polymorphic beans.
		 *
		 * @param value Whether to add the column.
		 * @return This object.
		 */
		public Builder addBeanTypesParquet(boolean value) {
			addBeanTypesParquet = value;
			return this;
		}

		/**
		 * Sets whether dates are written as TIMESTAMP(MILLIS).
		 *
		 * @param value If true, use INT64+TIMESTAMP_MILLIS; else use ISO 8601 string.
		 * @return This object.
		 */
		public Builder writeDatesAsTimestamp(boolean value) {
			writeDatesAsTimestamp = value;
			return this;
		}

		/**
		 * Emits explicit logical-type discriminator metadata for ambiguous physical representations.
		 *
		 * <p>
		 * Disabled by default, which preserves the current Parquet wire shape (physical type plus
		 * {@code convertedType} only). Modeled on JSON's optional <js>_type</js> discriminator: when enabled,
		 * the serializer additionally writes the Parquet <c>LogicalType</c> union into the file footer for
		 * columns whose physical encoding is otherwise ambiguous.
		 *
		 * <p>
		 * First-pass scope is UUID only — the one type whose round-trip relies on the
		 * {@code FIXED_LEN_BYTE_ARRAY} physical-type signal. The legacy {@code convertedType} field is still
		 * emitted alongside the union, so readers that consume only the legacy field (including older Juneau
		 * versions) continue to work unchanged, while modern readers can route on {@code logicalType}.
		 *
		 * @param value Whether to emit logical-type discriminator metadata.
		 * @return This object.
		 */
		public Builder emitLogicalTypes(boolean value) {
			emitLogicalTypes = value;
			return this;
		}

		/**
		 * Emits binary-native logical types (DECIMAL, DATE, TIME, TIMESTAMP-micros) on write instead of the
		 * default string / TIMESTAMP-millis normalization.
		 *
		 * <p>
		 * Disabled by default, which preserves the current wire shape (and lossless Juneau&#8596;Juneau
		 * round-trips).  When enabled, {@code BigDecimal} columns are written as DECIMAL with scale/precision,
		 * {@code LocalDate} as DATE, {@code LocalTime}/{@code OffsetTime} as TIME(MICROS), and date/instant
		 * temporals as TIMESTAMP(MICROS) — interoperable with parquet-mr / Spark and sub-millisecond precise.
		 * Read-side decode of these types is always on regardless of this flag.
		 *
		 * @param value Whether to emit binary-native logical types on write.
		 * @return This object.
		 */
		public Builder nativeLogicalTypes(boolean value) {
			nativeLogicalTypes = value;
			return this;
		}

		/**
		 * Sets how to handle cyclic references during serialization.
		 *
		 * @param value THROW to fail with {@link SerializeException};
		 * 	NULL to serialize cycles as <jk>null</jk>.
		 * @return This object.
		 */
		public Builder cycleHandling(ParquetCycleHandling value) {
			cycleHandling = value != null ? value : ParquetCycleHandling.NULL;
			return this;
		}

		/**
		 * Sets the maximum expansion depth for self-referential (recursive) bean types.
		 *
		 * <p>
		 * A self-referential type (e.g. <c>class Node { String val; Node next; }</c>) is expanded into this
		 * many nested column levels so acyclic data (trees, linked lists) up to this depth round-trips
		 * losslessly instead of degrading to a {@code toString} representation (GAP-13).  Structure deeper
		 * than this is truncated to <jk>null</jk> (documented-lossy).  Larger values support deeper structures
		 * at the cost of a wider schema; the schema is expanded structurally regardless of the actual data
		 * depth, so keep this modest for recursive types with high fan-out.
		 *
		 * @param value The maximum recursion expansion depth. Values below 1 are clamped to 1.
		 * @return This object.
		 */
		public Builder maxRecursionDepth(int value) {
			maxRecursionDepth = Math.max(1, value);
			return this;
		}

		/**
		 * The string used to represent <jk>null</jk> map keys in Parquet MAP key_value format.
		 *
		 * <p>
			Parquet MAP keys are UTF8; <jk>null</jk> keys cannot be stored directly. When serializing a
		 * {@link Map} with a <jk>null</jk> key, this string is used. When parsing, a key
		 * equal to this string is restored as <jk>null</jk>.
		 *
		 * @param value The placeholder string for null keys. Default is <js>"&lt;NULL&gt;"</js>.
		 * @return This object.
		 */
		public Builder nullKeyString(String value) {
			nullKeyString = value == null ? "<NULL>" : value;
			return this;
		}

		@Override /* OutputStreamSerializer.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public ParquetSerializer build() {
			return cache(CACHE).build(ParquetSerializer.class);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), compressionCodec, rowGroupSize, pageSize, addBeanTypesParquet, writeDatesAsTimestamp, emitLogicalTypes, nativeLogicalTypes, cycleHandling, maxRecursionDepth, nullKeyString);
		}
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final CompressionCodec compressionCodec;
	final int rowGroupSize;
	final int pageSize;
	final boolean addBeanTypesParquet;
	final boolean writeDatesAsTimestamp;
	final boolean emitLogicalTypes;
	final boolean nativeLogicalTypes;
	final ParquetCycleHandling cycleHandling;
	final int maxRecursionDepth;
	final String nullKeyString;

	private final Map<BeanPropertyMeta,ParquetBeanPropertyMeta> parquetBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,ParquetClassMeta> parquetClassMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public ParquetSerializer(Builder builder) {
		super(builder);
		compressionCodec = builder.compressionCodec;
		rowGroupSize = builder.rowGroupSize;
		pageSize = builder.pageSize;
		addBeanTypesParquet = builder.addBeanTypesParquet;
		writeDatesAsTimestamp = builder.writeDatesAsTimestamp;
		emitLogicalTypes = builder.emitLogicalTypes;
		nativeLogicalTypes = builder.nativeLogicalTypes;
		cycleHandling = builder.cycleHandling;
		maxRecursionDepth = builder.maxRecursionDepth;
		nullKeyString = builder.nullKeyString;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	public ParquetSerializerSession.Builder createSession() {
		return ParquetSerializerSession.create(this);
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
	 * Convenience delegator for the whole-value {@link RecordWriter} using <b>default session
	 * arguments</b>.  The real implementation lives on
	 * {@link ParquetSerializerSession#serializeRecords(Object)}.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* RecordWritable */
	public RecordWriter serializeRecords(Object output) throws IOException {
		return ((RecordWritable) getSession()).serializeRecords(output);
	}

	/**
	 * Convenience delegator for the buffered array-element {@link RecordWriter} using <b>default
	 * session arguments</b>.  The real implementation lives on
	 * {@link ParquetSerializerSession#serializeArrayRecords(Object)}.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* ArrayRecordWritable */
	public RecordWriter serializeArrayRecords(Object output) throws IOException {
		return ((ArrayRecordWritable) getSession()).serializeArrayRecords(output);
	}

	/**
	 * The Parquet record writer is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* RecordWritable */
	public boolean isRecordStreaming() { return false; }

	/**
	 * The Parquet array-record writer is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* ArrayRecordWritable */
	public boolean isArrayRecordStreaming() { return false; }
}
