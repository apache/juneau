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
package org.apache.juneau.marshall.csv;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Serializes POJO models to CSV (Comma Separated Values) format.
 *
 * <p>
 * This serializer converts Java objects (primarily collections of beans) into CSV format, where each bean
 * becomes a row and bean properties become columns. The first row typically contains column headers derived
 * from bean property names.
 *
 * <h5 class='section'>JSON Parity Features (opt-in):</h5>
 * <ul>
 *   <li><b>Type discriminator</b> — {@link CsvSerializer.Builder#addBeanTypes() addBeanTypes()}.{@link CsvSerializer.Builder#addRootType() addRootType()}
 *       adds a {@code _type} column for polymorphic parsing.
 *   <li><b>Byte arrays</b> — {@link CsvSerializer.Builder#byteArrayFormat(CsvByteArrayCellFormat) byteArrayFormat(BASE64)} (default) or
 *       {@code SEMICOLON_DELIMITED} for {@code byte[]}; primitive arrays as {@code [1;2;3]}.
 *   <li><b>Nested structures</b> — {@link CsvSerializer.Builder#allowNestedStructures(boolean) allowNestedStructures(true)}
 *       enables inline {@code {key:val}} and {@code [val;val]} in cells.
 *   <li><b>Null marker</b> — {@link CsvSerializer.Builder#nullValue(String) nullValue("&lt;NULL&gt;")} (default) for unambiguous null.
 * </ul>
 *
 * <h5 class='section'>Data Structures Not Supported:</h5>
 * <ul>
 *   <li><b>Parent/inherited properties</b> — Hierarchy flattens; may collide with child names.
 *   <li><b>Optional wrappers</b> — May serialize as the inner value; round-trip differs from tree formats.
 * </ul>
 *
 * <h5 class='section'>Best Supported:</h5>
 * <p>
 * Collections of flat beans or maps whose properties are primitives, strings, numbers, enums, dates,
 * byte arrays, or (with {@code allowNestedStructures}) nested beans, maps, and lists.
 *
 * <h5 class='figure'>Example output (list of maps with a,b):</h5>
 * <p class='bcode'>
 * 	a,b
 * 	foo,bar
 * </p>
 *
 * <h5 class='figure'>Complex (list of beans with nested address, flattened):</h5>
 * <p class='bcode'>
 * 	name,age,address_street,address_city,address_state,tags
 * 	Alice,30,123 Main St,Boston,MA,"a,b,c"
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource" // Closeable resources are owned by the caller's serializer session; Eclipse JDT @Owning warning is by design.
})
public class CsvSerializer extends WriterSerializer implements CsvMetaProvider, RecordWritable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder<Builder> {

		private static final Cache<HashKey,CsvSerializer> CACHE = Cache.of(HashKey.class, CsvSerializer.class).build();

		private CsvByteArrayCellFormat byteArrayFormat;
		private boolean allowNestedStructures;
		private String nullValue;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/csv");
			byteArrayFormat = CsvByteArrayCellFormat.BASE64;
			nullValue = "<NULL>";
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			byteArrayFormat = copyFrom.byteArrayFormat;
			allowNestedStructures = copyFrom.allowNestedStructures;
			nullValue = copyFrom.nullValue;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(CsvSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			byteArrayFormat = copyFrom.byteArrayFormat;
			allowNestedStructures = copyFrom.allowNestedStructures;
			nullValue = copyFrom.nullValue;
		}

		/**
		 * String to write for null values. Parser treats cells matching this as null.
		 *
		 * <p>
		 * Default is {@code <NULL>} to avoid confusion with the literal string {@code "null"}.
		 *
		 * @param value The null marker string.
		 * @return This object.
		 */
		public Builder nullValue(String value) {
			nullValue = value;
			return this;
		}

		/**
		 * Enables inline {@code {key:val}} and {@code [val;val]} notation in cells for nested beans, maps, and lists.
		 *
		 * @param value Whether to allow nested structures.
		 * @return This object.
		 */
		public Builder allowNestedStructures(boolean value) {
			allowNestedStructures = value;
			return this;
		}

		/**
		 * Format for serializing {@code byte[]} arrays in CSV cells.
		 *
		 * <p>
		 * Default is {@link CsvByteArrayCellFormat#BASE64} (matches JSON).
		 *
		 * @param value The format to use.
		 * @return This object.
		 */
		public Builder byteArrayFormat(CsvByteArrayCellFormat value) {
			byteArrayFormat = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), byteArrayFormat, allowNestedStructures, nullValue);
		}

		@Override /* Overridden from Context.Builder<?> */
		public CsvSerializer build() {
			return cache(CACHE).build(CsvSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}


	}

	/** Default serializer, all default settings.*/
	public static final CsvSerializer DEFAULT = new CsvSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Map<ClassMeta<?>,CsvClassMeta> csvClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,CsvBeanPropertyMeta> csvBeanPropertyMetas = new ConcurrentHashMap<>();
	private final CsvByteArrayCellFormat byteArrayFormat;
	private final boolean allowNestedStructures;
	private final String nullValue;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public CsvSerializer(Builder builder) {
		super(builder);
		byteArrayFormat = builder.byteArrayFormat != null ? builder.byteArrayFormat : CsvByteArrayCellFormat.BASE64;
		allowNestedStructures = builder.allowNestedStructures;
		nullValue = builder.nullValue != null ? builder.nullValue : "<NULL>";
	}

	/**
	 * Returns the format for serializing {@code byte[]} arrays in CSV cells.
	 *
	 * @return The byte array format.
	 */
	public CsvByteArrayCellFormat getByteArrayFormat() {
		return byteArrayFormat;
	}

	/**
	 * Returns whether inline {@code {key:val}} and {@code [val;val]} notation is enabled.
	 *
	 * @return Whether nested structures are allowed.
	 */
	public boolean isAllowNestedStructures() {
		return allowNestedStructures;
	}

	/**
	 * Returns the string written for null values.
	 *
	 * @return The null marker.
	 */
	public String getNullValue() {
		return nullValue;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public CsvSerializerSession.Builder createSession() {
		return CsvSerializerSession.create(this);
	}

	@Override /* Overridden from CsvMetaProvider */
	public CsvBeanPropertyMeta getCsvBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return CsvBeanPropertyMeta.DEFAULT;
		return csvBeanPropertyMetas.computeIfAbsent(bpm, k -> new CsvBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from CsvMetaProvider */
	public CsvClassMeta getCsvClassMeta(ClassMeta<?> cm) {
		return csvClassMetas.computeIfAbsent(cm, k -> new CsvClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public CsvSerializerSession getSession() { return createSession().build(); }

	/**
	 * Convenience delegator that opens a {@link RecordWriter} over the output using
	 * <b>default session arguments</b> (mirrors {@link #serialize(Object)}).
	 *
	 * <p>
	 * CSV is naturally row-oriented &mdash; each {@link RecordWriter#write(Object) write(...)} call appends
	 * one row &mdash; but the current cursor is buffered (the full list is materialized at close); a streaming
	 * implementation is a Phase 3b deferred item, so {@link #isRecordStreaming()} returns <jk>false</jk>.
	 *
	 * <p>
	 * The real implementation lives on {@link CsvSerializerSession#serializeRecords(Object)}.  Callers that
	 * need request-derived configuration should call {@link #createSession()} and invoke
	 * {@link CsvSerializerSession#serializeRecords(Object)} on the built session instead.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* RecordWritable */
	public RecordWriter serializeRecords(Object output) throws IOException {
		return getSession().serializeRecords(output);
	}

	@Override /* RecordWritable */
	public boolean isRecordStreaming() {
		return false;
	}
}