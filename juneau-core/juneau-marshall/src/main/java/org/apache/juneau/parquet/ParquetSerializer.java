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
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes bean collections to Apache Parquet binary format.
 *
 * <p>
 * Parquet is a columnar format; each bean is one row and each bean property is one column.
 * The serializer accepts a {@link java.util.Collection}, array, or single bean at the root.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Produces <c>Content-Type</c>: <bc>application/vnd.apache.parquet</bc>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115"  // Constants use UPPER_snakeCase naming convention
})
public class ParquetSerializer extends OutputStreamSerializer implements ParquetMetaProvider {

	private static final String ARG_copyFrom = "copyFrom";

	/** Default serializer. */
	public static final ParquetSerializer DEFAULT = new ParquetSerializer(create());

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializer.Builder {

		private static final Cache<HashKey,ParquetSerializer> CACHE = Cache.of(HashKey.class, ParquetSerializer.class).build();

		private CompressionCodec compressionCodec = CompressionCodec.UNCOMPRESSED;
		private int rowGroupSize = 128 * 1024 * 1024;
		private int pageSize = 1024 * 1024;
		private boolean addBeanTypesParquet = false;
		private boolean writeDatesAsTimestamp = true;
		private ParquetCycleHandling cycleHandling = ParquetCycleHandling.NULL;
		private String nullKeyString;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/vnd.apache.parquet");
			accept("application/vnd.apache.parquet");
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
			cycleHandling = copyFrom.cycleHandling;
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
			cycleHandling = copyFrom.cycleHandling;
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
		 * Sets how to handle cyclic references during serialization.
		 *
		 * @param value THROW to fail with {@link org.apache.juneau.serializer.SerializeException};
		 * 	NULL to serialize cycles as <jk>null</jk>.
		 * @return This object.
		 */
		public Builder cycleHandling(ParquetCycleHandling value) {
			cycleHandling = value != null ? value : ParquetCycleHandling.NULL;
			return this;
		}

		/**
		 * The string used to represent <jk>null</jk> map keys in Parquet MAP key_value format.
		 *
		 * <p>
			Parquet MAP keys are UTF8; <jk>null</jk> keys cannot be stored directly. When serializing a
		 * {@link java.util.Map} with a <jk>null</jk> key, this string is used. When parsing, a key
		 * equal to this string is restored as <jk>null</jk>.
		 *
		 * @param value The placeholder string for null keys. Default is <js>"&lt;NULL&gt;"</js>.
		 * @return This object.
		 */
		public Builder nullKeyString(String value) {
			nullKeyString = value == null ? "<NULL>" : value;
			return this;
		}

		@Override /* OutputStreamSerializer.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public ParquetSerializer build() {
			return cache(CACHE).build(ParquetSerializer.class);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), compressionCodec, rowGroupSize, pageSize, addBeanTypesParquet, writeDatesAsTimestamp, cycleHandling, nullKeyString);
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
	final ParquetCycleHandling cycleHandling;
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
		cycleHandling = builder.cycleHandling;
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
}
